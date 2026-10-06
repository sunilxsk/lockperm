package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule










internal class RecentsDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val FLAG = 0x00800000 

    fun install() {
        if (!snapshot().blockHideRecents) return

        
        runCatching {
            val c = loadClassAnywhere("android.app.ActivityManager\$AppTask") ?: return@runCatching
            c.declaredMethods.filter { it.name == "setExcludeFromRecents" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked AppTask.setExcludeFromRecents")
                    null
                }
            }
        }

        
        runCatching {
            val c = loadClassAnywhere("android.app.ActivityOptions") ?: return@runCatching
            c.declaredMethods.filter { it.name == "setExcludeFromRecents" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked ActivityOptions.setExcludeFromRecents")
                    null
                }
            }
        }

        
        
        
        
        stripAtStartActivity()

        logInfo("recents defender installed")
    }

    private fun stripAtStartActivity() {
        val names = setOf(
            "startActivity", "startActivityForResult",
            "startActivityIfNeeded", "startActivityFromChild",
        )
        listOf("android.app.Activity", "android.app.ContextImpl", "android.content.ContextWrapper")
            .forEach { name ->
                val c = frameworkCls(name) ?: return@forEach
                c.declaredMethods.filter { it.name in names }.forEach { m ->
                    hookMethod(m) { chain ->
                        val args = runCatching { chain.args }.getOrNull() ?: return@hookMethod chain.proceed()
                        val idx = args.indexOfFirst { it is android.content.Intent }
                        if (idx >= 0) {
                            val it0 = args[idx] as android.content.Intent
                            if (it0.flags and FLAG != 0) {
                                runCatching { it0.flags = it0.flags and FLAG.inv() }
                                logWarn("stripped FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS")
                            }
                        }
                        chain.proceed()
                    }
                }
            }
    }
}









internal object OverlayTuner {

    private const val FLAG_NOT_TOUCHABLE = 0x00000010
    private const val FLAG_NOT_FOCUSABLE = 0x00000008

    private const val MATCH_PARENT = -1
    private const val WRAP_CONTENT = -2

    




    private val adjusting = ThreadLocal<Boolean>()

    fun isAdjusting(): Boolean = adjusting.get() == true

    
    @JvmStatic
    fun tune(lp: android.view.WindowManager.LayoutParams, cfg: XpState.Snapshot, log: (String) -> Unit) {
        if (cfg.blockOverlay) return

        if (cfg.overlayUntouchable) {
            lp.flags = lp.flags or FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE
            log("overlay -> 穿透点击")
        }
        if (cfg.overlayTransparent) {
            lp.alpha = 0f
            log("overlay -> 完全透明")
        }
        if (cfg.overlayMaxPercent in 1..99) {
            val dm = runCatching {
                android.content.res.Resources.getSystem().displayMetrics
            }.getOrNull()
            if (dm != null) {
                val maxW = dm.widthPixels * cfg.overlayMaxPercent / 100
                val maxH = dm.heightPixels * cfg.overlayMaxPercent / 100
                
                
                if (lp.width == MATCH_PARENT || lp.width > maxW) lp.width = maxW
                if (lp.height == MATCH_PARENT || lp.height > maxH) lp.height = maxH
                log("overlay -> 限制到屏幕 ${cfg.overlayMaxPercent}%")
            }
        }
    }

    
    @JvmStatic
    fun needsMeasure(lp: android.view.WindowManager.LayoutParams, cfg: XpState.Snapshot): Boolean =
        !cfg.blockOverlay && cfg.overlayMaxPercent in 1..99 &&
                (lp.width == WRAP_CONTENT || lp.height == WRAP_CONTENT)

    



    @JvmStatic
    fun clampMeasured(
        lp: android.view.WindowManager.LayoutParams,
        measuredW: Int,
        measuredH: Int,
        cfg: XpState.Snapshot,
        log: (String) -> Unit,
    ): Boolean {
        if (cfg.blockOverlay || cfg.overlayMaxPercent !in 1..99) return false
        val dm = runCatching {
            android.content.res.Resources.getSystem().displayMetrics
        }.getOrNull() ?: return false
        val maxW = dm.widthPixels * cfg.overlayMaxPercent / 100
        val maxH = dm.heightPixels * cfg.overlayMaxPercent / 100

        
        val sw = if (measuredW > maxW && maxW > 0) maxW.toFloat() / measuredW else 1f
        val sh = if (measuredH > maxH && maxH > 0) maxH.toFloat() / measuredH else 1f
        val scale = minOf(sw, sh).coerceAtMost(1f)
        if (scale >= 1f) return false
        lp.width = (measuredW * scale).toInt().coerceAtLeast(1)
        lp.height = (measuredH * scale).toInt().coerceAtLeast(1)
        log("overlay -> 实测 ${measuredW}x${measuredH}，回缩到 ${lp.width}x${lp.height}")
        return true
    }

    
    @JvmStatic
    fun <T> withAdjustGuard(block: () -> T): T {
        adjusting.set(true)
        return try {
            block()
        } finally {
            adjusting.set(false)
        }
    }

