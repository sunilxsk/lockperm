package io.github.sunilxsk.lockperm

import android.Manifest





object XpConfig {

    const val PREFS = "xp_config"

    

    




    const val APP_PREFIX = "cfg_"

    fun appPrefix(pkg: String): String = APP_PREFIX + pkg + "_"

    fun appKey(pkg: String, key: String): String = APP_PREFIX + pkg + "_" + key

    
    
    
    
    const val KEY_NATIVE_HOOK = "native_hook_enable"
    const val DEF_NATIVE_HOOK = true

    
    const val KEY_NATIVE_BLOCK_EXIT = "native_block_exit"
    const val DEF_NATIVE_BLOCK_EXIT = false

    



    const val KEY_NATIVE_ANTI_DETECT = "native_anti_detect"
    const val DEF_NATIVE_ANTI_DETECT = true

    







    val GLOBAL_KEYS: Set<String> = setOf(
        KEY_NATIVE_HOOK,
        KEY_NATIVE_ANTI_DETECT,
        KEY_LOG_ENABLE,
    )

    
    fun isGlobalKey(key: String): Boolean = key in GLOBAL_KEYS

    
    fun physicalKey(pkg: String?, key: String): String =
        if (isGlobalKey(key) || pkg.isNullOrBlank()) key else appKey(pkg, key)

    
    const val KEY_ENABLE_ANDROID_ID = "enable_android_id"
    const val KEY_ANDROID_ID = "android_id_value"

    
    const val KEY_ENABLE_BUILD = "enable_build_info"
    const val KEY_BUILD_MODEL = "build_model"
    const val KEY_BUILD_BRAND = "build_brand"
    const val KEY_BUILD_MANUFACTURER = "build_manufacturer"
    const val KEY_BUILD_FINGERPRINT = "build_fingerprint"
    const val KEY_BUILD_SERIAL = "build_serial"
    const val KEY_BUILD_HARDWARE = "build_hardware"
    const val KEY_BUILD_BOARD = "build_board"
    const val KEY_BUILD_DEVICE = "build_device"
    const val KEY_BUILD_SOC_MODEL = "build_soc_model"
    const val KEY_BUILD_PRODUCT = "build_product"
    const val KEY_BUILD_BOOTLOADER = "build_bootloader"
    const val KEY_BUILD_DISPLAY = "build_display"
    const val KEY_BUILD_RADIO = "build_radio"
    const val KEY_BUILD_HOST = "build_host"
    const val KEY_BUILD_TAGS = "build_tags"
    const val KEY_BUILD_TYPE = "build_type"
    const val KEY_BUILD_ID = "build_id"
    const val KEY_BUILD_USER = "build_user"
    const val KEY_BUILD_RELEASE = "build_release"
    const val KEY_BUILD_SECURITY_PATCH = "build_security_patch"
    const val KEY_BUILD_INCREMENTAL = "build_incremental"
    const val KEY_BUILD_CODENAME = "build_codename"
    const val KEY_BUILD_BASE_OS = "build_base_os"

    
    const val KEY_DEVICE_NAME = "device_name"
    
    const val KEY_GSF_ID = "gsf_id"
    
    const val KEY_ADS_ID = "ads_id"
    
    const val KEY_APPSET_ID = "appset_id"
    
    const val KEY_DRM_ID = "drm_id"
    
    const val KEY_HIDE_ACCOUNTS = "hide_accounts"

    
    
    
    const val KEY_WIFI_FAKE_ENABLE = "wifi_fake_enable"
    const val KEY_WIFI_FAKE_SSID = "wifi_fake_ssid"
    const val KEY_WIFI_FAKE_BSSID = "wifi_fake_bssid"
    const val KEY_WIFI_FAKE_RSSI = "wifi_fake_rssi"
    const val KEY_WIFI_FAKE_SPEED = "wifi_fake_speed"
    const val KEY_WIFI_FAKE_IP = "wifi_fake_ip"
    const val KEY_WIFI_FAKE_FREQ = "wifi_fake_freq"
    const val KEY_WIFI_FAKE_SCAN = "wifi_fake_scan"
    const val KEY_WIFI_FAKE_LIST = "wifi_fake_list"
    const val KEY_WIFI_FAKE_NETWORK = "wifi_fake_network"

    
    
    
    const val KEY_ROOT_FAKE_ENABLE = "root_fake_enable"
    const val KEY_ROOT_FAKE_FILE = "root_fake_file"
    const val KEY_ROOT_FAKE_MASK = "root_fake_mask"

    
    
    
    const val KEY_ACC_STATUS_SPOOF = "acc_status_spoof"
    const val KEY_ACC_STATUS_VALUE = "acc_status_value"

    
    
    
    const val KEY_VPN_HIDE_ENABLE = "vpn_hide_enable"
    const val KEY_VPN_HIDE_IFACE = "vpn_hide_iface"
    const val KEY_VPN_HIDE_CAPS = "vpn_hide_caps"
    const val KEY_VPN_HIDE_NETINFO = "vpn_hide_netinfo"
    const val KEY_VPN_HIDE_PROXY = "vpn_hide_proxy"
    const val KEY_VPN_HIDE_SETTINGS = "vpn_hide_settings"
    const val KEY_VPN_IFACES = "vpn_ifaces"

    const val DEF_VPN_IFACES =
        "tun0,tun1,tun2,tun3,ppp0,ppp1,ppp2,utun0,utun1,utun2,wg0,wg1,wg2," +
                "tap0,tap1,ipsec0,ipsec1,tun,ppp,utun"

    
    
    
    const val KEY_FAKE_WIFI_SSID = "fake_wifi_ssid"
    const val KEY_FAKE_WIFI_BSSID = "fake_wifi_bssid"
    const val KEY_FAKE_DEV_OFF = "fake_dev_off"
    const val KEY_FAKE_SIM_COUNTRY = "fake_sim_country"
    const val KEY_FAKE_SIM_OPERATOR = "fake_sim_operator"
    const val KEY_FAKE_SIM_OPERATOR_NAME = "fake_sim_operator_name"
    const val KEY_FAKE_SIM_SERIAL = "fake_sim_serial"
    const val KEY_FAKE_SIM_SUBSCRIBER = "fake_sim_subscriber"
    const val KEY_FAKE_PHONE_NUMBER = "fake_phone_number"
    const val KEY_FAKE_TIMEZONE = "fake_timezone"
    const val KEY_FAKE_LOCALE = "fake_locale"
    const val KEY_FAKE_TIME_ENABLE = "fake_time_enable"
    const val KEY_FAKE_TIME_OFFSET = "fake_time_offset"
    const val KEY_FAKE_UPTIME_ENABLE = "fake_uptime_enable"
    const val KEY_FAKE_UPTIME_HOURS = "fake_uptime_hours"
    const val KEY_FAKE_SDK_INT = "fake_sdk_int"
    const val KEY_FAKE_WIFI_MAC = "fake_wifi_mac"
    const val KEY_FAKE_BT_MAC = "fake_bt_mac"
    const val KEY_FAKE_IMEI = "fake_imei"
    const val KEY_FAKE_MEID = "fake_meid"
    const val KEY_FAKE_ICCID = "fake_iccid"
    const val KEY_FAKE_CARRIER = "fake_carrier"
    const val KEY_FAKE_FB_FID = "fake_fb_fid"
    const val KEY_FAKE_FB_IID = "fake_fb_iid"
    const val KEY_FAKE_HW_SERIAL = "fake_hw_serial"

    
    
    
    
    
    
    const val KEY_CUSTOM_PROPS = "custom_props"

    
    
    
    const val KEY_HIDE_PATHS = "hide_paths"

    const val KEY_FAKE_KERNEL = "fake_kernel"
    const val KEY_FAKE_ARCH = "fake_arch"
    const val KEY_FAKE_CPUINFO_HW = "fake_cpuinfo_hw"
    const val KEY_FAKE_PLATFORM = "fake_platform"
    const val KEY_FAKE_GPU = "fake_gpu"
    
