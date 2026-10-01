package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
















internal class CommandBlocker(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        hookRuntimeExec()
        hookProcessBuilder()
        hookProcessImpl()
        logInfo("command blocker installed")
    }

    
    private fun shouldBlock(): Boolean {
        if (XpState.Flags.allowExit) return false
        return XpState.Flags.forceExec || snapshot().blockExec
    }

    fun installNow() {
        XpState.Flags.forceExec = true
        install()
    }

    private fun hookRuntimeExec() {
        val runtime = runCatching { Class.forName("java.lang.Runtime") }.getOrNull() ?: return
        runtime.declaredMethods.filter { it.name == "exec" }.forEach { m ->
            hookMethod(m) { chain ->
                if (shouldBlock()) {
                    val cmd = describe(chain)
                    logWarn("blocked Runtime.exec($cmd)")
                    notifyBlocked(cmd)
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    





    private fun notifyBlocked(cmd: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastAt.get() < TOAST_THROTTLE_MS) return
        lastToastAt.set(now)
        val shown = if (cmd.isBlank()) "没获取到命令，已直接拦截" else "检测到了命令: ${cmd.take(60)} 拦截"
        toast(shown)
    }

    private fun hookProcessBuilder() {
        val pb = runCatching { Class.forName("java.lang.ProcessBuilder") }.getOrNull() ?: return
        pb.declaredMethods.filter { it.name == "start" }.forEach { m ->
            hookMethod(m) { chain ->
                if (shouldBlock()) {
                    logWarn("blocked ProcessBuilder.start()")
                    notifyBlocked(cmdOfBuilder(chain))
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    
    private fun hookProcessImpl() {
        val impl = runCatching { Class.forName("java.lang.ProcessImpl") }.getOrNull() ?: return
        impl.declaredMethods.filter { it.name == "start" }.forEach { m ->
            hookMethod(m) { chain ->
                if (shouldBlock()) {
                    logWarn("blocked ProcessImpl.start()")
                    notifyBlocked("")
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    
    private fun cmdOfBuilder(chain: io.github.libxposed.api.XposedInterface.Chain): String {
        val th = runCatching { chain.thisObject }.getOrNull() ?: return ""
        return runCatching {
            val f = th.javaClass.getDeclaredField("command")
            f.isAccessible = true
            val v = f.get(th)
            (v as? List<*>)?.filterIsInstance<String>()?.joinToString(" ") ?: v?.toString() ?: ""
        }.getOrDefault("")
    }

    
    private fun describe(chain: io.github.libxposed.api.XposedInterface.Chain): String {
        val args = runCatching { chain.args }.getOrNull() ?: return ""
        return args.joinToString(", ") { a ->
            when (a) {
                null -> "null"
                is Array<*> -> a.filterIsInstance<String>().joinToString(" ")
                else -> a.toString()
            }
        }.take(120)
    }

    companion object {
        
        private const val TOAST_THROTTLE_MS = 10_000L
        private val lastToastAt = java.util.concurrent.atomic.AtomicLong(0L)
    }
}
