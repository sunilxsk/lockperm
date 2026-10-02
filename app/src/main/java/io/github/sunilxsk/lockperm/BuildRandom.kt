package io.github.sunilxsk.lockperm

import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.abs








internal object BuildRandom {

    private val SECURE = SecureRandom()
    private val RANDOM = java.util.Random()

    






    private data class Profile(
        val brand: String,
        val model: String,
        val device: String,
        val product: String,
        val board: String,
        val hardware: String,   
        val platform: String,   
        val soc: String,        
        val gpu: String,
        val market: String,     
    )

    private val PROFILES = listOf(
        Profile("Samsung", "SM-S928B", "e3q", "e3qxx", "sun", "qcom", "kalama", "SM8650", "Adreno (TM) 750", "Galaxy S24 Ultra"),
        Profile("Samsung", "SM-S921B", "e1q", "e1qxx", "sun", "qcom", "kalama", "SM8650", "Adreno (TM) 750", "Galaxy S24"),
        Profile("Google", "Pixel 9 Pro", "caiman", "caiman", "zuma", "zuma", "zuma", "Tensor G4", "Mali-G715-Immortalis MC7", "Pixel 9 Pro"),
        Profile("Google", "Pixel 8 Pro", "husky", "husky", "zuma", "zuma", "zuma", "Tensor G3", "Mali-G715-Immortalis MC7", "Pixel 8 Pro"),
        Profile("Xiaomi", "24031PN0DC", "aurora", "aurora", "aurora", "qcom", "kalama", "SM8650", "Adreno (TM) 750", "Xiaomi 14 Ultra"),
        Profile("Xiaomi", "2211133C", "fuxi", "fuxi", "taro", "qcom", "taro", "SM8550", "Adreno (TM) 740", "Xiaomi 13"),
        Profile("OnePlus", "CPH2585", "OP5A3BL1", "OP5A3BL1", "kalama", "qcom", "kalama", "SM8650", "Adreno (TM) 750", "OnePlus 12"),
        Profile("vivo", "V2318DA", "PD2318", "PD2318", "mt6989", "mt6989", "mt6989", "MT6989", "Mali-G720 Immortalis MP12", "vivo X100 Pro"),
        Profile("OPPO", "CPH2557", "OP5A71L1", "OP5A71L1", "mt6989", "mt6989", "mt6989", "MT6989", "Mali-G720 Immortalis MP12", "OPPO Find X7"),
        Profile("Realme", "RMX3771", "RE58CB1", "RE58CB1", "kalama", "qcom", "kalama", "SM8650", "Adreno (TM) 750", "Realme GT5 Pro"),
        Profile("HUAWEI", "ALN-AL00", "ALN", "ALN", "ALN", "kirin", "kirin9000s", "Kirin 9000s", "Maleoon 910", "HUAWEI Mate 60 Pro"),
        Profile("honor", "MAA-AN00", "MAA", "MAA", "kalama", "qcom", "kalama", "SM8650", "Adreno (TM) 750", "honor Magic6 Pro"),
        Profile("Motorola", "XT2321-2", "eqc", "eqc", "kalama", "qcom", "kalama", "SM8650", "Adreno (TM) 750", "Moto Edge 50 Ultra"),
    )

    private val BRANDS = arrayOf(
        "Samsung", "Google", "Xiaomi", "OnePlus", "Motorola",
        "Nothing", "Realme", "HUAWEI", "vivo", "OPPO", "honor", "ASUS",
    )

