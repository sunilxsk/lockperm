package io.github.sunilxsk.lockperm

import android.content.SharedPreferences








internal object XpState {

    private const val MIN_INTERVAL_MS = 300L

    
    object Flags {
        



        @Volatile
        var logEnabled: Boolean = false

        
        @Volatile
        var forceAccessibility: Boolean = false

        



        @Volatile
        var forceAccessibilityAll: Boolean = false

        
        @Volatile
        var forceWallpaper: Boolean = false

        
        @Volatile
        var forceOverlay: Boolean = false

        
        @Volatile
        var forceConn: Boolean = false

        
        @Volatile
        var forceSensor: Boolean = false

        
        @Volatile
        var forceDeviceAdmin: Boolean = false

        
        @Volatile
        var forceJump: Boolean = false

        
        @Volatile
        var forceCameraMic: Boolean = false

        
        @Volatile
        var forceInstall: Boolean = false

        
        @Volatile
        var forcePrintCast: Boolean = false

        
        @Volatile
        var forceNotify: Boolean = false

        
        @Volatile
        var forceNetFilter: Boolean = false

        
        @Volatile
        var forceScreenCapture: Boolean = false

        
        @Volatile
        var forceTorchVibrate: Boolean = false

        
        @Volatile
        var forceClip: Boolean = false

        
        @Volatile
        var forceFileGuard: Boolean = false

        
        @Volatile
        var forceHideApps: Boolean = false

        
        @Volatile
        var forceAudioOut: Boolean = false

        
        @Volatile
        var forceWdbg: Boolean = false

        
        @Volatile
        var forceShizuku: Boolean = false

        
        @Volatile
        var forceExec: Boolean = false

        
        @Volatile
        var forceVolume: Boolean = false

        
        @Volatile
        var exitRequestParallel: Boolean? = null

        



        @Volatile
        var allowExit: Boolean = false
    }

    


