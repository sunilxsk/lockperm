package io.github.sunilxsk.lockperm

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale













internal object ControlPanel {

    private val mainHandler = Handler(Looper.getMainLooper())

    
    @Volatile
    private var attached: FrameLayout? = null

    
    @Volatile
    private var lpRef: WindowManager.LayoutParams? = null

    @Volatile
    private var hostActivity: Activity? = null

    @Volatile
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    
    
    private val logLock = Any()

    





    private fun snapshotBuf(areaIdx: Int): Pair<List<String>, Int> =
        synchronized(logLock) {
            val b = if (areaIdx == 1) log2 else log1
            b.toList() to lineTotal[areaIdx]
        }

    
    private val lastLine = arrayOfNulls<String>(2)

    
    private val dupCount = intArrayOf(1, 1)

    
    private fun stripStamp(line: String): String {
        var s = line
        val e = s.indexOf(']')
        if (s.startsWith("[") && e > 0 && e < 10) s = s.substring(e + 1).trimStart()
        val m = Regex(""" x\d+$""").find(s)
        if (m != null) s = s.substring(0, m.range.first)
        return s
    }

    






    private const val MAX_LINES = 400
    private val log1 = ArrayDeque<String>()
    private val log2 = ArrayDeque<String>()

    





    private val NOISE = listOf(
        
        "Empty SMPTE 2094-40 data",
        
        "err open mi_exception_log",
        "err write to mi_exception_log",
        "err open binder_delay",
        
        "avc: denied",
        "avc: granted",
        
        "BLASTBufferQueue",
        "BufferQueueConsumer",
        "BufferQueueProducer",
        "Expecting binder but got null",
        "hardware acceleration = true",
        "acquireNextBufferLocked",
        
        "fbcNotifySbeRescue",
        "perf_ioctl",
        "MiuiProcessManagerImpl",
        "PowerHalWrapper",
        "ProcessProfilingInfo",
        
        "non sticky GC",
        "Compiler allocated",
        "meow new tls",
        "meow delete tls",
        "meow reload",
        "Attempt to remove non-JNI local reference",
        "Access denied finding property",
        "ziparchive",
        "JIT profile information",
        "Unsupported class loader",
        "ClassLoaderContext",
        "Compat change id reported",
        "OpenSSLMessageDigest",
        "NetworkSecurityConfig",
        "No Network Security Config",
        "libmagtsync.so",
        "libMiGL",
        "QT",
        "libMEOW",
        "MiuiMultiWindowAdapter",
        "MiuiForceDarkConfig",
        "ForceDarkHelperStubImpl",
        "IS_CTS_MODE",
        "MULTI_WINDOW",
        "MSYNC3-VariableRefreshRate",
        "DecorView[]",
        "getWindowModeFromSystem",
        "onWindowFocusChanged",
        "HandWritingStubImpl",
        "ScoutStateMachine",
        "ApplicationLoaders",
        "MessageMonitor",
    )

    @Volatile
    private var logArea: Int = 1

    @Volatile
    private var consoleView: TextView? = null

    @Volatile
    private var minimized: Boolean = false

    @Volatile
    private var hidden: Boolean = false

    





    @Volatile
    private var darkTheme: Boolean = isNightNow()

    
    private fun isNightNow(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour < 6 || hour >= 18
    }

    
    @Volatile
    private var picking: Boolean = false

    
    @Volatile
    private var pausedByBackground = false

    @Volatile
    private var highlight: HighlightView? = null

    
    @Volatile
    private var lastVolume: Int = -1

    @Volatile
    private var volumeWatcher: Runnable? = null

    private const val HEIGHT_BOOST = 1.2f
    private const val BUTTON_SCALE = 0.95f

    
    @Synchronized
    fun currentActivity(): Activity? = hostActivity

    

    