    @JvmStatic
    fun active(cfg: XpState.Snapshot): Boolean =
        !cfg.blockOverlay &&
                (cfg.overlayUntouchable || cfg.overlayTransparent || cfg.overlayMaxPercent in 1..99)
}








internal object WindowFlagPresets {

    data class Flag(
        
        val id: String,
        val value: Int,
        val desc: String,
    )

    val ALL: List<Flag> = listOf(
        Flag("FLAG_SECURE", 0x00002000,
            "禁止截屏 / 录屏 / 投屏到不安全显示。开启后该窗口在截图里是黑的，应用内也截不到图。"),
        Flag("FLAG_KEEP_SCREEN_ON", 0x00000080,
            "保持屏幕常亮。只要这个窗口在前台，系统就不会自动熄屏。"),
        Flag("FLAG_FULLSCREEN", 0x00000400,
            "隐藏状态栏，窗口占满整屏。"),
        Flag("FLAG_NOT_FOCUSABLE", 0x00000008,
            "窗口不获取焦点，按键事件不发给它。悬浮窗想「只显示不抢焦点」就靠这个。"),
        Flag("FLAG_NOT_TOUCHABLE", 0x00000010,
            "窗口不接收任何触摸事件，点击会直接穿透到下面的界面。"),
        Flag("FLAG_NOT_TOUCH_MODAL", 0x00000020,
            "窗口区域内的触摸自己处理，区域外的触摸照常传给后面的窗口。"),
        Flag("FLAG_WATCH_OUTSIDE_TOUCH", 0x00040000,
            "只在 NOT_TOUCH_MODAL 下有意义：区域外的触摸会收到一个 ACTION_OUTSIDE。"),
        Flag("FLAG_DIM_BEHIND", 0x00000002,
            "窗口后面的内容整体变暗，值由 dimAmount 控制。"),
        Flag("FLAG_BLUR_BEHIND", 0x00000004,
            "窗口后面的内容做模糊处理（部分 ROM 已不支持，效果可能是无变化）。"),
        Flag("FLAG_SHOW_WALLPAPER", 0x00100000,
            "窗口后面显示系统壁纸而不是应用内容。"),
        Flag("FLAG_TURN_SCREEN_ON", 0x00200000,
            "窗口显示时点亮屏幕（熄灭状态下会先点亮）。"),
        Flag("FLAG_DISMISS_KEYGUARD", 0x00400000,
            "解除非安全锁屏（滑动锁、图案锁）。安全锁（密码）不会被解除。"),
        Flag("FLAG_SHOW_WHEN_LOCKED", 0x00080000,
            "允许窗口显示在锁屏之上。来电界面就是靠这个。"),
        Flag("FLAG_ALLOW_LOCK_WHILE_SCREEN_ON", 0x00000001,
            "配合 KEEP_SCREEN_ON：允许屏幕亮着的情况下仍然自动锁屏。"),
        Flag("FLAG_IGNORE_CHEEK_PRESSES", 0x00008000,
            "通话时过滤掉脸颊误触（屏幕紧贴脸部的触摸不生效）。"),
        Flag("FLAG_LAYOUT_NO_LIMITS", 0x00000200,
            "窗口布局不受屏幕边界限制，可以画到屏幕外。"),
        Flag("FLAG_LAYOUT_IN_SCREEN", 0x00000100,
            "窗口占满整屏，不考虑状态栏等装饰区域。"),
        Flag("FLAG_LAYOUT_INSET_DECOR", 0x00010000,
            "窗口不会被系统栏（状态栏 / 导航栏）盖住，会自动避让。"),
        Flag("FLAG_ALT_FOCUSABLE_IM", 0x00020000,
            "调整窗口与输入法的交互方式：窗口在输入法之上但仍能和输入法通信。"),
        Flag("FLAG_SPLIT_TOUCH", 0x00800000,
            "允许把多指触摸分别派发给不同窗口（默认同一串触摸只给一个窗口）。"),
        Flag("FLAG_HARDWARE_ACCELERATED", 0x01000000,
            "对这个窗口开启硬件加速渲染。"),
        Flag("FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS", 0x80000000.toInt(),
            "由窗口自己绘制状态栏 / 导航栏的背景，配合设置系统栏颜色使用。"),
        Flag("FLAG_TRANSLUCENT_STATUS", 0x04000000,
            "状态栏半透明，内容可以延伸到状态栏下面。"),
        Flag("FLAG_TRANSLUCENT_NAVIGATION", 0x08000000,
            "导航栏半透明，内容可以延伸到导航栏下面。"),
        Flag("FLAG_LOCAL_FOCUS_MODE", 0x10000000,
            "本地焦点模式：窗口内部自己管理焦点，不参与全局焦点。"),
    )