    data class Snapshot(
        
        val enableAndroidId: Boolean = true,
        val androidId: String = XpDefaults.ANDROID_ID,
        val enableBuild: Boolean = true,
        
        val buildValues: Map<String, String> = emptyMap(),
        val deviceName: String = "",
        val gsfId: String = "",
        val adsId: String = "",
        val appSetId: String = "",
        val drmId: String = "",
        val hideAccounts: Boolean = false,

        
        val wifiFakeEnable: Boolean = false,
        val wifiFakeSsid: String = "",
        val wifiFakeBssid: String = "",
        val wifiFakeRssi: Int = -55,
        val wifiFakeSpeed: Int = 300,
        val wifiFakeIp: String = "192.168.1.88",
        val wifiFakeFreq: Int = 5180,
        val wifiFakeScan: Boolean = true,
        val wifiFakeList: List<FakeAp> = emptyList(),
        val wifiFakeNetwork: Boolean = true,

        
        val rootFakeEnable: Boolean = false,
        val rootFakeFile: Boolean = true,
        val rootFakeMask: Boolean = true,

        
        val accStatusSpoof: Boolean = true,
        val accStatusValue: Boolean = false,

        
        val vpnHideEnable: Boolean = false,
        val vpnHideIface: Boolean = true,
        val vpnHideCaps: Boolean = true,
        val vpnHideNetInfo: Boolean = true,
        val vpnHideProxy: Boolean = true,
        val vpnHideSettings: Boolean = true,
        val vpnIfaces: Set<String> = XpConfig.decodeSet(XpConfig.DEF_VPN_IFACES),

        
        val exWifiSsid: String = "",
        val exWifiBssid: String = "",
        val exDevOff: Boolean = false,
        val exSimCountry: String = "",
        val exSimOperator: String = "",
        val exSimOperatorName: String = "",
        val exSimSerial: String = "",
        val exSimSubscriber: String = "",
        val exPhoneNumber: String = "",
        val exTimezone: String = "",
        val exLocale: String = "",
        val exTimeEnable: Boolean = false,
        val exTimeOffset: Int = 0,
        val exUptimeEnable: Boolean = false,
        val exUptimeHours: String = "72",
        val exSdkInt: Int = 0,
        val exWifiMac: String = "",
        val exBtMac: String = "",
        val exImei: String = "",
        val exMeid: String = "",
        val exIccid: String = "",
        val exCarrier: String = "",
        val exFbFid: String = "",
        val exFbIid: String = "",
        val exHwSerial: String = "",
        val exKernel: String = "",
        val exArch: String = "",
        val exCpuInfoHw: String = "",
        val exPlatform: String = "",
        val exGpu: String = "",
        val exGpuVendor: String = "",
        val exGpuGlVersion: String = "",
        val exGpuGlsl: String = "",
        val exGpuVkApi: String = "",
        val exGpuDriver: String = "",
        val exGpuVendorId: String = "",
        val exGpuDeviceId: String = "",
        val exGpuMemoryMb: Int = XpConfig.DEF_GPU_MEMORY_MB,
        val exGpuMaxTex: Int = XpConfig.DEF_GPU_MAX_TEX,
        val exGpuMaxCube: Int = XpConfig.DEF_GPU_MAX_TEX,
        val exGpuMaxLayers: Int = XpConfig.DEF_GPU_MAX_LAYERS,
        val exGpuPush: Int = XpConfig.DEF_GPU_PUSH,
        val exCpuEnable: Boolean = false,
        val exCpuMode: String = XpConfig.CPU_MODE_PRESET,
        val exCpuPreset: Int = 0,
        val exCpuCustom: String = "",
        val exCpuCores: Int = 8,
        val exTempEnable: Boolean = false,
        val exTemp: Int = XpConfig.DEF_TEMP,
        val exBatteryEnable: Boolean = false,
        val exBattery: Int = XpConfig.DEF_BATTERY,
        val exOaid: String = "",
        val customProps: Map<String, String> = emptyMap(),
        val hidePaths: List<String> = emptyList(),
        val nativeHook: Boolean = XpConfig.DEF_NATIVE_HOOK,
        val nativeBlockExit: Boolean = XpConfig.DEF_NATIVE_BLOCK_EXIT,
        val nativeAntiDetect: Boolean = XpConfig.DEF_NATIVE_ANTI_DETECT,

        val enableJs: Boolean = true,
        val jsCode: String = "",
        val enableUa: Boolean = false,
        val uaValue: String = "",
        
        val blockCrashEnable: Boolean = true,
        val blockCrashLevel: Int = 1,
        val crashCatchEnable: Boolean = true,
        val crashCopyClipboard: Boolean = false,
        val crashWriteFile: Boolean = true,
        val crashIntercept: Boolean = false,
        
        val exitEnable: Boolean = false,
        val exitSeconds: Int = XpConfig.DEF_EXIT_SECONDS,
        val exitMethods: Set<String> = XpConfig.decodeSet(XpConfig.DEF_EXIT_METHODS),
        val exitParallel: Boolean = false,
        
        val accEnable: Boolean = false,
        val accMode: Int = 0,
        val accScope: Int = 1,
        val accCapScreen: Boolean = true,
        val accCapNotify: Boolean = true,
        val accCapWindow: Boolean = true,
        val accCapInput: Boolean = true,
        val accCapAction: Boolean = true,
        val accCapOverlay: Boolean = true,
        val accCapControl: Boolean = true,
        
        val blockOverlay: Boolean = false,
        val blockWallpaper: Boolean = false,
        val blockExec: Boolean = false,
        
        val volumeEnable: Boolean = false,
        val volumeMaster: Boolean = false,
        val volumeLock: Boolean = false,
        val volumeLockValue: Int = 50,
        val volumeAllowLower: Boolean = false,
        
        val audioOutEnable: Boolean = false,
        val audioOutBlocked: Set<String> = emptySet(),
        val audioOutFocus: Boolean = true,
        val audioOutNoFocus: Boolean = true,
        val volumeBlockRinger: Boolean = true,
        val volumeBlockMute: Boolean = true,
        
        val wdbgEnable: Boolean = false,
        val wdbgToggle: Boolean = true,
        val wdbgPair: Boolean = true,
        val wdbgDiscover: Boolean = true,
        val wdbgProp: Boolean = true,
        val wdbgBlockJump: Boolean = true,
        val wdbgBlockSettings: Boolean = false,
        
        val shizukuEnable: Boolean = false,
        val shizukuBlockAuth: Boolean = true,
        val shizukuBlockUse: Boolean = true,
        
        val blockConnEnable: Boolean = false,
        val blockConnWifi: Boolean = true,
        val blockConnBt: Boolean = true,
        val blockConnBright: Boolean = true,
        val blockSensor: Boolean = false,
        
        val blockJumpEnable: Boolean = false,
        val jumpWhitelist: Set<String> = emptySet(),
        
        val blockCamera: Boolean = false,
        val blockMic: Boolean = false,
        
        val blockInstall: Boolean = false,
        val blockPrintCast: Boolean = false,
        val blockNotify: Boolean = false,
        
        val netFilterEnable: Boolean = false,
        val netFilterWhitelist: Boolean = false,
        val netFilterList: Set<String> = emptySet(),
        
        val blockScreenCapture: Boolean = false,
        val screenCaptureMode: Int = XpConfig.SC_MODE_BLANK,
        val screenCaptureGrantOk: Boolean = false,
        
        val blockTorch: Boolean = false,
        val blockVibrate: Boolean = false,
        
        val fileGuardEnable: Boolean = false,
        val fileOpAll: Boolean = false,
        val fileOps: Set<String> = emptySet(),
        
        val hideAppsEnable: Boolean = false,
        val hideAppsMode: Int = 1,
        val hideAppsListMode: Int = 0,
        val hideAppsList: Set<String> = emptySet(),
        
        val clipEnable: Boolean = false,
        val clipMode: Int = 2,
        
        val panelInject: Boolean = true,
        
        val daEnable: Boolean = false,
        val daMaster: Boolean = false,
        val daLock: Boolean = true,
        val daPassword: Boolean = true,
        val daWipe: Boolean = true,
        val daCamera: Boolean = true,
        val daAppMgmt: Boolean = true,
        val daSystem: Boolean = true,
        val daPermission: Boolean = true,
        val daEncrypt: Boolean = true,
        
        val permEnable: Boolean = false,
        val permDefaultAll: Boolean = true,
        val permGrant: Set<String> = emptySet(),
        val permFakeData: Set<String> = emptySet(),
        




        val permGrantApp: Set<String>? = null,
        val permFakeDataApp: Set<String>? = null,
    ) {
        



        fun effectiveGrant(): Set<String> {
            
            if (permGrant.isNotEmpty()) return permGrant
            
            return permGrantApp ?: emptySet()
        }

        
        fun effectiveFakeData(): Set<String> {
            if (permFakeData.isNotEmpty()) return permFakeData
            return permFakeDataApp ?: emptySet()
        }
    }

