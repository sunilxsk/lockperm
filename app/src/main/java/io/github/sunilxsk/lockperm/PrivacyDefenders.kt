package io.github.sunilxsk.lockperm

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import io.github.libxposed.api.XposedModule
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteOrder
import java.util.Collections












internal class JumpDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockJumpEnable) return
        hookJump()
        logInfo("jump defender installed")
    }

    
    fun installNow() {
        XpState.Flags.forceJump = true
        hookJump()
    }

    private fun on(): Boolean =
        XpState.Flags.forceJump || snapshot().blockJumpEnable

    private fun hookJump() {
        listOf(
            "android.app.Activity",
            "android.app.ContextImpl",
            "android.content.ContextWrapper",
        ).forEach { className ->
            val c = frameworkCls(className) ?: return@forEach
            c.declaredMethods.filter { it.name in START_METHODS }.forEach { m ->
                hookMethod(m) { chain ->
                    val intent = findIntent(chain)
                    if (intent != null && !allowed(intent)) {
                        logWarn("blocked jump: ${describe(intent)}")
                        return@hookMethod null
                    }
                    chain.proceed()
                }
            }
        }
        
        val inst = frameworkCls("android.app.Instrumentation")
        inst?.declaredMethods?.filter { it.name == "execStartActivity" }?.forEach { m ->
            hookMethod(m) { chain ->
                val intent = runCatching { chain.getArg(2) as? Intent }.getOrNull()
                if (intent != null && !allowed(intent)) {
                    logWarn("blocked jump (Instrumentation): ${describe(intent)}")
                    return@hookMethod null
                }
                chain.proceed()
            }
        }

        
        runCatching {
            val pi = frameworkCls("android.app.PendingIntent")
            pi?.declaredMethods?.filter {
                it.name == "send" || it.name == "sendAndReturnResult"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    
                    runCatching {
                        val self = chain.getThisObject() ?: return@runCatching
                        
                        val f = runCatching {
                            self.javaClass.getDeclaredField("intent")
                        }.getOrNull()
                        f?.isAccessible = true
                        val inner = runCatching { f?.get(self) as? Intent }.getOrNull()
                            ?: return@runCatching
                        if (!allowed(inner)) {
                            logWarn("blocked jump (PendingIntent): ${describe(inner)}")
                            return@hookMethod null
                        }
                    }
                    chain.proceed()
                }
            }
        }
    }

    private fun allowed(intent: Intent): Boolean {
        if (!on()) return true
        val target = targetPkg(intent)
        
        
        if (!target.isNullOrBlank() && target == XpState.packageName) return true
        if (target.isNullOrBlank()) return false
        val white = snapshot().jumpWhitelist
        
        if (white.isEmpty()) return false
        return target in white
    }

    private fun targetPkg(intent: Intent): String? {
        intent.component?.packageName?.let { return it }
        intent.`package`?.let { return it }
        val data = intent.dataString
        if (!data.isNullOrBlank()) {
            val m = Regex("""(?:package:|market\?id=|details\?id=)([a-zA-Z0-9_.\-]+)""").find(data)
            m?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }

    private fun describe(intent: Intent): String =
        "${intent.action} -> ${targetPkg(intent) ?: "(未知)"}"

    private fun findIntent(chain: io.github.libxposed.api.XposedInterface.Chain): Intent? {
        val args = runCatching { chain.args }.getOrNull() ?: return null
        args.filterIsInstance<Intent>().firstOrNull()?.let { return it }
        val arr = args.firstOrNull { it is Array<*> } as? Array<*> ?: return null
        return arr.filterIsInstance<Intent>().firstOrNull()
    }

    companion object {
        private val START_METHODS = setOf(
            "startActivity", "startActivityForResult", "startActivities",
            "startActivityIfNeeded", "startActivityFromChild",
        )
    }
}























internal class BackgroundLaunchDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private fun on(): Boolean = XpState.Flags.forceBgLaunch || snapshot().blockBgLaunch

    private fun strict(): Boolean = snapshot().blockBgLaunchStrict

    fun install() {
        if (!on()) return
        trackForeground()
        hookStarts()
        hookPendingIntent()
        logInfo("background launch defender installed")
    }

    fun installNow() {
        XpState.Flags.forceBgLaunch = true
        trackForeground()
        hookStarts()
        hookPendingIntent()
    }

    
    
    
    private fun trackForeground() {
        if (tracked) return
        runCatching {
            val app = runCatching { appContext() }.getOrNull() as? android.app.Application
                ?: return@runCatching
            app.registerActivityLifecycleCallbacks(
                object : android.app.Application.ActivityLifecycleCallbacks {
                    override fun onActivityResumed(a: android.app.Activity) {
                        resumed++
                    }

                    override fun onActivityPaused(a: android.app.Activity) {
                        if (resumed > 0) resumed--
                    }

                    override fun onActivityCreated(
                        a: android.app.Activity,
                        b: android.os.Bundle?,
                    ) = Unit

                    override fun onActivityStarted(a: android.app.Activity) = Unit
                    override fun onActivityStopped(a: android.app.Activity) = Unit
                    override fun onActivitySaveInstanceState(
                        a: android.app.Activity,
                        b: android.os.Bundle,
                    ) = Unit

                    override fun onActivityDestroyed(a: android.app.Activity) = Unit
                },
            )
            tracked = true
        }
    }

    
    
    
    
    private fun importance(): Int? {
        val ctx = runCatching { appContext() }.getOrNull() ?: return null
        val am = runCatching {
            ctx.getSystemService(android.content.Context.ACTIVITY_SERVICE)
                as? android.app.ActivityManager
        }.getOrNull() ?: return null
        val pid = android.os.Process.myPid()
        val list = runCatching { am.runningAppProcesses }.getOrNull() ?: return null
        for (r in list) {
            if (r.pid == pid) return r.importance
        }
        return null
    }

    private fun background(): Boolean {
        if (resumed > 0) return false
        val imp = importance() ?: return false
        val threshold = if (strict()) {
            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } else {
            IMP_FOREGROUND_SERVICE
        }
        return imp > threshold
    }

    
    
    
    private fun hookStarts() {
        listOf(
            "android.app.Activity",
            "android.app.ContextImpl",
            "android.content.ContextWrapper",
        ).forEach { className ->
            runCatching {
                val c = frameworkCls(className) ?: return@runCatching
                c.declaredMethods.filter { it.name in START_METHODS }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on() || !background()) return@hookMethod chain.proceed()
                        logWarn("blocked background launch: $className.${m.name}")
                        null
                    }
                }
            }
        }

        runCatching {
            val inst = frameworkCls("android.app.Instrumentation") ?: return@runCatching
            inst.declaredMethods.filter { it.name == "execStartActivity" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on() || !background()) return@hookMethod chain.proceed()
                    logWarn("blocked background launch: Instrumentation.execStartActivity")
                    null
                }
            }
        }
    }

    
    
    
    
    private fun hookPendingIntent() {
        runCatching {
            val pi = frameworkCls("android.app.PendingIntent") ?: return@runCatching

            
            pi.declaredMethods.filter {
                it.name == "getActivity" || it.name == "getActivities" ||
                    it.name == "getActivityAsUser" || it.name == "getActivitiesAsUser"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    if (r is android.app.PendingIntent) {
                        runCatching { activityPis.add(r) }
                    }
                    r
                }
            }

            pi.declaredMethods.filter {
                it.name == "send" || it.name == "sendAndReturnResult"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on() || !snapshot().blockBgLaunchPending) {
                        return@hookMethod chain.proceed()
                    }
                    if (!background()) return@hookMethod chain.proceed()
                    val self = runCatching { chain.getThisObject() }.getOrNull()
                        as? android.app.PendingIntent
                    if (self != null && !isActivityPi(self)) return@hookMethod chain.proceed()
                    logWarn("blocked background launch: PendingIntent.${m.name}")
                    null
                }
            }
        }
    }

    
    private fun isActivityPi(pi: android.app.PendingIntent): Boolean {
        runCatching {
            val r = pi.javaClass.getMethod("isActivity").invoke(pi)
            if (r is Boolean) return r
        }
        return activityPis.contains(pi)
    }

    companion object {
        @Volatile
        private var resumed = 0

        @Volatile
        private var tracked = false

        
        private val activityPis: MutableSet<android.app.PendingIntent> =
            java.util.Collections.synchronizedSet(
                java.util.Collections.newSetFromMap(
                    java.util.WeakHashMap<android.app.PendingIntent, Boolean>(),
                ),
            )

        private const val IMP_FOREGROUND_SERVICE = 125

        private val START_METHODS = setOf(
            "startActivity", "startActivityForResult", "startActivities",
            "startActivityIfNeeded", "startActivityFromChild",
            "startActivityFromFragment",
        )
    }
}









internal class CameraMicDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (snapshot().blockCamera) hookCamera()
        if (snapshot().blockMic) hookMic()
        logInfo("camera/mic defender installed")
    }

    fun installNow() {
        XpState.Flags.forceCameraMic = true
        hookCamera()
        hookMic()
    }

    private fun cameraOn(): Boolean =
        XpState.Flags.forceCameraMic || snapshot().blockCamera

    private fun micOn(): Boolean =
        XpState.Flags.forceCameraMic || snapshot().blockMic

    private fun hookCamera() {
        
        runCatching {
            val cm = frameworkCls("android.hardware.camera2.CameraManager")
            cm?.declaredMethods?.filter { it.name == "openCamera" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!cameraOn()) return@hookMethod chain.proceed()
                    logWarn("blocked CameraManager.openCamera")
                    
                    throw android.hardware.camera2.CameraAccessException(
                        android.hardware.camera2.CameraAccessException.CAMERA_DISABLED
                    )
                }
            }
        }

        
        runCatching {
            val c1 = frameworkCls("android.hardware.Camera")
            c1?.declaredMethods?.filter { it.name == "open" || it.name == "openLegacy" }
                ?.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!cameraOn()) return@hookMethod chain.proceed()
                        logWarn("blocked Camera.open -> null")
                        null
                    }
                }
        }

        
        runCatching {
            val cd = frameworkCls("android.hardware.camera2.CameraDevice")
            cd?.declaredMethods?.filter { it.name == "createCaptureSession" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!cameraOn()) return@hookMethod chain.proceed()
                    logWarn("blocked CameraDevice.createCaptureSession")
                    deniedFor(m)
                }
            }
        }
    }

    private fun hookMic() {
        
        runCatching {
            val ar = frameworkCls("android.media.AudioRecord")
            ar?.declaredMethods?.filter { it.name == "getRecordingState" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!micOn()) return@hookMethod chain.proceed()
                    1 
                }
            }
            ar?.declaredMethods?.filter { it.name == "startRecording" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!micOn()) return@hookMethod chain.proceed()
                    logWarn("blocked AudioRecord.startRecording")
                    deniedFor(m)
                }
            }
            
            ar?.declaredMethods?.filter { it.name == "getState" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!micOn()) return@hookMethod chain.proceed()
                    STATE_UNINITIALIZED
                }
            }
        }

        
        runCatching {
            val mr = frameworkCls("android.media.MediaRecorder")
            mr?.declaredMethods?.filter { it.name == "start" || it.name == "prepare" }
                ?.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!micOn()) return@hookMethod chain.proceed()
                        logWarn("blocked MediaRecorder.${m.name}")
                        deniedFor(m)
                    }
                }
        }
    }

    companion object {
        private const val STATE_UNINITIALIZED = 0
    }
}











