package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule








internal class WifiFakeDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val WIFI_STATE_ENABLED = 3
    private val TRANSPORT_WIFI = 1
    private val NET_CAPABILITY_INTERNET = 12
    private val NET_CAPABILITY_NOT_METERED = 11
    private val NET_CAPABILITY_VALIDATED = 16
    private val TYPE_VPN = 17

    fun install() {
        val cfg = snapshot()
        if (!cfg.wifiFakeEnable) return
        hookWifiManager()
        hookWifiInfo()
        if (cfg.wifiFakeNetwork) {
            hookNetworkCapabilities()
            hookNetworkInfo()
            hookConnectivityManager()
        }
        logInfo("wifi fake installed (ssid=${cfg.wifiFakeSsid})")
    }

    

    private fun hookWifiManager() {
        val wm = loadClassAnywhere("android.net.wifi.WifiManager") ?: return

        wm.declaredMethods.filter { it.name == "isWifiEnabled" }.forEach { m ->
            hookMethod(m) { _ -> true }
        }
        wm.declaredMethods.filter {
            it.name == "getWifiState" || it.name == "getWifiApState"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (m.name == "getWifiState") WIFI_STATE_ENABLED else chain.proceed()
            }
        }
        wm.declaredMethods.filter { it.name == "getConnectionInfo" }.forEach { m ->
            hookMethod(m) { chain ->
                val orig = chain.proceed()
                if (orig != null) {
                    patchWifiInfo(orig)
                    orig
                } else {
                    buildWifiInfo() ?: orig
                }
            }
        }
        wm.declaredMethods.filter { it.name == "getScanResults" }.forEach { m ->
            hookMethod(m) { chain ->
                val cfg = snapshot()
                if (!cfg.wifiFakeScan) return@hookMethod chain.proceed()
                val list = cfg.wifiFakeList
                if (list.isEmpty()) return@hookMethod chain.proceed()
                val made = buildScanResults(list)
                if (made.isEmpty()) chain.proceed() else made
            }
        }
        logInfo("wifi manager hooks installed")
    }

    

    private fun hookWifiInfo() {
        val wi = loadClassAnywhere("android.net.wifi.WifiInfo") ?: return
        val cfg = snapshot()

        if (cfg.wifiFakeSsid.isNotEmpty()) {
            wi.declaredMethods.filter { it.name == "getSSID" }.forEach { m ->
                hookMethod(m) { _ -> "\"${snapshot().wifiFakeSsid}\"" }
            }
        }
        if (cfg.wifiFakeBssid.isNotEmpty()) {
            wi.declaredMethods.filter { it.name == "getBSSID" }.forEach { m ->
                hookMethod(m) { _ -> snapshot().wifiFakeBssid }
            }
        }
        wi.declaredMethods.filter { it.name == "getRssi" }.forEach { m ->
            hookMethod(m) { _ -> snapshot().wifiFakeRssi }
        }
        wi.declaredMethods.filter { it.name == "getLinkSpeed" }.forEach { m ->
            hookMethod(m) { _ -> snapshot().wifiFakeSpeed }
        }
        wi.declaredMethods.filter { it.name == "getFrequency" }.forEach { m ->
            hookMethod(m) { _ -> snapshot().wifiFakeFreq }
        }
        wi.declaredMethods.filter { it.name == "getIpAddress" }.forEach { m ->
            hookMethod(m) { _ -> ipToInt(snapshot().wifiFakeIp) }
        }
        wi.declaredMethods.filter { it.name == "getNetworkId" }.forEach { m ->
            hookMethod(m) { _ -> 1 }
        }
        wi.declaredMethods.filter { it.name == "getHiddenSSID" }.forEach { m ->
            hookMethod(m) { _ -> false }
        }
        logInfo("wifi info hooks installed")
    }

    private fun patchWifiInfo(info: Any) {
        val cfg = snapshot()
        if (cfg.wifiFakeSsid.isNotEmpty()) {
            call(info, "setSSID", "\"${cfg.wifiFakeSsid}\"")
        }
        if (cfg.wifiFakeBssid.isNotEmpty()) {
            call(info, "setBSSID", cfg.wifiFakeBssid)
        }
        call(info, "setRssi", cfg.wifiFakeRssi)
        call(info, "setLinkSpeed", cfg.wifiFakeSpeed)
        call(info, "setNetworkId", 1)
        runCatching {
            val inet = java.net.InetAddress.getByName(cfg.wifiFakeIp)
            info.javaClass.getDeclaredMethod(
                "setInetAddress", java.net.InetAddress::class.java
            ).apply { isAccessible = true }.invoke(info, inet)
        }
    }

    private fun call(target: Any, name: String, value: Any?) {
        runCatching {
            val c = target.javaClass
            val m = when (value) {
                is Int -> c.getDeclaredMethod(name, INT_TYPE)
                is Boolean -> c.getDeclaredMethod(name, java.lang.Boolean.TYPE)
                else -> c.getDeclaredMethod(name, String::class.java)
            }
            m.isAccessible = true
            m.invoke(target, value)
        }
    }

    private fun buildWifiInfo(): Any? {
        val wi = loadClassAnywhere("android.net.wifi.WifiInfo") ?: return null
        val obj = runCatching {
            val ctor = wi.getDeclaredConstructor()
            ctor.isAccessible = true
            ctor.newInstance()
        }.getOrNull() ?: return null
        patchWifiInfo(obj)
        return obj
    }

    private fun buildScanResults(list: List<FakeAp>): List<Any> {
        val sr = loadClassAnywhere("android.net.wifi.ScanResult") ?: return emptyList()
        val out = ArrayList<Any>(list.size)
        list.forEach { ap ->
            val obj = runCatching {
                val ctor = sr.getDeclaredConstructor()
                ctor.isAccessible = true
                ctor.newInstance()
            }.getOrNull() ?: return@forEach
            runCatching { setField(obj, "SSID", "\"${ap.ssid}\"") }
            runCatching { setField(obj, "BSSID", ap.bssid) }
            runCatching { setField(obj, "level", ap.level) }
            runCatching { setField(obj, "frequency", 5180) }
            runCatching { setField(obj, "capabilities", ap.caps) }
            out.add(obj)
        }
        return out
    }

    private fun setField(obj: Any, name: String, value: Any?) {
        val f = obj.javaClass.getDeclaredField(name)
        f.isAccessible = true
        f.set(obj, value)
    }

    private fun ipToInt(ip: String): Int {
        val parts = ip.split(".")
        if (parts.size != 4) return 0
        return runCatching {
            ((parts[0].toInt() and 0xFF) shl 0) or
                    ((parts[1].toInt() and 0xFF) shl 8) or
                    ((parts[2].toInt() and 0xFF) shl 16) or
                    ((parts[3].toInt() and 0xFF) shl 24)
        }.getOrDefault(0)
    }

    

    private fun hookNetworkCapabilities() {
        val nc = loadClassAnywhere("android.net.NetworkCapabilities") ?: return
        runCatching {
            nc.getDeclaredMethod("hasTransport", INT_TYPE).let { m ->
                hookMethod(m) { chain ->
                    val t = chain.getArg(0) as? Int
                    if (t == TRANSPORT_WIFI) true else chain.proceed()
                }
            }
        }
        runCatching {
            nc.getDeclaredMethod("hasCapability", INT_TYPE).let { m ->
                hookMethod(m) { chain ->
                    val c = chain.getArg(0) as? Int
                    when (c) {
                        NET_CAPABILITY_INTERNET,
                        NET_CAPABILITY_NOT_METERED,
                        NET_CAPABILITY_VALIDATED -> true

                        else -> chain.proceed()
                    }
                }
            }
        }
        runCatching {
            nc.getDeclaredMethod("getTransportTypes").let { m ->
                hookMethod(m) { chain ->
                    val arr = chain.proceed() as? IntArray
                    if (arr == null || arr.isEmpty()) intArrayOf(TRANSPORT_WIFI)
                    else if (arr.contains(TRANSPORT_WIFI)) arr
                    else arr + TRANSPORT_WIFI
                }
            }
        }
    }

    

    private fun hookNetworkInfo() {
        val nii = loadClassAnywhere("android.net.NetworkInfo") ?: return
        val getType = runCatching { nii.getDeclaredMethod("getType") }.getOrNull()
        listOf("isConnected", "isConnectedOrConnecting", "isAvailable").forEach { fn ->
            runCatching {
                nii.getDeclaredMethod(fn).let { m ->
                    hookMethod(m) { chain ->
                        val t = runCatching {
                            getType?.invoke(chain.getThisObject()) as? Int
                        }.getOrNull()
                        
                        if (t == TYPE_VPN) chain.proceed() else true
                    }
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("getTypeName").let { m ->
                hookMethod(m) { chain ->
                    val n = chain.proceed() as? String
                    if (n.isNullOrEmpty()) "WIFI" else n
                }
            }
        }
    }

    

    private fun hookConnectivityManager() {
        val cm = loadClassAnywhere("android.net.ConnectivityManager") ?: return
        cm.declaredMethods.filter { it.name == "getActiveNetworkInfo" }.forEach { m ->
            hookMethod(m) { chain ->
                val orig = chain.proceed()
                if (orig != null) orig else buildNetworkInfo()
            }
        }
    }

    private fun buildNetworkInfo(): Any? {
        val nii = loadClassAnywhere("android.net.NetworkInfo") ?: return null
        val obj = runCatching {
            val ctor = nii.getDeclaredConstructor(INT_TYPE)
            ctor.isAccessible = true
            ctor.newInstance(TRANSPORT_WIFI)
        }.getOrNull() ?: runCatching {
            val ctor = nii.getDeclaredConstructor(
                INT_TYPE, INT_TYPE, String::class.java, String::class.java
            )
            ctor.isAccessible = true
            ctor.newInstance(1, 0, "WIFI", "")
        }.getOrNull() ?: return null
        runCatching {
            val ms = nii.getDeclaredMethod("setIsAvailable", java.lang.Boolean.TYPE)
            ms.isAccessible = true
            ms.invoke(obj, true)
        }
        runCatching {
            val st = loadClassAnywhere("android.net.NetworkInfo\$DetailedState")
            if (st != null) {
                val connected = st.getField("CONNECTED").get(null)
                val ms = nii.getDeclaredMethod(
                    "setDetailedState",
                    st, String::class.java, String::class.java
                )
                ms.isAccessible = true
                ms.invoke(obj, connected, null, snapshot().wifiFakeSsid)
            }
        }
        return obj
    }
}