    const val KEY_FAKE_GPU_VENDOR = "fake_gpu_vendor"
    const val KEY_FAKE_GPU_GL_VERSION = "fake_gpu_gl_version"
    const val KEY_FAKE_GPU_GLSL = "fake_gpu_glsl"
    const val KEY_FAKE_GPU_VK_API = "fake_gpu_vk_api"
    const val KEY_FAKE_GPU_DRIVER = "fake_gpu_driver"
    const val KEY_FAKE_GPU_VENDOR_ID = "fake_gpu_vendor_id"
    const val KEY_FAKE_GPU_DEVICE_ID = "fake_gpu_device_id"
    const val KEY_FAKE_GPU_MEMORY_MB = "fake_gpu_memory_mb"
    const val KEY_FAKE_GPU_MAX_TEX = "fake_gpu_max_tex"
    const val KEY_FAKE_GPU_MAX_CUBE = "fake_gpu_max_cube"
    const val KEY_FAKE_GPU_MAX_LAYERS = "fake_gpu_max_layers"
    const val KEY_FAKE_GPU_PUSH = "fake_gpu_push"

    
    const val KEY_FAKE_CPU_ENABLE = "fake_cpu_enable"
    const val KEY_FAKE_CPU_MODE = "fake_cpu_mode"          
    const val KEY_FAKE_CPU_PRESET = "fake_cpu_preset"      
    const val KEY_FAKE_CPU_CUSTOM = "fake_cpu_custom"      
    const val KEY_FAKE_CPU_CORES = "fake_cpu_cores"
    const val KEY_FAKE_TEMP_ENABLE = "fake_temp_enable"
    const val KEY_FAKE_TEMP = "fake_temp"
    const val KEY_FAKE_BATTERY_ENABLE = "fake_battery_enable"
    const val KEY_FAKE_BATTERY = "fake_battery"
    const val KEY_OAID = "fake_oaid"

    const val DEF_KERNEL = "6.6.28-android15-8-g3f2a1b4c5d6-ab12345678"
    const val DEF_ARCH = "aarch64"
    const val DEF_TEMP = 99
    const val DEF_BATTERY = 88

    const val DEF_GPU_MEMORY_MB = 7469
    const val DEF_GPU_MAX_TEX = 16384
    const val DEF_GPU_MAX_LAYERS = 4096
    const val DEF_GPU_PUSH = 256

    
    
    
    
    
    
    const val CPU_MODE_PRESET = "preset"
    const val CPU_MODE_CUSTOM = "custom"

    data class CpuPreset(
        val soc: String,        
        val board: String,      
        val cpuinfo: String,    
    )

    val CPU_PRESETS: List<CpuPreset> by lazy { buildCpuPresets() }

    fun cpuPresetNames(): List<String> =
        listOf("骁龙 8 Gen 1", "天玑 9000", "Exynos 2200", "麒麟 9000", "骁龙 865")

    private fun buildCpuPresets(): List<CpuPreset> = listOf(
        CpuPreset("SM8450", "sm8450", cpuinfoOf("SM8450", "0x41", "0xd4b", 8, "Qualcomm")),
        CpuPreset("MT6983", "mt6983", cpuinfoOf("MT6983", "0x41", "0xd4c", 8, "MediaTek")),
        CpuPreset("S5E9925", "s5e9925", cpuinfoOf("S5E9925", "0x53", "0x001", 8, "Samsung")),
        CpuPreset("Kirin 9000", "hi3660", cpuinfoOf("Kirin 9000", "0x48", "0xd02", 8, "HiSilicon")),
        CpuPreset("SM8250", "sm8250", cpuinfoOf("SM8250", "0x41", "0xd44", 8, "Qualcomm")),
    )

    
    fun cpuinfoOf(soc: String, implementer: String, part: String, cores: Int, vendor: String): String {
        val sb = StringBuilder()
        val feats = "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp " +
                "cpuid asimdrdm lrcpc dcpop asimddp"
        
        for (i in 0 until cores) {
            val big = i >= cores - 2
            sb.append("processor\t: $i\n")
            sb.append("BogoMIPS\t: 38.40\n")
            sb.append("Features\t: $feats\n")
            sb.append("CPU implementer\t: $implementer\n")
            sb.append("CPU architecture: 8\n")
            sb.append("CPU variant\t: ${if (big) 0x2 else 0x1}\n")
            sb.append("CPU part\t: ${if (big) part else "0xd05"}\n")
            sb.append("CPU revision\t: ${i % 4}\n\n")
        }
        sb.append("Hardware\t: $soc\n")
        return sb.toString()
    }

    
    fun vkApiVersion(text: String): Int {
        val p = text.split(".").map { it.trim().toIntOrNull() ?: 0 }
        if (p.isEmpty()) return 0
        val major = p[0]
        val minor = p.getOrNull(1) ?: 0
        val patch = p.getOrNull(2) ?: 0
        return ((major shl 22) or (minor shl 12) or patch)
    }

    
    fun hexInt(text: String): Int =
        text.trim().removePrefix("0x").removePrefix("0X").toIntOrNull(16) ?: 0

    
    val ANDROID_VERSIONS: List<Pair<String, Int>> = listOf(
        "10" to 29, "11" to 30, "12" to 31, "12L" to 32,
        "13" to 33, "14" to 34, "15" to 35, "16" to 36,
    )

    fun releaseFor(sdk: Int): String =
        ANDROID_VERSIONS.lastOrNull { it.second <= sdk }?.first ?: ""

    fun sdkFor(release: String): Int =
        ANDROID_VERSIONS.firstOrNull { it.first.equals(release, true) }?.second ?: 0

    
    fun gpuForSoc(soc: String): String {
        val s = soc.uppercase()
        val direct = GPU_BY_SOC[s]
        if (direct != null) return direct
        return when {
            s.startsWith("SM8") -> "Adreno (TM) 750"
            s.startsWith("SM7") -> "Adreno (TM) 660"
            s.startsWith("SM6") -> "Adreno (TM) 640"
            s.contains("TENSOR") -> "Mali-G715-Immortalis MC7"
            s.startsWith("MT7") -> "Mali-G720 Immortalis MP12"
            s.contains("KIRIN") -> "Maleoon 910"
            else -> "Adreno (TM) 750"
        }
    }

    private val GPU_BY_SOC: Map<String, String> = mapOf(
        "SM8650" to "Adreno (TM) 750",
        "SM8635" to "Adreno (TM) 735",
        "SM8550" to "Adreno (TM) 740",
        "SM8475" to "Adreno (TM) 730",
        "SM8450" to "Adreno (TM) 730",
        "SM8350" to "Adreno (TM) 660",
        "TENSOR G3" to "Mali-G715-Immortalis MC7",
        "TENSOR G4" to "Mali-G715-Immortalis MC7",
        "MT6989" to "Mali-G720 Immortalis MP12",
        "MT6895" to "Mali-G610 MC6",
        "KIRIN 9000S" to "Maleoon 910",
    )

    fun socVendor(soc: String): String {
        val s = soc.uppercase()
        return when {
            s.startsWith("SM") || s.contains("SNAPDRAGON") -> "Qualcomm"
            s.startsWith("MT") || s.contains("DIMENSITY") -> "MediaTek"
            s.contains("TENSOR") -> "Google"
            s.contains("KIRIN") -> "HiSilicon"
            s.contains("EXYNOS") -> "Samsung"
            else -> "Qualcomm"
        }
    }

    fun gpuVendor(gpu: String): String {
        val g = gpu.uppercase()
        return when {
            g.contains("ADRENO") -> "Qualcomm"
            g.contains("MALEOON") -> "HiSilicon"
            g.contains("MALI") || g.contains("IMMORTALIS") -> "ARM"
            g.contains("POWERVR") -> "Imagination Technologies"
            else -> "ARM"
        }
    }

    
    
    
    const val UI_PREFS = "ui_prefs"
    const val UI_THEME_MODE = "theme_mode"      
    const val UI_THEME_COLOR = "theme_color"    
    const val UI_APP_ICON = "app_icon"          
    const val UI_SCALE = "ui_scale"             

    const val THEME_DEFAULT = 0
    const val THEME_DYNAMIC = 1
    const val THEME_CUSTOM = 2

    const val DEF_UI_SCALE = 93
    const val DEF_THEME_COLOR = -14376723       

    
    val APP_ICON_ALIASES: List<String> = listOf(
        "io.github.sunilxsk.lockperm.AliasDefault",
        "io.github.sunilxsk.lockperm.AliasShield",
        "io.github.sunilxsk.lockperm.AliasLock",
        "io.github.sunilxsk.lockperm.AliasStar",
        "io.github.sunilxsk.lockperm.AliasVerified",
    )
    val APP_ICON_LABELS: List<String> = listOf("原版", "盾牌", "锁", "星", "认证")

    



