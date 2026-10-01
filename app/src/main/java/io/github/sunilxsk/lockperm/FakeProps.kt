package io.github.sunilxsk.lockperm

import java.util.Locale












internal object FakeProps {

    private val FAMILY = listOf("system", "vendor", "product", "odm", "system_ext")

    









    private fun expand(prop: String): List<String> {
        val out = LinkedHashSet<String>()
        out.add(prop)
        if (!prop.startsWith("ro.")) return out.toList()

        when {
            
            prop.startsWith("ro.product.") -> {
                val tail = prop.removePrefix("ro.product.")
                
                if (!tail.startsWith("cpu.")) {
                    FAMILY.forEach { out.add("ro.product.$it.$tail") }
                }
            }

            
            
            prop.startsWith("ro.build.") -> {
                val tail = prop.removePrefix("ro.build.")
                FAMILY.forEach { out.add("ro.$it.build.$tail") }
                out.add("ro.bootimage.build.$tail")
                
                if (tail.startsWith("version.")) {
                    val v = tail.removePrefix("version.")
                    FAMILY.forEach { out.add("ro.$it.build.version.$v") }
                }
            }

            
            prop == "ro.hardware" -> {
                out.add("ro.boot.hardware")
                out.add("ro.hardware.platform")
            }

            
            prop == "ro.serialno" -> {
                out.add("ro.boot.serialno")
                out.add("ro.kernel.android.serialno")
            }

            
            prop == "ro.bootloader" -> {
                out.add("ro.boot.bootloader")
            }

            
            prop.startsWith("ro.boot.") -> {
                out.add("ro." + prop.removePrefix("ro.boot."))
            }
        }
        return out.toList()
    }

    fun build(cfg: XpState.Snapshot): Map<String, String> {
        val out = LinkedHashMap<String, String>()

        fun put(prop: String, v: String?) {
            if (v.isNullOrEmpty()) return
            expand(prop).forEach { out[it] = v }
        }

        
        XpConfig.BUILD_FIELDS.forEach { f ->
            put(f.prop, cfg.buildValues[f.field])
        }

        val bv = cfg.buildValues
        val brand = bv["BRAND"] ?: ""
        val product = bv["PRODUCT"] ?: ""
        val device = bv["DEVICE"] ?: ""
        val release = bv["RELEASE"] ?: ""
        val id = bv["ID"] ?: ""
        val inc = bv["INCREMENTAL"] ?: ""
        val tags = bv["TAGS"] ?: "release-keys"
        val type = bv["TYPE"] ?: "user"

        
        if (bv["FINGERPRINT"] == null && brand.isNotEmpty() &&
            product.isNotEmpty() && device.isNotEmpty() && release.isNotEmpty()
        ) {
            put("ro.build.fingerprint", "$brand/$product/$device:$release/$id/$inc:$type/$tags")
        }

        
        var rel = release
        var sdk = cfg.exSdkInt
        if (sdk > 0 && rel.isEmpty()) rel = XpConfig.releaseFor(sdk)
        if (rel.isNotEmpty() && sdk <= 0) sdk = XpConfig.sdkFor(rel)
        if (rel.isNotEmpty()) {
            put("ro.build.version.release", rel)
            put("ro.build.version.codename", "REL")
        }
        if (sdk > 0) {
            put("ro.build.version.sdk", sdk.toString())
            put("ro.build.version.preview_sdk", "0")
            put("ro.build.version.min_supported_target_sdk", "23")
        }

        
        val soc = cfg.exCpuInfoHw.ifEmpty { bv["SOC_MODEL"].orEmpty() }
        val platform = cfg.exPlatform.ifEmpty { bv["HARDWARE"].orEmpty().ifEmpty { soc } }
        val hw = bv["HARDWARE"].orEmpty().ifEmpty { platform.ifEmpty { soc } }
        put("ro.soc.model", soc)
        put("ro.soc.manufacturer", XpConfig.socVendor(soc))
        put("ro.hardware", hw)
        put("ro.board.platform", platform)
        put("ro.hardware.chipset", soc)

        
        
        val kernel = kernelVersion(cfg)
        put("ro.kernel.version", kernel)
        put("ro.build.kernel.id", kernel)
        
        if (platform.isNotEmpty()) {
            put("ro.hardware.platform", platform)
            put("ro.board.chipset", platform)
        }

        
        abiFor(cfg.exArch)?.let { a ->
            put("ro.product.cpu.abi", a.first)
            put("ro.product.cpu.abilist", a.second)
            if (a.third.isNotEmpty()) put("ro.product.cpu.abilist32", a.third)
            if (a.fourth.isNotEmpty()) put("ro.product.cpu.abilist64", a.fourth)
        }

        
        if (cfg.exDevOff) {
            put("ro.debuggable", "0")
            put("ro.secure", "1")
            put("ro.build.type", "user")
            put("ro.build.tags", "release-keys")
            put("ro.boot.mode", "normal")
        }

        
        val serial = bv["SERIAL"]
        if (!serial.isNullOrEmpty()) {
            put("ro.boot.serialno", serial)
            put("ro.serialno", serial)
            put("ro.kernel.android.serialno", serial)
        }

        
        
        
        put("ro.product.device", device)
        put("ro.product.name", product)
        put("ro.product.model", bv["MODEL"])
        put("ro.product.brand", brand)
        put("ro.product.manufacturer", bv["MANUFACTURER"])
        put("ro.product.board", bv["BOARD"])
        if (product.isNotEmpty()) put("ro.build.product", product)
        
        val gpu = cfg.exGpu
        if (gpu.isNotEmpty()) {
            put("ro.hardware.egl", gpu.lowercase().replace(" ", "_"))
            put("ro.opengles.version", "196610")
        }

        
        cfg.customProps.forEach { (k, v) ->
            if (k.isBlank()) return@forEach
            expand(k).forEach { out[it] = v }
        }

        return out
    }

    
    private fun abiFor(arch: String): Quad? {
        val a = arch.lowercase().ifEmpty { "aarch64" }
        return when {
            a.startsWith("aarch64") || a.startsWith("arm64") ->
                Quad("arm64-v8a", "arm64-v8a,armeabi-v7a,armeabi", "armeabi-v7a,armeabi", "arm64-v8a")

            a.startsWith("armv7") || a.startsWith("armv8") || a == "arm" ->
                Quad("armeabi-v7a", "armeabi-v7a,armeabi", "armeabi-v7a,armeabi", "")

            a == "x86_64" || a == "amd64" ->
                Quad("x86_64", "x86_64,x86", "x86", "x86_64")

            a == "x86" || a == "i686" ->
                Quad("x86", "x86,armeabi-v7a,armeabi", "x86,armeabi-v7a,armeabi", "")

            else -> null
        }
    }

