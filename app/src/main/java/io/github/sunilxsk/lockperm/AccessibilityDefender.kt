package io.github.sunilxsk.lockperm

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap















internal class AccessibilityDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg0 = snapshot()
        
        
        
        
        
        hookServiceLifecycle()
        if (cfg0.accScope != XpConfig.ACC_SCOPE_CLOSE_ONLY) {
            installCapabilityHooks()
            hookAccessibilityManager()
            hookEventDelivery()
            hookServiceInfoIdentity()
            hookNodeProvider()
            hookInteractionClient()
            hookManagerBinder()
            hookNameAndSettingsBypass()
        }
        startWatchdog()
        val cfg = snapshot()
        logInfo(
            "accessibility defender installed (enable=${cfg.accEnable}, mode=${cfg.accMode}, " +
                "scope=${cfg.accScope}, fake=${cfg.accFakeMode}${if (fakeMode()) " 伪装成功" else " 默认"})"
        )
    }

    

    




    






    private fun statusOff(): Boolean {
        val cfg = snapshot()
        if (cfg.accStatusSpoof) return !cfg.accStatusValue
        
        if (fakeMode()) return false
        return shouldDisableContinuously() || XpState.Flags.forceAccessibility
    }

    













    private fun blankNode(): Any? =
        runCatching { android.view.accessibility.AccessibilityNodeInfo.obtain() }.getOrNull()

    







    private fun deniedNodeFor(m: Method): Any? {
        val t = m.returnType
        return when {
            t == android.view.accessibility.AccessibilityNodeInfo::class.java -> blankNode()
            t == android.os.Bundle::class.java -> android.os.Bundle()
            List::class.java.isAssignableFrom(t) ||
                java.util.Collection::class.java.isAssignableFrom(t) ->
                java.util.ArrayList<Any>()
            t == String::class.java || t == CharSequence::class.java -> ""
            else -> deniedFor(m)
        }
    }

    
    private fun fakeMode(): Boolean =
        snapshot().accFakeMode == XpConfig.ACC_MODE_FAKE_SUCCESS

    








    private fun successFor(m: Method): Any? {
        val t = m.returnType
        return when {
            t == android.view.accessibility.AccessibilityNodeInfo::class.java -> blankNode()
            t == java.lang.Boolean.TYPE -> true
            t == java.lang.Integer.TYPE -> 1
            t == java.lang.Long.TYPE -> 1L
            t == java.lang.Float.TYPE -> 1f
            t == java.lang.Double.TYPE -> 1.0
            t == java.lang.Short.TYPE -> 1.toShort()
            t == java.lang.Byte.TYPE -> 1.toByte()
            t == java.lang.Character.TYPE -> 0.toChar()
            t == String::class.java -> ""
            
            
            
            t == CharSequence::class.java -> ""
            t == Void.TYPE -> null
            t.isArray -> runCatching { java.lang.reflect.Array.newInstance(t.componentType, 0) }
                .getOrNull()
            List::class.java.isAssignableFrom(t) ||
                java.util.Collection::class.java.isAssignableFrom(t) ->
                java.util.ArrayList<Any>()
            android.util.SparseArray::class.java.isAssignableFrom(t) -> android.util.SparseArray<Any>()
            else -> null
        }
    }

    private fun hookAccessibilityManager() {
        val am = frameworkCls("android.view.accessibility.AccessibilityManager") ?: return

        
        am.declaredMethods.filter { it.name == "isEnabled" }.forEach { m ->
            hookMethod(m) { chain ->
                val cfg = snapshot()
                if (cfg.accStatusSpoof) {
                    if (!cfg.accStatusValue) forceDisabledFlags(chain.getThisObject())
                    cfg.accStatusValue
                } else if (fakeMode()) {
                    
                    
                    
                    
                    
                    true
                } else if (shouldDisableContinuously() || XpState.Flags.forceAccessibility) {
                    
                    forceDisabledFlags(chain.getThisObject())
                    false
                } else {
                    chain.proceed()
                }
            }
        }
        am.declaredMethods.filter { it.name == "isTouchExplorationEnabled" }.forEach { m ->
            hookMethod(m) { chain ->
                val cfg = snapshot()
                if (cfg.accStatusSpoof && !cfg.accStatusValue) {
                    forceDisabledFlags(chain.getThisObject())
                    false
                } else {
                    chain.proceed()
                }
            }
        }
        am.declaredMethods.filter { it.name == "getEnabledAccessibilityServiceList" }.forEach { m ->
            hookMethod(m) { chain ->
                val r = chain.proceed()
                if (statusOff()) java.util.ArrayList<Any>() else r
            }
        }

        
        
        
        am.declaredMethods.filter {
            it.name == "getInstalledAccessibilityServiceList" ||
                it.name == "getAccessibilityServiceList"
        }.forEach { m ->
            hookMethod(m) { chain ->
                val r = chain.proceed()
                if (statusOff()) java.util.ArrayList<Any>() else r
            }
        }

        
        am.declaredMethods.filter {
            it.name == "isAccessibilityButtonSupported" ||
                it.name == "isRequestFromAccessibilityTool"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (statusOff()) false else chain.proceed()
            }
        }
        
        
        am.declaredMethods.filter {
            it.name == "isHighContrastTextEnabled" || it.name == "isAudioDescriptionRequested"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (statusOff()) false else chain.proceed()
            }
        }
        
        
        am.declaredMethods.filter { it.name == "getRecommendedTimeoutMillis" }.forEach { m ->
            hookMethod(m) { chain ->
                if (!statusOff()) return@hookMethod chain.proceed()
                val original = chain.args.firstOrNull { it is Int } as? Int
                if (original != null) original else orProceed(chain, null)
            }
        }
        
        am.declaredMethods.filter {
            it.name == "getAccessibilityFocusColor" || it.name == "getAccessibilityFocusStrokeWidth"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (statusOff()) 0 else chain.proceed()
            }
        }
        
        
        am.declaredMethods.filter {
            it.name == "sendAccessibilityEvent" || it.name == "interrupt"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (statusOff()) null else chain.proceed()
            }
        }
        
        
        runCatching {
            am.declaredMethods.filter {
                it.name == "setState" || it.name == "setStateLocked" || it.name == "setEnabled"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    if (statusOff()) forceDisabledFlags(chain.getThisObject())
                    r
                }
            }
        }
        
        

        
        val sg = frameworkCls("android.provider.Settings\$Secure")
        if (sg != null) {
            runCatching {
                sg.declaredMethods.filter { it.name == "getString" }.forEach { m ->
                    hookMethod(m) { chain ->
                        val k = chain.getArg(1) as? String
                        if (statusOff() && k != null && isA11ySettingKey(k)) "" else chain.proceed()
                    }
                }
                
                
                sg.declaredMethods.filter { it.name == "getStringForUser" }.forEach { m ->
                    hookMethod(m) { chain ->
                        val k = chain.getArg(1) as? String
                        if (statusOff() && k != null && isA11ySettingKey(k)) "" else chain.proceed()
                    }
                }
                
                
                sg.declaredMethods.filter {
                    it.name == "getInt" || it.name == "getIntForUser"
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        val k = chain.args.filterIsInstance<String>().firstOrNull()
                        if (statusOff() && k != null && isA11ySettingKey(k)) 0 else chain.proceed()
                    }
                }
            }
        }
        logInfo("accessibility manager hook installed")
    }

    

    private fun isA11ySettingKey(k: String): Boolean {
        val s = k.lowercase()
        return s.contains("accessibility") || s.contains("touch_exploration") ||
            s.contains("enabled_accessibility")
    }

    




    private fun forceDisabledFlags(thiz: Any?) {
        val o = thiz ?: return
        var c: Class<*>? = o.javaClass
        while (c != null && c != Any::class.java) {
            for (name in listOf(
                "mIsEnabled", "mIsTouchExplorationEnabled",
                "mIsHighTextContrastEnabled", "mIsAudioDescriptionByDefaultEnabled",
            )) {
                runCatching {
                    val f = c!!.getDeclaredField(name)
                    f.isAccessible = true
                    if (f.type == java.lang.Boolean.TYPE) f.setBoolean(o, false)
                }
            }
            c = c.superclass
        }
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
            svc.declaredConstructors.forEach { c ->
                hookCtor(c) { chain ->
                    val r = chain.proceed()
                    val service = chain.getThisObject() as? AccessibilityService
                    if (service != null) {
                        Instances.add(service)
                        if (shouldDisableContinuously()) disableNow(service)
                    }
                    r
                }
            }
        }.onFailure { logWarn("accessibility: ctor hook skipped: ${it.message}") }

        
        
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
        
        if (cfg.accScope == XpConfig.ACC_SCOPE_HOOK_ONLY) return false
        
        if (oneShotPending()) return true
        
        return cfg.accEnable && (cfg.accMode == 0 || !cfg.exitEnable)
    }

    private fun shouldBlockEvent(event: AccessibilityEvent?): Boolean {
        
        
        if (fakeMode()) return false
        
        if (snapshot().accScope == XpConfig.ACC_SCOPE_CLOSE_ONLY) return false
        val cfg = snapshot()
        val force = XpState.Flags.forceAccessibility
        
        if (XpState.Flags.forceAccessibilityAll) return true
        if (!force && !cfg.accEnable) return false
        

        val type = event?.eventType ?: return false
        return when {
            
            cfg.accCapNotify && type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> true
            cfg.accCapNotify && type == TYPE_ANNOUNCEMENT -> true

            
            cfg.accCapWindow && type == TYPE_WINDOW_STATE_CHANGED -> true
            cfg.accCapWindow && type == TYPE_WINDOW_CONTENT_CHANGED -> true
            cfg.accCapWindow && type == TYPE_WINDOWS_CHANGED -> true
            cfg.accCapWindow && type == TYPE_VIEW_SCROLLED -> true
            cfg.accCapWindow && type == TYPE_VIEW_ACCESSIBILITY_FOCUSED -> true
            cfg.accCapWindow && type == TYPE_GESTURE_DETECTION_START -> true
            cfg.accCapWindow && type == TYPE_GESTURE_DETECTION_END -> true
            cfg.accCapWindow && type == TYPE_TOUCH_INTERACTION_START -> true
            cfg.accCapWindow && type == TYPE_TOUCH_INTERACTION_END -> true

            
            cfg.accCapInput && type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> true
            cfg.accCapInput && type == TYPE_VIEW_TEXT_SELECTION_CHANGED -> true
            cfg.accCapInput && type == TYPE_VIEW_TEXT_TRAVERSED -> true
            cfg.accCapInput && type == TYPE_VIEW_FOCUSED -> true
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

        
        blockOn(node, SCREEN_NODE_METHODS, capScreen, "AccessibilityNodeInfo", dataClass = true)
        blockOn(svc, setOf("getRootInActiveWindow", "findFocus"), capScreen, "AccessibilityService")
        blockOn(
            svc,
            setOf("findAccessibilityNodeInfosByText", "findAccessibilityNodeInfosByViewId"),
            capScreen,
            "AccessibilityService",
        )
        
        
        
        
        listOf("getBoundsInScreen", "getBoundsInWindow", "getRegionInScreen").forEach { n ->
            runCatching {
                node.declaredMethods.filter { it.name == n }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (active() && breakageEnabled() && capScreen()) {
                            val a = chain.getArg(0)
                            if (a is Rect) {
                                a.setEmpty()
                            } else if (a != null) {
                                runCatching { a.javaClass.getMethod("setEmpty").invoke(a) }
                            }
                            null
                        } else {
                            chain.proceed()
                        }
                    }
                }
            }
        }
        
        blockOn(
            frameworkCls("android.view.accessibility.AccessibilityInteractionClient"),
            INTERACTION_METHODS,
            capScreen,
            "AccessibilityInteractionClient",
        )

        
        
        
        
        
        blockOn(event, setOf("getText"), { capNotify() || capInput() }, "AccessibilityEvent", dataClass = true)
        blockOn(record, setOf("getText"), { capNotify() || capInput() }, "AccessibilityRecord", dataClass = true)
        blockOn(event, NOTIFY_METHODS - "getText", capNotify, "AccessibilityEvent", dataClass = true)
        blockOn(record, NOTIFY_METHODS - "getText", capNotify, "AccessibilityRecord", dataClass = true)

        
        blockOn(event, EVENT_WINDOW_METHODS, capWindow, "AccessibilityEvent", dataClass = true)
        blockOn(record, RECORD_WINDOW_METHODS, capWindow, "AccessibilityRecord", dataClass = true)
        blockOn(svc, setOf("getWindows", "getWindowsOnAllDisplays"), capWindow, "AccessibilityService")
        blockOn(winInfo, WINDOW_METHODS, capWindow, "AccessibilityWindowInfo", dataClass = true)

        
        blockOn(event, INPUT_METHODS - "getText", capInput, "AccessibilityEvent", dataClass = true)
        blockOn(record, INPUT_METHODS - "getText", capInput, "AccessibilityRecord", dataClass = true)

        
        
        
        
        
        
        blockOn(svc, OVERLAY_SERVICE_METHODS, capOverlay, "AccessibilityService")

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
            MAGNIFICATION_METHODS,
            capControl,
            "MagnificationController",
        )
        blockOn(
            frameworkCls("android.accessibilityservice.AccessibilityService\$SoftKeyboardController"),
            SOFT_KEYBOARD_METHODS,
            capControl,
            "SoftKeyboardController",
        )
        blockOn(
            frameworkCls("android.accessibilityservice.FingerprintGestureController"),
            FINGERPRINT_METHODS,
            capControl,
            "FingerprintGestureController",
        )
        blockOn(
            frameworkCls("android.accessibilityservice.AccessibilityButtonController"),
            BUTTON_CONTROLLER_METHODS,
            capControl,
            "AccessibilityButtonController",
        )
        blockOn(
            frameworkCls("android.accessibilityservice.BrailleDisplayController"),
            BRAILLE_METHODS,
            capControl,
            "BrailleDisplayController",
        )



        hookSetServiceInfo(svc)
    }

    
    
    
    
    

    




    private fun hookServiceInfoIdentity() {
        val info = frameworkCls("android.accessibilityservice.AccessibilityServiceInfo") ?: return
        val off: () -> Boolean = { active() && breakageEnabled() && capOn(snapshot().accCapScreen) }
        info.declaredMethods.filter {
            it.name == "getId" || it.name == "getResolveInfo" ||
                it.name == "getComponentName" || it.name == "getSettingsActivityName"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (off()) deniedFor(m) else chain.proceed()
            }
        }
        info.declaredMethods.filter {
            it.name == "getCapabilities" || it.name == "getEventTypes" ||
                it.name == "getFeedbackType" || it.name == "getFlags"
        }.forEach { m ->
            hookMethod(m) { chain ->
                if (off()) 0 else chain.proceed()
            }
        }
        runCatching {
            info.declaredMethods.filter { it.name == "getPackageNames" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (off()) emptyArray<String>() else chain.proceed()
                }
            }
        }
        logInfo("accessibility service info hook installed")
    }

    



    private fun hookNodeProvider() {
        val p = frameworkCls("android.view.accessibility.AccessibilityNodeProvider") ?: return
        val off: () -> Boolean = { active() && breakageEnabled() && capOn(snapshot().accCapScreen) }
        p.declaredMethods.filter {
            it.name == "createAccessibilityNodeInfo" ||
                it.name == "findAccessibilityNodeInfosByText" ||
                it.name == "findFocus"
        }.forEach { m ->
            hookMethod(m) { chain ->
                
                
                if (off()) {
                    if (fakeMode()) successFor(m) else deniedNodeFor(m)
                } else {
                    chain.proceed()
                }
            }
        }
        p.declaredMethods.filter { it.name == "performAction" }.forEach { m ->
            hookMethod(m) { chain ->
                if (off()) false else chain.proceed()
            }
        }
        logInfo("accessibility node provider hook installed")
    }

    




    private fun hookInteractionClient() {
        val c = frameworkCls("android.view.accessibility.AccessibilityInteractionClient") ?: return
        val off: () -> Boolean = { active() && breakageEnabled() && capOn(snapshot().accCapScreen) }
        INTERACTION_METHODS.forEach { name ->
            c.declaredMethods.filter { it.name == name }.forEach { m ->
                hookMethod(m) { chain ->
                    if (off()) deniedNodeFor(m) else chain.proceed()
                }
            }
        }
        c.declaredMethods.filter { it.name == "getConnection" }.forEach { m ->
            hookMethod(m) { chain ->
                if (off()) null else chain.proceed()
            }
        }
        
        
        c.declaredMethods.filter { it.name == "addConnection" }.forEach { m ->
            hookMethod(m) { chain ->
                if (off() && capOn(snapshot().accCapControl)) {
                    logWarn("blocked AccessibilityInteractionClient.addConnection")
                    null
                } else {
                    chain.proceed()
                }
            }
        }
        logInfo("accessibility interaction client hook installed")
    }

    




    private fun hookManagerBinder() {
        val stub = frameworkCls("android.view.accessibility.IAccessibilityManager\$Stub")
            ?: frameworkCls("android.view.accessibility.IAccessibilityManager")
        if (stub != null) {
            val off: () -> Boolean = { statusOff() }
            stub.declaredMethods.filter { it.name == "asInterface" }.forEach { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed() ?: return@hookMethod null
                    if (!off()) return@hookMethod r
                    runCatching { wrapManagerBinder(r) }.getOrDefault(r)
                }
            }
        } else {
            logWarn("binder hook: IAccessibilityManager not found, skipped")
        }
        logInfo("accessibility manager binder hook installed")
    }

    




    private fun wrapManagerBinder(original: Any): Any {
        val iface = runCatching {
            Class.forName("android.view.accessibility.IAccessibilityManager", false, classLoader)
        }.getOrNull() ?: return original
        return java.lang.reflect.Proxy.newProxyInstance(
            classLoader, arrayOf(iface),
        ) javaProxy@{ _, method, args ->
            val name = method.name
            when {
                name == "getEnabledAccessibilityServiceList" ||
                    name == "getInstalledAccessibilityServiceList" ->
                    java.util.ArrayList<Any>()
                name == "isAudioDescriptionByDefaultEnabled" -> java.lang.Boolean.FALSE
                name == "getAccessibilityShortcutTargets" -> java.util.ArrayList<Any>()
                name == "sendFingerprintGesture" -> java.lang.Boolean.FALSE
                name == "getRecommendedTimeoutMillis" ->
                    args?.firstOrNull { it is Int } as? Int ?: 0
                name == "sendAccessibilityEvent" || name == "interrupt" -> null
                else -> runCatching { method.invoke(original, *(args ?: emptyArray())) }
                    .getOrNull()
            }
        }
    }

    




    private fun hookNameAndSettingsBypass() {
        val pm = frameworkCls("android.app.ApplicationPackageManager")
            ?: frameworkCls("android.content.pm.PackageManager")
        if (pm != null) {
            val off: () -> Boolean = { statusOff() }
            val isA11yAction = { intent: Any? ->
                runCatching {
                    val m = intent?.javaClass?.getMethod("getAction")
                    val a = m?.invoke(intent) as? String
                    a != null && a.contains("accessibilityservice", ignoreCase = true)
                }.getOrDefault(false)
            }
            pm.declaredMethods.filter {
                it.name == "queryIntentServices" || it.name == "queryIntentServicesAsUser"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    if (!off() || r == null) return@hookMethod r
                    if (!isA11yAction(chain.args.firstOrNull { it?.javaClass?.name?.contains("Intent") == true })) {
                        return@hookMethod r
                    }
                    logWarn("blocked PackageManager.${m.name} (accessibility service scan)")
                    
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        (r as MutableList<Any?>).clear()
                    }
                    r
                }
            }
        }
        logInfo("accessibility package manager bypass hook installed")
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
                        (cfg.accEnable && cfg.accCapOverlay &&
                            cfg.accScope != XpConfig.ACC_SCOPE_CLOSE_ONLY)
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
        
        return cfg.accScope != XpConfig.ACC_SCOPE_CLOSE_ONLY
    }

    

    








    private object Watchdog {
        @Volatile
        var running = false
        val lock = Any()
    }

    






    private fun startWatchdog() {
        
        synchronized(Watchdog.lock) {
            if (Watchdog.running) return
            Watchdog.running = true
        }
        Thread {
            var round = 0
            var idle = 0
            while (true) {
                runCatching {
                    
                    
                    val stopNow = !shouldDisableContinuously() &&
                            !XpState.Flags.forceAccessibility && !oneShotPending()
                    if (stopNow) {
                        idle++
                        if (idle >= 10) {
                            synchronized(Watchdog.lock) { Watchdog.running = false }
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
        dataClass: Boolean = false,
    ) {
        val c = clazz ?: return
        c.declaredMethods.filter { it.name in names }.forEach { m ->
            hookMethod(m) { chain ->
                if (active() && breakageEnabled() && cap()) {
                    if (fakeMode()) {
                        if (dataClass) {
                            
                            chain.proceed()
                        } else {
                            logWarn("fake success $tag.${m.name}")
                            successFor(m)
                        }
                    } else {
                        logWarn("blocked $tag.${m.name}")
                        
                        deniedNodeFor(m)
                    }
                } else {
                    chain.proceed()
                }
            }
        }
    }

    

    







    fun disableAllNow() {
        
        
        if (snapshot().accScope == XpConfig.ACC_SCOPE_HOOK_ONLY) {
            logInfo("disableAllNow skipped: 运行方式为只运行钩子")
            return
        }
        
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
            
            "findFocus", "focusSearch", "findAccessibilityNodeInfosByViewIdUiThread",
            "getAvailableExtraData", "getExtraRenderingData", "refreshWithExtraData",
            "isLongClickable", "isCheckable", "isAccessibilityDataSensitive",
            
            "getContainerTitle", "getExtraRenderingInfo", "isTextEntryKey",
            "getBoundsInParent", "getTextSelectionEnd", "getMinDurationBetweenContentChanges",
        )

        
        private val INTERACTION_METHODS: Set<String> = setOf(
            "findAccessibilityNodeInfoByAccessibilityId",
            "findAccessibilityNodeInfoByAccessibilityIdUiThread",
            "findAccessibilityNodeInfosByText",
            "findAccessibilityNodeInfosByViewId",
            "findAccessibilityNodeInfosByTextUiThread",
            "findAccessibilityNodeInfosByViewIdUiThread",
            "findFocus", "findFocusUiThread",
            "performAccessibilityAction",
            
            "getWindows", "getWindowsOnAllDisplays", "getWindow", "clearCache",
            
            "focusSearch", "getRootInActiveWindow", "clearAccessibilityCache",
        )

        
        private val NOTIFY_METHODS: Set<String> = setOf(
            "getText", "getContentDescription", "getParcelableData",
            "getItemCount", "getCurrentItemIndex", "getFromIndex", "getToIndex",
            
            "getTextChangeTypes", "getSpeechStateChangeTypes", "getClassName",
        )


        private val INPUT_METHODS: Set<String> = setOf(
            "getText", "getBeforeText",
            
            "getInputType", "getLiveRegion", "getMaxTextLength", "getHintText",
        )


        private val EVENT_WINDOW_METHODS: Set<String> = setOf(
            "getSource", "getEventType", "getAction",
            "getMovementGranularity", "getContentChangeTypes", "getDisplayId",
            
            "getWindowChanges", "getTextChangeTypes", "getSpeechStateChangeTypes",
            
            "getRecord", "getRecordCount",
            
            "getWindow",
        )

        




        private val RECORD_WINDOW_METHODS: Set<String> = setOf(
            "getClassName", "getPackageName", "getWindowId",
            "getSource", "getSourceNodeId", "getText", "getContentDescription",
            "getBeforeText", "getFromIndex", "getToIndex",
            "getItemCount", "getCurrentItemIndex",
            "isScrollable", "getScrollX", "getScrollY",
            "getMaxScrollX", "getMaxScrollY", "getScrollDeltaX", "getScrollDeltaY",
            "getCollectionInfo", "getCollectionItemInfo",
            "getContentChangeTypes", "getMovementGranularity", "getTextChangeTypes",
        )

        
        private val WINDOW_METHODS: Set<String> = setOf(
            "getRoot", "getChild", "getTitle", "getId", "getLayer", "getType",
            "isActive", "isFocused", "isAccessibilityFocused", "getParent", "getAnchor",
            "getDisplayId", "getRegionInScreen",
            
            "getChildCount", "getBoundsInScreen", "getControlledWindow",
            "getControlledWindowsCount", "getControllingWindow",
            "getLocales", "getTransitionTimeMillis", "isInPictureInPictureMode",
            "refresh",
        )

        
        private val OVERLAY_SERVICE_METHODS: Set<String> = setOf(
            "attachAccessibilityOverlayToDisplay", "attachAccessibilityOverlayToWindow",
            "detachAccessibilityOverlay",
        )

        
        private val CONTROL_SERVICE_METHODS: Set<String> = setOf(
            "takeScreenshot", "takeScreenshotOfWindow",
            "setGestureDetectionPassthroughRegion", "setTouchExplorationPassthroughRegion",
            "setAnimationScale", "setAccessibilityFocusAppearance", "setCacheEnabled",
            "getSystemActions", "getTouchInteractionController", "getInputMethod",
            "onCreateInputMethod", "getAccessibilityButtonController",
            "getMagnificationController", "getSoftKeyboardController",
            "getFingerprintGestureController",
            
            "getAccessibilityFocusAppearance", "clearCache", "clearCachedSubtree",
            "isNodeInCache", "isCacheEnabled", "getServiceInfo",
            "getBrailleDisplayController",
        )

        
        private val MAGNIFICATION_METHODS: Set<String> = setOf(
            "setMagnificationScale", "setScale", "setCenter", "setMagnificationConfig",
            "reset", "resetCurrentMagnification", "getScale", "getCenterX", "getCenterY",
            "getMagnificationRegion", "getCurrentMagnificationRegion", "getMagnificationConfig",
            "addListener", "removeListener",
            
            "isActivated", "isMagnifying",
        )

        
        private val SOFT_KEYBOARD_METHODS: Set<String> = setOf(
            "setShowSoftKeyboard", "showSoftKeyboard", "setSoftKeyboardShowMode",
            "setShowMode", "getShowMode", "setInputMethodEnabled", "switchToInputMethod",
            "addOnShowModeChangedListener", "removeOnShowModeChangedListener",
        )

        private val FINGERPRINT_METHODS: Set<String> = setOf(
            "dispatchFingerprintGesture", "isGestureDetectionAvailable",
            "registerFingerprintGestureCallback", "unregisterFingerprintGestureCallback",
        )

        private val BUTTON_CONTROLLER_METHODS: Set<String> = setOf(
            "isAccessibilityButtonAvailable",
            "registerAccessibilityButtonCallback", "unregisterAccessibilityButtonCallback",
        )

        
        private val BRAILLE_METHODS: Set<String> = setOf(
            "connect", "disconnect", "write", "isConnected",
        )

        



        private val CONTROL_CALLBACKS: Set<String> = setOf(
            "onKeyEvent", "onGesture", "onMotionEvent", "onSystemActionsChanged",
            "onMagnificationChanged", "onSoftKeyboardShowModeChanged",
            "onFingerprintCapturingGesturesChanged", "onFingerprintGesture",
            "onAccessibilityButtonClicked", "onAccessibilityButtonAvailabilityChanged",
            "onPerformGestureResult", "init",
            
            "onTouchInteractionStart", "onTouchInteractionEnd",
            "onCreateInputMethod", "onAccessibilityInputConnectionCreated",
            
            
            "onTouchStateChanged", "createImeSession", "startInput",
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

        
        private const val TYPE_VIEW_FOCUSED = 8
        private const val TYPE_VIEW_SCROLLED = 4096
        private const val TYPE_VIEW_TEXT_SELECTION_CHANGED = 8192
        private const val TYPE_ANNOUNCEMENT = 16384
        private const val TYPE_VIEW_ACCESSIBILITY_FOCUSED = 32768
        private const val TYPE_VIEW_TEXT_TRAVERSED = 131072
        private const val TYPE_GESTURE_DETECTION_START = 262144
        private const val TYPE_GESTURE_DETECTION_END = 524288
        private const val TYPE_TOUCH_INTERACTION_START = 1048576
        private const val TYPE_TOUCH_INTERACTION_END = 2097152

        
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
