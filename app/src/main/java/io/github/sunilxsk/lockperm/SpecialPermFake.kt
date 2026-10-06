package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method














internal class SpecialPermFake(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    






    private val OP_TO_GROUP: Map<String, String> = mapOf(
        
        "android:get_usage_stats" to XpConfig.PERM_USAGE_STATS,
        "android:system_alert_window" to XpConfig.PERM_DRAW_OVERLAY,
        "android:write_settings" to XpConfig.PERM_WRITE_SETTINGS,
        "android:request_install_packages" to XpConfig.PERM_INSTALL_UNKNOWN,
        "android:manage_external_storage" to XpConfig.PERM_MANAGE_STORAGE,
        "android:schedule_exact_alarm" to XpConfig.PERM_EXACT_ALARM,
        "android:use_exact_alarm" to XpConfig.PERM_EXACT_ALARM,
        "android:boot_completed" to XpConfig.PERM_AUTOSTART,
        "android:post_notification" to XpConfig.PERM_NOTIFICATION,
        "android:access_notifications" to XpConfig.PERM_NOTIFICATION_LISTENER,
        "android:activate_vpn" to XpConfig.PERM_VPN,
        "android:manage_media" to XpConfig.PERM_MANAGE_MEDIA,

        
        "android:read_contacts" to XpConfig.PERM_CONTACTS,
        "android:write_contacts" to XpConfig.PERM_CONTACTS,
        "android:get_accounts" to XpConfig.PERM_CONTACTS,
        "android:read_sms" to XpConfig.PERM_SMS,
        "android:write_sms" to XpConfig.PERM_SMS,
        "android:send_sms" to XpConfig.PERM_SMS,
        "android:receive_sms" to XpConfig.PERM_SMS,
        "android:read_call_log" to XpConfig.PERM_CALL_LOG,
        "android:write_call_log" to XpConfig.PERM_CALL_LOG,
        "android:read_phone_state" to XpConfig.PERM_PHONE,
        "android:read_phone_numbers" to XpConfig.PERM_PHONE,
        "android:call_phone" to XpConfig.PERM_PHONE,
        "android:answer_phone_calls" to XpConfig.PERM_PHONE,
        "android:add_voicemail" to XpConfig.PERM_PHONE,
        "android:use_sip" to XpConfig.PERM_PHONE,
        "android:process_outgoing_calls" to XpConfig.PERM_PHONE,
        "android:fine_location" to XpConfig.PERM_LOCATION,
        "android:coarse_location" to XpConfig.PERM_LOCATION,
        "android:read_calendar" to XpConfig.PERM_CALENDAR,
        "android:write_calendar" to XpConfig.PERM_CALENDAR,
        "android:camera" to XpConfig.PERM_CAMERA,
        "android:record_audio" to XpConfig.PERM_MICROPHONE,
        "android:body_sensors" to XpConfig.PERM_SENSORS,
        "android:activity_recognition" to XpConfig.PERM_ACTIVITY,
        "android:read_external_storage" to XpConfig.PERM_STORAGE,
        "android:write_external_storage" to XpConfig.PERM_STORAGE,
        "android:read_media_images" to XpConfig.PERM_STORAGE,
        "android:read_media_video" to XpConfig.PERM_STORAGE,
        "android:read_media_audio" to XpConfig.PERM_STORAGE,
        "android:access_media_location" to XpConfig.PERM_STORAGE,
        
        "android:bluetooth_scan" to XpConfig.PERM_NEARBY,
        "android:bluetooth_connect" to XpConfig.PERM_NEARBY,
        "android:bluetooth_advertise" to XpConfig.PERM_NEARBY,
        "android:nearby_wifi_devices" to XpConfig.PERM_NEARBY,
        "android:uwb_ranging" to XpConfig.PERM_NEARBY,
    )

    
    private val RUNTIME_OP_TO_GROUP = HashMap<String, String>()

    fun install() {
        buildPermToOp()
        hookAppOps()
        hookOverlay()
        hookInstallUnknown()
        hookManageStorage()
        hookManageMedia()
        hookExactAlarm()
        hookBatteryOptimization()
        hookVpn()
        hookWriteSettings()
        hookNotification()
        hookFullScreenIntent()
        hookNotificationListener()
        hookDeviceAdmin()
        hookSettingsString()
        hookAccessibilityManager()
        val cfg = snapshot()
        logInfo(
            "special permission fake installed (grant=${cfg.effectiveGrant().size} 组, " +
                    "appConfig=${cfg.permGrantApp != null})"
        )
    }

    



    private fun on(groupId: String): Boolean {
        val cfg = snapshot()
        if (!cfg.permEnable) return false
        return groupId in cfg.effectiveGrant()
    }

    

    




    private val APPOPS_METHODS = setOf(
        "checkOp", "checkOpNoThrow", "checkOpRawNoThrow",
        "noteOp", "noteOpNoThrow",
        "startOp", "startOpNoThrow",
        "unsafeCheckOp", "unsafeCheckOpNoThrow",
        "unsafeCheckOpRaw", "unsafeCheckOpRawNoThrow",
        "startProxyOp", "startProxyOpNoThrow",
        "noteProxyOp", "noteProxyOpNoThrow",
    )

    




    private fun buildPermToOp() {
        val appOps = frameworkCls("android.app.AppOpsManager") ?: return
        val m = runCatching {
            appOps.getDeclaredMethod("permissionToOp", String::class.java)
        }.getOrNull() ?: return
        runCatching { m.isAccessible = true }
        
        val inst = runCatching { appContext()?.getSystemService("appops") }.getOrNull()
        var n = 0
        for (g in XpConfig.PERM_GROUPS) {
            if (g.special) continue
            for (perm in g.perms) {
                val op = runCatching { m.invoke(inst, perm) as? String }.getOrNull()
                    ?: runCatching { m.invoke(null, perm) as? String }.getOrNull()
                if (!op.isNullOrBlank() && RUNTIME_OP_TO_GROUP[op] == null) {
                    RUNTIME_OP_TO_GROUP[op] = g.id
                    n++
                }
            }
        }
        if (n > 0) logInfo("permissionToOp 解析到 $n 条 AppOp 映射")
    }

    private fun hookAppOps() {
        val appOps = frameworkCls("android.app.AppOpsManager") ?: return
        appOps.declaredMethods
            .filter { m -> m.name in APPOPS_METHODS }
            .forEach { m ->
                hookMethod(m) { chain ->
                    val group = opGroupOf(m, chain)
                    if (group != null && on(group)) {
                        logWarn("appops ${m.name} -> allowed ($group)")
                        MODE_ALLOWED
                    } else {
                        chain.proceed()
                    }
                }
            }
    }

    
    private fun opGroupOf(m: Method, chain: io.github.libxposed.api.XposedInterface.Chain): String? {
        val args = chain.args

        for (a in args) {
            if (a is String) {
                OP_TO_GROUP[a]?.let { return it }
                
                RUNTIME_OP_TO_GROUP[a]?.let { return it }
            }
        }
        
        val op = args.filterIsInstance<Int>().firstOrNull() ?: return null
        return when (op) {
            OP_SYSTEM_ALERT_WINDOW -> XpConfig.PERM_DRAW_OVERLAY
            OP_WRITE_SETTINGS -> XpConfig.PERM_WRITE_SETTINGS
            OP_GET_USAGE_STATS -> XpConfig.PERM_USAGE_STATS
            OP_REQUEST_INSTALL_PACKAGES -> XpConfig.PERM_INSTALL_UNKNOWN
            OP_MANAGE_EXTERNAL_STORAGE -> XpConfig.PERM_MANAGE_STORAGE
            OP_SCHEDULE_EXACT_ALARM -> XpConfig.PERM_EXACT_ALARM
            OP_BOOT_COMPLETED -> XpConfig.PERM_AUTOSTART
            else -> null
        }
    }

    

    private fun hookOverlay() {
        runCatching {
            val settings = frameworkCls("android.provider.Settings") ?: return@runCatching
            val canDraw = frameworkCls("android.provider.Settings\$Secure")?.let {
                runCatching { it.getDeclaredMethod("canDrawOverlays", android.content.Context::class.java) }.getOrNull()
            } ?: runCatching { settings.getDeclaredMethod("canDrawOverlays", android.content.Context::class.java) }.getOrNull()
            if (canDraw != null) {
                hookMethod(canDraw) { chain ->
                    if (on(XpConfig.PERM_DRAW_OVERLAY)) true else chain.proceed()
                }
            }
        }
    }

    

    private fun hookInstallUnknown() {
        val pm = frameworkCls("android.content.pm.PackageManager") ?: return
        runCatching {
            val m = pm.getDeclaredMethod("canRequestPackageInstalls")
            hookMethod(m) { chain ->
                if (on(XpConfig.PERM_INSTALL_UNKNOWN)) true else chain.proceed()
            }
        }
        val apm = frameworkCls("android.app.ApplicationPackageManager")
        if (apm != null) {
            runCatching {
                val m = apm.getDeclaredMethod("canRequestPackageInstalls")
                hookMethod(m) { chain ->
                    if (on(XpConfig.PERM_INSTALL_UNKNOWN)) true else chain.proceed()
                }
            }
        }
    }

    

    private fun hookManageStorage() {
        runCatching {
            val env = frameworkCls("android.os.Environment") ?: return@runCatching
            val m = env.getDeclaredMethod("isExternalStorageManager")
            hookMethod(m) { chain ->
                if (on(XpConfig.PERM_MANAGE_STORAGE)) true else chain.proceed()
            }
        }
    }

    

    
    private fun hookManageMedia() {
        runCatching {
            val ms = frameworkCls("android.provider.MediaStore") ?: return@runCatching
            val m = ms.getDeclaredMethod("canManageMedia", android.content.Context::class.java)
            hookMethod(m) { chain ->
                if (on(XpConfig.PERM_MANAGE_MEDIA)) true else chain.proceed()
            }
        }
    }

    private fun hookExactAlarm() {
        runCatching {
            val am = frameworkCls("android.app.AlarmManager") ?: return@runCatching
            runCatching {
                val m = am.getDeclaredMethod("canScheduleExactAlarms")
                hookMethod(m) { chain ->
                    if (on(XpConfig.PERM_EXACT_ALARM)) true else chain.proceed()
                }
            }
        }
    }

    

    private fun hookBatteryOptimization() {
        runCatching {
            val pm = frameworkCls("android.os.PowerManager") ?: return@runCatching
            val m = pm.getDeclaredMethod("isIgnoringBatteryOptimizations", String::class.java)
            hookMethod(m) { chain ->
                val pkg = chain.getArg(0) as? String
                if (on(XpConfig.PERM_BATTERY_OPT) && pkg == XpState.packageName) {
                    true
                } else {
                    chain.proceed()
                }
            }
        }
    }

    

    private fun hookVpn() {
        runCatching {
            val vpn = frameworkCls("android.net.VpnService") ?: return@runCatching
            
            val m = vpn.getDeclaredMethod("prepare", android.content.Context::class.java)
            hookMethod(m) { chain ->
                if (on(XpConfig.PERM_VPN)) null else chain.proceed()
            }
        }
    }

    

    private fun hookWriteSettings() {
        runCatching {
            val sys = frameworkCls("android.provider.Settings\$System") ?: return@runCatching
            val m = sys.getDeclaredMethod("canWrite", android.content.Context::class.java)
            hookMethod(m) { chain ->
                if (on(XpConfig.PERM_WRITE_SETTINGS)) true else chain.proceed()
            }
        }
    }

    

    private fun hookNotification() {
        runCatching {
            val nm = frameworkCls("android.app.NotificationManager") ?: return@runCatching
            runCatching {
                val m = nm.getDeclaredMethod("areNotificationsEnabled")
                hookMethod(m) { chain ->
                    if (on(XpConfig.PERM_NOTIFICATION)) true else chain.proceed()
                }
            }
        }
    }

    

    
    private fun hookNotificationListener() {
        runCatching {
            val nm = frameworkCls("android.app.NotificationManager") ?: return@runCatching
            nm.declaredMethods.filter {
                it.name == "isNotificationListenerAccessGranted" ||
                        it.name == "getEnabledNotificationListeners"
            }.forEach { m ->
                val forComponent = m.parameterTypes.any { it.name == "android.content.ComponentName" }
                hookMethod(m) { chain ->
                    if (!on(XpConfig.PERM_NOTIFICATION_LISTENER)) return@hookMethod chain.proceed()
                    
                    if (forComponent) {
                        val cn = chain.args.filterIsInstance<android.content.ComponentName>()
                            .firstOrNull()
                        if (cn?.packageName != XpState.packageName) return@hookMethod chain.proceed()
                    }
                    logWarn("noti listener ${m.name} -> 伪装已授权")
                    if (m.returnType == java.lang.Boolean.TYPE) true else chain.proceed()
                }
            }
        }
    }

    
    private fun hookFullScreenIntent() {
        runCatching {
            val nm = frameworkCls("android.app.NotificationManager") ?: return@runCatching
            nm.declaredMethods
                .filter { it.name == "canUseFullScreenIntent" }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        if (on(XpConfig.PERM_FULL_SCREEN_INTENT)) true else chain.proceed()
                    }
                }
        }
    }

    private fun hookDeviceAdmin() {
        runCatching {
            val dpm = frameworkCls("android.app.admin.DevicePolicyManager") ?: return@runCatching
            val m = dpm.getDeclaredMethod("isAdminActive", android.content.ComponentName::class.java)
            hookMethod(m) { chain ->
                val cn = chain.getArg(0) as? android.content.ComponentName
                if (on(XpConfig.PERM_DEVICE_ADMIN) && cn?.packageName == XpState.packageName) {
                    true
                } else {
                    chain.proceed()
                }
            }
        }
    }

    




    private fun hookSettingsString() {
        val targets = setOf(
            "enabled_accessibility_services",
            "enabled_notification_listeners",
        )
        listOf(
            "android.provider.Settings\$Secure",
            "android.provider.Settings\$Global",
        ).forEach { name ->
            val holder = cls(name) ?: return@forEach
            holder.declaredMethods.filter { it.name == "getString" }.forEach { m ->
                hookMethod(m) { chain ->
                    val result = chain.proceed()
                    val key = chain.args.filterIsInstance<String>().firstOrNull()
                    if (key !in targets) return@hookMethod result

                    val pkg = XpState.packageName
                    val want = when (key) {
                        "enabled_accessibility_services" -> on(XpConfig.PERM_ACCESSIBILITY)
                        else -> on(XpConfig.PERM_NOTIFICATION_LISTENER)
                    }
                    if (!want || pkg.isEmpty() || result != null && result.toString().contains(pkg)) {
                        return@hookMethod result
                    }
                    val base = result?.toString().orEmpty()
                    val injected = listOf(base, pkg, "$pkg/.AccessibilityService")
                        .filter { it.isNotBlank() }
                        .joinToString(":")
                    logWarn("injected $key for $pkg")
                    injected
                }
            }
        }
    }

    
    private fun hookAccessibilityManager() {
        runCatching {
            val am = frameworkCls("android.view.accessibility.AccessibilityManager") ?: return@runCatching
            runCatching {
                val m = am.getDeclaredMethod("isEnabled")
                hookMethod(m) { chain ->
                    if (on(XpConfig.PERM_ACCESSIBILITY)) true else chain.proceed()
                }
            }
            
            am.declaredMethods
                .filter { it.name == "getEnabledAccessibilityServiceList" }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on(XpConfig.PERM_ACCESSIBILITY)) return@hookMethod chain.proceed()
                        val result = chain.proceed()
                        val list = result as? List<*> ?: return@hookMethod result
                        val pkg = XpState.packageName
                        if (pkg.isEmpty()) return@hookMethod result
                        val has = list.any {
                            runCatching {
                                val info = it as? android.accessibilityservice.AccessibilityServiceInfo
                                info?.resolveInfo?.serviceInfo?.packageName == pkg
                            }.getOrNull() ?: false
                        }
                        if (has) return@hookMethod result
                        val fake = buildFakeServiceInfo(pkg) ?: return@hookMethod result
                        logWarn("accessibility service list -> 注入本应用")
                        ArrayList<Any>(list.filterNotNull()).apply { add(fake) }
                    }
                }
        }
    }

    






    private fun buildFakeServiceInfo(pkg: String): Any? {
        val ri = android.content.pm.ResolveInfo().apply {
            serviceInfo = android.content.pm.ServiceInfo().apply {
                packageName = pkg
                name = "$pkg.FakeAccessibilityService"
            }
        }
        val asi = android.accessibilityservice.AccessibilityServiceInfo()
        val cls = asi.javaClass

        for (fname in listOf("resolveInfo", "mResolveInfo")) {
            val f = runCatching { cls.getDeclaredField(fname) }.getOrNull() ?: continue
            runCatching {
                f.isAccessible = true
                f.set(asi, ri)
                return asi
            }
        }
        runCatching {
            cls.getDeclaredMethod("setResolveInfo", android.content.pm.ResolveInfo::class.java)
                .apply { isAccessible = true }
                .invoke(asi, ri)
            return asi
        }
        return null
    }

    companion object {
        private const val MODE_ALLOWED = 0

        private const val OP_SYSTEM_ALERT_WINDOW = 24
        private const val OP_WRITE_SETTINGS = 23
        private const val OP_GET_USAGE_STATS = 43
        private const val OP_REQUEST_INSTALL_PACKAGES = 66
        private const val OP_MANAGE_EXTERNAL_STORAGE = 92
        private const val OP_SCHEDULE_EXACT_ALARM = 89
        private const val OP_BOOT_COMPLETED = 68
    }
}
