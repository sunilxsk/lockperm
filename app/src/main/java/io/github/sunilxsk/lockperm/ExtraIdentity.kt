package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field












internal class ExtraIdentity(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private var uptimeBaseReal = 0L
    private var uptimeTarget = 0L

    fun install() {
        val cfg = snapshot()

        
        if (!cfg.wifiFakeEnable) hookWifiSsid(cfg.exWifiSsid, cfg.exWifiBssid)

        if (cfg.exDevOff) hookDevOptionsOff()
        hookTelephony()
        hookTimezone(cfg.exTimezone)
        hookLocaleAll(cfg.exLocale)
        if (cfg.exTimeEnable && cfg.exTimeOffset != 0) hookTimeOffset(cfg.exTimeOffset)
        if (cfg.exUptimeEnable) hookUptime(cfg.exUptimeHours)
        hookAndroidVersion(cfg)
        hookMac(cfg.exWifiMac, cfg.exBtMac)
        hookKernelArch(cfg)
        hookHwSerial(cfg.exHwSerial, cfg.buildValues["SERIAL"])
        hookFirebase(cfg.exFbFid, cfg.exFbIid)
        hookGsf(cfg.gsfId)
        if (cfg.exOaid.isNotEmpty()) hookOaid(cfg.exOaid)

        logInfo("extra identity installed")
    }

    

    private fun hookWifiSsid(ssid: String, bssid: String) {
        if (ssid.isEmpty() && bssid.isEmpty()) return
        val wi = loadClassAnywhere("android.net.wifi.WifiInfo") ?: return
        if (ssid.isNotEmpty()) {
            wi.declaredMethods.filter { it.name == "getSSID" }.forEach { m ->
                hookMethod(m) { _ -> "\"$ssid\"" }
            }
        }
        if (bssid.isNotEmpty()) {
            wi.declaredMethods.filter { it.name == "getBSSID" }.forEach { m ->
                hookMethod(m) { _ -> bssid }
            }
        }
        logInfo("wifi ssid hooked")
    }

    

    private val DEV_KEYS = setOf(
        "development_settings_enabled", "adb_enabled", "adb_wifi_enabled",
        "verifier_verify_adb_installs", "stay_on_while_plugged_in",
        "bugreport_in_power_menu", "oem_unlock_enabled",
    )

    private fun hookDevOptionsOff() {
        listOf(
            "android.provider.Settings\$Global",
            "android.provider.Settings\$Secure",
        ).forEach { name ->
            val c = loadClassAnywhere(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "getString" }.forEach { m ->
                hookMethod(m) { chain ->
                    val k = chain.getArg(1) as? String
                    if (k != null && k in DEV_KEYS) "0" else chain.proceed()
                }
            }
            c.declaredMethods.filter { it.name == "getInt" }.forEach { m ->
                hookMethod(m) { chain ->
                    val k = chain.getArg(1) as? String
                    if (k != null && k in DEV_KEYS) 0 else chain.proceed()
                }
            }
        }
        
        logInfo("dev options off hooked")
    }

    

    private fun hookTelephony() {
        val cfg = snapshot()
        val tm = loadClassAnywhere("android.telephony.TelephonyManager") ?: return

        val map = LinkedHashMap<String, String>()
        if (cfg.exSimOperator.isNotEmpty()) {
            map["getSimOperator"] = cfg.exSimOperator
            map["getNetworkOperator"] = cfg.exSimOperator
        }
        if (cfg.exSimCountry.isNotEmpty()) {
            map["getSimCountryIso"] = cfg.exSimCountry
            map["getNetworkCountryIso"] = cfg.exSimCountry
        }
        if (cfg.exSimOperatorName.isNotEmpty()) {
            map["getSimOperatorName"] = cfg.exSimOperatorName
        }
        if (cfg.exCarrier.isNotEmpty()) {
            map["getNetworkOperatorName"] = cfg.exCarrier
        }
        val iccid = cfg.exIccid.ifEmpty { cfg.exSimSerial }
        if (iccid.isNotEmpty()) map["getSimSerialNumber"] = iccid
        if (cfg.exSimSubscriber.isNotEmpty()) map["getSubscriberId"] = cfg.exSimSubscriber
        if (cfg.exPhoneNumber.isNotEmpty()) {
            map["getLine1Number"] = cfg.exPhoneNumber
            map["getMsisdn"] = cfg.exPhoneNumber
            map["getVoiceMailNumber"] = cfg.exPhoneNumber
            map["getLine1AlphaTag"] = cfg.exPhoneNumber
        }
        if (cfg.exImei.isNotEmpty()) {
            map["getDeviceId"] = cfg.exImei
            map["getImei"] = cfg.exImei
        }
        if (cfg.exMeid.isNotEmpty()) map["getMeid"] = cfg.exMeid

        if (map.isEmpty()) return

        map.forEach { (name, value) ->
            tm.declaredMethods.filter { it.name == name && it.returnType == String::class.java }
                .forEach { m -> hookMethod(m) { _ -> value } }
        }
        if (cfg.exImei.isNotEmpty() || cfg.exSimOperator.isNotEmpty()) {
            tm.declaredMethods.filter { it.name == "getSimState" }.forEach { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    if (r is Int && r == 1) 5 else r
                }
            }
        }
        logInfo("telephony hooked (${map.size} fields)")
    }

    

    private fun hookTimezone(id: String) {
        if (id.isBlank()) return
        val tz = loadClassAnywhere("java.util.TimeZone") ?: return
        val fixed = runCatching {
            val m = tz.getDeclaredMethod("getTimeZone", String::class.java)
            m.isAccessible = true
            m.invoke(null, id)
        }.getOrNull()
        if (fixed == null) {
            logWarn("timezone id invalid: $id")
            return
        }
        tz.declaredMethods.filter { it.name == "getDefault" && it.parameterTypes.isEmpty() }
            .forEach { m -> hookMethod(m) { _ -> fixed } }
        logInfo("timezone hooked -> $id")
    }

    

    











    private fun hookLocaleAll(tag: String) {
        if (tag.isBlank()) return
        val loc = runCatching { java.util.Locale.forLanguageTag(tag.replace('_', '-')) }.getOrNull()
        if (loc == null || loc.language.isEmpty()) {
            logWarn("locale tag invalid: $tag")
            return
        }

        val localeCls = loadClassAnywhere("java.util.Locale")

        
        runCatching { java.util.Locale.setDefault(loc) }

        
        if (localeCls != null) {
            localeCls.declaredMethods.filter {
                it.name == "getDefault" && it.parameterTypes.isEmpty()
            }.forEach { m -> hookMethod(m) { _ -> loc } }

            runCatching {
                val cat = loadClassAnywhere("java.util.Locale\$Category")
                if (cat != null) {
                    localeCls.declaredMethods.filter {
                        it.name == "getDefault" && it.parameterTypes.size == 1 &&
                                it.parameterTypes[0] == cat
                    }.forEach { m -> hookMethod(m) { _ -> loc } }
                }
            }
        }

        
        runCatching {
            val sys = loadClassAnywhere("java.lang.System") ?: return@runCatching
            sys.declaredMethods.filter {
                it.name == "getProperty" && it.parameterTypes.size == 1
            }.forEach { m ->
                hookMethod(m) { chain ->
                    when (chain.getArg(0) as? String) {
                        "user.language" -> loc.language
                        "user.country" -> loc.country
                        "user.variant" -> loc.variant
                        "user.locale" -> "${loc.language}_${loc.country}"
                        else -> chain.proceed()
                    }
                }
            }
        }

        val localeList = buildLocaleList(loc)

        
        if (localeList != null) {
            runCatching {
                val ll = loadClassAnywhere("android.os.LocaleList") ?: return@runCatching
                ll.declaredMethods.filter { it.name == "getDefault" && it.parameterTypes.isEmpty() }
                    .forEach { m -> hookMethod(m) { _ -> localeList } }
                ll.declaredMethods.filter { it.name == "get" && it.parameterTypes.size == 1 }
                    .forEach { m -> hookMethod(m) { _ -> loc } }
                ll.declaredMethods.filter { it.name == "size" && it.parameterTypes.isEmpty() }
                    .forEach { m -> hookMethod(m) { _ -> 1 } }
                logInfo("locale list hooked")
            }
        }

        
        val cfgCls = loadClassAnywhere("android.content.res.Configuration")
        if (cfgCls != null && localeList != null) {
            runCatching {
                cfgCls.declaredMethods.filter {
                    it.name == "getLocales" && it.parameterTypes.isEmpty()
                }.forEach { m -> hookMethod(m) { _ -> localeList } }
            }
        }

        
        val res = loadClassAnywhere("android.content.res.Resources")
        if (res != null) {
            runCatching {
                res.declaredMethods.filter {
                    it.name == "getConfiguration" && it.parameterTypes.isEmpty()
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        val r = chain.proceed()
                        patchConfig(r, loc, localeList)
                        r
                    }
                }
            }
            runCatching {
                res.declaredMethods.filter { it.name == "updateConfiguration" }.forEach { m ->
                    hookMethod(m) { chain ->
                        patchConfig(chain.getArg(0), loc, localeList)
                        chain.proceed()
                    }
                }
            }
        }

        
        runCatching {
            val am = loadClassAnywhere("android.content.res.AssetManager") ?: return@runCatching
            am.declaredMethods.filter { it.name == "getLocales" }.forEach { m ->
                hookMethod(m) { _ -> arrayOf(loc.toLanguageTag()) }
            }
        }

        logInfo("locale hooked -> $tag")
    }

    private fun buildLocaleList(loc: java.util.Locale): Any? {
        val ll = loadClassAnywhere("android.os.LocaleList") ?: return null
        runCatching {
            val c1 = ll.getDeclaredConstructor(java.util.Locale::class.java)
            c1.isAccessible = true
            return c1.newInstance(loc)
        }
        runCatching {
            val c2 = ll.getDeclaredConstructor(Array<java.util.Locale>::class.java)
            c2.isAccessible = true
            return c2.newInstance(arrayOf(loc))
        }
        return null
    }

    private fun patchConfig(obj: Any?, loc: java.util.Locale, list: Any?) {
        if (obj == null) return
        runCatching {
            val f = obj.javaClass.getDeclaredField("locale")
            f.isAccessible = true
            f.set(obj, loc)
        }
        if (list != null) {
            runCatching {
                val f = obj.javaClass.getDeclaredField("locales")
                f.isAccessible = true
                f.set(obj, list)
            }
        }
    }

    

    private fun hookTimeOffset(minutes: Int) {
        val sys = loadClassAnywhere("java.lang.System") ?: return
        val delta = minutes * 60_000L
        runCatching {
            sys.getDeclaredMethod("currentTimeMillis").let { m ->
                hookMethod(m) { chain ->
                    val t = chain.proceed() as? Long
                    if (t == null) t else t + delta
                }
            }
        }
        logInfo("time offset hooked -> ${minutes}min")
    }

    

    






    private fun hookUptime(hoursRaw: String) {
        val hours = hoursRaw.toFloatOrNull() ?: return
        FakeFiles.uptime = FakeProps.uptimeContent(hours)
        logInfo("uptime fake armed -> ${hours}h")

        uptimeTarget = (hours * 3_600_000f).toLong()
        uptimeBaseReal = android.os.SystemClock.uptimeMillis()
        val shift = { real: Long -> uptimeTarget + (real - uptimeBaseReal) }

        val sc = loadClassAnywhere("android.os.SystemClock") ?: return
        runCatching {
            sc.getDeclaredMethod("uptimeMillis").let { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed() as? Long ?: return@hookMethod null
                    shift(r)
                }
            }
        }
        runCatching {
            sc.getDeclaredMethod("elapsedRealtime").let { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed() as? Long ?: return@hookMethod null
                    shift(r)
                }
            }
        }
        runCatching {
            sc.getDeclaredMethod("elapsedRealtimeNanos").let { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed() as? Long ?: return@hookMethod null
                    shift(r) * 1_000_000L
                }
            }
        }
    }

    

    




    private fun hookAndroidVersion(cfg: XpState.Snapshot) {
        var release = cfg.buildValues["RELEASE"].orEmpty()
        var sdk = cfg.exSdkInt
        if (sdk > 0 && release.isEmpty()) release = XpConfig.releaseFor(sdk)
        if (release.isNotEmpty() && sdk <= 0) sdk = XpConfig.sdkFor(release)
        if (release.isEmpty() && sdk <= 0) return

        val ver = loadClassAnywhere("android.os.Build\$VERSION") ?: return

        if (release.isNotEmpty()) {
            setStaticString(ver, "RELEASE", release)
            runCatching { setStaticString(ver, "RELEASE_OR_CODENAME", release) }
            runCatching { setStaticString(ver, "CODENAME", "REL") }
            logInfo("android release hooked -> $release")
        }
        if (sdk > 0) {
            if (setStaticInt(ver, "SDK_INT", sdk)) {
                logInfo("sdk int hooked -> $sdk")
            } else {
                logWarn("sdk int hook failed（部分 ROM 会内联该常量）")
            }
            runCatching { setStaticString(ver, "SDK", sdk.toString()) }
            runCatching { setStaticInt(ver, "PREVIEW_SDK_INT", 0) }
        }
    }

    

    private fun hookMac(wifi: String, bt: String) {
        if (wifi.isNotEmpty()) {
            val wi = loadClassAnywhere("android.net.wifi.WifiInfo")
            wi?.declaredMethods?.filter { it.name == "getMacAddress" }
                ?.forEach { m -> hookMethod(m) { _ -> wifi } }
        }
        if (bt.isNotEmpty()) {
            val ba = loadClassAnywhere("android.bluetooth.BluetoothAdapter")
            ba?.declaredMethods?.filter { it.name == "getAddress" }
                ?.forEach { m -> hookMethod(m) { _ -> bt } }
        }
        if (wifi.isNotEmpty() || bt.isNotEmpty()) logInfo("mac hooked")
    }

    

    private fun hookKernelArch(cfg: XpState.Snapshot) {
        val kernel = FakeProps.kernelVersion(cfg)
        val arch = FakeProps.arch(cfg)
        FakeFiles.version = FakeProps.procVersion(cfg)
        if (cfg.exCpuInfoHw.isNotEmpty() || cfg.buildValues["SOC_MODEL"] != null) {
            FakeFiles.cpuinfo = FakeProps.cpuInfo(cfg)
        }

        
        
        
        runCatching { System.setProperty("os.version", kernel) }
        if (arch.isNotEmpty()) runCatching { System.setProperty("os.arch", arch) }

        val sys = loadClassAnywhere("java.lang.System") ?: return
        runCatching {
            sys.getDeclaredMethod("getProperty", String::class.java).let { m ->
                hookMethod(m) { chain ->
                    when (chain.getArg(0) as? String) {
                        "os.version" -> kernel
                        "os.arch" -> arch
                        "os.name" -> "Linux"
                        else -> chain.proceed()
                    }
                }
            }
        }
        runCatching {
            sys.getDeclaredMethod("getProperty", String::class.java, String::class.java).let { m ->
                hookMethod(m) { chain ->
                    when (chain.getArg(0) as? String) {
                        "os.version" -> kernel
                        "os.arch" -> arch
                        "os.name" -> "Linux"
                        else -> chain.proceed()
                    }
                }
            }
        }
        logInfo("kernel hooked -> $kernel / $arch")
    }

    

    private fun hookHwSerial(serial: String, buildSerial: String?) {
        if (serial.isEmpty() || !buildSerial.isNullOrEmpty()) return
        val b = loadClassAnywhere("android.os.Build") ?: return
        runCatching {
            b.getDeclaredMethod("getSerial").let { m -> hookMethod(m) { _ -> serial } }
        }
        logInfo("hardware serial hooked")
    }

    private fun hookFirebase(fid: String, iid: String) {
        if (fid.isEmpty() && iid.isEmpty()) return

        if (fid.isNotEmpty()) {
            val fi = cls("com.google.firebase.installations.FirebaseInstallations")
            val tasks = cls("com.google.android.gms.tasks.Tasks")
            if (fi != null && tasks != null) {
                runCatching {
                    val forResult = tasks.getDeclaredMethod("forResult", Object::class.java)
                    forResult.isAccessible = true
                    fi.declaredMethods.filter { it.name == "getId" }.forEach { m ->
                        hookMethod(m) { _ -> forResult.invoke(null, fid) }
                    }
                    logInfo("firebase installation id hooked")
                }
            }
        }

        val want = iid.ifEmpty { fid }
        if (want.isNotEmpty()) {
            listOf(
                "com.google.firebase.iid.FirebaseInstanceId",
                "com.google.firebase.iid.internal.FirebaseInstanceIdInternal",
            ).forEach { name ->
                val c = cls(name) ?: return@forEach
                c.declaredMethods.filter {
                    it.name == "getId" && it.returnType == String::class.java
                }.forEach { m -> hookMethod(m) { _ -> want } }
            }
            logInfo("firebase instance id hooked")
        }
    }

    private fun hookGsf(gsfId: String) {
        if (gsfId.isEmpty()) return
        listOf(
            "com.google.android.gsf.Gservices",
            "com.google.android.gsf.Gservices\$Contract",
        ).forEach { name ->
            val c = cls(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "getString" }.forEach { m ->
                hookMethod(m) { chain ->
                    val k = chain.getArg(1) as? String
                    if (k == "android_id") gsfId else chain.proceed()
                }
            }
        }
    }

    
    private val OAID_CLASSES = listOf(
        "com.android.creator.IdsSupplier",
        "com.android.creator.OaidHelper",
        "com.bun.lib.MsaIdInterface",
        "com.bun.miitmdid.core.MdidSdkHelper",
        "com.bun.miitmdid.core.OaidHelper",
        "com.bun.miitmdid.a",
        "com.bun.miitmdid.b",
        "com.bun.miitmdid.c",
        "com.zui.deviceidservice.DeviceIdManager",
        "com.heytap.openid.OpenIDManager",
    )

    private val OAID_GETTERS = setOf(
        "getOAID", "getOaid", "getOAIDSync", "getOaidSync",
        "getVAID", "getAAID", "getUDID", "getId", "getOpenId", "getOUID",
    )

    private fun hookOaid(oaid: String) {
        var n = 0
        OAID_CLASSES.forEach { name ->
            val c = cls(name) ?: return@forEach
            c.declaredMethods.forEach { m ->
                when {
                    m.name in OAID_GETTERS && m.returnType == String::class.java -> {
                        if (hookMethod(m) { _ -> oaid }) n++
                    }

                    (m.name == "isSupported" || m.name == "isSupport" || m.name == "supported") &&
                            m.returnType == java.lang.Boolean.TYPE -> {
                        if (hookMethod(m) { _ -> true }) n++
                    }
                }
            }
        }
        logInfo("oaid hooked ($n methods)")
    }

    

    private fun setStaticString(clazz: Class<*>, name: String, value: String): Boolean {
        return runCatching {
            val f = clazz.getDeclaredField(name)
            f.isAccessible = true
            clearFinal(f)
            f.set(null, value)
            true
        }.onFailure { logWarn("setStaticString $name failed: ${it.message}") }
            .getOrDefault(false)
    }

    private fun setStaticInt(clazz: Class<*>, name: String, value: Int): Boolean {
        return runCatching {
            val f = clazz.getDeclaredField(name)
            f.isAccessible = true
            clearFinal(f)
            f.setInt(null, value)
            true
        }.onFailure { logWarn("setStaticInt $name failed: ${it.message}") }
            .getOrDefault(false)
    }

    private fun clearFinal(f: Field) {
        runCatching {
            val m = Field::class.java.getDeclaredField("modifiers")
            m.isAccessible = true
            m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        }.onFailure {
            runCatching {
                val m = Field::class.java.getDeclaredField("accessFlags")
                m.isAccessible = true
                m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
            }
        }
    }
}
