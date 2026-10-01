package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule


















internal class TorchVibrateDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun installNow() {
        XpState.Flags.forceTorchVibrate = true
        install()
    }

    private fun torchOn(): Boolean =
        XpState.Flags.forceTorchVibrate || snapshot().blockTorch

    private fun vibrateOn(): Boolean =
        XpState.Flags.forceTorchVibrate || snapshot().blockVibrate

    fun install() {
        if (torchOn()) hookTorch()
        if (vibrateOn()) hookVibrate()
    }

    

    









    private fun hookTorch() {
        hookTorchCamera2()
        hookTorchCamera1()
        hookTorchCaptureRequest()
        hookVendorTorch()
    }

    
    private fun hookTorchCamera2() {
        val cm = frameworkCls("android.hardware.camera2.CameraManager") ?: run {
            logWarn("torch: CameraManager not found")
            return
        }
        val hits = cm.declaredMethods.filter { it.name == "setTorchMode" }
        if (hits.isEmpty()) {
            logWarn("torch: setTorchMode not found on CameraManager")
            return
        }
        hits.forEach { m ->
            hookMethod(m) { chain ->
                
                val on = chain.args.filterIsInstance<Boolean>().firstOrNull()
                if (on == false) return@hookMethod chain.proceed()
                logWarn("blocked torch: CameraManager.setTorchMode")
                deniedFor(m)
            }
        }
        logInfo("torch hook: CameraManager.setTorchMode x${hits.size}")
    }

    
    private fun hookTorchCamera1() {
        val params = frameworkCls("android.hardware.Camera\$Parameters")
        if (params != null) {
            params.declaredMethods.filter { it.name == "setFlashMode" }.forEach { m ->
                hookMethod(m) { chain ->
                    val mode = chain.args.filterIsInstance<String>().firstOrNull()
                    if (isTorchMode(mode)) {
                        logWarn("blocked torch: Parameters.setFlashMode($mode)")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }
            }
            logInfo("torch hook: Camera.Parameters.setFlashMode")
        } else {
            logWarn("torch: Camera\$Parameters not found")
        }

        val camera = frameworkCls("android.hardware.Camera")
        if (camera != null) {
            camera.declaredMethods.filter { it.name == "setParameters" }.forEach { m ->
                hookMethod(m) { chain ->
                    val p = chain.args.firstOrNull { it?.javaClass?.name?.contains("Parameters") == true }
                    val mode = runCatching { p?.javaClass?.getMethod("getFlashMode")?.invoke(p) as? String }.getOrNull()
                    if (isTorchMode(mode)) {
                        logWarn("blocked torch: Camera.setParameters(flash=$mode)")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }
            }
            logInfo("torch hook: Camera.setParameters")
        }
    }

    
    private fun hookTorchCaptureRequest() {
        val builder = frameworkCls("android.hardware.camera2.CaptureRequest\$Builder")
        if (builder != null) {
            builder.declaredMethods.filter { it.name == "set" }.forEach { m ->
                hookMethod(m) { chain ->
                    val key = chain.args.firstOrNull()
                    val keyName = runCatching {
                        key?.javaClass?.getMethod("getName")?.invoke(key) as? String
                    }.getOrNull()
                    if (keyName != null && "flash" in keyName.lowercase()) {
                        val v = chain.args.getOrNull(1)
                        val num = (v as? Number)?.toInt()
                        
                        if (num == 2) {
                            logWarn("blocked torch: CaptureRequest.Builder.set($keyName, TORCH)")
                            return@hookMethod deniedFor(m)
                        }
                    }
                    chain.proceed()
                }
            }
        }
    }

    private fun isTorchMode(mode: String?): Boolean {
        if (mode == null) return true
        val m = mode.uppercase()
        return "TORCH" in m || m == "ON"
    }

    



    private fun hookVendorTorch() {
        val names = setOf(
            "setFlashlightEnabled", "setTorchEnabled", "setFlashlight", "setTorch",
            "turnOnFlashlight", "turnOnFlashLight", "openFlashlight", "openFlash",
            "enableFlashlight", "enableTorch", "setFlashlightOn", "setTorchOn",
        )
        listOf(
            "android.hardware.camera2.CameraManager",
            "android.hardware.Camera",
            "android.app.SystemServiceRegistry",
        ).forEach { className ->
            val c = frameworkCls(className) ?: return@forEach
            c.declaredMethods.filter { it.name in names }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked torch: ${m.name} (vendor)")
                    deniedFor(m)
                }
            }
        }
    }

    

    private fun hookVibrate() {
        runCatching {
            val v = frameworkCls("android.os.Vibrator") ?: return@runCatching
            v.declaredMethods
                .filter { it.name == "vibrate" || it.name == "cancel" }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        logWarn("blocked Vibrator.${m.name}")
                        deniedFor(m)
                    }
                }
            logInfo("vibrate hook: Vibrator")
        }

        
        runCatching {
            val vm = frameworkCls("android.os.VibratorManager") ?: return@runCatching
            vm.declaredMethods.filter { it.name == "vibrate" || it.name == "cancel" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked VibratorManager.${m.name}")
                    deniedFor(m)
                }
            }
            logInfo("vibrate hook: VibratorManager")
        }

        
        runCatching {
            val svc = frameworkCls("android.os.SystemVibrator") ?: return@runCatching
            svc.declaredMethods.filter { it.name == "vibrate" || it.name == "cancel" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked SystemVibrator.${m.name}")
                    deniedFor(m)
                }
            }
        }
    }

}
