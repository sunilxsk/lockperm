package io.github.sunilxsk.lockperm

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam







class XposedModuleEntry : XposedModule() {

    
    private object PermFakeOnce {
        private val done = java.util.concurrent.atomic.AtomicBoolean(false)
        fun claim(): Boolean = done.compareAndSet(false, true)
    }

    





    private fun logAt(level: Int, tag: String, msg: String) {
        if (!XpState.Flags.logEnabled) return
        runCatching { log(level, tag, msg) }
    }

    






    private fun hostCacheDir(pkg: String): String {
        val candidates = listOf(
            "/data/user/0/$pkg/cache",
            "/data/data/$pkg/cache",
        )
        for (p in candidates) {
            val ok = runCatching {
                val f = java.io.File(p)
                if (f.isDirectory) true else f.mkdirs()
            }.getOrDefault(false)
            if (ok) return p
        }
        return candidates.last()
    }

    companion object {
        private const val TAG = "LockPerm"
        private const val SELF = "io.github.sunilxsk.lockperm"
    }

    override fun onPackageReady(param: PackageReadyParam) {
        val pkg = param.packageName
        if (SELF == pkg) return

        
        if (pkg == "android" || pkg == "system" || pkg.startsWith("com.android.systemui")) {
            return
        }

        val prefs = try {
            getRemotePreferences(XpConfig.PREFS)
        } catch (t: Throwable) {
            logAt(Log.ERROR, TAG, "getRemotePreferences failed: $t")
            return
        }

        val cl = param.classLoader
        XpState.setHost(pkg)
        val cfg = XpState.refresh(prefs, force = true)

        
        
        
        
        
        runCatching { BuildFields.apply(cfg) }
            .onFailure { logAt(Log.WARN, TAG, "build fields early failed: ${it.message}") }

        HookRuntime.bind(this, prefs, cl)

        
        runCatching { CommandBlocker(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "command blocker failed: ${it.message}") }

        
        runCatching { HookIdentity(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "identity hook failed: ${it.message}") }

        
        runCatching { ExtraIdentity(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "extra identity failed: ${it.message}") }

        
        runCatching { HardwareSpoofer(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "hardware spoofer failed: ${it.message}") }

        
        runCatching { ShellSpoofer(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "shell spoofer failed: ${it.message}") }

        
        runCatching { HidePathDefender(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "hide path failed: ${it.message}") }

        
        runCatching { FileSpoofer(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "file spoofer failed: ${it.message}") }

        
        runCatching { WifiFakeDefender(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "wifi fake failed: ${it.message}") }

        
        runCatching { RootFakeDefender(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "root fake failed: ${it.message}") }

        
        runCatching { VpnProxyDefender(this, prefs, cl).install() }
            .onFailure { logAt(Log.WARN, TAG, "vpn hide failed: ${it.message}") }

        
        if (cfg.blockCrashEnable) {
            runCatching { HookCrashBlocker(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "crash blocker failed: ${it.message}") }
        }

        
        if (cfg.crashCatchEnable) {
            runCatching { CrashCatcher(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "crash catcher failed: ${it.message}") }
        }

        
        
        
        
        
        if (cfg.permEnable && PermFakeOnce.claim()) {
            logAt(Log.INFO, TAG, "[${XpState.packageName}] permission fake: host=${XpState.packageName} current=$pkg")
            runCatching { PermissionFake(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "permission fake failed: ${it.message}") }
            runCatching { SpecialPermFake(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "special permission fake failed: ${it.message}") }
        }

        
        if (XpState.packageName != pkg) {
            logAt(
                Log.INFO, TAG,
                "note: 宿主=${XpState.packageName}，本次加载=$pkg（按宿主配置生效）"
            )
        }

        
        if (cfg.volumeEnable) {
            runCatching { VolumeDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "volume defender failed: ${it.message}") }
        }

        
        if (cfg.clipEnable) {
            runCatching { ClipboardDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "clipboard defender failed: ${it.message}") }
        }

        
        
        
        val guardOn = true
        if (guardOn) {
            
            runCatching { ExitExecutor.install(this, prefs) }
                .onFailure { logAt(Log.WARN, TAG, "exit executor failed: ${it.message}") }

            
            runCatching { AccessibilityDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "accessibility defender failed: ${it.message}") }

            
            runCatching { OverlayDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "overlay defender failed: ${it.message}") }

            
            runCatching { WirelessDebuggingDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "wireless debugging defender failed: ${it.message}") }

            
            runCatching { ShizukuDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "shizuku defender failed: ${it.message}") }

            
            runCatching { FileGuard(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "file guard failed: ${it.message}") }

            
            runCatching { AppListHider(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "app list hider failed: ${it.message}") }

            
            runCatching { AudioOutputBlocker(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "audio output blocker failed: ${it.message}") }

            
            runCatching { SystemControlDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "system control defender failed: ${it.message}") }

            
            
            runCatching { JumpDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "jump defender failed: ${it.message}") }

            
            runCatching { CameraMicDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "camera/mic defender failed: ${it.message}") }

            
            runCatching { InstallDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "install defender failed: ${it.message}") }

            
            runCatching { PrintCastDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "print/cast defender failed: ${it.message}") }

            
            runCatching { NotificationDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "notification defender failed: ${it.message}") }

            
            runCatching { NetworkFilter(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "network filter failed: ${it.message}") }

            
            runCatching { ScreenCaptureDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "screen capture defender failed: ${it.message}") }

            
            runCatching { TorchVibrateDefender(this, prefs, cl).install() }
                .onFailure { logAt(Log.WARN, TAG, "torch/vibrate defender failed: ${it.message}") }

            
            if (cfg.daEnable) {
                runCatching { DeviceAdminDefender(this, prefs, cl).install() }
                    .onFailure { logAt(Log.WARN, TAG, "device admin defender failed: ${it.message}") }
            }

            
            if (cfg.panelInject) {
                runCatching { ControlPanel.install(this, prefs, cl) }
                    .onFailure { logAt(Log.WARN, TAG, "control panel failed: ${it.message}") }
                
                runCatching { LogRecorder(this, prefs, cl).install() }
                    .onFailure { logAt(Log.WARN, TAG, "log recorder failed: ${it.message}") }
            }

        }

        
        
        
        
        
        
        
        
        
        
        if (cfg.nativeActive) {
            val cache = hostCacheDir(pkg)
            val payload = runCatching { NativePayload.build(cfg, cache) }
                .onFailure { logAt(Log.WARN, TAG, "native payload failed: ${it.message}") }
                .getOrNull()
            if (payload != null) {
                val ok = NativeBridge.apply(payload)
                logAt(
                    Log.INFO, TAG,
                    "native bridge for $pkg: ok=$ok groups=${cfg.nativeGroups} " +
                        "ready=${NativeBridge.ready()}"
                )
            }
        } else {
            
            
            
            runCatching { NativeBridge.disableIfPushed() }
                .onFailure { logAt(Log.WARN, TAG, "native disable failed: ${it.message}") }
            logAt(
                Log.INFO, TAG,
                "native hook not injected for $pkg: switch=${cfg.nativeHook} " +
                    "(mode=${cfg.nativeAppMode}) groups=${cfg.nativeGroups}"
            )
        }

        
        
        logAt(
            Log.INFO, TAG,
            "配置生效[$pkg] host=${XpState.packageName} panel=${cfg.panelInject} " +
                    "exit=${cfg.exitEnable} acc=${cfg.accEnable} overlay=${cfg.blockOverlay} " +
                    "wallpaper=${cfg.blockWallpaper} da=${cfg.daEnable} vol=${cfg.volumeEnable} " +
                    "audio=${cfg.audioOutEnable} clip=${cfg.clipEnable} torch=${cfg.blockTorch} " +
                    "vibrate=${cfg.blockVibrate} file=${cfg.fileGuardEnable} hide=${cfg.hideAppsEnable} " +
                    "wdbg=${cfg.wdbgEnable} shizuku=${cfg.shizukuEnable} conn=${cfg.blockConnEnable} " +
                    "sensor=${cfg.blockSensor} exec=${cfg.blockExec} perm=${cfg.permEnable}"
        )

        logAt(Log.INFO, TAG, "hooks installed for $pkg")
    }
}
