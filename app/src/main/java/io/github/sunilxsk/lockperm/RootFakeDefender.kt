package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule










internal class RootFakeDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

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
        if (!cfg.rootFakeEnable) return
        if (cfg.rootFakeFile) hookSuFiles()
        logInfo("root fake installed (file=${cfg.rootFakeFile})")
    }

    private fun hookSuFiles() {
        val file = loadClassAnywhere("java.io.File") ?: return
        listOf("exists", "isFile", "canExecute").forEach { name ->
            file.declaredMethods.filter { it.name == name }.forEach { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val p = runCatching {
                        self?.javaClass?.getDeclaredMethod("getAbsolutePath")
                            ?.apply { isAccessible = true }?.invoke(self) as? String
                    }.getOrNull()
                    if (p != null && isSuPath(p)) true else chain.proceed()
                }
            }
        }
        logInfo("su file hooks installed")
    }

    private fun isSuPath(p: String): Boolean {
        if (p in SU_PATHS) return true
        val l = p.lowercase()
        return SU_PATHS.any { it.lowercase() == l } || l.endsWith("/su") || l.endsWith("/su.bin")
    }
}
