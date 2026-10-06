package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule











internal class ShellSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val SHELL = "/system/bin/sh"
    private val bypass: ThreadLocal<Boolean> = ThreadLocal.withInitial<Boolean> { false }

    private val ID_ROOT =
        "uid=0(root) gid=0(root) groups=0(root),1004(input),1007(log),1015(sdcard_rw)," +
                "3003(inet),3006(net_bw_stats),3009(readproc) context=u:r:su:s0"

    private val LS_ROOT = listOf(
        "acct", "apex", "bin", "cache", "config", "d", "data", "debug_ramdisk", "dev",
        "etc", "linkerconfig", "metadata", "mnt", "odm", "oem", "postinstall", "proc",
        "product", "res", "root", "sbin", "sdcard", "storage", "system", "system_dlkm",
        "system_ext", "tmp", "vendor", "vendor_dlkm",
    )
    private val LS_SYSTEM = listOf(
        "app", "bin", "build.prop", "etc", "fonts", "framework", "lib", "lib64",
        "lost+found", "media", "priv-app", "product", "system_ext", "usr", "vendor", "xbin",
    )
    private val LS_DATA = listOf(
        "adb", "anr", "app", "backup", "bootchart", "cache", "dalvik-cache", "data",
        "local", "log", "media", "misc", "nfc", "ota", "property", "system", "tombstones",
        "user", "vendor",
    )

    private val SU_PATHS = setOf(
        "/system/bin/su", "/system/xbin/su", "/system/bin/su.bin", "/system/xbin/su.bin",
        "/sbin/su", "/system/su", "/su/bin/su", "/su/bin/su.bin",
        "/system/bin/.ext/su", "/system/xbin/daemonsu", "/system/xbin/ext/su",
        "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
        "/system/app/Superuser.apk", "/system/app/Superuser/Superuser.apk",
        "/system/bin/failsafe/su", "/system/sd/xbin/su",
    )

    fun install() {
        
        
        
        val cfg = snapshot()
        val nothing = FakeProps.build(cfg).isEmpty() && !cfg.rootFakeEnable
        if (nothing) {
            logInfo("shell spoofer skipped (nothing to spoof)")
            return
        }
        hookRuntimeExec()
        hookProcessBuilder()
        logInfo("shell spoofer installed")
    }

    private fun spawn(script: String): java.lang.Process? {
        return runCatching {
            bypass.set(true)
            try {
                java.lang.ProcessBuilder(listOf(SHELL, "-c", script)).start()
            } finally {
                bypass.set(false)
            }
        }.getOrNull()
    }

    private fun tokensOf(arg: Any?): List<String>? = when (arg) {
        is String -> arg.split(" ").filter { it.isNotEmpty() }
        is Array<*> -> arg.map { it?.toString() ?: "" }.filter { it.isNotEmpty() }
        is List<*> -> arg.map { it?.toString() ?: "" }.filter { it.isNotEmpty() }
        else -> null
    }

    private fun commandOf(self: Any?): List<String>? {
        if (self == null) return null
        return runCatching {
            val m = self.javaClass.getDeclaredMethod("command")
            m.isAccessible = true
            val v = m.invoke(self)
            @Suppress("UNCHECKED_CAST")
            (v as? List<String>)?.map { it }
        }.getOrNull()
    }

    

    private fun hookRuntimeExec() {
        val rt = loadClassAnywhere("java.lang.Runtime") ?: return
        rt.declaredMethods.filter { it.name == "exec" }.forEach { m ->
            hookMethod(m) { chain ->
                if (bypass.get() == true) {
                    chain.proceed()
                } else {
                    val toks = tokensOf(chain.getArg(0))
                    val script = if (toks == null) null else decide(toks)
                    if (script == null) chain.proceed() else spawn(script) ?: chain.proceed()
                }
            }
        }
    }

    private fun hookProcessBuilder() {
        val pb = loadClassAnywhere("java.lang.ProcessBuilder") ?: return
        pb.declaredMethods.filter { it.name == "start" }.forEach { m ->
            hookMethod(m) { chain ->
                if (bypass.get() == true) {
                    chain.proceed()
                } else {
                    val list = commandOf(chain.getThisObject())
                    val script = if (list == null) null else decide(list)
                    if (script == null) chain.proceed() else spawn(script) ?: chain.proceed()
                }
            }
        }
    }

    private fun base(t: String): String = t.substringAfterLast('/')

    private fun decide(tokens: List<String>): String? {
        if (tokens.isEmpty()) return null
        val cfg = snapshot()

        val gi = tokens.indexOfFirst { base(it) == "getprop" }
        if (gi >= 0) return getpropScript(tokens, gi, cfg)

        val ui = tokens.indexOfFirst { base(it) == "uname" }
        if (ui >= 0) return unameScript(tokens, ui, cfg)

        val ci = tokens.indexOfFirst { base(it) == "cat" }
        if (ci >= 0) return catScript(tokens, ci, cfg)

        
        
        val dfi = tokens.indexOfFirst { base(it) == "df" }
        if (dfi >= 0) {
            dfScript(tokens, cfg)?.let { return it }
        }
        val dui = tokens.indexOfFirst { base(it) == "du" }
        if (dui >= 0) {
            duScript(tokens, cfg)?.let { return it }
        }

        if (cfg.rootFakeEnable) return rootScript(tokens, cfg)
        return null
    }

    

    private fun getpropScript(tokens: List<String>, idx: Int, cfg: XpState.Snapshot): String? {
        val props = FakeProps.build(cfg)
        if (props.isEmpty()) return null

        val key = tokens.drop(idx + 1).firstOrNull { !it.startsWith("-") }
        if (!key.isNullOrEmpty()) {
            val v = props[key] ?: return null
            return printfLines(listOf("[$key]: [$v]"))
        }
        
        
        
        val lines = props.map { (k, v) -> "[$k]: [$v]" }
        val pattern = props.keys.joinToString("|") { it.replace(".", "\\.") }
        return "{ getprop 2>/dev/null | grep -v -E '^\\[($pattern)\\]:'; ${printfLines(lines)}; }"
    }

    

    private val UNAME_WORDS = mapOf(
        "all" to 'a', "kernel-name" to 's', "sysname" to 's',
        "nodename" to 'n', "kernel-release" to 'r', "release" to 'r',
        "kernel-version" to 'v', "machine" to 'm',
        "processor" to 'p', "hardware-platform" to 'i', "operating-system" to 'o',
    )

    private fun unameScript(tokens: List<String>, idx: Int, cfg: XpState.Snapshot): String? {
        val flags = LinkedHashSet<Char>()
        tokens.drop(idx + 1).forEach { a ->
            if (!a.startsWith("-")) return@forEach
            val body = a.trimStart('-')
            if (body.isEmpty()) return@forEach
            val w = UNAME_WORDS[body]
            if (w != null) {
                flags.add(w)
            } else {
                body.forEach { if (it in "asnrvmipo") flags.add(it) }
            }
        }
        val out = FakeProps.uname(cfg, flags) ?: return null
        return printfLines(listOf(out))
    }

    

    private fun catScript(tokens: List<String>, idx: Int, cfg: XpState.Snapshot): String? {
        val path = tokens.drop(idx + 1).firstOrNull { !it.startsWith("-") }
        if (path.isNullOrEmpty()) return null
        val content = procContent(path, cfg)
        if (content != null) return printfLines(content.split("\n"))
        return if (cfg.rootFakeEnable && cfg.rootFakeMask) maskCmd(tokens.joinToString(" ")) else null
    }

    private fun procContent(path: String, cfg: XpState.Snapshot): String? {
        FakeFiles.content(path)?.let { return it }
        return when (path) {
            
            
            "/proc/sys/kernel/osrelease" -> FakeProps.kernelVersion(cfg).ifEmpty { null }
            "/proc/sys/kernel/ostype" -> "Linux"
            "/proc/sys/kernel/version" ->
                FakeProps.procVersion(cfg)
                    .takeIf { FakeProps.kernelVersion(cfg).isNotEmpty() }
            "/proc/sys/kernel/arch" -> FakeProps.arch(cfg).ifEmpty { null }
            else -> null
        }
    }

    

    private fun dfScript(tokens: List<String>, cfg: XpState.Snapshot): String? {
        if (!cfg.enableBuild || !cfg.mem2Enable) return null
        val totalKb = cfg.storTotalGb.coerceIn(1, 8192) * 1024L * 1024L
        val availKb = cfg.storAvailGb.coerceIn(0, cfg.storTotalGb.coerceAtLeast(1)) * 1024L * 1024L
        val usedKb = (totalKb - availKb).coerceAtLeast(0L)
        val usedPct = if (totalKb > 0) usedKb * 100L / totalKb else 0L
        val memKb = cfg.memTotalMb.coerceIn(256, 262144) * 1024L
        
        
        return printfLines(
            listOf(
                "Filesystem           1K-blocks      Used Available Use% Mounted on",
                "/dev/block/dm-8      $totalKb $usedKb $availKb ${usedPct}% /data",
                "/dev/fuse            $totalKb $usedKb $availKb ${usedPct}% /storage/emulated",
                "tmpfs                $memKb ${memKb / 8} ${memKb * 7 / 8} 12% /dev",
            )
        )
    }

    private fun duScript(tokens: List<String>, cfg: XpState.Snapshot): String? {
        if (!cfg.enableBuild || !cfg.mem2Enable) return null
        val human = tokens.any { it.startsWith("-") && it.contains("h") }
        val path = tokens.drop(1).firstOrNull { !it.startsWith("-") } ?: return null
        val totalKb = cfg.storTotalGb.coerceIn(1, 8192) * 1024L * 1024L
        val availKb = cfg.storAvailGb.coerceIn(0, cfg.storTotalGb.coerceAtLeast(1)) * 1024L * 1024L
        val usedKb = (totalKb - availKb).coerceAtLeast(0L)
        val text = if (human) "${usedKb / 1024L / 1024L}G\t$path" else "$usedKb\t$path"
        return printfLines(listOf(text))
    }

    

    private fun rootScript(tokens: List<String>, cfg: XpState.Snapshot): String? {
        val mask = cfg.rootFakeMask
        val bin = base(tokens[0])
        val joined = tokens.joinToString(" ")

        return when {
            bin == "su" || bin == "sudo" || bin == "su.bin" || bin == "daemonsu" -> {
                val i = tokens.indexOf("-c")
                val inner = if (i >= 0 && i + 1 < tokens.size) {
                    tokens.subList(i + 1, tokens.size).joinToString(" ")
                } else {
                    ""
                }
                scriptForInner(inner, mask)
            }

            bin == "id" || bin == "whoami" ->
                if (bin == "whoami") printfLines(listOf("root")) else printfLines(listOf(ID_ROOT))

            bin == "ls" -> lsScript(tokens.getOrNull(1), mask, joined)

            bin == "which" -> {
                val what = tokens.getOrNull(1).orEmpty()
                if (what == "su" || what == "sudo") printfLines(listOf("/system/bin/su"))
                else if (mask) maskCmd(joined) else null
            }

            bin == "getprop" -> {
                val key = tokens.getOrNull(1).orEmpty()
                when (key) {
                    "ro.secure" -> printfLines(listOf("0"))
                    "ro.debuggable" -> printfLines(listOf("0"))
                    else -> null
                }
            }

            mask && looksLikeProbe(joined) -> maskCmd(joined)

            else -> null
        }
    }

    private fun scriptForInner(inner: String, mask: Boolean): String {
        val t = inner.trim()
        return when {
            t.isEmpty() -> ":"
            t.startsWith("id") -> printfLines(listOf(ID_ROOT))
            t.startsWith("whoami") -> printfLines(listOf("root"))
            t.startsWith("ls") -> lsScript(
                t.removePrefix("ls").trim().ifEmpty { "/" }, mask, t
            ) ?: ":"

            t.startsWith("getprop") -> {
                val key = t.removePrefix("getprop").trim()
                when (key) {
                    "ro.secure" -> printfLines(listOf("0"))
                    "ro.debuggable" -> printfLines(listOf("0"))
                    else -> ":"
                }
            }

            mask -> maskCmd(t)
            else -> ":"
        }
    }

    private fun lsScript(target: String?, mask: Boolean, joined: String): String? {
        val p = (target ?: "").trim().trimEnd('/')
        return when {
            p.isEmpty() || p == "/" -> printfLines(LS_ROOT)
            p == "/system" || p == "/system_ext" || p == "/vendor" -> printfLines(LS_SYSTEM)
            p == "/data" -> printfLines(LS_DATA)
            mask -> maskCmd(joined)
            else -> null
        }
    }

    private fun looksLikeProbe(cmd: String): Boolean {
        val c = cmd.lowercase()
        return c.contains("su ") || c.endsWith("su") || c.contains("magisk") ||
                c.contains("busybox") || c.contains(" /data/") || c.contains(" /system/") ||
                c.startsWith("mount") || c.startsWith("ps ") ||
                c.startsWith("test ") || c.startsWith("[ ")
    }

    private fun maskCmd(cmd: String): String = "($cmd) 2>/dev/null; exit 0"

    private fun printfLines(lines: List<String>): String =
        "printf '%s\\n' " + lines.joinToString(" ") { q(it) }

    
    private fun q(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    
    fun isSuPath(p: String): Boolean {
        if (p in SU_PATHS) return true
        val l = p.lowercase()
        return SU_PATHS.any { it.lowercase() == l } || l.endsWith("/su") || l.endsWith("/su.bin")
    }
}
