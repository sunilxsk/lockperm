package io.github.sunilxsk.lockperm

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean











internal class CrashCatcher(
    private val module: XposedModule,
    private val prefs: SharedPreferences,
    private val classLoader: ClassLoader,
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var forceIntercept: Boolean = false

    fun install(forceIntercept: Boolean = false) {
        if (forceIntercept) this.forceIntercept = true
        if (!Holder.installed.compareAndSet(false, true)) {
            if (forceIntercept) startLooperGuard()
            return
        }

        hookDefaultHandlerSetter()

        mainHandler.post {
            runCatching {
                val current = Thread.getDefaultUncaughtExceptionHandler()
                val wrapped = WrappedHandler(
                    original = current,
                    onCrash = { t, e -> handleCrash(t, e) },
                    shouldIntercept = { shouldIntercept() },
                )
                Holder.instance = wrapped
                Holder.selfInstalling = true
                try {
                    Thread.setDefaultUncaughtExceptionHandler(wrapped)
                } finally {
                    Holder.selfInstalling = false
                }
            }
        }

        startLooperGuard()
    }

    

    private fun hookDefaultHandlerSetter() {
        runCatching {
            val threadClass = Class.forName("java.lang.Thread", false, classLoader)
            val handlerInterface =
                Class.forName("java.lang.Thread\$UncaughtExceptionHandler", false, classLoader)
            val setter = threadClass.getDeclaredMethod(
                "setDefaultUncaughtExceptionHandler", handlerInterface
            )
            module.hook(setter)
                .setPriority(XposedInterface.PRIORITY_HIGHEST)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (Holder.selfInstalling) {
                        chain.proceed()
                    } else {
                        val original = chain.getArg(0) as? Thread.UncaughtExceptionHandler
                        val existing = Holder.instance
                        if (existing != null) {
                            
                            (existing as? WrappedHandler)?.updateOriginal(original)
                            chain.proceed(arrayOf<Any?>(existing))
                        } else {
                            val wrapped = WrappedHandler(
                                original = original,
                                onCrash = { t, e -> handleCrash(t, e) },
                                shouldIntercept = { shouldIntercept() },
                            )
                            Holder.instance = wrapped
                            chain.proceed(arrayOf<Any?>(wrapped))
                        }
                    }
                }
        }
    }

    

    private fun startLooperGuard() {
        if (!shouldIntercept()) return
        if (!Holder.guardStarted.compareAndSet(false, true)) return
        mainHandler.post {
            while (true) {
                try {
                    Looper.loop()
                    break
                } catch (t: Throwable) {
                    handleCrash(Thread.currentThread(), t)
                    
                }
            }
        }
    }

    

    private fun handleCrash(thread: Thread?, throwable: Throwable) {
        val cfg = XpState.refresh(prefs)
        val report = formatCrash(thread, throwable)
        
        if (XpState.Flags.logEnabled) {
            runCatching { module.log(Log.ERROR, TAG, "[CrashCatcher] $report") }
        }
        if (cfg.crashCopyClipboard) copyToClipboard(report)
        if (cfg.crashWriteFile) writeToErrorFile(report)
    }

    private fun formatCrash(thread: Thread?, throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return buildString {
            appendLine("=== LockPerm Crash Report ===")
            appendLine("Time    : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
            appendLine("Thread  : ${thread?.name ?: "unknown"}")
            appendLine("Package : ${XpState.packageName.ifEmpty { "unknown" }}")
            appendLine("Type    : ${throwable.javaClass.name}")
            appendLine("Message : ${throwable.message}")
            appendLine()
            append(sw.toString())
            appendLine()
            appendLine("===========================")
        }
    }

    private fun copyToClipboard(text: String) {
        mainHandler.post {
            runCatching {
                val app = currentApplication() ?: return@post
                val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: return@post
                cm.setPrimaryClip(ClipData.newPlainText("LockPerm-Crash", text))
            }
        }
    }

    





    private fun writeToErrorFile(text: String) {
        Thread {
            runCatching {
                val app = currentApplication() ?: return@runCatching
                val external = app.getExternalFilesDir(null)
                val dir = external ?: File(app.filesDir, "xp_crash")
                if (!dir.exists()) dir.mkdirs()
                File(dir, XpConfig.CRASH_LOG_NAME).appendText(text + "\n\n")
            }
        }.start()
    }

    private fun currentApplication(): Application? = runCatching {
        val clazz = Class.forName("android.app.ActivityThread", false, classLoader)
        val m = clazz.getDeclaredMethod("currentApplication")
        m.isAccessible = true
        m.invoke(null) as? Application
    }.getOrNull()

    private fun shouldIntercept(): Boolean = forceIntercept || XpState.refresh(prefs).crashIntercept

    
    private object Holder {
        val installed = AtomicBoolean(false)
        val guardStarted = AtomicBoolean(false)

        @Volatile
        var instance: Thread.UncaughtExceptionHandler? = null

        @Volatile
        var selfInstalling: Boolean = false
    }

    companion object {
        private const val TAG = "LockPerm"
    }
}


internal class WrappedHandler(
    private var original: Thread.UncaughtExceptionHandler?,
    private val onCrash: (Thread, Throwable) -> Unit,
    private val shouldIntercept: () -> Boolean,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        runCatching { onCrash(t, e) }
        if (runCatching { shouldIntercept() }.getOrDefault(false)) return
        runCatching { original?.uncaughtException(t, e) }
    }

    @Synchronized
    fun updateOriginal(handler: Thread.UncaughtExceptionHandler?) {
        original = handler
    }
}