internal class InstallDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockInstall) return
        hookInstaller()
        hookPolicy()
        hookIntents()
        logInfo("install defender installed")
    }

    fun installNow() {
        XpState.Flags.forceInstall = true
        hookInstaller()
        hookPolicy()
        hookIntents()
    }

    private fun on(): Boolean = XpState.Flags.forceInstall || snapshot().blockInstall

    private fun hookInstaller() {
        runCatching {
            val pi = frameworkCls("android.content.pm.PackageInstaller")
            pi?.declaredMethods?.filter {
                it.name == "createSession" || it.name == "openSession" ||
                        it.name == "installExistingPackage" || it.name == "updateSessionAppIcon"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    logWarn("blocked PackageInstaller.${m.name}")
                    deniedFor(m)
                }
            }
            
            val sess = frameworkCls("android.content.pm.PackageInstaller\$Session")
            sess?.declaredMethods?.filter {
                it.name == "commit" || it.name == "openWrite" || it.name == "openRead" ||
                        it.name == "write" || it.name == "fsync" || it.name == "transfer"
            }?.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on()) return@hookMethod chain.proceed()
                        logWarn("blocked PackageInstaller.Session.${m.name}")
                        deniedFor(m)
                    }
                }
        }
    }

    private fun hookPolicy() {
        
        runCatching {
            val pm = frameworkCls("android.content.pm.PackageManager")
            pm?.declaredMethods?.filter {
                it.name == "installExistingPackage" || it.name == "installExistingPackageAsUser"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    logWarn("blocked PackageManager.${m.name}")
                    deniedFor(m)
                }
            }
        }
        runCatching {
            val dpm = frameworkCls("android.app.admin.DevicePolicyManager")
            dpm?.declaredMethods?.filter {
                it.name == "installExistingPackage" || it.name == "installSystemUpdate" ||
                        it.name == "installCaCert"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    logWarn("blocked DevicePolicyManager.${m.name}")
                    deniedFor(m)
                }
            }
        }
    }

    private fun hookIntents() {
        val targets = setOf(
            Intent.ACTION_VIEW,
            Intent.ACTION_INSTALL_PACKAGE,
            "android.intent.action.INSTALL_PACKAGE",
        )
        listOf(
            "android.app.Activity",
            "android.app.ContextImpl",
            "android.content.ContextWrapper",
        ).forEach { className ->
            val c = frameworkCls(className) ?: return@forEach
            c.declaredMethods.filter { it.name in START_METHODS }.forEach { m ->
                hookMethod(m) { chain ->
                    val intent = findIntent(chain) ?: return@hookMethod chain.proceed()
                    if (!on()) return@hookMethod chain.proceed()
                    if (isInstallIntent(intent, targets)) {
                        logWarn("blocked install intent: ${intent.action}")
                        return@hookMethod null
                    }
                    chain.proceed()
                }
            }
        }
        val inst = frameworkCls("android.app.Instrumentation")
        inst?.declaredMethods?.filter { it.name == "execStartActivity" }?.forEach { m ->
            hookMethod(m) { chain ->
                val intent = runCatching { chain.getArg(2) as? Intent }.getOrNull()
                    ?: return@hookMethod chain.proceed()
                if (on() && isInstallIntent(intent, targets)) {
                    logWarn("blocked install intent (Instrumentation)")
                    return@hookMethod null
                }
                chain.proceed()
            }
        }
    }

    private fun isInstallIntent(intent: Intent, targets: Set<String>): Boolean {
        val action = intent.action
        val type = intent.type
        val isApkType = type == "application/vnd.android.package-archive"
        val isApkAction = action in targets && (isApkType || action == Intent.ACTION_INSTALL_PACKAGE ||
                action == "android.intent.action.INSTALL_PACKAGE")
        if (isApkAction) return true
        
        val data = intent.dataString
        if (!data.isNullOrBlank() && data.lowercase().endsWith(".apk")) return true
        return false
    }

    private fun findIntent(chain: io.github.libxposed.api.XposedInterface.Chain): Intent? {
        val args = runCatching { chain.args }.getOrNull() ?: return null
        args.filterIsInstance<Intent>().firstOrNull()?.let { return it }
        val arr = args.firstOrNull { it is Array<*> } as? Array<*> ?: return null
        return arr.filterIsInstance<Intent>().firstOrNull()
    }

    companion object {
        private val START_METHODS = setOf(
            "startActivity", "startActivityForResult", "startActivities",
            "startActivityIfNeeded", "startActivityFromChild",
        )
    }
}










internal class PrintCastDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockPrintCast) return
        hookPrint()
        hookRouter()
        logInfo("print/cast defender installed")
    }

    fun installNow() {
        XpState.Flags.forcePrintCast = true
        hookPrint()
        hookRouter()
    }

    private fun on(): Boolean = XpState.Flags.forcePrintCast || snapshot().blockPrintCast

    private fun hookPrint() {
        runCatching {
            val pm = frameworkCls("android.print.PrintManager")
            pm?.declaredMethods?.filter { it.name == "print" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    logWarn("blocked PrintManager.print")
                    deniedFor(m)
                }
            }
            
            val job = frameworkCls("android.print.PrintJob")
            job?.declaredMethods?.filter { it.name == "start" || it.name == "restart" }
                ?.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on()) return@hookMethod chain.proceed()
                        logWarn("blocked PrintJob.${m.name}")
                        deniedFor(m)
                    }
                }
        }
    }

    private fun hookRouter() {
        runCatching {
            val mr = frameworkCls("android.media.MediaRouter")
            mr?.declaredMethods?.filter {
                it.name == "selectRoute" || it.name == "selectRouteInt" ||
                        it.name == "addCallback" || it.name == "setSelectedRoute"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    logWarn("blocked MediaRouter.${m.name}")
                    deniedFor(m)
                }
            }
            
            listOf(
                "androidx.mediarouter.media.MediaRouter",
                "android.support.v7.media.MediaRouter",
            ).forEach { name ->
                val c = cls(name) ?: return@forEach
                c.declaredMethods.filter {
                    it.name == "select" || it.name == "addCallback" ||
                            it.name == "unselect" || it.name == "setMediaSession"
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on()) return@hookMethod chain.proceed()
                        logWarn("blocked $name.${m.name}")
                        deniedFor(m)
                    }
                }
            }
        }
    }
}








internal class NotificationDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().blockNotify) return
        hookNotify()
        logInfo("notification defender installed")
    }

    fun installNow() {
        XpState.Flags.forceNotify = true
        hookNotify()
    }

    private fun on(): Boolean = XpState.Flags.forceNotify || snapshot().blockNotify

    private fun hookNotify() {
        val names = listOf(
            "android.app.NotificationManager",
            "androidx.core.app.NotificationManagerCompat",
            "android.support.v4.app.NotificationManagerCompat",
        )
        names.forEach { name ->
            val c = (if (name.startsWith("android.")) frameworkCls(name) else cls(name))
                ?: return@forEach
            c.declaredMethods.filter {
                it.name == "notify" || it.name == "notifyAsPackage" ||
                        it.name == "notifyAsUser" ||
                        it.name == "enqueueNotificationWithTag" ||
                        it.name == "createNotificationChannel" ||
                        it.name == "createNotificationChannelGroup" ||
                        it.name == "createNotificationChannels" ||
                        it.name == "createNotificationChannelGroups"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    logWarn("blocked $name.${m.name}")
                    deniedFor(m)
                }
            }
        }
    }
}













internal class NetworkFilter(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        if (!snapshot().netFilterEnable) return
        hookUrl()
        hookOkHttp()
        logInfo("network filter installed")
    }

    
    fun installNow() {
        XpState.Flags.forceNetFilter = true
        hookUrl()
        hookOkHttp()
    }

    private fun blocked(url: String?): Boolean {
        val cfg = snapshot()
        if (!XpState.Flags.forceNetFilter && !cfg.netFilterEnable) return false
        val host = hostOf(url) ?: return false
        val list = cfg.netFilterList
        
        
        if (list.isEmpty()) return XpState.Flags.forceNetFilter
        val hit = list.any { h ->
            host == h || host.endsWith(".$h")
        }
        return if (cfg.netFilterWhitelist) !hit else hit
    }

    private fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val s = url.lowercase()
            val idx = s.indexOf("://")
            val rest = if (idx >= 0) s.substring(idx + 3) else s
            val end = rest.indexOfFirst { it == '/' || it == ':' || it == '?' }
            val host = if (end >= 0) rest.substring(0, end) else rest
            host.substringAfter('@')
        }.getOrNull()
    }

    
    private fun urlOf(args: List<Any?>): String? {
        args.filterIsInstance<java.net.URL>().firstOrNull()?.let { return it.toString() }
        args.filterIsInstance<String>().firstOrNull { it.startsWith("http") }?.let { return it }
        
        return null
    }

    




    private fun hookDns() {
        runCatching {
            val ia = frameworkCls("java.net.InetAddress")
            ia?.declaredMethods?.filter {
                it.name == "getByName" || it.name == "getAllByName"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    val host = runCatching {
                        chain.args?.filterIsInstance<String>()?.firstOrNull()
                    }.getOrNull()
                    if (blocked(host)) {
                        logWarn("blocked dns: $host")
                        throw java.net.UnknownHostException("blocked by LockPerm: $host")
                    }
                    chain.proceed()
                }
            }
        }
        
        runCatching {
            val sk = frameworkCls("java.net.Socket")
            sk?.declaredMethods?.filter { it.name == "connect" }?.forEach { m ->
                hookMethod(m) { chain ->
                    val host = runCatching {
                        val a = chain.args?.filterIsInstance<java.net.InetSocketAddress>()?.firstOrNull()
                        a?.hostString
                    }.getOrNull()
                    if (blocked(host)) {
                        logWarn("blocked socket: $host")
                        throw java.io.IOException("blocked by LockPerm: $host")
                    }
                    chain.proceed()
                }
            }
        }
    }

    private fun hookUrl() {
        hookDns()
        runCatching {
            val u = frameworkCls("java.net.URL")
            u?.declaredMethods?.filter {
                it.name == "openConnection" || it.name == "openStream"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    val self = runCatching { chain.getThisObject() as? java.net.URL }.getOrNull()
                    val url = self?.toString() ?: urlOf(runCatching { chain.args }.getOrNull() ?: emptyList())
                    if (blocked(url)) {
                        logWarn("blocked network: $url")
                        throw java.io.IOException("blocked by LockPerm")
                    }
                    chain.proceed()
                }
            }
        }
        runCatching {
            val h = frameworkCls("com.android.okhttp.internal.huc.HttpURLConnectionImpl")
                ?: frameworkCls("java.net.HttpURLConnection")
            h?.declaredMethods?.filter { it.name == "connect" || it.name == "getInputStream" }
                ?.forEach { m ->
                    hookMethod(m) { chain ->
                        val self = runCatching { chain.getThisObject() }.getOrNull()
                        val url = runCatching {
                            self?.javaClass?.getMethod("getURL")?.invoke(self)?.toString()
                        }.getOrNull()
                        if (blocked(url)) {
                            logWarn("blocked network: $url")
                            throw java.io.IOException("blocked by LockPerm")
                        }
                        chain.proceed()
                    }
                }
        }
    }

    private fun hookOkHttp() {
        listOf(
            "okhttp3.OkHttpClient",
            "okhttp3.internal.connection.RealCall",
        ).forEach { name ->
            val c = cls(name) ?: return@forEach
            c.declaredMethods.filter { it.name == "newCall" }.forEach { m ->
                hookMethod(m) { chain ->
                    val url = runCatching {
                        val req = chain.args.filterIsInstance<Any>().firstOrNull { it.javaClass.name.contains("Request") }
                        req?.javaClass?.getMethod("url")?.invoke(req)?.toString()
                    }.getOrNull()
                    if (blocked(url)) {
                        logWarn("blocked okhttp: $url")
                        throw java.io.IOException("blocked by LockPerm")
                    }
                    chain.proceed()
                }
            }
        }
    }
}


































