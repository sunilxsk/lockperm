package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import android.os.Process
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean


















internal class HookCrashBlocker(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val myPid: Int = Process.myPid()
    private val installed = mutableListOf<String>()
    private val installLock = AtomicBoolean(false)

    
    private var swUncaught = false
    private var swUi = false
    private var swSelfProc = false
    private var swExit = false
    private var swSignal = false
    private var swAm = false
    private var swTask = false
    private var swCmd = false
    private var swDebugger = false
    private var swThread = false

    private var threadPerSec = 0
    private var threadMax = 0
    private var threadRecyclePct = 20

    fun install() {
        if (!installLock.compareAndSet(false, true)) {
            logWarn("HookCrashBlocker already installed, skip")
            return
        }

        val level = snapshot().blockCrashLevel
        resolveSwitches(level)

        logInfo(
            "=== CrashBlocker start: level=$level " +
                    "uncaught=$swUncaught ui=$swUi self=$swSelfProc exit=$swExit " +
                    "signal=$swSignal am=$swAm task=$swTask cmd=$swCmd dbg=$swDebugger " +
                    "thread=$swThread(perSec=$threadPerSec max=$threadMax pct=$threadRecyclePct) " +
                    "pkg=${XpState.packageName} pid=$myPid ==="
        )

        
        if (swSelfProc) {
            safely("Process.killProcess") { hookKill("killProcess") }
            safely("Process.killProcessQuiet") { hookKill("killProcessQuiet") }
            safely("Process.killProcessGroup") { hookKillProcessGroup() }
        }

        
        if (swExit) {
            safely("System.exit") { hookSimple("java.lang.System", "exit", INT_TYPE) }
            safely("Runtime.exit") { hookSimple("java.lang.Runtime", "exit", INT_TYPE) }
            safely("Runtime.halt") { hookSimple("java.lang.Runtime", "halt", INT_TYPE) }
            safely("VMRuntime.exit") { hookVmExit() }
            safely("Os._exit") { hookOsExit() }
        }

        
        if (swSignal) {
            safely("Process.sendSignal") { hookSignal("sendSignal") }
            safely("Process.sendSignalQuiet") { hookSignal("sendSignalQuiet") }
            safely("Os.kill") { hookOsKill() }
            safely("Os.killpg") { hookOsKillpg() }
            safely("Sun.misc.Signal.raise") { hookSignalRaise() }
        }

        
        if (swAm) {
            safely("ActivityManager.killBackgroundProcesses") { hookAmKillBackground() }
            safely("ActivityManager.forceStopPackage") { hookAmForceStop() }
            safely("AMS.killBackgroundProcesses") { hookAmsKillBackground() }
            safely("AMS.forceStopPackage") { hookAmsForceStop() }
        }

        
        if (swUncaught) {
            safely("default-uncaught-handler") { hookUncaughtExceptionHandler() }
            safely("block-set-handler") { hookThreadSetHandler() }
            safely("thread-dispatch-uncaught") { hookThreadDispatchUncaught() }
            safely("runtimeinit-kill-handler") { hookRuntimeInitKillHandler() }
            safely("crashcatcher") {
                CrashCatcher(module, prefs, classLoader).install(forceIntercept = true)
            }
        }

        
        if (swUi) {
            safely("handler-dispatch") { hookHandlerDispatchMessage() }
            safely("looper-loop-guard") { hookLooperLoop() }
            safely("activitythread-main") { hookActivityThreadMain() }
            safely("ActivityThrad.H") { hookActivityThreadH() }
        }

        
        if (swTask) {
            safely("Activity.finishAffinity") { hookFinishAffinity() }
            safely("Activity.finishAndRemoveTask") { hookFinishAndRemoveTask() }
            safely("Activity.moveTaskToBack") { hookMoveTaskToBack() }
            safely("ActivityManager.moveTaskToBack") { hookAmMoveTaskToBack() }
        }

        
        if (swCmd) {
            safely("Runtime.exec") { hookRuntimeExec() }
            safely("ProcessBuilder.start") { hookProcessBuilderStart() }
        }

        if (swDebugger) {
            safely("Debug.waitForDebugger") { hookDebugWaitForDebugger() }
        }

        
        if (swThread) {
            safely("thread-guard") { hookThreadStart() }
        }

        logInfo("=== CrashBlocker done: ${installed.size} hooks $installed ===")
    }

    

    



    private fun resolveSwitches(level: Int) {
        val preset = presetFor(level)

        swUncaught = boolKey(KEY_UNCAUGHT, preset[KEY_UNCAUGHT] ?: false)
        swUi = boolKey(KEY_UI, preset[KEY_UI] ?: false)
        swSelfProc = boolKey(KEY_SELF_PROC, preset[KEY_SELF_PROC] ?: true)
        swExit = boolKey(KEY_EXIT, preset[KEY_EXIT] ?: false)
        swSignal = boolKey(KEY_SIGNAL, preset[KEY_SIGNAL] ?: false)
        swAm = boolKey(KEY_AM, preset[KEY_AM] ?: false)
        swTask = boolKey(KEY_TASK, preset[KEY_TASK] ?: false)
        swCmd = boolKey(KEY_CMD, preset[KEY_CMD] ?: false)
        swDebugger = boolKey(KEY_DEBUGGER, preset[KEY_DEBUGGER] ?: false)
        swThread = boolKey(KEY_THREAD, preset[KEY_THREAD] ?: false)

        threadPerSec = intKey(KEY_THREAD_PER_SEC, DEF_THREAD_PER_SEC).coerceAtLeast(0)
        threadMax = intKey(KEY_THREAD_MAX, DEF_THREAD_MAX).coerceAtLeast(0)
        threadRecyclePct = intKey(KEY_THREAD_PCT, DEF_THREAD_PCT).coerceIn(0, 100)
    }

    
    private fun boolKey(key: String, def: Boolean): Boolean {
        val pk = XpConfig.appKey(XpState.packageName, key)
        return runCatching {
            when {
                prefs.contains(pk) -> prefs.getBoolean(pk, def)
                prefs.contains(key) -> prefs.getBoolean(key, def)
                else -> def
            }
        }.getOrDefault(def)
    }

    private fun intKey(key: String, def: Int): Int {
        val pk = XpConfig.appKey(XpState.packageName, key)
        return runCatching {
            when {
                prefs.contains(pk) -> prefs.getInt(pk, def)
                prefs.contains(key) -> prefs.getInt(key, def)
                else -> def
            }
        }.getOrDefault(def)
    }

    private inline fun safely(name: String, block: () -> Unit) {
        try {
            block()
            installed.add(name)
        } catch (t: Throwable) {
            logWarn("hook '$name' failed: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    

    



    






    private fun hookPassthrough(method: Method, interceptor: (XposedInterface.Chain) -> Any?) {
        if (java.lang.reflect.Modifier.isAbstract(method.modifiers)) return
        if (!claimHook("HookCrashBlocker", method)) return
        runCatching {
            module.hook(method)
                .setPriority(XposedInterface.PRIORITY_HIGHEST)
                .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                .intercept(interceptor)
            true
        }.onFailure {
            logWarn("hook(passthrough) ${method.declaringClass.name}.${method.name} failed", it)
        }
    }

    private fun hookRuntimeExec() {
        val rtCls = loadClassAnywhere("java.lang.Runtime") ?: return
        val arrStr = Array<String>::class.java

        hookExecMethod(rtCls, "exec", String::class.java)
        hookExecMethod(rtCls, "exec", String::class.java, arrStr)
        hookExecMethod(rtCls, "exec", String::class.java, arrStr, File::class.java)
        hookExecMethod(rtCls, "exec", arrStr)
        hookExecMethod(rtCls, "exec", arrStr, arrStr)
        hookExecMethod(rtCls, "exec", arrStr, arrStr, File::class.java)
        logInfo("Runtime.exec all overloads hooked (passthrough)")
    }

    private fun hookExecMethod(clazz: Class<*>, name: String, vararg params: Class<*>) {
        val m = tryGetMethod(clazz, name, *params) ?: return
        makeAccessible(m)

        hookPassthrough(m) { chain ->
            if (!swCmd || XpState.Flags.allowExit) {
                chain.proceed()
            } else {
                val cmd = extractCommand(chain.args)
                if (isKillCommand(cmd)) {
                    logWarn("blocked Runtime.exec: $cmd")
                    throw SecurityException("Command blocked by LockPerm: $cmd")
                } else {
                    chain.proceed()
                }
            }
        }
    }

    private fun hookProcessBuilderStart() {
        val clazz = loadClassAnywhere("java.lang.ProcessBuilder") ?: return
        val m = tryGetMethod(clazz, "start") ?: return
        makeAccessible(m)

        hookPassthrough(m) { chain ->
            if (!swCmd || XpState.Flags.allowExit) {
                chain.proceed()
            } else {
                val pb = chain.thisObject
                val cmd = runCatching {
                    val cm = pb?.javaClass?.getMethod("command")
                    cm?.isAccessible = true
                    (cm?.invoke(pb) as? List<*>)?.filterIsInstance<String>()?.joinToString(" ")
                }.getOrNull()

                if (isKillCommand(cmd)) {
                    logWarn("blocked ProcessBuilder.start: $cmd")
                    throw SecurityException("Command blocked by LockPerm: $cmd")
                } else {
                    chain.proceed()
                }
            }
        }
    }

    private fun extractCommand(args: List<Any?>): String? {
        if (args.isEmpty()) return null
        val first = args[0] ?: return null
        return when (first) {
            is String -> first
            is Array<*> -> first.filterIsInstance<String>().joinToString(" ")
            else -> first.toString()
        }
    }

    



    private fun isKillCommand(cmd: String?): Boolean {
        if (cmd.isNullOrBlank()) return false

        val lower = cmd.lowercase().trim()
        val compact = lower.replace(Regex("\\s+"), " ")

        val dangerousTokens = setOf(
            "kill", "pkill", "killall", "killall5", "xkill",
            "reboot", "shutdown", "poweroff", "halt",
            "sigkill", "sigterm", "sigquit",
        )

        val tokens = lower
            .split(Regex("[\\s;|&()<>`]+"))
            .map { it.trim('\'', '"', '`', ',') }
            .filter { it.isNotEmpty() }

        for (t in tokens) {
            if (t in dangerousTokens) return true
        }

        val patterns = listOf(
            "am force-stop", "am kill", "am kill-all",
            "force-stop ", "kill-all",
            "kill -9", "kill -kill", "kill -sigkill", "kill -term", "kill -sigterm",
            "pkill -", "killall -",
            "/system/bin/kill", "/system/xbin/kill", "/system/bin/pkill",
            "proc/sysrq-trigger", "echo c > /proc",
            "process.killprocess", "runtime.halt", "system.exit",
        )
        return patterns.any { compact.contains(it) }
    }

    

    private val tracked = java.util.Collections.synchronizedList(
        java.util.ArrayList<WeakReference<Thread>>()
    )
    private val rateLock = Any()
    private var secStartMs = 0L
    private var secCount = 0

    
    private val guardBypass = ThreadLocal.withInitial<Boolean> { false }

    private fun hookThreadStart() {
        val threadClass = loadClassAnywhere("java.lang.Thread") ?: return
        val m = tryGetMethod(threadClass, "start") ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (!swThread || XpState.Flags.allowExit || guardBypass.get() == true) {
                chain.proceed()
            } else {
                val self = chain.thisObject as? Thread
                guardBypass.set(true)
                try {
                    
                    if (threadPerSec > 0) {
                        val now = System.currentTimeMillis()
                        val over = synchronized(rateLock) {
                            if (now - secStartMs >= 1000L) {
                                secStartMs = now
                                secCount = 0
                            }
                            secCount++
                            secCount > threadPerSec
                        }
                        if (over) {
                            logWarn("thread guard: rate limit ${threadPerSec}/s reached, blocked start")
                            return@hookMethod null
                        }
                    }

                    
                    if (threadMax > 0) {
                        val n = prune()
                        if (n >= threadMax) {
                            if (threadRecyclePct > 0) {
                                val hard = threadMax + (threadMax * threadRecyclePct / 100)
                                if (n >= hard) {
                                    val freed = recycle(threadMax)
                                    logWarn("thread guard: $n >= max*($threadRecyclePct%)=$hard, recycled $freed")
                                }
                            }
                            if (prune() >= threadMax) {
                                logWarn("thread guard: concurrent cap $threadMax reached, blocked start")
                                return@hookMethod null
                            }
                        }
                    }

                    if (self != null) {
                        tracked.add(WeakReference(self))
                        if (tracked.size > 4096) tracked.subList(0, 1024).clear()
                    }
                } catch (t: Throwable) {
                    logWarn("thread guard error, pass through", t)
                } finally {
                    guardBypass.set(false)
                }
                chain.proceed()
            }
        }
        logInfo("thread guard installed (perSec=$threadPerSec max=$threadMax pct=$threadRecyclePct)")
    }

    
    private fun prune(): Int {
        return synchronized(tracked) {
            var i = tracked.size - 1
            while (i >= 0) {
                val t = tracked[i].get()
                if (t == null || !t.isAlive) tracked.removeAt(i)
                i--
            }
            var n = 0
            for (r in tracked) if (r.get() != null) n++
            n
        }
    }

    
    private fun recycle(limit: Int): Int {
        val me = Thread.currentThread()
        var freed = 0
        synchronized(tracked) {
            var alive = 0
            for (r in tracked) if (r.get() != null) alive++

            var i = 0
            while (i < tracked.size && alive > limit) {
                val t = tracked[i].get()
                if (t == null || t === me || !t.isAlive) {
                    tracked.removeAt(i)
                    if (t != null) alive--
                    continue
                }
                runCatching {
                    t.interrupt()
                    logInfo("thread guard: interrupt '${t.name}' (${t.id})")
                }
                tracked.removeAt(i)
                alive--
                freed++
            }
        }
        return freed
    }

    

    private fun hookKill(name: String) {
        val clazz = loadClassAnywhere("android.os.Process") ?: return
        val m = tryGetMethod(clazz, name, INT_TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pid = chain.getArg(0) as? Int ?: -1
            if (isSelfPid(pid) && !XpState.Flags.allowExit) {
                logWarn("blocked Process.$name($pid)")
                null
            } else chain.proceed()
        }
    }

    private fun hookKillProcessGroup() {
        val clazz = loadClassAnywhere("android.os.Process") ?: return
        val m = tryGetMethod(clazz, "killProcessGroup", INT_TYPE, INT_TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pid = chain.getArg(1) as? Int ?: -1
            if ((isSelfPid(pid) || pid == myPid) && !XpState.Flags.allowExit) {
                logWarn("blocked Process.killProcessGroup(pid=$pid)")
                null
            } else chain.proceed()
        }
    }

    private fun hookSimple(className: String, methodName: String, vararg params: Class<*>) {
        val clazz = loadClassAnywhere(className) ?: return
        val m = tryGetMethod(clazz, methodName, *params) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked $className.$methodName(${chain.getArg(0)})")
                null
            }
        }
    }

    private fun hookSignal(name: String) {
        val clazz = loadClassAnywhere("android.os.Process") ?: return
        val m = tryGetMethod(clazz, name, INT_TYPE, INT_TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pid = chain.getArg(0) as? Int ?: -1
            val sig = chain.getArg(1) as? Int ?: 0
            if (isSelfPid(pid) && sig != 0 && !XpState.Flags.allowExit) {
                logWarn("blocked Process.$name($pid, $sig)")
                null
            } else chain.proceed()
        }
    }

    private fun hookOsKill() {
        val clazz = loadClassAnywhere("android.system.Os") ?: return
        val m = tryGetMethod(clazz, "kill", INT_TYPE, INT_TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pid = chain.getArg(0) as? Int ?: -1
            val sig = chain.getArg(1) as? Int ?: 0
            if (isSelfPid(pid) && sig != 0 && !XpState.Flags.allowExit) {
                logWarn("blocked Os.kill($pid, $sig)")
                null
            } else chain.proceed()
        }
    }

    private fun hookOsKillpg() {
        val clazz = loadClassAnywhere("android.system.Os") ?: return
        val m = tryGetMethod(clazz, "killpg", INT_TYPE, INT_TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pgid = chain.getArg(0) as? Int ?: -1
            val sig = chain.getArg(1) as? Int ?: 0
            if ((pgid == myPid || pgid == 0) && sig != 0 && !XpState.Flags.allowExit) {
                logWarn("blocked Os.killpg($pgid, $sig)")
                null
            } else chain.proceed()
        }
    }

    private fun hookOsExit() {
        val clazz = loadClassAnywhere("android.system.Os") ?: return
        val m = tryGetMethod(clazz, "_exit", INT_TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked Os._exit(${chain.getArg(0)})")
                null
            }
        }
    }

    private fun hookVmExit() {
        val clazz = loadClassAnywhere("dalvik.system.VMRuntime") ?: return
        val m = tryGetMethod(clazz, "exit", INT_TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked VMRuntime.exit(${chain.getArg(0)})")
                null
            }
        }
    }

    private fun hookSignalRaise() {
        val signalCls = loadClassAnywhere("sun.misc.Signal") ?: return
        val m = tryGetMethod(signalCls, "raise", signalCls) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked Signal.raise(${chain.getArg(0)})")
                null
            }
        }
    }

    

    private fun hookAmKillBackground() {
        val clazz = loadClassAnywhere("android.app.ActivityManager") ?: return
        val m = tryGetMethod(clazz, "killBackgroundProcesses", String::class.java) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pkg = chain.getArg(0) as? String
            if (pkg != null && pkg == XpState.packageName && !XpState.Flags.allowExit) {
                logWarn("blocked ActivityManager.killBackgroundProcesses($pkg)")
                null
            } else chain.proceed()
        }
    }

    private fun hookAmForceStop() {
        val clazz = loadClassAnywhere("android.app.ActivityManager") ?: return
        val m = tryGetMethod(clazz, "forceStopPackage", String::class.java) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pkg = chain.getArg(0) as? String
            if (pkg != null && pkg == XpState.packageName && !XpState.Flags.allowExit) {
                logWarn("blocked ActivityManager.forceStopPackage($pkg)")
                null
            } else chain.proceed()
        }
    }

    private fun hookAmsKillBackground() {
        val clazz = loadClassAnywhere("com.android.server.am.ActivityManagerService") ?: return
        val m2 = tryGetMethod(clazz, "killBackgroundProcesses", String::class.java, INT_TYPE)
        val m1 = if (m2 == null) tryGetMethod(clazz, "killBackgroundProcesses", String::class.java) else null
        val m = m2 ?: m1 ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pkg = chain.getArg(0) as? String
            if (pkg != null && pkg == XpState.packageName && !XpState.Flags.allowExit) {
                logWarn("blocked AMS.killBackgroundProcesses($pkg)")
                null
            } else chain.proceed()
        }
    }

    private fun hookAmsForceStop() {
        val clazz = loadClassAnywhere("com.android.server.am.ActivityManagerService") ?: return
        val m2 = tryGetMethod(clazz, "forceStopPackage", String::class.java, INT_TYPE)
        val m1 = if (m2 == null) tryGetMethod(clazz, "forceStopPackage", String::class.java) else null
        val m = m2 ?: m1 ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            val pkg = chain.getArg(0) as? String
            if (pkg != null && pkg == XpState.packageName && !XpState.Flags.allowExit) {
                logWarn("blocked AMS.forceStopPackage($pkg)")
                null
            } else chain.proceed()
        }
    }

    

    private fun hookUncaughtExceptionHandler() {
        val old = Thread.getDefaultUncaughtExceptionHandler()
        val ours = Thread.UncaughtExceptionHandler { t, e ->
            if (XpState.Flags.allowExit) {
                old?.uncaughtException(t, e)
                return@UncaughtExceptionHandler
            }
            logWarn("uncaught on '${t?.name}': ${e?.javaClass?.name}: ${e?.message}")
        }
        Thread.setDefaultUncaughtExceptionHandler(ours)
        logInfo("default uncaught handler installed (old=$old)")
    }

    private fun hookThreadSetHandler() {
        val threadClass = loadClassAnywhere("java.lang.Thread") ?: return
        val handlerClass = loadClassAnywhere("java.lang.Thread\$UncaughtExceptionHandler") ?: return
        val m = tryGetMethod(threadClass, "setDefaultUncaughtExceptionHandler", handlerClass) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (!XpState.Flags.allowExit && chain.getArg(0) != null) {
                logWarn("blocked setDefaultUncaughtExceptionHandler")
                null
            } else chain.proceed()
        }
    }

    private fun hookThreadDispatchUncaught() {
        val threadClass = loadClassAnywhere("java.lang.Thread") ?: return
        val m = tryGetMethod(threadClass, "dispatchUncaughtException", Throwable::class.java) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                val ex = chain.getArg(0) as? Throwable
                val th = chain.thisObject
                logWarn("thread '${(th as? Thread)?.name}' dispatchUncaught: ${ex?.javaClass?.name}: ${ex?.message}")
                null
            }
        }
    }

    private fun hookRuntimeInitKillHandler() {
        val clazz = loadClassAnywhere("com.android.internal.os.RuntimeInit\$KillApplicationHandler")
            ?: loadClassAnywhere("com.android.internal.os.RuntimeInit\$UncaughtHandler")
            ?: return
        val m = tryGetMethod(clazz, "uncaughtException", Thread::class.java, Throwable::class.java) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                val ex = chain.getArg(1) as? Throwable
                logWarn("blocked KillApplicationHandler: ${ex?.javaClass?.name}: ${ex?.message}")
                null
            }
        }
    }

    

    private fun hookHandlerDispatchMessage() {
        val handlerClass = loadClassAnywhere("android.os.Handler") ?: return
        val msgClass = loadClassAnywhere("android.os.Message") ?: return
        val m = tryGetMethod(handlerClass, "dispatchMessage", msgClass) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                try {
                    chain.proceed()
                } catch (t: Throwable) {
                    logWarn("dispatchMessage swallowed: ${t.javaClass.name}: ${t.message}")
                    null
                }
            }
        }
    }

    private fun hookLooperLoop() {
        val looperClass = loadClassAnywhere("android.os.Looper") ?: return
        val loopMethod = tryGetMethod(looperClass, "loop") ?: return
        makeAccessible(loopMethod)

        hookMethod(loopMethod) { chain ->
            var restarts = 0
            while (restarts <= MAX_LOOPER_RESTARTS) {
                try {
                    return@hookMethod chain.proceed()
                } catch (t: Throwable) {
                    if (XpState.Flags.allowExit) throw t
                    restarts++
                    logWarn("main looper swallowed #$restarts: ${t.javaClass.name}: ${t.message}")
                    try {
                        Thread.sleep(LOOPER_RESTART_DELAY_MS)
                    } catch (_: InterruptedException) {
                    }
                    if (restarts > MAX_LOOPER_RESTARTS) {
                        logWarn("looper guard gave up after $MAX_LOOPER_RESTARTS restarts")
                        return@hookMethod null
                    }
                }
            }
            null
        }
        logInfo("Looper.loop guard installed")
    }

    private fun hookActivityThreadMain() {
        val clazz = loadClassAnywhere("android.app.ActivityThread") ?: return
        val m = tryGetMethod(clazz, "main", Array<String>::class.java) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            try {
                chain.proceed()
            } catch (t: Throwable) {
                if (XpState.Flags.allowExit) throw t
                logWarn("ActivityThread.main swallowed: ${t.javaClass.name}: ${t.message}")
                null
            }
        }
    }

    private fun hookActivityThreadH() {
        val clazz = loadClassAnywhere("android.app.ActivityThread\$H") ?: return
        val msgCls = loadClassAnywhere("android.os.Message") ?: return
        val m = tryGetMethod(clazz, "handleMessage", msgCls) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else try {
                chain.proceed()
            } catch (t: Throwable) {
                logWarn("ActivityThread\$H swallowed: ${t.javaClass.name}: ${t.message}")
                null
            }
        }
    }

    private fun hookDebugWaitForDebugger() {
        val clazz = loadClassAnywhere("android.os.Debug") ?: return
        val m = tryGetMethod(clazz, "waitForDebugger") ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked Debug.waitForDebugger()")
                null
            }
        }
    }

    

    private fun hookFinishAffinity() {
        val clazz = loadClassAnywhere("android.app.Activity") ?: return
        val m = tryGetMethod(clazz, "finishAffinity") ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked Activity.finishAffinity()")
                null
            }
        }
    }

    private fun hookFinishAndRemoveTask() {
        val clazz = loadClassAnywhere("android.app.Activity") ?: return
        val m = tryGetMethod(clazz, "finishAndRemoveTask") ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked Activity.finishAndRemoveTask()")
                null
            }
        }
    }

    private fun hookMoveTaskToBack() {
        val clazz = loadClassAnywhere("android.app.Activity") ?: return
        val m = tryGetMethod(clazz, "moveTaskToBack", java.lang.Boolean.TYPE) ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked Activity.moveTaskToBack(${chain.getArg(0)})")
                false
            }
        }
    }

    private fun hookAmMoveTaskToBack() {
        val clazz = loadClassAnywhere("android.app.ActivityManager") ?: return
        val m2 = tryGetMethod(clazz, "moveTaskToBack", INT_TYPE, java.lang.Boolean.TYPE)
        val m1 = if (m2 == null) tryGetMethod(clazz, "moveTaskToBack", INT_TYPE) else null
        val m = m2 ?: m1 ?: return
        makeAccessible(m)

        hookMethod(m) { chain ->
            if (XpState.Flags.allowExit) chain.proceed()
            else {
                logWarn("blocked ActivityManager.moveTaskToBack(${chain.getArg(0)})")
                false
            }
        }
    }

    

    private fun isSelfPid(pid: Int): Boolean = pid == myPid || pid == 0

    companion object {
        private const val MAX_LOOPER_RESTARTS = 30
        private const val LOOPER_RESTART_DELAY_MS = 10L

        
        const val KEY_SELF_PROC = "crash_block_selfproc"
        const val KEY_EXIT = "crash_block_exit"
        const val KEY_SIGNAL = "crash_block_signal"
        const val KEY_AM = "crash_block_am"
        const val KEY_UNCAUGHT = "crash_block_uncaught"
        const val KEY_UI = "crash_block_ui"
        const val KEY_TASK = "crash_block_task"
        const val KEY_CMD = "crash_block_cmd"
        const val KEY_DEBUGGER = "crash_block_debugger"
        const val KEY_THREAD = "crash_block_thread"

        
        const val KEY_THREAD_PER_SEC = "crash_thread_per_sec"
        const val KEY_THREAD_MAX = "crash_thread_max"
        const val KEY_THREAD_PCT = "crash_thread_pct"

        const val DEF_THREAD_PER_SEC = 200
        const val DEF_THREAD_MAX = 800
        const val DEF_THREAD_PCT = 20

        
        fun presetFor(level: Int): Map<String, Boolean> {
            val m = mutableMapOf(
                KEY_SELF_PROC to true,
                KEY_EXIT to false,
                KEY_SIGNAL to false,
                KEY_AM to false,
                KEY_UNCAUGHT to false,
                KEY_UI to false,
                KEY_TASK to false,
                KEY_CMD to false,
                KEY_DEBUGGER to false,
                KEY_THREAD to false,
            )
            if (level >= 1) {
                m[KEY_EXIT] = true
                m[KEY_SIGNAL] = true
                m[KEY_CMD] = true
            }
            if (level >= 2) {
                m[KEY_AM] = true
                m[KEY_DEBUGGER] = true
                m[KEY_UI] = true
            }
            if (level >= 3) {
                m[KEY_UNCAUGHT] = true
                m[KEY_TASK] = true
                m[KEY_THREAD] = true
            }
            return m
        }
    }
}
