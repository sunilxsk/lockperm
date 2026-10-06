package io.github.sunilxsk.lockperm

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean











internal object ExitExecutor {

    private val activities: MutableSet<Activity> =
        Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    private val mainHandler = Handler(Looper.getMainLooper())
    private val countdownStarted = AtomicBoolean(false)
    private val killing = AtomicBoolean(false)

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var appContext: Context? = null

    

    







    private val hooksInstalled = AtomicBoolean(false)

    fun install(module: XposedModule, prefs: SharedPreferences) {
        this.prefs = prefs
        if (!hooksInstalled.compareAndSet(false, true)) return

        runCatching {
            val m = Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
            module.hook(m)
                .setPriority(io.github.libxposed.api.XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(io.github.libxposed.api.XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    val act = chain.getThisObject() as? Activity
                    if (act != null) {
                        synchronized(activities) { activities.add(act) }
                        if (appContext == null) {
                            appContext = act.applicationContext
                            maybeStartCountdown()
                            
                            val ctx = act.applicationContext
                            runCatching { ConditionExitWatcher.start(module, prefs, ctx) }
                        }
                    }
                    result
                }
        }

        
        
        runCatching {
            val m = Activity::class.java.getDeclaredMethod("onUserInteraction")
            module.hook(m).intercept { chain ->
                runCatching { ConditionExitWatcher.noteInteraction() }
                chain.proceed()
            }
        }

        runCatching {
            val m = Activity::class.java.getDeclaredMethod("onDestroy")
            module.hook(m).intercept { chain ->
                val act = chain.getThisObject() as? Activity
                if (act != null) synchronized(activities) { activities.remove(act) }
                chain.proceed()
            }
        }
    }

    

    private fun maybeStartCountdown() {
        val cfg = config() ?: return

        if (!cfg.exitEnable) return
        
        
        if (!cfg.exitCountdown) return
        val seconds = cfg.exitSeconds
        if (seconds <= 0) return
        if (!countdownStarted.compareAndSet(false, true)) return

        var remain = seconds
        val tick = object : Runnable {
            override fun run() {
                val cur = config() ?: return
                if (!cur.exitEnable) {
                    countdownStarted.set(false)
                    return
                }
                if (remain > 5) {
                    if (remain % 5 == 0 || remain == seconds) {
                        toast("将在 ${remain} 秒后按设定方案退出")
                    }
                    remain--
                    mainHandler.postDelayed(this, 1000)
                } else if (remain > 0) {
                    toast("即将退出：${remain}")
                    remain--
                    mainHandler.postDelayed(this, 1000)
                } else {
                    runNow(parallel = cur.exitParallel, beforeExit = true)
                }
            }
        }
        mainHandler.postDelayed(tick, 1000)
    }

    

    
    fun runSingle(method: String) {
        runWith(listOf(method), parallel = false)
    }

    
    fun runNow(parallel: Boolean, beforeExit: Boolean = true) {
        val cfg = config()
        val methods = if (parallel || cfg == null) {
            XpConfig.EXIT_METHOD_KEYS
        } else {
            val picked = cfg.exitMethods.toList()
            picked.ifEmpty { XpConfig.EXIT_METHOD_KEYS }
        }
        runWith(methods, parallel, beforeExit)
    }

    private fun runWith(methods: List<String>, parallel: Boolean, beforeExit: Boolean = true) {
        if (!killing.compareAndSet(false, true)) return
        val cfg = config()

        if (beforeExit && cfg != null && cfg.accEnable && cfg.accMode == 1) {
            
            AccessibilityDefender.disableAllNow()
        }
        
        
        
        
        
        val exitScheduled = cfg != null && cfg.exitEnable
        if (beforeExit && exitScheduled &&
            cfg != null && cfg.daEnable && cfg.daCloseMode == XpConfig.DA_CLOSE_BEFORE_EXIT
        ) {
            DeviceAdminDefender.removeAllNow()
        }

        val list = methods.ifEmpty { XpConfig.EXIT_METHOD_KEYS }

        Thread {
            XpState.Flags.allowExit = true
            try {
                if (parallel) runParallel(list) else runSequential(list)
            } finally {
                
                runCatching { android.os.Process.killProcess(android.os.Process.myPid()) }
                runCatching { Runtime.getRuntime().halt(0) }
            }
        }.start()
    }

    private fun runSequential(methods: List<String>) {
        methods.forEach { name ->
            runCatching { exec(name) }
            runCatching { Thread.sleep(80) }
        }
    }

    


    private fun runParallel(methods: List<String>) {
        val barrier = CountDownLatch(1)
        val threads = methods.map { name ->
            Thread {
                runCatching { barrier.await() }
                runCatching { exec(name) }
            }.apply { isDaemon = true }
        }
        threads.forEach { runCatching { it.start() } }
        barrier.countDown()          
        threads.forEach { runCatching { it.join(1200) } }
    }

    private fun exec(name: String) {
        when (name) {
            "kill" -> android.os.Process.killProcess(android.os.Process.myPid())

            "exit" -> System.exit(0)

            "halt" -> Runtime.getRuntime().halt(0)

            "signal" -> {
                runCatching {
                    android.os.Process.sendSignal(
                        android.os.Process.myPid(), android.os.Process.SIGNAL_KILL
                    )
                }
                runCatching {
                    val os = Class.forName("android.system.Os")
                    val kill = os.getDeclaredMethod(
                        "kill", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                    )
                    kill.invoke(null, android.os.Process.myPid(), 9)
                }
            }

            "npe" -> triggerNullPointerCrash()

            "exec" -> runKillCommand()

            "finish" -> {
                val list: List<Activity> = synchronized(activities) { activities.toList() }
                list.forEach { act ->
                    runCatching { act.finishAffinity() }
                    runCatching { act.finishAndRemoveTask() }
                    runCatching { act.moveTaskToBack(true) }
                }
            }
        }
    }

    






    private fun triggerNullPointerCrash() {
        mainHandler.post {
            val t: Throwable = runCatching {
                val nothing: String? = null
                nothing!!.length
                return@runCatching RuntimeException("unreachable")
            }.getOrElse { it }
            runCatching {
                val handler = Thread.getDefaultUncaughtExceptionHandler()
                if (handler != null) {
                    handler.uncaughtException(Thread.currentThread(), t)
                } else {
                    throw t
                }
            }
        }
    }

    











    private fun runKillCommand() {
        val pids = collectOwnPids()
        if (pids.isEmpty()) {
            
            runCatching { android.os.Process.killProcess(android.os.Process.myPid()) }
            return
        }
        for (pid in pids) {
            
            val ok = runCatching {
                val p = Runtime.getRuntime().exec(
                    arrayOf("/system/bin/kill", "-9", pid.toString())
                )
                runCatching { p.waitFor() }
                true
            }.getOrDefault(false)
            if (!ok) {
                
                runCatching { android.os.Process.killProcess(pid) }
                runCatching {
                    val os = Class.forName("android.system.Os")
                    val kill = os.getDeclaredMethod(
                        "kill", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                    )
                    kill.invoke(null, pid, 9)
                }
            }
        }
    }

    












    private fun collectOwnPids(): Set<Int> {
        val out = LinkedHashSet<Int>()
        val myPid = android.os.Process.myPid()
        val ctx = appContextSafe()
        val pkg = XpState.packageName.ifEmpty { ctx?.packageName ?: "" }
    
        
        runCatching {
            val c = ctx ?: return@runCatching
            val am = c.getSystemService(Context.ACTIVITY_SERVICE)
                    as? android.app.ActivityManager ?: return@runCatching
            am.runningAppProcesses?.forEach { info ->
                
                if (info.pkgList?.any { it == pkg } == true ||
                    info.processName == pkg ||
                    info.processName.startsWith("$pkg:")
                ) {
                    out.add(info.pid)
                }
            }
        }
    
        
        
        runCatching {
            if (pkg.isEmpty()) return@runCatching
            val proc = java.io.File("/proc")
            val files = proc.listFiles() ?: return@runCatching
            for (f in files) {
                val pid = f.name.toIntOrNull() ?: continue
                if (pid <= 0) continue
                val cmdline = runCatching {
                    java.io.File(f, "cmdline").readText().trim('\u0000', '\n', ' ', '\t')
                }.getOrNull() ?: continue
                
                val name = cmdline.substringBefore('\u0000').trim()
                if (name == pkg || name.startsWith("$pkg:")) {
                    out.add(pid)
                }
            }
        }
    
        
        out.add(myPid)
        return out
    }
    
    
    private fun appContextSafe(): Context? {
        appContext?.let { return it }
        return runCatching {
            val at = Class.forName("android.app.ActivityThread")
            val m = at.getDeclaredMethod("currentApplication")
            m.isAccessible = true
            m.invoke(null) as? Context
        }.getOrNull()
    }

    

    private fun config(): XpState.Snapshot? {
        val p = prefs ?: return null
        return XpState.refresh(p)
    }

    fun currentActivity(): Activity? = synchronized(activities) { activities.firstOrNull() }

    private fun toast(msg: String) {
        val ctx = appContext ?: return
        mainHandler.post {
            runCatching { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
        }
    }
}
