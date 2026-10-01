package io.github.sunilxsk.lockperm

import android.content.Intent
import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule

















internal class WirelessDebuggingDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val TOGGLE_METHODS = setOf(
        "isAdbWifiEnabled", "isAdbWifiSupported", "isAdbEnabled",
        "enableAdbWireless", "disableAdbWireless", "setAdbEnabled",
    )

    
    private val PAIR_METHODS = setOf(
        "pair", "unpair", "connect", "disconnect", "connectByPairedDevice",
        "getPairedDevices", "getConnectedDevices", "getPairedDevice",
        "startPairing", "cancelPairing", "getPairingState",
    )

    
    private val ADB_SERVICE_TYPES = listOf("_adb-tls-connect", "_adb-tls-pairing", "adb-tls")

    
    private val ADB_SETTINGS_KEYS = setOf(
        "adb_wifi_enabled", "adb_enabled", "adb_wifi_supported",
    )

    
    private val ADB_PROP_HINTS = listOf("persist.adb", "adb.", "service.adb", "init.svc.adbd")

    fun installNow() {
        XpState.Flags.forceWdbg = true
        install()
    }

    private fun on(): Boolean =
        XpState.Flags.forceWdbg || snapshot().wdbgEnable

    fun install() {
        if (!on()) return
        hookAdbManager()
        hookAdbBinder()
        hookSettingsGlobal()
        hookSystemProperties()
        hookNsd()
        hookJump()
        logInfo("wireless debugging defender installed")
    }

    private fun on(value: Boolean): Boolean = snapshot().wdbgEnable && value

    

    private fun hookAdbManager() {
        val am = frameworkCls("android.debug.AdbManager")
        if (am == null) {
            logWarn("wdbg: AdbManager not found (系统隐藏类，某些 ROM 上取不到)")
            return
        }
        var n = 0
        am.declaredMethods.forEach { m ->
            when {
                m.name in TOGGLE_METHODS && on(snapshot().wdbgToggle) -> {
                    hookMethod(m) { chain ->
                        logWarn("blocked AdbManager.${m.name}")
                        deniedFor(m)
                    }
                    n++
                }
                m.name in PAIR_METHODS && on(snapshot().wdbgPair) -> {
                    hookMethod(m) { chain ->
                        logWarn("blocked AdbManager.${m.name}")
                        deniedFor(m)
                    }
                    n++
                }
            }
        }
        if (n > 0) logInfo("wdbg: AdbManager hooked x$n")
    }

    

    



    private fun hookAdbBinder() {
        val names = listOf(
            "android.debug.IAdbManager\$Stub\$Proxy",
            "android.debug.IAdbManager\$Stub",
            "android.debug.IAdbManager",
        )
        var n = 0
        names.forEach { name ->
            val c = frameworkCls(name) ?: return@forEach
            c.declaredMethods.forEach { m ->
                when {
                    m.name in TOGGLE_METHODS && on(snapshot().wdbgToggle) -> {
                        hookMethod(m) { chain ->
                            logWarn("blocked IAdbManager.${m.name}")
                            deniedFor(m)
                        }
                        n++
                    }
                    m.name in PAIR_METHODS && on(snapshot().wdbgPair) -> {
                        hookMethod(m) { chain ->
                            logWarn("blocked IAdbManager.${m.name}")
                            deniedFor(m)
                        }
                        n++
                    }
                }
            }
        }
        if (n > 0) logInfo("wdbg: IAdbManager hooked x$n")
    }

    

    private fun hookSettingsGlobal() {
        if (!on(snapshot().wdbgToggle)) return
        listOf(
            "android.provider.Settings\$Global",
            "android.provider.Settings\$Secure",
        ).forEach { name ->
            val c = frameworkCls(name) ?: return@forEach
            c.declaredMethods.filter { it.name in SETTINGS_RW }.forEach { m ->
                hookMethod(m) { chain ->
                    val key = runCatching { chain.getArg(1)?.toString() }.getOrNull()
                    if (key != null && key in ADB_SETTINGS_KEYS) {
                        logWarn("blocked Settings ${m.name}($key)")
                        return@hookMethod deniedFor(m)
                    }
                    chain.proceed()
                }
            }
        }
    }

    

    private fun hookSystemProperties() {
        if (!on(snapshot().wdbgProp)) return
        val sp = frameworkCls("android.os.SystemProperties") ?: return
        sp.declaredMethods.filter { it.name in setOf("get", "getBoolean", "getInt", "set") }
            .forEach { m ->
                hookMethod(m) { chain ->
                    val key = runCatching { chain.getArg(0)?.toString() }.getOrNull()
                    if (key != null && isAdbProp(key)) {
                        logWarn("blocked SystemProperties.${m.name}($key)")
                        return@hookMethod deniedFor(m)
                    }
                    chain.proceed()
                }
            }
        logInfo("wdbg: SystemProperties hooked")
    }

    private fun isAdbProp(key: String): Boolean {
        val k = key.lowercase()
        return ADB_PROP_HINTS.any { k.startsWith(it) } || (k.contains("adb") && k.contains("wifi"))
    }

    

    private fun hookNsd() {
        if (!on(snapshot().wdbgDiscover)) return
        val nsd = frameworkCls("android.net.nsd.NsdManager")
        if (nsd == null) {
            logWarn("wdbg: NsdManager not found")
            return
        }
        nsd.declaredMethods.filter {
            it.name == "discoverServices" || it.name == "resolveService" ||
                    it.name == "registerService"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (argsMentionAdb(chain)) {
                    logWarn("blocked NsdManager.${m.name} (adb service)")
                    return@hookMethod deniedFor(m)
                }
                chain.proceed()
            }
        }
        logInfo("wdbg: NsdManager hooked")
    }

    private fun argsMentionAdb(chain: io.github.libxposed.api.XposedInterface.Chain): Boolean {
        val args = runCatching { chain.args }.getOrNull() ?: return false
        return args.any { a ->
            val s = a?.toString()?.lowercase() ?: return@any false
            ADB_SERVICE_TYPES.any { s.contains(it) } ||
                    (s.contains("adb") && (s.contains("tcp") || s.contains("tls")))
        }
    }

    

    










    private fun hookJump() {
        val blockJump = on(snapshot().wdbgBlockJump)
        val blockSettings = on(snapshot().wdbgBlockSettings)
        if (!blockJump && !blockSettings) return

        val patterns = JUMP_PATTERNS

        hookJumpOn("android.app.Activity", patterns, true)
        hookJumpOn("android.app.ContextImpl", patterns, false)
        hookJumpOn("android.content.ContextWrapper", patterns, false)

        
        val inst = frameworkCls("android.app.Instrumentation")
        if (inst != null) {
            inst.declaredMethods.filter { it.name == "execStartActivity" }.forEach { m ->
                hookMethod(m) { chain ->
                    val intent = runCatching { chain.getArg(2) as? android.content.Intent }.getOrNull()
                    if (intent != null && matches(intent, patterns)) {
                        logWarn("blocked jump (Instrumentation): $intent")
                        return@hookMethod null
                    }
                    chain.proceed()
                }
            }
        }
        logInfo("wdbg: jump hook installed (jump=$blockJump, settings=$blockSettings)")
    }

    private fun hookJumpOn(className: String, patterns: List<Pair<String, String>>, self: Boolean) {
        val c = frameworkCls(className) ?: return
        c.declaredMethods.filter { it.name in START_METHODS }.forEach { m ->
            hookMethod(m) { chain ->
                val intent = findIntent(chain)
                if (intent != null && matches(intent, patterns)) {
                    logWarn("blocked jump (${m.name}): $intent")
                    return@hookMethod null
                }
                chain.proceed()
            }
        }
    }

    
    private fun findIntent(chain: io.github.libxposed.api.XposedInterface.Chain): android.content.Intent? {
        val args = runCatching { chain.args }.getOrNull() ?: return null
        args.filterIsInstance<android.content.Intent>().firstOrNull()?.let { return it }
        val arr = args.firstOrNull { it is Array<*> } as? Array<*> ?: return null
        return arr.filterIsInstance<android.content.Intent>().firstOrNull()
    }

    private fun matches(intent: android.content.Intent, patterns: List<Pair<String, String>>): Boolean {
        val cfg = snapshot()
        val blockJump = on(cfg.wdbgBlockJump)
        val blockSettings = on(cfg.wdbgBlockSettings)
        if (!blockJump && !blockSettings) return false

        val text = buildString {
            append(intent.action ?: "")
            append(' ')
            append(intent.component?.className ?: "")
            append(' ')
            append(intent.component?.packageName ?: "")
            append(' ')
            append(intent.dataString ?: "")
        }.lowercase()

        
        val isSettings = text.contains("com.android.settings") ||
                text.contains("settings\$") ||
                text.contains("android.settings.")

        if (blockSettings && isSettings) return true

        
        if (blockJump) {
            val list = if (blockSettings) patterns else patterns.filter { it.first != "设置" }
            for ((_, regex) in list) {
                if (regex.split("|").any { it.isNotBlank() && text.contains(it.lowercase()) }) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private val SETTINGS_RW = setOf("getString", "putString", "getInt", "putInt", "getLong", "putLong")

        
        private val START_METHODS = setOf(
            "startActivity", "startActivityForResult", "startActivities",
            "startActivityIfNeeded", "startActivityFromChild",
        )

        



        private val JUMP_PATTERNS: List<Pair<String, String>> =
            XpConfig.WDBG_JUMP_TARGETS + ("设置" to "com.android.settings|android.settings.")
    }
}