    fun appendLog(area: Int, msg: String) {
        
        if (area == 2 && NOISE.any { msg.contains(it) }) return

        
        
        
        
        synchronized(logLock) { appendLogLocked(area, msg) }
        if (logArea == area) postConsole()
    }

    
    private fun appendLogLocked(area: Int, msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] $msg"
        val idx = area - 1
        val buf = if (area == 2) log2 else log1

        
        
        
        val last = lastLine[idx]
        if (last != null && stripStamp(last) == msg) {
            dupCount[idx]++
            buf.removeLastOrNull()
            buf.addLast("[$time] $msg x${dupCount[idx]}")
            lineTotal[idx] = lineTotal[idx] + 1
            return
        }
        dupCount[idx] = 1

        
        while (buf.size >= MAX_LINES) buf.removeFirstOrNull()
        buf.addLast(line)
        lastLine[idx] = line
        lineTotal[idx] = lineTotal[idx] + 1
    }

    private fun log(msg: String) = appendLog(1, msg)

    



    @Volatile
    private var lastUserScrollAt = 0L

    



    @Volatile
    private var userPeeking = false

    
    @Volatile
    private var pendingRefresh = false

    
    private const val SCROLL_PAUSE_MS = 5000L

    
    private const val SCROLL_CHECK_MS = 500L

    
    private const val CONSOLE_FLUSH_MS = 80L

    
    @Volatile
    private var consoleScrollView: ScrollView? = null

    
    @Volatile
    private var flushScheduled = false

    
    private val resumeRunnable = object : Runnable {
        override fun run() {
            if (!userPeeking) return
            val idle = System.currentTimeMillis() - lastUserScrollAt
            if (idle < SCROLL_PAUSE_MS) {
                
                mainHandler.postDelayed(this, SCROLL_CHECK_MS)
                return
            }
            
            userPeeking = false
            pendingRefresh = false
            flushConsole()
        }
    }

    
    private const val BOTTOM_SLOP_DP = 24

    private fun bottomSlopPx(): Int {
        val sv = consoleScrollView
        val dm = runCatching { sv?.resources?.displayMetrics?.density }.getOrNull() ?: 2f
        return (BOTTOM_SLOP_DP * dm).toInt()
    }

    
    private fun isAtBottom(): Boolean {
        val sv = consoleScrollView ?: return true
        val tv = consoleView ?: return true
        val max = (tv.height - sv.height).coerceAtLeast(0)
        return (max - sv.scrollY) <= bottomSlopPx()
    }

    
    private fun resumeAutoIfNeeded() {
        if (!userPeeking) return
        userPeeking = false
        pendingRefresh = false
        mainHandler.removeCallbacks(resumeRunnable)
        runCatching { flushConsole() }
    }

    
    private fun markUserScroll() {
        if (!userPeeking) {
            userPeeking = true
            
            mainHandler.removeCallbacks(resumeRunnable)
            mainHandler.postDelayed(resumeRunnable, SCROLL_CHECK_MS)
        }
        lastUserScrollAt = System.currentTimeMillis()
    }

    
    @Volatile
    private var programmaticScroll = false

    






    private val scrollRunnable = Runnable {
        val sv = consoleScrollView ?: return@Runnable
        val tv = consoleView ?: return@Runnable
        programmaticScroll = true
        runCatching {
            val target = (tv.height - sv.height).coerceAtLeast(0)
            sv.scrollTo(0, target)
        }
        
        sv.post { programmaticScroll = false }
    }

    





    private fun scrollToBottom() {
        if (consoleScrollView == null) return
        mainHandler.removeCallbacks(scrollRunnable)
        mainHandler.post(scrollRunnable)
    }

    





    private val lineTotal = intArrayOf(0, 0)

    
    private val renderedTotal = intArrayOf(0, 0)

    
    private fun flushConsole() {
        val tv = consoleView ?: return
        
        if (userPeeking) {
            pendingRefresh = true
            return
        }
        val idx = logArea - 1
        if (lineTotal[idx] - renderedTotal[idx] <= 0) {
            
            scrollToBottom()
            return
        }
        
        
        
        
        
        
        
        
        
        runCatching {
            val (lines, total) = snapshotBuf(idx)
            tv.text = colorize(lines)
            renderedTotal[idx] = total
        }
        scrollToBottom()
    }

    
    private fun rebuildConsole() {
        val tv = consoleView ?: return
        runCatching {
            val i = logArea - 1
            val (lines, total) = snapshotBuf(i)
            tv.text = colorize(lines)
            renderedTotal[i] = total
        }
        scrollToBottom()
    }

    





    private fun postConsole() {
        
        if (mainHandler.looper.thread === Thread.currentThread()) {
            runCatching { flushConsole() }
            return
        }
        if (flushScheduled) return
        flushScheduled = true
        mainHandler.postDelayed({
            flushScheduled = false
            runCatching { flushConsole() }
        }, CONSOLE_FLUSH_MS)
    }

    





    private fun colorize(lines: Collection<String>): CharSequence {
        val sb = android.text.SpannableStringBuilder()
        lines.forEachIndexed { idx, raw ->
            if (idx > 0) sb.append('\n')
            
            var body = raw
            val close = raw.indexOf(']')
            if (raw.startsWith("[") && close > 0) {
                sb.append(raw, 0, close + 1)
                body = raw.substring(close + 1)
                if (body.startsWith(' ')) {
                    sb.append(' ')
                    body = body.substring(1)
                }
            }
            val lvl = body.firstOrNull()?.takeIf { it in "VDIWEF" && body.getOrNull(1) == '/' }
            val color: Int
            if (lvl != null) {
                color = levelColor(lvl)
            } else {
                
                val isErr = ERR_WORDS.any { body.contains(it) }
                color = if (isErr) levelColor('E') else levelColor('I')
                sb.append(if (isErr) "E " else "I ")
            }
            val start = sb.length
            sb.append(body)
            sb.setSpan(
                android.text.style.ForegroundColorSpan(color),
                start, sb.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return sb
    }

    
    private fun levelColor(lvl: Char): Int = when (lvl) {
        'V' -> if (darkTheme) 0xFF9E9E9E.toInt() else 0xFF757575.toInt()
        'D' -> if (darkTheme) 0xFF6EC6FF.toInt() else 0xFF0277BD.toInt()
        'I' -> if (darkTheme) 0xFF66BB6A.toInt() else 0xFF2E7D32.toInt()
        'W' -> if (darkTheme) 0xFFFFB74D.toInt() else 0xFFEF6C00.toInt()
        'E' -> if (darkTheme) 0xFFEF5350.toInt() else 0xFFC62828.toInt()
        else -> if (darkTheme) 0xFFF06292.toInt() else 0xFFAD1457.toInt() 
    }

    
    private val ERR_WORDS = listOf("失败", "错误", "异常", "failed", "error", "Error", "崩溃")

    private fun currentBuf(): ArrayDeque<String> = if (logArea == 2) log2 else log1

    

    
    @Volatile
    private var hooksInstalled = false

    fun install(module: XposedModule, prefs: SharedPreferences, classLoader: ClassLoader) {
        
        
        if (hooksInstalled) return
        hooksInstalled = true
        runCatching {
            val onResume = Activity::class.java.getDeclaredMethod("onResume")
            module.hook(onResume)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    val act = chain.getThisObject() as? Activity
                    if (act != null) {
                        pausedByBackground = false
                        
                        
                        
                        
                        val alreadyHere = attached != null && hostActivity === act
                        if (!alreadyHere) {
                            mainHandler.postDelayed({
                                runCatching {
                                    val cfg = XpState.refresh(prefs)
                                    if (cfg.panelInject) attach(act, cfg)
                                }
                            }, 600)
                        }
                    }
                    result
                }
        }

        
        
        
        runCatching {
            val onDestroy = Activity::class.java.getDeclaredMethod("onDestroy")
            module.hook(onDestroy)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val act = chain.getThisObject() as? Activity
                    if (act != null && act === hostActivity) {
                        mainHandler.post { runCatching { detach() } }
                    }
                    chain.proceed()
                }
        }

        
        runCatching {
            val onStop = Activity::class.java.getDeclaredMethod("onStop")
            module.hook(onStop)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val act = chain.getThisObject() as? Activity
                    if (act != null && act === hostActivity) {
                        
                        pausedByBackground = true
                    }
                    chain.proceed()
                }
        }

        runCatching { installEnhancedHooks(module) }
        
        log("本日志由 Hook 读取，如需查看完整日志，请使用 Logcat命令。")

        
        runCatching {
            val dispatch = Activity::class.java.getDeclaredMethod(
                "dispatchTouchEvent", MotionEvent::class.java
            )
            module.hook(dispatch)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val act = chain.getThisObject() as? Activity
                    val ev = runCatching { chain.getArg(0) as? MotionEvent }.getOrNull()
                    if (picking && act != null && ev != null) {
                        runCatching { handlePick(act, ev) }
                    }
                    chain.proceed()
                }
        }
    }

    

    @Synchronized
    private fun attach(activity: Activity, cfg: XpState.Snapshot) {
        if (attached != null) {
            if (hostActivity === activity) {
                runCatching { attached?.visibility = View.VISIBLE }
                return
            }
            detach()
        }
        val container = FrameLayout(activity)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = dp(activity, 8f)
        lp.y = dp(activity, 72f)
        lp.token = activity.window.decorView.windowToken
        lp.windowAnimations = 0

        runCatching {
            activity.windowManager.addView(container, lp)
            attached = container
            lpRef = lp
            hostActivity = activity
            minimized = false
            hidden = false
            
            if (startMinimized(activity)) showMini(activity) else showPanel(activity)
            
            mainHandler.postDelayed({ runCatching { applyRules(activity) } }, 800)

            val dec = activity.window.decorView
            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                var lastW = 0
                var lastH = 0
                override fun onGlobalLayout() {
                    val w = dec.width
                    val h = dec.height
                    if (w == lastW && h == lastH) return
                    lastW = w
                    lastH = h
                    runCatching { applySize(activity) }
                }
            }
            dec.viewTreeObserver.addOnGlobalLayoutListener(listener)
            layoutListener = listener
        }
    }

    @Synchronized
    private fun detach() {
        stopVolumeWatch()
        stopPicking()
        hidden = false
        minimized = false
        val view = attached ?: return
        val act = hostActivity
        val listener = layoutListener
        runCatching {
            if (act != null && listener != null) {
                act.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
        runCatching {
            if (act != null && view.isAttachedToWindow) act.windowManager.removeView(view)
        }
        attached = null
        hostActivity = null
        layoutListener = null
        lpRef = null
        consoleView = null
    }

    
    private fun applySize(activity: Activity) {
        val container = attached ?: return
        val lp = container.layoutParams as? WindowManager.LayoutParams ?: return
        val dec = activity.window.decorView
        val sw = if (dec.width > 0) dec.width else screenW(activity)
        val sh = if (dec.height > 0) dec.height else screenH(activity)
        if (minimized) {
            lp.width = dp(activity, 48f)
            lp.height = dp(activity, 48f)
        } else {
            lp.width = (sw - dp(activity, 16f)).coerceAtLeast(dp(activity, 200f))
            
            val base = (sh / 4f).coerceAtLeast(dp(activity, 180f).toFloat())
            val cap = sh * 0.45f
            lp.height = (base * HEIGHT_BOOST).coerceAtMost(cap * HEIGHT_BOOST).toInt()
        }
        lpRef = lp
        runCatching { activity.windowManager.updateViewLayout(container, lp) }
    }

    private fun screenW(activity: Activity): Int = activity.resources.displayMetrics.widthPixels
    private fun screenH(activity: Activity): Int = activity.resources.displayMetrics.heightPixels

    





    private fun shortSide(activity: Activity): Int =
        minOf(screenW(activity), screenH(activity))

    
    private fun attrWindowW(activity: Activity): Int {
        val ss = shortSide(activity)
        return minOf((ss * 0.72f).toInt(), (screenW(activity) * 0.9f).toInt())
    }

    
    private fun dialogWindowW(activity: Activity): Int {
        val d = activity.resources.displayMetrics.density
        val ss = shortSide(activity)
        return minOf((ss * 0.8f).toInt(), (420 * d).toInt(), (screenW(activity) * 0.9f).toInt())
    }

    

    private fun showPanel(activity: Activity) {
        val container = attached ?: return
        darkTheme = resolveTheme(activity)
        minimized = false
        hidden = false
        stopVolumeWatch()
        runCatching { container.visibility = View.VISIBLE }
        runCatching { container.removeAllViews() }
        runCatching { container.addView(buildPanel(activity)) }
        
        
        renderedTotal[0] = 0
        renderedTotal[1] = 0
        userPeeking = false
        pendingRefresh = false
        runCatching { rebuildConsole() }
        runCatching { applySize(activity) }
    }

    private fun showMini(activity: Activity) {
        val container = attached ?: return
        minimized = true
        runCatching { container.removeAllViews() }
        val btn = ImageButton(activity)
        val d = activity.resources.displayMetrics.density
        val icon = runCatching {
            activity.packageManager.getApplicationIcon(activity.packageName)
        }.getOrNull()
        if (icon != null) {
            runCatching {
                val bmp = android.graphics.Bitmap.createBitmap(
                    (40 * d).toInt(), (40 * d).toInt(),
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
                val c = Canvas(bmp)
                icon.setBounds(0, 0, c.width, c.height)
                icon.draw(c)
                btn.setImageBitmap(bmp)
            }
        } else {
            btn.setBackgroundColor(if (darkTheme) 0xCC000000.toInt() else 0xCCFFFFFF.toInt())
        }
        btn.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        val bg = GradientDrawable()
        bg.setColor(if (darkTheme) 0xCC000000.toInt() else 0xCCFFFFFF.toInt())
        bg.cornerRadius = 24 * d
        bg.setStroke((1 * d).toInt(), strokeColor())
        btn.background = bg
        
        
        
        val doExpand = {
            mainHandler.post {
                if (minimized) runCatching { showPanel(activity) }
            }
        }
        btn.setOnClickListener { doExpand() }
        
        attachDrag(activity, btn) { doExpand() }
        runCatching { container.addView(btn) }
        runCatching { applySize(activity) }
        runCatching { snapEdge(activity) }
    }

    





    @Synchronized
    private fun hidePanel(context: Context) {
        val container = attached ?: return
        val act = hostActivity ?: return
        hidden = true
        stopPicking()
        runCatching { if (container.isAttachedToWindow) act.windowManager.removeView(container) }
        log("已隐藏：按音量键可随时唤出（之前点过的功能都还在）")
        startVolumeWatch(context)
    }

    
    @Synchronized
    private fun showAgain(context: Context) {
        val container = attached ?: return
        val act = hostActivity ?: return
        val lp = lpRef ?: return
        hidden = false
        stopVolumeWatch()
        runCatching {
            if (!container.isAttachedToWindow) {
                lp.token = act.window.decorView.windowToken
                act.windowManager.addView(container, lp)
            }
        }
        runCatching { container.visibility = View.VISIBLE }
        runCatching { applySize(act) }
    }

    private fun startVolumeWatch(context: Context) {
        stopVolumeWatch()
        val am = runCatching {
            context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        }.getOrNull()
        if (am == null) {
            log("拿不到音量服务，无法用音量键唤出")
            return
        }
        runCatching { lastVolume = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) }
        val task = object : Runnable {
            override fun run() {
                if (attached == null || !hidden) {
                    stopVolumeWatch()
                    return
                }
                val cur = runCatching {
                    am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                }.getOrNull()
                
                
                if (cur != null && lastVolume >= 0 && cur != lastVolume && !pausedByBackground) {
                    lastVolume = cur
                    log("检测到音量变化，唤出面板")
                    showAgain(context)
                    return
                }
                if (cur != null) lastVolume = cur
                mainHandler.postDelayed(this, 100)
            }
        }
        volumeWatcher = task
        mainHandler.postDelayed(task, 100)
    }

    private fun stopVolumeWatch() {
        volumeWatcher?.let { runCatching { mainHandler.removeCallbacks(it) } }
        volumeWatcher = null
    }

    
    @Volatile
    private var snapAnimator: android.animation.ValueAnimator? = null

    
    private fun cancelSnapAnim() {
        snapAnimator?.let { runCatching { it.cancel() } }
        snapAnimator = null
    }

    




    private fun snapEdge(activity: Activity, animate: Boolean = true) {
        if (!minimized) return
        val container = attached ?: return
        val lp = container.layoutParams as? WindowManager.LayoutParams ?: return
        val w = screenW(activity)
        val size = dp(activity, 48f)
        val center = lp.x + size / 2
        val targetX = if (center < w / 2) 0 else (w - size)
        lp.y = lp.y.coerceIn(0, (screenH(activity) - size * 2).coerceAtLeast(0))

        if (!animate || lp.x == targetX) {
            lp.x = targetX
            lpRef = lp
            runCatching { activity.windowManager.updateViewLayout(container, lp) }
            return
        }

        cancelSnapAnim()
        val from = lp.x
        val anim = android.animation.ValueAnimator.ofInt(from, targetX).apply {
            duration = 260L
            
            interpolator = android.view.animation.OvershootInterpolator(0.6f)
            addUpdateListener {
                val v = it.animatedValue as? Int ?: return@addUpdateListener
                lp.x = v
                runCatching { activity.windowManager.updateViewLayout(container, lp) }
            }
        }
        snapAnimator = anim
        anim.start()
        lpRef = lp
    }

    

    private fun panelBg(): Int =
        if (darkTheme) 0xE6000000.toInt() else 0xE6FFFFFF.toInt()

    private fun textColor(): Int =
        if (darkTheme) Color.WHITE else Color.BLACK

    private fun btnBg(): Int =
        if (darkTheme) 0x40FFFFFF.toInt() else 0x40000000.toInt()

    private fun barBg(): Int =
        if (darkTheme) 0x33FFFFFF.toInt() else 0x33000000.toInt()

    private fun strokeColor(): Int =
        if (darkTheme) 0x66FFFFFF else 0x66000000.toInt()

    private fun consoleColor(): Int =
        if (darkTheme) 0xFFB0FFB0.toInt() else 0xFF006400.toInt()

    

    private fun buildPanel(context: Context): View {
        val act = context as? Activity
        val d = context.resources.displayMetrics.density

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        val bg = GradientDrawable()
        bg.setColor(panelBg())
        bg.cornerRadius = 14 * d
        bg.setStroke((1 * d).toInt(), strokeColor())
        root.background = bg

        darkTheme = resolveTheme(context)

        
        val bar = LinearLayout(context)
        bar.orientation = LinearLayout.HORIZONTAL
        
        
        bar.isBaselineAligned = false
        bar.setPadding((6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt())
        bar.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        val barBg2 = GradientDrawable()
        barBg2.setColor(barBg())
        barBg2.cornerRadius = 10 * d
        bar.background = barBg2
        root.addView(bar)

        if (act != null) {
            
            val mini = smallButton(context, "－")
            mini.setOnClickListener { runCatching { showMini(act) } }
            bar.addView(mini)

            
            val logBtn = smallButton(context, "日志$logArea")
            logBtn.setOnClickListener {
                logArea = if (logArea == 1) 2 else 1
                logBtn.text = "日志$logArea"
                
                rebuildConsole()
            }
            bar.addView(logBtn)

        }

        val title = TextView(context)
        title.text = "XP 掌控"
        title.setTextColor(textColor())
        title.textSize = 11f
        title.includeFontPadding = false
        title.setPadding((6 * d).toInt(), 0, 0, 0)
        title.layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f,
        )
        bar.addView(title)

        val hide = smallButton(context, "隐藏")
        hide.setOnClickListener {
            runCatching { hidePanel(context) }
            
            toast(context, "已隐藏：点击音量键后重现")
        }
        bar.addView(hide)

        
        val content = LinearLayout(context)
        content.orientation = LinearLayout.HORIZONTAL
        content.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
        )
        root.addView(content)

        val leftScroll = ScrollView(context)
        leftScroll.layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f,
        )
        val left = LinearLayout(context)
        left.orientation = LinearLayout.VERTICAL
        left.setPadding((6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
        left.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        buildFunctionButtons(context, left)
        leftScroll.addView(left)
        content.addView(leftScroll)

        val div = View(context)
        div.setBackgroundColor(barBg())
        div.layoutParams = LinearLayout.LayoutParams(
            (1 * d).toInt(), ViewGroup.LayoutParams.MATCH_PARENT,
        )
        content.addView(div)

        val right = LinearLayout(context)
        right.orientation = LinearLayout.VERTICAL
        right.layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 3f,
        )
        content.addView(right)

        val consoleScroll = ScrollView(context)
        consoleScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
        )
        val tv = TextView(context)
        tv.setTextColor(consoleColor())
        tv.textSize = 10f
        tv.typeface = android.graphics.Typeface.MONOSPACE
        tv.setPadding((8 * d).toInt(), (6 * d).toInt(), (8 * d).toInt(), (6 * d).toInt())
        tv.text = colorize(snapshotBuf(logArea - 1).first)
        
        tv.setTextIsSelectable(true)
        tv.isLongClickable = true
        tv.isFocusableInTouchMode = false
        consoleView = tv
        consoleScrollView = consoleScroll
        consoleScroll.addView(tv)
        right.addView(consoleScroll)

        
        
        
        
        
        
        
        
        consoleScroll.viewTreeObserver.addOnScrollChangedListener {
            
            
            if (programmaticScroll) return@addOnScrollChangedListener
            if (isAtBottom()) resumeAutoIfNeeded() else markUserScroll()
        }

        val toolsScroll = android.widget.HorizontalScrollView(context)
        toolsScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        val tools = LinearLayout(context)
        tools.orientation = LinearLayout.HORIZONTAL
        tools.setPadding((6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
        tools.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        addTool(tools, "生成UI树") { dumpUiTree(act) }
        addTool(tools, "当前界面") { dumpActivity(act) }
        addTool(tools, "配置快照") { dumpConfig(context) }
        addTool(tools, "系统信息") { dumpSystem(context) }
        addTool(tools, "权限自检") { dumpPermissions(context) }
        addTool(tools, "线程堆栈") { dumpStack() }
        
        addTool(tools, "切换主题") { toggleTheme(context, clear = false) }
            .setOnLongClickListener {
                toggleTheme(context, clear = true)
                true
            }
        addTool(tools, "复制") { copyConsole(context) }
        addTool(tools, "保存") { saveConsole(context) }
        addTool(tools, "清空") { clearConsole() }
        addTool(tools, "切换状态") { toggleStartMode(context) }
        toolsScroll.addView(tools)
        right.addView(toolsScroll)

        attachDrag(context, bar)

        if (log1.isEmpty() && logArea == 1) {
            log("控制台就绪。右侧是调试工具，左侧是一键功能。")
        }
        return root
    }

    






    private fun smallButton(context: Context, text: String): TextView {
        val d = context.resources.displayMetrics.density
        val btn = TextView(context)
        btn.text = text
        btn.textSize = 10f * BUTTON_SCALE
        btn.setTextColor(textColor())
        btn.gravity = Gravity.CENTER
        
        btn.includeFontPadding = false
        btn.setPadding(
            (7 * d * BUTTON_SCALE).toInt(), 0,
            (7 * d * BUTTON_SCALE).toInt(), 0,
        )
        val bg = GradientDrawable()
        bg.setColor(btnBg())
        bg.cornerRadius = 7 * d
        btn.background = bg
        btn.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, (22 * d).toInt(),
        ).apply { rightMargin = (4 * d).toInt() }
        return btn
    }

    private fun buildFunctionButtons(context: Context, left: LinearLayout) {
        val act = context as? Activity

        
        addButton(left, "拾取控件") {
            if (act == null) {
                log("拾取：拿不到当前界面")
                return@addButton
            }
            picking = !picking
            if (picking) startPicking(act) else stopPicking()
            log("拾取控件：${if (picking) "开启，手指划过即高亮，点击选定" else "关闭"}")
        }

        
        val exitHost = LinearLayout(context)
        exitHost.orientation = LinearLayout.VERTICAL
        exitHost.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        left.addView(exitHost)

        var expanded = false
        val toggle = addButton(exitHost, "退出 ▸") { }
        toggle.setOnClickListener {
            expanded = !expanded
            toggle.text = if (expanded) "退出 ▾" else "退出 ▸"
            while (exitHost.childCount > 1) {
                runCatching { exitHost.removeViewAt(exitHost.childCount - 1) }
            }
            if (!expanded) return@setOnClickListener
            XpConfig.EXIT_METHODS.forEach { (key, label) ->
                addButton(exitHost, label) {
                    log("退出：$label")
                    ExitExecutor.runSingle(key)
                }
            }
            addButton(exitHost, "所有方式并行") {
                log("并行执行所有退出方案")
                ExitExecutor.runNow(parallel = true, beforeExit = false)
            }
        }

        addButton(left, "拾取组件(增强版)") {
            val a = act
            if (a == null) {
                log("拿不到当前界面")
            } else if (enhancedPicking) {
                stopEnhancedPick()
            } else {
                stopPicking()
                startEnhancedPick(a)
            }
        }
        addButton(left, "执行 JS") {
            val a = hostActivity ?: act
            if (a == null) log("执行 JS：拿不到当前界面") else WebViewJsRunner.run(a)
        }
        addButton(left, "★ 一键全开") {
            val n = HookRuntime.allInOne()
            log("一键全开：$n 项已生效")
            toast(context, "一键全开：$n 项")
        }
        addButton(left, "阻止改壁纸") { runOne("阻止改壁纸") { HookRuntime.blockWallpaperNow() } }
        addButton(left, "阻止悬浮窗") { runOne("阻止悬浮窗") { HookRuntime.blockOverlayNow() } }
        addButton(left, "禁WiFi/蓝牙") { runOne("禁WiFi/蓝牙/亮度") { HookRuntime.blockConnNow() } }
        addButton(left, "禁传感器") { runOne("禁传感器") { HookRuntime.blockSensorNow() } }
        addButton(left, "阻止跳转") { runOne("阻止跳转") { HookRuntime.blockJumpNow() } }
        addButton(left, "禁摄像头麦克风") { runOne("禁摄像头/麦克风") { HookRuntime.blockCameraMicNow() } }
        addButton(left, "阻止安装") { runOne("阻止安装") { HookRuntime.blockInstallNow() } }
        addButton(left, "禁打印投屏") { runOne("禁打印/投屏") { HookRuntime.blockPrintCastNow() } }
        addButton(left, "拦截通知") { runOne("拦截通知") { HookRuntime.blockNotifyNow() } }
        addButton(left, "拦屏幕捕获") { runOne("拦屏幕捕获") { HookRuntime.blockScreenCaptureNow() } }
        addButton(left, "拦网络域名") { runOne("网络域名过滤") { HookRuntime.blockNetNow() } }
        addButton(left, "无障碍合一") { runOne("无障碍全部合一") { HookRuntime.accessibilityAllInOne() } }
        addButton(left, "设备管理员合一") { runOne("设备管理员全部合一") { HookRuntime.deviceAdminAllInOne() } }
        addButton(left, "关无障碍全能") {
            log("关闭无障碍全能")
            AccessibilityDefender.forceCloseAll()
        }
        addButton(left, "禁闪光灯振动") { runOne("禁闪光灯/振动") { HookRuntime.blockTorchVibrateNow() } }
        addButton(left, "禁剪贴板") { runOne("禁读写剪贴板") { HookRuntime.blockClipNow() } }
        addButton(left, "禁建文件") { runOne("禁随意创建文件") { HookRuntime.blockFileNow() } }
        addButton(left, "隐藏应用列表") { runOne("隐藏应用列表") { HookRuntime.blockHideAppsNow() } }
        addButton(left, "禁发声") { runOne("禁播放/发声音") { HookRuntime.blockAudioOutNow() } }
        addButton(left, "禁音量") { runOne("禁控制音量") { HookRuntime.blockVolumeNow() } }
        addButton(left, "禁无线调试") { runOne("禁获取无线调试") { HookRuntime.blockWdbgNow() } }
        addButton(left, "禁Shizuku") { runOne("禁 Shizuku") { HookRuntime.blockShizukuNow() } }
        addButton(left, "禁执行命令") { runOne("禁执行命令") { HookRuntime.blockExecNow() } }
        addButton(left, "立即结束进程") {
            log("结束进程")
            ExitExecutor.runNow(parallel = true, beforeExit = false)
        }
        addButton(left, "模拟返回键") {
            val a = hostActivity ?: act
            if (a == null) log("模拟返回：拿不到界面") else {
                runCatching { a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)) }
                runCatching { a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)) }
                log("已发送返回键")
            }
        }
        addButton(left, "回到桌面") {
            val ok = runCatching {
                val i = android.content.Intent(android.content.Intent.ACTION_MAIN)
                i.addCategory(android.content.Intent.CATEGORY_HOME)
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                true
            }.getOrDefault(false)
            log("回到桌面 → ${if (ok) "已发送" else "失败"}")
        }
        addButton(left, "清空剪贴板") {
            val ok = runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as? android.content.ClipboardManager
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                true
            }.getOrDefault(false)
            log("清空剪贴板 → ${if (ok) "已清空" else "失败"}")
        }

        
        
        addButton(left, "关FLAG_SECURE") {
            startFlagSecureWatch(context)
            toast(context, "已开始持续关闭所有窗口的 FLAG_SECURE")
        }
        addButton(left, "加FLAG_SECURE") {
            stopFlagSecureWatch()
            setFlagSecureAll(context, true)
            toast(context, "已为所有窗口添加 FLAG_SECURE")
            log("FLAG_SECURE → 已为所有窗口添加")
        }
        addButton(left, "关FLAG_SECURE(持久)") {
            addGlobalFlagSecureRule(context, enable = false)
            startFlagSecureWatch(context)
            toast(context, "已持久化：关闭所有窗口的 FLAG_SECURE")
            log("FLAG_SECURE 持久化 → 关闭所有窗口")
        }
        addButton(left, "加FLAG_SECURE(持久)") {
            stopFlagSecureWatch()
            setFlagSecureAll(context, true)
            addGlobalFlagSecureRule(context, enable = true)
            toast(context, "已持久化：添加所有窗口的 FLAG_SECURE")
            log("FLAG_SECURE 持久化 → 添加所有窗口")
        }
    }

    
    private fun stopFlagSecureWatch() {
        flagSecureOffAll = false
        runCatching { flagSecureThread?.interrupt() }
        flagSecureThread = null
    }

    






    private fun addGlobalFlagSecureRule(context: Context, enable: Boolean) {
        val arr = loadRules(context)
        
        val kept = org.json.JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val isGlobalFs = o.optString("action") == "flag_secure" &&
                    o.optString("scope", "window") == "all"
            if (!isGlobalFs) kept.put(o)
        }
        val o = org.json.JSONObject()
        o.put("action", "flag_secure")
        o.put("scope", "all")
        o.put("enable", enable)
        kept.put(o)
        saveRules(context, kept)
    }

    private fun runOne(name: String, block: () -> Boolean) {
        val ok = runCatching { block() }.getOrDefault(false)
        log("$name → ${if (ok) "已生效" else "失败"}")
        val ctx = hostActivity
        if (ctx != null) toast(ctx, if (ok) "$name 已生效" else "$name 失败")
    }

    

    
    private const val THEME_FLAG = "panel_theme.flag"

    private fun themeFile(context: Context): java.io.File? =
        runCatching { java.io.File(context.getExternalFilesDir(null), THEME_FLAG) }.getOrNull()

    
    private fun resolveTheme(context: Context): Boolean {
        val f = themeFile(context) ?: return isNightNow()
        return when (runCatching { f.readText() }.getOrNull()?.trim()) {
            "dark" -> true
            "light" -> false
            else -> isNightNow()
        }
    }

    




    private fun toggleTheme(context: Context, clear: Boolean) {
        val f = themeFile(context)
        if (clear) {
            if (f != null) runCatching { f.delete() }
            toast(context, "已恢复：按时间自动切换主题")
        } else {
            val next = !resolveTheme(context)
            if (f != null) {
                runCatching {
                    f.parentFile?.mkdirs()
                    f.writeText(if (next) "dark" else "light")
                }
            }
            toast(context, "主题已设为：${if (next) "深色" else "浅色"}（已持久化）")
        }
        val act = hostActivity ?: return
        darkTheme = resolveTheme(context)
        mainHandler.post { runCatching { showPanel(act) } }
    }

    

    
    private const val MINI_FLAG = "panel_start_minimized.flag"

    
    private fun flagFile(context: Context): java.io.File? =
        runCatching { java.io.File(context.getExternalFilesDir(null), MINI_FLAG) }.getOrNull()

    
    private fun startMinimized(context: Context): Boolean {
        val f = flagFile(context) ?: return false
        return runCatching { f.exists() }.getOrDefault(false)
    }

    





    private fun toggleStartMode(context: Context) {
        val f = flagFile(context)
        if (f == null) {
            toast(context, "切换失败：拿不到私有目录")
            return
        }
        val nowMini = runCatching { f.exists() }.getOrDefault(false)
        val ok = if (nowMini) {
            
            runCatching { f.delete() }.getOrDefault(false) || !runCatching { f.exists() }.getOrDefault(true)
        } else {
            runCatching {
                f.parentFile?.mkdirs()
                f.createNewFile()
            }.getOrDefault(false)
        }
        if (!ok) {
            toast(context, "切换失败：无法写入私有目录")
            return
        }
        
        val mini = runCatching { f.exists() }.getOrDefault(false)
        toast(
            context,
            if (mini) "下次进入默认最小化显示悬浮球" else "下次进入默认显示面板"
        )
        log("启动显示方式 → ${if (mini) "最小化悬浮球" else "展开面板"}")
    }

    

    private fun startPicking(activity: Activity) {
        pickDone = false
        runCatching {
            val dec = activity.window.decorView as? ViewGroup ?: return
            val hv = HighlightView(activity)
            
            hv.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            dec.addView(hv)
            highlight = hv
        }
    }

    private fun stopPicking() {
        picking = false
        val hv = highlight ?: return
        runCatching { (hv.parent as? ViewGroup)?.removeView(hv) }
        highlight = null
    }

    
    @Volatile
    private var pickDone = false

    private fun handlePick(activity: Activity, ev: MotionEvent) {
        
        if (pickDone) return
        val hv = highlight ?: return
        val root = activity.window.decorView as? ViewGroup ?: return
        val x = ev.rawX
        val y = ev.rawY
        
        
        
        val target = findViewAt(root, x.toInt(), y.toInt(), hv) ?: root
        mainHandler.post {
            runCatching {
                hv.setTarget(target, x, y)
                if (ev.action == MotionEvent.ACTION_UP && target != null && !pickDone) {
                    pickDone = true
                    dumpViewInfo(target)
                    stopPicking()
                    log("拾取完成（已退出拾取模式）")
                }
            }
        }
    }

    









    private fun findViewAt(root: View, x: Int, y: Int, skip: View?): View? {
        if (root === skip) return null
        val g = root as? ViewGroup ?: return root
        
        for (i in g.childCount - 1 downTo 0) {
            val child = g.getChildAt(i) ?: continue
            if (child === skip) continue
            if (child.visibility != View.VISIBLE) continue
            
            if (child.alpha < 0.05f) continue
            val loc = IntArray(2)
            runCatching { child.getLocationOnScreen(loc) }
            val w = child.width
            val h = child.height
            if (w <= 0 || h <= 0) continue
            val r = Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
            if (r.contains(x, y)) {
                
                val deeper = findViewAt(child, x, y, skip)
                return deeper ?: child
            }
        }
        return null
    }

    private fun dumpViewInfo(v: View) {
        val loc = IntArray(2)
        runCatching { v.getLocationOnScreen(loc) }
        val idName = runCatching {
            if (v.id > 0) v.resources.getResourceEntryName(v.id) else ""
        }.getOrDefault("")
        val text = (v as? TextView)?.text?.toString() ?: ""
        log("===== 控件属性 =====")
        log("  类名: ${v.javaClass.name}")
        log("  id: ${if (idName.isBlank()) "(无)" else "$idName (0x${Integer.toHexString(v.id)})"}")
        log("  text: ${if (text.isBlank()) "(无)" else text.take(80)}")
        log("  坐标: (${loc[0]}, ${loc[1]})  尺寸: ${v.width}x${v.height}")
        log("  padding: L${v.paddingLeft} T${v.paddingTop} R${v.paddingRight} B${v.paddingBottom}")
        log("  可点击: ${v.isClickable}  可长按: ${v.isLongClickable}  启用: ${v.isEnabled}")
        log("  焦点: ${v.isFocusable}  可见性: ${v.visibility}  alpha: ${v.alpha}")
        log("  contentDescription: ${v.contentDescription ?: "(无)"}")
        runCatching {
            val lp = v.layoutParams
            if (lp != null) log("  layoutParams: ${lp.javaClass.simpleName} ${lp.width}x${lp.height}")
        }
        log("====================")
    }

    
    private class HighlightView(context: Context) : View(context) {
        private var rect: Rect? = null
        private var label: String = ""
        private val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        private val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 28f
            isAntiAlias = true
        }
        private val bgPaint = Paint().apply { color = 0x33FF0000.toInt() }

        init {
            setWillNotDraw(false)
            isClickable = false
        }

        fun setTarget(v: View?, x: Float, y: Float) {
            if (v == null) {
                rect = null
                label = ""
            } else {
                val loc = IntArray(2)
                runCatching { v.getLocationOnScreen(loc) }
                rect = Rect(loc[0], loc[1], loc[0] + v.width, loc[1] + v.height)
                val id = runCatching {
                    if (v.id > 0) v.resources.getResourceEntryName(v.id) else ""
                }.getOrDefault("")
                label = buildString {
                    append(v.javaClass.simpleName)
                    if (id.isNotBlank()) append(" #").append(id)
                    append(" (${x.toInt()}, ${y.toInt()})")
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val r = rect ?: return
            canvas.drawRect(r, bgPaint)
            canvas.drawRect(r, paint)
            if (label.isNotBlank()) {
                canvas.drawText(label, r.left.toFloat(), (r.top - 8f).coerceAtLeast(30f), textPaint)
            }
        }
    }

    

    private fun clearConsole() {
        renderedTotal[logArea - 1] = 0
        lastLine[logArea - 1] = null
        dupCount[logArea - 1] = 1
        synchronized(logLock) { currentBuf().clear() }
        mainHandler.post { runCatching { consoleView?.text = "" } }
    }

    
    private fun saveConsole(context: Context) {
        val dir = runCatching { context.getExternalFilesDir(null) }.getOrNull()
        if (dir == null) {
            log("保存失败：拿不到私有目录")
            return
        }
        val path = runCatching {
            val name = if (logArea == 2) "xp_software.log" else XpConfig.CONSOLE_LOG_NAME
            val f = File(dir, name)
            f.writeText(snapshotBuf(logArea - 1).first.joinToString("\n"))
            f.absolutePath
        }.getOrNull()
        if (path == null) {
            log("保存失败")
        } else {
            log("已保存（日志 $logArea 区）：$path")
            toast(context, "已保存：$path")
        }
    }

    





    private fun copyConsole(context: Context) {
        val text = snapshotBuf(logArea - 1).first.joinToString("\n")
        if (text.isBlank()) {
            log("复制 → 当前日志区是空的，没什么可复制")
            return
        }
        val result = runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as? android.content.ClipboardManager
                ?: return@runCatching "拿不到 ClipboardManager"
            cm.setPrimaryClip(android.content.ClipData.newPlainText("xp_console", text))
            
            
            
            val back = runCatching { cm.primaryClip }.getOrNull()
            val got = runCatching {
                back?.getItemAt(0)?.coerceToText(context)?.toString()
            }.getOrNull().orEmpty()
            if (got.isNotEmpty()) "已复制 ${text.length} 字（回读校验通过）"
            else "已复制 ${text.length} 字（当前系统不允许回读，未校验）"
        }.getOrElse { "失败：${it.message ?: it.javaClass.simpleName}" }

        if (result.startsWith("已复制")) {
            log("复制 → $result")
            toast(context, result)
        } else {
            log("复制失败 → $result")
            toast(context, "复制失败：$result")
        }
    }

    private fun dumpUiTree(act: Activity?) {
        if (act == null) {
            log("生成 UI 树：拿不到当前界面")
            return
        }
        val root = runCatching { act.window.decorView }.getOrNull() ?: return
        log("===== UI 树 =====")
        val sb = StringBuilder()
        walk(root, 0, sb)
        sb.toString().lines().forEach { if (it.isNotBlank()) log(it) }
        log("===== 结束 =====")
    }

    private fun walk(view: View, depth: Int, sb: StringBuilder) {
        if (depth > 40) return
        val indent = "  ".repeat(depth)
        val name = view.javaClass.simpleName.ifBlank { view.javaClass.name }
        val id = runCatching {
            if (view.id > 0) view.resources.getResourceEntryName(view.id) else ""
        }.getOrDefault("")
        val extra = StringBuilder()
        (view as? TextView)?.text?.toString()?.let { t ->
            if (t.isNotBlank()) extra.append(" text=\"").append(t.take(40)).append('"')
        }
        val vis = when (view.visibility) {
            View.VISIBLE -> "V"
            View.INVISIBLE -> "I"
            else -> "G"
        }
        val loc = IntArray(2)
        runCatching { view.getLocationOnScreen(loc) }
        sb.append(indent).append(name)
        if (id.isNotBlank()) sb.append(" #").append(id)
        sb.append(" [$vis] ${view.width}x${view.height} @(${loc[0]},${loc[1]})")
        sb.append(extra).append('\n')
        (view as? ViewGroup)?.let { g ->
            for (i in 0 until g.childCount) {
                g.getChildAt(i)?.let { walk(it, depth + 1, sb) }
            }
        }
    }

    private fun dumpActivity(act: Activity?) {
        if (act == null) {
            log("拿不到当前 Activity")
            return
        }
        log("Activity: ${act.javaClass.name}")
        log("  包名: ${act.packageName}")
        log("  任务ID: ${act.taskId}  是否 finishing: ${act.isFinishing}")
        runCatching {
            act.intent?.let { i -> log("  Intent: action=${i.action} data=${i.data}") }
        }
        log("  根视图: ${runCatching { act.window.decorView.javaClass.name }.getOrDefault("?")}")
    }

    private fun dumpConfig(context: Context) {
        val cfg = HookRuntime.snapshot()
        if (cfg == null) {
            log("读不到配置")
            return
        }
        log("===== 当前生效配置 =====")
        log("  宿主: ${XpState.packageName}")
        log("  悬浮窗: ${cfg.panelInject}  退出: ${cfg.exitEnable}  无障碍: ${cfg.accEnable}")
        log("  壁纸: ${cfg.blockWallpaper}  悬浮窗拦截: ${cfg.blockOverlay}")
        log("  音量: ${cfg.volumeEnable}  发声: ${cfg.audioOutEnable}  剪贴板: ${cfg.clipEnable}")
        log("  手电筒: ${cfg.blockTorch}  振动: ${cfg.blockVibrate}")
        log("  文件: ${cfg.fileGuardEnable}  隐藏应用: ${cfg.hideAppsEnable}")
        log("  无线调试: ${cfg.wdbgEnable}  Shizuku: ${cfg.shizukuEnable}")
        log("  WiFi/蓝牙/亮度: ${cfg.blockConnEnable}  传感器: ${cfg.blockSensor}")
        log("  跳转: ${cfg.blockJumpEnable}  摄像头: ${cfg.blockCamera}  麦克风: ${cfg.blockMic}")
        log("  安装: ${cfg.blockInstall}  打印投屏: ${cfg.blockPrintCast}  通知: ${cfg.blockNotify}")
        log("  网络过滤: ${cfg.netFilterEnable}  屏幕捕获: ${cfg.blockScreenCapture}")
        log("  设备管理员: ${cfg.daEnable}  权限伪装: ${cfg.permEnable}")
        log("========================")
    }

    private fun dumpPermissions(context: Context) {
        log("===== 权限自检 =====")
        val want = listOf(
            android.Manifest.permission.CAMERA to "相机",
            android.Manifest.permission.RECORD_AUDIO to "麦克风",
            android.Manifest.permission.ACCESS_FINE_LOCATION to "定位",
            android.Manifest.permission.READ_CONTACTS to "通讯录",
            android.Manifest.permission.READ_SMS to "短信",
            android.Manifest.permission.READ_CALL_LOG to "通话记录",
            android.Manifest.permission.READ_CALENDAR to "日历",
        )
        want.forEach { (p, label) ->
            val r = runCatching { context.checkSelfPermission(p) }.getOrNull()
            val txt = when (r) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> "已授予"
                android.content.pm.PackageManager.PERMISSION_DENIED -> "未授予"
                else -> "未知($r)"
            }
            log("  $label: $txt")
        }
        log("  所有文件访问: ${runCatching { android.os.Environment.isExternalStorageManager() }.getOrNull()}")
        log("===================")
    }

    private fun dumpStack() {
        log("===== 当前线程堆栈 =====")
        Thread.currentThread().stackTrace.take(20).forEach { log("  $it") }
        log("========================")
    }

    private fun dumpSystem(context: Context) {
        val dm = context.resources.displayMetrics
        log("分辨率: ${dm.widthPixels}x${dm.heightPixels}  density=${dm.density}")
        log("Android SDK: ${android.os.Build.VERSION.SDK_INT}  机型: ${android.os.Build.MODEL}")
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024
        log("内存: 已用 ${used}MB / 最大 ${rt.maxMemory() / 1024 / 1024}MB")
        log("线程数: ${Thread.activeCount()}")
    }

    

    private fun addTool(root: LinearLayout, text: String, onClick: () -> Unit): TextView {
        val ctx = root.context
        val d = ctx.resources.displayMetrics.density
        val btn = TextView(ctx)
        btn.text = text
        btn.textSize = 9.5f * BUTTON_SCALE
        btn.setTextColor(textColor())
        btn.gravity = Gravity.CENTER
        btn.includeFontPadding = false
        btn.setPadding(
            (7 * d * BUTTON_SCALE).toInt(), (3 * d * BUTTON_SCALE).toInt(),
            (7 * d * BUTTON_SCALE).toInt(), (3 * d * BUTTON_SCALE).toInt(),
        )
        val bg = GradientDrawable()
        bg.setColor(btnBg())
        bg.cornerRadius = 8 * d
        btn.background = bg
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.rightMargin = (6 * d * BUTTON_SCALE).toInt()
        btn.layoutParams = lp
        btn.setOnClickListener { runCatching { onClick() } }
        root.addView(btn)
        return btn
    }

    private fun addButton(root: LinearLayout, text: String, onClick: () -> Unit): TextView {
        val ctx = root.context
        val d = ctx.resources.displayMetrics.density
        val btn = TextView(ctx)
        btn.text = text
        btn.textSize = 10.5f * BUTTON_SCALE
        btn.setTextColor(textColor())
        btn.gravity = Gravity.CENTER
        btn.includeFontPadding = false
        btn.setPadding(
            (6 * d * BUTTON_SCALE).toInt(), (4 * d * BUTTON_SCALE).toInt(),
            (6 * d * BUTTON_SCALE).toInt(), (4 * d * BUTTON_SCALE).toInt(),
        )
        val btnBg2 = GradientDrawable()
        btnBg2.setColor(btnBg())
        btnBg2.cornerRadius = 8 * d
        btn.background = btnBg2
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topMargin = (4 * d * BUTTON_SCALE).toInt()
        btn.layoutParams = lp
        btn.setOnClickListener { runCatching { onClick() } }
        root.addView(btn)
        return btn
    }

    





    private fun attachDrag(context: Context, handle: View, onTap: (() -> Unit)? = null) {
        var lastX = 0f
        var lastY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        val slop = dp(context, 8f)
        handle.setOnTouchListener { _, event ->
            val container = attached ?: return@setOnTouchListener false
            val lp = container.layoutParams as? WindowManager.LayoutParams
                ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    cancelSnapAnim()
                    lastX = event.rawX
                    lastY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()
                    if (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop) moved = true
                    lp.x = startX + dx
                    lp.y = startY + dy
                    runCatching {
                        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                            ?.updateViewLayout(container, lp)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!moved) {
                        onTap?.invoke()
                    } else if (minimized) {
                        hostActivity?.let { runCatching { snapEdge(it) } }
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun dp(context: Context, value: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics
        ).toInt()

    private fun toast(context: Context, msg: String) {
        runCatching { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    

    @Volatile
    private var enhancedPicking = false

    @Volatile
    private var pickOverlay: PickOverlay? = null

    @Volatile
    private var attrWindow: FrameLayout? = null

    @Volatile
    private var attrTarget: View? = null

    @Volatile
    private var pendingImageTarget: android.widget.ImageView? = null

    @Volatile
    private var pendingImagePersist: Boolean = false

    @Volatile
    private var flagSecureOffAll = false

    @Volatile
    private var flagSecureThread: Thread? = null

    @Volatile
    private var enhancedModule: XposedModule? = null

    private const val REQ_PICK_IMAGE = 0x5A5A
    private const val RULES_FILE = "xp_view_rules.json"

    





    class PickOverlay(ctx: android.content.Context) : View(ctx) {
        var hi: Rect? = null
        var label: String = ""
        
        var subLabel: String = ""

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            val up = ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL
            ControlPanel.onPickTouch(ev.rawX, ev.rawY, up)
            return true
        }

        override fun draw(canvas: Canvas) {
            super.draw(canvas)
            val r = hi ?: return
            val d = resources.displayMetrics.density
            val stroke = android.graphics.Paint().apply {
                color = 0xFFFF3B30.toInt()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 3 * d
            }
            val fill = android.graphics.Paint().apply {
                color = 0x33FF3B30.toInt()
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(r, fill)
            canvas.drawRect(r, stroke)
            if (label.isNotEmpty() || subLabel.isNotEmpty()) {
                val tp = android.graphics.Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = 10 * d
                    isAntiAlias = true
                }
                
                val tp2 = android.graphics.Paint().apply {
                    color = 0xFFFFF176.toInt()
                    textSize = 9.5f * d
                    isAntiAlias = true
                }
                val hasSub = subLabel.isNotEmpty()
                var y = (r.top - (if (hasSub) 6 else 6) * d)
                    .coerceAtLeast(tp.textSize + (if (hasSub) 16 else 2) * d)
                if (label.isNotEmpty()) {
                    canvas.drawText(label, r.left.toFloat(), y, tp)
                }
                if (hasSub) {
                    canvas.drawText(subLabel, r.left.toFloat(), y + 12 * d, tp2)
                }
            }
        }
    }

    
    private fun startEnhancedPick(activity: Activity) {
        enhancedPicking = true
        attrWindow?.let { runCatching { activity.windowManager.removeView(it) } }
        attrWindow = null
        val dec = runCatching { activity.window.decorView as? ViewGroup }.getOrNull() ?: return
        val ov = PickOverlay(activity)
        ov.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        runCatching { dec.addView(ov); ov.bringToFront() }
        pickOverlay = ov
        log("增强拾取：已开启（应用上的按钮已失效，滑动或点击选择组件）")
        runCatching { applyRules(activity) }
    }

    
    private fun stopEnhancedPick() {
        enhancedPicking = false
        flagSecureOffAll = false
        flagSecureThread?.interrupt()
        flagSecureThread = null
        runCatching { (pickOverlay?.parent as? ViewGroup)?.removeView(pickOverlay) }
        pickOverlay = null
        closeAttrWindow()
        log("增强拾取：已退出，恢复正常操作")
    }

    



    private fun depthOf(v: View): Int {
        var d = 0
        var cur: android.view.ViewParent? = v.parent
        while (cur is View) {
            d++
            cur = cur.parent
        }
        return (d).coerceAtLeast(0)
    }

    private fun onPickTouch(x: Float, y: Float, isUp: Boolean) {
        val act = hostActivity ?: return
        val root = runCatching { act.window.decorView as? ViewGroup }.getOrNull() ?: return
        val ov = pickOverlay ?: return
        val t = findViewAt(root, x.toInt(), y.toInt(), ov)
        mainHandler.post {
            runCatching {
                if (t == null) {
                    ov.hi = null
                    ov.label = ""
                    ov.subLabel = ""
                    ov.invalidate()
                    return@runCatching
                }
                val loc = IntArray(2)
                t.getLocationOnScreen(loc)
                ov.hi = Rect(loc[0], loc[1], loc[0] + t.width, loc[1] + t.height)
                val idn = idNameOf(t)
                ov.label = "${t.javaClass.simpleName}${if (idn.isBlank()) "" else " #$idn"}"
                ov.subLabel =
                    "(${loc[0]}, ${loc[1]}) ${t.width}x${t.height} 深度${depthOf(t)}"
                ov.invalidate()
                if (isUp) {
                    attrTarget = t
                    showAttrWindow(act, t, loc[0], loc[1] + t.height)
                }
            }
        }
    }

    

    private fun closeAttrWindow() {
        val w = attrWindow ?: return
        val act = hostActivity
        if (act != null) runCatching { act.windowManager.removeView(w) }
        attrWindow = null
    }

    
    private fun showAttrWindow(activity: Activity, v: View, left: Int, bottom: Int) {
        closeAttrWindow()
        val d = activity.resources.displayMetrics.density
        val sw = screenW(activity)
        val sh = screenH(activity)
        val w = attrWindowW(activity)
        
        val h = (minOf(sw, sh) / 3.2f).toInt().coerceAtLeast((160 * d).toInt())

        val box = FrameLayout(activity)
        val lp = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = left.coerceIn(0, (sw - w).coerceAtLeast(0))
        lp.y = (bottom + (6 * d).toInt()).coerceIn(0, (sh - h).coerceAtLeast(0))
        lp.token = activity.window.decorView.windowToken
        lp.windowAnimations = 0

        val bg = GradientDrawable()
        bg.setColor(panelBg())
        bg.cornerRadius = 12 * d
        bg.setStroke((1 * d).toInt(), strokeColor())
        box.background = bg

        val scroll = android.widget.ScrollView(activity)
        val col = LinearLayout(activity)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())

        val title = android.widget.TextView(activity)
        title.text = "${v.javaClass.simpleName} ${idNameOf(v).ifEmpty { "(无 id)" }}"
        title.setTextColor(textColor())
        title.textSize = 11f
        title.setPadding(0, 0, 0, (6 * d).toInt())
        col.addView(title)

        addTool(col, "查看详细信息") { showDetailDialog(activity, v) }
        addTool(col, "移除组件") { runCatching { (v.parent as? ViewGroup)?.removeView(v) }; log("已移除该组件") }
        addTool(col, "隐藏组件") { v.visibility = View.INVISIBLE; log("已隐藏该组件") }
        addTool(col, "持久化的移除") { addRule(activity, v, "remove"); log("已持久化：移除（下次进入生效）") }
        addTool(col, "持久化的隐藏") { addRule(activity, v, "hide"); log("已持久化：隐藏（下次进入生效）") }
        addTool(col, "替换图片") { pickAndReplaceImage(activity, v, false) }
        addTool(col, "替换图片(持久化)") { pickAndReplaceImage(activity, v, true) }
        addTool(col, "保存图片") { saveImageViewImage(activity, v) }
        addTool(col, "控件转图片") { controlToImage(activity, v) }
        addTool(col, "整页截图") { captureActivity(activity) }
        addTool(col, "更改控件颜色") { showColorDialog(activity, v, false) }
        addTool(col, "更改控件颜色(持久化)") { showColorDialog(activity, v, true) }
        addTool(col, "修改文字") { showTextDialog(activity, v) }
        addTool(col, "关闭 FLAG_SECURE") { askFlagSecure(activity, v, false) }
        addTool(col, "添加 FLAG_SECURE") { askFlagSecure(activity, v, true) }
        addTool(col, "关闭 FLAG_SECURE(持久化)") { askFlagSecurePersist(activity, v, false) }
        addTool(col, "添加 FLAG_SECURE(持久化)") { askFlagSecurePersist(activity, v, true) }
        addTool(col, "删除所有持久化配置") { confirmClearPersist(activity) }
        addTool(col, "退出拾取增强版") { stopEnhancedPick() }

        scroll.addView(col)
        box.addView(scroll)
        runCatching {
            activity.windowManager.addView(box, lp)
            attrWindow = box
        }.onFailure { log("属性窗口创建失败：${it.message}") }
    }

    

    






    private fun confirmClearPersist(context: Context) {
        val d = context.resources.displayMetrics.density
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding((12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt())

        val tip = android.widget.TextView(context)
        tip.text = "将删除：\n· 所有持久化规则（移除/隐藏/改文字/换图/FLAG_SECURE）\n· 持久化保存的图片文件\n\n此操作不可撤销。"
        tip.setTextColor(textColor())
        tip.textSize = 11f
        tip.setPadding(0, 0, 0, (8 * d).toInt())
        col.addView(tip)

        addTool(col, "确认删除") {
            closeDialogWindow()
            clearPersist(context)
        }
        addTool(col, "取消") { closeDialogWindow() }
        showDialogWindow(context, "删除所有持久化配置", col)
    }

    private fun clearPersist(context: Context) {
        val dir = runCatching { context.getExternalFilesDir(null) }.getOrNull()
        var files = 0
        var rules = 0

        
        val arr = loadRules(context)
        val names = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            rules++
            val fn = r.optString("value", "")
            if (r.optString("action") == "image" && fn.isNotEmpty()) names.add(fn)
        }

        if (dir != null) {
            
            names.forEach { n ->
                val f = java.io.File(dir, n)
                if (runCatching { f.exists() }.getOrDefault(false)) {
                    if (runCatching { f.delete() }.getOrDefault(false)) files++
                }
            }
            
            runCatching {
                dir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.startsWith("view_") &&
                        (f.name.endsWith(".png") || f.name.endsWith(".jpg") ||
                            f.name.endsWith(".webp") || f.name.endsWith(".gif"))
                    ) {
                        if (f.delete()) files++
                    }
                }
            }
        }

        
        val rf = rulesFile(context)
        val ok = rf?.let { runCatching { it.delete() }.getOrDefault(false) } ?: false

        
        stopEnhancedPick()

        toast(context, "已删除 ${rules} 条规则、${files} 个图片文件")
        log("删除所有持久化配置：规则 ${rules} 条，图片 ${files} 个，规则文件删除=${ok}")
    }

    

    





    private fun showDetailDialog(context: Context, v: View) {
        val act = context as? Activity ?: return
        val sb = StringBuilder()
        sb.appendLine("类名: ${v.javaClass.name}")
        val idn = idNameOf(v)
        sb.appendLine("id: ${if (idn.isEmpty()) "(无)" else "$idn (0x${Integer.toHexString(v.id)})"}")
        val loc = IntArray(2)
        runCatching { v.getLocationOnScreen(loc) }
        sb.appendLine("坐标: (${loc[0]}, ${loc[1]})")
        sb.appendLine("尺寸: ${v.width} x ${v.height}  (测量: ${v.measuredWidth} x ${v.measuredHeight})")
        sb.appendLine("padding: L${v.paddingLeft} T${v.paddingTop} R${v.paddingRight} B${v.paddingBottom}")
        sb.appendLine("paddingStart=${v.paddingStart} paddingEnd=${v.paddingEnd}")
        runCatching {
            val lp2 = v.layoutParams
            if (lp2 is ViewGroup.MarginLayoutParams) {
                sb.appendLine("margin: L${lp2.leftMargin} T${lp2.topMargin} R${lp2.rightMargin} B${lp2.bottomMargin}")
            }
            if (lp2 != null) sb.appendLine("layoutParams: ${lp2.javaClass.simpleName} ${lp2.width}x${lp2.height}")
        }
        sb.appendLine("text: ${textOf(v).ifEmpty { "(无)" }.take(200)}")
        sb.appendLine("contentDescription: ${v.contentDescription ?: "(无)"}")
        sb.appendLine("可点击=${v.isClickable} 可长按=${v.isLongClickable}")
        sb.appendLine("启用=${v.isEnabled} 可获得焦点=${v.isFocusable} alpha=${v.alpha}")
        sb.appendLine("可见性=${visName(v.visibility)}  已附着=${runCatching { v.isAttachedToWindow }.getOrDefault(false)}")
        sb.appendLine("tag: ${v.tag ?: "(无)"}")
        sb.appendLine("translation: x=${v.translationX} y=${v.translationY} z=${v.translationZ}")
        sb.appendLine("scale: x=${v.scaleX} y=${v.scaleY}  rotation=${v.rotation}")
        if (v is android.widget.TextView) {
            sb.appendLine("文字颜色=0x${Integer.toHexString(v.currentTextColor)}  字号=${v.textSize}")
        }
        if (v is android.widget.ImageView) {
            sb.appendLine("scaleType=${v.scaleType}  adjustViewBounds=${v.adjustViewBounds}")
        }
        sb.appendLine("--- 所属结构 ---")
        sb.appendLine("父容器: ${v.parent?.javaClass?.name ?: "(无)"}")
        runCatching {
            val p = v.parent as? ViewGroup
            if (p != null) sb.appendLine("在父容器中的序号: ${p.indexOfChild(v)} / 共 ${p.childCount}")
        }
        sb.appendLine("当前活动: ${act.javaClass.name}")
        sb.appendLine("包名: ${act.packageName}")
        sb.appendLine("--- 窗口 ---")
        sb.appendLine("Window Flags: 0x${Integer.toHexString(act.window.attributes.flags)}")
        sb.appendLine("是否有 FLAG_SECURE: ${hasWindowFlagSecure(act.window)}")
        sb.appendLine("是否有 FLAG_NOT_FOCUSABLE: ${(act.window.attributes.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0}")
        sb.appendLine("softInputMode: ${act.window.attributes.softInputMode}")
        sb.appendLine("--- 调用堆栈（前 12 层）---")
        Thread.currentThread().stackTrace.take(12).forEach { sb.appendLine("  $it") }

        val d = act.resources.displayMetrics.density
        val wrap = LinearLayout(act)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())

        val scroll = android.widget.ScrollView(act)
        scroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (act.resources.displayMetrics.heightPixels * 0.45f).toInt(),
        )
        val tv = android.widget.TextView(act)
        tv.text = sb.toString()
        tv.setTextColor(textColor())
        tv.textSize = 10.5f
        tv.typeface = android.graphics.Typeface.MONOSPACE
        
        tv.setTextIsSelectable(true)
        tv.setPadding((6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
        scroll.addView(tv)
        wrap.addView(scroll)

        val row = LinearLayout(act)
        row.orientation = LinearLayout.HORIZONTAL
        addTool(row, "复制到剪贴板") {
            val ok = runCatching {
                val cm = act.getSystemService(Context.CLIPBOARD_SERVICE)
                        as? android.content.ClipboardManager
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("view_info", sb.toString()))
                true
            }.getOrDefault(false)
            toast(act, if (ok) "已复制详细信息" else "复制失败")
        }
        wrap.addView(row)

        showDialogWindow(act, "组件详细信息", wrap)
    }

    private fun visName(v: Int): String = when (v) {
        View.VISIBLE -> "VISIBLE"
        View.INVISIBLE -> "INVISIBLE"
        else -> "GONE"
    }

    private fun hasWindowFlagSecure(w: android.view.Window): Boolean =
        (w.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0

    

    private fun rulesFile(context: Context): java.io.File? =
        runCatching { java.io.File(context.getExternalFilesDir(null), RULES_FILE) }.getOrNull()

    private fun loadRules(context: Context): org.json.JSONArray {
        val f = rulesFile(context) ?: return org.json.JSONArray()
        if (runCatching { f.exists() }.getOrDefault(false).not()) return org.json.JSONArray()
        return runCatching { org.json.JSONArray(f.readText()) }.getOrElse { org.json.JSONArray() }
    }

    private fun saveRules(context: Context, arr: org.json.JSONArray) {
        val f = rulesFile(context) ?: return
        runCatching { f.parentFile?.mkdirs(); f.writeText(arr.toString(2)) }
    }

    




    private fun addRule(context: Context, v: View, action: String, extra: org.json.JSONObject? = null) {
        val arr = loadRules(context)
        val o = org.json.JSONObject()
        o.put("action", action)
        val idn = idNameOf(v)
        if (idn.isNotEmpty()) o.put("idName", idn)
        o.put("cls", v.javaClass.name)
        val t = textOf(v)
        if (t.isNotEmpty()) o.put("text", t)
        if (extra != null) {
            val it = extra.keys()
            while (it.hasNext()) {
                val k = it.next()
                o.put(k, extra.get(k))
            }
        }
        arr.put(o)
        saveRules(context, arr)
    }

    private fun matchesRule(v: View, r: org.json.JSONObject): Boolean {
        val idn = r.optString("idName", "")
        if (idn.isNotEmpty()) {
            if (idNameOf(v) == idn) return true
            
        }
        if (r.optString("cls", "") != v.javaClass.name) return false
        val t = r.optString("text", "")
        if (t.isNotEmpty() && textOf(v) != t) return false
        return true
    }

    
    private fun applyRules(context: Context) {
        val act = context as? Activity ?: return
        val arr = loadRules(context)
        if (arr.length() == 0) return
        val root = runCatching { act.window.decorView as? ViewGroup }.getOrNull() ?: return
        var hit = 0

        
        
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            if (r.optString("action") != "flag_secure") continue
            if (r.optString("scope", "window") != "all") continue
            val en = r.optBoolean("enable", false)
            setFlagSecureAll(context, en)
            if (!en) startFlagSecureWatch(context)
            hit++
        }

        walk(root) { v ->
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                if (!matchesRule(v, r)) continue
                when (r.optString("action")) {
                    "remove" -> runCatching { (v.parent as? ViewGroup)?.removeView(v) }
                    "hide" -> v.visibility = View.INVISIBLE
                    "text" -> {
                        val s = r.optString("value", "")
                        runCatching { (v as? android.widget.TextView)?.text = s }
                    }
                    "color" -> applyColorRule(v, r)
                    "flag_secure" -> {
                        val en = r.optBoolean("enable", false)
                        if (r.optString("scope", "window") == "all") {
                            setFlagSecureAll(context, en)
                            
                            if (!en) startFlagSecureWatch(context)
                        } else {
                            setFlagSecureForView(context, v, en)
                        }
                    }
                    "image" -> {
                        val fn = r.optString("value", "")
                        if (fn.isNotEmpty()) {
                            val f = java.io.File(context.getExternalFilesDir(null), fn)
                            if (runCatching { f.exists() }.getOrDefault(false)) {
                                val bmp = runCatching {
                                    android.graphics.BitmapFactory.decodeFile(f.absolutePath)
                                }.getOrNull()
                                if (bmp != null) runCatching {
                                    val iv = v as? android.widget.ImageView ?: return@runCatching
                                    iv.setImageBitmap(fitForView(iv, bmp))
                                }
                            }
                        }
                    }
                }
                hit++
                break
            }
        }
        if (hit > 0) log("持久化组件规则：命中 $hit 个")
    }

    private fun walk(root: View, body: (View) -> Unit) {
        body(root)
        val g = root as? ViewGroup ?: return
        for (i in 0 until g.childCount) g.getChildAt(i)?.let { walk(it, body) }
    }

    private fun idNameOf(v: View): String = runCatching {
        if (v.id > 0) v.resources.getResourceEntryName(v.id) else ""
    }.getOrDefault("")

    private fun textOf(v: View): String =
        runCatching { (v as? android.widget.TextView)?.text?.toString() }.getOrNull().orEmpty()

    

    private fun pickAndReplaceImage(context: Context, v: View, persist: Boolean) {
        if (v !is android.widget.ImageView) {
            toast(context, "该组件不是 ImageView，不支持替换图片")
            return
        }
        val act = context as? Activity
        if (act == null) {
            toast(context, "拿不到当前界面")
            return
        }
        pendingImageTarget = v
        pendingImagePersist = persist
        val it = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
        it.addCategory(android.content.Intent.CATEGORY_OPENABLE)
        it.type = "image/*"
        runCatching {
            act.startActivityForResult(it, REQ_PICK_IMAGE)
        }.onFailure {
            pendingImageTarget = null
            toast(context, "打不开系统文件选择器")
        }
    }

    
    private fun handlePickResult(context: Context, requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        if (requestCode != REQ_PICK_IMAGE) return
        val v = pendingImageTarget ?: return
        pendingImageTarget = null
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            toast(context, "未选择图片")
            return
        }
        val uri = data.data ?: return
        val act = context
        val bytes = runCatching {
            act.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            toast(context, "读取图片失败")
            return
        }
        val ext = guessExt(act, uri)
        val idn = idNameOf(v).ifEmpty { "view_${v.javaClass.simpleName}_${System.currentTimeMillis()}" }
        val fileName = "$idn.$ext"
        if (pendingImagePersist) {
            val f = runCatching { java.io.File(act.getExternalFilesDir(null), fileName) }.getOrNull()
            if (f == null) {
                toast(context, "拿不到私有目录")
                return
            }
            runCatching {
                f.parentFile?.mkdirs()
                f.writeBytes(bytes)
            }.onFailure {
                toast(context, "保存图片失败")
                return
            }
            val extra = org.json.JSONObject()
            extra.put("value", fileName)
            addRule(act, v, "image", extra)
            toast(context, "已持久化替换图片：$fileName")
        } else {
            toast(context, "已替换图片（本次生效）")
        }
        val bmp = runCatching {
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
        if (bmp != null && v is android.widget.ImageView) {
            runCatching { v.setImageBitmap(fitForView(v, bmp)) }
            log("替换图片 → ${if (pendingImagePersist) "持久化 $fileName" else "仅本次"}")
        }
    }

    

    
    private fun privateDir(context: Context): java.io.File? =
        runCatching { context.getExternalFilesDir(null) }.getOrNull()

    





    private fun saveImageViewImage(context: Context, v: View) {
        if (v !is android.widget.ImageView) {
            toast(context, "该组件不是 ImageView，没有图片可保存")
            return
        }
        val dr = v.drawable
        if (dr == null) {
            toast(context, "这个 ImageView 当前没有内容")
            return
        }
        val bmp = drawableToBitmap(dr, dr.intrinsicWidth, dr.intrinsicHeight)
        if (bmp == null) {
            toast(context, "取不到图片内容")
            return
        }
        val dir = privateDir(context)
        if (dir == null) {
            toast(context, "拿不到私有目录")
            return
        }
        val name = "${idNameOf(v).ifEmpty { "view_${v.javaClass.simpleName}_${System.currentTimeMillis()}" }}.png"
        val f = java.io.File(dir, name)
        val ok = runCatching {
            dir.mkdirs()
            java.io.FileOutputStream(f).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            true
        }.getOrDefault(false)
        log("保存图片 → ${if (ok) f.absolutePath else "失败"}")
        toast(context, if (ok) "已保存：$name" else "保存失败")
    }

    
    private fun drawableToBitmap(dr: android.graphics.drawable.Drawable, w: Int, h: Int): android.graphics.Bitmap? {
        if (dr is android.graphics.drawable.BitmapDrawable) {
            val b = runCatching { dr.bitmap }.getOrNull()
            if (b != null && !b.isRecycled) return b
        }
        val ww = if (w > 0) w else 256
        val hh = if (h > 0) h else 256
        return runCatching {
            val bmp = android.graphics.Bitmap.createBitmap(ww, hh, android.graphics.Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            dr.setBounds(0, 0, ww, hh)
            dr.draw(c)
            bmp
        }.getOrNull()
    }

    





    private fun controlToImage(context: Context, v: View) {
        val dir = privateDir(context)
        if (dir == null) {
            toast(context, "拿不到私有目录")
            return
        }
        val w = v.width.takeIf { it > 0 } ?: 1
        val h = v.height.takeIf { it > 0 } ?: 1
        val bmp = runCatching {
            val b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            
            c.drawColor(0xFFFFFFFF.toInt())
            v.draw(c)
            b
        }.getOrNull()
        if (bmp == null) {
            toast(context, "转图片失败")
            return
        }
        val name = "control_${idNameOf(v).ifEmpty { v.javaClass.simpleName }}_${System.currentTimeMillis()}.png"
        val f = java.io.File(dir, name)
        val ok = runCatching {
            dir.mkdirs()
            java.io.FileOutputStream(f).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            true
        }.getOrDefault(false)
        log("控件转图片 → ${if (ok) f.absolutePath else "失败"}")
        toast(context, if (ok) "已保存：$name" else "保存失败")
    }

    
    private fun captureActivity(context: Context) {
        val act = context as? Activity
        val dir = privateDir(context)
        if (act == null || dir == null) {
            toast(context, "拿不到当前界面或私有目录")
            return
        }
        val root = runCatching { act.window.decorView }.getOrNull()
        if (root == null) {
            toast(context, "拿不到当前界面")
            return
        }
        val w = root.width.takeIf { it > 0 } ?: screenW(act)
        val h = root.height.takeIf { it > 0 } ?: screenH(act)
        val bmp = runCatching {
            val b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            c.drawColor(0xFFFFFFFF.toInt())
            root.draw(c)
            b
        }.getOrNull()
        if (bmp == null) {
            toast(context, "截图失败")
            return
        }
        val name = "screen_${System.currentTimeMillis()}.png"
        val f = java.io.File(dir, name)
        val ok = runCatching {
            dir.mkdirs()
            java.io.FileOutputStream(f).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            true
        }.getOrDefault(false)
        log("整页截图 → ${if (ok) f.absolutePath else "失败"}")
        toast(context, if (ok) "已保存：$name" else "保存失败")
    }

    

    private val PRESET_COLORS = listOf(
        "透明" to 0x00000000,
        "黑" to 0xFF000000.toInt(),
        "白" to 0xFFFFFFFF.toInt(),
        "红" to 0xFFFF3B30.toInt(),
        "橙" to 0xFFFF9500.toInt(),
        "黄" to 0xFFFFCC00.toInt(),
        "绿" to 0xFF34C759.toInt(),
        "青" to 0xFF00C7BE.toInt(),
        "蓝" to 0xFF007AFF.toInt(),
        "紫" to 0xFFAF52DE.toInt(),
        "灰" to 0xFF8E8E93.toInt(),
    )

    





    private fun showColorDialog(context: Context, v: View, persist: Boolean) {
        val d = context.resources.displayMetrics.density
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())

        val isText = v is android.widget.TextView
        var target: String = if (isText) "text" else "bg"

        fun apply(hex: String) {
            val color = runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()
            if (color == null) {
                toast(context, "颜色格式不对，应形如 #FF0000")
                return
            }
            when (target) {
                "text" -> if (v is android.widget.TextView) v.setTextColor(color)
                else -> runCatching { v.setBackgroundColor(color) }
            }
            runCatching { v.invalidate(); (v.parent as? View)?.invalidate() }
            if (persist) {
                val extra = org.json.JSONObject()
                extra.put("value", hex)
                extra.put("target", target)
                addRule(context, v, "color", extra)
            }
            log("改颜色 → $target $hex${if (persist) "（持久化）" else ""}")
            toast(context, "已设置 ${if (target == "text") "文字色" else "背景色"}$hex")
        }

        
        if (isText) {
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            val bText = addTool(row, "文字色") { target = "text"; toast(context, "已选：文字色") }
            val bBg = addTool(row, "背景色") { target = "bg"; toast(context, "已选：背景色") }
            bText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            bBg.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            col.addView(row)
            toast(context, "默认改文字色，点「背景色」可切换")
        }

        
        var rowN = 0
        PRESET_COLORS.chunked(4).forEach { group ->
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            group.forEach { (name, color) ->
                val b = addTool(row, name) { apply(String.format("#%08X", color)) }
                b.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(row)
            rowN++
        }

        
        val input = android.widget.EditText(context)
        input.hint = "#RRGGBB 或 #AARRGGBB"
        input.setText(if (target == "text") "#FFFFFFFF" else "#FF000000")
        input.setTextColor(textColor())
        input.setHintTextColor(0xFF888888.toInt())
        input.textSize = 12f
        input.setSingleLine(true)
        col.addView(input)

        addTool(col, "应用自定义颜色") {
            val hex = input.text?.toString()?.trim().orEmpty()
            if (!hex.startsWith("#")) {
                toast(context, "要以 # 开头")
            } else {
                apply(hex)
            }
        }
        showDialogWindow(context, if (persist) "更改颜色（持久化）" else "更改颜色", col)
        
        input.post {
            runCatching {
                input.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                        as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun applyColorRule(v: View, r: org.json.JSONObject) {
        val hex = r.optString("value", "")
        if (hex.isBlank()) return
        val color = runCatching { android.graphics.Color.parseColor(hex) }.getOrNull() ?: return
        val target = r.optString("target", "bg")
        runCatching {
            if (target == "text") {
                if (v is android.widget.TextView) v.setTextColor(color)
            } else {
                v.setBackgroundColor(color)
            }
        }
    }

    








    private fun fitForView(v: android.widget.ImageView, src: android.graphics.Bitmap): android.graphics.Bitmap {
        runCatching {
            val w = v.width.takeIf { it > 0 } ?: src.width
            val h = v.height.takeIf { it > 0 } ?: src.height
            val scale = minOf(w.toFloat() / src.width, h.toFloat() / src.height, 1f)
            if (scale in 0f..1f && scale < 1f) {
                val nw = (src.width * scale).toInt().coerceAtLeast(1)
                val nh = (src.height * scale).toInt().coerceAtLeast(1)
                val scaled = android.graphics.Bitmap.createScaledBitmap(src, nw, nh, true)
                
                if (v.scaleType == android.widget.ImageView.ScaleType.FIT_XY) {
                    v.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
                v.adjustViewBounds = true
                return scaled
            }
            if (v.scaleType == android.widget.ImageView.ScaleType.FIT_XY) {
                v.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
            v.adjustViewBounds = true
        }
        return src
    }

    private fun guessExt(context: Context, uri: android.net.Uri): String {
        val t = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        return when {
            t.contains("png") -> "png"
            t.contains("webp") -> "webp"
            t.contains("gif") -> "gif"
            t.contains("jpeg") || t.contains("jpg") -> "jpg"
            else -> "png"
        }
    }

    

    






    private fun applyText(tv: android.widget.TextView, s: String): Boolean {
        val run = {
            runCatching {
                tv.text = s
                tv.invalidate()
                tv.requestLayout()
                (tv.parent as? View)?.let { p ->
                    p.invalidate()
                    p.requestLayout()
                }
                tv.rootView?.invalidate()
                true
            }.getOrDefault(false)
        }
        
        return if (mainHandler.looper.thread === Thread.currentThread()) {
            run()
        } else {
            var ok = false
            mainHandler.post { ok = run() }
            ok
        }
    }

    private fun showTextDialog(context: Context, v: View) {
        val tv = v as? android.widget.TextView
        if (tv == null) {
            toast(context, "该组件没有文字")
            return
        }
        val input = android.widget.EditText(context)
        input.setText(textOf(v))
        input.setSingleLine(false)
        input.setTextColor(textColor())
        input.setHintTextColor(if (darkTheme) 0xFF888888.toInt() else 0xFF999999.toInt())
        input.isFocusable = true
        input.isFocusableInTouchMode = true
        input.isClickable = true
        input.setSelectAllOnFocus(false)
        input.setSelection(input.text?.length ?: 0)
        
        
        
        
        input.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.post {
                    runCatching {
                        v.requestFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                                as? android.view.inputmethod.InputMethodManager
                        imm?.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
        val d = context.resources.displayMetrics.density
        input.setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())
        val wrap = LinearLayout(context)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding((12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt())
        wrap.addView(input)
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        addTool(row, "修改") {
            val s = input.text.toString()
            val ok = applyText(tv, s)
            closeDialogWindow()
            toast(context, if (ok) "已改为：$s" else "修改失败：$s")
            log("修改文字 → $s ok=$ok")
        }
        addTool(row, "持久化修改") {
            val s = input.text.toString()
            val ok = applyText(tv, s)
            val extra = org.json.JSONObject()
            extra.put("value", s)
            addRule(context, v, "text", extra)
            closeDialogWindow()
            toast(context, if (ok) "已持久化修改文字：$s" else "已保存规则，但本次写入失败")
            log("修改文字（持久化）→ $s ok=$ok")
        }
        wrap.addView(row)
        showDialogWindow(context, "修改文字", wrap)
    }

    

    @Volatile
    private var dialogWindow: FrameLayout? = null

    






    private fun showDialogWindow(context: Context, title: String, content: View) {
        closeDialogWindow()
        val act = context as? Activity ?: return
        val d = act.resources.displayMetrics.density
        val w = dialogWindowW(act)
        val box = FrameLayout(act)
        val lp = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.CENTER
        lp.token = act.window.decorView.windowToken
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        
        box.isFocusableInTouchMode = true
        val bg = GradientDrawable()
        bg.setColor(panelBg())
        bg.cornerRadius = 12 * d
        bg.setStroke((1 * d).toInt(), strokeColor())
        val outer = LinearLayout(act)
        outer.orientation = LinearLayout.VERTICAL
        outer.background = bg
        val t = android.widget.TextView(act)
        t.text = title
        t.setTextColor(textColor())
        t.textSize = 12f
        t.setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (4 * d).toInt())
        outer.addView(t)
        outer.addView(content)
        val close = android.widget.TextView(act)
        close.text = "关闭"
        close.setTextColor(textColor())
        close.gravity = Gravity.CENTER
        close.setPadding(0, (4 * d).toInt(), 0, (10 * d).toInt())
        close.setOnClickListener { closeDialogWindow() }
        outer.addView(close)
        box.addView(outer)
        runCatching {
            act.windowManager.addView(box, lp)
            dialogWindow = box
        }
    }

    private fun closeDialogWindow() {
        val w = dialogWindow ?: return
        val act = hostActivity
        if (act != null) runCatching { act.windowManager.removeView(w) }
        dialogWindow = null
    }

    

    private fun askFlagSecure(context: Context, v: View, enable: Boolean) {
        val d = context.resources.displayMetrics.density
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding((12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt())
        val verb = if (enable) "添加" else "关闭"
        addTool(col, "${verb}所有窗口的 FLAG_SECURE") {
            closeDialogWindow()
            if (enable) {
                setFlagSecureAll(context, true)
                toast(context, "已为所有窗口添加 FLAG_SECURE")
            } else {
                startFlagSecureWatch(context)
                toast(context, "已开始持续关闭所有窗口的 FLAG_SECURE")
            }
        }
        addTool(col, "${verb}这个窗口的 FLAG_SECURE") {
            closeDialogWindow()
            val ok = setFlagSecureForView(context, v, enable)
            toast(
                context,
                if (ok) "已${verb}该窗口的 FLAG_SECURE"
                else if (enable) "已添加"
                else "此窗口没有添加安全标志"
            )
        }
        showDialogWindow(context, "${verb} FLAG_SECURE", col)
    }

    




    private fun askFlagSecurePersist(context: Context, v: View, enable: Boolean) {
        val d = context.resources.displayMetrics.density
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding((12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt(), (12 * d).toInt())
        val verb = if (enable) "添加" else "关闭"

        addTool(col, "${verb}所有窗口(持久化)") {
            closeDialogWindow()
            val extra = org.json.JSONObject()
            extra.put("scope", "all")
            extra.put("enable", enable)
            addRule(context, v, "flag_secure", extra)
            if (!enable) startFlagSecureWatch(context)
            toast(context, "已持久化：${verb}所有窗口的 FLAG_SECURE")
            log("FLAG_SECURE 持久化 → ${verb}所有窗口")
        }

        addTool(col, "${verb}这个窗口(持久化)") {
            closeDialogWindow()
            val ok = setFlagSecureForView(context, v, enable)
            val extra = org.json.JSONObject()
            extra.put("scope", "window")
            extra.put("enable", enable)
            addRule(context, v, "flag_secure", extra)
            toast(
                context,
                if (ok) "已持久化：${verb}该窗口的 FLAG_SECURE"
                else if (enable) "已持久化（该窗口原本没有，下次进入会添加）"
                else "此窗口没有添加安全标志"
            )
            log("FLAG_SECURE 持久化 → ${verb}这个窗口 ok=$ok")
        }

        showDialogWindow(context, "${verb} FLAG_SECURE（持久化）", col)
    }

    
    private fun startFlagSecureWatch(context: Context) {
        flagSecureOffAll = true
        setFlagSecureAll(context, false)
        flagSecureThread?.interrupt()
        val t = Thread {
            while (flagSecureOffAll) {
                runCatching { setFlagSecureAll(context, false) }
                runCatching { Thread.sleep(200) }
            }
        }
        t.isDaemon = true
        flagSecureThread = t
        t.start()
        log("FLAG_SECURE：已开启持续关闭（200ms 扫描 + addView 拦截）")
    }

    private fun setFlagSecureAll(context: Context, enable: Boolean) {
        val wm = runCatching {
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        }.getOrNull() ?: return
        forEachWindow { view, params ->
            val has = (params.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            if (enable && !has) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_SECURE
                runCatching { wm.updateViewLayout(view, params) }
            } else if (!enable && has) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                runCatching { wm.updateViewLayout(view, params) }
            }
        }
    }

    
    private fun setFlagSecureForView(context: Context, target: View, enable: Boolean): Boolean {
        val wm = runCatching {
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        }.getOrNull() ?: return false
        var done = false
        forEachWindow { view, params ->
            if (done) return@forEachWindow
            if (!containsView(view, target)) return@forEachWindow
            val has = (params.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            if (enable && !has) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_SECURE
                runCatching { wm.updateViewLayout(view, params) }
                done = true
            } else if (!enable && has) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                runCatching { wm.updateViewLayout(view, params) }
                done = true
            }
        }
        return done
    }

    
    private fun forEachWindow(body: (View, WindowManager.LayoutParams) -> Unit) {
        runCatching {
            val g = Class.forName("android.view.WindowManagerGlobal")
            val inst = g.getDeclaredMethod("getInstance").apply { isAccessible = true }.invoke(null)
                ?: return
            val rawV = g.getDeclaredField("mViews").apply { isAccessible = true }.get(inst)
            val rawP = g.getDeclaredField("mParams").apply { isAccessible = true }.get(inst)
            val views = toViewList(rawV) ?: return
            val params = toParamList(rawP) ?: return
            val n = minOf(views.size, params.size)
            for (i in 0 until n) runCatching { body(views[i], params[i]) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toViewList(raw: Any?): List<View>? = when (raw) {
        is ArrayList<*> -> raw as? List<View>
        is Array<*> -> raw.filterIsInstance<View>()
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun toParamList(raw: Any?): List<WindowManager.LayoutParams>? = when (raw) {
        is ArrayList<*> -> raw as? List<WindowManager.LayoutParams>
        is Array<*> -> raw.filterIsInstance<WindowManager.LayoutParams>()
        else -> null
    }

    private fun containsView(root: View, target: View): Boolean {
        if (root === target) return true
        val g = root as? ViewGroup ?: return false
        for (i in 0 until g.childCount) {
            if (containsView(g.getChildAt(i) ?: continue, target)) return true
        }
        return false
    }

    

    
    private fun installEnhancedHooks(module: XposedModule) {
        enhancedModule = module

        
        runCatching {
            val m = Activity::class.java.getDeclaredMethod(
                "onActivityResult", Int::class.java, Int::class.java,
                android.content.Intent::class.java,
            )
            module.hook(m)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val res = chain.proceed()
                    runCatching {
                        val act = chain.getThisObject() as? Activity ?: return@runCatching
                        val rc = chain.getArg(0) as? Int ?: return@runCatching
                        val result = chain.getArg(1) as? Int ?: return@runCatching
                        val data = chain.getArg(2) as? android.content.Intent
                        handlePickResult(act, rc, result, data)
                    }
                    res
                }
        }

        
        runCatching {
            val wmi = Class.forName("android.view.WindowManagerImpl")
            wmi.declaredMethods.filter {
                it.name == "addView" || it.name == "updateViewLayout"
            }.forEach { m ->
                module.hook(m)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (flagSecureOffAll) {
                            runCatching {
                                for (a in chain.args) {
                                    if (a is WindowManager.LayoutParams) {
                                        a.flags = a.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                    }
                                }
                            }
                        }
                        chain.proceed()
                    }
            }
        }

        
        
        
        runCatching {
            val w = Class.forName("android.view.Window")
            w.declaredMethods.filter {
                it.name == "setFlags" || it.name == "addFlags" || it.name == "clearFlags"
            }.forEach { m ->
                module.hook(m)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (flagSecureOffAll) {
                            val a0 = chain.args.getOrNull(0)
                            if (a0 is Int) {
                                
                                
                                val newArgs = chain.args.toTypedArray()
                                newArgs[0] = a0 and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                return@intercept chain.proceed(newArgs)
                            }
                        }
                        chain.proceed()
                    }
            }
        }

        
        runCatching {
            val w = Class.forName("android.view.Window")
            w.declaredMethods.filter { it.name == "setAttributes" }.forEach { m ->
                module.hook(m)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (flagSecureOffAll) {
                            runCatching {
                                val a0 = chain.args.getOrNull(0)
                                if (a0 is WindowManager.LayoutParams) {
                                    a0.flags = a0.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                }
                            }
                        }
                        chain.proceed()
                    }
            }
        }

        
        
        runCatching {
            val sv = Class.forName("android.view.SurfaceView")
            sv.declaredMethods.filter { it.name == "setSecure" }.forEach { m ->
                module.hook(m)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (flagSecureOffAll && chain.args.getOrNull(0) == true) {
                            val newArgs = chain.args.toTypedArray()
                            newArgs[0] = java.lang.Boolean.FALSE
                            return@intercept chain.proceed(newArgs)
                        }
                        chain.proceed()
                    }
            }
        }

        
        
        listOf(
            "com.android.internal.policy.PhoneWindow",
            "com.android.internal.policy.DecorView",
            "android.view.WindowManagerGlobal",
        ).forEach { name ->
            runCatching {
                val c = Class.forName(name)
                c.declaredMethods.filter {
                    it.name == "setFlags" || it.name == "addFlags" || it.name == "clearFlags"
                }.forEach { m ->
                    module.hook(m)
                        .setPriority(XposedInterface.PRIORITY_DEFAULT)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept { chain ->
                            if (flagSecureOffAll) {
                                val a0 = chain.args.getOrNull(0)
                                if (a0 is Int) {
                                    val newArgs = chain.args.toTypedArray()
                                    newArgs[0] = a0 and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                    return@intercept chain.proceed(newArgs)
                                }
                            }
                            chain.proceed()
                        }
                }
            }
        }

        
        
        
        
        runCatching {
            val g = Class.forName("android.view.WindowManagerGlobal")
            g.declaredMethods.filter {
                it.name == "addView" || it.name == "updateViewLayout" || it.name == "addViewWithInsets"
            }.forEach { m ->
                module.hook(m)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (flagSecureOffAll) {
                            runCatching {
                                for (a in chain.args) {
                                    if (a is WindowManager.LayoutParams) {
                                        a.flags = a.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                    }
                                }
                            }
                        }
                        chain.proceed()
                    }
            }
            
            
            
            log("已预置 FLAG_SECURE 拦截点（仅装好待命，不会自动生效，需点下方按钮）")
        }

        
        
        listOf(
            "android.view.WindowManagerImpl",
            "android.view.Window",
        ).forEach { name ->
            runCatching {
                val c = Class.forName(name)
                c.declaredMethods.filter { it.name == "addView" }.forEach { m ->
                    module.hook(m)
                        .setPriority(XposedInterface.PRIORITY_DEFAULT)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept { chain ->
                            if (flagSecureOffAll) {
                                runCatching {
                                    for (a in chain.args) {
                                        if (a is WindowManager.LayoutParams) {
                                            a.flags = a.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                        }
                                    }
                                }
                            }
                            chain.proceed()
                        }
                }
            }
        }

        
        
        runCatching {
            val m = Activity::class.java.getDeclaredMethod("onResume")
            module.hook(m)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val r = chain.proceed()
                    if (flagSecureOffAll) {
                        runCatching {
                            val act = chain.getThisObject() as? Activity
                            act?.let { setFlagSecureAll(it, false) }
                        }
                    }
                    r
                }
        }
    }

}