    val BUILD_FIELDS: List<BuildField> = listOf(
        BuildField(KEY_BUILD_BRAND, "BRAND 品牌", "BRAND", "ro.product.brand"),
        BuildField(KEY_BUILD_MANUFACTURER, "MANUFACTURER 制造商", "MANUFACTURER", "ro.product.manufacturer"),
        BuildField(KEY_BUILD_MODEL, "MODEL 型号", "MODEL", "ro.product.model"),
        BuildField(KEY_BUILD_DEVICE, "DEVICE 设备名", "DEVICE", "ro.product.device"),
        BuildField(KEY_BUILD_PRODUCT, "PRODUCT 产品名", "PRODUCT", "ro.product.name"),
        BuildField(KEY_BUILD_BOARD, "BOARD 主板", "BOARD", "ro.product.board"),
        BuildField(KEY_BUILD_HARDWARE, "HARDWARE 硬件", "HARDWARE", "ro.hardware"),
        BuildField(KEY_BUILD_SOC_MODEL, "SOC_MODEL 芯片型号", "SOC_MODEL", "ro.soc.model"),
        BuildField(KEY_BUILD_FINGERPRINT, "FINGERPRINT 指纹", "FINGERPRINT", "ro.build.fingerprint"),
        BuildField(KEY_BUILD_ID, "ID 构建 ID", "ID", "ro.build.id"),
        BuildField(KEY_BUILD_DISPLAY, "DISPLAY 显示 ID", "DISPLAY", "ro.build.display.id"),
        BuildField(KEY_BUILD_TYPE, "TYPE 编译类型", "TYPE", "ro.build.type"),
        BuildField(KEY_BUILD_TAGS, "TAGS 编译标签", "TAGS", "ro.build.tags"),
        BuildField(KEY_BUILD_HOST, "HOST 编译主机", "HOST", "ro.build.host"),
        BuildField(KEY_BUILD_USER, "USER 编译用户", "USER", "ro.build.user"),
        BuildField(KEY_BUILD_BOOTLOADER, "BOOTLOADER", "BOOTLOADER", "ro.bootloader"),
        BuildField(KEY_BUILD_RADIO, "RADIO 基带版本", "RADIO", "ro.build.radio"),
        BuildField(KEY_BUILD_SERIAL, "SERIAL 序列号", "SERIAL", "ro.serialno"),
        
        BuildField(KEY_BUILD_RELEASE, "VERSION.RELEASE 系统版本", "RELEASE", "ro.build.version.release", OWNER_VERSION),
        BuildField(KEY_BUILD_SECURITY_PATCH, "VERSION.SECURITY_PATCH 安全补丁", "SECURITY_PATCH", "ro.build.version.security_patch", OWNER_VERSION),
        BuildField(KEY_BUILD_INCREMENTAL, "VERSION.INCREMENTAL 增量版本", "INCREMENTAL", "ro.build.version.incremental", OWNER_VERSION),
        BuildField(KEY_BUILD_CODENAME, "VERSION.CODENAME 开发代号", "CODENAME", "ro.build.version.codename", OWNER_VERSION),
        BuildField(KEY_BUILD_BASE_OS, "VERSION.BASE_OS 基础系统", "BASE_OS", "ro.build.version.base_os", OWNER_VERSION),
    )

    const val OWNER_BUILD = "android.os.Build"
    const val OWNER_VERSION = "android.os.Build\$VERSION"

    data class BuildField(
        val key: String,
        val label: String,
        val field: String,
        val prop: String,
        val owner: String = OWNER_BUILD,
    )

    
    
    data class TextFieldSpec(val key: String, val label: String, val hint: String = "")

    val EXTRA_FIELDS_NET: List<TextFieldSpec> = listOf(
        TextFieldSpec(KEY_FAKE_WIFI_SSID, "WiFi 名称 SSID", "留空不改"),
        TextFieldSpec(KEY_FAKE_WIFI_BSSID, "WiFi BSSID（路由器 MAC）", "如 02:00:00:00:00:00"),
        TextFieldSpec(KEY_FAKE_WIFI_MAC, "WiFi MAC 地址", "如 02:11:22:33:44:55"),
        TextFieldSpec(KEY_FAKE_BT_MAC, "蓝牙 MAC 地址", "如 02:AA:BB:CC:DD:EE"),
        TextFieldSpec(KEY_FAKE_CARRIER, "网络运营商名称", "如 China Mobile"),
    )

    val EXTRA_FIELDS_SIM: List<TextFieldSpec> = listOf(
        TextFieldSpec(KEY_FAKE_SIM_OPERATOR, "SIM 运营商代码 MCC+MNC", "如 46000"),
        TextFieldSpec(KEY_FAKE_SIM_OPERATOR_NAME, "SIM 运营商名称", "如 CMCC"),
        TextFieldSpec(KEY_FAKE_SIM_COUNTRY, "SIM 国家码", "如 cn"),
        TextFieldSpec(KEY_FAKE_SIM_SERIAL, "ICCID（SIM 卡序列号）", "19~20 位数字"),
        TextFieldSpec(KEY_FAKE_SIM_SUBSCRIBER, "IMSI（订阅者 ID）", "15 位数字"),
        TextFieldSpec(KEY_FAKE_PHONE_NUMBER, "本机手机号", "防应用轻易读到手机号"),
        TextFieldSpec(KEY_FAKE_IMEI, "IMEI", "15 位数字"),
        TextFieldSpec(KEY_FAKE_MEID, "MEID", "14 位十六进制"),
    )

    val EXTRA_FIELDS_SYS: List<TextFieldSpec> = listOf(
        TextFieldSpec(KEY_FAKE_TIMEZONE, "时区", "如 Asia/Shanghai、America/New_York"),
        TextFieldSpec(KEY_FAKE_LOCALE, "系统语言 / 地区", "如 zh_CN、en_US、ja_JP"),
        TextFieldSpec(KEY_FAKE_SDK_INT, "Android SDK 版本", "如 33/34/35，0 表示不修改"),
        TextFieldSpec(KEY_FAKE_UPTIME_HOURS, "已运行时间（小时）", "如 72"),
        TextFieldSpec(KEY_FAKE_TIME_OFFSET, "时间偏移（分钟，可负）", "0 表示不偏移"),
    )

    val EXTRA_FIELDS_HW: List<TextFieldSpec> = listOf(
        TextFieldSpec(KEY_FAKE_KERNEL, "内核版本", "如 6.6.28-android15-8-g3f2a1b4c5d6-ab12345678"),
        TextFieldSpec(KEY_FAKE_ARCH, "内核架构", "如 aarch64 / x86_64"),
        TextFieldSpec(KEY_FAKE_GPU, "GPU 渲染器名", "如 Adreno (TM) 750"),
        TextFieldSpec(KEY_FAKE_GPU_VENDOR, "GPU 厂商", "留空按渲染器名推导"),
        TextFieldSpec(KEY_FAKE_GPU_GL_VERSION, "OpenGL ES 版本", "如 OpenGL ES 3.2 V@0502.0"),
        TextFieldSpec(KEY_FAKE_GPU_GLSL, "GLSL 版本", "如 OpenGL ES GLSL ES 3.20"),
        TextFieldSpec(KEY_FAKE_GPU_VK_API, "Vulkan API 版本", "如 1.1.0"),
        TextFieldSpec(KEY_FAKE_GPU_DRIVER, "驱动版本（十六进制）", "如 0x8020000"),
        TextFieldSpec(KEY_FAKE_GPU_VENDOR_ID, "厂商 ID（十六进制）", "如 0x13B5"),
        TextFieldSpec(KEY_FAKE_GPU_DEVICE_ID, "设备 ID（十六进制）", "如 0x72120000"),
        TextFieldSpec(KEY_FAKE_GPU_MEMORY_MB, "显存大小（MB）", "如 7469"),
        TextFieldSpec(KEY_FAKE_GPU_MAX_TEX, "最大图像尺寸 1D/2D/3D", "如 16384"),
        TextFieldSpec(KEY_FAKE_GPU_MAX_CUBE, "最大 Cube 图像尺寸", "如 16384"),
        TextFieldSpec(KEY_FAKE_GPU_MAX_LAYERS, "最大图像层数", "如 4096"),
        TextFieldSpec(KEY_FAKE_GPU_PUSH, "Max Push Constants Size", "如 256"),
        TextFieldSpec(KEY_FAKE_CPUINFO_HW, "/proc/cpuinfo Hardware", "留空=跟随 SOC 型号"),
        TextFieldSpec(KEY_FAKE_PLATFORM, "ro.board.platform", "留空=跟随 Hardware"),
        TextFieldSpec(KEY_FAKE_TEMP, "设备温度（摄氏度）", "0~120"),
        TextFieldSpec(KEY_FAKE_BATTERY, "电量百分比", "0~100"),
    )

