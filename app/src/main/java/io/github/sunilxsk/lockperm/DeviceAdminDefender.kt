package io.github.sunilxsk.lockperm

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.lang.reflect.Method
import android.os.Handler
import android.os.Looper
import io.github.libxposed.api.XposedModule
import java.util.concurrent.atomic.AtomicBoolean
















internal class DeviceAdminDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val mainHandler = Handler(Looper.getMainLooper())

    




    private val internalCall: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    private fun <T> withInternalBypass(block: () -> T): T {
        val prev = internalCall.get() == true
        internalCall.set(true)
        return try {
            block()
        } finally {
            internalCall.set(prev)
        }
    }

    fun install() {
        val cfg = snapshot()

        
        
        
        if (cfg.daBlockRequest) hookActivationRequest()

        if (!cfg.daEnable && !XpState.Flags.forceDeviceAdmin) return
        INSTANCE = this

        
        
        if (hooksOn()) {
            hookPolicyApis()
            hookReceiverCallbacks()
            hookOwnershipQueries()
            hookPolicyBinder()
            hookPackageScanBypass()
            hookDeviceOwnerSettings()
        }
        
        
        
        if (removalOn()) startRemoveLoop()

        logInfo(
            "device admin defender installed (master=${cfg.daMaster}, " +
                "scope=${cfg.daScope}, " +
                "mode=${cfg.daFakeMode}${if (fakeMode()) " 伪装成功" else " 默认"})"
        )
    }

    
    private fun hooksOn(): Boolean =
        snapshot().daScope != XpConfig.DA_SCOPE_CLOSE_ONLY

    




    private fun removalWanted(): Boolean =
        snapshot().daScope != XpConfig.DA_SCOPE_HOOK_ONLY

    
    private fun closeEnabled(): Boolean =
        snapshot().daMaster || XpState.Flags.forceDeviceAdmin

    




    private fun closeContinuously(): Boolean {
        val cfg = snapshot()
        return cfg.daCloseMode == XpConfig.DA_CLOSE_CONTINUOUS ||
            !cfg.exitEnable
    }

    private fun removalOn(): Boolean =
        removalWanted() && closeEnabled() && closeContinuously()

    

    
    



    private fun ownerOff(): Boolean {
        
        if (fakeMode()) return false
        
        if (!hooksOn()) return false
        return XpState.Flags.forceDeviceAdmin ||
            snapshot().let { it.daEnable && it.daMaster }
    }

    
    private fun fakeMode(): Boolean =
        snapshot().daFakeMode == XpConfig.DA_MODE_FAKE_SUCCESS

    






    private fun successFor(m: Method): Any? {
        val t = m.returnType
        return when {
            t == java.lang.Boolean.TYPE -> true
            t == java.lang.Integer.TYPE -> 1
            t == java.lang.Long.TYPE -> 1L
            t == java.lang.Float.TYPE -> 1f
            t == java.lang.Double.TYPE -> 1.0
            t == java.lang.Short.TYPE -> 1.toShort()
            t == java.lang.Byte.TYPE -> 1.toByte()
            t == String::class.java -> ""
            t == Void.TYPE -> null
            t.isArray -> runCatching { java.lang.reflect.Array.newInstance(t.componentType, 0) }
                .getOrNull()
            List::class.java.isAssignableFrom(t) ||
                java.util.Collection::class.java.isAssignableFrom(t) ->
                java.util.ArrayList<Any>()
            else -> null
        }
    }

    
    private fun fakeAdminComponent(): ComponentName? {
        val pkg = XpState.packageName
        if (pkg.isBlank()) return null
        return runCatching { ComponentName(pkg, "$pkg.DeviceAdminReceiver") }.getOrNull()
    }

    private fun blocked(key: String): Boolean {
        val cfg = snapshot()
        if (!hooksOn()) return false
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
            "setDeviceOwnerLockScreenInfo", "getDeviceOwnerLockScreenInfo",
            
            "setSecureLockScreenDisabled", "setKeyguardPresentationDisabled",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_PASSWORD,
            "resetPassword", "resetPasswordWithToken", "setResetPasswordToken",
            "clearResetPasswordToken", "isResetPasswordTokenActive",
            "setPasswordQuality", "getPasswordQuality",
            "setPasswordMaximumLength",
            "getRequiredPasswordComplexity",
            "isActivePasswordSufficientForDeviceRequirement",
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
            
            "setPasswordMinimumMetrics", "setRequiredPasswordFlags",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_WIPE,
            "wipeData", "wipeDataWithReason", "wipeDevice", "wipeDeviceWithReason",
            "wipeProfile",
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
            
            "isPackageSuspended", "getUnsuspendablePackages",
            "setPersonalAppsSuspended", "getPersonalAppsSuspendedReasons",
            "setUserControlDisabledPackages", "getUserControlDisabledPackages",
            "setProtectedPackages", "getProtectedPackages",
            "setApplicationExecutablePolicy", "getApplicationExecutablePolicy",
            
            "setPermittedInputMethods", "getPermittedInputMethods",
            "setPermittedAccessibilityServices", "getPermittedAccessibilityServices",
            "isAccessibilityServicePermittedByAdmin", "isInputMethodPermittedByAdmin",
            
            "uninstallPackage",
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
            
            "isStatusBarDisabled", "isMasterVolumeMuted", "isSecurityLoggingEnabled",
            "isNetworkLoggingEnabled", "isCommonCriteriaModeEnabled",
            "isLogoutEnabled", "isManagedProfile", "isEphemeralUser",
            "isOrganizationOwnedDeviceWithManagedProfile", "isAffiliatedUser",
            "listForegroundAffiliatedUsers", "isDeviceFinanced", "getEnrollmentSpecificId",
            "isDeviceIdAttestationSupported", "isUniqueDeviceAttestationSupported",
            "isUsbMassStorageEnabled", "isPreferentialNetworkServiceEnabled",
            "isOverrideApnEnabled", "hasLockdownAdminConfiguredNetworks",
            "getAutoTimeEnabled", "getAutoTimeZoneEnabled",
            "getAutoTimePolicy", "getAutoTimeZonePolicy",
            "getGlobalPrivateDnsMode", "getGlobalPrivateDnsHost",
            "getAlwaysOnVpnLockdownEnabled", "getAlwaysOnVpnLockdownWhitelist",
            "getPreferentialNetworkServiceConfigs", "getOverrideApns",
            "getNearbyAppStreamingPolicy", "getNearbyNotificationStreamingPolicy",
            "getMinimumRequiredWifiSecurityLevel", "getWifiSsidPolicy",
            "getContentProtectionPolicy", "getMtePolicy", "getDnsPolicy",
            "getManagedSubscriptionsPolicy", "getCredentialManagerPolicy",
            "getPendingSystemUpdate", "getFactoryResetProtectionPolicy",
            "getOrganizationName", "getOrganizationColor",
            "getLongSupportMessage", "getShortSupportMessage",
            "getDevicePolicyManagementRoleHolderPackage", "getParentProfileInstance",
            "getSecondaryUsers", "getBindDeviceAdminTargetUsers",
            "getCrossProfileWidgetProviders", "getCrossProfilePackages",
            "getManagedProfileMaximumTimeOff", "isComplianceAcknowledgementRequired",
            "isSafeOperation", "canAdminGrantSensorsPermissions",
            
            "setSystemBarsDisabled", "setLocationEnabled", "setTime", "setTimeZone",
            "setPersonalAppsSuspended", "setManagedSubscriptionsPolicy",
            "setCredentialManagerPolicy", "setDnsPolicy", "setResolvedDnsPolicy",
            "setOverrideApn", "updateOverrideApn", "removeOverrideApn",
            "setPreferentialNetworkServiceConfigs", "setGlobalProxy",
            "setManagedProfileContactsAccessPolicy", "getManagedProfileContactsAccessPolicy",
            "setManagedProfileCallerIdAccessPolicy", "getManagedProfileCallerIdAccessPolicy",
            "addCrossProfileWidgetProvider", "bindDeviceAdminServiceAsUser",
            "startUserInBackground", "stopUser", "setProfileDisabled",
            "requestBugreport", "setTrustAgentConfiguration", "getTrustAgentConfiguration",
            "createAdminSupportIntent", "transferOwnership", "setOrganizationId",
            
            "installCaCert", "uninstallCaCert", "uninstallAllUserCaCerts", "hasCaCertInstalled",
            "installKeyPair", "removeKeyPair", "generateKeyPair",
            "grantKeyPairToApp", "revokeKeyPairFromApp", "getKeyPairGrants",
            "grantKeyPairToWifiAuth", "revokeKeyPairFromWifiAuth",
            "isKeyPairGrantedToWifiAuth", "setKeyPairCertificate",
            
            "setPolicy", "getPolicy", "getResolvedDeviceWidePolicy",
            "getResolvedPerUserPolicy",
            
            "reboot", "setAirplaneModeRestricted",
            "getGlobalSetting", "getSystemSetting", "getSecureSetting",
            "setMasterVolumeMutedForPackage", "getBluetoothContactSharingDisabled",
            "canUsbDataSignalingBeDisabled",
            
            "createUser", "createAndInitializeUser", "logoutUser",
            "setProfileIcon", "setProfileOwnerName", "setUserDisplayName",
            "setUserAccentColor",
            
            "setCrossProfileWidgets", "getCrossProfileCallerIdDisabled",
            "setPermittedCrossProfileNotificationListeners",
            "getPermittedCrossProfileNotificationListeners",
            "acknowledgeDeviceCompliant",
            
            "addPersistentPreferredActivity", "clearPackagePersistentPreferredActivities",
            "setGlobalPrivateDnsModeOpportunistic", "setGlobalPrivateDnsModeSpecifiedHost",
            
            "getLastSecurityLogRetrievalTime", "getLastNetworkLogRetrievalTime",
            
            "addUserRestrictionGlobally", "getAffiliationIds",
            
            "getPolicyState", "getEnforcingAdmin", "getDevicePolicyState",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_PERMISSION,
            "setPermissionPolicy", "getPermissionPolicy",
            "setPermissionGrantState", "getPermissionGrantState",
            "setDefaultSmsApplication", "setDefaultDialerApplication",
            "setDelegatedScopes", "getDelegatedScopes",
            
            "getApplicationRestrictionsManagingPackage",
            "isCallerApplicationRestrictionsManagingPackage",
            "getDelegatePackages", "isProvisioningAllowed",
        )

        
        blockAll(
            dpm, XpConfig.KEY_DA_ENCRYPT,
            "setStorageEncryption", "getStorageEncryption", "getStorageEncryptionStatus",
            "requestStorageEncryption", "setRequireStorageEncryption",
            "getStorageEncryptionStatusForUser", "isDeviceEncrypted",
        )

        
        
        

        
        
        
        
        dpm.declaredMethods.filter {
            it.name == "isDeviceOwnerApp" || it.name == "isProfileOwnerApp" ||
                it.name == "isDeviceOwnerAppOnAnyUser" ||
                it.name == "hasGrantedPolicy" || it.name == "isDeviceManaged"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (ownerOff()) {
                    logWarn("blocked DevicePolicyManager.${m.name}")
                    deniedFor(m)
                } else {
                    chain.proceed()
                }
            }
        }
        dpm.declaredMethods.filter {
            it.name == "getDeviceOwnerComponentOnAnyUser" || it.name == "getProfileOwner" ||
                it.name == "getProfileOwnerAsUser" || it.name == "getDeviceOwnerComponent"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (ownerOff()) {
                    logWarn("blocked DevicePolicyManager.${m.name}")
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    







    private fun hookOwnershipQueries() {
        val dpm = cls("android.app.admin.DevicePolicyManager") ?: return

        
        val boolNames = setOf(
            "isDeviceOwnerApp", "isDeviceOwnerAppOnAnyUser", "isProfileOwnerApp",
            "isDeviceOwner", "isProfileOwner", "isDeviceManaged", "isProfileManaged",
            "isManagedProfile", "isOrganizationOwnedDeviceWithManagedProfile",
            "hasDeviceOwner", "hasProfileOwner", "packageHasActiveAdmins",
            "hasGrantedPolicy", "isAdminActive", "isManagedKiosk",
            "isUnattendedManagedKiosk", "isAffiliatedUser", "isDeviceFinanced",
            "isComplianceAcknowledgementRequired", "isLogoutEnabled", "isEphemeralUser",
            "isDeviceIdAttestationSupported", "isUniqueDeviceAttestationSupported",
            "isProvisioningAllowed",
            
            "isOrganizationOwnedDevice", "isDeviceOwnerAppOnCallingUser",
            "isProfileOwnerOfOrganizationOwnedDevice",
        )
        
        val objNames = setOf(
            "getDeviceOwner", "getDeviceOwnerComponent", "getDeviceOwnerComponentOnAnyUser",
            "getDeviceOwnerComponentOnCallingUser", "getProfileOwner", "getProfileOwnerAsUser",
            "getDeviceOwnerName", "getDeviceOwnerNameOnAnyUser",
            "getProfileOwnerName", "getProfileOwnerNameAsUser",
            "getDeviceOwnerLockScreenInfo", "getOrganizationName",
            "getDevicePolicyManagementRoleHolderPackage", "getParentProfileInstance",
            
            "getProfileOwnerOrDeviceOwnerSupervisorComponent",
            "getDeviceOwnerProtectedPackages",
        )
        
        val intNames = setOf(
            "getDeviceOwnerUserId", "getUserProvisioningState",
            "getDeviceOwnerUser", "getProfileOwnerUser",
        )

        
        
        
        val skip: (String) -> Boolean = { n ->
            n == "isAdminActive" || n == "getUserProvisioningState"
        }

        
        val fakeTrue = setOf(
            "isAdminActive", "isDeviceOwnerApp", "isDeviceOwnerAppOnAnyUser",
            "isProfileOwnerApp", "isDeviceOwner", "isProfileOwner",
            "hasDeviceOwner", "hasProfileOwner", "packageHasActiveAdmins",
            "hasGrantedPolicy", "isDeviceManaged",
        )
        dpm.declaredMethods.filter { it.name in boolNames && !skip(it.name) }.forEach { m ->
            hookMethod(m) { chain ->
                if (fakeMode()) {
                    if (m.name in fakeTrue) {
                        logWarn("fake success DevicePolicyManager.${m.name}")
                        return@hookMethod true
                    }
                    return@hookMethod chain.proceed()
                }
                if (!ownerOff()) return@hookMethod chain.proceed()
                logWarn("blocked DevicePolicyManager.${m.name}")
                false
            }
        }
        dpm.declaredMethods.filter { it.name in objNames }.forEach { m ->
            hookMethod(m) { chain ->
                if (fakeMode()) {
                    
                    
                    if (m.returnType == ComponentName::class.java) {
                        logWarn("fake success DevicePolicyManager.${m.name}")
                        return@hookMethod fakeAdminComponent()
                    }
                    return@hookMethod chain.proceed()
                }
                if (!ownerOff()) return@hookMethod chain.proceed()
                logWarn("blocked DevicePolicyManager.${m.name}")
                null
            }
        }
        dpm.declaredMethods.filter { it.name == "getDeviceOwnerUserId" }.forEach { m ->
            hookMethod(m) { chain ->
                if (!ownerOff()) return@hookMethod chain.proceed()
                -10000
            }
        }
        
        dpm.declaredMethods.filter { it.name == "getActiveAdmins" }.forEach { m ->
            hookMethod(m) { chain ->
                if (internalCall.get() == true) return@hookMethod chain.proceed()
                if (fakeMode()) {
                    val c = fakeAdminComponent()
                    logWarn("fake success DevicePolicyManager.getActiveAdmins")
                    return@hookMethod if (c == null) {
                        java.util.ArrayList<Any>()
                    } else {
                        listOf(c)
                    }
                }
                if (!ownerOff()) return@hookMethod chain.proceed()
                java.util.ArrayList<Any>()
            }
        }
        
        dpm.declaredMethods.filter { it.name == "isAdminActive" }.forEach { m ->
            hookMethod(m) { chain ->
                if (internalCall.get() == true) return@hookMethod chain.proceed()
                if (fakeMode()) {
                    logWarn("fake success DevicePolicyManager.isAdminActive (overload)")
                    return@hookMethod true
                }
                if (!ownerOff()) return@hookMethod chain.proceed()
                logWarn("blocked DevicePolicyManager.isAdminActive (overload)")
                false
            }
        }
        logInfo("device ownership queries hooked")
    }

    





    private fun hookPolicyBinder() {
        val stub = cls("android.app.admin.IDevicePolicyManager\$Stub")
            ?: cls("android.app.admin.IDevicePolicyManager")
        if (stub == null) {
            logWarn("policy binder hook: IDevicePolicyManager not found, skipped")
            return
        }
        stub.declaredMethods.filter { it.name == "asInterface" }.forEach { m ->
            hookMethod(m) { chain ->
                val r = chain.proceed() ?: return@hookMethod null
                if (!ownerOff()) return@hookMethod r
                runCatching { wrapPolicyBinder(r) }.getOrDefault(r)
            }
        }
        logInfo("device policy binder hook installed")
    }

    private fun wrapPolicyBinder(original: Any): Any {
        val iface = runCatching {
            Class.forName("android.app.admin.IDevicePolicyManager", false, classLoader)
        }.getOrNull() ?: return original
        return java.lang.reflect.Proxy.newProxyInstance(
            classLoader, arrayOf(iface),
        ) { _, method, args ->
            val n = method.name
            when {
                
                n == "isAdminActive" || n == "packageHasActiveAdmins" ||
                    n == "hasDeviceOwner" || n == "hasProfileOwner" ||
                    n == "isDeviceOwner" || n == "isProfileOwner" ||
                    n == "isManagedProfile" || n == "isManagedKiosk" ||
                    n == "isDeviceFinanced" || n == "isAffiliatedUser" ||
                    n == "hasGrantedPolicy" -> java.lang.Boolean.FALSE
                
                n == "getDeviceOwner" || n == "getDeviceOwnerComponent" ||
                    n == "getDeviceOwnerComponentOnAnyUser" ||
                    n == "getDeviceOwnerName" || n == "getProfileOwner" ||
                    n == "getProfileOwnerName" -> null
                
                n == "getActiveAdmins" -> java.util.ArrayList<Any>()
                
                n == "getDeviceOwnerUserId" -> -10000
                
                else -> runCatching { method.invoke(original, *(args ?: emptyArray())) }
                    .getOrNull()
            }
        }
    }

    




    private fun hookPackageScanBypass() {
        val pm = cls("android.app.ApplicationPackageManager")
            ?: cls("android.content.pm.PackageManager")
        if (pm == null) {
            logWarn("package scan hook: PackageManager not found, skipped")
            return
        }
        val isAdminAction = { intent: Any? ->
            runCatching {
                val m = intent?.javaClass?.getMethod("getAction")
                val a = m?.invoke(intent) as? String
                a != null && (
                    a == DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN ||
                        a.contains("DEVICE_ADMIN", ignoreCase = true) ||
                        a.contains("PROVISION", ignoreCase = true)
                    )
            }.getOrDefault(false)
        }
        pm.declaredMethods.filter {
            it.name == "queryBroadcastReceivers" || it.name == "queryBroadcastReceiversAsUser" ||
                it.name == "queryIntentServices" || it.name == "queryIntentServicesAsUser"
        }.forEach { m ->
            hookMethod(m) { chain ->
                val r = chain.proceed()
                if (!ownerOff() || r == null) return@hookMethod r
                val intent = chain.args.firstOrNull { it?.javaClass?.name?.contains("Intent") == true }
                if (!isAdminAction(intent)) return@hookMethod r
                logWarn("blocked PackageManager.${m.name} (device admin scan)")
                
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    (r as MutableList<Any?>).clear()
                }
                r
            }
        }
        logInfo("device admin package scan bypass hook installed")
    }

    




    private fun hookDeviceOwnerSettings() {
        val keys = setOf("device_owner", "device_provisioned", "user_setup_complete")
        for (clsName in listOf(
            "android.provider.Settings\$Secure",
            "android.provider.Settings\$Global",
        )) {
            val c = cls(clsName) ?: continue
            runCatching {
                c.declaredMethods.filter {
                    it.name == "getString" || it.name == "getStringForUser"
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        val k = chain.args.filterIsInstance<String>().firstOrNull()
                        if (!ownerOff() || k !in keys) return@hookMethod chain.proceed()
                        if (k == "device_owner") "" else "1"
                    }
                }
                c.declaredMethods.filter {
                    it.name == "getInt" || it.name == "getIntForUser"
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        val k = chain.args.filterIsInstance<String>().firstOrNull()
                        if (!ownerOff() || k !in keys) return@hookMethod chain.proceed()
                        if (k == "device_owner") 0 else 1
                    }
                }
            }
        }
        logInfo("device owner settings bypass hook installed")
    }

    
    private fun blockAll(clazz: Class<*>, key: String, vararg names: String) {
        val targets = names.toSet()
        clazz.declaredMethods
            .filter { it.name in targets }
            .forEach { m ->
                hookMethod(m) { chain ->
                    if (blocked(key)) {
                        if (fakeMode()) {
                            logWarn("fake success DevicePolicyManager.${m.name}")
                            successFor(m)
                        } else {
                            logWarn("blocked DevicePolicyManager.${m.name}")
                            deniedFor(m)
                        }
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

        
        
        
        
        val callbacks: List<Pair<String, String>> = listOf(
            
            "onPasswordFailed" to XpConfig.KEY_DA_PASSWORD,
            "onPasswordSucceeded" to XpConfig.KEY_DA_PASSWORD,
            "onPasswordChanged" to XpConfig.KEY_DA_PASSWORD,
            "onPasswordExpiring" to XpConfig.KEY_DA_PASSWORD,
            
            "onDisabled" to XpConfig.KEY_DA_SYSTEM,
            "onDisableRequested" to XpConfig.KEY_DA_SYSTEM,
            "onProfileProvisioningComplete" to XpConfig.KEY_DA_SYSTEM,
            "onReadyForUserInitialization" to XpConfig.KEY_DA_SYSTEM,
            
            "onLockTaskModeEntering" to XpConfig.KEY_DA_SYSTEM,
            "onLockTaskModeExiting" to XpConfig.KEY_DA_SYSTEM,
            
            "onSystemUpdatePending" to XpConfig.KEY_DA_SYSTEM,
            
            "onSecurityLogsAvailable" to XpConfig.KEY_DA_SYSTEM,
            "onNetworkLogsAvailable" to XpConfig.KEY_DA_SYSTEM,
            "onBugreportSharingDeclined" to XpConfig.KEY_DA_SYSTEM,
            "onBugreportFailed" to XpConfig.KEY_DA_SYSTEM,
            "onBugreportShared" to XpConfig.KEY_DA_SYSTEM,
            
            "onUserAdded" to XpConfig.KEY_DA_SYSTEM,
            "onUserRemoved" to XpConfig.KEY_DA_SYSTEM,
            "onUserStarted" to XpConfig.KEY_DA_SYSTEM,
            "onUserStopped" to XpConfig.KEY_DA_SYSTEM,
            "onUserSwitched" to XpConfig.KEY_DA_SYSTEM,
            
            "onTransferOwnershipComplete" to XpConfig.KEY_DA_SYSTEM,
            "onTransferAffiliatedProfileOwnershipComplete" to XpConfig.KEY_DA_SYSTEM,
            
            "onChoosePrivateKeyAlias" to XpConfig.KEY_DA_SYSTEM,
            
            "onOperationSafetyStateChanged" to XpConfig.KEY_DA_SYSTEM,
            "onComplianceAcknowledgementRequired" to XpConfig.KEY_DA_SYSTEM,
            "onUsbDataSignalingChanged" to XpConfig.KEY_DA_SYSTEM,
            "onUninstallApp" to XpConfig.KEY_DA_APPMGMT,
        )

        callbacks.forEach { (name, key) ->
            receiver.declaredMethods.filter { it.name == name }.forEach { m ->
                hookMethod(m) { chain ->
                    if (blocked(key)) {
                        logWarn("blocked DeviceAdminReceiver.$name")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }
            }
        }

        
        
        receiver.declaredMethods.filter { it.name == "onEnabled" }.forEach { m ->
            hookMethod(m) { chain ->
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
        var count = 0

        
        val activity = cls("android.app.Activity")
        if (activity != null) {
            count += hookStartMethods(
                activity,
                setOf(
                    "startActivity", "startActivityForResult", "startActivities",
                    "startActivityIfNeeded", "startActivityFromChild",
                    "startActivityFromFragment",
                ),
            )
        }

        
        for (n in listOf("android.app.ContextImpl", "android.content.ContextWrapper")) {
            val c = cls(n) ?: continue
            count += hookStartMethods(c, setOf("startActivity", "startActivityAsUser"))
        }

        
        
        val inst = cls("android.app.Instrumentation")
        if (inst != null) {
            count += hookStartMethods(inst, setOf("execStartActivity"))
        }

        
        val pi = cls("android.app.PendingIntent")
        if (pi != null) {
            pi.declaredMethods.filter { it.name == "send" }.forEach { m ->
                val ok = hookMethod(m) { chain ->
                    if (!snapshot().daBlockRequest) return@hookMethod chain.proceed()
                    val intent = chain.args.filterIsInstance<Intent>().firstOrNull()
                    if (intent != null && isAdminIntent(intent)) {
                        logWarn("blocked device admin request via PendingIntent.send")
                        return@hookMethod null
                    }
                    chain.proceed()
                }
                if (ok) count++
            }
        }

        logInfo("device admin activation request blocked ($count entry points)")
    }

    
    private fun hookStartMethods(clazz: Class<*>, names: Set<String>): Int {
        var n = 0
        clazz.declaredMethods.filter { it.name in names }.forEach { m ->
            val ok = hookMethod(m) { chain ->
                
                if (!snapshot().daBlockRequest) return@hookMethod chain.proceed()
                val intent = chain.args.filterIsInstance<Intent>().firstOrNull()
                    ?: return@hookMethod chain.proceed()
                if (!isAdminIntent(intent)) return@hookMethod chain.proceed()
                logWarn("blocked device admin activation request: ${intent.action}")
                null
            }
            if (ok) n++
        }
        return n
    }

    private fun isAdminIntent(intent: Intent): Boolean {
        val action = intent.action
        if (action == DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN) return true
        if (action == ACTION_PROVISION_MANAGED_DEVICE) return true
        if (action == ACTION_PROVISION_MANAGED_PROFILE) return true
        if (action == ACTION_SET_PROFILE_OWNER) return true
        
        if (action == ACTION_GET_PROVISIONING_MODE) return true
        if (action == ACTION_ADMIN_POLICY_COMPLIANCE) return true
        if (action == ACTION_CHECK_POLICY_COMPLIANCE) return true
        if (runCatching {
                intent.hasExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN) ||
                    intent.hasExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME) ||
                    intent.hasExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME)
            }.getOrDefault(false)) return true

        
        
        
        return runCatching {
            val comp = intent.component
            if (comp != null) {
                val clsName = comp.className.orEmpty()
                val pkg = comp.packageName.orEmpty()
                if (clsName.contains("DeviceAdmin", ignoreCase = true)) return@runCatching true
                if (pkg == "com.android.settings" &&
                    clsName.contains("DeviceAdmin", ignoreCase = true)
                ) return@runCatching true
            }
            false
        }.getOrDefault(false)
    }

    

    



    fun removeAllNow() {
        
        
        if (!removalWanted() || !closeEnabled()) return
        logInfo("removeAllNow: 退出前摘除设备管理员")
        repeat(4) { i ->
            tryRemoveActiveAdmin()
            runCatching { Thread.sleep(if (i < 2) 80 else 150) }
        }
    }

    private fun startRemoveLoop() {
        if (!Holder.loopStarted.compareAndSet(false, true)) return
        val tick = object : Runnable {
            override fun run() {
                val cfg = snapshot()
                
                
                
                
                val on = removalWanted() && closeContinuously() &&
                    (XpState.Flags.forceDeviceAdmin || (cfg.daEnable && cfg.daMaster))
                if (!on) {
                    Holder.loopStarted.set(false)
                    when {
                        !removalWanted() ->
                            logInfo("remove loop stopped: 运行方式设为只运行钩子")
                        !closeContinuously() ->
                            logInfo("remove loop stopped: 关闭时机设为退出前关闭一次")
                    }
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

                
                val admins = runCatching { withInternalBypass { dpm.activeAdmins } }.getOrNull()
                admins?.filter { it.packageName == pkg }?.forEach { admin ->
                    repeat(3) {
                        runCatching { dpm.removeActiveAdmin(admin) }
                        
                        
                        
                        runCatching {
                            val m = dpm.javaClass.getMethod(
                                "clearProfileOwner", ComponentName::class.java
                            )
                            m.invoke(dpm, admin)
                        }
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

        @Volatile
        private var INSTANCE: DeviceAdminDefender? = null

        
        fun removeAllNow() {
            runCatching { INSTANCE?.removeAllNow() }
        }

        private const val ACTION_PROVISION_MANAGED_DEVICE =
            "android.app.action.PROVISION_MANAGED_DEVICE"
        private const val ACTION_PROVISION_MANAGED_PROFILE =
            "android.app.action.PROVISION_MANAGED_PROFILE"
        private const val ACTION_SET_PROFILE_OWNER =
            "android.app.action.SET_PROFILE_OWNER"
        private const val EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
        private const val EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME"
        private const val ACTION_GET_PROVISIONING_MODE =
            "android.app.action.GET_PROVISIONING_MODE"
        private const val ACTION_ADMIN_POLICY_COMPLIANCE =
            "android.app.action.ADMIN_POLICY_COMPLIANCE"
        private const val ACTION_CHECK_POLICY_COMPLIANCE =
            "android.app.action.CHECK_POLICY_COMPLIANCE"

        private object Holder {
            val loopStarted = AtomicBoolean(false)
        }
    }
}