private enum class MediaKind { VIDEO, IMAGE, AUDIO, UNKNOWN }

private val MEDIA_VIDEO_EXTS =
    setOf("mp4", "m4v", "3gp", "3gpp", "webm", "mkv", "mov", "ts", "avi")

private val MEDIA_AUDIO_EXTS =
    setOf("aac", "m4a", "mp3", "ogg", "oga", "opus", "flac", "wav", "amr", "awb")

private val MEDIA_IMAGE_EXTS =
    setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

private val MEDIA_NAME_RE =
    Regex("^media[_-]?projection[_-]?(\\d+)?\\.([A-Za-z0-9]+)$", RegexOption.IGNORE_CASE)


private const val MIN_SLOT_US = 500_000L


private object ScFit {
    @Volatile
    var fit = true
}

private class MediaItem(
    val file: File,
    val order: Int,
    val kind: MediaKind,
    val videoDurationUs: Long,
    val audioDurationUs: Long,
) {
    val hasVideo: Boolean get() = kind == MediaKind.VIDEO
    val hasAudio: Boolean get() = kind == MediaKind.AUDIO || audioDurationUs > 0L
    val isImage: Boolean get() = kind == MediaKind.IMAGE
    val path: String get() = file.absolutePath
    val name: String get() = file.name
}


private class Slot(val item: MediaItem, val durUs: Long)


private fun probeMedia(f: File, order: Int): MediaItem? {
    val ext = f.name.substringAfterLast('.', "").lowercase()

    if (ext in MEDIA_IMAGE_EXTS) {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(f.absolutePath, o) }
        if (o.outWidth <= 0 || o.outHeight <= 0) return null
        return MediaItem(f, order, MediaKind.IMAGE, 0L, 0L)
    }

    var vDur = 0L
    var aDur = 0L
    runCatching {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(f.absolutePath)
            for (i in 0 until ex.trackCount) {
                val fmt = ex.getTrackFormat(i)
                val mime = runCatching { fmt.getString(MediaFormat.KEY_MIME) }.getOrNull()
                    ?: continue
                val d = runCatching { fmt.getLong(MediaFormat.KEY_DURATION) }.getOrDefault(0L)
                when {
                    mime.startsWith("video/") -> if (d > vDur) vDur = d
                    mime.startsWith("audio/") -> if (d > aDur) aDur = d
                }
            }
        } finally {
            runCatching { ex.release() }
        }
    }

    val kind = when {
        vDur > 0L -> MediaKind.VIDEO
        aDur > 0L -> MediaKind.AUDIO
        ext in MEDIA_AUDIO_EXTS -> MediaKind.AUDIO
        ext in MEDIA_VIDEO_EXTS -> MediaKind.VIDEO
        else -> MediaKind.UNKNOWN
    }
    if (kind == MediaKind.UNKNOWN) return null
    return MediaItem(f, order, kind, vDur, aDur)
}











private object ScreenStage {

    private val lock = Any()

    @Volatile
    private var anchorNs = System.nanoTime()

    @Volatile
    private var baseUs = 0L

    fun nowUs(): Long = baseUs + (System.nanoTime() - anchorNs) / 1000L

    fun sleepUntil(us: Long, maxWaitUs: Long = 300_000L) {
        var w = us - nowUs()
        if (w <= 0) return
        if (w > maxWaitUs) w = maxWaitUs
        val ms = w / 1000L
        val ns = ((w % 1000L) * 1000L).toInt()
        runCatching {
            if (ms > 0L) Thread.sleep(ms, ns) else Thread.sleep(0, ns)
        }
    }

    fun sleepUntilFull(us: Long, chunkUs: Long = 250_000L, maxTotalUs: Long = 2_000_000L) {
        val t0 = nowUs()
        while (true) {
            if (us - nowUs() <= 0L) break
            if (nowUs() - t0 >= maxTotalUs) break
            sleepUntil(us, chunkUs)
        }
    }

    
    
    
    class State(
        val video: MediaItem?,        
        val videoStartUs: Long,       
        val videoPosUs: Long,         
        val image: MediaItem?,        
        val audio: MediaItem?,        
        val audioPosUs: Long,         
        val text: String?,            
        val epoch: Long,              
    )

    @Volatile
    var ready = false
        private set

    @Volatile
    private var started = false

    
    private var items: List<MediaItem> = emptyList()

    
    private var autoSlots: List<Slot> = emptyList()
    private var autoIndex = 0
    private var autoStart = 0L

    
    private var fallback: MediaItem? = null
    private var firstImage: MediaItem? = null

    
    private val badAudio = HashSet<String>()

    
    private var ov: MediaItem? = null
    private var ovIdx = -1
    private var ovStart = 0L
    private var ovDur = -1L

    private var epoch = 0L

    fun markStart() {
        if (started) return
        synchronized(lock) {
            if (started) return
            started = true
            restartLocked()
        }
    }

    
    fun resetSession() {
        synchronized(lock) {
            started = true
            restartLocked()
        }
    }

    private fun restartLocked() {
        val now = nowUs()
        autoIndex = 0
        autoStart = now
        ov = null
        ovIdx = -1
        ovStart = now
        ovDur = -1L
        epoch++
    }

    fun apply(auto: List<Slot>, all: List<MediaItem>, fb: MediaItem?) {
        synchronized(lock) {
            val now = nowUs()
            autoSlots = auto
            autoIndex = 0
            autoStart = now
            items = all
            fallback = fb
            firstImage = all.firstOrNull { it.isImage }
            ov = null
            ovIdx = -1
            epoch++
            ready = true
            started = false
        }
    }

    private fun durAt(i: Int): Long =
        autoSlots.getOrNull(i)?.durUs?.coerceAtLeast(MIN_SLOT_US) ?: 0L

    private fun audioFor(v: MediaItem?): MediaItem? =
        if (v != null && v.audioDurationUs > 0L && v.path !in badAudio) v else fallback

    fun markAudioBad(p: String?) {
        if (p.isNullOrEmpty()) return
        synchronized(lock) { badAudio.add(p) }
    }

    private fun tick(now: Long) {
        val o = ov
        if (o != null) {
            
            if (ovDur > 0L && now - ovStart >= ovDur) {
                ov = null
                ovIdx = -1
                stepAuto(now)
            }
            return
        }
        val n = autoSlots.size
        if (n == 0) return
        
        
        var total = 0L
        for (k in 0 until n) total += durAt(k)
        if (total <= 0L) return
        var elapsed = now - autoStart
        if (elapsed >= total) {
            val cycles = elapsed / total
            autoStart += cycles * total
            elapsed -= cycles * total
            epoch++
        }
        var guard = 0
        while (guard++ <= n) {
            val d = durAt(autoIndex)
            if (d <= 0L) break
            if (elapsed >= d) {
                elapsed -= d
                autoStart += d
                autoIndex = (autoIndex + 1) % n
                epoch++
            } else break
        }
    }

    private fun stepAuto(now: Long) {
        val n = autoSlots.size
        if (n == 0) return
        autoIndex = (autoIndex + 1) % n
        autoStart = now
        epoch++
    }

    fun state(): State = synchronized(lock) {
        val now = nowUs()
        tick(now)
        val e = epoch
        val o = ov
        if (o != null) {
            val pos = (now - ovStart).coerceAtLeast(0L)
            when {
                o.isImage ->
                    State(null, 0L, 0L, o, audioFor(o), pos, null, e)
                o.hasVideo ->
                    State(o, ovStart, pos, null, audioFor(o), pos, null, e)
                else ->
                    State(null, 0L, 0L, null, o, pos, o.name, e)
            }
        } else {
            val slot = autoSlots.getOrNull(autoIndex)
            if (slot == null) {
                
                val img = firstImage
                State(null, 0L, 0L, img, audioFor(img), 0L, null, e)
            } else {
                val it = slot.item
                val pos = (now - autoStart).coerceAtLeast(0L)
                if (it.hasVideo) {
                    State(it, autoStart, pos, null, audioFor(it), pos, null, e)
                } else {
                    State(null, 0L, 0L, null, it, pos, it.name, e)
                }
            }
        }
    }

    fun manualNext(): Boolean = move(+1)

    fun manualPrev(): Boolean = move(-1)

    private fun move(delta: Int): Boolean = synchronized(lock) {
        val all = items
        if (all.size <= 1) return false
        val base = if (ovIdx >= 0) ovIdx else indexOfAuto()
        val n = all.size
        val i = (((base + delta) % n) + n) % n
        select(all[i], i)
        return true
    }

    private fun indexOfAuto(): Int {
        val it = autoSlots.getOrNull(autoIndex)?.item
        if (it == null) return 0
        val i = items.indexOfFirst { x -> x.path == it.path }
        return if (i < 0) 0 else i
    }

    private fun select(it: MediaItem, i: Int) {
        ov = it
        ovIdx = i
        ovStart = nowUs()
        ovDur = when {
            it.isImage -> -1L                                                  
            it.hasVideo -> it.videoDurationUs.coerceAtLeast(MIN_SLOT_US)
            else -> it.audioDurationUs.coerceAtLeast(MIN_SLOT_US)
        }
        epoch++
    }

    fun currentLabel(): String {
        val st = state()
        return st.video?.name ?: st.image?.name ?: st.audio?.name ?: "-"
    }
}





private class VideoFeed(private val surface: Surface) {

    @Volatile
    private var stopFlag = false

    private var thread: Thread? = null

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var playing: String? = null
        private set

    fun start(): Boolean {
        stopFlag = false
        val t = Thread({ run() }, "xp-sc-video")
        t.isDaemon = true
        thread = t
        t.start()
        return true
    }