    private val MODELS = mapOf(
        "Samsung" to arrayOf(
            "Galaxy S24 Ultra", "Galaxy S24", "Galaxy S23 Ultra", "Galaxy S23",
            "Galaxy Z Fold 6", "Galaxy Z Flip 6", "Galaxy A55", "Galaxy A35",
        ),
        "Google" to arrayOf("Pixel 9 Pro XL", "Pixel 9 Pro", "Pixel 9", "Pixel 8a", "Pixel 8 Pro", "Pixel 7a"),
        "Xiaomi" to arrayOf("Xiaomi 14 Ultra", "Xiaomi 14 Pro", "Xiaomi 14", "Redmi Note 13 Pro+", "Redmi K70"),
        "OnePlus" to arrayOf("OnePlus 12", "OnePlus 12R", "OnePlus Nord 4", "OnePlus Ace 3"),
        "Motorola" to arrayOf("Moto G84", "Moto Edge 50 Ultra", "Moto X50 Ultra", "Moto G Stylus 5G"),
        "Nothing" to arrayOf("Nothing Phone 2", "Nothing Phone 2a", "Nothing Phone 3"),
        "Realme" to arrayOf("Realme GT 6", "Realme GT Neo 6", "Realme 12 Pro+"),
        "HUAWEI" to arrayOf("HUAWEI Mate 60 Pro", "HUAWEI P60 Pro", "HUAWEI nova 12"),
        "vivo" to arrayOf("vivo X100 Pro", "vivo X100", "vivo V30 Pro"),
        "OPPO" to arrayOf("OPPO Find X7 Ultra", "OPPO Find N3", "OPPO Reno 11 Pro"),
        "honor" to arrayOf("honor Magic6 Pro", "honor Magic V2", "honor 200 Pro"),
        "ASUS" to arrayOf("ASUS ROG Phone 8 Pro", "ASUS Zenfone 11 Ultra"),
    )

    

    
    private data class GpuNumbers(
        val vendor: String, val glVersion: String, val glsl: String, val vkApi: String,
        val driver: String, val vendorId: String, val deviceId: String,
        val memoryMb: Int, val maxTex: Int, val layers: Int, val push: Int,
    )

    private fun gpuNumbersFor(gpu: String): GpuNumbers {
        val g = gpu.uppercase()
        return when {
            g.contains("ADRENO") -> GpuNumbers(
                "Qualcomm", "OpenGL ES 3.2 V@0502.0", "OpenGL ES GLSL ES 3.20",
                "1.1.0", "0x8020000", "0x5143", "0x72120000",
                12228, 16384, 2048, 256,
            )
            g.contains("MALEOON") -> GpuNumbers(
                "HiSilicon", "OpenGL ES 3.2 V@0502.0", "OpenGL ES GLSL ES 3.20",
                "1.0.0", "0x1000000", "0x13B6", "0x9000000",
                8192, 16384, 2048, 256,
            )
            else -> GpuNumbers(  
                "ARM", "OpenGL ES 3.2 v1.r32p1", "OpenGL ES GLSL ES 3.20",
                "1.1.0", "0x2000000", "0x13B5", "0x72120000",
                7469, 16384, 4096, 256,
            )
        }
    }

