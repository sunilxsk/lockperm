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
    private val EGL_RENDERER = 0x305D

    fun install() {
        val cfg = snapshot()

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
            hookGl()
            hookEgl()
        }
        logInfo("hardware spoofer installed (gpu=${cfg.exGpu}, temp=${cfg.exTempEnable}, batt=${cfg.exBatteryEnable})")
    }

    

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
            val c = loadClassAnywhere(name) ?: return@forEach
            c.declaredMethods.filter {
                it.name == "glGetString" &&
                        it.parameterTypes.size == 1 &&
                        it.parameterTypes[0] == INT_TYPE
            }.forEach { m ->
                hookMethod(m) { chain ->
                    val cfg = snapshot()
                    val gpu = cfg.exGpu
                    if (gpu.isEmpty()) return@hookMethod chain.proceed()
                    when (chain.getArg(0) as? Int) {
                        GL_RENDERER -> gpu
                        GL_VENDOR -> XpConfig.gpuVendor(gpu)
                        GL_VERSION -> "OpenGL ES 3.2 V@0502.0"
                        GL_SHADING_LANGUAGE_VERSION -> "OpenGL ES GLSL ES 3.20"
                        else -> chain.proceed()
                    }
                }
            }
        }
        logInfo("gl renderer hooked")
    }

    private fun hookEgl() {
        listOf(
            "android.opengl.EGL14", "android.opengl.EGL15", "android.opengl.EGL10",
            "com.google.android.gles_jni.EGLImpl",
            "javax.microedition.khronos.egl.EGL10",
            "javax.microedition.khronos.egl.EGL11",
        ).forEach { name ->
            val c = loadClassAnywhere(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "eglQueryString" && it.parameterTypes.size == 2 }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        val cfg = snapshot()
                        val gpu = cfg.exGpu
                        if (gpu.isEmpty()) return@hookMethod chain.proceed()
                        if (chain.getArg(1) as? Int == EGL_RENDERER) gpu else chain.proceed()
                    }
                }
        }
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