    private fun run() {
        val info = MediaCodec.BufferInfo()
        var ex: MediaExtractor? = null
        var cd: MediaCodec? = null
        var useSurface = false
        var curPath: String? = null
        var curEpoch = -1L
        var startUs = 0L
        var inputEos = false
        var basePts = 0L
        try {
            while (!stopFlag) {
             try {
                ScreenStage.markStart()
                val st = ScreenStage.state()
                val item = st.video

                if (item == null) {
                    runCatching { Thread.sleep(50) }
                    continue
                }

                if (curPath != item.path || curEpoch != st.epoch) {
                    releaseCodec(cd, ex)
                    ex = null
                    cd = null
                    curPath = null
                    playing = null
                    val opened = openVideo(item)
                    if (opened == null) {
                        lastError = "无法解码 ${item.name}"
                        runCatching { Thread.sleep(300) }
                        continue
                    }
                    ex = opened.first
                    cd = opened.second
                    useSurface = opened.third
                    val dur = item.videoDurationUs
                    val seekTo = if (dur > 0L) (st.videoPosUs % dur) else 0L
                    runCatching {
                        opened.first.seekTo(seekTo, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    }
                    basePts = runCatching { opened.first.sampleTime }
                        .getOrDefault(0L).coerceAtLeast(0L)
                    inputEos = false
                    curPath = item.path
                    curEpoch = st.epoch
                    startUs = st.videoStartUs + basePts
                    playing = item.name
                    lastError = null
                }

                val e = ex ?: continue
                val c = cd ?: continue

                if (!inputEos) {
                    runCatching {
                        val inIdx = c.dequeueInputBuffer(10_000)
                        if (inIdx >= 0) {
                            val buf = c.getInputBuffer(inIdx)
                            if (buf != null) {
                                val size = e.readSampleData(buf, 0)
                                if (size >= 0) {
                                    val s = e.sampleTime
                                    c.queueInputBuffer(inIdx, 0, size, (s - basePts).coerceAtLeast(0L), 0)
                                    e.advance()
                                } else {
                                    inputEos = true
                                    c.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                }
                            }
                        }
                    }
                }

                val outIdx = runCatching { c.dequeueOutputBuffer(info, 10_000) }.getOrDefault(-1)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    outIdx >= 0 -> {
                        val pts = (info.presentationTimeUs - basePts).coerceAtLeast(0L)
                        val target = startUs + pts
                        ScreenStage.sleepUntil(target, 250_000L)
                        val late = ScreenStage.nowUs() - target
                        applyScaling(c)
                        if (late < 500_000L) {
                            if (useSurface) {
                                runCatching { c.releaseOutputBuffer(outIdx, true) }
                            } else {
                                drawCanvas(c, outIdx)
                            }
                        } else {
                            runCatching { c.releaseOutputBuffer(outIdx, false) }
                        }
                    }
                }
             } catch (_: Throwable) {
                runCatching { Thread.sleep(60) }
             }
            }
        } catch (_: Throwable) {
        } finally {
            releaseCodec(cd, ex)
            playing = null
        }
    }


    private fun openVideo(item: MediaItem): Triple<MediaExtractor, MediaCodec, Boolean>? =
        runCatching { openVideoInner(item) }
            .onFailure { lastError = it.message }
            .getOrNull()

    private fun openVideoInner(item: MediaItem): Triple<MediaExtractor, MediaCodec, Boolean>? {
        val ex = MediaExtractor()
        ex.setDataSource(item.path)
        var track = -1
        for (i in 0 until ex.trackCount) {
            val mime = runCatching { ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME) }.getOrNull()
            if (mime?.startsWith("video/") == true) {
                track = i
                break
            }
        }
        if (track < 0) {
            ex.release()
            return null
        }
        ex.selectTrack(track)
        val fmt = ex.getTrackFormat(track)
        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: run {
            ex.release()
            return null
        }
        val cd = MediaCodec.createDecoderByType(mime)
        
        
        
        
        
        val useSurface = if (!ScFit.fit) {
            runCatching {
                cd.configure(fmt, surface, null, 0)
                true
            }.getOrDefault(false)
        } else {
            false
        }
        if (!useSurface) runCatching { cd.configure(fmt, null, null, 0) }
        cd.start()
        applyScaling(cd)
        return Triple(ex, cd, useSurface)
    }

    private var scalingMode = -1

    private fun applyScaling(c: MediaCodec) {
        val want = if (ScFit.fit) 1 else 2
        if (scalingMode == want) return
        runCatching {
            c.setVideoScalingMode(want)
            scalingMode = want
        }
    }

    private fun drawCanvas(c: MediaCodec, outIdx: Int) {
        val img = runCatching { c.getOutputImage(outIdx) }.getOrNull()
        if (img != null) {
            val bmp = runCatching { imageToBitmap(img) }.getOrNull()
            if (bmp != null) {
                runCatching {
                    val canvas = surface.lockCanvas(null)
                    if (canvas != null) {
                        try {
                            canvas.drawColor(Color.BLACK)
                            val cw = canvas.width.toFloat()
                            val ch = canvas.height.toFloat()
                            if (cw <= 0f || ch <= 0f) return@runCatching
                            val scale = if (ScFit.fit) {
                                minOf(cw / bmp.width, ch / bmp.height)
                            } else {
                                maxOf(cw / bmp.width, ch / bmp.height)
                            }
                            if (scale.isNaN() || scale.isInfinite() || scale <= 0f) return@runCatching
                            val dw = bmp.width * scale
                            val dh = bmp.height * scale
                            val left = (cw - dw) / 2f
                            val top = (ch - dh) / 2f
                            val dst = RectF(left, top, left + dw, top + dh)
                            canvas.drawBitmap(bmp, null, dst, null)
                        } finally {
                            surface.unlockCanvasAndPost(canvas)
                        }
                    }
                }
            }
        }
        runCatching { c.releaseOutputBuffer(outIdx, false) }
    }

    private fun imageToBitmap(img: Image): Bitmap? {
        val w = img.width
        val h = img.height
        val planes = img.planes
        if (planes.size < 3) return null
        val y = planes[0]
        val u = planes[1]
        val v = planes[2]
        val nv21 = ByteArray(w * h * 3 / 2)

        val yBuf = y.buffer
        val uBuf = u.buffer
        val vBuf = v.buffer
        val yRow = y.rowStride
        val yPix = y.pixelStride
        val uRow = u.rowStride
        val uPix = u.pixelStride
        val vRow = v.rowStride
        val vPix = v.pixelStride

        var pos = 0
        for (r in 0 until h) {
            val start = r * yRow
            if (yPix == 1) {
                val len = w.coerceAtMost(yBuf.limit() - start)
                if (len <= 0) break
                yBuf.position(start)
                yBuf.get(nv21, pos, len)
                pos += len
            } else {
                for (c in 0 until w) {
                    val idx = start + c * yPix
                    if (idx < yBuf.limit()) nv21[pos++] = yBuf.get(idx)
                }
            }
        }

        val chromaH = h / 2
        val chromaW = w / 2
        var cp = w * h
        for (r in 0 until chromaH) {
            for (c in 0 until chromaW) {
                val vi = r * vRow + c * vPix
                val ui = r * uRow + c * uPix
                if (vi < vBuf.limit() && cp < nv21.size) nv21[cp++] = vBuf.get(vi)
                if (ui < uBuf.limit() && cp < nv21.size) nv21[cp++] = uBuf.get(ui)
            }
        }
        val out = ByteArrayOutputStream()
        YuvImage(nv21, android.graphics.ImageFormat.NV21, w, h, null)
            .compressToJpeg(android.graphics.Rect(0, 0, w, h), 90, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }

    private fun releaseCodec(c: MediaCodec?, e: MediaExtractor?) {
        runCatching { c?.stop(); c?.release() }
        runCatching { e?.release() }
    }

    fun stop() {
        stopFlag = true
        runCatching { thread?.interrupt() }
        thread = null
        playing = null
    }
}









private class AudioFeed {

    companion object {
        private const val RING = 48000 * 2
        private const val MAX_WAIT_US = 250_000L
        private const val CATCHUP_US = 400_000L
        private const val ENC_FLOAT = 4
        private const val PCM_16BIT = 2
    }

    @Volatile
    private var stopFlag = false

    @Volatile
    var lastError: String? = null
        private set

    private val lock = Any()
    private val ring = ShortArray(RING)
    private var rW = 0
    private var rR = 0
    private var avail = 0

    private val info = MediaCodec.BufferInfo()

    private var ex: MediaExtractor? = null
    private var cd: MediaCodec? = null
    private var curPath: String? = null
    private var srcRate = 44100
    private var srcCh = 1
    private var audioDurUs = 0L
    private var basePts = 0L
    private var pcmFloat = false

    private var decFrames = 0L

    private var consumedUs = -1L

    private var zeroStreak = 0
    private var reopened = false

    fun start() {
        stopFlag = false
    }

    fun stop() {
        stopFlag = true
        synchronized(lock) { releaseDecoder() }
    }

    fun readShorts(out: ShortArray, off: Int, want: Int, targetRate: Int, dstCh: Int): Int {
        ScreenStage.markStart()
        val dc = dstCh.coerceIn(1, 8)
        val frames = want / dc
        if (frames <= 0) return 0
        val rate = targetRate.coerceIn(4000, 192000)
        val durUs = frames.toLong() * 1_000_000L / rate.toLong()

        synchronized(lock) {
            val now = ScreenStage.nowUs()
            if (consumedUs < 0L) consumedUs = now
            var end = consumedUs + durUs

            if (end > now) {
                ScreenStage.sleepUntilFull(end, MAX_WAIT_US)
                val n2 = ScreenStage.nowUs()
                if (end > n2 + CATCHUP_US) {
                    consumedUs = n2
                    end = n2 + durUs
                }
            } else if (now - end > CATCHUP_US) {
                consumedUs = now - durUs
                end = now
            }

            if (!stopFlag) {
                val st = ScreenStage.state()
                val src = st.audio
                if (src != null && src.hasAudio) {
                    mixInto(out, off, frames, dc, rate, src, st.audioPosUs)
                } else {
                    fillSilence(out, off, frames, dc)
                }
            } else {
                fillSilence(out, off, frames, dc)
            }
            consumedUs = end
        }
        return frames * dc
    }

    private fun fillSilence(out: ShortArray, off: Int, frames: Int, dc: Int) {
        var p = off
        for (i in 0 until frames) {
            for (k in 0 until dc) {
                if (p < out.size) out[p++] = 0
            }
        }
    }

