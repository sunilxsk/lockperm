package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import java.util.Locale













internal class MemoryStorageSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg = snapshot()
        if (!cfg.enableBuild || !cfg.mem2Enable) {
            logInfo("memory/storage spoof skipped (build=${cfg.enableBuild} on=${cfg.mem2Enable})")
            return
        }
        var n = 0
        n += hookMemoryInfo()
        n += hookStatFs()
        pushMemInfo()
        logInfo("memory/storage spoof installed (hooks=$n)")
    }

    

    private fun memTotalBytes(): Long =
        snapshot().memTotalMb.coerceIn(256, 262144) * 1024L * 1024L

    private fun memAvailBytes(): Long {
        val c = snapshot()
        return c.memAvailMb.coerceIn(0, c.memTotalMb.coerceAtLeast(1)) * 1024L * 1024L
    }

    private fun storTotalBytes(): Long =
        snapshot().storTotalGb.coerceIn(1, 8192) * 1024L * 1024L * 1024L

    private fun storAvailBytes(): Long {
        val c = snapshot()
        return c.storAvailGb.coerceIn(0, c.storTotalGb.coerceAtLeast(1)) * 1024L * 1024L * 1024L
    }

    

    private fun hookMemoryInfo(): Int {
        val am = loadClassAnywhere("android.app.ActivityManager") ?: return 0
        val mi = loadClassAnywhere("android.app.ActivityManager\$MemoryInfo") ?: return 0
        val fTotal = field(mi, "totalMem") ?: return 0
        val fAvail = field(mi, "availMem")
        val fThres = field(mi, "threshold")
        val fLow = field(mi, "lowMemory")
        var n = 0
        am.declaredMethods.filter {
            it.name == "getMemoryInfo" && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0].name == mi.name
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    runCatching {
                        val self = chain.getArg(0) ?: return@runCatching
                        setLong(fTotal, self, memTotalBytes())
                        fAvail?.let { setLong(it, self, memAvailBytes()) }
                        
                        
                        fThres?.let { setLong(it, self, (memTotalBytes() * 0.15).toLong()) }
                        fLow?.let { setBool(it, self, false) }
                    }
                    r
                }) n++
        }
        return n
    }

    

    private fun hookStatFs(): Int {
        val sf = loadClassAnywhere("android.os.StatFs") ?: return 0
        val bs = 4096L
        var n = 0
        sf.declaredMethods.filter { it.parameterTypes.isEmpty() }.forEach { m ->
            val name = m.name
            val want = when (name) {
                "getBlockSize", "getBlockSizeLong",
                "getBlockCount", "getBlockCountLong",
                "getFreeBlocks", "getFreeBlocksLong",
                "getAvailableBlocks", "getAvailableBlocksLong",
                "getTotalBytes", "getFreeBytes", "getAvailableBytes" -> true
                else -> false
            }
            if (!want) return@forEach
            if (hookMethod(m) { _ ->
                    when (name) {
                        "getBlockSize" -> bs.toInt()
                        "getBlockSizeLong" -> bs
                        "getBlockCount" -> (storTotalBytes() / bs).toInt()
                        "getBlockCountLong" -> storTotalBytes() / bs
                        "getFreeBlocks" -> (storAvailBytes() / bs).toInt()
                        "getFreeBlocksLong" -> storAvailBytes() / bs
                        "getAvailableBlocks" -> (storAvailBytes() / bs).toInt()
                        "getAvailableBlocksLong" -> storAvailBytes() / bs
                        "getTotalBytes" -> storTotalBytes()
                        "getFreeBytes" -> storAvailBytes()
                        else -> storAvailBytes()
                    }
                }) n++
        }
        return n
    }

    

    private fun pushMemInfo() {
        FakeFiles.setExtra("/proc/meminfo", memInfoText(snapshot()))
    }

    companion object {
        







        fun memInfoText(c: XpState.Snapshot): String {
            val totalKb = c.memTotalMb.coerceIn(256, 262144) * 1024L
            val availKb = c.memAvailMb.coerceIn(0, c.memTotalMb.coerceAtLeast(1)) * 1024L
            return buildString {
                append("MemTotal:        ").append(totalKb).append(" kB\n")
                append("MemFree:         ").append(availKb * 3 / 4).append(" kB\n")
                append("MemAvailable:    ").append(availKb).append(" kB\n")
                append("Buffers:         ").append(totalKb / 40).append(" kB\n")
                append("Cached:          ").append(availKb / 2).append(" kB\n")
                append("SwapCached:         0 kB\n")
                append("Active:          ").append(totalKb / 3).append(" kB\n")
                append("Inactive:        ").append(totalKb / 4).append(" kB\n")
                append("SwapTotal:       ").append(totalKb / 2).append(" kB\n")
                append("SwapFree:        ").append(totalKb / 2).append(" kB\n")
                append("Dirty:                0 kB\n")
                append("Writeback:             0 kB\n")
                append("AnonPages:       ").append(totalKb / 3).append(" kB\n")
                append("Mapped:          ").append(totalKb / 5).append(" kB\n")
                append("Shmem:           ").append(totalKb / 100).append(" kB\n")
                append("KernelStack:     ").append(totalKb / 200).append(" kB\n")
                append(String.format(Locale.US, "CmaTotal:        %d kB\n", totalKb / 16))
                append(String.format(Locale.US, "CmaFree:         %d kB\n", totalKb / 32))
            }
        }
    }

    

    private fun field(clazz: Class<*>, name: String): java.lang.reflect.Field? =
        runCatching { clazz.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()

    private fun setLong(f: java.lang.reflect.Field, obj: Any, v: Long) {
        runCatching { clearFinal(f); f.setLong(obj, v) }
    }

    private fun setBool(f: java.lang.reflect.Field, obj: Any, v: Boolean) {
        runCatching { clearFinal(f); f.setBoolean(obj, v) }
    }

    private fun clearFinal(f: java.lang.reflect.Field) {
        runCatching {
            val m = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
            m.isAccessible = true
            m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        }.onFailure {
            runCatching {
                val m = java.lang.reflect.Field::class.java.getDeclaredField("accessFlags")
                m.isAccessible = true
                m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
            }
        }
    }
}
