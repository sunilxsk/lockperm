package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule














internal class DpiSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg = snapshot()
        val dpi = cfg.dpiValue
        if (!cfg.dpiEnable || dpi <= 0) {
            logInfo("dpi spoofer skipped (enable=${cfg.dpiEnable} dpi=$dpi)")
            return
        }

        var n = 0
        n += hookResourcesMetrics()
        n += hookDisplayMetrics()
        logInfo("dpi spoofer installed -> $dpi (hooks=$n)")
    }

    
    private fun targetDpi(): Int {
        val cfg = snapshot()
        if (!cfg.dpiEnable) return 0
        val v = cfg.dpiValue
        return if (v in XpConfig.MIN_DPI..XpConfig.MAX_DPI) v else 0
    }

    






    private fun applyMetrics(dm: Any?): Boolean {
        val dpi = targetDpi()
        if (dm == null || dpi <= 0) return false
        return runCatching {
            val before = getInt(dm, "densityDpi")
            val density = dpi / 160f
            setInt(dm, "densityDpi", dpi)
            setFloat(dm, "density", density)
            
            
            val fs = getFloat(dm, "scaledDensity")
            val base = if (before > 0) before / 160f else density
            val ratio = if (base > 0f) fs / base else 1f
            setFloat(dm, "scaledDensity", density * if (ratio > 0f) ratio else 1f)
            setFloat(dm, "xdpi", dpi.toFloat())
            setFloat(dm, "ydpi", dpi.toFloat())
            true
        }.getOrDefault(false)
    }

    
    private fun applyConfig(cfgObj: Any?): Boolean {
        val dpi = targetDpi()
        if (cfgObj == null || dpi <= 0) return false
        return runCatching {
            val before = getInt(cfgObj, "densityDpi").takeIf { it > 0 } ?: 160
            setInt(cfgObj, "densityDpi", dpi)
            
            val w = getInt(cfgObj, "screenWidthDp")
            val h = getInt(cfgObj, "screenHeightDp")
            if (w > 0) setInt(cfgObj, "screenWidthDp", (w * before / dpi))
            if (h > 0) setInt(cfgObj, "screenHeightDp", (h * before / dpi))
            true
        }.getOrDefault(false)
    }

    

    private fun hookResourcesMetrics(): Int {
        val res = loadClassAnywhere("android.content.res.Resources") ?: return 0
        var n = 0
        
        
        res.declaredMethods.filter {
            it.name == "getDisplayMetrics" && it.parameterTypes.isEmpty()
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    applyMetrics(r)
                    r
                }) n++
        }
        res.declaredMethods.filter {
            it.name == "getConfiguration" && it.parameterTypes.isEmpty()
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    applyConfig(r)
                    r
                }) n++
        }
        
        res.declaredMethods.filter { it.name == "updateConfiguration" }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val idx = m.parameterTypes.indexOfFirst { it.name == "android.content.res.Configuration" }
                    if (idx >= 0) applyConfig(chain.getArg(idx))
                    
                    for (i in m.parameterTypes.indices) {
                        if (m.parameterTypes[i].name == "android.util.DisplayMetrics") {
                            applyMetrics(chain.getArg(i))
                        }
                    }
                    chain.proceed()
                }) n++
        }
        return n
    }

    

    private fun hookDisplayMetrics(): Int {
        val disp = loadClassAnywhere("android.view.Display") ?: return 0
        var n = 0
        disp.declaredMethods.filter {
            (it.name == "getMetrics" || it.name == "getRealMetrics" ||
                    it.name == "getCurrentMetrics") &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0].name == "android.util.DisplayMetrics"
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    applyMetrics(chain.getArg(0))
                    r
                }) n++
        }
        return n
    }

    

    private fun getInt(obj: Any, name: String): Int =
        runCatching {
            val f = obj.javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.getInt(obj)
        }.getOrDefault(0)

    private fun setInt(obj: Any, name: String, v: Int) {
        runCatching {
            val f = obj.javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.setInt(obj, v)
        }
    }

    private fun getFloat(obj: Any, name: String): Float =
        runCatching {
            val f = obj.javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.getFloat(obj)
        }.getOrDefault(0f)

    private fun setFloat(obj: Any, name: String, v: Float) {
        runCatching {
            val f = obj.javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.setFloat(obj, v)
        }
    }
}
