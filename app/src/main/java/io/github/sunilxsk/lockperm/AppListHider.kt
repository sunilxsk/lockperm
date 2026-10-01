package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule












internal class AppListHider(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val METHODS = setOf(
        "getInstalledApplications",
        "getInstalledPackages",
    )

    fun installNow() {
        XpState.Flags.forceHideApps = true
        install()
    }

    private fun on(): Boolean =
        XpState.Flags.forceHideApps || snapshot().hideAppsEnable

    fun install() {
        if (!on()) return

        listOf(
            "android.app.ApplicationPackageManager",
            "android.content.pm.PackageManager",
        ).forEach { name ->
            val c = cls(name) ?: return@forEach
            c.declaredMethods.filter { it.name in METHODS }.forEach { m ->
                hookMethod(m) { chain ->
                    val result = chain.proceed()
                    filter(result)
                }
            }
        }
        logInfo("app list hider installed")
    }

    


    private fun filter(result: Any?): Any? {
        val cfg = snapshot()
        if (!cfg.hideAppsEnable) return result
        val list = result as? List<*> ?: return result

        
        if (cfg.hideAppsMode == 0) {
            logWarn("hide apps: return empty list")
            return emptyList<Any>()
        }

        val names = cfg.hideAppsList
        val whitelist = cfg.hideAppsListMode == 0
        if (names.isEmpty() && whitelist) {
            
            return emptyList<Any>()
        }
        if (names.isEmpty()) return result   

        val kept = ArrayList<Any>()
        for (item in list) {
            val pkg = pkgOf(item)
            val hit = pkg != null && pkg in names
            val keep = if (whitelist) hit else !hit
            if (keep && item != null) kept.add(item)
        }
        logWarn("hide apps: ${list.size} -> ${kept.size} (${if (whitelist) "whitelist" else "blacklist"})")
        return kept
    }

    
    private fun pkgOf(item: Any?): String? {
        if (item == null) return null
        return when (item) {
            is android.content.pm.ApplicationInfo -> item.packageName
            is android.content.pm.PackageInfo -> item.packageName
            else -> runCatching {
                val f = item.javaClass.getDeclaredField("packageName")
                f.isAccessible = true
                f.get(item) as? String
            }.getOrNull()
        }
    }
}
