package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
















internal class ClipboardDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val READ_METHODS = setOf(
        "getPrimaryClip",
        "getPrimaryClipDescription",
        "getText",
        "hasPrimaryClip",
        "getPrimaryClipSource",
    )

    private val WRITE_METHODS = setOf(
        "setPrimaryClip",
        "setText",
        "clearPrimaryClip",
    )

    
    fun installNow() {
        XpState.Flags.forceClip = true
        install()
    }

    private fun on(): Boolean = XpState.Flags.forceClip || snapshot().clipEnable

    fun install() {
        val clazz = cls("android.content.ClipboardManager") ?: run {
            logWarn("ClipboardManager not found")
            return
        }

        clazz.declaredMethods.forEach { m ->
            when (m.name) {
                in READ_METHODS -> hookMethod(m) { chain ->
                    if (blockRead()) {
                        logWarn("blocked clipboard read: ${m.name}")
                        emptyFor(m)
                    } else {
                        chain.proceed()
                    }
                }

                in WRITE_METHODS -> hookMethod(m) { chain ->
                    if (blockWrite()) {
                        logWarn("blocked clipboard write: ${m.name}")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }
            }
        }

        logInfo("clipboard defender installed (mode=${snapshot().clipMode})")
    }

    private fun blockRead(): Boolean {
        if (!on()) return false
        if (XpState.Flags.forceClip) return true
        val cfg = snapshot()
        return cfg.clipMode == 0 || cfg.clipMode == 2
    }

    private fun blockWrite(): Boolean {
        if (!on()) return false
        if (XpState.Flags.forceClip) return true
        val cfg = snapshot()
        return cfg.clipMode == 1 || cfg.clipMode == 2
    }

    





    private fun emptyFor(m: java.lang.reflect.Method): Any? {
        if (m.name == "hasPrimaryClip") return false
        return deniedFor(m)
    }
}
