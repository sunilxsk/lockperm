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
        var n = 0

        
        runCatching {
            val wifi = frameworkCls("android.net.wifi.WifiManager")
            if (wifi == null) {
                logWarn("WifiManager not found")
                return@runCatching
            }
            wifi.declaredMethods.forEach { m ->
                if (isWifiControl(m.name)) {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!connOn(snapshot().blockConnWifi)) return@hookMethod chain.proceed()
                            logWarn("blocked WifiManager.${m.name}")
                            deniedFor(m)
                        }
                        n++
                    }
                }
            }
        }

        
        runCatching {
            val wifi = frameworkCls("android.net.wifi.WifiManager") ?: return@runCatching
            wifi.declaredMethods.forEach { m ->
                val nm = m.name
                if (nm.endsWith("Hidden") || nm.endsWith("hidden")) {
                    val base = nm.substring(0, nm.length - 6)
                    if (isWifiControl(base) || isWifiControl(nm)) {
                        runCatching {
                            hookMethod(m) { chain ->
                                if (!connOn(snapshot().blockConnWifi)) return@hookMethod chain.proceed()
                                logWarn("blocked WifiManager.$nm")
                                deniedFor(m)
                            }
                            n++
                        }
                    }
                }
            }
        }

        
        runCatching {
            val cm = frameworkCls("android.net.ConnectivityManager") ?: return@runCatching
            val names = setOf(
                "teardownNetwork", "reportNetworkConnectivity",
                "requestNetwork", "setNetworkPreference",
                "setProcessDefaultNetwork", "bindProcessToNetwork",
                "startUsingNetworkFeature", "stopUsingNetworkFeature",
            )
            cm.declaredMethods.filter { it.name in names }.forEach { m ->
                runCatching {
                    hookMethod(m) { chain ->
                        if (!connOn(snapshot().blockConnWifi)) return@hookMethod chain.proceed()
                        logWarn("blocked ConnectivityManager.${m.name}")
                        deniedFor(m)
                    }
                    n++
                }
            }
        }

        
        runCatching {
            val nm = frameworkCls("android.net.wifi.WifiNative") ?: return@runCatching
            val names = setOf(
                "disconnect", "reconnect", "reassociate", "removeNetwork",
                "enableNetwork", "disableNetwork", "selectNetwork", "setNetworkVariable",
            )
            nm.declaredMethods.filter { it.name in names }.forEach { m ->
                runCatching {
                    hookMethod(m) { chain ->
                        if (!connOn(snapshot().blockConnWifi)) return@hookMethod chain.proceed()
                        logWarn("blocked WifiNative.${m.name}")
                        deniedFor(m)
                    }
                    n++
                }
            }
        }

        if (n > 0) logInfo("wifi hooked x$n")
        else logWarn("wifi: 没有匹配到任何方法，可能被 ROM 改名了")
    }

    
    private fun isWifiControl(raw: String): Boolean {
        val l = raw.lowercase().trimEnd('_')
        if (l.isEmpty()) return false

        
        when (l) {
            "disconnect", "reconnect", "reassociate" -> return true
            "forget", "forgetnetwork", "removenetworksuggestions" -> return true
            "addnetworksuggestions", "removesuggestion" -> return true
            "startscan", "startscanactive" -> return false
            "enable", "disable" -> return false
            else -> Unit
        }

        
        val related = l.contains("wifi") || l.contains("network") || l.contains("hotspot") ||
            l.contains("softap") || l.contains("suggestion") || l.contains("wificonfig") ||
            l.contains("supplicant")
        if (!related) return false

        return when {
            
            l == "setwifienabled" || l == "setwifiapenabled" ||
                l == "setsoftapenabled" || l == "setwifiautoconnect" ||
                l == "enablewifi" || l == "disablewifi" -> true
            l.startsWith("setwifi") && l.contains("enabled") -> true
            
            l == "disconnect" || l == "reconnect" || l == "reassociate" -> true
            l.contains("disconnect") -> true
            
            l == "forget" || l == "removenetwork" || l == "addnetwork" ||
                l == "updatenetwork" || l == "savenetwork" || l == "addorupdatenetwork" -> true
            l.contains("removenetwork") || l.contains("forget") -> true
            l.contains("suggestion") && (l.startsWith("add") || l.startsWith("remove")) -> true
            l == "enablenetwork" || l == "disablenetwork" || l == "selectnetwork" -> true
            l.contains("enablenetwork") || l.contains("disablenetwork") -> true
            l == "setnetworkvariable" -> true
            
            l == "startsoftap" || l == "stopsoftap" || l == "setsoftapconfiguration" -> true
            l == "startlocalonlyhotspot" || l == "cancellocalonlyhotspotrequest" -> true
            l.contains("hotspot") && (l.startsWith("start") || l.startsWith("cancel") ||
                l.startsWith("stop")) -> true
            else -> false
        }
    }

    
    
    
    private fun hookBluetooth() {
        if (!connOn(snapshot().blockConnBt)) return
        var n = 0

        runCatching {
            val names = listOf(
                "android.bluetooth.BluetoothAdapter",
                "android.bluetooth.BluetoothManager",
                "android.bluetooth.BluetoothPan",
                "android.bluetooth.BluetoothA2dp",
                "android.bluetooth.BluetoothHeadset",
                "android.bluetooth.BluetoothDevice",
            )
            names.forEach { clsName ->
                val c = frameworkCls(clsName) ?: return@forEach
                c.declaredMethods.forEach { m ->
                    if (isBtControl(m.name)) {
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
            }
        }

        if (n > 0) logInfo("bluetooth hooked x$n")
        else logWarn("bluetooth: 没有匹配到任何方法，可能被 ROM 改名了")
    }

    private fun isBtControl(raw: String): Boolean {
        val l = raw.lowercase()
        return when {
            l == "enable" || l == "disable" || l == "enableble" || l == "disableble" -> true
            l == "enablebluetooth" || l == "disablebluetooth" -> true
            l.startsWith("setbluetooth") && l.contains("enabled") -> true
            l == "setname" || l == "setdiscoverabletimeout" ||
                l == "setscanmode" || l == "setdiscoverable" -> true
            l == "startdiscovery" || l == "canceldiscovery" ||
                l == "startdiscoverable" || l == "stopdiscoverable" -> true
            l == "createbond" || l == "removebond" || l == "cancelbondprocess" -> true
            l == "connect" || l == "disconnect" -> true
            else -> false
        }
    }

    
    
    
    private fun hookBrightness() {
        if (!connOn(snapshot().blockConnBright)) return

        
        listOf(
            "android.provider.Settings\$System",
            "android.provider.Settings\$Secure",
            "android.provider.Settings\$Global",
        ).forEach { name ->
            val c = frameworkCls(name) ?: return@forEach
            c.declaredMethods.filter { m ->
                m.name.startsWith("put") || m.name == "setSetting" ||
                    m.name == "putStringForUser" || m.name == "putIntForUser" ||
                    m.name == "putFloatForUser"
            }.forEach { m ->
                runCatching {
                    hookMethod(m) { chain ->
                        if (!connOn(snapshot().blockConnBright)) return@hookMethod chain.proceed()
                        val key = runCatching { chain.getArg(1)?.toString() }.getOrNull()
                        if (key != null && isBrightnessKey(key)) {
                            logWarn("blocked brightness write: $key (${m.name})")
                            return@hookMethod true
                        }
                        chain.proceed()
                    }
                }
            }
        }

        
        runCatching {
            val win = frameworkCls("android.view.Window") ?: return@runCatching
            win.declaredMethods.filter { it.name == "setAttributes" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!connOn(snapshot().blockConnBright)) return@hookMethod chain.proceed()
                    runCatching { resetBrightness(chain.args) }
                    chain.proceed()
                }
            }
        }
        runCatching {
            listOf(
                "com.android.internal.policy.PhoneWindow",
                "com.android.internal.policy.DecorView",
            ).forEach { cn ->
                val c = frameworkCls(cn) ?: return@forEach
                c.declaredMethods.filter { it.name == "setAttributes" }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!connOn(snapshot().blockConnBright)) return@hookMethod chain.proceed()
                        runCatching { resetBrightness(chain.args) }
                        chain.proceed()
                    }
                }
            }
        }

        
        runCatching {
            listOf(
                "android.view.WindowManagerImpl",
                "android.view.WindowManagerGlobal",
            ).forEach { cn ->
                val c = frameworkCls(cn) ?: return@forEach
                c.declaredMethods.filter {
                    it.name == "updateViewLayout" || it.name == "addView"
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!connOn(snapshot().blockConnBright)) return@hookMethod chain.proceed()
                        runCatching { resetBrightness(chain.args) }
                        chain.proceed()
                    }
                }
            }
        }

        
        runCatching {
            val lp = frameworkCls("android.view.WindowManager\$LayoutParams")
                ?: return@runCatching
            lp.declaredFields.filter { it.name == "screenBrightness" }.forEach { f ->
                f.isAccessible = true
            }
        }

        
        runCatching {
            val pm = frameworkCls("android.os.PowerManager") ?: return@runCatching
            pm.declaredMethods.filter {
                it.name == "setBacklightBrightness" || it.name == "setScreenBrightness" ||
                    it.name == "setTemporaryScreenBrightnessSettingOverride"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!connOn(snapshot().blockConnBright)) return@hookMethod chain.proceed()
                    logWarn("blocked PowerManager.${m.name}")
                    deniedFor(m)
                }
            }
        }

        logInfo("brightness defender installed")
    }

    private fun resetBrightness(args: List<Any?>?) {
        if (args == null) return
        args.filterIsInstance<android.view.WindowManager.LayoutParams>().forEach { lp ->
            if (lp.screenBrightness >= 0f) {
                lp.screenBrightness = -1f
                logWarn("reset window brightness -> follow system")
            }
        }
    }

    private fun isBrightnessKey(key: String): Boolean {
        val k = key.lowercase()
        return k.contains("brightness") || k.contains("backlight")
    }

    

    private fun hookSensors() {
        hookSensorClass("android.hardware.SensorManager")
        hookSensorClass("android.hardware.SystemSensorManager")
        hookSensorEventQueue()
        hookSensorDirectChannel()
        logInfo("sensor defender installed")
    }

    private fun hookSensorClass(clsName: String) {
        if (!sensorOn()) return
        val c = frameworkCls(clsName) ?: return
        var n = 0
        c.declaredMethods.forEach { m ->
            val name = m.name
            when {
                
                
                
                name.startsWith("registerListener") ||
                        name == "requestTriggerSensor" ||
                        name == "cancelTriggerSensor" ||
                        name == "flush" ||
                        name.startsWith("registerDynamicSensorCallback") -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!sensorOn()) return@hookMethod chain.proceed()
                            logWarn("blocked sensor register: $clsName.$name")
                            deniedFor(m)
                        }
                        n++
                    }
                }

                
                name == "createDirectChannel" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!sensorOn()) return@hookMethod chain.proceed()
                            logWarn("blocked sensor direct channel: $clsName.$name")
                            null
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

    







    private fun hookSensorEventQueue() {
        if (!sensorOn()) return
        val ssm = frameworkCls("android.hardware.SystemSensorManager") ?: return
        runCatching {
            val queues = ssm.declaredClasses.filter {
                it.name.contains("EventQueue") || it.name.contains("BaseEventQueue")
            }
            var n = 0
            for (q in queues) {
                q.declaredMethods.filter {
                    it.name == "dispatchSensorEvent" || it.name == "addSensor" ||
                        it.name == "removeSensor"
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!sensorOn()) return@hookMethod chain.proceed()
                        logWarn("blocked sensor dispatch: ${q.simpleName}.${m.name}")
                        deniedFor(m)
                    }
                    n++
                }
            }
            if (n > 0) logInfo("sensor event queue hooked x$n")
        }.onFailure { logWarn("sensor event queue hook skipped: ${it.message}") }
    }

    



    private fun hookSensorDirectChannel() {
        if (!sensorOn()) return
        val dc = frameworkCls("android.hardware.SensorDirectChannel") ?: return
        var n = 0
        dc.declaredMethods.filter { it.name == "read" }.forEach { m ->
            hookMethod(m) { chain ->
                if (!sensorOn()) return@hookMethod chain.proceed()
                logWarn("blocked SensorDirectChannel.read")
                0
            }
            n++
        }
        if (n > 0) logInfo("sensor direct channel hooked x$n")
    }

}