    fun maskOf(ids: Set<String>): Int {
        var m = 0
        for (f in ALL) if (f.id in ids) m = m or f.value
        return m
    }

    fun byId(id: String): Flag? = ALL.firstOrNull { it.id == id }
}

internal class WindowFlagDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val FLAG_SECURE = 0x00002000

    private fun active(): Boolean {
        val c = snapshot()
        return c.winSecureMode != 0 || c.winFlags.isNotEmpty()
    }

    private fun addMask(c: XpState.Snapshot): Int {
        var m = WindowFlagPresets.maskOf(c.winFlags)
        if (c.winSecureMode == 2) m = m or FLAG_SECURE
        return m
    }

    private fun removeMask(c: XpState.Snapshot): Int {
        var m = 0
        
        if (c.winSecureMode == 1) m = m or FLAG_SECURE
        return m
    }

    fun install() {
        if (!active()) return
        hookWindow()
        hookLayoutParams()
        logInfo("window flag tuner installed")
    }

    
    private fun hookWindow() {
        val win = frameworkCls("android.view.Window") ?: return
        win.declaredMethods.filter { it.name == "addFlags" || it.name == "setFlags" }.forEach { m ->
            hookMethod(m) { chain ->
                val c = snapshot()
                if (!active()) return@hookMethod chain.proceed()
                val args = runCatching { chain.args }.getOrNull() ?: return@hookMethod chain.proceed()
                val idx = args.indexOfFirst { it is Int }
                if (idx < 0) return@hookMethod chain.proceed()
                val v = (args[idx] as Int)
                var out = v
                out = (out or addMask(c)) and removeMask(c).inv()
                if (out == v) return@hookMethod chain.proceed()
                val next = args.toMutableList()
                next[idx] = out
                chain.proceed(next.toTypedArray())
            }
        }
    }

    
    private fun hookLayoutParams() {
        listOf("android.view.WindowManagerImpl", "android.view.WindowManagerGlobal").forEach { n ->
            runCatching {
                val c = frameworkCls(n) ?: return@runCatching
                hookAll(c, { it.name == "addView" || it.name == "updateViewLayout" }) { chain ->
                    val cfg = snapshot()
                    if (!active()) return@hookAll chain.proceed()
                    val args = runCatching { chain.args }.getOrNull()
                    val idx = args?.indexOfFirst {
                        it is android.view.WindowManager.LayoutParams
                    } ?: -1
                    if (idx < 0) return@hookAll chain.proceed()
                    val lp = args!![idx] as android.view.WindowManager.LayoutParams
                    val before = lp.flags
                    lp.flags = (before or addMask(cfg)) and removeMask(cfg).inv()
                    chain.proceed()
                }
            }
        }
    }
}








internal class WifiSavedDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockWifiSaved) return

        val wm = frameworkCls("android.net.wifi.WifiManager") ?: return

        
        val names = setOf(
            "getConfiguredNetworks",     
            "getPrivilegedConfiguredNetworks",
            "getWifiConfigsForPasspointProvision",
        )
        wm.declaredMethods.filter { it.name in names }.forEach { m ->
            hookMethod(m) { chain ->
                logWarn("blocked WifiManager.${m.name}（已保存 WiFi 列表）")
                deniedFor(m)
            }
        }

        
        wm.declaredMethods.filter { m ->
            m.name == "getMatchingWifiConfig" || m.name == "getConfiguredNetwork"
        }.forEach { m ->
            hookMethod(m) { chain ->
                logWarn("blocked WifiManager.${m.name}")
                deniedFor(m)
            }
        }

        
        runCatching {
            val sp = frameworkCls("android.provider.Settings\$Secure") ?: return@runCatching
            sp.declaredMethods.filter { it.name == "getString" }.forEach { m ->
                hookMethod(m) { chain ->
                    val key = chain.args.filterIsInstance<String>().firstOrNull()
                    if (key != null && key.contains("wifi", true)) {
                        logWarn("blocked Settings.Secure.getString($key)")
                        return@hookMethod null
                    }
                    chain.proceed()
                }
            }
        }

        logInfo("wifi saved list defender installed")
    }
}