    val EXTRA_FIELDS_ID: List<TextFieldSpec> = listOf(
        TextFieldSpec(KEY_OAID, "OAID（匿名设备标识符）"),
        TextFieldSpec(KEY_FAKE_HW_SERIAL, "Hardware Serial 硬件序列号"),
        TextFieldSpec(KEY_FAKE_FB_FID, "Firebase Installation ID"),
        TextFieldSpec(KEY_FAKE_FB_IID, "Firebase App Instance ID"),
    )

    const val KEY_ENABLE_JS = "enable_js"
    const val KEY_JS_CODE = "js_code"
    const val KEY_ENABLE_UA = "enable_ua"
    const val KEY_UA_VALUE = "ua_value"

    
    const val KEY_BLOCK_CRASH_ENABLE = "block_crash_enable"
    
    const val KEY_BLOCK_CRASH_LEVEL = "block_crash_level"

    
    const val KEY_CRASH_CATCH_ENABLE = "crash_catch_enable"
    const val KEY_CRASH_COPY_CLIPBOARD = "crash_copy_clipboard"
    const val KEY_CRASH_WRITE_FILE = "crash_write_file"
    const val KEY_CRASH_INTERCEPT = "crash_intercept"

    
    const val KEY_GUARD_ACTIVE = "guard_active_enable"
    const val KEY_GUARD_PASSIVE = "guard_passive_enable"

    
    const val KEY_EXIT_ENABLE = "exit_enable"
    const val KEY_EXIT_SECONDS = "exit_seconds"
    
    const val KEY_EXIT_METHODS = "exit_methods"
    
    const val KEY_EXIT_PARALLEL = "exit_parallel_all"

    
    const val KEY_ACC_ENABLE = "acc_block_enable"
    
    const val KEY_ACC_MODE = "acc_mode"
    
    const val KEY_ACC_SCOPE = "acc_scope"
    const val KEY_ACC_CAP_SCREEN = "acc_cap_screen"
    const val KEY_ACC_CAP_NOTIFY = "acc_cap_notify"
    const val KEY_ACC_CAP_WINDOW = "acc_cap_window"
    const val KEY_ACC_CAP_INPUT = "acc_cap_input"
    const val KEY_ACC_CAP_ACTION = "acc_cap_action"
    
    const val KEY_ACC_CAP_OVERLAY = "acc_cap_overlay"
    




    const val KEY_ACC_CAP_CONTROL = "acc_cap_control"

    
    const val KEY_BLOCK_OVERLAY = "block_overlay"
    const val KEY_BLOCK_WALLPAPER = "block_wallpaper"
    
    const val KEY_BLOCK_EXEC = "block_exec"
    
    const val EXIT_METHOD_EXEC = "exec"

    
    const val KEY_PANEL_INJECT = "panel_inject"

    
    const val KEY_VOLUME_ENABLE = "volume_enable"
    
    const val KEY_VOLUME_MASTER = "volume_master"
    
    const val KEY_VOLUME_LOCK = "volume_lock"
    const val KEY_VOLUME_LOCK_VALUE = "volume_lock_value"
    const val KEY_VOLUME_INCLUDE_SYSTEM = "volume_include_system"
    
    const val KEY_VOLUME_ALLOW_LOWER = "volume_allow_lower"
    const val KEY_VOLUME_BLOCK_RINGER = "volume_block_ringer"
    const val KEY_VOLUME_BLOCK_MUTE = "volume_block_mute"

    
    const val KEY_BLOCK_CONN_ENABLE = "block_conn_enable"
    const val KEY_BLOCK_CONN_WIFI = "block_conn_wifi"
    const val KEY_BLOCK_CONN_BT = "block_conn_bt"
    const val KEY_BLOCK_CONN_BRIGHT = "block_conn_bright"

    const val KEY_BLOCK_SENSOR = "block_sensor"

    
    const val KEY_BLOCK_JUMP_ENABLE = "block_jump_enable"
    
    const val KEY_BLOCK_JUMP_WHITELIST = "block_jump_whitelist"

    
    const val KEY_BLOCK_CAMERA_ENABLE = "block_camera_enable"
    const val KEY_BLOCK_MIC_ENABLE = "block_mic_enable"

    
    const val KEY_BLOCK_INSTALL_ENABLE = "block_install_enable"

    
    const val KEY_BLOCK_PRINT_CAST = "block_print_cast"

    
    const val KEY_BLOCK_NOTIFY = "block_notify"

    
    const val KEY_NET_FILTER_ENABLE = "net_filter_enable"
    const val KEY_NET_FILTER_WHITELIST = "net_filter_whitelist"
    const val KEY_NET_FILTER_LIST = "net_filter_list"

    
    const val KEY_BLOCK_SCREEN_CAPTURE = "block_screen_capture"
    const val KEY_SCREEN_CAPTURE_MODE = "screen_capture_mode"
    const val KEY_SCREEN_CAPTURE_GRANT_OK = "screen_capture_grant_ok"

    const val SC_MODE_IMAGE = 0   
    const val SC_MODE_BLANK = 1   
    const val SC_MODE_DENY = 2    

    
    const val KEY_BLOCK_TORCH = "block_torch"
    const val KEY_BLOCK_VIBRATE = "block_vibrate"

    
    const val KEY_FILE_GUARD_ENABLE = "file_guard_enable"
    const val KEY_FILE_OP_ALL = "file_op_all"
    const val KEY_FILE_OP_INSERT = "file_op_insert"
    const val KEY_FILE_OP_WRITE = "file_op_write"
    const val KEY_FILE_OP_READ = "file_op_read"
    const val KEY_FILE_OP_QUERY = "file_op_query"
    const val KEY_FILE_OP_UPDATE = "file_op_update"
    const val KEY_FILE_OP_DELETE = "file_op_delete"
    const val KEY_FILE_OP_JAVA = "file_op_java"
    const val KEY_FILE_OP_NIO = "file_op_nio"

    
    const val KEY_HIDE_APPS_ENABLE = "hide_apps_enable"
    
    const val KEY_HIDE_APPS_MODE = "hide_apps_mode"
    
    const val KEY_HIDE_APPS_LIST_MODE = "hide_apps_list_mode"
    const val KEY_HIDE_APPS_LIST = "hide_apps_list"

    
    const val KEY_AUDIO_OUT_ENABLE = "audio_out_enable"
    const val KEY_AUDIO_OUT_MEDIA = "audio_out_media"
    const val KEY_AUDIO_OUT_CALL = "audio_out_call"
    const val KEY_AUDIO_OUT_RING = "audio_out_ring"
    const val KEY_AUDIO_OUT_NOTIFY = "audio_out_notify"
    const val KEY_AUDIO_OUT_ALARM = "audio_out_alarm"
    const val KEY_AUDIO_OUT_SYSTEM = "audio_out_system"
    const val KEY_AUDIO_OUT_TTS = "audio_out_tts"
    
    const val KEY_AUDIO_OUT_FOCUS = "audio_out_focus"
    
    const val KEY_AUDIO_OUT_NO_FOCUS = "audio_out_no_focus"

    
    val AUDIO_OUT_ITEMS: List<Triple<String, String, String>> = listOf(
        Triple(
            KEY_AUDIO_OUT_MEDIA,
            "媒体输出",
            "音乐 / 视频 / 游戏音效：AudioTrack.play、MediaPlayer、SoundPool 走 USAGE_MEDIA / GAME / STREAM_MUSIC 的",
        ),
        Triple(
            KEY_AUDIO_OUT_CALL,
            "通话输出",
            "USAGE_VOICE_COMMUNICATION / STREAM_VOICE_CALL：语音通话、网络电话的声音",
        ),
        Triple(
            KEY_AUDIO_OUT_RING,
            "铃声",
            "USAGE_NOTIFICATION_RINGTONE / STREAM_RING：来电铃声",
        ),
        Triple(
            KEY_AUDIO_OUT_NOTIFY,
            "通知音",
            "USAGE_NOTIFICATION / STREAM_NOTIFICATION：消息提示音",
        ),
        Triple(
            KEY_AUDIO_OUT_ALARM,
            "闹钟",
            "USAGE_ALARM / STREAM_ALARM：闹钟与定时器",
        ),
        Triple(
            KEY_AUDIO_OUT_SYSTEM,
            "系统 / 触屏音",
            "USAGE_ASSISTANCE_SONIFICATION / USAGE_SYSTEM / STREAM_SYSTEM：按键音、触屏反馈音",
        ),
        Triple(
            KEY_AUDIO_OUT_TTS,
            "语音播报 / TTS",
            "USAGE_ASSISTANCE_ACCESSIBILITY / TextToSpeech.speak：朗读、无障碍播报、导航语音",
        ),
    )

    
    const val KEY_WDBG_ENABLE = "wdbg_enable"
    
