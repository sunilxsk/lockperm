package io.github.sunilxsk.lockperm

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface
import java.util.Locale


















internal object DeviceProbe {

    
    @SuppressLint("MissingPermission", "HardwareIds", "NewApi", "QueryPermissionsNeeded")
    fun collect(context: Context): DeviceTemplate {
        val nat = runCatching { NativeBridge.probe() }.getOrDefault(emptyMap())

        
        
        val netEnv = networkEnv()
        val sysEnv = systemEnv()
        val envBrief = buildList {
            netEnv["net.ifaces"]?.let { add("网卡 $it") }
            sysEnv["env.ANDROID_DATA"]?.let { add("data=$it") }
            sysEnv["env.TMPDIR"]?.let { add("tmp=$it") }
        }.joinToString("　")

        val tpl = DeviceTemplate(
            name = "本设备 ${Build.MODEL}",
            note = "采集于 ${tplTime(System.currentTimeMillis())}" +
                if (envBrief.isNotBlank()) "　$envBrief" else "",
        )

        
        for (f in XpConfig.BUILD_FIELDS) {
            val v = buildField(f.field, f.owner)
            if (!v.isNullOrBlank()) tpl.set(f.key, v)
        }

        
        val kernel = nat["uname.release"]
            .takeIf { !it.isNullOrBlank() }
            ?: runCatching { System.getProperty("os.version") }.getOrNull()
            ?: nat["proc.version"].orEmpty()
        if (kernel.isNotBlank()) tpl.set(XpConfig.KEY_FAKE_KERNEL, kernel.trim())

        val arch = nat["uname.machine"]
            .takeIf { !it.isNullOrBlank() }
            ?: runCatching { System.getProperty("os.arch") }.getOrNull()
            ?: nat["native.abi"].orEmpty()
        if (arch.isNotBlank()) tpl.set(XpConfig.KEY_FAKE_ARCH, arch.trim())

        
        val hw = nat["cpuinfo.hardware"]
            .takeIf { !it.isNullOrBlank() }
            ?: nat["soc0.machine"]
            ?: nat["prop.hardware"]
            ?: buildField("HARDWARE", XpConfig.OWNER_BUILD)
            ?: buildField("SOC_MODEL", XpConfig.OWNER_BUILD)
        if (!hw.isNullOrBlank()) tpl.set(XpConfig.KEY_FAKE_CPUINFO_HW, hw.trim())

        nat["prop.board_platform"]?.takeIf { it.isNotBlank() }
            ?.let { tpl.set(XpConfig.KEY_FAKE_PLATFORM, it.trim()) }

        val cores = nat["cpuinfo.processors"]?.toIntOrNull()
            ?: runCatching { Runtime.getRuntime().availableProcessors() }.getOrNull()
        if (cores != null && cores > 0) {
            tpl.set(XpConfig.KEY_FAKE_CPU_CORES, cores.toString())
        }

        
        val cpuinfo = runCatching { readProc("/proc/cpuinfo") }.getOrNull()
        if (!cpuinfo.isNullOrBlank()) {
            tpl.set(XpConfig.KEY_FAKE_CPU_MODE, XpConfig.CPU_MODE_CUSTOM)
            tpl.set(XpConfig.KEY_FAKE_CPU_CUSTOM, cpuinfo)
            
            nat["cpufreq.max_list"]?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_CPU_MAX_FREQ, it.trim()) }
            nat["cpufreq.min_list"]?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_CPU_MIN_FREQ, it.trim()) }
        }

        
        val gpu = runCatching { gpuProbe() }.getOrDefault(emptyMap())
        val gpuName = gpu["renderer"].orEmpty().ifBlank {
            runCatching { eglRenderer() }.getOrNull().orEmpty()
        }
        if (gpuName.isNotBlank()) {
            tpl.set(XpConfig.KEY_FAKE_GPU, gpuName.trim())
            gpu["vendor"]?.takeIf { it.isNotBlank() }?.let { tpl.set(XpConfig.KEY_FAKE_GPU_VENDOR, it) }
            gpu["glversion"]?.takeIf { it.isNotBlank() }?.let { tpl.set(XpConfig.KEY_FAKE_GPU_GL_VERSION, it) }
            gpu["glsl"]?.takeIf { it.isNotBlank() }?.let { tpl.set(XpConfig.KEY_FAKE_GPU_GLSL, it) }
            gpu["maxtex"]?.takeIf { it.isNotBlank() }?.let { tpl.set(XpConfig.KEY_FAKE_GPU_MAX_TEX, it) }
            gpu["maxcube"]?.takeIf { it.isNotBlank() }?.let { tpl.set(XpConfig.KEY_FAKE_GPU_MAX_CUBE, it) }
            gpu["layers"]?.takeIf { it.isNotBlank() }?.let { tpl.set(XpConfig.KEY_FAKE_GPU_MAX_LAYERS, it) }
            gpu["push"]?.takeIf { it.isNotBlank() }?.let { tpl.set(XpConfig.KEY_FAKE_GPU_PUSH, it) }
            
            val n = BuildRandom.gpuNumbersFor(gpuName)
            tpl.set(XpConfig.KEY_FAKE_GPU_VENDOR, gpu["vendor"].orEmpty().ifBlank { n.vendor })
            tpl.set(XpConfig.KEY_FAKE_GPU_VK_API, n.vkApi)
            tpl.set(XpConfig.KEY_FAKE_GPU_DRIVER, n.driver)
            tpl.set(XpConfig.KEY_FAKE_GPU_VENDOR_ID, n.vendorId)
            tpl.set(XpConfig.KEY_FAKE_GPU_DEVICE_ID, n.deviceId)
            tpl.set(XpConfig.KEY_FAKE_GPU_MEMORY_MB, n.memoryMb.toString())
            if (gpu["maxtex"].isNullOrBlank()) tpl.set(XpConfig.KEY_FAKE_GPU_MAX_TEX, n.maxTex.toString())
            if (gpu["layers"].isNullOrBlank()) tpl.set(XpConfig.KEY_FAKE_GPU_MAX_LAYERS, n.layers.toString())
            if (gpu["push"].isNullOrBlank()) tpl.set(XpConfig.KEY_FAKE_GPU_PUSH, n.push.toString())
        }

        
        val kb = nat["meminfo.total_kb"]?.toLongOrNull()
            ?: runCatching {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE)
                    as? android.app.ActivityManager
                val mi = android.app.ActivityManager.MemoryInfo()
                am?.getMemoryInfo(mi)
                mi.totalMem / 1024L
            }.getOrNull()
        if (kb != null && kb > 0) {
            tpl.set(XpConfig.KEY_FAKE_MEM_MB, (kb / 1024L).toString())
        }

        
        runCatching {
            val i = context.registerReceiver(
                null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val level = i?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = i?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                tpl.set(XpConfig.KEY_FAKE_BATTERY, (level * 100 / scale).toString())
            }
        }

        
        runCatching {
            val up = readProc("/proc/uptime")
            val first = up?.split(" ")?.firstOrNull()?.toFloatOrNull()
            if (first != null && first > 0f) {
                tpl.set(
                    XpConfig.KEY_FAKE_UPTIME_HOURS,
                    String.format(Locale.US, "%.1f", first / 3600f)
                )
            }
        }

        
        runCatching {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!id.isNullOrBlank()) tpl.set(XpConfig.KEY_ANDROID_ID, id)
        }

        
        val serial = runCatching { Build.getSerial() }.getOrNull()
            ?: nat["prop.serialno"]?.takeIf { it.isNotBlank() }
            ?: buildField("SERIAL", XpConfig.OWNER_BUILD)
        if (!serial.isNullOrBlank()) tpl.set(XpConfig.KEY_FAKE_HW_SERIAL, serial)

        
        fun tele(): android.telephony.TelephonyManager? =
            runCatching {
                context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            }.getOrNull()

        tele()?.let { tm ->
            runCatching { tm.simOperator }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_SIM_OPERATOR, it) }
            runCatching { tm.simOperatorName }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_SIM_OPERATOR_NAME, it) }
            runCatching { tm.simCountryIso }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_SIM_COUNTRY, it) }
            runCatching { tm.networkOperatorName }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_CARRIER, it) }
            runCatching { tm.line1Number }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_PHONE_NUMBER, it) }
            runCatching { tm.subscriberId }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_SIM_SUBSCRIBER, it) }
            runCatching { tm.simSerialNumber }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_ICCID, it) }
            runCatching { tm.deviceSoftwareVersion }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_BUILD_RADIO, it) }
            runCatching { tm.imei }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_IMEI, it) }
            runCatching { tm.meid }?.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_FAKE_MEID, it) }
        }

        
        runCatching {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
                as? android.net.wifi.WifiManager
            val info = wm?.connectionInfo
            info?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
                ?.let { tpl.set(XpConfig.KEY_FAKE_WIFI_SSID, it) }
            info?.bssid?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
                ?.let { tpl.set(XpConfig.KEY_FAKE_WIFI_BSSID, it) }
            info?.macAddress?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
                ?.let { tpl.set(XpConfig.KEY_FAKE_WIFI_MAC, it) }
        }
        runCatching {
            val ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            ba?.address?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
                ?.let { tpl.set(XpConfig.KEY_FAKE_BT_MAC, it) }
        }

        
        tpl.set(XpConfig.KEY_FAKE_TIMEZONE, java.util.TimeZone.getDefault().id)
        tpl.set(
            XpConfig.KEY_FAKE_LOCALE,
            "${Locale.getDefault().language}_${Locale.getDefault().country}"
        )
        tpl.set(XpConfig.KEY_FAKE_SDK_INT, Build.VERSION.SDK_INT.toString())
        runCatching {
            val name = Settings.Global.getString(context.contentResolver, "device_name")
            if (!name.isNullOrBlank()) tpl.set(XpConfig.KEY_DEVICE_NAME, name)
        }

        
        if (tpl.get(XpConfig.KEY_BUILD_RADIO).isBlank()) {
            nat["prop.baseband"]?.takeIf { it.isNotBlank() }
                ?.let { tpl.set(XpConfig.KEY_BUILD_RADIO, it) }
        }

        return tpl
    }

    

    private fun buildField(name: String, owner: String): String? {
        return runCatching {
            val cls = Class.forName(owner)
            val f = cls.getDeclaredField(name)
            f.isAccessible = true
            when (val v = f.get(null)) {
                is String -> v
                is Int -> v.toString()
                else -> v?.toString()
            }
        }.getOrNull()?.takeIf { it.isNotBlank() && it != "unknown" && it != "UNKNOWN" }
    }

    private fun readProc(path: String): String? = runCatching {
        java.io.FileInputStream(path).use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()

    



    private fun eglRenderer(): String? {
        val dpy = android.opengl.EGL14.eglGetDisplay(android.opengl.EGL14.EGL_DEFAULT_DISPLAY)
        if (dpy == null || dpy == android.opengl.EGL14.EGL_NO_DISPLAY) return null
        val ver = IntArray(2)
        if (!android.opengl.EGL14.eglInitialize(dpy, ver, 0, ver, 1)) return null
        return try {
            
            android.opengl.EGL14.eglQueryString(dpy, 0x305D)
        } finally {
            runCatching { android.opengl.EGL14.eglTerminate(dpy) }
        }
    }

    
    
    
    
    
    

    private const val EGL_SURFACE_TYPE = 0x3033
    private const val EGL_PBUFFER_BIT = 0x0001
    private const val EGL_RENDERABLE_TYPE = 0x3040
    private const val EGL_OPENGL_ES2_BIT = 0x0004
    private const val EGL_OPENGL_ES3_BIT = 0x0040
    private const val EGL_RED_SIZE = 0x3024
    private const val EGL_GREEN_SIZE = 0x3023
    private const val EGL_BLUE_SIZE = 0x3022
    private const val EGL_ALPHA_SIZE = 0x3021
    private const val EGL_NONE = 0x3038
    private const val EGL_WIDTH = 0x3057
    private const val EGL_HEIGHT = 0x3056
    private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098

    private const val EGL_VENDOR = 0x3053
    private const val GL_VENDOR = 0x1F00
    private const val GL_RENDERER = 0x1F01
    private const val GL_VERSION = 0x1F02
    private const val GL_SHADING_LANGUAGE_VERSION = 0x8B8C
    private const val GL_MAX_TEXTURE_SIZE = 0x0D33
    private const val GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C
    private const val GL_MAX_ARRAY_TEXTURE_LAYERS = 0x88FF
    private const val GL_MAX_PUSH_CONSTANTS = 0x8869 

    private fun gpuProbe(): Map<String, String> {
        val egl = android.opengl.EGL14.eglGetDisplay(android.opengl.EGL14.EGL_DEFAULT_DISPLAY)
        if (egl == null || egl == android.opengl.EGL14.EGL_NO_DISPLAY) return emptyMap()
        val ver = IntArray(2)
        if (!android.opengl.EGL14.eglInitialize(egl, ver, 0, ver, 1)) return emptyMap()

        val cfgAttribs = intArrayOf(
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8,
            EGL_NONE
        )
        val cfgs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val numCfg = IntArray(1)
        if (!android.opengl.EGL14.eglChooseConfig(egl, cfgAttribs, 0, cfgs, 0, 1, numCfg, 0) ||
            numCfg[0] <= 0
        ) {
            
            val attr2 = intArrayOf(
                EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8,
                EGL_NONE
            )
            if (!android.opengl.EGL14.eglChooseConfig(egl, attr2, 0, cfgs, 0, 1, numCfg, 0) ||
                numCfg[0] <= 0
            ) {
                android.opengl.EGL14.eglTerminate(egl)
                return emptyMap()
            }
        }
        val cfg = cfgs[0] ?: run {
            android.opengl.EGL14.eglTerminate(egl)
            return emptyMap()
        }

        val pbuf = intArrayOf(EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE)
        val surf = android.opengl.EGL14.eglCreatePbufferSurface(egl, cfg, pbuf, 0)
        if (surf == null || surf == android.opengl.EGL14.EGL_NO_SURFACE) {
            android.opengl.EGL14.eglTerminate(egl)
            return emptyMap()
        }
        val ctxAttr = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE)
        var ctx = android.opengl.EGL14.eglCreateContext(
            egl, cfg, android.opengl.EGL14.EGL_NO_CONTEXT, ctxAttr, 0
        )
        if (ctx == null || ctx == android.opengl.EGL14.EGL_NO_CONTEXT) {
            val attr2 = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE)
            ctx = android.opengl.EGL14.eglCreateContext(
                egl, cfg, android.opengl.EGL14.EGL_NO_CONTEXT, attr2, 0
            )
        }
        if (ctx == null || ctx == android.opengl.EGL14.EGL_NO_CONTEXT) {
            android.opengl.EGL14.eglDestroySurface(egl, surf)
            android.opengl.EGL14.eglTerminate(egl)
            return emptyMap()
        }

        val out = LinkedHashMap<String, String>()
        try {
            if (!android.opengl.EGL14.eglMakeCurrent(egl, surf, surf, ctx)) return emptyMap()
            out["renderer"] = android.opengl.GLES20.glGetString(GL_RENDERER).orEmpty()
            out["vendor"] = android.opengl.GLES20.glGetString(GL_VENDOR).orEmpty()
            out["glversion"] = android.opengl.GLES20.glGetString(GL_VERSION).orEmpty()
            out["glsl"] = android.opengl.GLES20.glGetString(GL_SHADING_LANGUAGE_VERSION).orEmpty()
            out["maxtex"] = glInt(GL_MAX_TEXTURE_SIZE).toString()
            out["maxcube"] = glInt(GL_MAX_CUBE_MAP_TEXTURE_SIZE).toString()
            out["layers"] = glInt(GL_MAX_ARRAY_TEXTURE_LAYERS).toString()
            out["push"] = glInt(GL_MAX_PUSH_CONSTANTS).toString()
            
            out["eglvendor"] = android.opengl.EGL14.eglQueryString(egl, EGL_VENDOR).orEmpty()
        } catch (_: Throwable) {
            
        } finally {
            runCatching {
                android.opengl.EGL14.eglMakeCurrent(
                    egl, android.opengl.EGL14.EGL_NO_SURFACE,
                    android.opengl.EGL14.EGL_NO_SURFACE, android.opengl.EGL14.EGL_NO_CONTEXT
                )
            }
            runCatching { android.opengl.EGL14.eglDestroySurface(egl, surf) }
            runCatching { android.opengl.EGL14.eglDestroyContext(egl, ctx) }
            runCatching { android.opengl.EGL14.eglTerminate(egl) }
        }
        return out.filterValues { it.isNotBlank() && it != "0" }
    }

    private fun glInt(pname: Int): Int {
        val v = IntArray(1)
        android.opengl.GLES20.glGetIntegerv(pname, v, 0)
        return v[0]
    }

    



    private fun networkEnv(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        runCatching {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            val names = ArrayList<String>()
            val addrs = ArrayList<String>()
            while (ifaces.hasMoreElements()) {
                val ni = ifaces.nextElement() ?: continue
                names.add(ni.name)
                for (a in ni.inetAddresses) {
                    val s = a.hostAddress ?: continue
                    if (s.contains(":")) continue 
                    addrs.add("${ni.name}=$s")
                }
            }
            out["net.ifaces"] = names.filter { it != "lo" }.joinToString(",")
            out["net.addrs"] = addrs.joinToString(",")
        }
        return out
    }

    
    private fun systemEnv(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        runCatching {
            val env = System.getenv()
            listOf("PATH", "ANDROID_ROOT", "ANDROID_DATA", "ANDROID_STORAGE", "EXTERNAL_STORAGE",
                "ASEC_MOUNTPOINT", "LOOP_MOUNTPOINT", "BOOTCLASSPATH", "TMPDIR")
                .forEach { k ->
                    val v = env[k]
                    if (!v.isNullOrBlank()) out["env.$k"] = v
                }
        }
        return out
    }
}
