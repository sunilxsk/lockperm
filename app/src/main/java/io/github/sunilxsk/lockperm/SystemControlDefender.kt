package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule

























internal class SystemControlDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (snapshot().blockConnEnable) hookConnectivity()
        if (snapshot().blockSensor) hookSensors()
    }

    
    fun installConnOnly() {
        XpState.Flags.forceConn = true
        hookConnectivity()
    }

    
    fun installSensorOnly() {
        XpState.Flags.forceSensor = true
        hookSensors()
    }

    

    private fun connOn(sub: Boolean): Boolean {
        val cfg = snapshot()
        if (XpState.Flags.forceConn) return true
        return cfg.blockConnEnable && sub
    }

    private fun sensorOn(): Boolean {
        if (XpState.Flags.forceSensor) return true
        return snapshot().blockSensor
    }

    

    private fun hookConnectivity() {
        hookWifi()
        hookBluetooth()
        hookBrightness()
        logInfo("connectivity defender installed")
    }

    private fun hookWifi() {
        if (!connOn(snapshot().blockConnWifi)) return
        val wifi = frameworkCls("android.net.wifi.WifiManager")
        if (wifi == null) {
            logWarn("WifiManager not found")
            return
        }
        var n = 0
        wifi.declaredMethods.forEach { m ->
            val name = m.name.lowercase()
            val isToggle = name == "setwifienabled" || name == "setwifiapenabled" ||
                    name == "startlocalonlyhotspot" || name == "cancellocalonlyhotspotrequest" ||
                    (name.startsWith("setwifi") && name.contains("enabled"))
            if (!isToggle) return@forEach
            runCatching {
                hookMethod(m) { chain ->
                    if (!connOn(snapshot().blockConnWifi)) return@hookMethod chain.proceed()
                    logWarn("blocked WifiManager.${m.name}")
                    deniedFor(m)
                }
                n++
            }
        }
        if (n > 0) logInfo("wifi toggle hooked x$n")
    }

    private fun hookBluetooth() {
        if (!connOn(snapshot().blockConnBt)) return
        val names = listOf(
            "android.bluetooth.BluetoothAdapter",
            "android.bluetooth.BluetoothManager",
        )
        var n = 0
        names.forEach { clsName ->
            val c = frameworkCls(clsName) ?: return@forEach
            c.declaredMethods.filter { m ->
                val l = m.name.lowercase()
                l == "enable" || l == "disable" || l == "enableble" ||
                        (l.startsWith("set") && l.contains("bluetooth") && l.contains("enabled"))
            }.forEach { m ->
                runCatching {
                    hookMethod(m) { chain ->
                        if (!connOn(snapshot().blockConnBt)) return@hookMethod chain.proceed()
                        logWarn("blocked $clsName.${m.name}")
                        deniedFor(m)
                    }
                    n++
                }
            }
        }
        if (n > 0) logInfo("bluetooth toggle hooked x$n")
    }

    

    private val BRIGHTNESS_KEYS = setOf(
        "screen_brightness", "screen_brightness_mode",
        "screen_brightness_float", "screen_brightness_for_vr",
        "brightness", "brightness_mode", "auto_brightness",
    )

    private fun hookBrightness() {
        if (!connOn(snapshot().blockConnBright)) return

        
        listOf(
            "android.provider.Settings\$System",
            "android.provider.Settings\$Secure",
            "android.provider.Settings\$Global",
        ).forEach { name ->
            val c = frameworkCls(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "putInt" || it.name == "putString" || it.name == "putFloat" }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        val key = runCatching { chain.getArg(1)?.toString() }.getOrNull()
                        if (key != null && isBrightnessKey(key)) {
                            logWarn("blocked brightness write: $key")
                            return@hookMethod true
                        }
                        chain.proceed()
                    }
                }
        }

        
        runCatching {
            val pw = frameworkCls("com.android.internal.policy.PhoneWindow")
                ?: frameworkCls("com.android.internal.policy.DecorView")
            if (pw == null) return@runCatching
            pw.declaredMethods.filter { it.name == "setAttributes" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!connOn(snapshot().blockConnBright)) return@hookMethod chain.proceed()
                    runCatching {
                        val lp = chain.args.filterIsInstance<android.view.WindowManager.LayoutParams>()
                            .firstOrNull()
                        
                        if (lp != null && lp.screenBrightness >= 0f) {
                            lp.screenBrightness = -1f
                            logWarn("reset window brightness -> follow system")
                        }
                    }
                    chain.proceed()
                }
            }
        }

        
        runCatching {
            val wm = frameworkCls("android.view.WindowManagerImpl")
            if (wm == null) return@runCatching
            wm.declaredMethods.filter { it.name == "updateViewLayout" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!connOn(snapshot().blockConnBright)) return@hookMethod chain.proceed()
                    runCatching {
                        val lp = chain.args.filterIsInstance<android.view.WindowManager.LayoutParams>()
                            .firstOrNull()
                        if (lp != null && lp.screenBrightness >= 0f) lp.screenBrightness = -1f
                    }
                    chain.proceed()
                }
            }
        }

        logInfo("brightness defender installed")
    }

    private fun isBrightnessKey(key: String): Boolean {
        val k = key.lowercase()
        return k in BRIGHTNESS_KEYS || (k.contains("brightness"))
    }

    

    private fun hookSensors() {
        hookSensorClass("android.hardware.SensorManager")
        hookSensorClass("android.hardware.SystemSensorManager")
        logInfo("sensor defender installed")
    }

    private fun hookSensorClass(clsName: String) {
        if (!sensorOn()) return
        val c = frameworkCls(clsName) ?: return
        var n = 0
        c.declaredMethods.forEach { m ->
            val name = m.name
            when {
                
                name == "registerListener" || name == "requestTriggerSensor" ||
                        name == "flush" || name == "registerDynamicSensorCallback" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!sensorOn()) return@hookMethod chain.proceed()
                            logWarn("blocked sensor register: $clsName.$name")
                            deniedFor(m)
                        }
                        n++
                    }
                }

                
                name == "getSensorList" || name == "getDynamicSensorList" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!sensorOn()) return@hookMethod chain.proceed()
                            logWarn("sensor list -> empty")
                            java.util.Collections.emptyList<Any>()
                        }
                        n++
                    }
                }

                
                name == "getDefaultSensor" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!sensorOn()) return@hookMethod chain.proceed()
                            logWarn("default sensor -> null")
                            null
                        }
                        n++
                    }
                }
            }
        }
        if (n > 0) logInfo("sensor hooked ($clsName) x$n")
    }

}