    private fun mixInto(
        out: ShortArray,
        off: Int,
        frames: Int,
        dc: Int,
        rate: Int,
        item: MediaItem,
        posUs: Long,
    ) {
        if (curPath != item.path) {
            if (!openAudio(item)) {
                fillSilence(out, off, frames, dc)
                return
            }
            zeroStreak = 0
            reopened = false
        }

        var srcUs = posUs
        if (audioDurUs > 0L) srcUs %= audioDurUs

        val srcFrame = srcUs * srcRate.toLong() / 1_000_000L
        val ringStart = decFrames - avail.toLong()
        val tol = (srcRate / 4).coerceAtLeast(256).toLong()

        if (srcFrame < ringStart - tol || srcFrame > decFrames + tol) {
            if (!seek(srcFrame)) {
                fillSilence(out, off, frames, dc)
                return
            }
        }

        val skip = (srcFrame - (decFrames - avail.toLong())).coerceAtLeast(0L)
        val need = (frames.toLong() * srcRate.toLong() / rate.toLong()).coerceAtLeast(1L)
        pump(skip + need)

        if (avail <= 0) onSilent(item)

        val dropN = skip.coerceAtMost(avail.toLong()).toInt()
        drop(dropN)
        val take = need.toInt().coerceAtMost(avail)

        val step = srcRate.toDouble() / rate.toDouble()
        var p = off
        for (i in 0 until frames) {
            val x = i * step
            val i0 = x.toInt()
            val t = (x - i0).toFloat()
            val s0 = if (i0 < take) peek(i0) else 0
            val s1 = if (i0 + 1 < take) peek(i0 + 1) else s0
            val v = (s0 + (s1 - s0) * t).toInt().coerceIn(-32768, 32767).toShort()
            for (k in 0 until dc) {
                if (p < out.size) out[p++] = v
            }
        }
        drop(take)
    }

    fun paceOnly(frames: Int, targetRate: Int) {
        ScreenStage.markStart()
        if (frames <= 0) return
        val rate = targetRate.coerceIn(4000, 192000)
        val durUs = frames.toLong() * 1_000_000L / rate.toLong()
        synchronized(lock) {
            val now = ScreenStage.nowUs()
            if (consumedUs < 0L) consumedUs = now
            val end = consumedUs + durUs
            if (end > now) ScreenStage.sleepUntilFull(end, MAX_WAIT_US)
            consumedUs = end
        }
    }

    
    private fun onSilent(item: MediaItem) {
        zeroStreak++
        if (zeroStreak < 48) return          
        zeroStreak = 0
        if (!reopened) {
            reopened = true
            val p = item.path
            releaseDecoder()
            curPath = null
            if (openAudio(item)) {
                curPath = p
                return
            }
        }
        ScreenStage.markAudioBad(curPath ?: item.path)
        releaseDecoder()
        curPath = null
    }

    
    private fun pump(want: Long) {
        var guard = 0
        var idle = 0
        while (avail.toLong() < want && guard++ < 128) {
            if (decodeMore()) idle = 0
            else if (++idle >= 16) break
        }
    }

    private fun peek(j: Int): Int {
        if (j < 0 || j >= avail) return 0
        return ring[(rR + j) % RING].toInt()
    }

    private fun drop(n: Int) {
        if (n <= 0) return
        val m = n.coerceAtMost(avail)
        rR = (rR + m) % RING
        avail -= m
        if (avail < 0) avail = 0
    }

    private fun openAudio(item: MediaItem): Boolean =
        runCatching { openAudioInner(item) }
            .onFailure { lastError = it.message }
            .getOrDefault(false)

    private fun openAudioInner(item: MediaItem): Boolean {
        releaseDecoder()
        val e = MediaExtractor()
        e.setDataSource(item.path)
        var track = -1
        for (i in 0 until e.trackCount) {
            val mime = runCatching { e.getTrackFormat(i).getString(MediaFormat.KEY_MIME) }.getOrNull()
            if (mime?.startsWith("audio/") == true) {
                track = i
                break
            }
        }
        if (track < 0) {
            lastError = "无音轨"
            e.release()
            return false
        }
        e.selectTrack(track)
        val fmt = e.getTrackFormat(track)
        val mime = fmt.getString(MediaFormat.KEY_MIME)
        if (mime == null) {
            lastError = "无 mime"
            e.release()
            return false
        }
        srcRate = runCatching { fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) }.getOrDefault(44100)
            .coerceIn(4000, 192000)
        srcCh = runCatching { fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrDefault(1)
            .coerceIn(1, 8)
        audioDurUs = runCatching { fmt.getLong(MediaFormat.KEY_DURATION) }.getOrDefault(0L)

        val c = MediaCodec.createDecoderByType(mime)
        val ok = runCatching {
            fmt.setInteger(MediaFormat.KEY_PCM_ENCODING, PCM_16BIT)
            c.configure(fmt, null, null, 0)
        }.isSuccess
        if (!ok) {
            runCatching { fmt.removeKey(MediaFormat.KEY_PCM_ENCODING) }
            val ok2 = runCatching { c.configure(fmt, null, null, 0) }.isSuccess
            if (!ok2) {
                lastError = "解码器配置失败"
                runCatching { c.release() }
                e.release()
                return false
            }
        }
        c.start()

        e.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        basePts = runCatching { e.sampleTime }.getOrDefault(0L).coerceAtLeast(0L)
        pcmFloat = false

        ex = e
        cd = c
        curPath = item.path
        avail = 0
        rR = 0
        rW = 0
        decFrames = 0
        return true
    }

    private fun seek(frame: Long): Boolean {
        val e = ex ?: return false
        val c = cd ?: return false
        return runCatching {
            val us = (frame * 1_000_000L / srcRate.toLong().coerceAtLeast(1L)).coerceAtLeast(0L)
            e.seekTo(us + basePts, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            runCatching { c.flush() }
            avail = 0
            rR = 0
            rW = 0
            decFrames = runCatching {
                (e.sampleTime - basePts).coerceAtLeast(0L) * srcRate.toLong() / 1_000_000L
            }.getOrDefault(frame)
            true
        }.getOrDefault(false)
    }

    private fun decodeMore(): Boolean {
        val c = cd ?: return false
        val e = ex ?: return false
        var produced = false

        runCatching {
            val inIdx = c.dequeueInputBuffer(0)
            if (inIdx >= 0) {
                val buf = c.getInputBuffer(inIdx)
                if (buf != null) {
                    var size = e.readSampleData(buf, 0)
                    if (size < 0) {
                        e.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        avail = 0
                        rR = 0
                        rW = 0
                        decFrames = 0
                        size = e.readSampleData(buf, 0)
                    }
                    if (size >= 0) {
                        c.queueInputBuffer(
                            inIdx, 0, size,
                            (e.sampleTime - basePts).coerceAtLeast(0L), 0,
                        )
                        e.advance()
                    } else {
                        c.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                }
            }
        }

        runCatching {
            val outIdx = c.dequeueOutputBuffer(info, 1000)
            when {
                outIdx >= 0 -> {
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0 && info.size > 0) {
                        val buf = c.getOutputBuffer(outIdx)
                        if (buf != null) {
                            pushPcm(buf, info.size)
                            produced = true
                        }
                    }
                    c.releaseOutputBuffer(outIdx, false)
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    runCatching {
                        val f = c.outputFormat
                        srcRate = runCatching { f.getInteger(MediaFormat.KEY_SAMPLE_RATE) }
                            .getOrDefault(srcRate).coerceIn(4000, 192000)
                        srcCh = runCatching { f.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }
                            .getOrDefault(srcCh).coerceIn(1, 8)
                        pcmFloat = runCatching { f.getInteger(MediaFormat.KEY_PCM_ENCODING) }
                            .getOrDefault(PCM_16BIT) == ENC_FLOAT
                    }
                }
            }
        }
        return produced
    }

    private fun pushPcm(buf: java.nio.ByteBuffer, size: Int) {
        val c = srcCh.coerceIn(1, 8)
        val b = runCatching { buf.duplicate().order(ByteOrder.LITTLE_ENDIAN) }.getOrNull() ?: return
        val unit = if (pcmFloat) 4 else 2
        val frames = (size / unit / c).coerceAtLeast(0)
        if (frames <= 0) return

        val fb = if (pcmFloat) runCatching { b.asFloatBuffer() }.getOrNull() else null
        val sb = if (!pcmFloat) runCatching { b.asShortBuffer() }.getOrNull() else null
        if (fb == null && sb == null) return

        for (i in 0 until frames) {
            var sum = 0.0
            var n = 0
            for (k in 0 until c) {
                if (pcmFloat) {
                    if (fb == null || !fb.hasRemaining()) break
                    val fv = fb.get()
                    sum += if (fv < -1f) -32768.0 else if (fv > 1f) 32767.0 else fv.toDouble() * 32767.0
                    n++
                } else {
                    if (sb == null || !sb.hasRemaining()) break
                    sum += sb.get().toInt().toDouble()
                    n++
                }
            }
            if (n == 0) break
            if (avail >= RING) {
                val d = (RING / 8).coerceAtLeast(1)
                rR = (rR + d) % RING
                avail -= d
                if (avail < 0) avail = 0
            }
            ring[rW] = (sum / n).toInt().coerceIn(-32768, 32767).toShort()
            rW++
            if (rW >= RING) rW = 0
            avail++
            decFrames++
        }
    }

    private fun releaseDecoder() {
        runCatching { cd?.stop(); cd?.release() }
        runCatching { ex?.release() }
        cd = null
        ex = null
        curPath = null
        avail = 0
        rR = 0
        rW = 0
        decFrames = 0
    }
}



internal class ScreenCaptureDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    companion object {

        private const val REPAINT_INTERVAL_MS = 33L
        private const val RESCAN_INTERVAL_MS = 1000L

        private val SURFACES: MutableList<Surface> =
            Collections.synchronizedList(mutableListOf<Surface>())

        private val SINKS: MutableSet<Surface> =
            Collections.synchronizedSet(mutableSetOf<Surface>())

        private val SINK_READERS: MutableSet<ImageReader> =
            Collections.synchronizedSet(mutableSetOf<ImageReader>())

        private val VIDEOS: MutableMap<Surface, VideoFeed> =
            Collections.synchronizedMap(mutableMapOf<Surface, VideoFeed>())

        @Volatile
        private var audio: AudioFeed? = null

        
        @Volatile
        private var captureActive = false

        
        @Volatile
        private var sessionStart = false

        @Volatile
        private var enterToasted = false

        
        @Volatile
        private var volThread: Thread? = null

        @Volatile
        private var volLast = -1

        @Volatile
        private var volMuteUntilMs = 0L

        
        private var plItems: List<MediaItem> = emptyList()
        private var plAuto: List<Slot> = emptyList()
        private var plImages: List<MediaItem> = emptyList()

        private val FAILS: MutableMap<Surface, Int> =
            Collections.synchronizedMap(mutableMapOf<Surface, Int>())

        @Volatile
        private var running = false

        @Volatile
        private var painter: Thread? = null

        @Volatile
        private var cached: Pair<String, Bitmap?>? = null

        @Volatile
        private var notifiedState: String? = null

        @Volatile
        private var playlistSig = ""

        @Volatile
        private var lastScanMs = 0L

        @Volatile
        private var playlistItems: List<MediaItem> = emptyList()
    }