internal class WakeLockDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val FLAG_KEEP_SCREEN_ON = 0x00000080

    fun install() {
        if (!snapshot().blockWakeLock) return

        hookWakeLock()
        hookWindowFlag()
        hookKeepScreenOn()
        logInfo("wake lock defender installed")
    }

    private fun hookWakeLock() {
        
        runCatching {
            val pm = frameworkCls("android.os.PowerManager") ?: return@runCatching
            pm.declaredMethods.filter { it.name == "newWakeLock" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked PowerManager.newWakeLock")
                    null
                }
            }
        }
        
        runCatching {
            val wl = loadClassAnywhere("android.os.PowerManager\$WakeLock") ?: return@runCatching
            wl.declaredMethods.filter { m ->
                m.name == "acquire" || m.name == "acquireLocked"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked WakeLock.${m.name}")
                    deniedFor(m)
                }
            }
        }
    }

    
    private fun hookWindowFlag() {
        val win = frameworkCls("android.view.Window") ?: return
        win.declaredMethods.filter { m ->
            m.name == "addFlags" || m.name == "setFlags"
        }.forEach { m ->
            hookMethod(m) { chain ->
                val args = runCatching { chain.args }.getOrNull() ?: return@hookMethod chain.proceed()
                val idx = args.indexOfFirst { it is Int }
                if (idx < 0) return@hookMethod chain.proceed()
                @Suppress("DEPRECATION")
                val f = args[idx] as Int
                if (f and FLAG_KEEP_SCREEN_ON == 0) return@hookMethod chain.proceed()
                logWarn("stripped FLAG_KEEP_SCREEN_ON")
                val cleared = f and FLAG_KEEP_SCREEN_ON.inv()
                val next = args.toMutableList()
                next[idx] = cleared
                chain.proceed(next.toTypedArray())
            }
        }
    }

    
    private fun hookKeepScreenOn() {
        val view = frameworkCls("android.view.View") ?: return
        view.declaredMethods.filter { it.name == "setKeepScreenOn" }.forEach { m ->
            hookMethod(m) { chain ->
                
                val on = chain.args.filterIsInstance<Boolean>().firstOrNull()
                if (on == true) {
                    logWarn("blocked View.setKeepScreenOn(true)")
                    return@hookMethod null
                }
                chain.proceed()
            }
        }
    }
}












internal class ScreenOffDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg = snapshot()
        if (!cfg.blockScreenOff) return

        hookPowerManager()
        hookPowerManagerService()
        
        if (cfg.screenOffReflect) hookReflectiveCall()
        logInfo("screen off defender installed")
    }

    private fun hookPowerManager() {
        val pm = frameworkCls("android.os.PowerManager") ?: return
        
        pm.declaredMethods.filter { m ->
            m.name == "goToSleep" || m.name == "nap"
        }.forEach { m ->
            hookMethod(m) { chain ->
                logWarn("blocked PowerManager.${m.name}")
                deniedFor(m)
            }
        }
        
        
        if (!snapshot().screenOffWakeLock) return
        runCatching {
            val wl = loadClassAnywhere("android.os.PowerManager\$WakeLock") ?: return@runCatching
            wl.declaredMethods.filter { it.name == "release" }.forEach { m ->
                hookMethod(m) { chain ->
                    
                    
                    if (isScreenLock(chain.getThisObject())) {
                        logWarn("blocked WakeLock.release (screen dim/bright)")
                        return@hookMethod null
                    }
                    chain.proceed()
                }
            }
        }
    }

    private fun isScreenLock(obj: Any?): Boolean {
        val o = obj ?: return false
        
        
        return runCatching {
            val f = o.javaClass.getDeclaredField("mFlags")
            f.isAccessible = true
            val flags = f.get(o) as? Int ?: return false
            flags and 0x0000000a != 0 || flags and 0x00000006 != 0
        }.getOrDefault(false)
    }

    




    private fun hookPowerManagerService() {
        
        runCatching {
            val sm = frameworkCls("android.os.ServiceManager") ?: return@runCatching
            sm.declaredMethods.filter { it.name == "getService" || it.name == "getServiceOrThrow" }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        val name = chain.args.filterIsInstance<String>().firstOrNull()
                        if (name == "power" || name?.contains("power", true) == true) {
                            logWarn("blocked ServiceManager.getService(power)")
                            return@hookMethod null
                        }
                        chain.proceed()
                    }
                }
        }
        
        runCatching {
            val stub = loadClassAnywhere("android.os.IPowerManager\$Stub") ?: return@runCatching
            stub.declaredMethods.filter { it.name == "asInterface" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked IPowerManager.Stub.asInterface")
                    null
                }
            }
        }
        
        runCatching {
            val pm = frameworkCls("android.os.PowerManager") ?: return@runCatching
            pm.declaredMethods.filter { it.name == "getService" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked PowerManager.getService")
                    null
                }
            }
        }
    }

    








    private fun hookReflectiveCall() {
        runCatching {
            val m = java.lang.reflect.Method::class.java
                .getDeclaredMethod(
                    "invoke", Any::class.java, Array<Any>::class.java
                )
            hookMethod(m) { chain ->
                val method = chain.getThisObject() as? java.lang.reflect.Method
                val name = method?.name
                if (name == "goToSleep" || name == "nap" || name == "forceSuspend") {
                    logWarn("blocked reflective $name")
                    return@hookMethod null
                }
                chain.proceed()
            }
        }
    }
}






