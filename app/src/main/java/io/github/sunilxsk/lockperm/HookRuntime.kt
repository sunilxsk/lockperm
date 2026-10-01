package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule













internal object HookRuntime {

    @Volatile
    private var module: XposedModule? = null

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var classLoader: ClassLoader? = null

    fun bind(module: XposedModule, prefs: SharedPreferences, classLoader: ClassLoader) {
        this.module = module
        this.prefs = prefs
        this.classLoader = classLoader
    }

    

    fun blockWallpaperNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        XpState.Flags.forceWallpaper = true
        return runCatching {
            OverlayDefender(m, p, cl).installWallpaperOnly()
            true
        }.getOrDefault(false)
    }

    

    fun blockOverlayNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        XpState.Flags.forceOverlay = true
        return runCatching {
            OverlayDefender(m, p, cl).installOverlayOnly()
            true
        }.getOrDefault(false)
    }

    

    
    fun accessibilityAllInOne(): Boolean {
        val (m, p, cl) = triple() ?: return false
        XpState.Flags.forceAccessibility = true
        XpState.Flags.forceAccessibilityAll = true
        return runCatching {
            
            AccessibilityDefender(m, p, cl).install()
            AccessibilityDefender.forceCloseAll()
            true
        }.getOrDefault(false)
    }

    

    
    fun deviceAdminAllInOne(): Boolean {
        val (m, p, cl) = triple() ?: return false
        XpState.Flags.forceDeviceAdmin = true
        return runCatching {
            DeviceAdminDefender(m, p, cl).install()
            true
        }.getOrDefault(false)
    }

    

    fun blockConnNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching {
            SystemControlDefender(m, p, cl).installConnOnly()
            true
        }.getOrDefault(false)
    }

    

    fun blockSensorNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching {
            SystemControlDefender(m, p, cl).installSensorOnly()
            true
        }.getOrDefault(false)
    }

    



    

    fun blockJumpNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { JumpDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    fun blockCameraMicNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { CameraMicDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    fun blockInstallNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { InstallDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    fun blockPrintCastNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { PrintCastDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    fun blockNotifyNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { NotificationDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    fun blockScreenCaptureNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { ScreenCaptureDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    fun blockNetNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { NetworkFilter(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    
    
    
    

    
    fun blockTorchVibrateNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { TorchVibrateDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockClipNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { ClipboardDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockFileNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { FileGuard(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockHideAppsNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { AppListHider(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockAudioOutNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { AudioOutputBlocker(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockWdbgNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { WirelessDebuggingDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockShizukuNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { ShizukuDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockExecNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { CommandBlocker(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    
    fun blockVolumeNow(): Boolean {
        val (m, p, cl) = triple() ?: return false
        return runCatching { VolumeDefender(m, p, cl).installNow(); true }.getOrDefault(false)
    }

    




    fun allInOne(): Int {
        val (m, p, cl) = triple() ?: return 0
        var ok = 0
        fun tryIt(block: () -> Boolean) {
            if (runCatching { block() }.getOrDefault(false)) ok++
        }
        tryIt { blockWallpaperNow() }
        tryIt { blockOverlayNow() }
        tryIt { blockConnNow() }
        tryIt { blockSensorNow() }
        tryIt { blockJumpNow() }
        tryIt { blockCameraMicNow() }
        tryIt { blockInstallNow() }
        tryIt { blockPrintCastNow() }
        tryIt { blockNotifyNow() }
        tryIt { blockScreenCaptureNow() }
        tryIt { blockNetNow() }
        tryIt { accessibilityAllInOne() }
        tryIt { deviceAdminAllInOne() }
        return ok
    }

    
    fun snapshot(): XpState.Snapshot? {
        val p = prefs ?: return null
        return runCatching { XpState.refresh(p, force = true) }.getOrNull()
    }

    private fun triple(): Triple<XposedModule, SharedPreferences, ClassLoader>? {
        val m = module ?: return null
        val p = prefs ?: return null
        val cl = classLoader ?: return null
        return Triple(m, p, cl)
    }
}
