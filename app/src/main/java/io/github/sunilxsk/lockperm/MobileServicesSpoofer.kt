package io.github.sunilxsk.lockperm

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method
import java.util.UUID


















internal class MobileServicesSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg = snapshot()
        val gms = cfg.gmsEnable
        val hms = cfg.hmsEnable
        if (!gms && !hms) return

        var n = 0
        n += hookPackageQueries()
        if (gms) n += installGms()
        if (hms) n += installHms()
        logInfo("mobile services spoof installed (hooks=$n, gms=$gms, hms=$hms)")
    }

    

    private val GMS_PKGS = setOf(
        "com.google.android.gms",          
        "com.android.vending",             
        "com.google.android.gsf",          
        "com.google.android.gsf.login",
    )

    private val HMS_PKGS = setOf(
        "com.huawei.hwid",                 
        "com.huawei.android.hms",
        "com.huawei.hms",
        "com.huawei.appmarket",            
        "com.huawei.android.pushagent",    
    )

    
    private val GMS_FEATURES = setOf(
        "com.google.android.feature.GOOGLE_EXPERIENCE",
        "com.google.android.feature.GOOGLE_BUILD",
        "com.google.android.feature.CHRE",
        "com.google.android.feature.PIXEL_EXPERIENCE",
    )

    
    private fun kindOf(pkg: String): Int = when {
        pkg in GMS_PKGS -> 1
        pkg in HMS_PKGS -> 2
        else -> 0
    }

    private fun installedOn(kind: Int): Boolean {
        val cfg = snapshot()
        return when (kind) {
            1 -> cfg.gmsEnable && cfg.gmsInstalled
            2 -> cfg.hmsEnable && cfg.hmsInstalled
            else -> false
        }
    }

    private fun versionOf(kind: Int): String {
        val cfg = snapshot()
        val v = if (kind == 1) cfg.gmsVersion else cfg.hmsVersion
        return v.ifBlank { if (kind == 1) XpConfig.DEF_GMS_VERSION else XpConfig.DEF_HMS_VERSION }
    }

    private fun codeOf(kind: Int): Int {
        val cfg = snapshot()
        val c = if (kind == 1) cfg.gmsVersionCode else cfg.hmsVersionCode
        return if (c > 0) c else if (kind == 1) XpConfig.DEF_GMS_VERSION_CODE else XpConfig.DEF_HMS_VERSION_CODE
    }

    

    private fun hookPackageQueries(): Int {
        var n = 0
        listOf(
            "android.app.ApplicationPackageManager",
            "android.content.pm.PackageManager",
        ).forEach { name ->
            val c = cls(name) ?: return@forEach
            c.declaredMethods.forEach { m ->
                val ok = when (m.name) {
                    "getPackageInfo" -> hookMethod(m) { chain -> onGetPackageInfo(m, chain) }
                    "getApplicationInfo" -> hookMethod(m) { chain -> onGetApplicationInfo(m, chain) }
                    "getInstalledPackages" -> hookMethod(m) { chain -> onInstalledList(chain, true) }
                    "getInstalledApplications" -> hookMethod(m) { chain -> onInstalledList(chain, false) }
                    "getLaunchIntentForPackage" -> hookMethod(m) { chain -> onLaunchIntent(m, chain) }
                    "getPackageGids" -> hookMethod(m) { chain -> onPackageGids(m, chain) }
                    "getPackageUid" -> hookMethod(m) { chain -> onPackageUid(m, chain) }
                    "hasSystemFeature" -> hookMethod(m) { chain -> onHasSystemFeature(chain) }
                    else -> false
                }
                if (ok) n++
            }
        }
        return n
    }

    
    private fun pkgArgOf(chain: XposedInterface.Chain, m: Method): String? {
        for (i in 0 until m.parameterTypes.size) {
            val a = runCatching { chain.getArg(i) }.getOrNull() ?: continue
            if (a is String) {
                if (a.contains('.')) return a
            } else {
                val s = runCatching {
                    a.javaClass.getMethod("getPackageName").invoke(a) as? String
                }.getOrNull()
                if (!s.isNullOrBlank()) return s
            }
        }
        return null
    }

    private fun onGetPackageInfo(m: Method, chain: XposedInterface.Chain): Any? {
        val pkg = pkgArgOf(chain, m) ?: return chain.proceed()
        val kind = kindOf(pkg)
        if (kind == 0 || !installedOn(kind)) return chain.proceed()

        
        val real = runCatching { chain.proceed() }.getOrNull() as? PackageInfo
        if (real != null) {
            applyVersion(real, versionOf(kind), codeOf(kind))
            return real
        }
        return buildPackageInfo(pkg, versionOf(kind), codeOf(kind))
    }

    private fun onGetApplicationInfo(m: Method, chain: XposedInterface.Chain): Any? {
        val pkg = pkgArgOf(chain, m) ?: return chain.proceed()
        val kind = kindOf(pkg)
        if (kind == 0 || !installedOn(kind)) return chain.proceed()

        val real = runCatching { chain.proceed() }.getOrNull() as? ApplicationInfo
        if (real != null) return real
        return buildApplicationInfo(pkg)
    }

    private fun onInstalledList(chain: XposedInterface.Chain, packageInfo: Boolean): Any? {
        val result = runCatching { chain.proceed() }.getOrNull() ?: return null
        val src = result as? List<*> ?: return result
        val cfg = snapshot()
        val want = ArrayList<String>()
        if (cfg.gmsEnable && cfg.gmsInstalled) want.addAll(GMS_PKGS)
        if (cfg.hmsEnable && cfg.hmsInstalled) want.addAll(HMS_PKGS)
        if (want.isEmpty()) return result

        val have = HashSet<String>()
        for (item in src) {
            val p = pkgOf(item)
            if (p != null) have.add(p)
        }
        val missing = want.filter { it !in have }
        if (missing.isEmpty()) return result

        val out = ArrayList<Any>(src.size + missing.size)
        for (item in src) if (item != null) out.add(item)
        for (p in missing) {
            val kind = kindOf(p)
            val fake = if (packageInfo) {
                buildPackageInfo(p, versionOf(kind), codeOf(kind))
            } else {
                buildApplicationInfo(p)
            }
            if (fake != null) out.add(fake)
        }
        logWarn("mobile services: 包列表 ${src.size} -> ${out.size}")
        return out
    }

    private fun pkgOf(item: Any?): String? = when (item) {
        is PackageInfo -> item.packageName
        is ApplicationInfo -> item.packageName
        else -> runCatching {
            val f = item?.javaClass?.getDeclaredField("packageName")
            f?.isAccessible = true
            f?.get(item) as? String
        }.getOrNull()
    }

    private fun onLaunchIntent(m: Method, chain: XposedInterface.Chain): Any? {
        val pkg = pkgArgOf(chain, m) ?: return chain.proceed()
        val kind = kindOf(pkg)
        if (kind == 0 || !installedOn(kind)) return chain.proceed()
        val real = runCatching { chain.proceed() }.getOrNull() as? Intent
        if (real != null) return real
        return runCatching {
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
        }.getOrNull()
    }

    private fun onPackageGids(m: Method, chain: XposedInterface.Chain): Any? {
        val pkg = pkgArgOf(chain, m) ?: return chain.proceed()
        val kind = kindOf(pkg)
        if (kind == 0 || !installedOn(kind)) return chain.proceed()
        val real = runCatching { chain.proceed() }.getOrNull() as? IntArray
        if (real != null && real.isNotEmpty()) return real
        return intArrayOf(3003)
    }

    private fun onPackageUid(m: Method, chain: XposedInterface.Chain): Any? {
        val pkg = pkgArgOf(chain, m) ?: return chain.proceed()
        val kind = kindOf(pkg)
        if (kind == 0 || !installedOn(kind)) return chain.proceed()
        val r = runCatching { chain.proceed() }.getOrNull()
        if (r is Int && r > 0) return r
        return FAKE_UID
    }

    private fun onHasSystemFeature(chain: XposedInterface.Chain): Any? {
        val cfg = snapshot()
        val f = runCatching {
            chain.args.filterIsInstance<String>().firstOrNull()
        }.getOrNull()
        if (f != null && cfg.gmsEnable && cfg.gmsInstalled && f in GMS_FEATURES) return true
        return chain.proceed()
    }

    

    private fun applyVersion(pi: PackageInfo, version: String, code: Int) {
        if (version.isNotBlank()) pi.versionName = version
        if (code > 0) {
            setInt(pi, "versionCode", code)
            setLong(pi, "longVersionCode", code.toLong())
        }
    }

    private fun buildPackageInfo(pkg: String, version: String, code: Int): PackageInfo? =
        runCatching {
            val pi = PackageInfo()
            pi.packageName = pkg
            pi.versionName = version
            setInt(pi, "versionCode", code)
            setLong(pi, "longVersionCode", code.toLong())
            pi.applicationInfo = buildApplicationInfo(pkg)
            pi.signatures = runCatching { arrayOf(Signature(FAKE_SIG)) }.getOrNull()
            pi.requestedPermissions = emptyArray()
            val now = System.currentTimeMillis()
            setLong(pi, "firstInstallTime", now - 86400_000L * 30)
            setLong(pi, "lastUpdateTime", now - 86400_000L * 5)
            pi
        }.getOrNull()

    private fun buildApplicationInfo(pkg: String): ApplicationInfo? =
        runCatching {
            val ai = ApplicationInfo()
            ai.packageName = pkg
            ai.processName = pkg
            setInt(ai, "uid", FAKE_UID)
            setInt(ai, "targetSdkVersion", android.os.Build.VERSION.SDK_INT)
            setInt(ai, "flags", 0x800000)     
            setInt(ai, "privateFlags", 0)
            ai.enabled = true
            ai.sourceDir = "/data/app/$pkg/base.apk"
            ai.publicSourceDir = ai.sourceDir
            ai.dataDir = "/data/user/0/$pkg"
            ai.nativeLibraryDir = "/data/app/$pkg/lib"
            ai
        }.getOrNull()

    private fun setInt(target: Any, field: String, value: Int) {
        runCatching {
            val f = target.javaClass.getDeclaredField(field)
            f.isAccessible = true
            f.setInt(target, value)
        }
    }

    private fun setLong(target: Any, field: String, value: Long) {
        runCatching {
            val f = target.javaClass.getDeclaredField(field)
            f.isAccessible = true
            f.setLong(target, value)
        }
    }

    

    private fun installGms(): Int {
        val cfg = snapshot()
        var n = 0

        if (cfg.gmsAvailable) {
            
            
            listOf(
                "com.google.android.gms.common.GoogleApiAvailability",
                "com.google.android.gms.common.GooglePlayServicesUtil",
                "com.google.android.gms.common.GooglePlayServicesNotAvailableException",
            ).forEach { name ->
                val c = loadClassAnywhere(name) ?: return@forEach
                c.declaredMethods
                    .filter { it.name in GMS_AVAIL_METHODS }
                    .forEach { m ->
                        if (hookMethod(m) { _ ->
                                logWarn("gms: ${m.name} -> ${m.returnType.simpleName} 伪装可用")
                                availableValue(m, 1)
                            }) n++
                    }
            }
        }

        if (cfg.gmsAdidEnable) {
            n += hookAdId(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                adIdOf(),
                cfg.gmsAdidLimit,
                "gms",
            )
        }
        return n
    }

    private val GMS_AVAIL_METHODS = setOf(
        "isGooglePlayServicesAvailable",
        "isGooglePlayServicesAvailableForUser",
        "isPlayServicesPossiblyUpdating",
        "isPlayStorePossiblyUpdating",
        "isUserResolvableError",
        "isUserRecoverableError",
        "getErrorString",
        "getErrorDialog",
        "showErrorDialog",
        "showErrorDialogFragment",
        "showErrorNotification",
        "makeGooglePlayServicesAvailable",
        "getApkVersion",
        "getClientVersion",
        "getOpenSourceSoftwareLicenseInfo",
    )

    

    private fun installHms(): Int {
        val cfg = snapshot()
        var n = 0

        if (cfg.hmsAvailable) {
            listOf(
                "com.huawei.hms.api.HuaweiApiAvailability",
                "com.huawei.hms.api.HuaweiMobileServicesUtil",
                "com.huawei.hms.api.HuaweiApiAvailabilityImpl",
            ).forEach { name ->
                val c = loadClassAnywhere(name) ?: return@forEach
                c.declaredMethods
                    .filter { it.name in HMS_AVAIL_METHODS }
                    .forEach { m ->
                        if (hookMethod(m) { _ ->
                                logWarn("hms: ${m.name} -> ${m.returnType.simpleName} 伪装可用")
                                availableValue(m, 2)
                            }) n++
                    }
            }
        }

        if (cfg.hmsAdidEnable) {
            n += hookAdId(
                "com.huawei.hms.ads.identifier.AdvertisingIdClient",
                oaidOf(),
                false,
                "hms",
            )
        }
        if (cfg.hmsAvailable) n += hookHmsToken()
        return n
    }

    
    private val hmsToken: String by lazy {
        "AA" + oaidOf().replace("-", "").take(140).padEnd(140, '0')
    }

    




    private fun hookHmsToken(): Int {
        val c = loadClassAnywhere("com.huawei.hms.aaid.HmsInstanceId") ?: return 0
        var n = 0
        c.declaredMethods
            .filter { (it.name == "getToken" || it.name == "getId") && it.returnType == String::class.java }
            .forEach { m ->
                if (hookMethod(m) { _ ->
                        logWarn("hms: ${m.name} -> 伪装推送 token")
                        hmsToken
                    }) n++
            }
        return n
    }

    private val HMS_AVAIL_METHODS = setOf(
        "isHuaweiMobileServicesAvailable",
        "isHuaweiMobileNoticeAvailable",
        "isUserResolvableError",
        "isUserRecoverableError",
        "getErrorString",
        "getErrorDialog",
        "showErrorDialog",
        "showErrorDialogFragment",
        "showErrorNotification",
        "resolveError",
        "getApkVersion",
        "getClientVersion",
    )

    




    private fun availableValue(m: Method, kind: Int): Any? {
        val n = m.name
        return when (m.returnType) {
            java.lang.Boolean.TYPE -> when {
                n.startsWith("isUserResolvable") -> false
                n.startsWith("isUserRecoverable") -> false
                n.startsWith("isPlayServicesPossiblyUpdating") -> false
                n.startsWith("isPlayStorePossiblyUpdating") -> false
                else -> true
            }
            java.lang.Integer.TYPE -> when {
                
                n.startsWith("getApkVersion") || n.startsWith("getClientVersion") -> codeOf(kind)
                else -> 0
            }
            String::class.java -> "success"
            java.lang.Void.TYPE -> null
            else -> null
        }
    }

    private fun adIdOf(): String {
        val v = snapshot().gmsAdid
        return v.ifBlank { XpConfig.DEF_GMS_ADID }
    }

    private fun oaidOf(): String {
        val v = snapshot().exOaid
        return v.ifBlank { UUID.randomUUID().toString() }
    }

    





    private fun hookAdId(clientName: String, id: String, limit: Boolean, tag: String): Int {
        val client = loadClassAnywhere(clientName) ?: return 0
        var n = 0

        client.declaredMethods
            .filter { it.name == "getAdvertisingIdInfo" || it.name == "getAdvertisingIdInfoCompat" }
            .forEach { m ->
                if (hookMethod(m) { chain ->
                        val fake = buildAdIdInfo(client, id, limit)
                        if (fake != null) {
                            logWarn("$tag: ${m.name} -> 伪装广告 ID")
                            fake
                        } else {
                            chain.proceed()
                        }
                    }) n++
            }

        val info = runCatching {
            Class.forName("$clientName\$Info", false, client.classLoader)
        }.getOrNull()
        if (info != null) {
            info.declaredMethods.forEach { m ->
                val r = when (m.name) {
                    "getId" -> id
                    "isLimitAdTrackingEnabled" -> limit
                    "isLimitTrackingEnabled" -> limit
                    else -> null
                }
                if (r != null && hookMethod(m) { _ -> r }) n++
            }
        }
        return n
    }

    
    private fun buildAdIdInfo(client: Class<*>, id: String, limit: Boolean): Any? {
        val info = runCatching {
            Class.forName("${client.name}\$Info", false, client.classLoader)
        }.getOrNull() ?: return null

        for (c in info.declaredConstructors) {
            val types = c.parameterTypes
            if (types.isEmpty() || types[0] != String::class.java) continue
            val args = ArrayList<Any?>(types.size)
            for (t in types) {
                args.add(
                    when {
                        t == String::class.java -> id
                        t == java.lang.Boolean.TYPE -> limit
                        t == java.lang.Integer.TYPE -> 0
                        else -> null
                    }
                )
            }
            val obj = runCatching {
                c.isAccessible = true
                c.newInstance(*args.toTypedArray())
            }.getOrNull()
            if (obj != null) return obj
        }
        return null
    }

    companion object {
        
        private const val FAKE_UID = 10123

        
        private const val FAKE_SIG = "308201dd30820146020101300d06092a864886f70d01010505" +
            "0030373110300e060355040a1307416e64726f69643112301006035504031309416e64" +
            "726f6964310b06035504061304555341301e170d3138303130313030303030305a170d" +
            "3338303130313030303030305a"
    }
}
