package io.github.sunilxsk.lockperm

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean
















internal class DeviceAdminDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun install() {
        val cfg = snapshot()
        
        if (!cfg.daEnable && !XpState.Flags.forceDeviceAdmin) return

        hookPolicyApis()
        hookReceiverCallbacks()
        hookActivationRequest()

        if (cfg.daMaster || XpState.Flags.forceDeviceAdmin) startRemoveLoop()

        logInfo("device admin defender installed (master=${cfg.daMaster})")
    }

    

    
    private fun blocked(key: String): Boolean {
        val cfg = snapshot()
        if (XpState.Flags.forceDeviceAdmin) return true      
        if (!cfg.daEnable) return false
        if (cfg.daMaster) return true
        return when (key) {
            XpConfig.KEY_DA_LOCK -> cfg.daLock
            XpConfig.KEY_DA_PASSWORD -> cfg.daPassword
            XpConfig.KEY_DA_WIPE -> cfg.daWipe
            XpConfig.KEY_DA_CAMERA -> cfg.daCamera
            XpConfig.KEY_DA_APPMGMT -> cfg.daAppMgmt
            XpConfig.KEY_DA_SYSTEM -> cfg.daSystem
            XpConfig.KEY_DA_PERMISSION -> cfg.daPermission
            XpConfig.KEY_DA_ENCRYPT -> cfg.daEncrypt
            else -> false
        }
    }

    

    private fun hookPolicyApis() {
        val dpm = cls("android.app.admin.DevicePolicyManager") ?: return

        
        blockAll(
            dpm, XpConfig.KEY_DA_LOCK,
            "lockNow", "lockNowForUser",
            "setMaximumTimeToLock", "getMaximumTimeToLock", "setMaximumTimeToLockForUser",
            "setKeyguardDisabled", "setRequiredStrongAuthTimeout", "getRequiredStrongAuthTimeout",
            "setDeviceOwnerLockScreenInfo",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_PASSWORD,
            "resetPassword", "resetPasswordWithToken", "setResetPasswordToken",
            "clearResetPasswordToken", "isResetPasswordTokenActive",
            "setPasswordQuality", "getPasswordQuality",
            "setPasswordMinimumLength", "getPasswordMinimumLength",
            "setPasswordMinimumUpperCase", "getPasswordMinimumUpperCase",
            "setPasswordMinimumLowerCase", "getPasswordMinimumLowerCase",
            "setPasswordMinimumLetters", "getPasswordMinimumLetters",
            "setPasswordMinimumNumeric", "getPasswordMinimumNumeric",
            "setPasswordMinimumSymbols", "getPasswordMinimumSymbols",
            "setPasswordMinimumNonLetter", "getPasswordMinimumNonLetter",
            "setPasswordHistoryLength", "getPasswordHistoryLength",
            "setPasswordExpirationTimeout", "getPasswordExpirationTimeout",
            "setPasswordExpiration", "getPasswordExpiration",
            "setMaximumFailedPasswordsForWipe", "getMaximumFailedPasswordsForWipe",
            "getCurrentFailedPasswordAttempts", "getPasswordMaximumLength",
            "isActivePasswordSufficient", "setRequiredPasswordComplexity",
            "getPasswordComplexity", "getPasswordComplexityForUser",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_WIPE,
            "wipeData", "wipeDataWithReason", "wipeDevice", "wipeProfile",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_CAMERA,
            "setCameraDisabled", "getCameraDisabled", "setCameraAccessType",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_APPMGMT,
            "clearApplicationUserData", "setApplicationHidden", "isApplicationHidden",
            "setUninstallBlocked", "isUninstallBlocked",
            "setKeepUninstalledPackages", "getKeepUninstalledPackages",
            "setApplicationRestrictions", "getApplicationRestrictions",
            "setApplicationRestrictionsManagingPackage",
            "setApplicationExemptions", "getApplicationExemptions",
            "installSystemUpdate", "installExistingPackage", "installSystemUpdateForUser",
            "setPackagesSuspended", "getPackagesSuspended", "setPackagesSuspendedForUser",
            "enableSystemApp", "disableSystemApp", "setSystemAppUpdateDisabledApps",
            "setMeteredDataDisabledPackages", "setAppFunctionsPolicy",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_SYSTEM,
            
            "setGlobalSetting", "setSecureSetting", "setSystemSetting",
            "setGlobalPrivateDnsMode", "setGlobalPrivateDnsModeHost",
            "setAutoTimeRequired", "setAutoTimeEnabled", "setAutoTimePolicy",
            "setAutoTimeZoneEnabled", "setAutoTimeZonePolicy",
            
            "addUserRestriction", "clearUserRestriction", "addCrossProfileUserRestriction",
            "setUserRestriction", "getUserRestrictions",
            "setLockTaskPackages", "getLockTaskPackages", "setLockTaskFeatures",
            "getLockTaskFeatures", "startLockTask", "stopLockTask", "isLockTaskPermitted",
            "setKeyguardDisabledFeatures", "getKeyguardDisabledFeatures",
            "setStatusBarDisabled", "setMasterVolumeMuted",
            
            "setRecommendedGlobalProxy", "setAlwaysOnVpnPackage", "getAlwaysOnVpnPackage",
            "setConfiguredNetworksLockdownState", "setNetworkLoggingEnabled", "getNetworkLogs",
            "retrieveNetworkLogs", "setWifiEnabled", "setWifiSsidPolicy",
            "setMinimumRequiredWifiSecurityLevel", "setPreferentialNetworkServiceEnabled",
            
            "setScreenCaptureDisabled", "getScreenCaptureDisabled",
            "setUsbDataSignalingEnabled", "isUsbDataSignalingEnabled",
            "setBluetoothContactSharingDisabled",
            "setNearbyAppStreamingPolicy", "setNearbyNotificationStreamingPolicy",
            "setContentProtectionPolicy", "setMicrophoneDisabled",
            
            "setUserIcon", "setStartUserSessionMessage", "setEndUserSessionMessage",
            "setMtePolicy", "setCommonCriteriaModeEnabled",
            "createAndManageUser", "removeUser", "switchUser", "setProfileEnabled",
            "setProfileName", "setAffiliationIds", "setLogoutEnabled",
            "setSecurityLoggingEnabled", "retrieveSecurityLogs", "retrievePreRebootSecurityLogs",
            "setSystemUpdatePolicy", "getSystemUpdatePolicy",
            "setFactoryResetProtectionPolicy",
            "setCrossProfileCallerIdDisabled", "setCrossProfileContactsSearchDisabled",
            "setCrossProfilePackages", "setCrossProfileCalendarPackages",
            "addCrossProfileIntentFilter", "clearCrossProfileIntentFilters",
            "setAccountManagementDisabled", "getAccountTypesWithManagementDisabled",
            "setBackupServiceEnabled", "isBackupServiceEnabled",
            "setOrganizationName", "setOrganizationColor", "setShortSupportMessage",
            "setLongSupportMessage",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_PERMISSION,
            "setPermissionPolicy", "getPermissionPolicy",
            "setPermissionGrantState", "getPermissionGrantState",
            "setDefaultSmsApplication", "setDefaultDialerApplication",
            "setDelegatedScopes", "getDelegatedScopes",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_ENCRYPT,
            "setStorageEncryption", "getStorageEncryption", "getStorageEncryptionStatus",
            "requestStorageEncryption", "setRequireStorageEncryption",
        )

        
        runCatching {
            val m = dpm.getDeclaredMethod("isAdminActive", ComponentName::class.java)
            hookMethod(m) { chain ->
                val cfg = snapshot()
                if (cfg.daEnable && cfg.daMaster) {
                    logWarn("isAdminActive -> false (blocked)")
                    false
                } else {
                    chain.proceed()
                }
            }
        }
        runCatching {
            val m = dpm.getDeclaredMethod("getActiveAdmins")
            hookMethod(m) { chain ->
                val cfg = snapshot()
                if (cfg.daEnable && cfg.daMaster) {
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    
    private fun blockAll(clazz: Class<*>, key: String, vararg names: String) {
        val targets = names.toSet()
        clazz.declaredMethods
            .filter { it.name in targets }
            .forEach { m ->
                hookMethod(m) { chain ->
                    if (blocked(key)) {
                        logWarn("blocked DevicePolicyManager.${m.name}")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }
            }
    }

    

    private fun hookReceiverCallbacks() {
        val receiver = cls("android.app.admin.DeviceAdminReceiver")
            ?: runCatching { Class.forName("android.app.admin.DeviceAdminReceiver") }.getOrNull()
            ?: return

        
        listOf(
            "onPasswordFailed", "onPasswordSucceeded", "onPasswordChanged", "onPasswordExpiring"
        ).forEach { name ->
            val m: Method = runCatching { receiver.getDeclaredMethod(name, Context::class.java, Intent::class.java) }
                .getOrNull()
                ?: runCatching { receiver.getDeclaredMethod(name) }.getOrNull()
                ?: return@forEach
            hookMethod(m) { chain ->
                if (blocked(XpConfig.KEY_DA_PASSWORD)) {
                    logWarn("blocked DeviceAdminReceiver.$name")
                    null
                } else {
                    chain.proceed()
                }
            }
        }

        
        val onEnabled = runCatching {
            receiver.getDeclaredMethod("onEnabled", Context::class.java, Intent::class.java)
        }.getOrNull()
        if (onEnabled != null) {
            hookMethod(onEnabled) { chain ->
                val result = chain.proceed()
                val cfg = snapshot()
                if (cfg.daEnable && cfg.daMaster) {
                    mainHandler.post { tryRemoveActiveAdmin() }
                }
                result
            }
        }
    }

    

    private fun hookActivationRequest() {
        val activity = cls("android.app.Activity") ?: return
        val names = setOf("startActivity", "startActivityForResult", "startActivityIfNeeded", "startActivityFromChild")
        activity.declaredMethods.filter { it.name in names }.forEach { m ->
            hookMethod(m) { chain ->
                val intent = chain.args.filterIsInstance<Intent>().firstOrNull()
                val cfg = snapshot()
                if (intent != null && isAdminIntent(intent) && cfg.daEnable && cfg.daMaster) {
                    logWarn("blocked device admin activation request: ${intent.action}")
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    private fun isAdminIntent(intent: Intent): Boolean {
        val action = intent.action
        if (action == DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN) return true
        if (action == ACTION_PROVISION_MANAGED_DEVICE) return true
        if (action == ACTION_PROVISION_MANAGED_PROFILE) return true
        if (action == ACTION_SET_PROFILE_OWNER) return true
        return runCatching {
            intent.hasExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN) ||
                    intent.hasExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME)
        }.getOrDefault(false)
    }

    

    private fun startRemoveLoop() {
        if (!Holder.loopStarted.compareAndSet(false, true)) return
        val tick = object : Runnable {
            override fun run() {
                val cfg = snapshot()
                val on = XpState.Flags.forceDeviceAdmin || (cfg.daEnable && cfg.daMaster)
                if (!on) {
                    Holder.loopStarted.set(false)
                    return
                }
                tryRemoveActiveAdmin()
                mainHandler.postDelayed(this, REMOVE_INTERVAL_MS)
            }
        }
        mainHandler.postDelayed(tick, 1500)
    }

    
    private fun tryRemoveActiveAdmin() {
        Thread {
            runCatching {
                val app = currentApplication() ?: return@runCatching
                val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                    ?: return@runCatching
                val pkg = XpState.packageName
                if (pkg.isEmpty()) return@runCatching

                
                runCatching {
                    val clearOwner = dpm.javaClass.getMethod("clearDeviceOwnerApp", String::class.java)
                    clearOwner.invoke(dpm, pkg)
                }

                
                val admins = runCatching { dpm.activeAdmins }.getOrNull()
                admins?.filter { it.packageName == pkg }?.forEach { admin ->
                    repeat(3) {
                        runCatching { dpm.removeActiveAdmin(admin) }
                        runCatching { Thread.sleep(120) }
                    }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun currentApplication(): android.app.Application? = runCatching {
        val clazz = Class.forName("android.app.ActivityThread", false, classLoader)
        val m = clazz.getDeclaredMethod("currentApplication")
        m.isAccessible = true
        m.invoke(null) as? android.app.Application
    }.getOrNull()

    companion object {
        private const val REMOVE_INTERVAL_MS = 4000L

        private const val ACTION_PROVISION_MANAGED_DEVICE =
            "android.app.action.PROVISION_MANAGED_DEVICE"
        private const val ACTION_PROVISION_MANAGED_PROFILE =
            "android.app.action.PROVISION_MANAGED_PROFILE"
        private const val ACTION_SET_PROFILE_OWNER =
            "android.app.action.SET_PROFILE_OWNER"
        private const val EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"

        private object Holder {
            val loopStarted = AtomicBoolean(false)
        }
    }
}
