package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule










internal class HardwareSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val GL_VENDOR = 0x1F00
    private val GL_RENDERER = 0x1F01
    private val GL_VERSION = 0x1F02
    private val GL_SHADING_LANGUAGE_VERSION = 0x8B8C
    private val EGL_VENDOR = 0x3053
    private val EGL_RENDERER = 0x305D

    fun install() {
        val cfg = snapshot()
        
        val on = cfg.enableBuild
        if (!on) {
            logInfo("hardware spoofer skipped (master off)")
            return
        }

        if (cfg.exTempEnable) {
            val t = cfg.exTemp.coerceIn(0, 120)
            FakeFiles.temp = (t * 1000).toString()
        }
        if (cfg.exBatteryEnable) {
            FakeFiles.capacity = cfg.exBattery.coerceIn(0, 100).toString()
        }
        if (cfg.exTempEnable || cfg.exBatteryEnable) {
            hookBatteryManager()
            hookIntentExtras()
        }
        if (cfg.exGpu.isNotEmpty()) {
            gpuName = cfg.exGpu
            gpuVendor = cfg.exGpuVendor.ifEmpty { XpConfig.gpuVendor(cfg.exGpu) }
            gpuGlVersion = cfg.exGpuGlVersion.ifEmpty { "OpenGL ES 3.2 V@0502.0" }
            gpuGlsl = cfg.exGpuGlsl.ifEmpty { "OpenGL ES GLSL ES 3.20" }

            val lim = LinkedHashMap<Int, Int>()
            if (cfg.exGpuMaxTex > 0) {
                lim[GL_MAX_TEXTURE_SIZE] = cfg.exGpuMaxTex
                lim[GL_MAX_RENDERBUFFER_SIZE] = cfg.exGpuMaxTex
            }
            if (cfg.exGpuMaxCube > 0) lim[GL_MAX_CUBE_MAP_TEXTURE_SIZE] = cfg.exGpuMaxCube
            if (cfg.exGpuMaxLayers > 0) lim[GL_MAX_ARRAY_TEXTURE_LAYERS] = cfg.exGpuMaxLayers
            
            if (cfg.exGpuMaxTex >= 8192) {
                lim[GL_MAX_TEXTURE_IMAGE_UNITS] = 16
                lim[GL_MAX_VERTEX_ATTRIBS] = 16
            }
            glIntOverrides = lim

            hookGl()
            hookEgl()
            hookGlIntegerv()
        }
        logInfo("hardware spoofer installed (gpu=${cfg.exGpu}, temp=${cfg.exTempEnable}, batt=${cfg.exBatteryEnable})")
    }

    

    








    @Volatile
    private var gpuName = ""
    @Volatile
    private var gpuVendor = ""
    @Volatile
    private var gpuGlVersion = ""
    @Volatile
    private var gpuGlsl = ""

    
    @Volatile
    private var glIntOverrides: Map<Int, Int> = emptyMap()

    
    private val GL_MAX_TEXTURE_SIZE = 0x0D33
    private val GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C
    private val GL_MAX_RENDERBUFFER_SIZE = 0x84E8
    private val GL_MAX_ARRAY_TEXTURE_LAYERS = 0x88FF
    private val GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872
    private val GL_MAX_VERTEX_ATTRIBS = 0x8869

    private fun hookGl() {
        val names = listOf(
            "android.opengl.GLES10",
            "android.opengl.GLES10Ext",
            "android.opengl.GLES11",
            "android.opengl.GLES11Ext",
            "android.opengl.GLES20",
            "android.opengl.GLES30",
            "android.opengl.GLES31",
            "android.opengl.GLES31Ext",
            "android.opengl.GLES32",
            "android.opengl.GLES30Ext",
            "com.google.android.gles_jni.GLImpl",
            "com.google.android.gles_jni.GLImplExt",
            "javax.microedition.khronos.opengles.GL10",
            "javax.microedition.khronos.opengles.GL11",
            "javax.microedition.khronos.opengles.GL11Ext",
        )
        names.forEach { name ->
            
            val c = loadClassInit(name) ?: return@forEach
            c.declaredMethods.filter {
                it.name == "glGetString" &&
                        it.parameterTypes.size == 1 &&
                        it.parameterTypes[0] == INT_TYPE
            }.forEach { m ->
                hookMethod(m) { chain ->
                    val gpu = gpuName
                    if (gpu.isEmpty()) return@hookMethod chain.proceed()
                    when (chain.getArg(0) as? Int) {
                        GL_RENDERER -> gpu
                        GL_VENDOR -> gpuVendor
                        GL_VERSION -> gpuGlVersion
                        GL_SHADING_LANGUAGE_VERSION -> gpuGlsl
                        else -> chain.proceed()
                    }
                }
            }
        }
        logInfo("gl renderer hooked -> $gpuName")
    }

    private fun hookEgl() {
        listOf(
            "android.opengl.EGL14", "android.opengl.EGL15", "android.opengl.EGL10",
            "com.google.android.gles_jni.EGLImpl",
            "javax.microedition.khronos.egl.EGL10",
            "javax.microedition.khronos.egl.EGL11",
        ).forEach { name ->
            val c = loadClassInit(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "eglQueryString" && it.parameterTypes.size == 2 }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        val gpu = gpuName
                        if (gpu.isEmpty()) return@hookMethod chain.proceed()
                        when (chain.getArg(1) as? Int) {
                            EGL_RENDERER -> gpu
                            EGL_VENDOR -> gpuVendor.ifEmpty { XpConfig.gpuVendor(gpu) }
                            else -> chain.proceed()
                        }
                    }
                }
        }
    }

    



    private fun hookGlIntegerv() {
        if (glIntOverrides.isEmpty()) return
        listOf(
            "android.opengl.GLES10",
            "android.opengl.GLES11",
            "android.opengl.GLES20",
            "android.opengl.GLES30",
            "android.opengl.GLES31",
            "android.opengl.GLES32",
            "android.opengl.GLES30Ext",
            "com.google.android.gles_jni.GLImpl",
            "javax.microedition.khronos.opengles.GL10",
            "javax.microedition.khronos.opengles.GL11",
        ).forEach { name ->
            val c = loadClassInit(name) ?: return@forEach
            c.declaredMethods.filter {
                it.name == "glGetIntegerv" && it.parameterTypes.size == 2 &&
                        it.parameterTypes[0] == INT_TYPE
            }.forEach { m ->
                hookMethod(m) { chain ->
                    val pname = chain.getArg(0) as? Int ?: return@hookMethod chain.proceed()
                    val want = glIntOverrides[pname] ?: return@hookMethod chain.proceed()
                    chain.proceed()
                    writeIntOut(chain.getArg(1), want)
                    null
                }
            }
        }
        logInfo("gl limits hooked (${glIntOverrides.size} entries)")
    }

    
    private fun writeIntOut(target: Any?, value: Int) {
        when (target) {
            is IntArray -> if (target.isNotEmpty()) target[0] = value
            is java.nio.IntBuffer -> {
                val pos = target.position()
                if (pos < target.limit()) target.put(pos, value)
            }
        }
    }

    
    private fun loadClassInit(name: String): Class<*>? {
        runCatching { return Class.forName(name) }
        runCatching { return Class.forName(name, true, classLoader) }
        runCatching { return Class.forName(name, true, ClassLoader.getSystemClassLoader()) }
        return runCatching { Class.forName(name, true, Object::class.java.classLoader) }.getOrNull()
    }

    

    private fun hookBatteryManager() {
        val bm = loadClassAnywhere("android.os.BatteryManager") ?: return
        val capId = staticInt(bm, "BATTERY_PROPERTY_CAPACITY")
        val tempId = staticInt(bm, "BATTERY_PROPERTY_TEMPERATURE")

        bm.declaredMethods.filter {
            it.name == "getIntProperty" && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == INT_TYPE
        }.forEach { m ->
            hookMethod(m) { chain ->
                val id = chain.getArg(0) as? Int ?: return@hookMethod chain.proceed()
                val cfg = snapshot()
                when (id) {
                    capId -> if (cfg.exBatteryEnable) cfg.exBattery.coerceIn(0, 100) else chain.proceed()
                    tempId -> if (cfg.exTempEnable) cfg.exTemp.coerceIn(0, 120) * 10 else chain.proceed()
                    else -> chain.proceed()
                }
            }
        }
    }

    private fun staticInt(clazz: Class<*>, name: String): Int? =
        runCatching {
            val f = clazz.getDeclaredField(name)
            f.isAccessible = true
            f.getInt(null)
        }.getOrNull()

    



    private fun hookIntentExtras() {
        val intent = loadClassAnywhere("android.content.Intent") ?: return
        intent.declaredMethods.filter {
            it.name == "getIntExtra" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0].name == "java.lang.String"
        }.forEach { m ->
            hookMethod(m) { chain ->
                val cfg = snapshot()
                when (chain.getArg(0) as? String) {
                    "level" -> if (cfg.exBatteryEnable) cfg.exBattery.coerceIn(0, 100) else chain.proceed()
                    "scale" -> if (cfg.exBatteryEnable) 100 else chain.proceed()
                    "temperature" -> if (cfg.exTempEnable) cfg.exTemp.coerceIn(0, 120) * 10 else chain.proceed()
                    else -> chain.proceed()
                }
            }
        }
    }
}
