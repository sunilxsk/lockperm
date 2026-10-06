package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import kotlin.math.sqrt










internal class CameraSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val facingOf: MutableMap<Any, Int> =
        java.util.Collections.synchronizedMap(java.util.IdentityHashMap<Any, Int>())
    
    private val mapFacing: MutableMap<Any, Int> =
        java.util.Collections.synchronizedMap(java.util.IdentityHashMap<Any, Int>())

    fun install() {
        val cfg = snapshot()
        if (!cfg.enableBuild || !cfg.camEnable) {
            logInfo("camera spoof skipped (build=${cfg.enableBuild} on=${cfg.camEnable})")
            return
        }
        var n = 0
        n += hookCameraManager()
        n += hookCharacteristics()
        n += hookStreamConfigMap()
        n += hookCamera1()
        logInfo("camera spoof installed -> back ${cfg.camBackMp}MP / front ${cfg.camFrontMp}MP (hooks=$n)")
    }

    
    private fun sizeFor(mp: Int): Pair<Int, Int> {
        val safe = mp.coerceIn(1, 400)
        var ww = sqrt(safe * 1_000_000.0 * 4.0 / 3.0).toInt()
        var hh = ww * 3 / 4
        if (ww % 2 == 1) ww--
        if (hh % 2 == 1) hh--
        return ww to hh
    }

    
    private fun sizesFor(mp: Int): List<Pair<Int, Int>> {
        val (maxW, maxH) = sizeFor(mp)
        val (binW, binH) = sizeFor((mp / 4).coerceAtLeast(2))
        return listOf(
            maxW to maxH,
            binW to binH,
            (maxW / 2) to (maxH / 2),
            1920 to 1080,
            1280 to 720,
            640 to 480,
        )
    }

    private fun newSize(w: Int, h: Int): Any? =
        runCatching {
            val c = Class.forName("android.util.Size")
            val ctor = c.getDeclaredConstructor(
                Integer.TYPE, Integer.TYPE
            )
            ctor.isAccessible = true
            ctor.newInstance(w, h)
        }.getOrNull()

    private fun sizeArray(list: List<Pair<Int, Int>>): Any? {
        val cls = runCatching { Class.forName("android.util.Size") }.getOrNull() ?: return null
        val arr = java.lang.reflect.Array.newInstance(cls, list.size)
        list.forEachIndexed { i, (w, h) ->
            java.lang.reflect.Array.set(arr, i, newSize(w, h))
        }
        return arr
    }

    private fun mpFor(facing: Int?): Int {
        val c = snapshot()
        return if (facing == 0) c.camFrontMp else c.camBackMp
    }

    

    private fun hookCameraManager(): Int {
        val cm = loadClassAnywhere("android.hardware.camera2.CameraManager") ?: return 0
        val cc = loadClassAnywhere("android.hardware.camera2.CameraCharacteristics") ?: return 0
        val facingKey = runCatching {
            val f = cc.getDeclaredField("LENS_FACING")
            f.isAccessible = true
            f.get(null)
        }.getOrNull() ?: return 0
        val get = runCatching { cc.getDeclaredMethod("get", facingKey.javaClass) }.getOrNull()
            ?: return 0
        var n = 0
        cm.declaredMethods.filter {
            it.name == "getCameraCharacteristics" && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0].name == "java.lang.String"
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    runCatching {
                        if (r != null) {
                            get.isAccessible = true
                            val f = get.invoke(r, facingKey) as? Int
                            if (f != null) facingOf[r] = f
                        }
                    }
                    r
                }) n++
        }
        return n
    }

    








    private fun hookCharacteristics(): Int {
        val cc = loadClassAnywhere("android.hardware.camera2.CameraCharacteristics") ?: return 0
        var n = 0
        cc.declaredMethods.filter { it.name == "get" && it.parameterTypes.size == 1 }
            .forEach { m ->
                if (hookMethod(m) { chain ->
                        val r = chain.proceed()
                        val key = chain.getArg(0)
                        val nm = runCatching {
                            key?.javaClass?.getMethod("getName")?.invoke(key) as? String
                        }.getOrNull() ?: return@hookMethod r
                        val self = chain.getThisObject()
                        val facing = if (self != null) facingOf[self] else null
                        when (nm) {
                            "android.scaler.streamConfigurationMap" -> {
                                if (r != null && facing != null) mapFacing[r] = facing
                                r
                            }
                            "android.sensor.info.pixelArraySize",
                            "android.sensor.info.activeArraySize",
                            "android.sensor.info.preCorrectionActiveArraySize" -> {
                                val (w, h) = sizeFor(mpFor(facing))
                                rectOf(w, h) ?: r
                            }
                            else -> r
                        }
                    }) n++
            }
        return n
    }

    
    private fun rectOf(w: Int, h: Int): Any? =
        runCatching {
            val c = Class.forName("android.graphics.Rect")
            val ctor = c.getDeclaredConstructor(
                Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE
            )
            ctor.isAccessible = true
            ctor.newInstance(0, 0, w, h)
        }.getOrNull()

    private fun hookStreamConfigMap(): Int {
        val scm = loadClassAnywhere("android.hardware.camera2.params.StreamConfigurationMap")
            ?: return 0
        var n = 0
        scm.declaredMethods.filter { m ->
            m.name == "getOutputSizes" && m.parameterTypes.size == 1
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val facing = if (self != null) mapFacing[self] else null
                    val arr = sizeArray(sizesFor(mpFor(facing)))
                    arr ?: chain.proceed()
                }) n++
        }
        scm.declaredMethods.filter { m ->
            m.name == "getHighResolutionOutputSizes" && m.parameterTypes.size == 1
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val facing = if (self != null) mapFacing[self] else null
                    val (w, h) = sizeFor(mpFor(facing))
                    val arr = sizeArray(listOf(w to h))
                    arr ?: chain.proceed()
                }) n++
        }
        return n
    }

    

    




    private fun hookCamera1(): Int {
        val params = loadClassAnywhere("android.hardware.Camera\$Parameters") ?: return 0
        var n = 0
        params.declaredMethods.filter {
            (it.name == "getSupportedPictureSizes" || it.name == "getSupportedPreviewSizes") &&
                    it.parameterTypes.isEmpty()
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    runCatching {
                        val list = r as? List<*> ?: return@runCatching
                        if (list.isEmpty()) return@runCatching
                        
                        if (m.name == "getSupportedPreviewSizes") return@runCatching
                        var best: Any? = null
                        var bestArea = -1
                        for (it2 in list) {
                            val o = it2 ?: continue
                            val ww = intField(o, "width")
                            val hh = intField(o, "height")
                            if (ww <= 0 || hh <= 0) continue
                            if (ww * hh > bestArea) {
                                bestArea = ww * hh
                                best = o
                            }
                        }
                        val o = best ?: return@runCatching
                        val (fw, fh) = sizeFor(snapshot().camBackMp)
                        setIntField(o, "width", fw)
                        setIntField(o, "height", fh)
                    }
                    r
                }) n++
        }
        params.declaredMethods.filter { it.name == "getPictureSize" && it.parameterTypes.isEmpty() }
            .forEach { m ->
                if (hookMethod(m) { chain ->
                        val r = chain.proceed()
                        runCatching {
                            if (r != null) {
                                val (fw, fh) = sizeFor(snapshot().camBackMp)
                                setIntField(r, "width", fw)
                                setIntField(r, "height", fh)
                            }
                        }
                        r
                    }) n++
            }
        return n
    }

    private fun intField(obj: Any, name: String): Int =
        runCatching {
            val f = obj.javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.getInt(obj)
        }.getOrDefault(0)

    private fun setIntField(obj: Any, name: String, v: Int) {
        runCatching {
            val f = obj.javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.setInt(obj, v)
        }
    }
}