    fun install() {
        val cfg = snapshot()
        if (!cfg.blockScreenCapture) return
        
        
        ScFit.fit = cfg.screenCaptureFit
        when (cfg.screenCaptureMode) {
            XpConfig.SC_MODE_DENY -> hookDeny()
            else -> hookFake()
        }
        logInfo("screen capture defender installed (mode=${cfg.screenCaptureMode})")
    }

    fun installNow() {
        XpState.Flags.forceScreenCapture = true
        hookFake()
        hookDeny()
    }

    




    private fun mode(): Int =
        if (XpState.Flags.forceScreenCapture) XpConfig.SC_MODE_DENY
        else snapshot().screenCaptureMode

    private fun on(): Boolean =
        XpState.Flags.forceScreenCapture || snapshot().blockScreenCapture

    private fun faking(): Boolean = on() && mode() != XpConfig.SC_MODE_DENY

    private fun createSink(w: Int, h: Int): Surface? {
        return runCatching {
            val ww = w.coerceIn(16, 1920)
            val hh = h.coerceIn(16, 1920)
            val ir = ImageReader.newInstance(ww, hh, android.graphics.PixelFormat.RGBA_8888, 3)
            SINK_READERS.add(ir)
            val s = ir.surface
            SINKS.add(s)
            s
        }.getOrNull()
    }

    private fun drainSinks() {
        val list = synchronized(SINK_READERS) { SINK_READERS.toList() }
        for (ir in list) {
            runCatching { ir.acquireLatestImage()?.close() }
        }
    }

    private fun swapSurface(args: List<Any?>?): Array<Any?>? {
        if (args == null) return null
        val idx = args.indexOfFirst { it is Surface }
        if (idx < 0) return null
        val old = args[idx] as Surface
        if (old in SINKS) return null

        val ints = args.indices.filter { args[it] is Int }
        val w = if (ints.size >= 2) args[ints[0]] as Int else 720
        val h = if (ints.size >= 2) args[ints[1]] as Int else 1280
        val sink = createSink(w, h) ?: return null
        val out = args.toTypedArray()
        out[idx] = sink
        return out
    }

    private fun registerSurface(s: Surface?) {
        if (s == null || s in SINKS) return
        synchronized(SURFACES) {
            if (!SURFACES.contains(s)) SURFACES.add(s)
        }
        FAILS[s] = 0
        if (!captureActive) {
            captureActive = true
            sessionStart = true
            ScreenStage.resetSession()
        }
        ensurePainter()
        ensureVolumeWatch()
    }

    private fun ensurePainter() {
        val t = painter
        if (t != null && t.isAlive) return
        running = true
        val th = Thread({
            while (running) {
                try {
                    drainSinks()
                    if (faking()) {
                        ensureAudioFeed()
                        ensureVolumeWatch()
                        ScFit.fit = snapshot().screenCaptureFit
                        refreshPlaylist()
                        val wantToast = sessionStart || !enterToasted
                        if (wantToast) {
                            sessionStart = false
                            enterToasted = true
                            emitPlaylistToast()
                        }
                        val st = ScreenStage.state()
                        val list = synchronized(SURFACES) { SURFACES.toList() }
                        val imageMode = mode() == XpConfig.SC_MODE_IMAGE
                        when {
                            imageMode && st.video != null -> {
                                for (s in list) ensureVideo(s)
                            }
                            imageMode && st.image != null -> {
                                stopAllVideos()
                                val bmp = loadBitmap(st.image)
                                for (s in list) {
                                    if (!running || !faking()) break
                                    if (!paintBitmap(s, bmp)) bumpFail(s) else FAILS[s] = 0
                                }
                            }
                            imageMode -> {
                                stopAllVideos()
                                val txt = st.text
                                for (s in list) {
                                    if (!running || !faking()) break
                                    if (!paintText(s, txt)) bumpFail(s) else FAILS[s] = 0
                                }
                            }
                            else -> {
                                stopAllVideos()
                                for (s in list) {
                                    if (!running || !faking()) break
                                    if (!paintBlank(s)) bumpFail(s) else FAILS[s] = 0
                                }
                            }
                        }
                    } else {
                        stopAllVideos()
                        audio?.stop()
                        audio = null
                        captureActive = false
                        sessionStart = false
                        volLast = -1
                    }
                } catch (_: Throwable) {
                }
                try {
                    Thread.sleep(REPAINT_INTERVAL_MS)
                } catch (_: Throwable) {
                    break
                }
            }
        }, "xp-sc-repaint")
        th.isDaemon = true
        painter = th
        th.start()
    }

    private fun bumpFail(s: Surface) {
        val n = (FAILS[s] ?: 0) + 1
        FAILS[s] = n
        if (n > 20) {
            synchronized(SURFACES) { SURFACES.remove(s) }
            FAILS.remove(s)
        }
    }

    private fun ensureAudioFeed(): AudioFeed {
        val cur = audio
        if (cur != null) return cur
        refreshPlaylist()
        val f = AudioFeed()
        f.start()
        audio = f
        return f
    }

    private fun paintBitmap(s: Surface, bmp: Bitmap?): Boolean {
        if (VIDEOS.containsKey(s)) return true
        return runCatching {
            val canvas = s.lockCanvas(null) ?: return false
            try {
                if (bmp != null) drawImageFrame(canvas, bmp) else drawTextFrame(canvas, null)
            } finally {
                runCatching { s.unlockCanvasAndPost(canvas) }
            }
            true
        }.getOrDefault(false)
    }

    private fun paintText(s: Surface, text: String?): Boolean {
        if (VIDEOS.containsKey(s)) return true
        return runCatching {
            val canvas = s.lockCanvas(null) ?: return false
            try {
                drawTextFrame(canvas, text)
            } finally {
                runCatching { s.unlockCanvasAndPost(canvas) }
            }
            true
        }.getOrDefault(false)
    }

    private fun paintBlank(s: Surface): Boolean {
        if (VIDEOS.containsKey(s)) return true
        return runCatching {
            val canvas = s.lockCanvas(null) ?: return false
            try {
                drawTextFrame(canvas, null)
            } finally {
                runCatching { s.unlockCanvasAndPost(canvas) }
            }
            true
        }.getOrDefault(false)
    }

    private fun ensureVideo(s: Surface) {
        if (VIDEOS.containsKey(s)) return
        val feed = VideoFeed(s)
        if (feed.start()) VIDEOS[s] = feed
        else feed.stop()
    }

    private fun stopAllVideos() {
        val list = synchronized(VIDEOS) { VIDEOS.values.toList() }
        for (v in list) v.stop()
        synchronized(VIDEOS) { VIDEOS.clear() }
    }

    private fun drawImageFrame(canvas: Canvas, bmp: Bitmap) {
        canvas.drawColor(Color.BLACK)
        val cw = canvas.width.toFloat()
        val ch = canvas.height.toFloat()
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        if (bw <= 0f || bh <= 0f || cw <= 0f || ch <= 0f) return
        
        
        val scale = if (ScFit.fit) minOf(cw / bw, ch / bh) else maxOf(cw / bw, ch / bh)
        if (scale.isNaN() || scale.isInfinite() || scale <= 0f) return
        val dw = bw * scale
        val dh = bh * scale
        val left = (cw - dw) / 2f
        val top = (ch - dh) / 2f
        val dst = RectF(left, top, left + dw, top + dh)
        canvas.drawBitmap(bmp, null, dst, null)
    }

    
    private fun drawTextFrame(canvas: Canvas, text: String?) {
        canvas.drawColor(Color.DKGRAY)
        val s = text?.takeIf { it.isNotBlank() } ?: "内容已被拦截"
        val p = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val maxW = canvas.width.toFloat() * 0.86f
        var size = 42f
        p.textSize = size
        while (size > 14f && p.measureText(s) > maxW) {
            size -= 2f
            p.textSize = size
        }
        canvas.drawText(s, canvas.width / 2f, canvas.height / 2f, p)
    }

    
    
    
    private fun refreshPlaylist() {
        val nowMs = System.currentTimeMillis()
        if (ScreenStage.ready && nowMs - lastScanMs < RESCAN_INTERVAL_MS) return
        lastScanMs = nowMs

        val ctx = runCatching { appContext() }.getOrNull() ?: return
        val dir = runCatching { ctx.getExternalFilesDir(null) }.getOrNull() ?: return
        val arr = runCatching { dir.listFiles() }.getOrNull() ?: return

        val matched = ArrayList<Pair<Int, File>>()
        for (f in arr) {
            if (!f.isFile) continue
            val m = MEDIA_NAME_RE.matchEntire(f.name) ?: continue
            val num = m.groupValues[1].toIntOrNull() ?: 1
            matched.add(num to f)
        }
        matched.sortWith(compareBy<Pair<Int, File>> { it.first }.thenBy { it.second.name })

        val sig = buildString {
            for (p in matched) {
                append(p.second.name).append(':')
                append(p.second.lastModified()).append(':')
                append(p.second.length()).append('|')
            }
        }
        if (sig == playlistSig && ScreenStage.ready) return
        playlistSig = sig

        val items = ArrayList<MediaItem>()
        for (p in matched) {
            val it = runCatching { probeMedia(p.second, p.first) }.getOrNull() ?: continue
            items.add(it)
        }
        playlistItems = items

        val videos = items.filter { it.hasVideo }
        val audios = items.filter { !it.hasVideo && !it.isImage && it.audioDurationUs > 0L }
        val images = items.filter { it.isImage }

        
        val base = items.firstOrNull { !it.isImage }
        val autoSlots: List<Slot> = when {
            base == null -> emptyList()
            base.hasVideo -> videos.map { Slot(it, it.videoDurationUs.coerceAtLeast(MIN_SLOT_US)) }
            else -> audios.map { Slot(it, it.audioDurationUs.coerceAtLeast(MIN_SLOT_US)) }
        }
        
        val fb = items.firstOrNull { !it.isImage && it.audioDurationUs > 0L }

        ScreenStage.apply(autoSlots, items, fb)
        publishPlaylist(items, images, autoSlots)
        logWarn(playlistMessage())
        
        
    }

    private fun loadBitmap(item: MediaItem): Bitmap? {
        val key = "${item.path}|${item.file.lastModified()}"
        val c = cached
        if (c != null && c.first == key) return c.second
        val bmp = runCatching { decodeScaled(item.file) }.getOrNull()
        cached = key to bmp
        return bmp
    }

