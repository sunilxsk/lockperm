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

        
        
        
        
        val masterOn = cfg.enableBuild

        
        if (masterOn) {
            XpConfig.BUILD_FIELDS.forEach { f ->
                put(f.prop, cfg.buildValues[f.field])
            }
        }

        val bv = cfg.buildValues

        
        val brand = if (masterOn) bv["BRAND"] ?: "" else ""
        val product = if (masterOn) bv["PRODUCT"] ?: "" else ""
        val device = if (masterOn) bv["DEVICE"] ?: "" else ""
        val release = if (masterOn) bv["RELEASE"] ?: "" else ""
        val id = if (masterOn) bv["ID"] ?: "" else ""
        val inc = if (masterOn) bv["INCREMENTAL"] ?: "" else ""
        val tags = if (masterOn) bv["TAGS"] ?: "release-keys" else ""
        val type = if (masterOn) bv["TYPE"] ?: "user" else ""

        
        if (masterOn && bv["FINGERPRINT"] == null && brand.isNotEmpty() &&
            product.isNotEmpty() && device.isNotEmpty() && release.isNotEmpty()
        ) {
            put("ro.build.fingerprint", "$brand/$product/$device:$release/$id/$inc:$type/$tags")
        }

        
        var rel = release
        var sdk = if (masterOn) cfg.exSdkInt else 0
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

        
        
        
        if (masterOn) {
            val cpuPreset = if (cfg.exCpuEnable && cfg.exCpuMode == XpConfig.CPU_MODE_PRESET) {
                XpConfig.CPU_PRESETS.getOrNull(cfg.exCpuPreset)
            } else {
                null
            }
            val soc = cpuPreset?.soc
                ?: cfg.exCpuInfoHw.ifEmpty { bv["SOC_MODEL"].orEmpty() }
            val platform = cpuPreset?.board
                ?: cfg.exPlatform.ifEmpty { bv["HARDWARE"].orEmpty().ifEmpty { soc } }
            val hw = cpuPreset?.soc
                ?: bv["HARDWARE"].orEmpty().ifEmpty { platform.ifEmpty { soc } }
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
        }

        
        
        val arch = arch(cfg)
        if (arch.isNotEmpty()) {
            abiFor(arch)?.let { a ->
                put("ro.product.cpu.abi", a.first)
                put("ro.product.cpu.abilist", a.second)
                if (a.third.isNotEmpty()) put("ro.product.cpu.abilist32", a.third)
                if (a.fourth.isNotEmpty()) put("ro.product.cpu.abilist64", a.fourth)
            }
        }

        
        if (masterOn && cfg.exDevOff) {
            put("ro.debuggable", "0")
            put("ro.secure", "1")
            put("ro.build.type", "user")
            put("ro.build.tags", "release-keys")
            put("ro.boot.mode", "normal")
        }

        
        val serial = bv["SERIAL"]
        if (masterOn && !serial.isNullOrEmpty()) {
            put("ro.boot.serialno", serial)
            put("ro.serialno", serial)
            put("ro.kernel.android.serialno", serial)
        }

        
        
        
        if (masterOn) {
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
        }

        
        
        
        
        
        
        
        if (masterOn) {
            val radio = bv["RADIO"].orEmpty()
            if (radio.isNotEmpty()) {
                put("gsm.version.baseband", radio)
                put("gsm.version.baseband1", radio)
                put("gsm.version.baseband2", radio)
                put("gsm.baseband.capability", radio)
                put("gsm.version.ril-impl", radio)
                put("ril.hw.version", radio)
                put("ro.build.expect.baseband", radio)
                put("persist.radio.hw_version", radio)
                put("vendor.gsm.version.baseband", radio)
            }
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

    






    fun abiList(arch: String): List<String> = when {
        arch.isEmpty() -> emptyList()
        arch.startsWith("aarch64") || arch.startsWith("arm64") ->
            listOf("arm64-v8a", "armeabi-v7a", "armeabi")

        arch.startsWith("armv7") || arch.startsWith("armv8") || arch == "arm" ->
            listOf("armeabi-v7a", "armeabi")

        arch == "x86_64" || arch == "amd64" -> listOf("x86_64", "x86")
        arch == "x86" || arch == "i686" -> listOf("x86", "armeabi-v7a", "armeabi")
        else -> emptyList()
    }

    
    fun abiList32(arch: String): List<String> = abiList(arch).filter { it.contains("32") || it in setOf("armeabi-v7a", "armeabi", "x86") }

    
    fun abiList64(arch: String): List<String> = abiList(arch).filter { it in setOf("arm64-v8a", "x86_64") }

    

    




    fun kernelVersion(cfg: XpState.Snapshot): String = cfg.exKernel.trim()

    
    fun arch(cfg: XpState.Snapshot): String = cfg.exArch.trim()

    fun cpuHardware(cfg: XpState.Snapshot): String {
        if (!cfg.enableBuild) return ""
        return cfg.exCpuInfoHw.ifEmpty {
            cfg.buildValues["SOC_MODEL"].orEmpty().ifEmpty {
                cfg.buildValues["HARDWARE"].orEmpty()
            }
        }
    }

    fun procVersion(cfg: XpState.Snapshot): String =
        "Linux version ${kernelVersion(cfg)} (build-user@build-host) " +
                "(Android (8508608, based on r450784e) clang version 17.0.2) " +
                "#1 SMP PREEMPT Mon Jan 1 00:00:00 UTC 2024"

    const val UNAME_VERSION = "#1 SMP PREEMPT Mon Jan 1 00:00:00 UTC 2024"

    
    fun uname(cfg: XpState.Snapshot, flags: Set<Char>): String? {
        val kernel = kernelVersion(cfg)
        val arch = arch(cfg)
        
        if (kernel.isEmpty() && arch.isEmpty()) return null
        if (flags.isEmpty()) return "Linux"
        val parts = LinkedHashMap<Char, String>()
        parts['s'] = "Linux"
        parts['n'] = "localhost"
        if (kernel.isNotEmpty()) {
            parts['r'] = kernel
            parts['v'] = UNAME_VERSION
        }
        if (arch.isNotEmpty()) {
            parts['m'] = arch
            parts['p'] = arch
            parts['i'] = arch
        }
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
        
        if (!cfg.enableBuild) return ""
        if (!cfg.exCpuEnable && cfg.exCpuInfoHw.isEmpty()) return ""

        if (cfg.exCpuEnable && cfg.exCpuMode == XpConfig.CPU_MODE_CUSTOM) {
            val custom = cfg.exCpuCustom
            if (custom.isNotBlank()) return custom
        }
        if (cfg.exCpuEnable && cfg.exCpuMode == XpConfig.CPU_MODE_PRESET) {
            XpConfig.CPU_PRESETS.getOrNull(cfg.exCpuPreset)?.let { return it.cpuinfo }
        }

        val hw = cpuHardware(cfg)
        if (hw.isEmpty()) return ""
        val raw: String = FileSpoofer.readRaw("/proc/cpuinfo") ?: ""
        if (raw.isBlank()) return syntheticCpuInfo(hw, cfg.exCpuCores)

        
        
        
        
        val n = Regex("^processor\\s*:", RegexOption.MULTILINE).findAll(raw).count()
            .takeIf { it > 0 }?.coerceIn(1, 32) ?: cfg.exCpuCores.coerceIn(1, 32)
        val (mins, maxs, _) = coreFreqs(cfg, n)
        var idx = -1
        return raw.split("\n").joinToString("\n") { line: String ->
            val key = line.substringBefore(':').trim()
            when {
                key == "processor" -> {
                    idx++
                    line
                }
                key == "Hardware" -> "Hardware\t: $hw"
                key == "Processor" -> "Processor\t: $hw"
                key == "model name" -> "model name\t: $hw"
                
                
                key == "cpu MHz" -> if (idx in 0 until n) {
                    "cpu MHz\t: ${String.format(Locale.US, "%.2f", maxs[idx] / 1000f)}"
                } else line
                key == "BogoMIPS" -> line
                else -> line
            }
        }
        
        
        .let { text ->
            if (text.contains("Hardware")) text else "$text\nHardware\t: $hw\n"
        }
    }

    
    
    
    
    
    
    
    
    

    






    private fun coreFreqs(cfg: XpState.Snapshot, n: Int): Triple<IntArray, IntArray, IntArray> {
        val clusters = XpConfig.cpuClusters(cpuHardware(cfg))

        
        val min = IntArray(n)
        val max = IntArray(n)
        val base = IntArray(n)
        var i = 0
        for (c in clusters) {
            repeat(c.coreCount()) {
                if (i >= n) return@repeat
                min[i] = c.minKhz
                max[i] = c.maxKhz
                base[i] = c.maxKhz
                i++
            }
            if (i >= n) break
        }
        val last = clusters.last()
        while (i < n) {
            min[i] = last.minKhz
            max[i] = last.maxKhz
            base[i] = last.maxKhz
            i++
        }

        
        XpConfig.parseFreqList(cfg.exCpuMinFreq).let { list ->
            if (list.isNotEmpty()) {
                for (k in 0 until n) min[k] = list[minOf(k, list.lastIndex)]
            }
        }
        XpConfig.parseFreqList(cfg.exCpuMaxFreq).let { list ->
            if (list.isNotEmpty()) {
                for (k in 0 until n) max[k] = list[minOf(k, list.lastIndex)]
            }
        }

        
        val cur = IntArray(n)
        XpConfig.parseFreqList(cfg.exCpuCurFreq).let { list ->
            if (list.isNotEmpty()) {
                for (k in 0 until n) cur[k] = list[minOf(k, list.lastIndex)]
                return@let
            }
            var j = 0
            for (c in clusters) {
                val v = (c.minKhz + (c.maxKhz - c.minKhz) * 0.72f).toInt()
                repeat(c.coreCount()) { if (j < n) cur[j++] = v }
                if (j >= n) break
            }
            
            val tail = if (n > 1) { if (cur[n - 1] > 0) cur[n - 1] else max[n - 1] } else max[0]
            while (j < n) cur[j++] = tail
        }
        return Triple(min, max, cur)
    }

    
    fun coreFreqsForPreview(
        cfg: XpState.Snapshot,
        n: Int,
    ): Triple<IntArray, IntArray, IntArray> = coreFreqs(cfg, n.coerceIn(1, 32))

    
    private fun clusterTopo(clusters: List<XpConfig.CpuCluster>, n: Int): List<IntRange> {
        val out = ArrayList<IntRange>()
        var start = 0
        for (c in clusters) {
            if (start >= n) break
            val end = minOf(start + c.coreCount(), n) - 1
            out.add(start..end)
            start = end + 1
        }
        return out
    }

    



    fun cpuFiles(cfg: XpState.Snapshot): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val info = cpuInfo(cfg)
        if (info.isNotEmpty()) out["/proc/cpuinfo"] = info

        
        
        
        val hw0 = cpuHardware(cfg)
        if (info.isEmpty() && hw0.isEmpty()) return out

        val cores = if (info.isNotEmpty()) {
            Regex("^processor\\s*:", RegexOption.MULTILINE).findAll(info).count()
        } else {
            0
        }
        val n = cores.takeIf { it > 0 }?.coerceIn(1, 32) ?: cfg.exCpuCores.coerceIn(1, 32)
        val topo = "0-${n - 1}"

        
        out["/sys/devices/system/cpu/present"] = topo
        out["/sys/devices/system/cpu/possible"] = topo
        out["/sys/devices/system/cpu/online"] = topo
        out["/sys/devices/system/cpu/kernel_max"] = (n - 1).toString()
        out["/sys/devices/system/cpu/offline"] = ""
        out["/sys/devices/system/cpu/cpu0/cpufreq/affected_cpus"] = topo
        out["/sys/devices/system/cpu/cpu0/cpufreq/related_cpus"] = topo
        out["/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_transition_latency"] = "1000"
        out["/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"] = "schedutil"

        val hw = cpuHardware(cfg)
        val clusters = XpConfig.cpuClusters(hw)
        val (mins, maxs, curs) = coreFreqs(cfg, n)
        for (i in 0 until n) {
            val base = "/sys/devices/system/cpu/cpu$i/cpufreq"
            out["$base/cpuinfo_min_freq"] = mins[i].toString()
            out["$base/cpuinfo_max_freq"] = maxs[i].toString()
            out["$base/scaling_min_freq"] = mins[i].toString()
            out["$base/scaling_max_freq"] = maxs[i].toString()
            out["$base/scaling_cur_freq"] = curs[i].toString()
            
            out["$base/cpuinfo_cur_freq"] = curs[i].toString()
            out["$base/scaling_available_frequencies"] =
                "${mins[i]} ${(mins[i] + maxs[i]) / 2} ${maxs[i]}"
            out["$base/scaling_governor"] = "schedutil"
            out["$base/cpuinfo_transition_latency"] = "1000"
            
            out["$base/scaling_available_governors"] = "schedutil ondemand performance powersave"
        }

        
        
        val ranges = clusterTopo(clusters, n)
        ranges.forEachIndexed { pi, r ->
            val topo = if (r.first == r.last) "${r.first}" else "${r.first}-${r.last}"
            val p = "/sys/devices/system/cpu/cpufreq/policy$r.first"
            out["$p/related_cpus"] = topo
            out["$p/affected_cpus"] = topo
            out["$p/cpuinfo_min_freq"] = mins[r.first].toString()
            out["$p/cpuinfo_max_freq"] = maxs[r.first].toString()
            out["$p/scaling_min_freq"] = mins[r.first].toString()
            out["$p/scaling_max_freq"] = maxs[r.first].toString()
            out["$p/scaling_cur_freq"] = curs[r.first].toString()
            out["$p/scaling_governor"] = "schedutil"
            out["$p/cpuinfo_transition_latency"] = "1000"
            
            val alias = "/sys/devices/system/cpu/cpufreq/policy${pi * 4}"
            if (alias != p) {
                out["$alias/related_cpus"] = topo
                out["$alias/cpuinfo_min_freq"] = mins[r.first].toString()
                out["$alias/cpuinfo_max_freq"] = maxs[r.first].toString()
                out["$alias/scaling_cur_freq"] = curs[r.first].toString()
            }
        }
        
        out["/sys/devices/system/cpu/cpufreq/boost"] = "0"

        
        val platform = cfg.exPlatform.ifEmpty { hw }
        if (hw.isNotEmpty()) {
            out["/sys/devices/soc0/machine"] = hw
            out["/sys/devices/soc0/hardware"] = hw
            out["/sys/devices/soc0/family"] = platform
            out["/sys/devices/soc0/vendor"] = XpConfig.socVendor(hw)
            out["/sys/devices/soc0/soc_id"] = "0"
            out["/sys/devices/soc0/platform_version"] = "0"
            out["/sys/devices/soc0/serial_number"] = "0"
            out["/sys/devices/soc0/build_id"] = "0"
            out["/sys/devices/soc0/accessory_chip"] = "0"
            out["/sys/devices/soc0/image_variant"] = "0"
            out["/sys/devices/system/soc/soc0/machine"] = hw
            out["/sys/devices/system/soc/soc0/family"] = platform
            out["/sys/devices/system/soc/soc0/hardware"] = hw
        }
        return out
    }

    





    fun memInfo(cfg: XpState.Snapshot): String {
        val mb = cfg.exMemMb.coerceIn(256, 65536)
        val kb = mb * 1024
        val raw = FileSpoofer.readRaw("/proc/meminfo")
        if (raw.isNullOrBlank()) {
            return buildString {
                append("MemTotal:       $kb kB\n")
                append("MemFree:        ${kb / 4} kB\n")
                append("MemAvailable:   ${kb / 2} kB\n")
                append("Buffers:        ${kb / 32} kB\n")
                append("Cached:         ${kb / 3} kB\n")
                append("SwapCached:         0 kB\n")
                append("Active:         ${kb / 3} kB\n")
                append("Inactive:       ${kb / 4} kB\n")
                append("SwapTotal:      ${kb / 2} kB\n")
                append("SwapFree:       ${kb / 2} kB\n")
            }
        }
        var hit = false
        val out = raw.split("\n").map { line ->
            if (line.startsWith("MemTotal:")) {
                hit = true
                "MemTotal:       $kb kB"
            } else {
                line
            }
        }
        return if (hit) out.joinToString("\n") else "MemTotal:       $kb kB\n" + raw
    }

    
    private fun syntheticCpuInfo(hw: String, cores: Int): String {
        val n = cores.coerceIn(1, 32)
        val implementer = when {
            hw.uppercase().startsWith("MT") -> "0x41"
            hw.uppercase().startsWith("SM") -> "0x51"
            hw.contains("Kirin", true) -> "0x48"
            hw.uppercase().startsWith("S5E") -> "0x53"
            else -> "0x41"
        }
        val clusters = XpConfig.cpuClusters(hw)
        
        val scaled = if (clusters.sumOf { it.coreCount() } == n) {
            clusters
        } else {
            val total = clusters.sumOf { it.coreCount() }.coerceAtLeast(1)
            val out = ArrayList<XpConfig.CpuCluster>()
            var left = n
            clusters.forEachIndexed { i, c ->
                val take = if (i == clusters.lastIndex) {
                    left
                } else {
                    (c.coreCount().toFloat() / total * n).toInt().coerceIn(1, left)
                }
                if (take > 0) {
                    out.add(c.copy(cores = take))
                    left -= take
                }
            }
            if (out.isEmpty()) out.add(clusters.first().copy(cores = n))
            out
        }
        return XpConfig.cpuinfoOf(hw, implementer, scaled)
    }
}
