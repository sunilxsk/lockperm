package io.github.sunilxsk.lockperm
























internal object NativePayload {

    fun build(cfg: XpState.Snapshot, cacheDir: String?): String {
        val sb = StringBuilder()

        val kernel = FakeProps.kernelVersion(cfg)
        val arch = FakeProps.arch(cfg)
        
        if (cfg.nativeAntiDetect) sb.append("W\t1\n")
        if (kernel.isNotEmpty()) sb.append("K\t").append(kernel).append('\n')
        if (arch.isNotEmpty()) sb.append("A\t").append(arch).append('\n')
        if (cfg.exGpu.isNotEmpty()) {
            sb.append("G\t").append(cfg.exGpu).append('\n')
            
            val gv = cfg.exGpuVendor.ifEmpty { XpConfig.gpuVendor(cfg.exGpu) }
            sb.append("g\tvendor\t").append(gv).append('\n')
            val glVer = cfg.exGpuGlVersion.ifEmpty { "OpenGL ES 3.2 V@0502.0" }
            sb.append("g\tglversion\t").append(glVer).append('\n')
            val glsl = cfg.exGpuGlsl.ifEmpty { "OpenGL ES GLSL ES 3.20" }
            sb.append("g\tglsl\t").append(glsl).append('\n')
            if (cfg.exGpuVkApi.isNotEmpty()) {
                sb.append("g\tvkapi\t").append(XpConfig.vkApiVersion(cfg.exGpuVkApi)).append('\n')
            }
            if (cfg.exGpuDriver.isNotEmpty()) {
                sb.append("g\tdriver\t").append(XpConfig.hexInt(cfg.exGpuDriver)).append('\n')
            }
            if (cfg.exGpuVendorId.isNotEmpty()) {
                sb.append("g\tvendorid\t").append(XpConfig.hexInt(cfg.exGpuVendorId)).append('\n')
            }
            if (cfg.exGpuDeviceId.isNotEmpty()) {
                sb.append("g\tdeviceid\t").append(XpConfig.hexInt(cfg.exGpuDeviceId)).append('\n')
            }
            sb.append("g\tmemory\t").append(cfg.exGpuMemoryMb).append('\n')
            sb.append("g\tmaxdim\t").append(cfg.exGpuMaxTex).append('\n')
            sb.append("g\tmaxcube\t").append(cfg.exGpuMaxCube).append('\n')
            sb.append("g\tlayers\t").append(cfg.exGpuMaxLayers).append('\n')
            sb.append("g\tpush\t").append(cfg.exGpuPush).append('\n')
        }
        if (!cacheDir.isNullOrEmpty()) sb.append("D\t").append(cacheDir).append('\n')

        
        FakeProps.build(cfg).forEach { (k, v) ->
            if (k.isBlank()) return@forEach
            sb.append("P\t").append(k).append('\t').append(escape(v)).append('\n')
        }

        
        if (cfg.exTimeEnable && cfg.exTimeOffset != 0) {
            sb.append("T\t").append(cfg.exTimeOffset.toLong() * 60_000L).append('\n')
        }

        
        if (cfg.exUptimeEnable) {
            val hours = cfg.exUptimeHours.toFloatOrNull()
            if (hours != null && hours > 0f) {
                sb.append("U\t").append((hours * 3_600_000f).toLong()).append('\n')
            }
        }

        
        if (cfg.vpnHideEnable && cfg.vpnHideIface && cfg.vpnIfaces.isNotEmpty()) {
            sb.append("V\t").append(cfg.vpnIfaces.joinToString(",")).append('\n')
        }

        
        cfg.hidePaths.forEach { p ->
            if (p.isNotBlank()) sb.append("H\t").append(p.trim()).append('\n')
        }

        
        if (cfg.rootFakeEnable && cfg.rootFakeFile) {
            SU_PATHS.forEach { sb.append("S\t").append(it).append('\n') }
        }

        
        if (cfg.blockExec) sb.append("X\t1\n")

        
        if (cfg.nativeBlockExit) sb.append("E\t1\n")

        
        if (cfg.crashCatchEnable) sb.append("N\t1\n")

        
        
        
        if (cfg.exUptimeEnable) {
            val hours = cfg.exUptimeHours.toFloatOrNull()
            if (hours != null) {
                sb.append("C\t/proc/uptime\t").append(escape(FakeProps.uptimeContent(hours)))
                    .append('\n')
            }
        }

        
        val cpuContent = runCatching { FakeProps.cpuInfo(cfg) }.getOrDefault("")
        if (cpuContent.isNotEmpty()) {
            sb.append("C\t/proc/cpuinfo\t").append(escape(cpuContent)).append('\n')
        }

        if (kernel.isNotEmpty()) {
            sb.append("C\t/proc/version\t").append(escape(FakeProps.procVersion(cfg))).append('\n')
            sb.append("C\t/proc/sys/kernel/osrelease\t").append(escape(kernel)).append('\n')
        }

        
        if (cfg.vpnHideEnable && cfg.vpnHideIface) {
            VpnProxyDefender.netFiles.forEach { (path, content) ->
                if (content.isNotEmpty()) {
                    sb.append("C\t").append(path).append('\t').append(escape(content)).append('\n')
                }
            }
        }

        return sb.toString()
    }

    
    private val SU_PATHS = listOf(
        "/system/bin/su", "/system/xbin/su", "/system/bin/su.bin", "/system/xbin/su.bin",
        "/sbin/su", "/system/su", "/su/bin/su", "/su/bin/su.bin",
        "/system/bin/.ext/su", "/system/xbin/daemonsu", "/system/xbin/ext/su",
        "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
        "/system/app/Superuser.apk", "/system/app/Superuser/Superuser.apk",
        "/system/bin/failsafe/su", "/system/sd/xbin/su",
    )

    private fun escape(s: String): String {
        val o = StringBuilder(s.length + 8)
        for (c in s) {
            when (c) {
                '\\' -> o.append("\\\\")
                '\n' -> o.append("\\n")
                '\t' -> o.append("\\t")
                '\r' -> o.append("")
                else -> o.append(c)
            }
        }
        return o.toString()
    }
}
