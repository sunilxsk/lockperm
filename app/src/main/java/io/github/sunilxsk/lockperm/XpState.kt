package io.github.sunilxsk.lockperm

import android.content.SharedPreferences








internal object XpState {

    










    private const val MIN_INTERVAL_MS = 3000L

    














    object RealSdk {
        @Volatile
        var value: Int = 0
            private set

        




        fun capture() {
            if (value != 0) return
            value = runCatching { android.os.Build.VERSION.SDK_INT }.getOrDefault(0)
        }
    }

    
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
        var forceBgLaunch: Boolean = false

        
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
        var forceKeyConsume: Boolean = false

        
        @Volatile
        var forceVolume: Boolean = false

        
        @Volatile
        var exitRequestParallel: Boolean? = null

        



        @Volatile
        var allowExit: Boolean = false
    }

    


    class Snapshot {
        
        var enableAndroidId: Boolean = true
        var androidId: String = XpDefaults.ANDROID_ID
        var enableBuild: Boolean = true
        
        var buildValues: Map<String, String> = emptyMap()
        var deviceName: String = ""
        var gsfId: String = ""
        var adsId: String = ""
        var appSetId: String = ""
        var drmId: String = ""
        var hideAccounts: Boolean = false

        
        var wifiFakeEnable: Boolean = false
        var blockWifiSaved: Boolean = false
        var blockWakeLock: Boolean = false
        var wifiFakeSsid: String = ""
        var wifiFakeBssid: String = ""
        var wifiFakeRssi: Int = -55
        var wifiFakeSpeed: Int = 300
        var wifiFakeIp: String = "192.168.1.88"
        var wifiFakeFreq: Int = 5180
        var wifiFakeScan: Boolean = true
        var wifiFakeList: List<FakeAp> = emptyList()
        var wifiFakeNetwork: Boolean = true

        
        var rootFakeEnable: Boolean = false
        var rootFakeFile: Boolean = true
        var rootFakeMask: Boolean = true

        
        var accStatusSpoof: Boolean = true
        var accStatusValue: Boolean = false
        
        var accFakeMode: Int = XpConfig.ACC_MODE_DEFAULT

        
        var vpnHideEnable: Boolean = false
        var vpnHideIface: Boolean = true
        var vpnHideCaps: Boolean = true
        var vpnHideNetInfo: Boolean = true
        var vpnHideProxy: Boolean = true
        var vpnHideSettings: Boolean = true
        var vpnIfaces: Set<String> = XpConfig.decodeSet(XpConfig.DEF_VPN_IFACES)

        
        var exWifiSsid: String = ""
        var exWifiBssid: String = ""
        var exDevOff: Boolean = false
        var exSimCountry: String = ""
        var exSimOperator: String = ""
        var exSimOperatorName: String = ""
        var exSimSerial: String = ""
        var exSimSubscriber: String = ""
        var exPhoneNumber: String = ""
        var exTimezone: String = ""
        var exLocale: String = ""
        var exTimeEnable: Boolean = false
        var exTimeOffset: Int = 0
        var exUptimeEnable: Boolean = false
        var exUptimeHours: String = "72"
        var exSdkInt: Int = 0
        var exWifiMac: String = ""
        var exBtMac: String = ""
        var exImei: String = ""
        var exMeid: String = ""
        var exIccid: String = ""
        var exCarrier: String = ""
        var exFbFid: String = ""
        var exFbIid: String = ""
        var exHwSerial: String = ""
        var exKernel: String = ""
        var exArch: String = ""
        var exCpuInfoHw: String = ""
        var exPlatform: String = ""
        var exGpu: String = ""
        var exGpuVendor: String = ""
        var exGpuGlVersion: String = ""
        var exGpuGlsl: String = ""
        var exGpuVkApi: String = ""
        var exGpuDriver: String = ""
        var exGpuVendorId: String = ""
        var exGpuDeviceId: String = ""
        var exGpuMemoryMb: Int = XpConfig.DEF_GPU_MEMORY_MB
        var exGpuMaxTex: Int = XpConfig.DEF_GPU_MAX_TEX
        var exGpuMaxCube: Int = XpConfig.DEF_GPU_MAX_TEX
        var exGpuMaxLayers: Int = XpConfig.DEF_GPU_MAX_LAYERS
        var exGpuPush: Int = XpConfig.DEF_GPU_PUSH
        var exCpuEnable: Boolean = false
        var exCpuMode: String = XpConfig.CPU_MODE_PRESET
        var exCpuPreset: Int = 0
        var exCpuCustom: String = ""
        var exCpuCores: Int = 8
        var exCpuMinFreq: String = ""
        var exCpuMaxFreq: String = ""
        var exCpuCurFreq: String = ""
        var exTempEnable: Boolean = false
        var exTemp: Int = XpConfig.DEF_TEMP
        
        var locEnable: Boolean = false
        var locFields: Boolean = true
        var locMode: Int = 0
        var locLat: String = ""
        var locLon: String = ""
        var locAlt: String = ""
        var locAcc: String = ""
        var locGps: Boolean = true
        var locNet: Boolean = true
        var locFused: Boolean = true
        var locTelephony: Boolean = true
        var locStore: Boolean = false
        var locCell: Boolean = true
        var cellMcc: String = ""
        var cellMnc: String = ""
        var cellLac: String = ""
        var cellCid: String = ""
        var cellPci: String = ""
        
        var winSecureMode: Int = 0
        var winFlags: Set<String> = emptySet()
        var exBatteryEnable: Boolean = false
        var exBattery: Int = XpConfig.DEF_BATTERY
        var fakeBatteryDrain: Boolean = false
        var fakeBatteryDrainMin: Int = 5
        var dpiEnable: Boolean = false
        var dpiValue: Int = XpConfig.DEF_DPI
        var exMemEnable: Boolean = false
        var exMemMb: Int = XpConfig.DEF_MEM_MB
        
        var mem2Enable: Boolean = false
        var memTotalMb: Int = XpConfig.DEF_MEM_TOTAL_MB
        var memAvailMb: Int = XpConfig.DEF_MEM_AVAIL_MB
        var storTotalGb: Int = XpConfig.DEF_STOR_TOTAL_GB
        var storAvailGb: Int = XpConfig.DEF_STOR_AVAIL_GB
        
        var dispEnable: Boolean = false
        var resW: Int = XpConfig.DEF_RES_W
        var resH: Int = XpConfig.DEF_RES_H
        var refreshHz: Float = XpConfig.DEF_REFRESH.toFloat()
        var refreshList: String = XpConfig.DEF_REFRESH_LIST
        
        var camEnable: Boolean = false
        var camBackMp: Int = XpConfig.DEF_CAM_BACK_MP
        var camFrontMp: Int = XpConfig.DEF_CAM_FRONT_MP
        
        var netpEnable: Boolean = false
        var ipv4: String = ""
        var ipv6: String = ""
        var dns1: String = ""
        var dns2: String = ""
        var gateway: String = ""
        var wifiStd: String = XpConfig.DEF_WIFI_STD
        
        var batexEnable: Boolean = false
        var batStatus: String = XpConfig.DEF_BAT_STATUS
        var batCurrentMa: Int = XpConfig.DEF_BAT_CURRENT_MA
        var batVoltageMv: Int = XpConfig.DEF_BAT_VOLTAGE_MV
        var batDesignMah: Int = XpConfig.DEF_BAT_DESIGN_MAH
        
        var simEsim: Boolean = false
        var simEsimActive: Boolean = false
        var simData: Boolean = false
        var simRoamEnable: Boolean = false
        var simRoam: Boolean = false
        var exOaid: String = ""
        var gmsEnable: Boolean = false
        var gmsInstalled: Boolean = true
        var gmsAvailable: Boolean = true
        var gmsAdidEnable: Boolean = true
        var gmsAdid: String = ""
        var gmsAdidLimit: Boolean = false
        var gmsVersion: String = XpConfig.DEF_GMS_VERSION
        var gmsVersionCode: Int = XpConfig.DEF_GMS_VERSION_CODE
        var hmsEnable: Boolean = false
        var hmsInstalled: Boolean = true
        var hmsAvailable: Boolean = true
        var hmsAdidEnable: Boolean = true
        var hmsVersion: String = XpConfig.DEF_HMS_VERSION
        var hmsVersionCode: Int = XpConfig.DEF_HMS_VERSION_CODE
        var nativeHook: Boolean = XpConfig.DEF_NATIVE_HOOK
        var nativeBlockExit: Boolean = XpConfig.DEF_NATIVE_BLOCK_EXIT
        var nativeAntiDetect: Boolean = XpConfig.DEF_NATIVE_ANTI_DETECT
        
        var nativeAppMode: Int = XpConfig.NATIVE_APP_DEFAULT
        
        var nativeActive: Boolean = false
        
        var nativeGroups: Int = 0

        var enableJs: Boolean = true
        var jsCode: String = ""
        var enableUa: Boolean = false
        var uaValue: String = ""
        
        var blockCrashEnable: Boolean = true
        var blockCrashLevel: Int = 1
        var crashCatchEnable: Boolean = true
        var crashCopyClipboard: Boolean = false
        var crashWriteFile: Boolean = true
        var crashIntercept: Boolean = false
        
        var exitEnable: Boolean = false
        var exitSeconds: Int = XpConfig.DEF_EXIT_SECONDS
        var exitMethods: Set<String> = XpConfig.decodeSet(XpConfig.DEF_EXIT_METHODS)
        var exitParallel: Boolean = false
        
        var accEnable: Boolean = false
        var accMode: Int = 0
        var accScope: Int = 1
        var accCapScreen: Boolean = true
        var accCapNotify: Boolean = true
        var accCapWindow: Boolean = true
        var accCapInput: Boolean = true
        var accCapAction: Boolean = true
        var accCapOverlay: Boolean = true
        var accCapControl: Boolean = true
        
        var blockOverlay: Boolean = false
        var overlayUntouchable: Boolean = false
        var overlayTransparent: Boolean = false
        var overlayMaxPercent: Int = 0
        var blockWallpaper: Boolean = false
        var blockHideRecents: Boolean = false
        var blockScreenOff: Boolean = false
        var screenOffWakeLock: Boolean = false
        var screenOffReflect: Boolean = false
        var blockNotifyHide: Boolean = false
        var blockProvider: Boolean = false
        var blockFgService: Boolean = false
        var daBlockRequest: Boolean = false
        
        var exitCountdown: Boolean = true
        var condAtTime: Boolean = false
        var condAtTimeHh: Int = 18
        var condAtTimeMm: Int = 0
        var condCountdown: Boolean = false
        var condCountdownMin: Int = 30
        var condMem: Boolean = false
        var condMemMb: Int = 1024
        var condCpu: Boolean = false
        var condCpuPct: Int = 80
        var condDisk: Boolean = false
        var condDiskMb: Int = 500
        var condNet: Boolean = false
        var condNetMode: Int = 0
        var condBatt: Boolean = false
        var condBattMode: Int = 0
        var condBattPct: Int = 10
        var condFile: Boolean = false
        var condFilePaths: List<String> = emptyList()
        var condFileEvent: Int = 0
        var condIdle: Boolean = false
        var condIdleMin: Int = 10
        var blockExec: Boolean = false
        
        var volumeEnable: Boolean = false
        var volumeMaster: Boolean = false
        var volumeLock: Boolean = false
        var volumeLockValue: Int = 50
        var volumeAllowLower: Boolean = false
        
        var audioOutEnable: Boolean = false
        var audioOutBlocked: Set<String> = emptySet()
        var audioOutFocus: Boolean = true
        var audioOutNoFocus: Boolean = true
        var volumeBlockRinger: Boolean = true
        var volumeBlockMute: Boolean = true
        
        var wdbgEnable: Boolean = false
        var wdbgToggle: Boolean = true
        var wdbgPair: Boolean = true
        var wdbgDiscover: Boolean = true
        var wdbgProp: Boolean = true
        var wdbgBlockJump: Boolean = true
        var wdbgBlockSettings: Boolean = false
        
        var shizukuEnable: Boolean = false
        var shizukuBlockAuth: Boolean = true
        var shizukuBlockUse: Boolean = true
        
        var blockConnEnable: Boolean = false
        var blockConnWifi: Boolean = true
        var blockConnBt: Boolean = true
        var blockConnBright: Boolean = true
        var blockSensor: Boolean = false
        
        var blockJumpEnable: Boolean = false
        var jumpWhitelist: Set<String> = emptySet()

        var blockBgLaunch: Boolean = false
        var blockBgLaunchPending: Boolean = true
        var blockBgLaunchStrict: Boolean = true
        
        var blockCamera: Boolean = false
        var blockMic: Boolean = false
        
        var blockInstall: Boolean = false
        var blockPrintCast: Boolean = false
        var blockNotify: Boolean = false
        
        var netFilterEnable: Boolean = false
        var netFilterWhitelist: Boolean = false
        var netFilterList: Set<String> = emptySet()
        
        var blockScreenCapture: Boolean = false
        var screenCaptureMode: Int = XpConfig.SC_MODE_BLANK
        var screenCaptureGrantOk: Boolean = false
        
        var blockTorch: Boolean = false
        var blockVibrate: Boolean = false

        var blockKeyConsume: Boolean = false
        
        var hasConfig: Boolean = false
        var blockKeyPassBack: Boolean = true

        var screenCaptureFit: Boolean = true
        
        var fileGuardEnable: Boolean = false
        var fileOpAll: Boolean = false
        var fileOps: Set<String> = emptySet()
        
        var hideAppsEnable: Boolean = false
        var hideAppsMode: Int = 1
        var hideAppsListMode: Int = 0
        var hideAppsList: Set<String> = emptySet()
        
        var clipEnable: Boolean = false
        var clipMode: Int = 2
        
        var panelInject: Boolean = true
        
        var daEnable: Boolean = false
        
        var daFakeMode: Int = XpConfig.DA_MODE_DEFAULT
        var daScope: Int = XpConfig.DA_SCOPE_CLOSE_AND_HOOK
        var daCloseMode: Int = XpConfig.DA_CLOSE_CONTINUOUS
        var daMaster: Boolean = false
        var daLock: Boolean = true
        var daPassword: Boolean = true
        var daWipe: Boolean = true
        var daCamera: Boolean = true
        var daAppMgmt: Boolean = true
        var daSystem: Boolean = true
        var daPermission: Boolean = true
        var daEncrypt: Boolean = true
        
        var permEnable: Boolean = false
        var permDefaultAll: Boolean = true
        var permGrant: Set<String> = emptySet()
        var permFakeData: Set<String> = emptySet()
        




        var permGrantApp: Set<String>? = null
        var permFakeDataApp: Set<String>? = null
        



        fun effectiveGrant(): Set<String> {
            
            if (permGrant.isNotEmpty()) return permGrant
            
            return permGrantApp ?: emptySet()
        }

        
        






        fun batteryLevel(): Int {
            val base = exBattery.coerceIn(0, 100)
            if (!exBatteryEnable || !fakeBatteryDrain) return base
            val step = fakeBatteryDrainMin.coerceIn(1, 1440)
            val elapsedMin = (System.currentTimeMillis() - SessionStart.ms) / 60000L
            return (base - elapsedMin / step).coerceIn(0, 100).toInt()
        }

        fun effectiveFakeData(): Set<String> {
            if (permFakeData.isNotEmpty()) return permFakeData
            return permFakeDataApp ?: emptySet()
        }
    }

    



    private object SessionStart {
        val ms: Long = System.currentTimeMillis()
    }

    @Volatile
    private var lastMs: Long = 0L

    
    @Volatile
    private var hasContent: Boolean = false

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

        
        
        
        
        
        if (raw.isEmpty() && hasContent) return snapshot
        hasContent = hasContent || raw.isNotEmpty()

        val map: Map<String, Any?> = projectForHost(raw, packageName)
        
        Flags.logEnabled = raw[XpConfig.KEY_LOG_ENABLE] as? Boolean
            ?: runCatching { prefs.getBoolean(XpConfig.KEY_LOG_ENABLE, false) }.getOrDefault(false)
        snapshot = Snapshot().apply {
            enableAndroidId = map.bool(XpConfig.KEY_ENABLE_ANDROID_ID, false)
            androidId = map.str(XpConfig.KEY_ANDROID_ID, XpDefaults.ANDROID_ID)
            enableBuild = map.bool(XpConfig.KEY_ENABLE_BUILD, true)
            buildValues = XpConfig.BUILD_FIELDS.mapNotNull { f ->
                val v = map.str(f.key, "").trim()
                if (v.isEmpty()) null else f.field to v
            }.toMap()
            deviceName = map.str(XpConfig.KEY_DEVICE_NAME, "").trim()
            gsfId = map.str(XpConfig.KEY_GSF_ID, "").trim()
            adsId = map.str(XpConfig.KEY_ADS_ID, "").trim()
            appSetId = map.str(XpConfig.KEY_APPSET_ID, "").trim()
            drmId = map.str(XpConfig.KEY_DRM_ID, "").trim()
            hideAccounts = map.bool(XpConfig.KEY_HIDE_ACCOUNTS, false)
            wifiFakeEnable = map.bool(XpConfig.KEY_WIFI_FAKE_ENABLE, false)
            blockWifiSaved = map.bool(XpConfig.KEY_BLOCK_WIFI_SAVED, false)
            blockWakeLock = map.bool(XpConfig.KEY_BLOCK_WAKELOCK, false)
            wifiFakeSsid = map.str(XpConfig.KEY_WIFI_FAKE_SSID, "").trim()
            wifiFakeBssid = map.str(XpConfig.KEY_WIFI_FAKE_BSSID, "").trim()
            wifiFakeRssi = map.int(XpConfig.KEY_WIFI_FAKE_RSSI, -55)
            wifiFakeSpeed = map.int(XpConfig.KEY_WIFI_FAKE_SPEED, 300)
            wifiFakeIp = map.str(XpConfig.KEY_WIFI_FAKE_IP, "192.168.1.88").trim()
            wifiFakeFreq = map.int(XpConfig.KEY_WIFI_FAKE_FREQ, 5180)
            wifiFakeScan = map.bool(XpConfig.KEY_WIFI_FAKE_SCAN, true)
            wifiFakeList = XpConfig.decodeApList(map.str(XpConfig.KEY_WIFI_FAKE_LIST, ""))
            wifiFakeNetwork = map.bool(XpConfig.KEY_WIFI_FAKE_NETWORK, true)
            rootFakeEnable = map.bool(XpConfig.KEY_ROOT_FAKE_ENABLE, false)
            rootFakeFile = map.bool(XpConfig.KEY_ROOT_FAKE_FILE, true)
            rootFakeMask = map.bool(XpConfig.KEY_ROOT_FAKE_MASK, true)
            accStatusSpoof = map.bool(XpConfig.KEY_ACC_STATUS_SPOOF, true)
            accStatusValue = map.bool(XpConfig.KEY_ACC_STATUS_VALUE, false)
            accFakeMode = map.int(XpConfig.KEY_ACC_FAKE_MODE, XpConfig.ACC_MODE_DEFAULT)
            vpnHideEnable = map.bool(XpConfig.KEY_VPN_HIDE_ENABLE, false)
            vpnHideIface = map.bool(XpConfig.KEY_VPN_HIDE_IFACE, true)
            vpnHideCaps = map.bool(XpConfig.KEY_VPN_HIDE_CAPS, true)
            vpnHideNetInfo = map.bool(XpConfig.KEY_VPN_HIDE_NETINFO, true)
            vpnHideProxy = map.bool(XpConfig.KEY_VPN_HIDE_PROXY, true)
            vpnHideSettings = map.bool(XpConfig.KEY_VPN_HIDE_SETTINGS, true)
            vpnIfaces = XpConfig.decodeSet(
                map.str(XpConfig.KEY_VPN_IFACES, XpConfig.DEF_VPN_IFACES)
            )
            exWifiSsid = map.str(XpConfig.KEY_FAKE_WIFI_SSID, "").trim()
            exWifiBssid = map.str(XpConfig.KEY_FAKE_WIFI_BSSID, "").trim()
            exDevOff = map.bool(XpConfig.KEY_FAKE_DEV_OFF, false)
            exSimCountry = map.str(XpConfig.KEY_FAKE_SIM_COUNTRY, "").trim()
            exSimOperator = map.str(XpConfig.KEY_FAKE_SIM_OPERATOR, "").trim()
            exSimOperatorName = map.str(XpConfig.KEY_FAKE_SIM_OPERATOR_NAME, "").trim()
            exSimSerial = map.str(XpConfig.KEY_FAKE_SIM_SERIAL, "").trim()
            exSimSubscriber = map.str(XpConfig.KEY_FAKE_SIM_SUBSCRIBER, "").trim()
            exPhoneNumber = map.str(XpConfig.KEY_FAKE_PHONE_NUMBER, "").trim()
            exTimezone = map.str(XpConfig.KEY_FAKE_TIMEZONE, "").trim()
            exLocale = map.str(XpConfig.KEY_FAKE_LOCALE, "").trim()
            exTimeEnable = map.bool(XpConfig.KEY_FAKE_TIME_ENABLE, false)
            exTimeOffset = map.int(XpConfig.KEY_FAKE_TIME_OFFSET, 0)
            exUptimeEnable = map.bool(XpConfig.KEY_FAKE_UPTIME_ENABLE, false)
            exUptimeHours = map.str(XpConfig.KEY_FAKE_UPTIME_HOURS, "72").trim()
            exSdkInt = map.int(XpConfig.KEY_FAKE_SDK_INT, 0)
            exWifiMac = map.str(XpConfig.KEY_FAKE_WIFI_MAC, "").trim()
            exBtMac = map.str(XpConfig.KEY_FAKE_BT_MAC, "").trim()
            exImei = map.str(XpConfig.KEY_FAKE_IMEI, "").trim()
            exMeid = map.str(XpConfig.KEY_FAKE_MEID, "").trim()
            exIccid = map.str(XpConfig.KEY_FAKE_ICCID, "").trim()
            exCarrier = map.str(XpConfig.KEY_FAKE_CARRIER, "").trim()
            exFbFid = map.str(XpConfig.KEY_FAKE_FB_FID, "").trim()
            exFbIid = map.str(XpConfig.KEY_FAKE_FB_IID, "").trim()
            exHwSerial = map.str(XpConfig.KEY_FAKE_HW_SERIAL, "").trim()
            exKernel = map.str(XpConfig.KEY_FAKE_KERNEL, "").trim()
            exArch = map.str(XpConfig.KEY_FAKE_ARCH, "").trim()
            exCpuInfoHw = map.str(XpConfig.KEY_FAKE_CPUINFO_HW, "").trim()
            exPlatform = map.str(XpConfig.KEY_FAKE_PLATFORM, "").trim()
            exGpu = map.str(XpConfig.KEY_FAKE_GPU, "").trim()
            exGpuVendor = map.str(XpConfig.KEY_FAKE_GPU_VENDOR, "").trim()
            exGpuGlVersion = map.str(XpConfig.KEY_FAKE_GPU_GL_VERSION, "").trim()
            exGpuGlsl = map.str(XpConfig.KEY_FAKE_GPU_GLSL, "").trim()
            exGpuVkApi = map.str(XpConfig.KEY_FAKE_GPU_VK_API, "").trim()
            exGpuDriver = map.str(XpConfig.KEY_FAKE_GPU_DRIVER, "").trim()
            exGpuVendorId = map.str(XpConfig.KEY_FAKE_GPU_VENDOR_ID, "").trim()
            exGpuDeviceId = map.str(XpConfig.KEY_FAKE_GPU_DEVICE_ID, "").trim()
            exGpuMemoryMb = map.int(XpConfig.KEY_FAKE_GPU_MEMORY_MB, XpConfig.DEF_GPU_MEMORY_MB)
            exGpuMaxTex = map.int(XpConfig.KEY_FAKE_GPU_MAX_TEX, XpConfig.DEF_GPU_MAX_TEX)
            exGpuMaxCube = map.int(XpConfig.KEY_FAKE_GPU_MAX_CUBE, XpConfig.DEF_GPU_MAX_TEX)
            exGpuMaxLayers = map.int(XpConfig.KEY_FAKE_GPU_MAX_LAYERS, XpConfig.DEF_GPU_MAX_LAYERS)
            exGpuPush = map.int(XpConfig.KEY_FAKE_GPU_PUSH, XpConfig.DEF_GPU_PUSH)
            exCpuEnable = map.bool(XpConfig.KEY_FAKE_CPU_ENABLE, false)
            exCpuMode = map.str(XpConfig.KEY_FAKE_CPU_MODE, XpConfig.CPU_MODE_PRESET)
            exCpuPreset = map.int(XpConfig.KEY_FAKE_CPU_PRESET, 0)
            exCpuCustom = map.str(XpConfig.KEY_FAKE_CPU_CUSTOM, "")
            exCpuCores = map.int(XpConfig.KEY_FAKE_CPU_CORES, 8)
            exCpuMinFreq = map.str(XpConfig.KEY_FAKE_CPU_MIN_FREQ, "")
            exCpuMaxFreq = map.str(XpConfig.KEY_FAKE_CPU_MAX_FREQ, "")
            exCpuCurFreq = map.str(XpConfig.KEY_FAKE_CPU_CUR_FREQ, "")
            exTempEnable = map.bool(XpConfig.KEY_FAKE_TEMP_ENABLE, false)
            exTemp = map.int(XpConfig.KEY_FAKE_TEMP, XpConfig.DEF_TEMP)
            fakeBatteryDrain = map.bool(XpConfig.KEY_FAKE_BATTERY_DRAIN, false)
            fakeBatteryDrainMin = map.int(XpConfig.KEY_FAKE_BATTERY_DRAIN_MIN, 5)
            locEnable = map.bool(XpConfig.KEY_LOC_ENABLE, false)
            locFields = map.bool(XpConfig.KEY_LOC_FIELDS, true)
            locMode = map.int(XpConfig.KEY_LOC_MODE, 0)
            locLat = map.str(XpConfig.KEY_LOC_LAT, "")
            locLon = map.str(XpConfig.KEY_LOC_LON, "")
            locAlt = map.str(XpConfig.KEY_LOC_ALT, "")
            locAcc = map.str(XpConfig.KEY_LOC_ACC, "")
            locGps = map.bool(XpConfig.KEY_LOC_GPS, true)
            locNet = map.bool(XpConfig.KEY_LOC_NET, true)
            locFused = map.bool(XpConfig.KEY_LOC_FUSED, true)
            locTelephony = map.bool(XpConfig.KEY_LOC_TELEPHONY, true)
            locStore = map.bool(XpConfig.KEY_LOC_STORE, false)
            locCell = map.bool(XpConfig.KEY_LOC_CELL, true)
            cellMcc = map.str(XpConfig.KEY_CELL_MCC, "")
            cellMnc = map.str(XpConfig.KEY_CELL_MNC, "")
            cellLac = map.str(XpConfig.KEY_CELL_LAC, "")
            cellCid = map.str(XpConfig.KEY_CELL_CID, "")
            cellPci = map.str(XpConfig.KEY_CELL_PCI, "")
            winSecureMode = map.int(XpConfig.KEY_WIN_SECURE_MODE, 0)
            winFlags = XpConfig.decodeLines(map.str(XpConfig.KEY_WIN_FLAGS, "")).toSet()
            exBatteryEnable = map.bool(XpConfig.KEY_FAKE_BATTERY_ENABLE, false)
            exBattery = map.int(XpConfig.KEY_FAKE_BATTERY, XpConfig.DEF_BATTERY)
            dpiEnable = map.bool(XpConfig.KEY_FAKE_DPI_ENABLE, false)
            dpiValue = map.int(XpConfig.KEY_FAKE_DPI, XpConfig.DEF_DPI)
            exMemEnable = map.bool(XpConfig.KEY_FAKE_MEM_ENABLE, false)
            exMemMb = map.int(XpConfig.KEY_FAKE_MEM_MB, XpConfig.DEF_MEM_MB)
            mem2Enable = map.bool(XpConfig.KEY_FAKE_MEM2_ENABLE, false)
            memTotalMb = map.int(XpConfig.KEY_FAKE_MEM_TOTAL_MB, XpConfig.DEF_MEM_TOTAL_MB)
            memAvailMb = map.int(XpConfig.KEY_FAKE_MEM_AVAIL_MB, XpConfig.DEF_MEM_AVAIL_MB)
            storTotalGb = map.int(XpConfig.KEY_FAKE_STOR_TOTAL_GB, XpConfig.DEF_STOR_TOTAL_GB)
            storAvailGb = map.int(XpConfig.KEY_FAKE_STOR_AVAIL_GB, XpConfig.DEF_STOR_AVAIL_GB)
            dispEnable = map.bool(XpConfig.KEY_FAKE_DISPLAY_ENABLE, false)
            resW = map.int(XpConfig.KEY_FAKE_RES_W, XpConfig.DEF_RES_W)
            resH = map.int(XpConfig.KEY_FAKE_RES_H, XpConfig.DEF_RES_H)
            refreshHz = map.str(XpConfig.KEY_FAKE_REFRESH, XpConfig.DEF_REFRESH.toString())
                .toFloatOrNull() ?: XpConfig.DEF_REFRESH.toFloat()
            refreshList = map.str(XpConfig.KEY_FAKE_REFRESH_LIST, XpConfig.DEF_REFRESH_LIST)
            camEnable = map.bool(XpConfig.KEY_FAKE_CAM_ENABLE, false)
            camBackMp = map.int(XpConfig.KEY_FAKE_CAM_BACK_MP, XpConfig.DEF_CAM_BACK_MP)
            camFrontMp = map.int(XpConfig.KEY_FAKE_CAM_FRONT_MP, XpConfig.DEF_CAM_FRONT_MP)
            netpEnable = map.bool(XpConfig.KEY_FAKE_NETP_ENABLE, false)
            ipv4 = map.str(XpConfig.KEY_FAKE_IPV4, "")
            ipv6 = map.str(XpConfig.KEY_FAKE_IPV6, "")
            dns1 = map.str(XpConfig.KEY_FAKE_DNS1, "")
            dns2 = map.str(XpConfig.KEY_FAKE_DNS2, "")
            gateway = map.str(XpConfig.KEY_FAKE_GATEWAY, "")
            wifiStd = map.str(XpConfig.KEY_FAKE_WIFI_STD, XpConfig.DEF_WIFI_STD)
            batexEnable = map.bool(XpConfig.KEY_FAKE_BATEX_ENABLE, false)
            batStatus = map.str(XpConfig.KEY_FAKE_BAT_STATUS, XpConfig.DEF_BAT_STATUS)
            batCurrentMa = map.int(XpConfig.KEY_FAKE_BAT_CURRENT_MA, XpConfig.DEF_BAT_CURRENT_MA)
            batVoltageMv = map.int(XpConfig.KEY_FAKE_BAT_VOLTAGE_MV, XpConfig.DEF_BAT_VOLTAGE_MV)
            batDesignMah = map.int(XpConfig.KEY_FAKE_BAT_DESIGN_MAH, XpConfig.DEF_BAT_DESIGN_MAH)
            simEsim = map.bool(XpConfig.KEY_FAKE_SIM_ESIM, false)
            simEsimActive = map.bool(XpConfig.KEY_FAKE_SIM_ESIM_ACTIVE, false)
            simData = map.bool(XpConfig.KEY_FAKE_SIM_DATA, false)
            simRoamEnable = map.bool(XpConfig.KEY_FAKE_SIM_ROAM_ENABLE, false)
            simRoam = map.bool(XpConfig.KEY_FAKE_SIM_ROAM, false)
            exOaid = map.str(XpConfig.KEY_OAID, "").trim()
            gmsEnable = map.bool(XpConfig.KEY_GMS_ENABLE, false)
            gmsInstalled = map.bool(XpConfig.KEY_GMS_INSTALLED, true)
            gmsAvailable = map.bool(XpConfig.KEY_GMS_AVAILABLE, true)
            gmsAdidEnable = map.bool(XpConfig.KEY_GMS_ADID_ENABLE, true)
            gmsAdid = map.str(XpConfig.KEY_GMS_ADID, "").trim()
            gmsAdidLimit = map.bool(XpConfig.KEY_GMS_ADID_LIMIT, false)
            gmsVersion = map.str(XpConfig.KEY_GMS_VERSION, XpConfig.DEF_GMS_VERSION).trim()
            gmsVersionCode = map.int(XpConfig.KEY_GMS_VERSION_CODE, XpConfig.DEF_GMS_VERSION_CODE)
            hmsEnable = map.bool(XpConfig.KEY_HMS_ENABLE, false)
            hmsInstalled = map.bool(XpConfig.KEY_HMS_INSTALLED, true)
            hmsAvailable = map.bool(XpConfig.KEY_HMS_AVAILABLE, true)
            hmsAdidEnable = map.bool(XpConfig.KEY_HMS_ADID_ENABLE, true)
            hmsVersion = map.str(XpConfig.KEY_HMS_VERSION, XpConfig.DEF_HMS_VERSION).trim()
            hmsVersionCode = map.int(XpConfig.KEY_HMS_VERSION_CODE, XpConfig.DEF_HMS_VERSION_CODE)
            
            
            
            nativeHook = raw.bool(XpConfig.KEY_NATIVE_HOOK, XpConfig.DEF_NATIVE_HOOK)
            nativeBlockExit = map.bool(
                XpConfig.KEY_NATIVE_BLOCK_EXIT, XpConfig.DEF_NATIVE_BLOCK_EXIT
            )
            nativeAntiDetect = raw.bool(
                XpConfig.KEY_NATIVE_ANTI_DETECT, XpConfig.DEF_NATIVE_ANTI_DETECT
            )
            nativeAppMode = map.int(XpConfig.KEY_NATIVE_APP_MODE, XpConfig.NATIVE_APP_DEFAULT)
            enableJs = map.bool(XpConfig.KEY_ENABLE_JS, false)
            jsCode = map.str(XpConfig.KEY_JS_CODE, XpDefaults.JS)
            enableUa = map.bool(XpConfig.KEY_ENABLE_UA, false)
            uaValue = map.str(XpConfig.KEY_UA_VALUE, "")
            blockCrashEnable = map.bool(XpConfig.KEY_BLOCK_CRASH_ENABLE, true)
            blockCrashLevel = map.int(XpConfig.KEY_BLOCK_CRASH_LEVEL, 1)
            crashCatchEnable = map.bool(XpConfig.KEY_CRASH_CATCH_ENABLE, true)
            crashCopyClipboard = map.bool(XpConfig.KEY_CRASH_COPY_CLIPBOARD, false)
            crashWriteFile = map.bool(XpConfig.KEY_CRASH_WRITE_FILE, true)
            crashIntercept = map.bool(XpConfig.KEY_CRASH_INTERCEPT, false)
            exitEnable = map.bool(XpConfig.KEY_EXIT_ENABLE, false)
            exitSeconds = map.int(XpConfig.KEY_EXIT_SECONDS, XpConfig.DEF_EXIT_SECONDS)
            exitMethods = XpConfig.decodeSet(map.str(XpConfig.KEY_EXIT_METHODS, XpConfig.DEF_EXIT_METHODS))
            exitParallel = map.bool(XpConfig.KEY_EXIT_PARALLEL, false)
            accEnable = map.bool(XpConfig.KEY_ACC_ENABLE, false)
            accMode = map.int(XpConfig.KEY_ACC_MODE, 0)
            accScope = map.int(XpConfig.KEY_ACC_SCOPE, 1)
            accCapScreen = map.bool(XpConfig.KEY_ACC_CAP_SCREEN, true)
            accCapNotify = map.bool(XpConfig.KEY_ACC_CAP_NOTIFY, true)
            accCapWindow = map.bool(XpConfig.KEY_ACC_CAP_WINDOW, true)
            accCapInput = map.bool(XpConfig.KEY_ACC_CAP_INPUT, true)
            accCapAction = map.bool(XpConfig.KEY_ACC_CAP_ACTION, true)
            accCapOverlay = map.bool(XpConfig.KEY_ACC_CAP_OVERLAY, true)
            accCapControl = map.bool(XpConfig.KEY_ACC_CAP_CONTROL, true)
            blockOverlay = map.bool(XpConfig.KEY_BLOCK_OVERLAY, false)
            overlayUntouchable = map.bool(XpConfig.KEY_OVERLAY_UNTouchABLE, false)
            overlayTransparent = map.bool(XpConfig.KEY_OVERLAY_TRANSPARENT, false)
            overlayMaxPercent = map.int(XpConfig.KEY_OVERLAY_MAX_PERCENT, 0).coerceIn(0, 100)
            blockWallpaper = map.bool(XpConfig.KEY_BLOCK_WALLPAPER, false)
            blockHideRecents = map.bool(XpConfig.KEY_BLOCK_HIDE_RECENTS, false)
            blockScreenOff = map.bool(XpConfig.KEY_BLOCK_SCREEN_OFF, false)
            screenOffWakeLock = map.bool(XpConfig.KEY_SCREEN_OFF_WAKELOCK, false)
            screenOffReflect = map.bool(XpConfig.KEY_SCREEN_OFF_REFLECT, false)
            blockNotifyHide = map.bool(XpConfig.KEY_BLOCK_NOTIFY_HIDE, false)
            blockProvider = map.bool(XpConfig.KEY_BLOCK_PROVIDER, false)
            blockFgService = map.bool(XpConfig.KEY_BLOCK_FOREGROUND_SERVICE, false)
            daBlockRequest = map.bool(XpConfig.KEY_DA_BLOCK_REQUEST, false)
            exitCountdown = map.bool(XpConfig.KEY_EXIT_COUNTDOWN, true)
            condAtTime = map.bool(XpConfig.KEY_COND_AT_TIME, false)
            condAtTimeHh = map.int(XpConfig.KEY_COND_AT_TIME_HH, 18)
            condAtTimeMm = map.int(XpConfig.KEY_COND_AT_TIME_MM, 0)
            condCountdown = map.bool(XpConfig.KEY_COND_COUNTDOWN, false)
            condCountdownMin = map.int(XpConfig.KEY_COND_COUNTDOWN_MIN, 30)
            condMem = map.bool(XpConfig.KEY_COND_MEM, false)
            condMemMb = map.int(XpConfig.KEY_COND_MEM_MB, 1024)
            condCpu = map.bool(XpConfig.KEY_COND_CPU, false)
            condCpuPct = map.int(XpConfig.KEY_COND_CPU_PCT, 80)
            condDisk = map.bool(XpConfig.KEY_COND_DISK, false)
            condDiskMb = map.int(XpConfig.KEY_COND_DISK_MB, 500)
            condNet = map.bool(XpConfig.KEY_COND_NET, false)
            condNetMode = map.int(XpConfig.KEY_COND_NET_MODE, 0)
            condBatt = map.bool(XpConfig.KEY_COND_BATT, false)
            condBattMode = map.int(XpConfig.KEY_COND_BATT_MODE, 0)
            condBattPct = map.int(XpConfig.KEY_COND_BATT_PCT, 10)
            condFile = map.bool(XpConfig.KEY_COND_FILE, false)
            condFilePaths = XpConfig.decodeLines(map.str(XpConfig.KEY_COND_FILE_PATHS, "")).toList()
            condFileEvent = map.int(XpConfig.KEY_COND_FILE_EVENT, 0)
            condIdle = map.bool(XpConfig.KEY_COND_IDLE, false)
            condIdleMin = map.int(XpConfig.KEY_COND_IDLE_MIN, 10)
            blockExec = map.bool(XpConfig.KEY_BLOCK_EXEC, false)
            volumeEnable = map.bool(XpConfig.KEY_VOLUME_ENABLE, false)
            volumeMaster = map.bool(XpConfig.KEY_VOLUME_MASTER, false)
            volumeLock = map.bool(XpConfig.KEY_VOLUME_LOCK, false)
            volumeLockValue = map.int(XpConfig.KEY_VOLUME_LOCK_VALUE, 50)
            audioOutEnable = map.bool(XpConfig.KEY_AUDIO_OUT_ENABLE, false)
            audioOutBlocked = XpConfig.AUDIO_OUT_ITEMS
                .filter { map.bool(it.first, false) }
                .map { it.first }.toSet()
            audioOutFocus = map.bool(XpConfig.KEY_AUDIO_OUT_FOCUS, true)
            audioOutNoFocus = map.bool(XpConfig.KEY_AUDIO_OUT_NO_FOCUS, true)
            volumeAllowLower = map.bool(XpConfig.KEY_VOLUME_ALLOW_LOWER, false)
            volumeBlockRinger = map.bool(XpConfig.KEY_VOLUME_BLOCK_RINGER, true)
            volumeBlockMute = map.bool(XpConfig.KEY_VOLUME_BLOCK_MUTE, true)
            wdbgEnable = map.bool(XpConfig.KEY_WDBG_ENABLE, false)
            wdbgToggle = map.bool(XpConfig.KEY_WDBG_TOGGLE, true)
            wdbgPair = map.bool(XpConfig.KEY_WDBG_PAIR, true)
            wdbgDiscover = map.bool(XpConfig.KEY_WDBG_DISCOVER, true)
            wdbgProp = map.bool(XpConfig.KEY_WDBG_PROP, true)
            wdbgBlockJump = map.bool(XpConfig.KEY_WDBG_BLOCK_JUMP, true)
            wdbgBlockSettings = map.bool(XpConfig.KEY_WDBG_BLOCK_SETTINGS, false)
            shizukuEnable = map.bool(XpConfig.KEY_SHIZUKU_ENABLE, false)
            shizukuBlockAuth = map.bool(XpConfig.KEY_SHIZUKU_BLOCK_AUTH, true)
            shizukuBlockUse = map.bool(XpConfig.KEY_SHIZUKU_BLOCK_USE, true)
            blockConnEnable = map.bool(XpConfig.KEY_BLOCK_CONN_ENABLE, false)
            blockConnWifi = map.bool(XpConfig.KEY_BLOCK_CONN_WIFI, true)
            blockConnBt = map.bool(XpConfig.KEY_BLOCK_CONN_BT, true)
            blockConnBright = map.bool(XpConfig.KEY_BLOCK_CONN_BRIGHT, true)
            blockSensor = map.bool(XpConfig.KEY_BLOCK_SENSOR, false)
            blockJumpEnable = map.bool(XpConfig.KEY_BLOCK_JUMP_ENABLE, false)
            jumpWhitelist = XpConfig.decodeLines(map.str(XpConfig.KEY_BLOCK_JUMP_WHITELIST, ""))
            blockBgLaunch = map.bool(XpConfig.KEY_BLOCK_BG_LAUNCH, false)
            blockBgLaunchPending = map.bool(XpConfig.KEY_BLOCK_BG_LAUNCH_PENDING, true)
            blockBgLaunchStrict = map.bool(XpConfig.KEY_BLOCK_BG_LAUNCH_STRICT, true)
            blockCamera = map.bool(XpConfig.KEY_BLOCK_CAMERA_ENABLE, false)
            blockMic = map.bool(XpConfig.KEY_BLOCK_MIC_ENABLE, false)
            blockInstall = map.bool(XpConfig.KEY_BLOCK_INSTALL_ENABLE, false)
            blockPrintCast = map.bool(XpConfig.KEY_BLOCK_PRINT_CAST, false)
            blockNotify = map.bool(XpConfig.KEY_BLOCK_NOTIFY, false)
            netFilterEnable = map.bool(XpConfig.KEY_NET_FILTER_ENABLE, false)
            netFilterWhitelist = map.bool(XpConfig.KEY_NET_FILTER_WHITELIST, false)
            netFilterList = XpConfig.decodeLines(map.str(XpConfig.KEY_NET_FILTER_LIST, ""))
            blockScreenCapture = map.bool(XpConfig.KEY_BLOCK_SCREEN_CAPTURE, false)
            screenCaptureMode = map.int(XpConfig.KEY_SCREEN_CAPTURE_MODE, XpConfig.SC_MODE_BLANK)
            screenCaptureGrantOk = map.bool(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, false)
            blockTorch = map.bool(XpConfig.KEY_BLOCK_TORCH, false)
            blockVibrate = map.bool(XpConfig.KEY_BLOCK_VIBRATE, false)
            blockKeyConsume = map.bool(XpConfig.KEY_BLOCK_KEY_CONSUME, false)
            hasConfig = raw.isNotEmpty()
            blockKeyPassBack = map.bool(XpConfig.KEY_BLOCK_KEY_PASS_BACK, true)
            screenCaptureFit = map.bool(XpConfig.KEY_SCREEN_CAPTURE_FIT, true)
            fileGuardEnable = map.bool(XpConfig.KEY_FILE_GUARD_ENABLE, false)
            fileOpAll = map.bool(XpConfig.KEY_FILE_OP_ALL, false)
            fileOps = XpConfig.FILE_OP_ITEMS
                .filter { map.bool(it.first, true) }
                .map { it.first }.toSet()
            hideAppsEnable = map.bool(XpConfig.KEY_HIDE_APPS_ENABLE, false)
            hideAppsMode = map.int(XpConfig.KEY_HIDE_APPS_MODE, 1)
            hideAppsListMode = map.int(XpConfig.KEY_HIDE_APPS_LIST_MODE, 0)
            hideAppsList = XpConfig.decodeLines(map.str(XpConfig.KEY_HIDE_APPS_LIST, ""))
            clipEnable = map.bool(XpConfig.KEY_CLIP_ENABLE, false)
            clipMode = map.int(XpConfig.KEY_CLIP_MODE, 2)
            panelInject = map.bool(XpConfig.KEY_PANEL_INJECT, true)
            daEnable = map.bool(XpConfig.KEY_DA_ENABLE, false)
            daFakeMode = map.int(XpConfig.KEY_DA_FAKE_MODE, XpConfig.DA_MODE_DEFAULT)
            daScope = map.int(XpConfig.KEY_DA_SCOPE, XpConfig.DA_SCOPE_CLOSE_AND_HOOK)
            daCloseMode = map.int(XpConfig.KEY_DA_CLOSE_MODE, XpConfig.DA_CLOSE_CONTINUOUS)
            daMaster = map.bool(XpConfig.KEY_DA_MASTER, false)
            daLock = map.bool(XpConfig.KEY_DA_LOCK, true)
            daPassword = map.bool(XpConfig.KEY_DA_PASSWORD, true)
            daWipe = map.bool(XpConfig.KEY_DA_WIPE, true)
            daCamera = map.bool(XpConfig.KEY_DA_CAMERA, true)
            daAppMgmt = map.bool(XpConfig.KEY_DA_APPMGMT, true)
            daSystem = map.bool(XpConfig.KEY_DA_SYSTEM, true)
            daPermission = map.bool(XpConfig.KEY_DA_PERMISSION, true)
            daEncrypt = map.bool(XpConfig.KEY_DA_ENCRYPT, true)
            
            permEnable = map.bool(XpConfig.KEY_PERM_ENABLE, false) ||
                    XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_GRANT, "")).isNotEmpty() ||
                    XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_FAKE_DATA, "")).isNotEmpty()
            permDefaultAll = map.bool(XpConfig.KEY_PERM_DEFAULT_ALL, true)
            permGrant = XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_GRANT, ""))
            permFakeData = XpConfig.decodeSet(map.str(XpConfig.KEY_PERM_FAKE_DATA, ""))
            permGrantApp = readAppSet(
                prefs, map, XpConfig.PREFIX_APP_GRANT, XpConfig.LEGACY_GRANT_READ, "授权组"
            )
            permFakeDataApp = readAppSet(
                prefs, map, XpConfig.PREFIX_APP_FAKE, XpConfig.LEGACY_FAKE_READ, "假数据组"
            )
        }
        snapshot = resolveNative(snapshot)
        return snapshot
    }

    








    private fun resolveNative(s: Snapshot): Snapshot {
        val on = when (s.nativeAppMode) {
            XpConfig.NATIVE_APP_ON -> true
            XpConfig.NATIVE_APP_OFF -> false
            else -> s.nativeHook
        }
        val groups = if (on) XpConfig.nativeGroups(s).first else 0
        
        s.nativeHook = on
        s.nativeActive = on && groups != 0
        s.nativeGroups = groups
        return s
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

    



    private const val APP_SET_CACHE_MS = 10_000L

    
    private const val APP_SET_MISS_CACHE_MS = 60_000L

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
            if (e.pkg != pkg) return@let
            val ttl = if (e.value == null) APP_SET_MISS_CACHE_MS else APP_SET_CACHE_MS
            if (now - e.ms < ttl) return e.value
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
            runCatching { prefs.all?.keys }.getOrNull().orEmpty() + map.keys
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