    private data class Quad(val first: String, val second: String, val third: String, val fourth: String)

    

    fun kernelVersion(cfg: XpState.Snapshot): String =
        cfg.exKernel.ifEmpty { XpConfig.DEF_KERNEL }

    fun arch(cfg: XpState.Snapshot): String =
        cfg.exArch.ifEmpty { XpConfig.DEF_ARCH }

    fun cpuHardware(cfg: XpState.Snapshot): String =
        cfg.exCpuInfoHw.ifEmpty {
            cfg.buildValues["SOC_MODEL"].orEmpty().ifEmpty {
                cfg.buildValues["HARDWARE"].orEmpty()
            }
        }

    fun procVersion(cfg: XpState.Snapshot): String =
        "Linux version ${kernelVersion(cfg)} (build-user@build-host) " +
                "(Android (8508608, based on r450784e) clang version 17.0.2) " +
                "#1 SMP PREEMPT Mon Jan 1 00:00:00 UTC 2024"

    private const val UNAME_VERSION = "#1 SMP PREEMPT Mon Jan 1 00:00:00 UTC 2024"

    fun uname(cfg: XpState.Snapshot, flags: Set<Char>): String {
        if (flags.isEmpty()) return "Linux"
        val parts = LinkedHashMap<Char, String>()
        parts['s'] = "Linux"
        parts['n'] = "localhost"
        parts['r'] = kernelVersion(cfg)
        parts['v'] = UNAME_VERSION
        parts['m'] = arch(cfg)
        parts['p'] = arch(cfg)
        parts['i'] = arch(cfg)
        parts['o'] = "GNU/Linux"
        
        val want = if ('a' in flags) setOf('s', 'n', 'r', 'v', 'm', 'o') else flags
        val ordered = "snrvmipo".toList()
        val sb = StringBuilder()
        var first = true
        for (ch in ordered) {
            if (ch !in want) continue
            val v = parts[ch] ?: continue
            if (!first) sb.append(' ')
            sb.append(v)
            first = false
        }
        return sb.toString()
    }

    
    fun uptimeContent(hours: Float): String =
        String.format(Locale.US, "%.2f %.2f", hours * 3600f, hours * 3600f * 0.35f)

    



    fun cpuInfo(cfg: XpState.Snapshot): String {
        val hw = cpuHardware(cfg)
        if (hw.isEmpty()) return ""
        val raw: String = FileSpoofer.readRaw("/proc/cpuinfo") ?: ""
        if (raw.isBlank()) return syntheticCpuInfo(hw)
        return raw.split("\n").joinToString("\n") { line: String ->
            if (line.startsWith("Hardware")) "Hardware\t: $hw" else line
        }
    }

    private fun syntheticCpuInfo(hw: String): String {
        val sb = StringBuilder()
        for (i in 0 until 8) {
            sb.append("processor\t: $i\n")
            sb.append("BogoMIPS\t: 38.40\n")
            sb.append("Features\t: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp " +
                    "asimdhp cpuid asimdrdm lrcpc dcpop asimddp\n")
            sb.append("CPU implementer\t: 0x41\n")
            sb.append("CPU architecture: 8\n")
            sb.append("CPU variant\t: 0x2\n")
            sb.append("CPU part\t: 0xd05\n")
            sb.append("CPU revision\t: ${i % 4}\n\n")
        }
        sb.append("Hardware\t: $hw\n")
        return sb.toString()
    }
}