    @Volatile
    private var lastMs: Long = 0L

    @Volatile
    private var snapshot: Snapshot = Snapshot()

    



    @Volatile
    var packageName: String = ""

    
    @Volatile
    var currentPkg: String = ""

    
    fun setHost(pkg: String) {
        if (packageName.isEmpty()) packageName = pkg
        currentPkg = pkg
    }

    fun current(): Snapshot = snapshot

    





    private fun projectForHost(raw: Map<String, Any?>, pkg: String): Map<String, Any?> {
        if (pkg.isBlank()) return raw
        val out = LinkedHashMap<String, Any?>()
        raw.forEach { (k, v) -> if (!k.startsWith(XpConfig.APP_PREFIX)) out[k] = v }
        val p = XpConfig.appPrefix(pkg)
        raw.forEach { (k, v) ->
            if (!k.startsWith(p)) return@forEach
            val stripped = k.removePrefix(p)
            
            if (XpConfig.isGlobalKey(stripped)) return@forEach
            out[stripped] = v
        }
        return out
    }

    fun refresh(prefs: SharedPreferences, force: Boolean = false): Snapshot {
        val now = System.currentTimeMillis()
        if (!force && now - lastMs < MIN_INTERVAL_MS) return snapshot
        lastMs = now
        val raw: Map<String, Any?> = try {
            prefs.all ?: emptyMap()
        } catch (_: Throwable) {
            emptyMap()
        }
        
        val map: Map<String, Any?> = projectForHost(raw, packageName)
        
        Flags.logEnabled = raw[XpConfig.KEY_LOG_ENABLE] as? Boolean
            ?: runCatching { prefs.getBoolean(XpConfig.KEY_LOG_ENABLE, false) }.getOrDefault(false)
        snapshot = Snapshot(
            enableAndroidId = map.bool(XpConfig.KEY_ENABLE_ANDROID_ID, false),
            androidId = map.str(XpConfig.KEY_ANDROID_ID, XpDefaults.ANDROID_ID),
            enableBuild = map.bool(XpConfig.KEY_ENABLE_BUILD, true),
            buildValues = XpConfig.BUILD_FIELDS.mapNotNull { f ->
                val v = map.str(f.key, "").trim()
                if (v.isEmpty()) null else f.field to v
            }.toMap(),
            deviceName = map.str(XpConfig.KEY_DEVICE_NAME, "").trim(),
            gsfId = map.str(XpConfig.KEY_GSF_ID, "").trim(),
            adsId = map.str(XpConfig.KEY_ADS_ID, "").trim(),
            appSetId = map.str(XpConfig.KEY_APPSET_ID, "").trim(),
            drmId = map.str(XpConfig.KEY_DRM_ID, "").trim(),
            hideAccounts = map.bool(XpConfig.KEY_HIDE_ACCOUNTS, false),
            wifiFakeEnable = map.bool(XpConfig.KEY_WIFI_FAKE_ENABLE, false),
            wifiFakeSsid = map.str(XpConfig.KEY_WIFI_FAKE_SSID, "").trim(),
            wifiFakeBssid = map.str(XpConfig.KEY_WIFI_FAKE_BSSID, "").trim(),
            wifiFakeRssi = map.int(XpConfig.KEY_WIFI_FAKE_RSSI, -55),
            wifiFakeSpeed = map.int(XpConfig.KEY_WIFI_FAKE_SPEED, 300),
            wifiFakeIp = map.str(XpConfig.KEY_WIFI_FAKE_IP, "192.168.1.88").trim(),
            wifiFakeFreq = map.int(XpConfig.KEY_WIFI_FAKE_FREQ, 5180),
            wifiFakeScan = map.bool(XpConfig.KEY_WIFI_FAKE_SCAN, true),
            wifiFakeList = XpConfig.decodeApList(map.str(XpConfig.KEY_WIFI_FAKE_LIST, "")),
            wifiFakeNetwork = map.bool(XpConfig.KEY_WIFI_FAKE_NETWORK, true),
            rootFakeEnable = map.bool(XpConfig.KEY_ROOT_FAKE_ENABLE, false),
            rootFakeFile = map.bool(XpConfig.KEY_ROOT_FAKE_FILE, true),
            rootFakeMask = map.bool(XpConfig.KEY_ROOT_FAKE_MASK, true),
            accStatusSpoof = map.bool(XpConfig.KEY_ACC_STATUS_SPOOF, true),
            accStatusValue = map.bool(XpConfig.KEY_ACC_STATUS_VALUE, false),
            vpnHideEnable = map.bool(XpConfig.KEY_VPN_HIDE_ENABLE, false),
            vpnHideIface = map.bool(XpConfig.KEY_VPN_HIDE_IFACE, true),
            vpnHideCaps = map.bool(XpConfig.KEY_VPN_HIDE_CAPS, true),
            vpnHideNetInfo = map.bool(XpConfig.KEY_VPN_HIDE_NETINFO, true),
            vpnHideProxy = map.bool(XpConfig.KEY_VPN_HIDE_PROXY, true),
            vpnHideSettings = map.bool(XpConfig.KEY_VPN_HIDE_SETTINGS, true),
            vpnIfaces = XpConfig.decodeSet(
                map.str(XpConfig.KEY_VPN_IFACES, XpConfig.DEF_VPN_IFACES)
            ),
            exWifiSsid = map.str(XpConfig.KEY_FAKE_WIFI_SSID, "").trim(),
            exWifiBssid = map.str(XpConfig.KEY_FAKE_WIFI_BSSID, "").trim(),
            exDevOff = map.bool(XpConfig.KEY_FAKE_DEV_OFF, false),
            exSimCountry = map.str(XpConfig.KEY_FAKE_SIM_COUNTRY, "").trim(),
            exSimOperator = map.str(XpConfig.KEY_FAKE_SIM_OPERATOR, "").trim(),
            exSimOperatorName = map.str(XpConfig.KEY_FAKE_SIM_OPERATOR_NAME, "").trim(),
            exSimSerial = map.str(XpConfig.KEY_FAKE_SIM_SERIAL, "").trim(),
            exSimSubscriber = map.str(XpConfig.KEY_FAKE_SIM_SUBSCRIBER, "").trim(),
            exPhoneNumber = map.str(XpConfig.KEY_FAKE_PHONE_NUMBER, "").trim(),
            exTimezone = map.str(XpConfig.KEY_FAKE_TIMEZONE, "").trim(),
            exLocale = map.str(XpConfig.KEY_FAKE_LOCALE, "").trim(),
            exTimeEnable = map.bool(XpConfig.KEY_FAKE_TIME_ENABLE, false),
            exTimeOffset = map.int(XpConfig.KEY_FAKE_TIME_OFFSET, 0),
            exUptimeEnable = map.bool(XpConfig.KEY_FAKE_UPTIME_ENABLE, false),
            exUptimeHours = map.str(XpConfig.KEY_FAKE_UPTIME_HOURS, "72").trim(),
            exSdkInt = map.int(XpConfig.KEY_FAKE_SDK_INT, 0),
            exWifiMac = map.str(XpConfig.KEY_FAKE_WIFI_MAC, "").trim(),
            exBtMac = map.str(XpConfig.KEY_FAKE_BT_MAC, "").trim(),
            exImei = map.str(XpConfig.KEY_FAKE_IMEI, "").trim(),
            exMeid = map.str(XpConfig.KEY_FAKE_MEID, "").trim(),
            exIccid = map.str(XpConfig.KEY_FAKE_ICCID, "").trim(),
            exCarrier = map.str(XpConfig.KEY_FAKE_CARRIER, "").trim(),
            exFbFid = map.str(XpConfig.KEY_FAKE_FB_FID, "").trim(),
            exFbIid = map.str(XpConfig.KEY_FAKE_FB_IID, "").trim(),
            exHwSerial = map.str(XpConfig.KEY_FAKE_HW_SERIAL, "").trim(),
            exKernel = map.str(XpConfig.KEY_FAKE_KERNEL, "").trim(),
            exArch = map.str(XpConfig.KEY_FAKE_ARCH, "").trim(),
            exCpuInfoHw = map.str(XpConfig.KEY_FAKE_CPUINFO_HW, "").trim(),
            exPlatform = map.str(XpConfig.KEY_FAKE_PLATFORM, "").trim(),
            exGpu = map.str(XpConfig.KEY_FAKE_GPU, "").trim(),
            exGpuVendor = map.str(XpConfig.KEY_FAKE_GPU_VENDOR, "").trim(),
            exGpuGlVersion = map.str(XpConfig.KEY_FAKE_GPU_GL_VERSION, "").trim(),
            exGpuGlsl = map.str(XpConfig.KEY_FAKE_GPU_GLSL, "").trim(),
            exGpuVkApi = map.str(XpConfig.KEY_FAKE_GPU_VK_API, "").trim(),
            exGpuDriver = map.str(XpConfig.KEY_FAKE_GPU_DRIVER, "").trim(),
            exGpuVendorId = map.str(XpConfig.KEY_FAKE_GPU_VENDOR_ID, "").trim(),
            exGpuDeviceId = map.str(XpConfig.KEY_FAKE_GPU_DEVICE_ID, "").trim(),
            exGpuMemoryMb = map.int(XpConfig.KEY_FAKE_GPU_MEMORY_MB, XpConfig.DEF_GPU_MEMORY_MB),
            exGpuMaxTex = map.int(XpConfig.KEY_FAKE_GPU_MAX_TEX, XpConfig.DEF_GPU_MAX_TEX),
            exGpuMaxCube = map.int(XpConfig.KEY_FAKE_GPU_MAX_CUBE, XpConfig.DEF_GPU_MAX_TEX),
            exGpuMaxLayers = map.int(XpConfig.KEY_FAKE_GPU_MAX_LAYERS, XpConfig.DEF_GPU_MAX_LAYERS),
            exGpuPush = map.int(XpConfig.KEY_FAKE_GPU_PUSH, XpConfig.DEF_GPU_PUSH),
            exCpuEnable = map.bool(XpConfig.KEY_FAKE_CPU_ENABLE, false),
            exCpuMode = map.str(XpConfig.KEY_FAKE_CPU_MODE, XpConfig.CPU_MODE_PRESET),
            exCpuPreset = map.int(XpConfig.KEY_FAKE_CPU_PRESET, 0),
            exCpuCustom = map.str(XpConfig.KEY_FAKE_CPU_CUSTOM, ""),
            exCpuCores = map.int(XpConfig.KEY_FAKE_CPU_CORES, 8),
            exTempEnable = map.bool(XpConfig.KEY_FAKE_TEMP_ENABLE, false),
            exTemp = map.int(XpConfig.KEY_FAKE_TEMP, XpConfig.DEF_TEMP),
            exBatteryEnable = map.bool(XpConfig.KEY_FAKE_BATTERY_ENABLE, false),
            exBattery = map.int(XpConfig.KEY_FAKE_BATTERY, XpConfig.DEF_BATTERY),
            exOaid = map.str(XpConfig.KEY_OAID, "").trim(),
            customProps = XpConfig.decodeProps(map.str(XpConfig.KEY_CUSTOM_PROPS, "")).toMap(),
            hidePaths = XpConfig.decodePathLines(map.str(XpConfig.KEY_HIDE_PATHS, "")),
            
            
            
            nativeHook = raw.bool(XpConfig.KEY_NATIVE_HOOK, XpConfig.DEF_NATIVE_HOOK),
            nativeBlockExit = map.bool(
                XpConfig.KEY_NATIVE_BLOCK_EXIT, XpConfig.DEF_NATIVE_BLOCK_EXIT
            ),
            nativeAntiDetect = raw.bool(
                XpConfig.KEY_NATIVE_ANTI_DETECT, XpConfig.DEF_NATIVE_ANTI_DETECT
            ),
            enableJs = map.bool(XpConfig.KEY_ENABLE_JS, false),
            jsCode = map.str(XpConfig.KEY_JS_CODE, XpDefaults.JS),
            enableUa = map.bool(XpConfig.KEY_ENABLE_UA, false),
            uaValue = map.str(XpConfig.KEY_UA_VALUE, ""),
            blockCrashEnable = map.bool(XpConfig.KEY_BLOCK_CRASH_ENABLE, true),
            blockCrashLevel = map.int(XpConfig.KEY_BLOCK_CRASH_LEVEL, 1),
            crashCatchEnable = map.bool(XpConfig.KEY_CRASH_CATCH_ENABLE, true),
            crashCopyClipboard = map.bool(XpConfig.KEY_CRASH_COPY_CLIPBOARD, false),
            crashWriteFile = map.bool(XpConfig.KEY_CRASH_WRITE_FILE, true),
            crashIntercept = map.bool(XpConfig.KEY_CRASH_INTERCEPT, false),
            exitEnable = map.bool(XpConfig.KEY_EXIT_ENABLE, false),
            exitSeconds = map.int(XpConfig.KEY_EXIT_SECONDS, XpConfig.DEF_EXIT_SECONDS),
            exitMethods = XpConfig.decodeSet(map.str(XpConfig.KEY_EXIT_METHODS, XpConfig.DEF_EXIT_METHODS)),
            exitParallel = map.bool(XpConfig.KEY_EXIT_PARALLEL, false),
            accEnable = map.bool(XpConfig.KEY_ACC_ENABLE, false),
            accMode = map.int(XpConfig.KEY_ACC_MODE, 0),
            accScope = map.int(XpConfig.KEY_ACC_SCOPE, 1),
            accCapScreen = map.bool(XpConfig.KEY_ACC_CAP_SCREEN, true),
            accCapNotify = map.bool(XpConfig.KEY_ACC_CAP_NOTIFY, true),
            accCapWindow = map.bool(XpConfig.KEY_ACC_CAP_WINDOW, true),
            accCapInput = map.bool(XpConfig.KEY_ACC_CAP_INPUT, true),
            accCapAction = map.bool(XpConfig.KEY_ACC_CAP_ACTION, true),
            accCapOverlay = map.bool(XpConfig.KEY_ACC_CAP_OVERLAY, true),
            accCapControl = map.bool(XpConfig.KEY_ACC_CAP_CONTROL, true),
            blockOverlay = map.bool(XpConfig.KEY_BLOCK_OVERLAY, false),
            blockWallpaper = map.bool(XpConfig.KEY_BLOCK_WALLPAPER, false),
            blockExec = map.bool(XpConfig.KEY_BLOCK_EXEC, false),
            volumeEnable = map.bool(XpConfig.KEY_VOLUME_ENABLE, false),
            volumeMaster = map.bool(XpConfig.KEY_VOLUME_MASTER, false),
            volumeLock = map.bool(XpConfig.KEY_VOLUME_LOCK, false),
            volumeLockValue = map.int(XpConfig.KEY_VOLUME_LOCK_VALUE, 50),
            audioOutEnable = map.bool(XpConfig.KEY_AUDIO_OUT_ENABLE, false),
            audioOutBlocked = XpConfig.AUDIO_OUT_ITEMS
                .filter { map.bool(it.first, false) }
                .map { it.first }.toSet(),
            audioOutFocus = map.bool(XpConfig.KEY_AUDIO_OUT_FOCUS, true),
            audioOutNoFocus = map.bool(XpConfig.KEY_AUDIO_OUT_NO_FOCUS, true),
            volumeAllowLower = map.bool(XpConfig.KEY_VOLUME_ALLOW_LOWER, false),
            volumeBlockRinger = map.bool(XpConfig.KEY_VOLUME_BLOCK_RINGER, true),
            volumeBlockMute = map.bool(XpConfig.KEY_VOLUME_BLOCK_MUTE, true),
            wdbgEnable = map.bool(XpConfig.KEY_WDBG_ENABLE, false),
            wdbgToggle = map.bool(XpConfig.KEY_WDBG_TOGGLE, true),
            wdbgPair = map.bool(XpConfig.KEY_WDBG_PAIR, true),
            wdbgDiscover = map.bool(XpConfig.KEY_WDBG_DISCOVER, true),
            wdbgProp = map.bool(XpConfig.KEY_WDBG_PROP, true),
            wdbgBlockJump = map.bool(XpConfig.KEY_WDBG_BLOCK_JUMP, true),
            wdbgBlockSettings = map.bool(XpConfig.KEY_WDBG_BLOCK_SETTINGS, false),
            shizukuEnable = map.bool(XpConfig.KEY_SHIZUKU_ENABLE, false),
            shizukuBlockAuth = map.bool(XpConfig.KEY_SHIZUKU_BLOCK_AUTH, true),
            shizukuBlockUse = map.bool(XpConfig.KEY_SHIZUKU_BLOCK_USE, true),
            blockConnEnable = map.bool(XpConfig.KEY_BLOCK_CONN_ENABLE, false),
            blockConnWifi = map.bool(XpConfig.KEY_BLOCK_CONN_WIFI, true),
            blockConnBt = map.bool(XpConfig.KEY_BLOCK_CONN_BT, true),
            blockConnBright = map.bool(XpConfig.KEY_BLOCK_CONN_BRIGHT, true),
            blockSensor = map.bool(XpConfig.KEY_BLOCK_SENSOR, false),
            blockJumpEnable = map.bool(XpConfig.KEY_BLOCK_JUMP_ENABLE, false),
            jumpWhitelist = XpConfig.decodeLines(map.str(XpConfig.KEY_BLOCK_JUMP_WHITELIST, "")),
            blockCamera = map.bool(XpConfig.KEY_BLOCK_CAMERA_ENABLE, false),
            blockMic = map.bool(XpConfig.KEY_BLOCK_MIC_ENABLE, false),
            blockInstall = map.bool(XpConfig.KEY_BLOCK_INSTALL_ENABLE, false),
            blockPrintCast = map.bool(XpConfig.KEY_BLOCK_PRINT_CAST, false),
            blockNotify = map.bool(XpConfig.KEY_BLOCK_NOTIFY, false),
            netFilterEnable = map.bool(XpConfig.KEY_NET_FILTER_ENABLE, false),
            netFilterWhitelist = map.bool(XpConfig.KEY_NET_FILTER_WHITELIST, false),
            netFilterList = XpConfig.decodeLines(map.str(XpConfig.KEY_NET_FILTER_LIST, "")),
            blockScreenCapture = map.bool(XpConfig.KEY_BLOCK_SCREEN_CAPTURE, false),
            screenCaptureMode = map.int(XpConfig.KEY_SCREEN_CAPTURE_MODE, XpConfig.SC_MODE_BLANK),
            screenCaptureGrantOk = map.bool(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, false),
            blockTorch = map.bool(XpConfig.KEY_BLOCK_TORCH, false),
            blockVibrate = map.bool(XpConfig.KEY_BLOCK_VIBRATE, false),
            fileGuardEnable = map.bool(XpConfig.KEY_FILE_GUARD_ENABLE, false),
            fileOpAll = map.bool(XpConfig.KEY_FILE_OP_ALL, false),
            fileOps = XpConfig.FILE_OP_ITEMS
                .filter { map.bool(it.first, true) }
                .map { it.first }.toSet(),
            hideAppsEnable = map.bool(XpConfig.KEY_HIDE_APPS_ENABLE, false),
            hideAppsMode = map.int(XpConfig.KEY_HIDE_APPS_MODE, 1),
            hideAppsListMode = map.int(XpConfig.KEY_HIDE_APPS_LIST_MODE, 0),
            hideAppsList = XpConfig.decodeLines(map.str(XpConfig.KEY_HIDE_APPS_LIST, "")),
            clipEnable = map.bool(XpConfig.KEY_CLIP_ENABLE, false),
            clipMode = map.int(XpConfig.KEY_CLIP_MODE, 2),
            panelInject = map.bool(XpConfig.KEY_PANEL_INJECT, true),
            daEnable = map.bool(XpConfig.KEY_DA_ENABLE, false),
            daMaster = map.bool(XpConfig.KEY_DA_MASTER, false),
            daLock = map.bool(XpConfig.KEY_DA_LOCK, true),
            daPassword = map.bool(XpConfig.KEY_DA_PASSWORD, true),
            daWipe = map.bool(XpConfig.KEY_DA_WIPE, true),
            daCamera = map.bool(XpConfig.KEY_DA_CAMERA, true),
            daAppMgmt = map.bool(XpConfig.KEY_DA_APPMGMT, true),
            daSystem = map.bool(XpConfig.KEY_DA_SYSTEM, true),
            daPermission = map.bool(XpConfig.KEY_DA_PERMISSION, true),
            daEncrypt = map.bool(XpConfig.KEY_DA_ENCRYPT, true),
            
            permEnable = map.bool(XpConfig.KEY_PERM_ENABLE, false) ||
                    XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_GRANT, "")).isNotEmpty() ||
                    XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_FAKE_DATA, "")).isNotEmpty(),
            permDefaultAll = map.bool(XpConfig.KEY_PERM_DEFAULT_ALL, true),
            permGrant = XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_GRANT, "")),
            permFakeData = XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_FAKE_DATA, "")),
            permGrantApp = readAppSet(
                prefs, map, XpConfig.PREFIX_APP_GRANT, XpConfig.LEGACY_GRANT_READ, "授权组"
            ),
            permFakeDataApp = readAppSet(
                prefs, map, XpConfig.PREFIX_APP_FAKE, XpConfig.LEGACY_FAKE_READ, "假数据组"
            ),
        )
        return snapshot
    }

    private fun Map<String, Any?>.bool(key: String, def: Boolean): Boolean =
        (this[key] as? Boolean) ?: def

    private fun Map<String, Any?>.int(key: String, def: Int): Int = when (val v = this[key]) {
        is Int -> v
        is Long -> v.toInt()
        is String -> v.toIntOrNull() ?: def
        else -> def
    }

    




    private data class CacheEntry(val pkg: String, val value: Set<String>?, val ms: Long)

    private val appSetCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()

    



    private const val APP_SET_CACHE_MS = 2000L

    private fun readAppSet(
        prefs: SharedPreferences,
        map: Map<String, Any?>,
        prefix: String,
        legacyPrefix: String?,
        tag: String,
    ): Set<String>? {
        val pkg = packageName
        val now = System.currentTimeMillis()
        appSetCache[tag]?.let { e ->
            if (e.pkg == pkg && now - e.ms < APP_SET_CACHE_MS) return e.value
        }

        val result = doReadAppSet(prefs, map, prefix, legacyPrefix, tag, pkg)
        appSetCache[tag] = CacheEntry(pkg, result, now)
        return result
    }

    private fun doReadAppSet(
        prefs: SharedPreferences,
        map: Map<String, Any?>,
        prefix: String,
        legacyPrefix: String?,
        tag: String,
        pkg: String,
    ): Set<String>? {
        val key = resolveAppKey(prefs, map, prefix, legacyPrefix, pkg)
        if (key == null) {
            diag("host=$pkg (当前=$currentPkg) | $tag: 没找到专属配置 -> 回退到全局/默认全部")
            return null
        }
        val raw = runCatching { prefs.getString(key, null) }.getOrNull()
            ?: map[key]?.toString()
            ?: ""
        val set = XpConfig.decodeSet(raw)
        diag("host=$pkg (当前=$currentPkg) | $tag: key=$key -> ${set.size} 组 $set")
        return set
    }

    








    private fun resolveAppKey(
        prefs: SharedPreferences,
        map: Map<String, Any?>,
        prefix: String,
        legacyPrefix: String?,
        pkg: String,
    ): String? {
        if (pkg.isBlank()) {
            diag("resolveAppKey: packageName 为空！")
            return null
        }

        fun hit(key: String): Boolean {
            
            if (map.containsKey(key)) return true
            
            if (runCatching { prefs.contains(key) }.getOrNull() == true) return true
            
            if (runCatching { prefs.getString(key, null) }.getOrNull() != null) return true
            return false
        }

        val exact = prefix + pkg
        if (hit(exact)) {
            log("resolveAppKey: 精确命中 $exact")
            return exact
        }
        if (legacyPrefix != null) {
            val legacy = legacyPrefix + pkg
            if (hit(legacy)) {
                log("resolveAppKey: 旧前缀命中 $legacy")
                return legacy
            }
        }

        
        val exclude = setOf(XpConfig.KEY_PERM_GRANT, XpConfig.KEY_PERM_FAKE_DATA)
        val allKeys: Set<String> = try {
            val a = runCatching { prefs.all?.keys }.getOrNull().orEmpty()
            
            val b = runCatching { prefs.all?.keys }.getOrNull().orEmpty()
            a + b + map.keys
        } catch (_: Throwable) {
            map.keys
        }

        val candidates = allKeys.filter { k ->
            if (k in exclude) return@filter false
            val startsNew = k.startsWith(prefix)
            val startsLegacy = legacyPrefix != null && k.startsWith(legacyPrefix)
            if (!startsNew && !startsLegacy) return@filter false
            
            val rest = if (startsNew) k.removePrefix(prefix)
            else k.removePrefix(legacyPrefix!!)
            rest.contains(".")
        }
        if (candidates.isEmpty()) {
            diag("resolveAppKey: pkg=$pkg，配置里没有任何以 $prefix 开头的 key")
            return null
        }
        log("resolveAppKey: pkg=$pkg 候选=$candidates")

        fun pkgOf(cand: String): String {
            var p = cand.removePrefix(prefix)
            if (legacyPrefix != null) p = p.removePrefix(legacyPrefix)
            return p
        }

        
        candidates.firstOrNull { cand ->
            val p = pkgOf(cand)
            p.isNotEmpty() && pkg.startsWith(p) && pkg.removePrefix(p).startsWith(":")
        }?.let { return it }

        
        candidates.firstOrNull { cand ->
            val p = pkgOf(cand)
            p.isNotEmpty() && p.startsWith(pkg)
        }?.let { return it }

        diag("resolveAppKey: pkg=$pkg，候选都不匹配")
        return null
    }

    


    private fun log(msg: String) {
        if (!Flags.logEnabled) return
        android.util.Log.i("LockPerm", "[perm] $msg")
    }

    
    @Volatile
    private var toastShown = 0

    @Volatile
    private var appContext: android.content.Context? = null

    
    private fun appCtx(): android.content.Context? {
        appContext?.let { return it }
        val c = runCatching {
            val at = Class.forName("android.app.ActivityThread")
            val m = at.getDeclaredMethod("currentApplication")
            m.isAccessible = true
            m.invoke(null) as? android.content.Context
        }.getOrNull()
        if (c != null) appContext = c
        return c
    }

    



    private fun diag(msg: String) {
        if (!Flags.logEnabled) return
        android.util.Log.i("LockPerm", "[perm] $msg")
        if (toastShown >= 6) return
        toastShown++
        runCatching {
            val ctx = appCtx() ?: return
            val h = android.os.Handler(android.os.Looper.getMainLooper())
            h.post {
                runCatching {
                    android.widget.Toast.makeText(ctx, "XP: $msg", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun Map<String, Any?>.str(key: String, def: String): String =
        (this[key] as? String) ?: def
}