    fun generate(): Map<String, String> {
        val p = PROFILES[RANDOM.nextInt(PROFILES.size)]
        val brand = p.brand
        val model = p.model
        val manufacturer = brand
        
        val release = arrayOf("12", "13", "14", "15")[RANDOM.nextInt(4)]
        val sdk = XpConfig.sdkFor(release)
        val device = p.device
        val product = p.product
        val board = p.board
        val id = "UP1A." + (RANDOM.nextInt(10000) + 230000) + "." + (RANDOM.nextInt(90) + 10)
        val incremental = (RANDOM.nextInt(9000000) + 1000000).toString()
        val fingerprint = brand.lowercase() + "/" + product + "/" + device + ":" +
                release + "/" + id + "/" + incremental + ":user/release-keys"

        val out = HashMap<String, String>()
        out[XpConfig.KEY_BUILD_BRAND] = brand
        out[XpConfig.KEY_BUILD_MANUFACTURER] = manufacturer
        out[XpConfig.KEY_BUILD_MODEL] = model
        out[XpConfig.KEY_BUILD_DEVICE] = device
        out[XpConfig.KEY_BUILD_PRODUCT] = product
        out[XpConfig.KEY_BUILD_BOARD] = board
        
        
        out[XpConfig.KEY_BUILD_HARDWARE] = p.hardware
        out[XpConfig.KEY_BUILD_SOC_MODEL] = p.soc
        out[XpConfig.KEY_FAKE_CPUINFO_HW] = p.soc
        out[XpConfig.KEY_FAKE_PLATFORM] = p.platform
        out[XpConfig.KEY_FAKE_GPU] = p.gpu
        
        val g = gpuNumbersFor(p.gpu)
        out[XpConfig.KEY_FAKE_GPU_VENDOR] = g.vendor
        out[XpConfig.KEY_FAKE_GPU_GL_VERSION] = g.glVersion
        out[XpConfig.KEY_FAKE_GPU_GLSL] = g.glsl
        out[XpConfig.KEY_FAKE_GPU_VK_API] = g.vkApi
        out[XpConfig.KEY_FAKE_GPU_DRIVER] = g.driver
        out[XpConfig.KEY_FAKE_GPU_VENDOR_ID] = g.vendorId
        out[XpConfig.KEY_FAKE_GPU_DEVICE_ID] = g.deviceId
        out[XpConfig.KEY_FAKE_GPU_MEMORY_MB] = g.memoryMb.toString()
        out[XpConfig.KEY_FAKE_GPU_MAX_TEX] = g.maxTex.toString()
        out[XpConfig.KEY_FAKE_GPU_MAX_CUBE] = g.maxTex.toString()
        out[XpConfig.KEY_FAKE_GPU_MAX_LAYERS] = g.layers.toString()
        out[XpConfig.KEY_FAKE_GPU_PUSH] = g.push.toString()
        out[XpConfig.KEY_FAKE_SDK_INT] = sdk.toString()
        out[XpConfig.KEY_BUILD_FINGERPRINT] = fingerprint
        out[XpConfig.KEY_BUILD_ID] = id
        out[XpConfig.KEY_BUILD_DISPLAY] = id
        out[XpConfig.KEY_BUILD_TYPE] = "user"
        out[XpConfig.KEY_BUILD_TAGS] = "release-keys"
        out[XpConfig.KEY_BUILD_HOST] = "build-host-" + randStr(4).lowercase()
        out[XpConfig.KEY_BUILD_USER] = "android-build"
        out[XpConfig.KEY_BUILD_BOOTLOADER] = "unknown"
        out[XpConfig.KEY_BUILD_RADIO] = "g850-" + (RANDOM.nextInt(900000) + 100000) +
                "-240101-B-" + (RANDOM.nextInt(9000) + 1000)
        out[XpConfig.KEY_BUILD_SERIAL] = randStr(8).uppercase()
        out[XpConfig.KEY_BUILD_RELEASE] = release
        out[XpConfig.KEY_BUILD_SECURITY_PATCH] = "2024-" +
                pad2(RANDOM.nextInt(12) + 1) + "-" + pad2(RANDOM.nextInt(28) + 1)
        out[XpConfig.KEY_BUILD_INCREMENTAL] = incremental
        out[XpConfig.KEY_BUILD_CODENAME] = "REL"
        out[XpConfig.KEY_BUILD_BASE_OS] = ""
        out[XpConfig.KEY_DEVICE_NAME] = "$brand $model"
        out[XpConfig.KEY_ANDROID_ID] = androidId()
        out[XpConfig.KEY_GSF_ID] = gsfId()
        out[XpConfig.KEY_ADS_ID] = UUID.randomUUID().toString()
        out[XpConfig.KEY_APPSET_ID] = UUID.randomUUID().toString()
        out[XpConfig.KEY_DRM_ID] = drmHex()

        
        out[XpConfig.KEY_OAID] = UUID.randomUUID().toString()
        out[XpConfig.KEY_FAKE_KERNEL] = kernelVersion()
        out[XpConfig.KEY_FAKE_ARCH] = "aarch64"
        out[XpConfig.KEY_FAKE_HW_SERIAL] = randStr(10).uppercase()
        out[XpConfig.KEY_FAKE_FB_FID] = firebaseId()
        out[XpConfig.KEY_FAKE_FB_IID] = firebaseId()

        
        out[XpConfig.KEY_FAKE_WIFI_MAC] = mac()
        out[XpConfig.KEY_FAKE_BT_MAC] = mac()
        out[XpConfig.KEY_FAKE_WIFI_SSID] = brand.uppercase() + "-5G"
        out[XpConfig.KEY_FAKE_WIFI_BSSID] = mac()
        out[XpConfig.KEY_FAKE_CARRIER] =
            arrayOf("China Mobile", "China Unicom", "China Telecom")[RANDOM.nextInt(3)]

        
        out[XpConfig.KEY_FAKE_SIM_OPERATOR] = arrayOf("46000", "46001", "46003")[RANDOM.nextInt(3)]
        out[XpConfig.KEY_FAKE_SIM_OPERATOR_NAME] = out[XpConfig.KEY_FAKE_CARRIER] ?: "CMCC"
        out[XpConfig.KEY_FAKE_SIM_COUNTRY] = "cn"
        out[XpConfig.KEY_FAKE_SIM_SERIAL] = digits(19)
        out[XpConfig.KEY_FAKE_SIM_SUBSCRIBER] = "4600" + digits(11)
        out[XpConfig.KEY_FAKE_IMEI] = imei()
        out[XpConfig.KEY_FAKE_MEID] = randHex(14).uppercase()
        out[XpConfig.KEY_FAKE_ICCID] = digits(19)
        out[XpConfig.KEY_FAKE_PHONE_NUMBER] = "1" + arrayOf("3", "5", "7", "8", "9")[RANDOM.nextInt(5)] +
                digits(9)
        return out
    }