    const val KEY_WDBG_TOGGLE = "wdbg_toggle"
    
    const val KEY_WDBG_PAIR = "wdbg_pair"
    
    const val KEY_WDBG_DISCOVER = "wdbg_discover"
    
    const val KEY_WDBG_PROP = "wdbg_prop"
    
    const val KEY_WDBG_BLOCK_JUMP = "wdbg_block_jump"
    
    const val KEY_WDBG_BLOCK_SETTINGS = "wdbg_block_settings"

    
    val WDBG_JUMP_TARGETS: List<Pair<String, String>> = listOf(
        "无线调试页面" to "adb_wireless|wireless_debugging|WirelessDebugging",
        "开发者选项" to "development_settings|DevelopmentSettings|APPLICATION_DEVELOPMENT",
        "USB 调试 / ADB 相关" to "adb|UsbDebugging|usb_debugging",
    )

    
    const val KEY_SHIZUKU_ENABLE = "shizuku_enable"
    
    const val KEY_SHIZUKU_BLOCK_AUTH = "shizuku_block_auth"
    
    const val KEY_SHIZUKU_BLOCK_USE = "shizuku_block_use"

    
    const val KEY_CLIP_ENABLE = "clip_enable"
    
    const val KEY_CLIP_MODE = "clip_mode"

    
    const val KEY_DA_ENABLE = "da_enable"
    
    const val KEY_DA_MASTER = "da_master"
    const val KEY_DA_LOCK = "da_lock"                 
    const val KEY_DA_PASSWORD = "da_password"         
    const val KEY_DA_WIPE = "da_wipe"                 
    const val KEY_DA_CAMERA = "da_camera"             
    const val KEY_DA_APPMGMT = "da_appmgmt"           
    const val KEY_DA_SYSTEM = "da_system"             
    const val KEY_DA_PERMISSION = "da_permission"     
    const val KEY_DA_ENCRYPT = "da_encrypt"           

    
    val DA_ITEMS: List<Triple<String, String, String>> = listOf(
        Triple(
            KEY_DA_LOCK,
            "强制锁屏相关",
            "禁止定时或立刻锁定屏幕：lockNow / setMaximumTimeToLock",
        ),
        Triple(
            KEY_DA_PASSWORD,
            "密码相关",
            "禁止强制密码长度 / 复杂度 / 超时 / 历史，禁止改锁屏密码、记录失败次数与密码到期回调",
        ),
        Triple(
            KEY_DA_WIPE,
            "恢复出厂设置",
            "禁止擦除手机全部数据：wipeData，以及策略不满足时自动锁定 / 擦除",
        ),
        Triple(
            KEY_DA_CAMERA,
            "禁用相机",
            "禁止通过策略关闭拍照：setCameraDisabled",
        ),
        Triple(
            KEY_DA_APPMGMT,
            "应用管理",
            "禁止静默安装 / 卸载 / 更新应用、清除应用数据、设置应用黑白名单",
        ),
        Triple(
            KEY_DA_SYSTEM,
            "系统控制",
            "禁止配置全局 Wi-Fi / VPN、Kiosk 展台模式、禁用 USB / 蓝牙 / NFC、录屏与用户限制",
        ),
        Triple(
            KEY_DA_PERMISSION,
            "权限管控",
            "禁止自动授予或拒绝其它应用的运行时权限：setPermissionPolicy / setPermissionGrantState",
        ),
        Triple(
            KEY_DA_ENCRYPT,
            "存储加密",
            "禁止请求存储加密：setStorageEncryption",
        ),
    )

    
    const val KEY_PERM_ENABLE = "perm_fake_enable"

    
    const val KEY_LOG_ENABLE = "log_enable"
    
    const val KEY_PERM_GRANT = "perm_grant_set"
    
    const val KEY_PERM_FAKE_DATA = "perm_fake_data_set"
    



    const val KEY_PERM_DEFAULT_ALL = "perm_default_all"

    
    const val DEF_EXIT_SECONDS = 25
    const val DEF_EXIT_METHODS = "kill,exit,halt,signal"

    



    val HIDE_APPS_PRESET: String = listOf(
        
        "com.android.systemui",
        "com.android.settings",
        "com.android.launcher3",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.dialer",
        "com.android.contacts",
        "com.android.mms",
        "com.android.providers.settings",
        "com.android.providers.media",
        "com.android.providers.downloads",
        "com.android.providers.contacts",
        "com.android.providers.telephony",
        "com.android.providers.calendar",
        "com.android.calendar",
        "com.android.deskclock",
        "com.android.camera2",
        "com.android.camera",
        "com.android.gallery3d",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.wifi",
        "com.android.inputmethod.latin",
        "com.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.android.vending",
        "com.android.browser",
        "android",
        
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.apps.photos",
        "com.google.android.apps.maps",
        "com.google.android.apps.messaging",
        "com.google.android.gm",
        "com.google.android.youtube",
        "com.google.android.inputmethod.latin",
        "com.google.android.dialer",
        "com.google.android.contacts",
        "com.google.android.calendar",
        "com.google.android.apps.docs",
        
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.tencent.wework",
        "com.tencent.qqlive",
        "com.tencent.mtt",
        "com.qq.qcloud",
        "com.alibaba.android.rimet",
        "com.taobao.taobao",
        "com.tmall.wireless",
        "com.jingdong.app.mall",
        "com.xunmeng.pinduoduo",
        "com.ss.android.ugc.aweme",
        "com.ss.android.article.news",
        "com.sina.weibo",
        "com.baidu.searchbox",
        "com.baidu.BaiduMap",
        "com.baidu.tieba",
        "com.netease.cloudmusic",
        "com.kugou.android",
        "com.tencent.news",
        "com.meituan.android",
        "com.dianping.v1",
        "com.ctrip.android.view",
        "com.qunar.im",
        "com.autonavi.minimap",
        "com.eg.android.AlipayGphone",
        "com.unionpay",
        "com.icbc",
        "com.android.chrome",
        "com.uc.browser",
        "com.UCMobile",
        "com.quark.browser",
        "com.bilibili.app.in",
        "com.zhihu.android",
        "com.xiaomi.market",
        "com.huawei.appmarket",
        "com.oppo.market",
        "com.vivo.appstore",
        "com.coloros.appmarket",
        "com.heytap.market",
        "com.bbk.appstore",
        "com.xiaomi.xmsf",
        "com.miui.home",
        "com.miui.securitycenter",
        "com.android.updater",
    ).joinToString("\n")

    



