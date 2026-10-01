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
        "android:boot_completed" to XpConfig.PERM_AUTOSTART,
        "android:post_notification" to XpConfig.PERM_NOTIFICATION,
    )

    fun install() {
        hookAppOps()
        hookOverlay()
        hookInstallUnknown()
        hookManageStorage()
        hookExactAlarm()
        hookBatteryOptimization()
        hookVpn()
        hookWriteSettings()
        hookNotification()
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

    

    private fun hookAppOps() {
        val appOps = frameworkCls("android.app.AppOpsManager") ?: return
        appOps.declaredMethods
            .filter { m ->
                m.name == "checkOp" || m.name == "checkOpNoThrow" ||
                        m.name == "noteOp" || m.name == "noteOpNoThrow" ||
                        m.name == "startOp" || m.name == "startOpNoThrow" ||
                        m.name == "unsafeCheckOp" || m.name == "unsafeCheckOpNoThrow" ||
                        m.name == "checkOpNoThrowRaw"
            }
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
        }
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
