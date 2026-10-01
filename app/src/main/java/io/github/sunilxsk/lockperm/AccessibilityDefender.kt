package io.github.sunilxsk.lockperm

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.libxposed.api.XposedModule
import java.util.Collections
import java.util.WeakHashMap















internal class AccessibilityDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        hookServiceLifecycle()
        installCapabilityHooks()
        hookAccessibilityManager()
        hookEventDelivery()
        startWatchdog()
        val cfg = snapshot()
        logInfo("accessibility defender installed (enable=${cfg.accEnable}, mode=${cfg.accMode}, scope=${cfg.accScope})")
    }

    

    




    private fun hookAccessibilityManager() {
        val am = frameworkCls("android.view.accessibility.AccessibilityManager") ?: return

        
        am.declaredMethods.filter { it.name == "isEnabled" }.forEach { m ->
            hookMethod(m) { chain ->
                val cfg = snapshot()
                if (cfg.accStatusSpoof) {
                    cfg.accStatusValue
                } else if (shouldDisableContinuously() || XpState.Flags.forceAccessibility) {
                    false
                } else {
                    chain.proceed()
                }
            }
        }
        am.declaredMethods.filter { it.name == "isTouchExplorationEnabled" }.forEach { m ->
            hookMethod(m) { chain ->
                val cfg = snapshot()
                if (cfg.accStatusSpoof && !cfg.accStatusValue) false else chain.proceed()
            }
        }
        am.declaredMethods.filter { it.name == "getEnabledAccessibilityServiceList" }.forEach { m ->
            hookMethod(m) { chain ->
                val cfg = snapshot()
                val r = chain.proceed()
                val off = if (cfg.accStatusSpoof) {
                    !cfg.accStatusValue
                } else {
                    shouldDisableContinuously() || XpState.Flags.forceAccessibility
                }
                if (off) java.util.Collections.emptyList<Any>() else r
            }
        }

        
        val sg = frameworkCls("android.provider.Settings\$Secure")
        if (sg != null) {
            runCatching {
                sg.declaredMethods.filter { it.name == "getString" }.forEach { m ->
                    hookMethod(m) { chain ->
                        val cfg = snapshot()
                        val k = chain.getArg(1) as? String
                        if (cfg.accStatusSpoof && !cfg.accStatusValue &&
                            k != null && k.contains("accessibility")
                        ) {
                            ""
                        } else {
                            chain.proceed()
                        }
                    }
                }
            }
        }
        logInfo("accessibility manager hook installed")
    }

    






    private fun hookEventDelivery() {
        val wrapper = frameworkCls("android.accessibilityservice.IAccessibilityServiceClientWrapper")
            ?: frameworkCls("android.accessibilityservice.AccessibilityService\$IAccessibilityServiceClientWrapper")
        if (wrapper == null) {
            logWarn("event delivery: IAccessibilityServiceClientWrapper not found")
            return
        }
        wrapper.declaredMethods.filter { it.name == "onAccessibilityEvent" }.forEach { m ->
            hookMethod(m) { chain ->
                val event = chain.args.filterIsInstance<AccessibilityEvent>().firstOrNull()
                if (shouldBlockEvent(event)) {
                    logWarn("event swallowed at delivery layer")
                    return@hookMethod null
                }
                chain.proceed()
            }
        }
        logInfo("event delivery hook installed")
    }

    

    private fun hookServiceLifecycle() {
        val svc = frameworkCls("android.accessibilityservice.AccessibilityService")
        if (svc == null) {
            logWarn("accessibility: AccessibilityService class not found")
            return
        }

        
        
        runCatching {
            val m = svc.getDeclaredMethod("onBind", android.content.Intent::class.java)
            hookMethod(m) { chain ->
                val r = chain.proceed()
                val service = chain.getThisObject() as? AccessibilityService
                if (service != null) {
                    Instances.add(service)
                    logWarn("captured service via onBind: ${service.javaClass.name}")
                    if (shouldDisableContinuously()) disableNow(service)
                }
                r
            }
            logInfo("accessibility: hooked onBind")
        }

        
        runCatching {
            val m = svc.getDeclaredMethod("onCreate")
            hookMethod(m) { chain ->
                val r = chain.proceed()
                val service = chain.getThisObject() as? AccessibilityService
                if (service != null) {
                    Instances.add(service)
                    if (shouldDisableContinuously()) disableNow(service)
                }
                r
            }
        }.onFailure { logWarn("accessibility: onCreate hook skipped") }

        
        runCatching {
            val base = frameworkCls("android.app.Service")
            val m = base?.getDeclaredMethod("onCreate")
            if (m != null) {
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    val service = chain.getThisObject() as? AccessibilityService
                    if (service != null) {
                        Instances.add(service)
                        if (shouldDisableContinuously()) disableNow(service)
                    }
                    r
                }
            }
        }

        
        runCatching {
            val m = svc.getDeclaredMethod("onServiceConnected")
            hookMethod(m) { chain ->
                val result = chain.proceed()
                val service = chain.getThisObject() as? AccessibilityService
                if (service != null) {
                    Instances.add(service)
                    if (shouldDisableContinuously()) disableNow(service)
                }
                result
            }
        }

        
        runCatching {
            val m = svc.getDeclaredMethod(
                "onAccessibilityEvent", AccessibilityEvent::class.java
            )
            hookMethod(m) { chain ->
                val service = chain.getThisObject() as? AccessibilityService
                if (service != null) {
                    Instances.add(service)
                    if (shouldDisableContinuously()) disableNow(service)
                }
                if (shouldBlockEvent(chain.getArg(0) as? AccessibilityEvent)) {
                    null
                } else {
                    chain.proceed()
                }
            }
        }

        
        runCatching {
            val m = svc.getDeclaredMethod("onInterrupt")
            hookMethod(m) { chain ->
                val service = chain.getThisObject() as? AccessibilityService
                if (service != null) {
                    Instances.add(service)
                    if (shouldDisableContinuously()) disableNow(service)
                }
                null
            }
        }
    }

    
    private fun capOn(value: Boolean): Boolean {
        if (XpState.Flags.forceAccessibilityAll) return true
        return value
    }

    
    private fun shouldDisableContinuously(): Boolean {
        if (XpState.Flags.forceAccessibility) return true
        val cfg = snapshot()
        
        if (cfg.accScope == 2) return false
        
        if (oneShotPending()) return true
        
        return cfg.accEnable && (cfg.accMode == 0 || !cfg.exitEnable)
    }

    private fun shouldBlockEvent(event: AccessibilityEvent?): Boolean {
        val cfg = snapshot()
        val force = XpState.Flags.forceAccessibility
        
        if (XpState.Flags.forceAccessibilityAll) return true
        if (!force && !cfg.accEnable) return false
        
        if (!force && cfg.accScope == 0) return false

        val type = event?.eventType ?: return false
        return when {
            cfg.accCapNotify && type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> true
            cfg.accCapWindow && type == TYPE_WINDOW_STATE_CHANGED -> true
            cfg.accCapWindow && type == TYPE_WINDOW_CONTENT_CHANGED -> true
            cfg.accCapWindow && type == TYPE_WINDOWS_CHANGED -> true
            cfg.accCapInput && type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> true
            else -> false
        }
    }

    

    



    private fun installCapabilityHooks() {
        
        val capScreen: () -> Boolean = { capOn(snapshot().accCapScreen) }
        val capNotify: () -> Boolean = { capOn(snapshot().accCapNotify) }
        val capWindow: () -> Boolean = { capOn(snapshot().accCapWindow) }
        val capInput: () -> Boolean = { capOn(snapshot().accCapInput) }
        val capAction: () -> Boolean = { capOn(snapshot().accCapAction) }
        val capOverlay: () -> Boolean = { capOn(snapshot().accCapOverlay) }
        val capControl: () -> Boolean = { capOn(snapshot().accCapControl) }

        val svc = frameworkCls("android.accessibilityservice.AccessibilityService")
        val node = AccessibilityNodeInfo::class.java
        val event = AccessibilityEvent::class.java
        val record = frameworkCls("android.view.accessibility.AccessibilityRecord")
        val winInfo = frameworkCls("android.view.accessibility.AccessibilityWindowInfo")

        
        blockOn(node, SCREEN_NODE_METHODS, capScreen, "AccessibilityNodeInfo")
        blockOn(svc, setOf("getRootInActiveWindow", "findFocus"), capScreen, "AccessibilityService")
        blockOn(
            svc,
            setOf("findAccessibilityNodeInfosByText", "findAccessibilityNodeInfosByViewId"),
            capScreen,
            "AccessibilityService",
        )
        
        runCatching {
            val m = node.getDeclaredMethod("getBoundsInScreen", Rect::class.java)
            hookMethod(m) { chain ->
                if (active() && breakageEnabled() && capScreen()) {
                    (chain.getArg(0) as? Rect)?.setEmpty()
                    null
                } else {
                    chain.proceed()
                }
            }
        }
        
        blockOn(
            frameworkCls("android.view.accessibility.AccessibilityInteractionClient"),
            INTERACTION_METHODS,
            capScreen,
            "AccessibilityInteractionClient",
        )

        
        
        
        
        
        blockOn(event, setOf("getText"), { capNotify() || capInput() }, "AccessibilityEvent")
        blockOn(record, setOf("getText"), { capNotify() || capInput() }, "AccessibilityRecord")
        blockOn(event, NOTIFY_METHODS - "getText", capNotify, "AccessibilityEvent")
        blockOn(record, NOTIFY_METHODS - "getText", capNotify, "AccessibilityRecord")

        
        blockOn(event, EVENT_WINDOW_METHODS, capWindow, "AccessibilityEvent")
        blockOn(record, RECORD_WINDOW_METHODS, capWindow, "AccessibilityRecord")
        blockOn(svc, setOf("getWindows", "getWindowsOnAllDisplays"), capWindow, "AccessibilityService")
        blockOn(winInfo, WINDOW_METHODS, capWindow, "AccessibilityWindowInfo")

        
        blockOn(event, INPUT_METHODS - "getText", capInput, "AccessibilityEvent")
        blockOn(record, INPUT_METHODS - "getText", capInput, "AccessibilityRecord")

        
        
        
        
        hookOverlayCreation()
        if (capOverlay()) startOverlaySweeper()

        
        blockOn(node, setOf("performAction"), capAction, "AccessibilityNodeInfo")
        blockOn(svc, setOf("performGlobalAction", "dispatchGesture"), capAction, "AccessibilityService")
        blockOn(
            frameworkCls("android.accessibilityservice.GestureDescription"),
            setOf("addStroke"),
            capAction,
            "GestureDescription",
        )

        
        
        
        
        blockOn(svc, CONTROL_SERVICE_METHODS, capControl, "AccessibilityService")
        blockOn(svc, CONTROL_CALLBACKS, capControl, "AccessibilityService")
        blockOn(record, setOf("getSourceNodeId"), capControl, "AccessibilityRecord")
        
        blockOn(
            frameworkCls("android.accessibilityservice.AccessibilityService\$MagnificationController"),
            setOf("setMagnificationScale", "setCenter", "reset"),
            capControl,
            "MagnificationController",
        )
        blockOn(
            frameworkCls("android.accessibilityservice.AccessibilityService\$SoftKeyboardController"),
            setOf("setShowSoftKeyboard", "showSoftKeyboard", "setSoftKeyboardShowMode"),
            capControl,
            "SoftKeyboardController",
        )
        blockOn(
            frameworkCls("android.accessibilityservice.FingerprintGestureController"),
            setOf("dispatchFingerprintGesture"),
            capControl,
            "FingerprintGestureController",
        )
        blockOn(
            frameworkCls("android.accessibilityservice.AccessibilityButtonController"),
            setOf("isAccessibilityButtonAvailable"),
            capControl,
            "AccessibilityButtonController",
        )
        
        
        
        hookSetServiceInfo(svc)
    }

    





    private fun hookSetServiceInfo(svc: Class<*>?) {
        val c = svc ?: return
        runCatching {
            c.declaredMethods.filter { it.name == "setServiceInfo" }.forEach { m ->
                hookMethod(m) { chain ->
                    val info = chain.args.firstOrNull { it?.javaClass?.name?.contains("AccessibilityServiceInfo") == true }
                    if (!active() || !breakageEnabled() || !capOn(snapshot().accCapControl)) {
                        return@hookMethod chain.proceed()
                    }
                    if (info == null) return@hookMethod chain.proceed()
                    val args = chain.args.toTypedArray()
                    val idx = args.indexOfFirst { it === info }
                    runCatching { stripDangerousFlags(info) }
                    args[idx] = info
                    chain.proceed(args)
                }
            }
        }
    }

    
    private fun stripDangerousFlags(info: Any) {
        val clazz = info.javaClass
        val f = runCatching { clazz.getDeclaredField("flags") }.getOrNull()
            ?: runCatching { clazz.superclass?.getDeclaredField("flags") }.getOrNull()
            ?: return
        f.isAccessible = true
        val cur = (f.get(info) as? Number)?.toInt() ?: return
        val cleaned = cur and DANGEROUS_FLAGS.inv()
        if (cleaned != cur) {
            f.set(info, cleaned)
            logWarn("stripped AccessibilityServiceInfo flags: ${cur.toString(2)} -> ${cleaned.toString(2)}")
        }
    }

    




    private fun hookOverlayCreation() {
        val cap: () -> Boolean = { capOn(snapshot().accCapOverlay) }
        runCatching {
            val impl = cls("android.view.WindowManagerImpl") ?: return@runCatching
            hookAll(impl, { m -> m.name == "addView" }) { chain ->
                if (active() && breakageEnabled() && cap() && hasA11yOverlayType(chain)) {
                    logWarn("blocked TYPE_ACCESSIBILITY_OVERLAY creation (WindowManagerImpl)")
                    null
                } else {
                    chain.proceed()
                }
            }
        }
        runCatching {
            val global = cls("android.view.WindowManagerGlobal") ?: return@runCatching
            hookAll(global, { m -> m.name == "addView" }) { chain ->
                if (active() && breakageEnabled() && cap() && hasA11yOverlayType(chain)) {
                    logWarn("blocked TYPE_ACCESSIBILITY_OVERLAY creation (WindowManagerGlobal)")
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    
    private fun hasA11yOverlayType(chain: io.github.libxposed.api.XposedInterface.Chain): Boolean {
        val args = runCatching { chain.args }.getOrNull() ?: return false
        for (a in args) {
            val lp = a as? android.view.WindowManager.LayoutParams ?: continue
            if (lp.type == TYPE_ACCESSIBILITY_OVERLAY) return true
        }
        return false
    }

    




    private fun startOverlaySweeper() {
        if (!Holder.sweeperStarted.compareAndSet(false, true)) return
        val tick = object : Runnable {
            override fun run() {
                val cfg = snapshot()
                val on = XpState.Flags.forceAccessibilityAll ||
                        (cfg.accEnable && cfg.accCapOverlay)
                if (!on) {
                    Holder.sweeperStarted.set(false)
                    return
                }
                runCatching { sweepA11yOverlayWindows() }
                mainHandler.postDelayed(this, SWEEP_INTERVAL_MS)
            }
        }
        mainHandler.postDelayed(tick, 1500)
    }

    private fun sweepA11yOverlayWindows() {
        val wmg = Class.forName("android.view.WindowManagerGlobal")
        val getInstance = wmg.getDeclaredMethod("getInstance")
        getInstance.isAccessible = true
        val inst = getInstance.invoke(null) ?: return

        val fViews = wmg.getDeclaredField("mViews")
        fViews.isAccessible = true
        val fParams = wmg.getDeclaredField("mParams")
        fParams.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val views = fViews.get(inst) as? ArrayList<android.view.View> ?: return
        @Suppress("UNCHECKED_CAST")
        val params = fParams.get(inst) as? ArrayList<android.view.WindowManager.LayoutParams>
            ?: return

        val remove = runCatching {
            wmg.getDeclaredMethod("removeView", android.view.View::class.java, java.lang.Boolean.TYPE)
        }.getOrNull()
        remove?.isAccessible = true

        val doomed = ArrayList<android.view.View>()
        val count = minOf(views.size, params.size)
        for (i in 0 until count) {
            if (params[i].type == TYPE_ACCESSIBILITY_OVERLAY) {
                doomed.add(views[i])
            }
        }
        for (v in doomed) {
            
            runCatching {
                val ctx = v.context
                val wm = ctx.getSystemService(android.content.Context.WINDOW_SERVICE)
                        as? android.view.WindowManager
                wm?.removeView(v)
            }.onFailure {
                runCatching { remove?.invoke(inst, v, java.lang.Boolean.TRUE) }
            }
        }
        if (doomed.isNotEmpty()) {
            logWarn("swept ${doomed.size} TYPE_ACCESSIBILITY_OVERLAY window(s)")
        }
    }

    
    private fun active(): Boolean {
        if (XpState.Flags.forceAccessibility) return true
        return snapshot().accEnable
    }

    
    private fun breakageEnabled(): Boolean {
        if (XpState.Flags.forceAccessibility) return true
        val cfg = snapshot()
        if (!cfg.accEnable) return false
        
        return cfg.accScope == 1 || cfg.accScope == 2
    }

    

    @Volatile
    private var watchdogRunning = false

    private val watchdogLock = Any()

    






    private fun startWatchdog() {
        synchronized(watchdogLock) {
            if (watchdogRunning) return
            watchdogRunning = true
        }
        Thread {
            var round = 0
            var idle = 0
            while (true) {
                runCatching {
                    
                    
                    if (!shouldDisableContinuously() && !XpState.Flags.forceAccessibility
                        && !oneShotPending()
                    ) {
                        idle++
                        if (idle >= 10) {
                            synchronized(watchdogLock) { watchdogRunning = false }
                            logInfo("accessibility watchdog stopped")
                            return@Thread
                        }
                    } else {
                        idle = 0
                    }
                    val list = Instances.all()
                    if (list.isNotEmpty()) {
                        round++
                        for (svc in list) {
                            runCatching { disableSelf(svc) }
                        }
                        
                        if (round == 1 || round % 25 == 0) {
                            logWarn("watchdog: 第 $round 轮，压制 ${list.size} 个服务实例")
                        }
                    }
                    Thread.sleep(400)
                }.onFailure {
                    runCatching { Thread.sleep(1000) }
                }
            }
        }.apply {
            isDaemon = true
            name = "xp-a11y-watchdog"
        }.start()
        logInfo("accessibility watchdog started")
    }

    
    private fun oneShotPending(): Boolean = oneShotUntil > System.currentTimeMillis()

    @Volatile
    private var oneShotUntil: Long = 0L

    

    



    private fun blockOn(
        clazz: Class<*>?,
        names: Set<String>,
        cap: () -> Boolean,
        tag: String,
    ) {
        val c = clazz ?: return
        c.declaredMethods.filter { it.name in names }.forEach { m ->
            hookMethod(m) { chain ->
                if (active() && breakageEnabled() && cap()) {
                    logWarn("blocked $tag.${m.name}")
                    deniedFor(m)
                } else {
                    chain.proceed()
                }
            }
        }
    }

    

    







    fun disableAllNow() {
        
        oneShotUntil = System.currentTimeMillis() + 10_000L
        startWatchdog()

        val list = Instances.all()
        if (list.isEmpty()) {
            logWarn("disableAllNow: 还没捕获到服务实例，靠看门狗兜底")
            return
        }
        val barrier = java.util.concurrent.CountDownLatch(1)
        val threads = list.map { svc ->
            Thread {
                runCatching { barrier.await() }
                repeat(15) { i ->
                    runCatching { disableSelf(svc) }
                    
                    runCatching { Thread.sleep(if (i < 5) 60 else 150) }
                }
            }.apply {
                isDaemon = true
                name = "xp-a11y-kill"
            }
        }
        threads.forEach { runCatching { it.start() } }
        barrier.countDown()          
        threads.forEach { runCatching { it.join(2500) } }
        logWarn("disableAllNow done (${list.size} 个实例)")
    }

    private fun disableNow(service: AccessibilityService) {
        Thread {
            repeat(6) { i ->
                runCatching { disableSelf(service) }
                runCatching { Thread.sleep(if (i < 3) 80 else 200) }
            }
        }.apply {
            isDaemon = true
            name = "xp-a11y-disable"
        }.start()
    }

    




    private fun disableSelf(service: AccessibilityService) {
        var ok = runCatching { service.disableSelf(); true }.getOrDefault(false)
        if (!ok) {
            ok = runCatching {
                val m = service.javaClass.getMethod("disableSelf")
                m.isAccessible = true
                m.invoke(service)
                true
            }.getOrDefault(false)
        }
        if (!ok) logWarn("disableSelf failed: ${service.javaClass.name}")
    }

    init {
        INSTANCE = this
    }

    companion object {
        

        
        private val SCREEN_NODE_METHODS: Set<String> = setOf(
            "getText", "getContentDescription", "getStateDescription", "getTooltipText",
            "getHintText", "getPaneTitle", "getError", "getClassName", "getPackageName",
            "getChildCount", "getChild", "getParent", "refresh", "getExtras", "getActionList",
            "isClickable", "isEnabled", "isFocused", "isChecked", "isSelected", "isScrollable",
            "isEditable", "isPassword", "isVisibleToUser", "isAccessibilityFocused",
            "isContentInvalid", "isContextClickable", "isDismissable", "isShowingHintText",
            "isTextSelectable", "isImportantForAccessibility", "isMultiLine", "isHeading",
            "isScreenReaderFocusable", "getViewIdResourceName", "getUniqueId",
            "getTraversalBefore", "getTraversalAfter", "getLabelFor", "getLabeledBy",
            "getTextSelectionStart", "getTextSelectionEnd", "getInputType", "getLiveRegion",
            "getDrawingOrder", "getMovementGranularities", "getMaxScrollX", "getMaxScrollY",
            "getScrollX", "getScrollY", "getCollectionInfo", "getCollectionItemInfo",
            "getRangeInfo", "getTouchDelegateInfo", "getWindow", "getWindowId",
        )

        
        private val INTERACTION_METHODS: Set<String> = setOf(
            "findAccessibilityNodeInfoByAccessibilityId",
            "findAccessibilityNodeInfoByAccessibilityIdUiThread",
            "findAccessibilityNodeInfosByText",
            "findAccessibilityNodeInfosByViewId",
            "findAccessibilityNodeInfosByTextUiThread",
            "findFocus", "findFocusUiThread",
            "performAccessibilityAction",
        )

        
        private val NOTIFY_METHODS: Set<String> = setOf(
            "getText", "getContentDescription", "getParcelableData",
            "getItemCount", "getCurrentItemIndex", "getFromIndex", "getToIndex",
        )

        
        private val INPUT_METHODS: Set<String> = setOf("getText", "getBeforeText")

        
        private val EVENT_WINDOW_METHODS: Set<String> = setOf(
            "getSource", "getEventType", "getAction",
            "getMovementGranularity", "getContentChangeTypes", "getDisplayId",
        )

        
        private val RECORD_WINDOW_METHODS: Set<String> = setOf(
            "getClassName", "getPackageName", "getWindowId",
        )

        
        private val WINDOW_METHODS: Set<String> = setOf(
            "getRoot", "getChild", "getTitle", "getId", "getLayer", "getType",
            "isActive", "isFocused", "isAccessibilityFocused", "getParent", "getAnchor",
            "getDisplayId", "getRegionInScreen",
        )

        
        private val CONTROL_SERVICE_METHODS: Set<String> = setOf(
            "takeScreenshot", "takeScreenshotOfWindow",
            "setGestureDetectionPassthroughRegion", "setTouchExplorationPassthroughRegion",
            "setAnimationScale", "setAccessibilityFocusAppearance", "setCacheEnabled",
            "getSystemActions", "getTouchInteractionController", "getInputMethod",
            "onCreateInputMethod", "getAccessibilityButtonController",
            "getMagnificationController", "getSoftKeyboardController",
            "getFingerprintGestureController",
        )

        



        private val CONTROL_CALLBACKS: Set<String> = setOf(
            "onKeyEvent", "onGesture", "onMotionEvent", "onSystemActionsChanged",
            "onMagnificationChanged", "onSoftKeyboardShowModeChanged",
            "onFingerprintCapturingGesturesChanged", "onFingerprintGesture",
            "onAccessibilityButtonClicked", "onAccessibilityButtonAvailabilityChanged",
            "onPerformGestureResult", "init",
        )

        
        private const val FLAG_RETRIEVE_INTERACTIVE_WINDOWS = 0x00000002
        private const val FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 0x00000004
        private const val FLAG_REQUEST_TOUCH_EXPLORATION_MODE = 0x00000008
        private const val FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY = 0x00000010
        private const val FLAG_REPORT_VIEW_IDS = 0x00000020
        private const val FLAG_REQUEST_FILTER_KEY_EVENTS = 0x00000040
        private const val FLAG_REQUEST_FINGERPRINT_GESTURES = 0x00000200
        private const val FLAG_REQUEST_MULTI_FINGER_GESTURES = 0x00000100
        private const val FLAG_INPUT_METHOD_EDITOR = 0x00000080

        private const val DANGEROUS_FLAGS =
            FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                    FLAG_REPORT_VIEW_IDS or
                    FLAG_REQUEST_FILTER_KEY_EVENTS or
                    FLAG_REQUEST_FINGERPRINT_GESTURES or
                    FLAG_REQUEST_MULTI_FINGER_GESTURES or
                    FLAG_INPUT_METHOD_EDITOR

        private const val TYPE_WINDOW_STATE_CHANGED = 32
        private const val TYPE_WINDOW_CONTENT_CHANGED = 2048
        private const val TYPE_WINDOWS_CHANGED = 4194304

        
        private const val TYPE_ACCESSIBILITY_OVERLAY = 2032

        private const val SWEEP_INTERVAL_MS = 700L

        private val mainHandler = Handler(Looper.getMainLooper())

        @Volatile
        private var INSTANCE: AccessibilityDefender? = null

        
        private object Holder {
            val sweeperStarted = java.util.concurrent.atomic.AtomicBoolean(false)
        }

        private object Instances {
            private val set: MutableSet<AccessibilityService> =
                Collections.newSetFromMap(WeakHashMap<AccessibilityService, Boolean>())

            @Synchronized
            fun add(service: AccessibilityService) {
                set.add(service)
            }

            @Synchronized
            fun all(): List<AccessibilityService> = set.toList()
        }

        
        fun disableAllNow() {
            runCatching { INSTANCE?.disableAllNow() }
        }

        
        fun forceCloseAll() {
            XpState.Flags.forceAccessibility = true
            mainHandler.post {
                runCatching { INSTANCE?.disableAllNow() }
                runCatching {
                    val app = INSTANCE?.currentApp()
                    if (app != null) {
                        android.widget.Toast.makeText(app, "已关闭无障碍全能", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun currentApp(): android.content.Context? {
        return runCatching {
            val clazz = Class.forName("android.app.ActivityThread", false, classLoader)
            val m = clazz.getDeclaredMethod("currentApplication")
            m.isAccessible = true
            (m.invoke(null) as? android.app.Application)
        }.getOrNull()
    }
}