    val DEFAULTS: Map<String, Any?> = mapOf(
        KEY_LOG_ENABLE to false,
        KEY_ENABLE_ANDROID_ID to false,
        KEY_ENABLE_BUILD to true,
        KEY_BUILD_MODEL to "",
        KEY_BUILD_BRAND to "",
        KEY_BUILD_MANUFACTURER to "",
        KEY_BUILD_FINGERPRINT to "",
        KEY_BUILD_SERIAL to "",
        KEY_BUILD_HARDWARE to "",
        KEY_BUILD_BOARD to "",
        KEY_BUILD_DEVICE to "",
        KEY_BUILD_SOC_MODEL to "",
        KEY_BUILD_PRODUCT to "",
        KEY_BUILD_BOOTLOADER to "",
        KEY_BUILD_DISPLAY to "",
        KEY_BUILD_RADIO to "",
        KEY_BUILD_HOST to "",
        KEY_BUILD_TAGS to "",
        KEY_BUILD_TYPE to "",
        KEY_BUILD_ID to "",
        KEY_BUILD_USER to "",
        KEY_BUILD_RELEASE to "",
        KEY_BUILD_SECURITY_PATCH to "",
        KEY_BUILD_INCREMENTAL to "",
        KEY_BUILD_CODENAME to "",
        KEY_BUILD_BASE_OS to "",
        KEY_DEVICE_NAME to "",
        KEY_GSF_ID to "",
        KEY_ADS_ID to "",
        KEY_APPSET_ID to "",
        KEY_DRM_ID to "",
        KEY_HIDE_ACCOUNTS to false,
        
        KEY_WIFI_FAKE_ENABLE to false,
        KEY_WIFI_FAKE_SSID to "",
        KEY_WIFI_FAKE_BSSID to "",
        KEY_WIFI_FAKE_RSSI to -55,
        KEY_WIFI_FAKE_SPEED to 300,
        KEY_WIFI_FAKE_IP to "192.168.1.88",
        KEY_WIFI_FAKE_FREQ to 5180,
        KEY_WIFI_FAKE_SCAN to true,
        KEY_WIFI_FAKE_LIST to "",
        KEY_WIFI_FAKE_NETWORK to true,
        
        KEY_ROOT_FAKE_ENABLE to false,
        KEY_ROOT_FAKE_FILE to true,
        KEY_ROOT_FAKE_MASK to true,
        
        KEY_ACC_STATUS_SPOOF to true,
        KEY_ACC_STATUS_VALUE to false,
        
        KEY_VPN_HIDE_ENABLE to false,
        KEY_VPN_HIDE_IFACE to true,
        KEY_VPN_HIDE_CAPS to true,
        KEY_VPN_HIDE_NETINFO to true,
        KEY_VPN_HIDE_PROXY to true,
        KEY_VPN_HIDE_SETTINGS to true,
        KEY_VPN_IFACES to DEF_VPN_IFACES,
        
        KEY_FAKE_WIFI_SSID to "",
        KEY_FAKE_WIFI_BSSID to "",
        KEY_FAKE_DEV_OFF to false,
        KEY_FAKE_SIM_COUNTRY to "",
        KEY_FAKE_SIM_OPERATOR to "",
        KEY_FAKE_SIM_OPERATOR_NAME to "",
        KEY_FAKE_SIM_SERIAL to "",
        KEY_FAKE_SIM_SUBSCRIBER to "",
        KEY_FAKE_PHONE_NUMBER to "",
        KEY_FAKE_TIMEZONE to "",
        KEY_FAKE_LOCALE to "",
        KEY_FAKE_TIME_ENABLE to false,
        KEY_FAKE_TIME_OFFSET to 0,
        KEY_FAKE_UPTIME_ENABLE to false,
        KEY_FAKE_UPTIME_HOURS to "72",
        KEY_FAKE_SDK_INT to 0,
        KEY_FAKE_WIFI_MAC to "",
        KEY_FAKE_BT_MAC to "",
        KEY_FAKE_IMEI to "",
        KEY_FAKE_MEID to "",
        KEY_FAKE_ICCID to "",
        KEY_FAKE_CARRIER to "",
        KEY_FAKE_FB_FID to "",
        KEY_FAKE_FB_IID to "",
        KEY_FAKE_HW_SERIAL to "",
        KEY_FAKE_KERNEL to "",
        KEY_FAKE_ARCH to "",
        KEY_FAKE_CPUINFO_HW to "",
        KEY_FAKE_PLATFORM to "",
        KEY_FAKE_GPU to "",
        KEY_FAKE_GPU_VENDOR to "",
        KEY_FAKE_GPU_GL_VERSION to "",
        KEY_FAKE_GPU_GLSL to "",
        KEY_FAKE_GPU_VK_API to "",
        KEY_FAKE_GPU_DRIVER to "",
        KEY_FAKE_GPU_VENDOR_ID to "",
        KEY_FAKE_GPU_DEVICE_ID to "",
        KEY_FAKE_GPU_MEMORY_MB to DEF_GPU_MEMORY_MB,
        KEY_FAKE_GPU_MAX_TEX to DEF_GPU_MAX_TEX,
        KEY_FAKE_GPU_MAX_CUBE to DEF_GPU_MAX_TEX,
        KEY_FAKE_GPU_MAX_LAYERS to DEF_GPU_MAX_LAYERS,
        KEY_FAKE_GPU_PUSH to DEF_GPU_PUSH,
        KEY_FAKE_CPU_ENABLE to false,
        KEY_FAKE_CPU_MODE to CPU_MODE_PRESET,
        KEY_FAKE_CPU_PRESET to 0,
        KEY_FAKE_CPU_CUSTOM to "",
        KEY_FAKE_CPU_CORES to 8,
        KEY_FAKE_TEMP_ENABLE to false,
        KEY_FAKE_TEMP to DEF_TEMP,
        KEY_FAKE_BATTERY_ENABLE to false,
        KEY_FAKE_BATTERY to DEF_BATTERY,
        KEY_OAID to "",
        KEY_CUSTOM_PROPS to "",
        KEY_HIDE_PATHS to "",
        KEY_NATIVE_HOOK to DEF_NATIVE_HOOK,
        KEY_NATIVE_BLOCK_EXIT to DEF_NATIVE_BLOCK_EXIT,
        KEY_NATIVE_ANTI_DETECT to DEF_NATIVE_ANTI_DETECT,
        KEY_ENABLE_JS to false,
        KEY_ENABLE_UA to false,
        KEY_BLOCK_CRASH_ENABLE to true,
        KEY_BLOCK_CRASH_LEVEL to 1,
        KEY_CRASH_CATCH_ENABLE to true,
        KEY_CRASH_COPY_CLIPBOARD to false,
        KEY_CRASH_WRITE_FILE to true,
        KEY_CRASH_INTERCEPT to false,
        KEY_GUARD_ACTIVE to false,
        KEY_GUARD_PASSIVE to false,
        KEY_EXIT_ENABLE to false,
        KEY_EXIT_SECONDS to DEF_EXIT_SECONDS,
        KEY_EXIT_METHODS to DEF_EXIT_METHODS,
        KEY_EXIT_PARALLEL to false,
        KEY_ACC_ENABLE to false,
        KEY_ACC_MODE to 0,
        KEY_ACC_SCOPE to 1,
        KEY_ACC_CAP_SCREEN to true,
        KEY_ACC_CAP_NOTIFY to true,
        KEY_ACC_CAP_WINDOW to true,
        KEY_ACC_CAP_INPUT to true,
        KEY_ACC_CAP_ACTION to true,
        KEY_ACC_CAP_OVERLAY to true,
        KEY_ACC_CAP_CONTROL to true,
        KEY_BLOCK_OVERLAY to false,
        KEY_BLOCK_WALLPAPER to false,
        KEY_BLOCK_EXEC to false,
        KEY_VOLUME_ENABLE to false,
        KEY_VOLUME_MASTER to false,
        KEY_VOLUME_LOCK to false,
        KEY_VOLUME_LOCK_VALUE to 50,
        KEY_AUDIO_OUT_ENABLE to false,
        KEY_AUDIO_OUT_MEDIA to false,
        KEY_AUDIO_OUT_CALL to false,
        KEY_AUDIO_OUT_RING to false,
        KEY_AUDIO_OUT_NOTIFY to false,
        KEY_AUDIO_OUT_ALARM to false,
        KEY_AUDIO_OUT_SYSTEM to false,
        KEY_AUDIO_OUT_TTS to false,
        KEY_AUDIO_OUT_FOCUS to true,
        KEY_AUDIO_OUT_NO_FOCUS to true,
        KEY_VOLUME_INCLUDE_SYSTEM to true,
        KEY_VOLUME_ALLOW_LOWER to false,
        KEY_VOLUME_BLOCK_RINGER to true,
        KEY_VOLUME_BLOCK_MUTE to true,
        KEY_CLIP_ENABLE to false,
        KEY_CLIP_MODE to 2,
        KEY_WDBG_ENABLE to false,
        KEY_WDBG_TOGGLE to true,
        KEY_WDBG_PAIR to true,
        KEY_WDBG_DISCOVER to true,
        KEY_WDBG_PROP to true,
        KEY_WDBG_BLOCK_JUMP to true,
        KEY_WDBG_BLOCK_SETTINGS to false,
        KEY_SHIZUKU_ENABLE to false,
        KEY_SHIZUKU_BLOCK_AUTH to true,
        KEY_SHIZUKU_BLOCK_USE to true,
        KEY_BLOCK_CONN_ENABLE to false,
        KEY_BLOCK_CONN_WIFI to true,
        KEY_BLOCK_CONN_BT to true,
        KEY_BLOCK_CONN_BRIGHT to true,
        KEY_BLOCK_SENSOR to false,
        KEY_BLOCK_JUMP_ENABLE to false,
        KEY_BLOCK_JUMP_WHITELIST to "",
        KEY_BLOCK_CAMERA_ENABLE to false,
        KEY_BLOCK_MIC_ENABLE to false,
        KEY_BLOCK_INSTALL_ENABLE to false,
        KEY_BLOCK_PRINT_CAST to false,
        KEY_BLOCK_NOTIFY to false,
        KEY_NET_FILTER_ENABLE to false,
        KEY_NET_FILTER_WHITELIST to false,
        KEY_NET_FILTER_LIST to "",
        KEY_BLOCK_SCREEN_CAPTURE to false,
        KEY_SCREEN_CAPTURE_MODE to SC_MODE_BLANK,
        KEY_SCREEN_CAPTURE_GRANT_OK to false,
        KEY_BLOCK_TORCH to false,
        KEY_BLOCK_VIBRATE to false,
        KEY_FILE_GUARD_ENABLE to false,
        KEY_FILE_OP_ALL to false,
        KEY_FILE_OP_INSERT to true,
        KEY_FILE_OP_WRITE to true,
        KEY_FILE_OP_READ to false,
        KEY_FILE_OP_QUERY to false,
        KEY_FILE_OP_UPDATE to true,
        KEY_FILE_OP_DELETE to true,
        KEY_FILE_OP_JAVA to true,
        KEY_FILE_OP_NIO to true,
        KEY_HIDE_APPS_ENABLE to false,
        KEY_HIDE_APPS_MODE to 1,
        KEY_HIDE_APPS_LIST_MODE to 0,
        KEY_HIDE_APPS_LIST to HIDE_APPS_PRESET,
        KEY_PANEL_INJECT to true,
        KEY_DA_ENABLE to false,
        KEY_DA_MASTER to false,
        KEY_DA_LOCK to true,
        KEY_DA_PASSWORD to true,
        KEY_DA_WIPE to true,
        KEY_DA_CAMERA to true,
        KEY_DA_APPMGMT to true,
        KEY_DA_SYSTEM to true,
        KEY_DA_PERMISSION to true,
        KEY_DA_ENCRYPT to true,
        KEY_PERM_ENABLE to false,
        KEY_PERM_GRANT to "",
        KEY_PERM_FAKE_DATA to "",
        KEY_PERM_DEFAULT_ALL to false,
    )

    
    const val CRASH_LOG_NAME = "error.log"

    
    const val CONSOLE_LOG_NAME = "xp_console.log"

    
    val CLIP_MODE_OPTIONS: List<String> = listOf(
        "只禁止读",
        "只禁止写",
        "全部禁止",
    )

    