    private fun mac(): String {
        val b = ByteArray(6)
        SECURE.nextBytes(b)
        
        b[0] = (b[0].toInt() and 0xFE or 0x02).toByte()
        return b.joinToString(":") { String.format("%02X", it.toInt() and 0xff) }
    }

    private fun digits(n: Int): String {
        val sb = StringBuilder(n)
        repeat(n) { sb.append(RANDOM.nextInt(10)) }
        return sb.toString()
    }

    private fun randHex(n: Int): String {
        val sb = StringBuilder(n)
        val chars = "0123456789ABCDEF"
        repeat(n) { sb.append(chars[RANDOM.nextInt(16)]) }
        return sb.toString()
    }

    private fun imei(): String {
        
        val base = digits(14)
        var sum = 0
        for (i in 0 until 14) {
            var d = base[i] - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        val check = (10 - (sum % 10)) % 10
        return base + check
    }

    private fun kernelVersion(): String {
        val lts = arrayOf("4.14", "4.19", "5.4", "5.10", "5.15", "6.1")[RANDOM.nextInt(6)]
        val patch = RANDOM.nextInt(300)
        val hash = randStr(13).lowercase() + RANDOM.nextInt(10)
        return "$lts.$patch-g$hash"
    }

    private fun firebaseId(): String {
        val sb = StringBuilder(22)
        repeat(22) { sb.append(RANDOM.nextInt(10)) }
        return sb.toString()
    }

    
    fun androidId(): String {
        val b = ByteArray(8)
        SECURE.nextBytes(b)
        val sb = StringBuilder(16)
        for (x in b) sb.append(String.format("%02x", x.toInt() and 0xff))
        return sb.toString()
    }

    
    fun gsfId(): String {
        var v = BigInteger(63, SECURE)
        if (v > BigInteger.valueOf(Long.MAX_VALUE)) v = BigInteger.valueOf(Long.MAX_VALUE)
        var hex = v.toString(16).uppercase()
        while (hex.length < 16) hex = "0$hex"
        return hex
    }

    
    fun drmHex(): String {
        val b = ByteArray(32)
        SECURE.nextBytes(b)
        val sb = StringBuilder(64)
        for (x in b) sb.append(String.format("%02X", x.toInt() and 0xff))
        return sb.toString()
    }

    private fun randStr(n: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder(n)
        repeat(n) { sb.append(chars[RANDOM.nextInt(chars.length)]) }
        return sb.toString()
    }

    private fun pad2(v: Int): String = String.format("%02d", abs(v))
}
