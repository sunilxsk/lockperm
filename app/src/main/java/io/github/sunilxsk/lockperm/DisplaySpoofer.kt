package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule














internal class DisplaySpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val modeRate: MutableMap<Any, Float> =
        java.util.Collections.synchronizedMap(java.util.IdentityHashMap<Any, Float>())

    fun install() {
        val cfg = snapshot()
        if (!cfg.dispEnable) {
            logInfo("display spoof skipped (off)")
            return
        }
        var n = 0
        n += hookMetrics()
        n += hookSize()
        n += hookRefreshRate()
        n += hookModes()
        logInfo("display spoof installed -> ${cfg.resW}x${cfg.resH} @${cfg.refreshHz}Hz (hooks=$n)")
    }

    private fun rates(): FloatArray {
        val c = snapshot()
        val list = c.refreshList.split(",", "，", " ", "\n")
            .mapNotNull { it.trim().toFloatOrNull() }
            .filter { it > 0f }
        return if (list.isEmpty()) floatArrayOf(c.refreshHz) else list.toFloatArray()
    }

    private fun w(): Int = snapshot().resW.coerceIn(240, 7680)
    private fun h(): Int = snapshot().resH.coerceIn(240, 7680)

    

    private fun hookMetrics(): Int {
        var n = 0
        
        val disp = loadClassAnywhere("android.view.Display") ?: return 0
        disp.declaredMethods.filter { m ->
            (m.name == "getMetrics" || m.name == "getRealMetrics" ||
                    m.name == "getCurrentMetrics") &&
                    m.parameterTypes.size == 1 &&
                    m.parameterTypes[0].name == "android.util.DisplayMetrics"
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    applyMetrics(chain.getArg(0))
                    r
                }) n++
        }
        
        val res = loadClassAnywhere("android.content.res.Resources")
        res?.declaredMethods?.filter { it.name == "getDisplayMetrics" && it.parameterTypes.isEmpty() }
            ?.forEach { m ->
                if (hookMethod(m) { chain ->
                        val r = chain.proceed()
                        applyMetrics(r)
                        r
                    }) n++
            }
        return n
    }

    private fun applyMetrics(dm: Any?) {
        if (dm == null) return
        runCatching {
            val fw = dm.javaClass.getDeclaredField("widthPixels")
            fw.isAccessible = true
            fw.setInt(dm, w())
        }
        runCatching {
            val fh = dm.javaClass.getDeclaredField("heightPixels")
            fh.isAccessible = true
            fh.setInt(dm, h())
        }
    }

    

    private fun hookSize(): Int {
        val disp = loadClassAnywhere("android.view.Display") ?: return 0
        var n = 0
        disp.declaredMethods.filter { m ->
            (m.name == "getSize" || m.name == "getRealSize") &&
                    m.parameterTypes.size == 1 &&
                    m.parameterTypes[0].name == "android.graphics.Point"
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    
                    val r = chain.proceed()
                    val p = chain.getArg(0) ?: return@hookMethod r
                    runCatching {
                        val fx = p.javaClass.getDeclaredField("x")
                        fx.isAccessible = true
                        fx.setInt(p, w())
                        val fy = p.javaClass.getDeclaredField("y")
                        fy.isAccessible = true
                        fy.setInt(p, h())
                    }
                    r
                }) n++
        }
        return n
    }

    

    private fun hookRefreshRate(): Int {
        val disp = loadClassAnywhere("android.view.Display") ?: return 0
        var n = 0
        disp.declaredMethods.filter { it.name == "getRefreshRate" && it.parameterTypes.isEmpty() }
            .forEach { m ->
                if (hookMethod(m) { _ -> snapshot().refreshHz }) n++
            }
        return n
    }

    

    private fun hookModes(): Int {
        val disp = loadClassAnywhere("android.view.Display") ?: return 0
        val mode = loadClassAnywhere("android.view.Display\$Mode")
        var n = 0

        
        disp.declaredMethods.filter { it.name == "getSupportedModes" && it.parameterTypes.isEmpty() }
            .forEach { m ->
                if (hookMethod(m) { chain ->
                        val raw = chain.proceed() ?: return@hookMethod null
                        val r = raw as? Array<*> ?: return@hookMethod raw
                        val want = rates()
                        val size = minOf(r.size, want.size)
                        if (size <= 0) return@hookMethod r
                        for (i in 0 until size) {
                            val mo = r[i] ?: continue
                            modeRate[mo] = want[i]
                        }
                        
                        
                        val out = java.lang.reflect.Array.newInstance(
                            r.javaClass.componentType, size
                        )
                        for (i in 0 until size) java.lang.reflect.Array.set(out, i, r[i])
                        out
                    }) n++
            }

        if (mode != null) {
            mode.declaredMethods.filter { it.name == "getRefreshRate" && it.parameterTypes.isEmpty() }
                .forEach { m ->
                    if (hookMethod(m) { chain ->
                            val self = chain.getThisObject()
                            (if (self != null) modeRate[self] else null) ?: snapshot().refreshHz
                        }) n++
                }
            mode.declaredMethods.filter { it.name == "getPhysicalWidth" && it.parameterTypes.isEmpty() }
                .forEach { m -> if (hookMethod(m) { _ -> w() }) n++ }
            mode.declaredMethods.filter { it.name == "getPhysicalHeight" && it.parameterTypes.isEmpty() }
                .forEach { m -> if (hookMethod(m) { _ -> h() }) n++ }
        }
        return n
    }
}