    private fun decodeScaled(f: File): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(f.absolutePath, opts) }
        val w = opts.outWidth
        val h = opts.outHeight
        if (w <= 0 || h <= 0) return BitmapFactory.decodeFile(f.absolutePath)
        var sample = 1
        while (maxOf(w, h) / sample > 2560) sample *= 2
        return BitmapFactory.decodeFile(
            f.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    
    
    
    
    
    private fun ensureVolumeWatch() {
        val t = volThread
        if (t != null && t.isAlive) return
        val th = Thread({
            while (true) {
                try {
                    Thread.sleep(200)
                } catch (_: Throwable) {
                    break
                }
                runCatching { pollVolume() }
            }
        }, "xp-sc-volume")
        th.isDaemon = true
        volThread = th
        th.start()
    }

    private fun pollVolume() {
        if (!faking() || !captureActive) {
            volLast = -1
            return
        }
        val ctx = runCatching { appContext() }.getOrNull() ?: return
        val am = runCatching {
            ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        }.getOrNull() ?: return
        val cur = runCatching {
            am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        }.getOrNull() ?: return
        val last = volLast
        volLast = cur
        if (last < 0 || cur == last) return

        val now = System.currentTimeMillis()
        if (now < volMuteUntilMs) return
        volMuteUntilMs = now + 3000L

        val moved = if (cur > last) ScreenStage.manualPrev() else ScreenStage.manualNext()
        if (!moved) return
        val name = ScreenStage.currentLabel()
        logWarn("screen capture: 音量键切换 → $name")
        toast("已切换 $name")
    }

    
    
    
    private fun publishPlaylist(
        items: List<MediaItem>,
        images: List<MediaItem>,
        autoSlots: List<Slot>,
    ) {
        plItems = items
        plImages = images
        plAuto = autoSlots
    }

    private fun playlistMessage(): String {
        val auto = plAuto
        val items = plItems
        val names = auto.joinToString(" → ") { it.item.name }
        val msg = when {
            auto.size > 1 ->
                "屏幕捕获：按顺序循环播放 ${auto.size} 个" +
                    (if (auto[0].item.hasVideo) "视频" else "音频") + " $names"
            auto.size == 1 ->
                "屏幕捕获：循环播放 ${auto[0].item.name}"
            plImages.isNotEmpty() ->
                "屏幕捕获：目录里只有图片（${plImages.first().name}），画面静态显示，音量键可手动切换"
            items.isEmpty() ->
                "屏幕捕获：目录下没有 media_projection 素材，画面按方案(b)文字显示"
            else ->
                "屏幕捕获：目录里没有可自动播放的素材，画面按方案(b)文字显示"
        }
        return if (items.size > 1) {
            "$msg（共 ${items.size} 个素材，录制中按音量键可切换）"
        } else msg
    }

    private fun emitPlaylistToast() {
        notifiedState = "pl|$playlistSig"
        val text = playlistMessage()
        logWarn(text)
        toast(text)
    }

    private fun hookAudio() {
        runCatching {
            val ar = frameworkCls("android.media.AudioRecord") ?: return
            ar.declaredMethods.filter { it.name == "read" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!faking()) return@hookMethod chain.proceed()
                    val args = runCatching { chain.args }.getOrNull()
                        ?: return@hookMethod chain.proceed()
                    val first = args.firstOrNull()
                    val n = readLen(args)
                    if (n <= 0) return@hookMethod chain.proceed()

                    val rec = runCatching { chain.getThisObject() }.getOrNull()
                            as? android.media.AudioRecord
                    val rate = runCatching { rec?.sampleRate }.getOrNull() ?: 44100
                    val dstCh = runCatching { rec?.channelCount }.getOrNull()?.coerceIn(1, 8) ?: 1

                    if (!captureActive) {
                        captureActive = true
                        sessionStart = true
                        ScreenStage.resetSession()
                    }
                    ensureVolumeWatch()
                    val feed = ensureAudioFeed()

                    when (first) {
                        is ShortArray -> {
                            val off = (args.getOrNull(1) as? Int)?.coerceIn(0, first.size) ?: 0
                            val want = n.coerceAtMost(first.size - off)
                            if (want <= 0) return@hookMethod 0
                            val got = runCatching {
                                feed.readShorts(first, off, want, rate, dstCh)
                            }.getOrDefault(0)
                            if (got < want) first.fill(0, off + got, off + want)
                            want
                        }
                        is ByteArray -> {
                            val off = (args.getOrNull(1) as? Int)?.coerceIn(0, first.size) ?: 0
                            val want = n.coerceAtMost(first.size - off)
                            if (want <= 0) return@hookMethod 0
                            val frames = want / 2
                            if (frames <= 0) {
                                for (k in off until (off + want).coerceAtMost(first.size)) first[k] = 0
                                return@hookMethod want
                            }
                            val tmp = ShortArray(frames)
                            val got = runCatching {
                                feed.readShorts(tmp, 0, frames, rate, dstCh)
                            }.getOrDefault(0)
                            var p = off
                            for (k in 0 until got) {
                                if (p + 1 >= first.size) break
                                val v = tmp[k].toInt()
                                first[p++] = (v and 0xFF).toByte()
                                first[p++] = ((v shr 8) and 0xFF).toByte()
                            }
                            if (p < off + want) {
                                for (k in p until (off + want).coerceAtMost(first.size)) first[k] = 0
                            }
                            want
                        }
                        is java.nio.ByteBuffer -> {
                            val frames = n / 2
                            val tmp = ShortArray(frames)
                            val got = runCatching {
                                feed.readShorts(tmp, 0, frames, rate, dstCh)
                            }.getOrDefault(0)
                            val d = first.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                            for (k in 0 until got) {
                                if (d.remaining() < 2) break
                                d.putShort(tmp[k])
                            }
                            repeat((frames - got).coerceAtLeast(0)) {
                                if (d.remaining() >= 2) d.putShort(0)
                            }
                            n
                        }
                        is FloatArray -> {
                            first.fill(0f)
                            runCatching { feed.paceOnly(n / dstCh, rate) }
                            n
                        }
                        else -> chain.proceed()
                    }
                }
            }
        }
    }

    private fun readLen(args: List<Any?>): Int {
        for (i in args.size - 1 downTo 1) {
            val v = args[i]
            if (v is Int) return v
        }
        return when (val f = args.firstOrNull()) {
            is ByteArray -> f.size
            is ShortArray -> f.size
            is FloatArray -> f.size
            else -> 0
        }
    }

    private fun hookDeny() {
        runCatching {
            val mpm = frameworkCls("android.media.projection.MediaProjectionManager")
            mpm?.declaredMethods?.filter { it.name == "createScreenCaptureIntent" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on() || mode() != XpConfig.SC_MODE_DENY) return@hookMethod chain.proceed()
                    if (snapshot().screenCaptureGrantOk) {
                        logWarn("screen capture: 返回伪装授权 Intent")
                        return@hookMethod Intent()
                    }
                    logWarn("blocked createScreenCaptureIntent")
                    null
                }
            }
            mpm?.declaredMethods?.filter { it.name == "getMediaProjection" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on() || mode() != XpConfig.SC_MODE_DENY) return@hookMethod chain.proceed()
                    logWarn("getMediaProjection -> null")
                    null
                }
            }
        }
        hookTestLibs()
    }

    private fun hookFake() {
        runCatching {
            val mp = frameworkCls("android.media.projection.MediaProjection")
            mp?.declaredMethods?.filter { it.name == "createVirtualDisplay" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!faking()) return@hookMethod chain.proceed()
                    val args = runCatching { chain.args }.getOrNull()
                    val appSurface = args?.filterIsInstance<Surface>()?.firstOrNull()
                    registerSurface(appSurface)
                    val newArgs = swapSurface(args)
                    if (newArgs != null) {
                        logWarn("screen capture: 已接管输出（真实画面转向吸收面）")
                        return@hookMethod chain.proceed(newArgs)
                    }
                    chain.proceed()
                }
            }

            mp?.declaredMethods?.filter { it.name == "stop" }?.forEach { m ->
                hookMethod(m) { chain ->
                    resetAll()
                    chain.proceed()
                }
            }
        }

        runCatching {
            val dm = frameworkCls("android.hardware.display.DisplayManager")
            dm?.declaredMethods?.filter { it.name == "createVirtualDisplay" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!faking()) return@hookMethod chain.proceed()
                    val args = runCatching { chain.args }.getOrNull()
                    registerSurface(args?.filterIsInstance<Surface>()?.firstOrNull())
                    val newArgs = swapSurface(args)
                    if (newArgs != null) {
                        logWarn("screen capture: DisplayManager 已接管输出")
                        return@hookMethod chain.proceed(newArgs)
                    }
                    chain.proceed()
                }
            }
        }

        runCatching {
            val vd = frameworkCls("android.hardware.display.VirtualDisplay")
            vd?.declaredMethods?.filter { it.name == "setSurface" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!faking()) return@hookMethod chain.proceed()
                    val args = runCatching { chain.args }.getOrNull()
                    registerSurface(args?.filterIsInstance<Surface>()?.firstOrNull())
                    val newArgs = swapSurface(args)
                    if (newArgs != null) return@hookMethod chain.proceed(newArgs)
                    chain.proceed()
                }
            }
            vd?.declaredMethods?.filter { it.name == "release" }?.forEach { m ->
                hookMethod(m) { chain ->
                    resetAll()
                    chain.proceed()
                }
            }
        }

        runCatching {
            val ir = frameworkCls("android.media.ImageReader")
            ir?.declaredMethods?.filter { it.name == "getSurface" }?.forEach { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    val self = runCatching { chain.getThisObject() }.getOrNull()
                    if (self !in SINK_READERS && faking() && r is Surface) registerSurface(r)
                    r
                }
            }
            ir?.declaredMethods?.filter {
                it.name == "acquireLatestImage" || it.name == "acquireNextImage"
            }?.forEach { m ->
                hookMethod(m) { chain ->
                    val self = runCatching { chain.getThisObject() }.getOrNull()
                    if (self in SINK_READERS) return@hookMethod chain.proceed()
                    if (!faking()) return@hookMethod chain.proceed()
                    null
                }
            }
        }

        runCatching {
            val sc = frameworkCls("android.view.SurfaceControl")
            val names = setOf(
                "screenshot", "captureLayers", "captureDisplay",
                "captureLayersEx", "screenshotToBuffer", "screenshotToSurface",
            )
            sc?.declaredMethods?.filter { it.name in names }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!faking()) return@hookMethod chain.proceed()
                    logWarn("blocked SurfaceControl.${m.name}")
                    null
                }
            }
        }

        runCatching {
            val pc = frameworkCls("android.view.PixelCopy")
            pc?.declaredMethods?.filter { it.name == "request" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!faking()) return@hookMethod chain.proceed()
                    runCatching {
                        chain.args?.filterIsInstance<Bitmap>()?.forEach { b ->
                            runCatching { b.eraseColor(Color.BLACK) }
                        }
                    }
                    logWarn("blocked PixelCopy.request")
                    0
                }
            }
        }

        runCatching {
            val tv = frameworkCls("android.view.TextureView")
            tv?.declaredMethods?.filter { it.name == "getBitmap" }?.forEach { m ->
                hookMethod(m) { chain ->
                    if (!faking()) return@hookMethod chain.proceed()
                    val r = chain.proceed()
                    if (r is Bitmap) runCatching { r.eraseColor(Color.BLACK) }
                    r
                }
            }
        }

        hookAudio()
        hookTestLibs()
        ensureVolumeWatch()
        ensurePainter()
    }

    private fun resetAll() {
        runCatching {
            stopAllVideos()
            synchronized(SURFACES) { SURFACES.clear() }
            FAILS.clear()
            captureActive = false
            sessionStart = false
            volLast = -1
        }
    }

    private fun hookTestLibs() {
        runCatching {
            cls("androidx.test.core.app.DeviceCapture")
                ?.declaredMethods?.filter { it.name == "takeScreenshot" }?.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on()) return@hookMethod chain.proceed()
                        val r = chain.proceed()
                        if (r is Bitmap) runCatching { r.eraseColor(Color.BLACK) }
                        r
                    }
                }
        }
        runCatching {
            cls("androidx.test.runner.screenshot.Screenshot")
                ?.declaredMethods?.filter { it.name == "capture" }?.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on()) return@hookMethod chain.proceed()
                        logWarn("blocked Screenshot.capture")
                        null
                    }
                }
        }
        runCatching {
            cls("androidx.test.core.view.WindowCapture")
                ?.declaredMethods?.filter { it.name == "captureRegionToBitmap" }?.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!on()) return@hookMethod chain.proceed()
                        val r = chain.proceed()
                        if (r is Bitmap) runCatching { r.eraseColor(Color.BLACK) }
                        r
                    }
                }
        }
    }
}
























