package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.net.InetAddress












internal class NetworkParamSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg = snapshot()
        if (!cfg.enableBuild || !cfg.netpEnable) {
            logInfo("net param spoof skipped (build=${cfg.enableBuild} on=${cfg.netpEnable})")
            return
        }
        var n = 0
        n += hookNetworkInterface()
        n += hookWifiInfo()
        n += hookLinkProperties()
        logInfo("net param spoof installed (hooks=$n)")
    }

    private fun addr(text: String): InetAddress? =
        runCatching { InetAddress.getByName(text.trim()) }.getOrNull()

    
    private fun toInt(text: String): Int? {
        val a = addr(text)?.address ?: return null
        if (a.size != 4) return null
        return ((a[0].toInt() and 0xFF)) or
                ((a[1].toInt() and 0xFF) shl 8) or
                ((a[2].toInt() and 0xFF) shl 16) or
                ((a[3].toInt() and 0xFF) shl 24)
    }

    

    private fun hookNetworkInterface(): Int {
        val ni = loadClassAnywhere("java.net.NetworkInterface") ?: return 0
        var n = 0
        ni.declaredMethods.filter {
            it.name == "getInetAddresses" && it.parameterTypes.isEmpty()
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    
                    runCatching { spoofAddresses(chain) }.getOrElse { chain.proceed() }
                }) n++
        }
        return n
    }

    
    private fun spoofAddresses(chain: XposedInterface.Chain): Any? {
        val cfg = snapshot()
        val r = chain.proceed() ?: return null
        val extra = ArrayList<InetAddress>()
        cfg.ipv4.takeIf { it.isNotBlank() }?.let { addr(it)?.let { a -> extra.add(a) } }
        cfg.ipv6.takeIf { it.isNotBlank() }?.let { addr(it)?.let { a -> extra.add(a) } }
        if (extra.isEmpty()) return r
        val out = ArrayList<InetAddress>()
        when (r) {
            is java.util.Enumeration<*> -> {
                while (r.hasMoreElements()) {
                    (r.nextElement() as? InetAddress)?.let { out.add(it) }
                }
            }
            is Collection<*> -> r.forEach { (it as? InetAddress)?.let { a -> out.add(a) } }
            else -> return r
        }
        
        out.addAll(0, extra)
        return java.util.Collections.enumeration(out)
    }

    

    private fun hookWifiInfo(): Int {
        val wi = loadClassAnywhere("android.net.wifi.WifiInfo") ?: return 0
        var n = 0
        wi.declaredMethods.filter { it.parameterTypes.isEmpty() }.forEach { m ->
            when (m.name) {
                
                
                "getIpAddress" -> {
                    if (hookMethod(m) { chain ->
                            val cfg = snapshot()
                            if (cfg.ipv4.isBlank()) return@hookMethod chain.proceed()
                            orProceed(chain, toInt(cfg.ipv4))
                        }) n++
                }
                "getServerAddress" -> {
                    if (hookMethod(m) { chain ->
                        val cfg = snapshot()
                        if (cfg.gateway.isBlank()) return@hookMethod chain.proceed()
                        orProceed(chain, toInt(cfg.gateway))
                    }) n++
                }
                "getWifiStandard" -> {
                    if (hookMethod(m) { chain ->
                        orProceed(chain, wifiStandard(wi, snapshot().wifiStd))
                    }) n++
                }
            }
        }
        return n
    }

    




    private fun wifiStandard(wi: Class<*>, std: String): Any? {
        val name = when (std.trim()) {
            "7" -> "WIFI_STANDARD_11BE"
            "6" -> "WIFI_STANDARD_11AX"
            "5" -> "WIFI_STANDARD_11AC"
            "4" -> "WIFI_STANDARD_11N"
            else -> return null
        }
        val v = runCatching {
            val f = wi.getDeclaredField(name)
            f.isAccessible = true
            f.getInt(null)
        }.getOrNull() ?: runCatching {
            
            val f = wi.getDeclaredField("WIFI_STANDARD_11AC")
            f.isAccessible = true
            f.getInt(null)
        }.getOrNull() ?: return null
        return v
    }

    

    private fun hookLinkProperties(): Int {
        val lp = loadClassAnywhere("android.net.LinkProperties") ?: return 0
        var n = 0
        lp.declaredMethods.filter { it.name == "getDnsServers" && it.parameterTypes.isEmpty() }
            .forEach { m ->
                if (hookMethod(m) { chain ->
                        
                        val cfg = snapshot()
                        val out = ArrayList<InetAddress>()
                        cfg.dns1.takeIf { it.isNotBlank() }?.let { addr(it)?.let { a -> out.add(a) } }
                        cfg.dns2.takeIf { it.isNotBlank() }?.let { addr(it)?.let { a -> out.add(a) } }
                        if (out.isEmpty()) chain.proceed() else out
                    }) n++
            }
        return n
    }
}
