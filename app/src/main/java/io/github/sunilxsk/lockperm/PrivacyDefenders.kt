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
        if (list.isEmpty()) return false
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
























internal class ScreenCaptureDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    companion object {
        
        private const val REPAINT_INTERVAL_MS = 33L

        
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

        
        private val NAME_RE =
            Regex("^media[_-]?projection\\.(jpg|jpeg|png|mp4)$", RegexOption.IGNORE_CASE)
    }

    fun install() {
        val cfg = snapshot()
        if (!cfg.blockScreenCapture) return
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

    private fun mode(): Int = snapshot().screenCaptureMode

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
        ensurePainter()
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
                        val wantVideo = mode() == XpConfig.SC_MODE_IMAGE
                        val mp4 = if (wantVideo) findMediaFile()?.takeIf { isMp4(it.name) } else null

                        val list = synchronized(SURFACES) { SURFACES.toList() }
                        if (mp4 != null) {
                            
                            for (s in list) ensureVideo(s, mp4)
                        } else {
                            stopAllVideos()
                            for (s in list) {
                                if (!running || !faking()) break
                                if (!paintSurface(s)) {
                                    val n = (FAILS[s] ?: 0) + 1
                                    FAILS[s] = n
                                    if (n > 20) {
                                        synchronized(SURFACES) { SURFACES.remove(s) }
                                        FAILS.remove(s)
                                    }
                                } else {
                                    FAILS[s] = 0
                                }
                            }
                        }
                    } else {
                        stopAllVideos()
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

    private fun paintSurface(s: Surface): Boolean {
        
        if (VIDEOS.containsKey(s)) return true
        return runCatching {
            val canvas = s.lockCanvas(null) ?: return false
            try {
                if (mode() == XpConfig.SC_MODE_IMAGE) drawImageFrame(canvas) else drawBlankFrame(canvas)
            } finally {
                runCatching { s.unlockCanvasAndPost(canvas) }
            }
            true
        }.getOrDefault(false)
    }

    private fun ensureVideo(s: Surface, f: File) {
        val exist = VIDEOS[s]
        if (exist != null && exist.path == f.absolutePath) return
        exist?.stop()
        val feed = VideoFeed(f.absolutePath, s)
        if (feed.start()) {
            VIDEOS[s] = feed
            
            val old = audio
            old?.stop()
            val feed = AudioFeed(f.absolutePath)
            val started = runCatching { feed.start() }.getOrDefault(false)
            audio = if (started) feed else null
            if (!started) feed.stop()
            val err = feed.lastError
            notifyOnce("video|${f.absolutePath}|${f.lastModified()}") {
                when {
                    started -> "屏幕捕获(动态画面)：已播放 ${f.name}（含音轨）"
                    err != null -> "屏幕捕获：${f.name} 音轨未启用（$err）"
                    else -> "屏幕捕获：${f.name} 音轨未启用（未知原因）"
                }
            }
        } else {
            feed.stop()
        }
    }

    private fun stopAllVideos() {
        val list = synchronized(VIDEOS) { VIDEOS.values.toList() }
        for (v in list) v.stop()
        synchronized(VIDEOS) { VIDEOS.clear() }
        audio?.stop()
        audio = null
    }

    



    private fun drawBlankFrame(canvas: Canvas) {
        canvas.drawColor(Color.DKGRAY)
        val p = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "内容已被拦截",
            canvas.width / 2f,
            canvas.height / 2f,
            p,
        )
    }

    



    private fun drawImageFrame(canvas: Canvas) {
        val bmp = loadOverlay()
        if (bmp == null) {
            drawBlankFrame(canvas)
            return
        }
        canvas.drawColor(Color.BLACK)
        val cw = canvas.width.toFloat()
        val ch = canvas.height.toFloat()
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val scale = maxOf(cw / bw, ch / bh)
        val dw = bw * scale
        val dh = bh * scale
        val dst = RectF((cw - dw) / 2f, (ch - dh) / 2f, (cw - dw) / 2f + dw, (ch - dh) / 2f + dh)
        canvas.drawBitmap(bmp, null, dst, null)
    }

    

    private fun isMp4(name: String): Boolean = name.lowercase().endsWith(".mp4")

    private fun findMediaFile(): File? {
        val ctx = runCatching { appContext() }.getOrNull() ?: return null
        val dir = runCatching { ctx.getExternalFilesDir(null) }.getOrNull() ?: return null
        val arr = runCatching { dir.listFiles() }.getOrNull() ?: return null
        return arr.firstOrNull { it.isFile && NAME_RE.matches(it.name) }
    }

    
    private fun loadOverlay(): Bitmap? {
        val f = findMediaFile()
        if (f == null) {
            notifyOnce("missing") { "屏幕捕获(静态图)：没找到素材，已降级为方案B（灰底提示）" }
            return null
        }
        if (isMp4(f.name)) {
            notifyOnce("ismp4|${f.absolutePath}") { "屏幕捕获：检测到 ${f.name}，按动态画面播放" }
            return null
        }
        val key = "${f.absolutePath}|${f.lastModified()}"
        val c = cached
        if (c != null && c.first == key) return c.second
        val bmp = runCatching { decodeScaled(f) }.getOrNull()
        cached = key to bmp
        if (bmp == null) {
            notifyOnce("bad|$key") { "屏幕捕获(静态图)：图片读取失败，已降级为方案B（灰底提示）" }
            return null
        }
        notifyOnce("ok|$key") { "屏幕捕获(静态图)：已使用 ${f.name}" }
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

    private fun notifyOnce(state: String, msg: () -> String) {
        if (notifiedState == state) return
        notifiedState = state
        val text = runCatching { msg() }.getOrNull() ?: return
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

                    when (first) {
                        is ShortArray -> {
                            val off = (args.getOrNull(1) as? Int)?.coerceIn(0, first.size) ?: 0
                            val want = n.coerceAtMost(first.size - off)
                            if (want <= 0) return@hookMethod 0
                            val got = audio?.readShorts(first, off, want, rate, dstCh) ?: 0
                            if (got < want) first.fill(0, off + got, off + want)
                            want
                        }
                        is ByteArray -> {
                            val off = (args.getOrNull(1) as? Int)?.coerceIn(0, first.size) ?: 0
                            val want = n.coerceAtMost(first.size - off)
                            if (want <= 0) return@hookMethod 0
                            val frames = want / 2
                            val tmp = ShortArray(frames)
                            
                            val got = audio?.readShorts(tmp, 0, frames, rate, dstCh) ?: 0
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
                            val got = audio?.readShorts(tmp, 0, frames, rate, dstCh) ?: 0
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
        ensurePainter()
    }

    private fun resetAll() {
        runCatching {
            stopAllVideos()
            synchronized(SURFACES) { SURFACES.clear() }
            FAILS.clear()
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










private class VideoFeed(val path: String, private val surface: Surface) {

    @Volatile
    private var stopFlag = false

    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var thread: Thread? = null

    
    private var durationUs = 0L
    
    private var roundOffsetUs = 0L
    
    private var lastPtsUs = 0L
    
    private var frameUs = 33_000L

    fun start(): Boolean {
        return runCatching {
            val ex = MediaExtractor()
            ex.setDataSource(path)
            var track = -1
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = runCatching { f.getString(MediaFormat.KEY_MIME) }.getOrNull()
                if (mime?.startsWith("video/") == true) { track = i; break }
            }
            if (track < 0) { ex.release(); return false }
            ex.selectTrack(track)
            val fmt = ex.getTrackFormat(track)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return false
            durationUs = runCatching { fmt.getLong(MediaFormat.KEY_DURATION) }.getOrDefault(0L)
            val fps = runCatching { fmt.getInteger(MediaFormat.KEY_FRAME_RATE) }.getOrDefault(0)
            if (fps > 0) frameUs = 1_000_000L / fps
            val cd = MediaCodec.createDecoderByType(mime)

            
            val useSurface = runCatching {
                cd.configure(fmt, surface, null, 0)
                true
            }.getOrDefault(false)
            if (!useSurface) {
                runCatching { cd.configure(fmt, null, null, 0) }
            }

            cd.start()
            extractor = ex
            codec = cd

            val t = Thread({
                if (useSurface) loopSurface(cd, ex) else loopCanvas(cd, ex)
            }, "xp-sc-video")
            t.isDaemon = true
            thread = t
            t.start()
            true
        }.getOrDefault(false)
    }

    






    private fun loopSurface(cd: MediaCodec, ex: MediaExtractor) {
        val info = MediaCodec.BufferInfo()
        val sawInputEos = false 
        var firstPtsUs = -1L
        var startNs = 0L
        while (!stopFlag) {
            
            if (!sawInputEos) {
                val inIdx = runCatching { cd.dequeueInputBuffer(10000) }.getOrDefault(-1)
                if (inIdx >= 0) {
                    val buf = runCatching { cd.getInputBuffer(inIdx) }.getOrNull()
                    if (buf != null) {
                        val size = runCatching { ex.readSampleData(buf, 0) }.getOrDefault(-1)
                        if (size < 0) {
                            
                            
                            
                            feedLoop(cd, ex, inIdx)
                        } else {
                            val pts = ex.sampleTime + roundOffsetUs
                            if (pts > lastPtsUs) lastPtsUs = pts
                            cd.queueInputBuffer(inIdx, 0, size, pts, 0)
                            ex.advance()
                        }
                    }
                }
            }
            
            val outIdx = runCatching { cd.dequeueOutputBuffer(info, 10000) }.getOrDefault(-1)
            when {
                outIdx >= 0 -> {
                    
                    waitForPts(info.presentationTimeUs, firstPtsUs, startNs)
                    if (firstPtsUs < 0L) {
                        firstPtsUs = info.presentationTimeUs
                        startNs = System.nanoTime()
                    }
                    runCatching { cd.releaseOutputBuffer(outIdx, true) }
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
            }
        }
    }

    
    private fun waitForPts(ptsUs: Long, firstPtsUs: Long, startNs: Long) {
        if (firstPtsUs < 0L || ptsUs <= firstPtsUs) return
        val targetNs = startNs + (ptsUs - firstPtsUs) * 1000L
        var waitNs = targetNs - System.nanoTime()
        
        if (waitNs > 2_000_000_000L) waitNs = 0L
        if (waitNs > 0) {
            runCatching {
                val ms = waitNs / 1_000_000L
                if (ms > 0) Thread.sleep(ms)
            }
        }
    }

    
    private fun loopCanvas(cd: MediaCodec, ex: MediaExtractor) {
        val info = MediaCodec.BufferInfo()
        val sawInputEos = false
        var firstPtsUs = -1L
        var startNs = 0L
        while (!stopFlag) {
            if (!sawInputEos) {
                val inIdx = runCatching { cd.dequeueInputBuffer(10000) }.getOrDefault(-1)
                if (inIdx >= 0) {
                    val buf = runCatching { cd.getInputBuffer(inIdx) }.getOrNull()
                    if (buf != null) {
                        val size = runCatching { ex.readSampleData(buf, 0) }.getOrDefault(-1)
                        if (size < 0) {
                            feedLoop(cd, ex, inIdx) 
                        } else {
                            val pts = ex.sampleTime + roundOffsetUs
                            if (pts > lastPtsUs) lastPtsUs = pts
                            cd.queueInputBuffer(inIdx, 0, size, pts, 0)
                            ex.advance()
                        }
                    }
                }
            }
            val outIdx = runCatching { cd.dequeueOutputBuffer(info, 10000) }.getOrDefault(-1)
            when {
                outIdx >= 0 -> {
                    waitForPts(info.presentationTimeUs, firstPtsUs, startNs)
                    if (firstPtsUs < 0L) {
                        firstPtsUs = info.presentationTimeUs
                        startNs = System.nanoTime()
                    }
                    val img = runCatching { cd.getOutputImage(outIdx) }.getOrNull()
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
                                        val scale = maxOf(cw / bmp.width, ch / bmp.height)
                                        val dw = bmp.width * scale
                                        val dh = bmp.height * scale
                                        val dst = RectF(
                                            (cw - dw) / 2f, (ch - dh) / 2f,
                                            (cw - dw) / 2f + dw, (ch - dh) / 2f + dh,
                                        )
                                        canvas.drawBitmap(bmp, null, dst, null)
                                    } finally {
                                        surface.unlockCanvasAndPost(canvas)
                                    }
                                }
                            }
                        }
                    }
                    runCatching { cd.releaseOutputBuffer(outIdx, false) }
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
            }
        }
    }

    




    private fun feedLoop(cd: MediaCodec, ex: MediaExtractor, inIdx: Int) {
        runCatching {
            val step = if (durationUs > 0) durationUs else (lastPtsUs + frameUs)
            roundOffsetUs = lastPtsUs + frameUs
            ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val buf = runCatching { cd.getInputBuffer(inIdx) }.getOrNull()
            if (buf != null) {
                val size = runCatching { ex.readSampleData(buf, 0) }.getOrDefault(-1)
                if (size >= 0) {
                    val pts = ex.sampleTime + roundOffsetUs
                    if (pts > lastPtsUs) lastPtsUs = pts
                    cd.queueInputBuffer(inIdx, 0, size, pts, 0)
                    ex.advance()
                } else {
                    cd.queueInputBuffer(inIdx, 0, 0, 0L, 0)
                }
            }
        }
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

    fun stop() {
        stopFlag = true
        runCatching { thread?.interrupt() }
        runCatching { codec?.stop(); codec?.release() }
        runCatching { extractor?.release() }
        thread = null
        codec = null
        extractor = null
    }
}

















private class AudioFeed(private val path: String) {

    companion object {
        
        private const val CAP = 48000 * 4
        private const val ENC_FLOAT = 4 
    }

    @Volatile
    private var stopFlag = false

    private val lock = Any()
    private val ring = ShortArray(CAP)
    private var writePos = 0
    private var readPos = 0
    private var avail = 0

    
    private var posFrac = 0.0
    
    private var prev = 0

    @Volatile
    private var srcRate = 44100
    
    @Volatile
    private var pcmFloat = false
    
    @Volatile
    var lastError: String? = null
        private set

    private var codec: MediaCodec? = null
    private var extractor: MediaExtractor? = null
    private var thread: Thread? = null

    fun start(): Boolean {
        return runCatching {
            val ex = MediaExtractor()
            ex.setDataSource(path)
            var track = -1
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = runCatching { f.getString(MediaFormat.KEY_MIME) }.getOrNull()
                if (mime?.startsWith("audio/") == true) { track = i; break }
            }
            if (track < 0) {
                lastError = "无音轨"
                ex.release()
                return false
            }
            ex.selectTrack(track)
            val fmt = ex.getTrackFormat(track)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: run {
                lastError = "无 mime"
                return false
            }
            srcRate = runCatching { fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) }.getOrDefault(44100)
            val ch = runCatching { fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrDefault(1)

            val cd = MediaCodec.createDecoderByType(mime)
            
            val ok = runCatching { cd.configure(fmt, null, null, 0) }.isSuccess
            if (!ok) {
                runCatching { fmt.setInteger(MediaFormat.KEY_PCM_ENCODING, 2) }
                    .onFailure { fmt.removeKey(MediaFormat.KEY_PCM_ENCODING) }
                cd.configure(fmt, null, null, 0)
            }
            cd.start()
            extractor = ex
            codec = cd
            val t = Thread({ loop(cd, ex, ch) }, "xp-sc-audio")
            t.isDaemon = true
            t.priority = Thread.NORM_PRIORITY + 1
            thread = t
            t.start()
            true
        }.onFailure { lastError = it.message }.getOrDefault(false)
    }

    private fun loop(cd: MediaCodec, ex: MediaExtractor, ch: Int) {
        val info = MediaCodec.BufferInfo()
        while (!stopFlag) {
            val inIdx = runCatching { cd.dequeueInputBuffer(10000) }.getOrDefault(-1)
            if (inIdx >= 0) {
                val buf = runCatching { cd.getInputBuffer(inIdx) }.getOrNull()
                if (buf != null) {
                    val size = runCatching { ex.readSampleData(buf, 0) }.getOrDefault(-1)
                    if (size < 0) {
                        
                        runCatching {
                            ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            val nb = cd.getInputBuffer(inIdx)
                            val ns = ex.readSampleData(nb!!, 0)
                            if (ns >= 0) {
                                cd.queueInputBuffer(inIdx, 0, ns, 0L, 0)
                                ex.advance()
                            } else {
                                cd.queueInputBuffer(inIdx, 0, 0, 0L, 0)
                            }
                        }
                    } else {
                        cd.queueInputBuffer(inIdx, 0, size, ex.sampleTime, 0)
                        ex.advance()
                    }
                }
            }
            val outIdx = runCatching { cd.dequeueOutputBuffer(info, 10000) }.getOrDefault(-1)
            when {
                outIdx >= 0 -> {
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0) {
                        val buf = runCatching { cd.getOutputBuffer(outIdx) }.getOrNull()
                        if (buf != null) writePcm(buf, info.size, ch)
                    }
                    runCatching { cd.releaseOutputBuffer(outIdx, false) }
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    runCatching {
                        val f = cd.outputFormat
                        srcRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        
                        pcmFloat = runCatching {
                            f.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }.getOrDefault(2) == ENC_FLOAT
                    }
                }
            }
        }
    }

    
    private fun writePcm(buf: java.nio.ByteBuffer, size: Int, ch: Int) {
        val c = ch.coerceIn(1, 8)
        val isFloat = pcmFloat
        val unit = if (isFloat) 4 else 2
        val frames = (size / unit / c).coerceAtLeast(0)
        if (frames <= 0) return
        val src = runCatching {
            buf.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        }.getOrNull() ?: return
        val fb = runCatching {
            if (isFloat) src.asFloatBuffer() else null
        }.getOrNull()
        val sb = if (!isFloat) runCatching { src.asShortBuffer() }.getOrNull() else null
        if (fb == null && sb == null) return

        synchronized(lock) {
            for (i in 0 until frames) {
                if (avail >= ring.size) {
                    
                    val drop = (ring.size / 8).coerceAtLeast(1)
                    readPos = (readPos + drop) % ring.size
                    avail -= drop
                    if (avail < 0) avail = 0
                }
                var sum = 0.0
                var cnt = 0
                for (k in 0 until c) {
                    if (isFloat) {
                        if (fb == null || !fb.hasRemaining()) break
                        val fv = fb.get()
                        sum += if (fv < -1f) -1.0 else if (fv > 1f) 1.0 else fv.toDouble()
                        cnt++
                    } else {
                        if (sb == null || !sb.hasRemaining()) break
                        sum += sb.get().toInt()
                        cnt++
                    }
                }
                if (cnt == 0) break
                val v = if (isFloat) {
                    (sum / cnt * 32767.0).toInt()
                } else {
                    (sum / cnt).toInt()
                }
                ring[writePos] = v.coerceIn(-32768, 32767).toShort()
                writePos++
                if (writePos >= ring.size) writePos = 0
                avail++
            }
        }
    }

    



    fun readShorts(out: ShortArray, off: Int, want: Int, targetRate: Int, dstCh: Int): Int {
        val dc = dstCh.coerceIn(1, 8)
        val frames = want / dc
        if (frames <= 0) return 0
        val rate = targetRate.coerceIn(4000, 192000)
        val ratio = srcRate.coerceAtLeast(1).toDouble() / rate.toDouble()

        synchronized(lock) {
            var done = 0
            for (i in 0 until frames) {
                while (posFrac >= 1.0) {
                    if (avail <= 0) { posFrac = 0.0; break }
                    prev = ring[readPos].toInt()
                    readPos++
                    if (readPos >= ring.size) readPos = 0
                    avail--
                    posFrac -= 1.0
                }
                val next = if (avail > 0) ring[readPos].toInt() else prev
                val t = posFrac.coerceIn(0.0, 1.0)
                val v = (prev + (next - prev) * t).toInt().coerceIn(-32768, 32767).toShort()
                posFrac += ratio

                val base = off + i * dc
                for (k in 0 until dc) {
                    if (base + k < out.size) out[base + k] = v
                }
                done++
            }
            return done * dc
        }
    }

    fun stop() {
        stopFlag = true
        runCatching { thread?.interrupt() }
        runCatching { codec?.stop(); codec?.release() }
        runCatching { extractor?.release() }
        thread = null
        codec = null
        extractor = null
    }
}