    val EXIT_METHODS: List<Pair<String, String>> = listOf(
        "kill" to "杀死进程",
        "exit" to "结束虚拟机",
        "halt" to "强制停止",
        "signal" to "SIGKILL 信号",
        "finish" to "结束所有界面",
        "npe" to "空指针闪退",
        "exec" to "执行命令",
    )

    
    val EXIT_METHOD_DETAIL: Map<String, String> = mapOf(
        "kill" to "Process.killProcess",
        "exit" to "System.exit",
        "halt" to "Runtime.halt",
        "signal" to "Process.sendSignal / Os.kill(SIGKILL)",
        "finish" to "finishAffinity / finishAndRemoveTask",
        "npe" to "制造 NullPointerException",
        "exec" to "逐个 kill -9 本应用各进程 PID",
    )

    val EXIT_METHOD_KEYS: List<String> = EXIT_METHODS.map { it.first }

    


    val ACC_SCOPE_OPTIONS: List<String> = listOf(
        "只关闭服务",
        "关闭 + 全部钩子",
        "只运行钩子",
    )

    

    
    const val PERM_CONTACTS = "CONTACTS"
    const val PERM_SMS = "SMS"
    const val PERM_CALL_LOG = "CALL_LOG"
    const val PERM_PHONE = "PHONE"
    const val PERM_STORAGE = "STORAGE"
    const val PERM_LOCATION = "LOCATION"
    const val PERM_CALENDAR = "CALENDAR"
    const val PERM_CAMERA = "CAMERA"
    const val PERM_MICROPHONE = "MICROPHONE"
    const val PERM_SENSORS = "SENSORS"
    const val PERM_NOTIFICATION = "NOTIFICATION"
    
    const val PERM_ACCESSIBILITY = "SPECIAL_ACCESSIBILITY"
    const val PERM_DEVICE_ADMIN = "SPECIAL_DEVICE_ADMIN"
    const val PERM_NOTIFICATION_LISTENER = "SPECIAL_NOTI_LISTENER"
    const val PERM_USAGE_STATS = "SPECIAL_USAGE_STATS"
    const val PERM_DRAW_OVERLAY = "SPECIAL_DRAW_OVERLAY"
    const val PERM_INSTALL_UNKNOWN = "SPECIAL_INSTALL_UNKNOWN"
    const val PERM_MANAGE_STORAGE = "SPECIAL_MANAGE_STORAGE"
    const val PERM_EXACT_ALARM = "SPECIAL_EXACT_ALARM"
    const val PERM_BATTERY_OPT = "SPECIAL_BATTERY_OPT"
    const val PERM_AUTOSTART = "SPECIAL_AUTOSTART"
    const val PERM_VPN = "SPECIAL_VPN"
    const val PERM_WRITE_SETTINGS = "SPECIAL_WRITE_SETTINGS"
    const val PERM_BACKGROUND_POPUP = "SPECIAL_BACKGROUND_POPUP"

    




    data class PermGroup(
        val id: String,
        val label: String,
        val perms: Set<String>,
        val fakeData: Boolean = false,
        val fakeHint: String = "",
        
        val special: Boolean = false,
    )

