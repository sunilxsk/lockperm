package io.github.sunilxsk.lockperm

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.FileObserver
import android.os.StatFs
import android.os.SystemClock
import io.github.libxposed.api.XposedModule













internal object ConditionExitWatcher {

    @Volatile
    private var started = false

    @Volatile
    private var fired = false

    
    private val startMs = System.currentTimeMillis()

    
    @Volatile
    private var lastTouchMs = System.currentTimeMillis()

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    
    @Volatile
    private var lastProcJiffies = -1L
    @Volatile
    private var lastTotalJiffies = -1L
    @Volatile
    private var lastCpuSampleMs = 0L
    @Volatile
    private var lastChildJiffies = 0L
    
    @Volatile
    private var cpuOverStreak = 0

    
    
    private fun log(msg: String) {
        if (!XpState.Flags.logEnabled) return
        android.util.Log.i("LockPerm", "[cond-exit] $msg")
    }

    
    @JvmStatic
    fun noteInteraction() {
        lastTouchMs = System.currentTimeMillis()
    }

    @JvmStatic
    fun start(module: XposedModule, prefs: android.content.SharedPreferences, context: Context?) {
        if (started) return
        val cfg = runCatching { XpState.refresh(prefs) }.getOrNull()
        if (cfg == null || !cfg.exitEnable) return
        
        if (!anyCondition(cfg)) return
        synchronized(this) {
            if (started) return
            started = true
        }

        val ctx = context ?: return
        watchBattery(ctx, prefs)
        watchFiles(ctx, prefs)
        watchNetwork(ctx, prefs)

        Thread({
            while (true) {
                try {
                    Thread.sleep(2000)
                    val c = runCatching { XpState.refresh(prefs) }.getOrNull()
                    if (c == null || !c.exitEnable) break
                    if (fired) break
                    val hit = check(c, ctx)
                    if (hit != null) {
                        fired = true
                        log("条件达成：$hit，执行退出")
                        mainHandler.post {
                            runCatching {
                                android.widget.Toast.makeText(
                                    ctx, "已达退出条件：$hit", android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            ExitExecutor.runNow(parallel = false, beforeExit = true)
                        }
                        break
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (_: Throwable) {
                    
                    break
                }
            }
        }, "lockperm-cond-exit").apply { isDaemon = true }.start()

        log("watcher started")
    }

    private fun anyCondition(c: XpState.Snapshot): Boolean =
        c.condAtTime || c.condCountdown || c.condMem || c.condCpu || c.condDisk ||
            c.condNet || c.condBatt || c.condFile || c.condIdle

    
    private fun check(c: XpState.Snapshot, ctx: Context): String? {
        if (c.condAtTime) {
            val hh = c.condAtTimeHh.coerceIn(0, 23)
            val mm = c.condAtTimeMm.coerceIn(0, 59)
            val cal = java.util.Calendar.getInstance()
            val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                cal.get(java.util.Calendar.MINUTE)
            if (nowMin == hh * 60 + mm) return "到达指定时刻 %02d:%02d".format(hh, mm)
        }

        if (c.condCountdown) {
            val min = c.condCountdownMin.coerceAtLeast(1)
            if (System.currentTimeMillis() - startMs >= min * 60_000L) {
                return "已运行 $min 分钟"
            }
        }

        if (c.condMem) {
            val mb = c.condMemMb.coerceAtLeast(1)
            
            
            
            
            val selfKb = runCatching { android.os.Debug.getPss() }.getOrNull() ?: 0L
            val childKb = scanChildren().rssKb
            val totalMb = (selfKb + childKb) / 1024L
            if (totalMb >= mb) {
                return "内存占用 ${totalMb}MB ≥ ${mb}MB（含子进程 ${childKb / 1024L}MB）"
            }
        }

        if (c.condDisk) {
            val mb = c.condDiskMb.coerceAtLeast(1)
            val availMb = freeMb(ctx)
            if (availMb >= 0 && availMb < mb) {
                return "剩余空间 ${availMb}MB < ${mb}MB"
            }
        }

        if (c.condNet) {
            
            val connected = isNetworkConnected(ctx)
            if (connected != null) {
                if (c.condNetMode == 0 && !connected) return "网络已断开"
                if (c.condNetMode == 1 && connected) return "网络已连接"
            }
        }

        if (c.condCpu) {
            val pct = cpuPercent()
            if (pct >= 0) {
                val limit = c.condCpuPct.coerceIn(1, 1600)
                if (pct >= limit) {
                    cpuOverStreak++
                    
                    if (cpuOverStreak >= 2) {
                        return "CPU 占用 $pct% ≥ $limit%（连续 2 次）"
                    }
                } else {
                    cpuOverStreak = 0
                }
                maybeLogCpu(pct, limit)
            }
        }

        if (c.condBatt && c.condBattMode == 0) {
            
            
            val pct = batteryPercent(ctx)
            if (pct >= 0 && pct <= c.condBattPct.coerceIn(0, 100)) {
                return "电量 $pct% ≤ ${c.condBattPct}%"
            }
        }

        if (c.condIdle) {
            val min = c.condIdleMin.coerceAtLeast(1)
            if (System.currentTimeMillis() - lastTouchMs >= min * 60_000L) {
                return "已 $min 分钟无操作"
            }
        }

        return null
    }

    

















    private fun cpuPercent(): Int {
        val proc = readProcSelfJiffies()
        if (proc < 0) return -1
        val now = System.currentTimeMillis()
        val total = readTotalJiffies()
        val child = scanChildren().jiffies

        
        if (lastProcJiffies < 0 || lastCpuSampleMs == 0L) {
            lastProcJiffies = proc
            lastTotalJiffies = total
            lastChildJiffies = child
            lastCpuSampleMs = now
            return -1
        }
        val dProc = proc - lastProcJiffies
        val dTotal = total - lastTotalJiffies
        val dChild = child - lastChildJiffies
        val dMs = now - lastCpuSampleMs
        lastProcJiffies = proc
        lastTotalJiffies = total
        lastChildJiffies = child
        lastCpuSampleMs = now
        if (dMs <= 0) return -1

        val all = dProc + dChild
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        val pct = if (dTotal > 0 && lastTotalJiffies >= 0) {
            
            100.0 * all.toDouble() / dTotal.toDouble() * cores
        } else {
            
            100.0 * (all / 100.0) / (dMs / 1000.0)
        }
        
        return pct.coerceIn(0.0, 100.0 * cores).toInt()
    }

    
    private fun readProcSelfJiffies(): Long {
        return runCatching {
            val line = java.io.File("/proc/self/stat").readText()
            
            val tail = line.substringAfterLast(')', line)
            val parts = tail.trim().split(Regex("\\s+"))
            
            parts[11].toLong() + parts[12].toLong()
        }.getOrDefault(-1L)
    }

    
    private data class ChildStat(val jiffies: Long, val rssKb: Long)

    






    private fun scanChildren(): ChildStat {
        val me = android.os.Process.myPid()
        val dir = java.io.File("/proc")
        val list = runCatching { dir.listFiles() }.getOrNull() ?: return ChildStat(0L, 0L)
        var jif = 0L
        var rss = 0L
        for (f in list) {
            val pid = f.name.toIntOrNull() ?: continue
            if (pid == me) continue
            runCatching {
                val line = java.io.File(f, "stat").readText()
                
                val tail = line.substringAfterLast(')', line)
                val parts = tail.trim().split(Regex("\\s+"))
                
                if (parts.size < 14) return@runCatching
                if (parts[1].toInt() != me) return@runCatching
                jif += parts[11].toLong() + parts[12].toLong()
                
                val statm = java.io.File(f, "statm").readText().trim().split(Regex("\\s+"))
                if (statm.size >= 2) {
                    rss += statm[1].toLong() * PAGE_KB
                }
            }
        }
        return ChildStat(jif, rss)
    }

    
    private const val PAGE_KB = 4L

    



    private fun readTotalJiffies(): Long {
        return runCatching {
            val line = java.io.File("/proc/stat").bufferedReader().readLine()
                ?: return@runCatching -1L
            val parts = line.trim().split(Regex("\\s+")).drop(1)
            parts.take(7).sumOf { it.toLongOrNull() ?: 0L }
        }.getOrDefault(-1L)
    }

    
    private var cpuDiagTick = 0

    private fun maybeLogCpu(pct: Int, limit: Int) {
        if (!XpState.Flags.logEnabled) return
        if (++cpuDiagTick % 5 != 0) return
        log("CPU 实测 ${pct}%（阈值 $limit%，连续超 $cpuOverStreak/2 次）")
    }

    private fun freeMb(ctx: Context): Long {
        return runCatching {
            val dir = ctx.filesDir ?: ctx.cacheDir ?: return@runCatching -1L
            val sf = StatFs(dir.absolutePath)
            sf.availableBytes / (1024 * 1024)
        }.getOrDefault(-1L)
    }

    











    @Volatile
    private var netConnected: Boolean? = null

    private fun isNetworkConnected(ctx: Context): Boolean? {
        netConnected?.let { return it }
        return probeNetwork(ctx)
    }

    
    private fun probeNetwork(ctx: Context): Boolean? {
        var sawAny = false
        runCatching {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return@runCatching
            
            val n = cm.activeNetwork
            if (n != null) {
                val caps = cm.getNetworkCapabilities(n)
                if (caps != null) {
                    sawAny = true
                    if (caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        return true
                    }
                }
            }
            
            
            val ni = cm.activeNetworkInfo
            if (ni != null) {
                sawAny = true
                if (ni.isConnected) return true
            }
        }
        
        
        runCatching {
            val en = java.net.NetworkInterface.getNetworkInterfaces()
            while (en != null && en.hasMoreElements()) {
                val i = en.nextElement()
                if (i.isLoopback || !i.isUp) continue
                val addrs = i.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (!a.isLoopbackAddress && a is java.net.Inet4Address) {
                        sawAny = true
                        return true
                    }
                }
            }
        }
        return if (sawAny) false else null
    }

    
    private fun watchNetwork(ctx: Context, prefs: android.content.SharedPreferences) {
        runCatching {
            val filter = IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
            ctx.registerReceiver(
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        if (fired) return
                        val noConn = intent?.getBooleanExtra(
                            android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY, false
                        ) ?: return
                        val nowConnected = !noConn
                        netConnected = nowConnected
                        val cfg = runCatching { XpState.refresh(prefs) }.getOrNull() ?: return
                        if (!cfg.exitEnable || !cfg.condNet) return
                        val hit =
                            if (cfg.condNetMode == 0 && !nowConnected) "网络已断开"
                            else if (cfg.condNetMode == 1 && nowConnected) "网络已连接"
                            else null
                        if (hit != null) fire(ctx, hit)
                    }
                },
                filter,
            )
            log("network watcher registered")
        }.onFailure { log("network watcher failed: ${it.message}") }
    }

    
    private fun batteryPercent(ctx: Context): Int {
        return runCatching {
            val it = ctx.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            ) ?: return@runCatching -1
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) -1 else level * 100 / scale
        }.getOrDefault(-1)
    }

    private fun isCharging(ctx: Context): Boolean {
        return runCatching {
            val it = ctx.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            ) ?: return@runCatching false
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }.getOrDefault(false)
    }

    
    private fun watchBattery(ctx: Context, prefs: android.content.SharedPreferences) {
        runCatching {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            filter.addAction(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            ctx.registerReceiver(
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        val act = intent?.action ?: return
                        val cfg = runCatching { XpState.refresh(prefs) }.getOrNull() ?: return
                        if (!cfg.exitEnable || !cfg.condBatt || fired) return
                        
                        val hit = when {
                            cfg.condBattMode == 1 &&
                                act == Intent.ACTION_POWER_CONNECTED -> "开始充电"
                            cfg.condBattMode == 2 &&
                                act == Intent.ACTION_POWER_DISCONNECTED -> "结束充电"
                            cfg.condBattMode == 0 &&
                                act == Intent.ACTION_BATTERY_CHANGED -> {
                                val pct = batteryPercent(ctx)
                                if (pct >= 0 && pct <= cfg.condBattPct.coerceIn(0, 100)) {
                                    "电量 $pct% ≤ ${cfg.condBattPct}%"
                                } else null
                            }
                            else -> null
                        }
                        if (hit != null) fire(ctx, hit)
                    }
                },
                filter,
            )
        }
    }

    
    private fun watchFiles(ctx: Context, prefs: android.content.SharedPreferences) {
        val cfg = runCatching { XpState.refresh(prefs) }.getOrNull() ?: return
        if (!cfg.condFile) return
        val paths = cfg.condFilePaths
        if (paths.isEmpty()) return

        val mask = when (cfg.condFileEvent) {
            1 -> FileObserver.CREATE
            2 -> FileObserver.MODIFY
            3 -> FileObserver.DELETE
            else -> FileObserver.CREATE or FileObserver.MODIFY or
                FileObserver.DELETE or FileObserver.MOVED_TO or FileObserver.MOVED_FROM
        }

        paths.forEach { raw ->
            var p = raw.trim()
            if (p.isBlank()) return@forEach
            
            val f = java.io.File(p)
            val target = if (f.isDirectory) f else f
            runCatching {
                val obs = object : FileObserver(target, mask) {
                    override fun onEvent(event: Int, path: String?) {
                        if (fired) return
                        val c = runCatching { XpState.refresh(prefs) }.getOrNull() ?: return
                        if (!c.exitEnable || !c.condFile) return
                        
                        if (!f.isDirectory && path != null && path != f.name) return
                        val what = when {
                            event and FileObserver.CREATE != 0 -> "被创建"
                            event and FileObserver.MODIFY != 0 -> "被修改"
                            event and FileObserver.DELETE != 0 -> "被删除"
                            event and FileObserver.MOVED_TO != 0 -> "被移入"
                            event and FileObserver.MOVED_FROM != 0 -> "被移出"
                            else -> null
                        } ?: return
                        fire(ctx, "$p $what")
                    }
                }
                obs.startWatching()
                log("watching file: $p")
            }.onFailure { log("FileObserver 启动失败 $p: ${it.message}") }
        }
    }

    private fun fire(ctx: Context, reason: String) {
        if (fired) return
        fired = true
        log("条件达成：$reason，执行退出")
        mainHandler.post {
            runCatching {
                android.widget.Toast.makeText(
                    ctx, "已达退出条件：$reason", android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            ExitExecutor.runNow(parallel = false, beforeExit = true)
        }
    }

    
    @JvmStatic
    fun markStart() {
        lastTouchMs = System.currentTimeMillis()
    }

    
    @JvmStatic
    fun idleMs(): Long = System.currentTimeMillis() - lastTouchMs

    @Suppress("unused")
    private fun startedAt(): Long = startMs

    @Suppress("unused")
    private fun uptimeMs(): Long = System.currentTimeMillis() - startMs

    @Suppress("unused")
    private fun bootElapsed(): Long = SystemClock.elapsedRealtime()
}
