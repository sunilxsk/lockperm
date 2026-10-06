package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method























internal class VpnProxyDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    companion object {
        
        val netFiles = java.util.concurrent.ConcurrentHashMap<String, String>()
    }

    private val TRANSPORT_VPN = 4
    private val NET_CAPABILITY_NOT_VPN = 15
    private val TYPE_VPN = 17
    private val TYPE_MOBILE = 0

    fun install() {
        val cfg = snapshot()
        if (!cfg.vpnHideEnable) return
        val ifaces = cfg.vpnIfaces
        if (cfg.vpnHideIface) {
            hookNetworkInterface(ifaces)
            hookLinkProperties(ifaces)
            
            
            prepareNetFiles(ifaces)
        }
        if (cfg.vpnHideCaps) {
            hookNetworkCapabilities()
            hookConnectivityManager()
        }
        if (cfg.vpnHideNetInfo) hookNetworkInfo()
        if (cfg.vpnHideProxy) hookProxy()
        if (cfg.vpnHideSettings) hookSettings()
        logInfo("vpn/proxy hide installed (ifaces=${ifaces.size})")
    }

    





    
    private val netFilesDone = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun prepareNetFiles(ifaces: Set<String>) {
        if (!netFilesDone.compareAndSet(false, true)) return
        listOf("/proc/net/dev", "/proc/net/if_inet6", "/proc/net/route").forEach { path ->
            val real = FileSpoofer.readRaw(path) ?: return@forEach
            val lines = real.split("\n")
            val head = lines.take(path.let { if (it.endsWith("dev")) 2 else 0 })
            val body = lines.drop(head.size).filter { line ->
                val name = line.trim().substringBefore(':').substringBefore(' ').trim()
                name.isEmpty() || !isVpn(name, ifaces)
            }
            val content = (head + body).joinToString("\n")
            FakeFiles.setExtra(path, content)
            netFiles[path] = content
        }
        logInfo("net files prepared: ${netFiles.keys.joinToString()}")
    }

    private fun isVpn(name: String?, ifaces: Set<String>): Boolean {
        if (name.isNullOrEmpty()) return false
        val n = name.lowercase()
        if (n in ifaces) return true
        return ifaces.any { p -> n.startsWith(p) }
    }

    






    private val capsBypass: ThreadLocal<Boolean> = ThreadLocal.withInitial<Boolean> { false }

    







    private val realNames: MutableMap<Any, String> =
        java.util.Collections.synchronizedMap(java.util.WeakHashMap<Any, String>())
    private val bypass: ThreadLocal<Boolean> = ThreadLocal.withInitial<Boolean> { false }

    private fun nameOf(self: Any?, getName: Method?): String {
        if (self == null) return ""
        realNames[self]?.let { return it }
        if (getName == null) return ""
        val real = runCatching {
            bypass.set(true)
            try {
                getName.invoke(self) as? String
            } finally {
                bypass.set(false)
            }
        }.getOrNull()
        if (!real.isNullOrEmpty()) realNames[self] = real
        return real ?: ""
    }

    

    private fun hookNetworkInterface(ifaces: Set<String>) {
        val ni = loadClassAnywhere("java.net.NetworkInterface") ?: return
        val getName = runCatching { ni.getDeclaredMethod("getName") }.getOrNull()

        runCatching {
            ni.getDeclaredMethod("getName").let { m ->
                hookMethod(m) { chain ->
                    val n = chain.proceed() as? String
                    val self = chain.getThisObject()
                    if (self != null && n != null) realNames[self] = n
                    if (bypass.get() == true) n else if (isVpn(n, ifaces)) null else n
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getDisplayName").let { m ->
                hookMethod(m) { chain ->
                    val n = chain.proceed() as? String
                    if (isVpn(nameOf(chain.getThisObject(), getName), ifaces)) null else n
                }
            }
        }
        listOf("isUp", "isVirtual", "isPointToPoint", "supportsMulticast").forEach { fn ->
            runCatching {
                ni.getDeclaredMethod(fn).let { m ->
                    hookMethod(m) { chain ->
                        if (isVpn(nameOf(chain.getThisObject(), getName), ifaces)) false
                        else chain.proceed()
                    }
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getInetAddresses").let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    if (isVpn(nameOf(self, getName), ifaces)) {
                        java.util.Collections.enumeration(emptyList<Any>())
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getNetworkInterfaces").let { m ->
                hookMethod(m) { chain ->
                    val orig = chain.proceed()
                    if (orig !is java.util.Enumeration<*>) {
                        orig
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val e = orig as java.util.Enumeration<Any?>
                        val all = java.util.Collections.list(e)
                        
                        all.forEach { ni2 -> runCatching { getName?.invoke(ni2) } }
                        val kept = all.filter { ni2 ->
                            !isVpn(nameOf(ni2, getName), ifaces)
                        }
                        java.util.Collections.enumeration(kept)
                    }
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getByName", String::class.java).let { m ->
                hookMethod(m) { chain ->
                    val n = chain.getArg(0) as? String
                    if (isVpn(n, ifaces)) null else chain.proceed()
                }
            }
        }
        
        
        
        
        
        runCatching {
            ni.getDeclaredMethod("getHardwareAddress").let { m ->
                hookMethod(m) { chain ->
                    if (isVpn(nameOf(chain.getThisObject(), getName), ifaces)) null
                    else chain.proceed()
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getIndex").let { m ->
                hookMethod(m) { chain ->
                    if (isVpn(nameOf(chain.getThisObject(), getName), ifaces)) -1
                    else chain.proceed()
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getMTU").let { m ->
                hookMethod(m) { chain ->
                    if (isVpn(nameOf(chain.getThisObject(), getName), ifaces)) -1
                    else chain.proceed()
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getSubInterfaces").let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    if (isVpn(nameOf(self, getName), ifaces)) {
                        java.util.Collections.enumeration(emptyList<Any>())
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getParent").let { m ->
                hookMethod(m) { chain ->
                    if (isVpn(nameOf(chain.getThisObject(), getName), ifaces)) null
                    else chain.proceed()
                }
            }
        }
        
        runCatching {
            ni.getDeclaredMethod("getByIndex", INT_TYPE).let { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    if (isVpn(nameOf(r, getName), ifaces)) null else r
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("getByInetAddress", java.net.InetAddress::class.java).let { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    if (isVpn(nameOf(r, getName), ifaces)) null else r
                }
            }
        }
        runCatching {
            ni.getDeclaredMethod("networkInterfaces").let { m ->
                hookMethod(m) { chain -> filterEnumeration(chain, getName, ifaces) }
            }
        }
        logInfo("network interface hooks installed")
    }

    
    private fun filterEnumeration(
        chain: io.github.libxposed.api.XposedInterface.Chain,
        getName: Method?,
        ifaces: Set<String>,
    ): Any? {
        val orig = chain.proceed() ?: return null
        if (orig !is java.util.Enumeration<*>) return orig
        @Suppress("UNCHECKED_CAST")
        val e = orig as java.util.Enumeration<Any?>
        val all = java.util.Collections.list(e)
        
        all.forEach { ni2 -> runCatching { getName?.invoke(ni2) } }
        val kept = all.filter { ni2 -> !isVpn(nameOf(ni2, getName), ifaces) }
        return java.util.Collections.enumeration(kept)
    }

    

    private fun hookNetworkCapabilities() {
        val nc = loadClassAnywhere("android.net.NetworkCapabilities") ?: return
        runCatching {
            nc.getDeclaredMethod("hasTransport", INT_TYPE).let { m ->
                hookMethod(m) { chain ->
                    if (capsBypass.get() == true) return@hookMethod chain.proceed()
                    val t = chain.getArg(0) as? Int
                    if (t == TRANSPORT_VPN) false else chain.proceed()
                }
            }
        }
        runCatching {
            nc.getDeclaredMethod("hasCapability", INT_TYPE).let { m ->
                hookMethod(m) { chain ->
                    if (capsBypass.get() == true) return@hookMethod chain.proceed()
                    val c = chain.getArg(0) as? Int
                    if (c == NET_CAPABILITY_NOT_VPN) true else chain.proceed()
                }
            }
        }
        runCatching {
            nc.getDeclaredMethod("getTransportTypes").let { m ->
                hookMethod(m) { chain ->
                    val arr = chain.proceed() as? IntArray ?: return@hookMethod null
                    if (capsBypass.get() == true || arr.isEmpty()) arr
                    else arr.filter { it != TRANSPORT_VPN }.toIntArray()
                }
            }
        }
        runCatching {
            nc.getDeclaredMethod("toString").let { m ->
                hookMethod(m) { chain ->
                    val s = chain.proceed() as? String
                    if (s.isNullOrEmpty() || !s.contains("VPN")) s
                    else s.replace(Regex("[|&\\s]*VPN"), "")
                }
            }
        }
        logInfo("network capabilities hooks installed")
    }

    

    private fun hookLinkProperties(ifaces: Set<String>) {
        val lp = loadClassAnywhere("android.net.LinkProperties") ?: return
        runCatching {
            lp.getDeclaredMethod("getInterfaceName").let { m ->
                hookMethod(m) { chain ->
                    val n = chain.proceed() as? String
                    if (isVpn(n, ifaces)) null else n
                }
            }
        }
        
        runCatching {
            lp.getDeclaredMethod("getAllInterfaceNames").let { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    @Suppress("UNCHECKED_CAST")
                    val list = r as? List<String> ?: return@hookMethod r
                    list.filter { !isVpn(it, ifaces) }
                }
            }
        }
        
        runCatching {
            lp.getDeclaredMethod("toString").let { m ->
                hookMethod(m) { chain ->
                    val s = chain.proceed() as? String ?: return@hookMethod null
                    scrubVpnName(s, ifaces)
                }
            }
        }
        logInfo("link properties hooks installed")
    }

    
    private fun scrubVpnName(text: String, ifaces: Set<String>): String {
        var out = text
        for (p in ifaces) {
            if (p.isBlank()) continue
            out = out.replace(p, "", ignoreCase = true)
        }
        return out.replace(Regex("""InterfaceName:\s*"""), "InterfaceName: ")
    }

    

    private fun hookNetworkInfo() {
        val nii = loadClassAnywhere("android.net.NetworkInfo") ?: return
        val getType = runCatching { nii.getDeclaredMethod("getType") }.getOrNull()

        runCatching {
            nii.getDeclaredMethod("getType").let { m ->
                hookMethod(m) { chain ->
                    val t = chain.proceed() as? Int
                    if (t == TYPE_VPN) TYPE_MOBILE else t
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("getTypeName").let { m ->
                hookMethod(m) { chain ->
                    val n = chain.proceed() as? String
                    if (n.equals("VPN", ignoreCase = true)) "MOBILE" else n
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("getSubtypeName").let { m ->
                hookMethod(m) { chain ->
                    val n = chain.proceed() as? String
                    if (n.equals("VPN", ignoreCase = true)) "LTE" else n
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("isConnected").let { m ->
                hookMethod(m) { chain ->
                    if (typeIs(chain.getThisObject(), getType, TYPE_VPN)) false
                    else chain.proceed()
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("isConnectedOrConnecting").let { m ->
                hookMethod(m) { chain ->
                    if (typeIs(chain.getThisObject(), getType, TYPE_VPN)) false
                    else chain.proceed()
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("getState").let { m ->
                hookMethod(m) { chain ->
                    if (typeIs(chain.getThisObject(), getType, TYPE_VPN)) {
                        runCatching {
                            val e = loadClassAnywhere("android.net.NetworkInfo\$State")
                            e?.getField("DISCONNECTED")?.get(null)
                        }.getOrNull() ?: chain.proceed()
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("getExtraInfo").let { m ->
                hookMethod(m) { chain ->
                    val s = chain.proceed() as? String
                    if (s.isNullOrEmpty()) s
                    else if (s.contains("vpn", ignoreCase = true)) null else s
                }
            }
        }
        
        runCatching {
            nii.getDeclaredMethod("getDetailedState").let { m ->
                hookMethod(m) { chain ->
                    if (typeIs(chain.getThisObject(), getType, TYPE_VPN)) {
                        runCatching {
                            val e = loadClassAnywhere("android.net.NetworkInfo\$DetailedState")
                            e?.getField("DISCONNECTED")?.get(null)
                        }.getOrNull() ?: chain.proceed()
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
        listOf("isAvailable", "isConnectedOrConnecting", "isFailover", "isRoaming").forEach { fn ->
            runCatching {
                nii.getDeclaredMethod(fn).let { m ->
                    hookMethod(m) { chain ->
                        if (typeIs(chain.getThisObject(), getType, TYPE_VPN)) false
                        else chain.proceed()
                    }
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("getReason").let { m ->
                hookMethod(m) { chain ->
                    val s = chain.proceed() as? String
                    if (s.isNullOrEmpty()) s
                    else if (s.contains("vpn", ignoreCase = true)) null else s
                }
            }
        }
        runCatching {
            nii.getDeclaredMethod("toString").let { m ->
                hookMethod(m) { chain ->
                    val s = chain.proceed() as? String
                    if (s.isNullOrEmpty()) s
                    else if (s.contains("vpn", ignoreCase = true)) {
                        s.replace(Regex("(?i)vpn"), "MOBILE")
                    } else s
                }
            }
        }
        logInfo("network info hooks installed")
    }

    

    private fun hookConnectivityManager() {
        val cm = loadClassAnywhere("android.net.ConnectivityManager") ?: return
        
        
        
        cm.declaredMethods.filter { it.name == "getAllNetworks" }.forEach { m ->
            hookMethod(m) { chain ->
                val arr = chain.proceed() as? Array<*> ?: return@hookMethod chain.proceed()
                val self = chain.getThisObject()
                val kept = arr.filterNotNull().filter { n -> !isVpnNetwork(self, n) }
                if (kept.size == arr.filterNotNull().size) return@hookMethod arr
                @Suppress("UNCHECKED_CAST")
                (arr as Array<Any?>).let { src ->
                    val type = src.javaClass.componentType
                    java.lang.reflect.Array.newInstance(type, kept.size).also { out ->
                        kept.forEachIndexed { i, v ->
                            java.lang.reflect.Array.set(out, i, v)
                        }
                    }
                }
            }
        }
        logInfo("connectivity manager hooks installed")
    }

    private fun typeIs(self: Any?, getType: Method?, want: Int): Boolean {
        if (self == null || getType == null) return false
        return runCatching { getType.invoke(self) as? Int }.getOrNull() == want
    }

    






    private fun isVpnNetwork(cm: Any?, net: Any?): Boolean {
        if (cm == null || net == null) return false
        return runCatching {
            val m = cm.javaClass.getMethod("getNetworkCapabilities", net.javaClass)
            val caps = runCatching {
                capsBypass.set(true)
                try {
                    m.invoke(cm, net)
                } finally {
                    capsBypass.set(false)
                }
            }.getOrNull() ?: return false
            val ht = caps.javaClass.getMethod("hasTransport", INT_TYPE)
            ht.invoke(caps, TRANSPORT_VPN) as? Boolean ?: false
        }.getOrDefault(false)
    }

    

    private fun hookProxy() {
        
        val sys = loadClassAnywhere("java.lang.System")
        if (sys != null) {
            runCatching {
                sys.getDeclaredMethod("getProperty", String::class.java).let { m ->
                    hookMethod(m) { chain ->
                        val k = chain.getArg(0) as? String
                        if (k != null && isProxyKey(k)) null else chain.proceed()
                    }
                }
            }
            runCatching {
                sys.getDeclaredMethod(
                    "getProperty", String::class.java, String::class.java
                ).let { m ->
                    hookMethod(m) { chain ->
                        val k = chain.getArg(0) as? String
                        if (k != null && isProxyKey(k)) null else chain.proceed()
                    }
                }
            }
        }

        
        val cm = loadClassAnywhere("android.net.ConnectivityManager")
        if (cm != null) {
            cm.declaredMethods.filter { it.name == "getDefaultProxy" }.forEach { m ->
                hookMethod(m) { _ -> null }
            }
        }

        
        val p = loadClassAnywhere("android.net.Proxy")
        if (p != null) {
            p.declaredMethods.filter { it.name == "getHost" || it.name == "getDefaultHost" }
                .forEach { m -> hookMethod(m) { _ -> null } }
            p.declaredMethods.filter { it.name == "getPort" || it.name == "getDefaultPort" }
                .forEach { m -> hookMethod(m) { _ -> -1 } }
        }

        
        
        
        
        val pi = loadClassAnywhere("android.net.ProxyInfo")
        if (pi != null) {
            pi.declaredMethods.filter {
                it.name == "getHost" || it.name == "getPacFileUrl" ||
                    it.name == "getExclusionListAsString"
            }.forEach { m -> hookMethod(m) { _ -> null } }
            pi.declaredMethods.filter { it.name == "getExclusionList" }.forEach { m ->
                hookMethod(m) { _ -> emptyArray<String>() }
            }
            pi.declaredMethods.filter { it.name == "getPort" }.forEach { m ->
                hookMethod(m) { _ -> -1 }
            }
            pi.declaredMethods.filter { it.name == "toString" }.forEach { m ->
                hookMethod(m) { _ -> "" }
            }
        }

        
        val lp = loadClassAnywhere("android.net.LinkProperties")
        if (lp != null) {
            lp.declaredMethods.filter { it.name == "getHttpProxy" }.forEach { m ->
                hookMethod(m) { _ -> null }
            }
        }
        logInfo("proxy hooks installed")
    }

    private fun isProxyKey(k: String): Boolean {
        val l = k.lowercase()
        return l.contains("proxy") || l == "http.proxyhost" ||
                l == "https.proxyhost" || l == "socksproxyhost"
    }

    

    private fun hookSettings() {
        listOf(
            "android.provider.Settings\$Secure",
            "android.provider.Settings\$Global",
        ).forEach { name ->
            val c = loadClassAnywhere(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "getString" }.forEach { m ->
                hookMethod(m) { chain ->
                    val k = chain.getArg(1) as? String
                    if (k != null && k.contains("vpn", ignoreCase = true)) "" else chain.proceed()
                }
            }
            c.declaredMethods.filter { it.name == "getStringForUser" }.forEach { m ->
                hookMethod(m) { chain ->
                    val k = chain.getArg(1) as? String
                    if (k != null && k.contains("vpn", ignoreCase = true)) "" else chain.proceed()
                }
            }
        }
        logInfo("settings vpn hooks installed")
    }
}