    val PERM_GROUPS: List<PermGroup> = listOf(
        PermGroup(
            id = PERM_CONTACTS,
            label = "通讯录",
            perms = setOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.GET_ACCOUNTS,
            ),
            fakeData = true,
            fakeHint = "通讯录会返回几个不存在的假联系人（姓名 / 号码均为虚构）",
        ),
        PermGroup(
            id = PERM_SMS,
            label = "短信",
            perms = setOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                "android.permission.READ_CELL_BROADCASTS",
            ),
            fakeData = true,
            fakeHint = "短信会返回几条虚构的会话记录",
        ),
        PermGroup(
            id = PERM_CALL_LOG,
            label = "通话记录",
            perms = setOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                "android.permission.PROCESS_OUTGOING_CALLS",
            ),
            fakeData = true,
            fakeHint = "通话记录会返回几条虚构的通话条目",
        ),
        PermGroup(
            id = PERM_PHONE,
            label = "电话",
            perms = setOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ANSWER_PHONE_CALLS,
            ),
            fakeData = true,
            fakeHint = "本机号码 / IMEI / 订阅 ID 等会返回伪造的固定值",
        ),
        PermGroup(
            id = PERM_STORAGE,
            label = "存储空间",
            perms = setOf(
                
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",                
                "android.permission.ACCESS_MEDIA_LOCATION",
            ),
            fakeData = true,
            fakeHint = "存储空间会返回几个伪造的目录 / 文件（实际并不存在）",
        ),
        PermGroup(
            id = PERM_LOCATION,
            label = "位置信息",
            perms = setOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ),
            fakeData = true,
            fakeHint = "最后一次已知位置会返回一个固定坐标",
        ),
        PermGroup(
            id = PERM_CALENDAR,
            label = "日历",
            perms = setOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
            ),
            fakeData = true,
            fakeHint = "日历会返回一条虚构的日程",
        ),
        PermGroup(
            id = PERM_CAMERA,
            label = "相机",
            perms = setOf(Manifest.permission.CAMERA),
            fakeData = false,
        ),
        PermGroup(
            id = PERM_MICROPHONE,
            label = "麦克风",
            perms = setOf(Manifest.permission.RECORD_AUDIO),
            fakeData = false,
        ),
        PermGroup(
            id = PERM_SENSORS,
            label = "身体传感器",
            perms = setOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.BODY_SENSORS_BACKGROUND,
            ),
            fakeData = false,
        ),
        PermGroup(
            id = PERM_NOTIFICATION,
            label = "通知",
            perms = setOf(Manifest.permission.POST_NOTIFICATIONS),
            fakeData = false,
        ),

        
        PermGroup(
            id = PERM_ACCESSIBILITY,
            label = "无障碍服务",
            perms = setOf("android.permission.BIND_ACCESSIBILITY_SERVICE"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_DEVICE_ADMIN,
            label = "设备管理员",
            perms = setOf("android.permission.BIND_DEVICE_ADMIN"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_NOTIFICATION_LISTENER,
            label = "通知读取（通知监听）",
            perms = setOf("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_USAGE_STATS,
            label = "使用情况访问",
            perms = setOf("android.permission.PACKAGE_USAGE_STATS"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_DRAW_OVERLAY,
            label = "悬浮窗（显示在其他应用上层）",
            perms = setOf("android.permission.SYSTEM_ALERT_WINDOW"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_INSTALL_UNKNOWN,
            label = "安装未知应用",
            perms = setOf("android.permission.REQUEST_INSTALL_PACKAGES"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_MANAGE_STORAGE,
            label = "所有文件访问权限",
            perms = setOf("android.permission.MANAGE_EXTERNAL_STORAGE"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_EXACT_ALARM,
            label = "精确闹钟",
            perms = setOf("android.permission.SCHEDULE_EXACT_ALARM"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_BATTERY_OPT,
            label = "忽略电池优化",
            perms = setOf("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_AUTOSTART,
            label = "自启动 / 关联启动",
            perms = setOf("android.permission.RECEIVE_BOOT_COMPLETED"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_VPN,
            label = "VPN",
            perms = setOf("android.permission.BIND_VPN_SERVICE"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_WRITE_SETTINGS,
            label = "修改系统设置",
            perms = setOf("android.permission.WRITE_SETTINGS"),
            fakeData = false,
            special = true,
        ),
        PermGroup(
            id = PERM_BACKGROUND_POPUP,
            label = "后台弹出界面",
            perms = emptySet(),
            fakeData = false,
            special = true,
        ),
    )

    


    val FILE_OP_ITEMS: List<Triple<String, String, String>> = listOf(
        Triple(
            KEY_FILE_OP_INSERT,
            "禁用创建文件记录（ContentResolver.insert）",
            " ",
        ),
        Triple(
            KEY_FILE_OP_WRITE,
            "禁用写入文件内容（openOutputStream 等）",
            "openOutputStream / openAssetFileDescriptor / openFileDescriptor / openFile，" +
                    " ",
        ),
        Triple(
            KEY_FILE_OP_READ,
            "禁用读取文件内容（openInputStream）",
            "读取自己或别处的文件内容；默认关闭",
        ),
        Triple(
            KEY_FILE_OP_QUERY,
            "禁用查询文件（ContentResolver.query）",
            "查询文件列表与元数据，通常只能查到 OWNER_PACKAGE_NAME 是自己的记录",
        ),
        Triple(
            KEY_FILE_OP_UPDATE,
            "禁用更新 / 发布文件（update）",
            "改元数据，或把 IS_PENDING 置 0 完成发布；关掉它文件会一直卡在未完成状态",
        ),
        Triple(
            KEY_FILE_OP_DELETE,
            "禁用删除文件（ContentResolver.delete）",
            "删除自己创建的文件",
        ),
        Triple(
            KEY_FILE_OP_JAVA,
            "禁用 java.io.File（创建 / 目录 / 删除 / 改名）",
            "不走 MediaStore 的老式写法：直接建文件、建目录、改名、删除",
        ),
        Triple(
            KEY_FILE_OP_NIO,
            "禁用 java.nio.file.Files（创建 / 写 / 移动 / 删）",
            "NIO 写法：Files.createFile / createDirectory / write / copy / move / delete",
        ),
    )

    
    val ALL_GROUP_IDS: Set<String> = PERM_GROUPS.map { it.id }.toSet()

    
    val ALL_FAKE_GROUP_IDS: Set<String> = PERM_GROUPS.filter { it.fakeData }.map { it.id }.toSet()

    

    
    const val PREFIX_APP_GRANT = "appcfg_grant_"
    const val PREFIX_APP_FAKE = "appcfg_fake_"
    
    const val LEGACY_GRANT_READ = "perm_grant_"
    const val LEGACY_FAKE_READ = "perm_fake_"
    private val LEGACY_EXCLUDE = setOf(KEY_PERM_GRANT, KEY_PERM_FAKE_DATA)

    
    fun keyPermGrantFor(pkg: String): String = PREFIX_APP_GRANT + pkg

    
    fun keyPermFakeFor(pkg: String): String = PREFIX_APP_FAKE + pkg

    
    fun isAppGrantKey(key: String): Boolean =
        (key.startsWith(PREFIX_APP_GRANT) ||
                (key.startsWith(LEGACY_GRANT_READ) && key !in LEGACY_EXCLUDE))

    
    fun pkgOfKey(key: String): String = when {
        key.startsWith(PREFIX_APP_GRANT) -> key.removePrefix(PREFIX_APP_GRANT)
        key.startsWith(PREFIX_APP_FAKE) -> key.removePrefix(PREFIX_APP_FAKE)
        key.startsWith(LEGACY_GRANT_READ) -> key.removePrefix(LEGACY_GRANT_READ)
        key.startsWith(LEGACY_FAKE_READ) -> key.removePrefix(LEGACY_FAKE_READ)
        else -> key
    }

    
    private val PERM_INDEX: Map<String, PermGroup> =
        PERM_GROUPS.flatMap { g -> g.perms.map { p -> p to g } }.toMap()

    
    fun groupOf(permission: String?): PermGroup? = permission?.let { PERM_INDEX[it] }

    
    fun groupById(id: String): PermGroup? = PERM_GROUPS.firstOrNull { it.id == id }

    
    fun decodeLines(raw: String?): Set<String> =
        raw?.split("\n", "\r\n", "\r")?.flatMap { it.split(",", ";", " ") }
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

    
    fun encodeSet(set: Set<String>): String = set.joinToString(",")

    
    fun decodeSet(raw: String?): Set<String> =
        raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

    
    
    
    fun encodeApList(list: List<FakeAp>): String =
        list.joinToString("\n") { "${it.ssid}|${it.bssid}|${it.level}|${it.caps}" }

    fun decodeApList(raw: String?): List<FakeAp> =
        raw?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { line ->
                val p = line.split("|")
                val ssid = p.getOrNull(0)?.trim().orEmpty()
                if (ssid.isEmpty()) return@mapNotNull null
                FakeAp(
                    ssid = ssid,
                    bssid = p.getOrNull(1)?.trim().orEmpty(),
                    level = p.getOrNull(2)?.trim()?.toIntOrNull() ?: -55,
                    caps = p.getOrNull(3)?.trim().orEmpty()
                        .ifEmpty { "[WPA2-PSK-CCMP][ESS]" },
                )
            } ?: emptyList()

    
    
    
    fun encodeProps(list: List<Pair<String, String>>): String =
        list.filter { it.first.trim().isNotEmpty() }
            .joinToString("\n") { (k, v) -> "${k.trim()}=${if (v.isBlank()) "null" else v}" }

    fun decodeProps(raw: String?): List<Pair<String, String>> =
        raw?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { line ->
                val i = line.indexOf('=')
                if (i <= 0) return@mapNotNull null
                val k = line.substring(0, i).trim()
                if (k.isEmpty()) return@mapNotNull null
                var v = line.substring(i + 1)
                if (v.equals("null", ignoreCase = true)) v = ""
                k to v
            } ?: emptyList()

    
    fun decodePathLines(raw: String?): List<String> =
        raw?.split("\n", "\r\n", "\r")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct() ?: emptyList()

    fun defaultApList(): String = encodeApList(
        listOf(
            FakeAp("HOME-WIFI-5G", "02:1a:2b:3c:4d:5e", -42, "[WPA2-PSK-CCMP][ESS]"),
            FakeAp("HOME-WIFI", "02:1a:2b:3c:4d:5f", -51, "[WPA2-PSK-CCMP][ESS]"),
            FakeAp("TP-LINK_8890", "a4:2b:8c:11:22:33", -67, "[WPA-PSK-CCMP][ESS]"),
            FakeAp("CMCC-EDU", "0a:00:27:00:00:05", -73, "[ESS]"),
            FakeAp("Starbucks Wi-Fi", "06:5c:11:9a:bb:cc", -80, "[ESS]"),
        )
    )
}


data class FakeAp(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val caps: String,
)