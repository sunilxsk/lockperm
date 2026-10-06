package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import android.media.AudioManager
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method














internal class VolumeDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val VOLUME_METHODS = setOf(
        "setStreamVolume",
        "adjustStreamVolume",
        "adjustVolume",
        "adjustSuggestedStreamVolume",
        "adjustMasterVolume",
        "setVolumeGroupVolume",
        "adjustVolumeGroupVolume",
        "setStreamSolo",
    )

    
    private val MUTE_METHODS = setOf("setStreamMute", "setMasterMute", "setMicrophoneMute")

    fun install() {
        val am = cls("android.media.AudioManager") ?: run {
            logWarn("AudioManager not found")
            return
        }

        am.declaredMethods.forEach { m ->
            when (m.name) {
                in VOLUME_METHODS -> hookMethod(m) { chain -> decide(m, chain) }
                in MUTE_METHODS -> hookMethod(m) { chain ->
                    val cfg = snapshot()
                    if (XpState.Flags.forceVolume ||
                        (cfg.volumeEnable && (cfg.volumeMaster || cfg.volumeBlockMute))) {
                        logWarn("blocked ${m.name}")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }

                "setRingerMode" -> hookMethod(m) { chain ->
                    val cfg = snapshot()
                    if (XpState.Flags.forceVolume ||
                        (cfg.volumeEnable && (cfg.volumeMaster || cfg.volumeBlockRinger))) {
                        logWarn("blocked setRingerMode")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }
            }
        }

        
        runCatching {
            val at = cls("android.media.AudioTrack")
            at?.declaredMethods?.filter { it.name == "setVolume" || it.name == "setStereoVolume" }
                ?.forEach { m -> hookMethod(m) { chain -> decide(m, chain) } }
        }
        runCatching {
            val mp = cls("android.media.MediaPlayer")
            mp?.declaredMethods?.filter { it.name == "setVolume" }
                ?.forEach { m -> hookMethod(m) { chain -> decide(m, chain) } }
        }

        
        if (snapshot().volumeLock) startLock()

        logInfo("volume defender installed")
    }

    

    



    private fun startLock() {
        val t = Thread {
            logInfo("volume lock started")
            var round = 0
            while (snapshot().volumeLock) {
                runCatching { applyLock() }
                round++
                if (round == 1 || round % 50 == 0) {
                    logInfo("volume lock: 第 $round 轮")
                }
                runCatching { Thread.sleep(LOCK_INTERVAL_MS) }
            }
            logInfo("volume lock stopped")
        }
        t.isDaemon = true
        t.name = "xp-volume-lock"
        t.start()
    }

    
    private val LOCK_STREAMS = intArrayOf(3, 2, 5, 1, 4, 0, 8, 10)

    private fun applyLock() {
        val cfg = snapshot()
        val pct = cfg.volumeLockValue.coerceIn(1, 100)
        val am = audioManager() ?: return
        for (stream in LOCK_STREAMS) {
            runCatching {
                val max = am.getStreamMaxVolume(stream)
                if (max <= 0) return@runCatching
                val target = (max * pct / 100f).toInt().coerceIn(0, max)
                if (am.getStreamVolume(stream) != target) {
                    am.setStreamVolume(stream, target, 0)
                }
            }
        }
    }

    

    


    fun installNow() {
        XpState.Flags.forceVolume = true
        install()
    }

    

    private fun decide(m: Method, chain: XposedInterface.Chain): Any? {
        val cfg = snapshot()
        if (!cfg.volumeEnable && !XpState.Flags.forceVolume) return chain.proceed()

        
        
        if (XpState.Flags.forceVolume) {
            logWarn("blocked ${m.name} (forced master)")
            return deniedFor(m)
        }

        if (cfg.volumeLock) return chain.proceed()

        
        if (cfg.volumeMaster) {
            logWarn("blocked ${m.name} (volume master)")
            return deniedFor(m)
        }

        
        if (!cfg.volumeAllowLower) {
            logWarn("blocked ${m.name}")
            return deniedFor(m)
        }

        val args = chain.args
        val name = m.name

        
        if (name.startsWith("adjust")) {
            val dir = args.getOrNull(1) as? Int
            if (dir != null && dir > 0) {
                logWarn("blocked $name: direction up")
                return deniedFor(m)
            }
            return chain.proceed()
        }

        
        val target = args.getOrNull(1) as? Int
        if (target != null) {
            val current = currentVolume(args)
            if (current != null && target > current) {
                logWarn("blocked $name: raise $current -> $target")
                return deniedFor(m)
            }
        }
        return chain.proceed()
    }

    
    private fun currentVolume(args: List<Any?>): Int? {
        val am = audioManager() ?: return null
        val stream = args.getOrNull(0) as? Int ?: return null
        return runCatching { am.getStreamVolume(stream) }.getOrNull()
    }

    private fun audioManager(): AudioManager? = runCatching {
        val app = runCatching {
            val at = Class.forName("android.app.ActivityThread", false, classLoader)
            val m = at.getDeclaredMethod("currentApplication")
            m.isAccessible = true
            m.invoke(null)
        }.getOrNull() as? android.content.Context ?: return null
        app.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
    }.getOrNull()

    companion object {
        
        private const val LOCK_INTERVAL_MS = 200L
    }
}