package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Modifier












internal object PanelToolsGuard {
    
    val inLog = ThreadLocal.withInitial { false }
}





internal class LogRecorder(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    






    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        val log = frameworkCls("android.util.Log") ?: return
        var n = 0
        log.declaredMethods
            
            .filter { m ->
                m.name in LOG_METHODS &&
                        !Modifier.isNative(m.modifiers) &&
                        !Modifier.isAbstract(m.modifiers)
            }
            .forEach { m ->
                if (hookMethod(m) { chain ->
                        
                        if (PanelToolsGuard.inLog.get() == true) {
                            return@hookMethod chain.proceed()
                        }
                        PanelToolsGuard.inLog.set(true)
                        try {
                            runCatching { record(m.name, runCatching { chain.args }.getOrNull()) }
                        } finally {
                            PanelToolsGuard.inLog.set(false)
                        }
                        chain.proceed()
                    }) n++
            }
        logInfo("log recorder installed ($n methods)")
    }

    private fun record(level: String, args: List<Any?>?) {
        val list = args ?: return
        
        val tag = list.firstOrNull { it is String }?.toString()
        val msg = list.filterIsInstance<String>().getOrNull(1)
        val lvl = when (level) {
            "v" -> "V"
            "d" -> "D"
            "i" -> "I"
            "w" -> "W"
            "e" -> "E"
            "wtf" -> "F"
            else -> level.uppercase()
        }
        ControlPanel.appendLog(2, "$lvl/${tag ?: "?"}: ${msg ?: list.joinToString(" ")}")
    }

    companion object {
        
        



        private val LOG_METHODS = setOf("v", "d", "i", "w", "e", "wtf", "println")
    }
}
