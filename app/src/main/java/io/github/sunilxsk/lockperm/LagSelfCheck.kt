package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean


















internal class LagSelfCheck(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val startAt: Long = System.currentTimeMillis() + WARMUP_MS

    private val fired = AtomicBoolean(false)

    fun install() {
        val log = frameworkCls("android.util.Log") ?: return
        val methods = log.declaredMethods.filter { m ->
            m.name in WATCH_LEVELS &&
                !Modifier.isNative(m.modifiers) &&
                !Modifier.isAbstract(m.modifiers)
        }
        if (methods.isEmpty()) return

        var n = 0
        methods.forEach { m ->
            if (hookMethod(m) { chain ->
                inspect(m.name, chain)
                chain.proceed()
            }) n++
        }
        logInfo("lag self-check installed ($n methods, threshold=$THRESHOLD frames)")
    }

    private fun inspect(level: String, chain: io.github.libxposed.api.XposedInterface.Chain) {
        
        if (fired.get()) return
        val now = System.currentTimeMillis()
        if (now < startAt) return

        val args = runCatching { chain.args }.getOrNull() ?: return
        
        
        for (a in args) {
            val s = a as? String ?: continue
            val n = frameCountOf(s) ?: continue
            if (n < THRESHOLD) continue
            if (!fired.compareAndSet(false, true)) return
            logInfo("lag detected: $n frames (from Log.$level)")
            toast(
                "检测到明显卡顿：单次掉帧 $n 帧。" +
                    "若影响使用，可尝试关闭部分防护功能后对比。"
            )
            return
        }
    }

    



    private fun frameCountOf(s: String): Int? {
        if (!s.startsWith("Skipped ")) return null
        val sp = s.indexOf(' ')
        if (sp <= 0) return null
        var i = sp + 1
        var v = 0
        var digits = 0
        while (i < s.length && digits < 8) {
            val c = s[i]
            if (c < '0' || c > '9') break
            v = v * 10 + (c - '0')
            digits++
            i++
        }
        if (digits == 0) return null
        
        if (!s.regionMatches(i, " frames", 0, 7, ignoreCase = false)) return null
        return v
    }

    companion object {
        private const val THRESHOLD = 100
        private const val WARMUP_MS = 5000L

        
        private val WATCH_LEVELS = setOf("i", "w")
    }
}
