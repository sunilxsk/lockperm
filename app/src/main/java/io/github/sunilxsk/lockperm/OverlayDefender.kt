package io.github.sunilxsk.lockperm

import android.content.Intent
import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule















internal class OverlayDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (snapshot().blockOverlay) hookOverlay()
        if (snapshot().blockWallpaper) hookWallpaper()
    }

    
    fun installOverlayOnly() {
        XpState.Flags.forceOverlay = true
        hookOverlay()
    }

    
    fun installWallpaperOnly() {
        XpState.Flags.forceWallpaper = true
        hookWallpaper()
    }

    

    private fun hookOverlay() {
        listOf("android.view.WindowManagerImpl", "android.view.WindowManagerGlobal")
            .forEach { name ->
                runCatching {
                    val clazz = cls(name) ?: return@runCatching
                    hookAll(clazz, { m -> m.name == "addView" }) { chain ->
                        if (overlayBlocked() && isSystemWindow(chain)) {
                            logWarn("blocked system overlay window ($name)")
                            null
                        } else {
                            chain.proceed()
                        }
                    }
                    logInfo("overlay hook installed: $name")
                }
            }
    }

    




    private fun isSystemWindow(chain: io.github.libxposed.api.XposedInterface.Chain): Boolean {
        val args = runCatching { chain.args }.getOrNull() ?: return false
        for (a in args) {
            val lp = a as? android.view.WindowManager.LayoutParams ?: continue
            val type = lp.type
            if (type == TYPE_TOAST) continue
            if (type >= FIRST_SYSTEM_WINDOW && type <= LAST_SYSTEM_WINDOW) return true
        }
        return false
    }

    

    
    private val WALLPAPER_METHODS: Set<String> = setOf(
        "setBitmap",           
        "setStream",           
        "setResource",         
        "setWallpaper",        
        "setLiveWallpaper",
        "clear",
        "getCropAndSetWallpaperIntent",
        "setWallpaperOffsetSteps",
        "setDisplayPadding",
    )

    private fun hookWallpaper() {
        val wm = cls("android.app.WallpaperManager")
        if (wm != null) {
            wm.declaredMethods.filter { it.name in WALLPAPER_METHODS }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!wallpaperBlocked()) return@hookMethod chain.proceed()
                    logWarn("blocked WallpaperManager.${m.name}")
                    deniedFor(m)
                }
            }
            logInfo("wallpaper (WallpaperManager) hook installed")
        }

        
        runCatching {
            val activity = cls("android.app.Activity") ?: return@runCatching
            activity.declaredMethods.filter { it.name == "setWallpaper" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!wallpaperBlocked()) return@hookMethod chain.proceed()
                    logWarn("blocked Activity.setWallpaper")
                    deniedFor(m)
                }
            }
            logInfo("wallpaper (Activity.setWallpaper) hook installed")
        }

        
        val launcher = cls("android.app.Activity") ?: return
        val starts = setOf(
            "startActivity", "startActivityForResult",
            "startActivityIfNeeded", "startActivityFromChild",
        )
        launcher.declaredMethods.filter { it.name in starts }.forEach { m ->
            hookMethod(m) { chain ->
                val intent = chain.args.filterIsInstance<Intent>().firstOrNull()
                if (wallpaperBlocked() && intent != null && isWallpaperIntent(intent)) {
                    logWarn("blocked wallpaper picker intent: ${intent.action}")
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    private fun isWallpaperIntent(intent: Intent): Boolean {
        val action = intent.action
        if (action == Intent.ACTION_SET_WALLPAPER) return true
        if (action == Intent.ACTION_ATTACH_DATA) {
            
            return runCatching {
                intent.type?.startsWith("image/") == true ||
                        intent.getStringExtra("mimeType")?.startsWith("image/") == true
            }.getOrDefault(false)
        }
        return runCatching {
            intent.component?.className?.contains("Wallpaper", ignoreCase = true) == true
        }.getOrDefault(false)
    }

    
    private fun overlayBlocked(): Boolean =
        XpState.Flags.forceOverlay || snapshot().blockOverlay

    
    private fun wallpaperBlocked(): Boolean =
        XpState.Flags.forceWallpaper || snapshot().blockWallpaper

    companion object {
        private const val FIRST_SYSTEM_WINDOW = 2000
        private const val LAST_SYSTEM_WINDOW = 2999
        private const val TYPE_TOAST = 2005
    }
}