internal class NotifyHideDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockNotifyHide) return

        val nm = frameworkCls("android.app.NotificationManager") ?: return
        nm.declaredMethods.filter { m ->
            m.name == "cancel" || m.name == "cancelAll" || m.name == "cancelAsUser"
        }.forEach { m ->
            hookMethod(m) { chain ->
                logWarn("blocked NotificationManager.${m.name}")
                null
            }
        }

        
        listOf(
            "androidx.core.app.NotificationManagerCompat",
            "android.support.v4.app.NotificationManagerCompat",
        ).forEach { name ->
            val c = cls(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "cancel" || it.name == "cancelAll" }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        logWarn("blocked $name.${m.name}")
                        deniedFor(m)
                    }
                }
        }

        
        
        logInfo("notify hide defender installed")
    }
}







internal class ProviderDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockProvider) return

        hookAcquire()
        hookResolver()
        logInfo("provider defender installed")
    }

    



    private fun hookAcquire() {
        val at = frameworkCls("android.app.ActivityThread") ?: return
        at.declaredMethods.filter { m ->
            m.name == "acquireProvider" || m.name == "installProvider" ||
                    m.name == "acquireExistingProvider"
        }.forEach { m ->
            hookMethod(m) { chain ->
                logWarn("blocked ActivityThread.${m.name}")
                null
            }
        }
    }

    
    private fun hookResolver() {
        val names = setOf("query", "insert", "update", "delete", "call")
        val cr = frameworkCls("android.content.ContentResolver") ?: return
        cr.declaredMethods.filter { it.name in names }.forEach { m ->
            hookMethod(m) { chain ->
                logWarn("blocked ContentResolver.${m.name}")
                deniedFor(m)
            }
        }
        
        cr.declaredMethods.filter { m ->
            m.name.startsWith("open") || m.name == "openTypedAssetFileDescriptor"
        }.forEach { m ->
            hookMethod(m) { chain ->
                logWarn("blocked ContentResolver.${m.name}")
                null
            }
        }
    }
}







internal class ForegroundServiceDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockFgService) return

        
        runCatching {
            val svc = frameworkCls("android.app.Service") ?: return@runCatching
            svc.declaredMethods.filter { m ->
                m.name == "startForeground" || m.name == "stopForeground"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    
                    if (m.name == "stopForeground") return@hookMethod chain.proceed()
                    logWarn("blocked Service.startForeground")
                    deniedFor(m)
                }
            }
        }

        
        listOf("android.app.ContextImpl", "android.content.ContextWrapper").forEach { name ->
            val c = frameworkCls(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "startForegroundService" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked $name.startForegroundService")
                    null
                }
            }
        }

        
        runCatching {
            val c = cls("androidx.core.content.ContextCompat") ?: return@runCatching
            c.declaredMethods.filter { it.name == "startForegroundService" }.forEach { m ->
                hookMethod(m) { chain ->
                    logWarn("blocked ContextCompat.startForegroundService")
                    deniedFor(m)
                }
            }
        }
        logInfo("foreground service defender installed")
    }
}