internal class KeyEventDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val FLAG_REQUEST_FILTER_KEY_EVENTS = 0x00000020

    private val deviceKeys = setOf(
        android.view.KeyEvent.KEYCODE_VOLUME_UP,
        android.view.KeyEvent.KEYCODE_VOLUME_DOWN,
        android.view.KeyEvent.KEYCODE_VOLUME_MUTE,
        android.view.KeyEvent.KEYCODE_CAMERA,
        android.view.KeyEvent.KEYCODE_FOCUS,
        android.view.KeyEvent.KEYCODE_HEADSETHOOK,
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE,
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        android.view.KeyEvent.KEYCODE_MEDIA_STOP,
        android.view.KeyEvent.KEYCODE_MEDIA_NEXT,
        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        android.view.KeyEvent.KEYCODE_MEDIA_REWIND,
        android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        android.view.KeyEvent.KEYCODE_MEDIA_RECORD,
        android.view.KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK,
        android.view.KeyEvent.KEYCODE_MENU,
        android.view.KeyEvent.KEYCODE_SEARCH,
        android.view.KeyEvent.KEYCODE_MUTE,
    )

    private fun on(): Boolean = snapshot().blockKeyConsume

    private fun passBack(): Boolean = snapshot().blockKeyPassBack

    private fun eventOf(chain: io.github.libxposed.api.XposedInterface.Chain): android.view.KeyEvent? {
        
        
        
        runCatching { chain.getArg(0) }.getOrNull()?.let {
            if (it is android.view.KeyEvent) return it
        }
        val args = runCatching { chain.args }.getOrNull()
        args?.filterIsInstance<android.view.KeyEvent>()?.firstOrNull()?.let { return it }
        return runCatching { chain.getThisObject() }.getOrNull() as? android.view.KeyEvent
    }

    












    private var lastEvent: Any? = null
    private var lastResult: Boolean = false

    private fun guarded(ev: android.view.KeyEvent?): Boolean {
        if (ev === lastEvent) return lastResult
        val r = compute(ev)
        lastEvent = ev
        lastResult = r
        return r
    }

    private fun compute(ev: android.view.KeyEvent?): Boolean {
        
        val cfg = snapshot()
        if (!cfg.blockKeyConsume) return false
        if (ev == null) return false
        if (cfg.blockKeyPassBack && ev.keyCode == android.view.KeyEvent.KEYCODE_BACK) return false
        return ev.keyCode in deviceKeys
    }

    







    private val logged = java.util.Collections.newSetFromMap<String>(java.util.concurrent.ConcurrentHashMap())

    private fun logKeyOnce(tag: String) {
        if (!XpState.Flags.logEnabled) return
        if (!logged.add(tag)) return
        logWarn("key: $tag（同类按键后续不再重复记录）")
    }

    fun install() {
        if (!on()) return
        hookAppHandlers()
        hookDispatch()
        hookAccessibility()
        hookInputPipeline()
        hookServiceInfoFlag()
        hookOverridesForAll()
        logInfo("key consume defender installed")
    }

    
    fun installNow() {
        XpState.Flags.forceKeyConsume = true
        hookAppHandlers()
        hookDispatch()
        hookAccessibility()
        hookInputPipeline()
        hookServiceInfoFlag()
        hookOverridesForAll()
        logInfo("key consume defender installed (forced)")
    }

    
    
    
    
    
    
    
    
    
    

    private val KEY_OVERRIDE_NAMES = setOf(
        "onKeyDown", "onKeyUp", "onKeyMultiple",
        "onKeyLongPress", "onKeyPreIme", "onUnhandledKeyEvent",
        "dispatchKeyEvent", "dispatchKeyEventPreIme", "onKeyEvent",
    )

    
    private val subclassScanned =
        java.util.Collections.newSetFromMap<Class<*>>(java.util.concurrent.ConcurrentHashMap())

    private fun hookOverridesForAll() {
        
        captureThenHook("android.app.Activity", setOf("onCreate", "onStart", "onResume"))
        captureThenHook("android.app.Dialog", setOf("onCreate", "show"))
        captureThenHook("android.inputmethodservice.InputMethodService", setOf("onCreate", "onCreateInputView"))
        captureThenHook(
            "android.accessibilityservice.AccessibilityService",
            setOf("onServiceConnected", "onCreate", "onBind"),
        )
        
        captureThenHook("android.view.View", setOf("onAttachedToWindow"))
    }

    private fun captureThenHook(className: String, entries: Set<String>) {
        runCatching {
            val c = frameworkCls(className) ?: return@runCatching
            c.declaredMethods.filter { it.name in entries }.forEach { m ->
                hookMethod(m) { chain ->
                    val r = chain.proceed()
                    runCatching {
                        val inst = chain.getThisObject()
                        if (inst != null) hookOverrides(inst.javaClass)
                    }
                    r
                }
            }
        }
    }

    private fun hookOverrides(clazz: Class<*>?) {
        val c = clazz ?: return
        if (!subclassScanned.add(c)) return
        
        if (c.name.startsWith("android.") && c.name.count { it == '.' } <= 2) return
        val methods = runCatching { c.declaredMethods }.getOrNull() ?: return
        for (m in methods) {
            if (m.name !in KEY_OVERRIDE_NAMES) continue
            if (m.returnType != Boolean::class.javaPrimitiveType) continue
            if (!m.parameterTypes.any { it == android.view.KeyEvent::class.java }) continue
            runCatching {
                
                
                hookMethod(m, deoptimize = true) { chain ->
                    if (!guarded(eventOf(chain))) return@hookMethod chain.proceed()
                    logKeyOnce("已拦截 ${c.simpleName}.${m.name}（应用自己的覆写）")
                    false
                }
            }
        }
    }

    
    
    
    
    
    
    
    private fun hookServiceInfoFlag() {
        runCatching {
            val c = frameworkCls("android.accessibilityservice.AccessibilityService")
                ?: return@runCatching
            c.declaredMethods.filter { it.name == "setServiceInfo" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    val args = runCatching { chain.args }.getOrNull()
                    val info = args?.firstOrNull {
                        it != null && it.javaClass.name.endsWith("AccessibilityServiceInfo")
                    }
                    if (info != null) {
                        runCatching {
                            val f = info.javaClass.getDeclaredField("flags")
                            f.isAccessible = true
                            val v = f.getInt(info)
                            if (v and FLAG_REQUEST_FILTER_KEY_EVENTS != 0) {
                                f.setInt(info, v and FLAG_REQUEST_FILTER_KEY_EVENTS.inv())
                                logKeyOnce("已清除无障碍的按键过滤标志（服务收不到按键）")
                            }
                        }
                    }
                    chain.proceed()
                }
            }
        }
    }

    
    
 	
    
    private fun hookAppHandlers() {
        val classes = listOf(
            "android.app.Activity",
            "android.app.Dialog",
            "android.view.View",
            "android.view.ViewGroup",
            "android.inputmethodservice.InputMethodService",
        )
        val names = setOf(
            "onKeyDown", "onKeyUp", "onKeyMultiple",
            "onKeyLongPress", "onKeyPreIme", "onUnhandledKeyEvent",
        )
        for (cn in classes) {
            runCatching {
                val c = frameworkCls(cn) ?: return@runCatching
                c.declaredMethods.filter { m ->
                    m.name in names &&
                        m.returnType == Boolean::class.javaPrimitiveType &&
                        m.parameterTypes.any { it == android.view.KeyEvent::class.java }
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!guarded(eventOf(chain))) return@hookMethod chain.proceed()
                        logKeyOnce("已拦截应用按键处理 ${cn}.${m.name}")
                        false
                    }
                }
            }
        }
    }

    
    
 	
    
    
    private fun hookDispatch() {
        val classes = listOf(
            "android.app.Activity",
            "android.app.Dialog",
            "android.view.View",
            "android.view.ViewGroup",
            "android.inputmethodservice.InputMethodService",
        )
        val names = setOf("dispatchKeyEvent", "dispatchKeyEventPreIme")
        for (cn in classes) {
            runCatching {
                val c = frameworkCls(cn) ?: return@runCatching
                c.declaredMethods.filter { m ->
                    m.name in names &&
                        m.returnType == Boolean::class.javaPrimitiveType &&
                        m.parameterTypes.any { it == android.view.KeyEvent::class.java }
                }.forEach { m ->
                    hookMethod(m) { chain ->
                        if (!guarded(eventOf(chain))) return@hookMethod chain.proceed()
                        logKeyOnce("已拦截按键分发 ${cn}.${m.name}")
                        false
                    }
                }
            }
        }
    }

    
    
    
    private fun hookAccessibility() {
        runCatching {
            val c = frameworkCls("android.accessibilityservice.AccessibilityService")
                ?: return@runCatching
            c.declaredMethods.filter { m ->
                m.name == "onKeyEvent" &&
                    m.returnType == Boolean::class.javaPrimitiveType &&
                    m.parameterTypes.any { it == android.view.KeyEvent::class.java }
            }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!guarded(eventOf(chain))) return@hookMethod chain.proceed()
                    logKeyOnce("已拦截无障碍服务的按键消费")
                    false
                }
            }
        }
    }

    
    
 	
    
    private fun hookInputPipeline() {
        runCatching {
            val c = frameworkCls("android.view.InputEventReceiver") ?: return@runCatching
            c.declaredMethods.filter { it.name == "finishInputEvent" }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!guarded(eventOf(chain))) return@hookMethod chain.proceed()
                    val args = runCatching { chain.args }.getOrNull() ?: return@hookMethod chain.proceed()
                    val out = args.toMutableList()
                    val i = out.indexOfLast { it is Boolean }
                    if (i < 0) return@hookMethod chain.proceed()
                    out[i] = false
                    logKeyOnce("已把 finishInputEvent 的 handled 改成 false")
                    chain.proceed(out.toTypedArray())
                }
            }
        }
    }
}
