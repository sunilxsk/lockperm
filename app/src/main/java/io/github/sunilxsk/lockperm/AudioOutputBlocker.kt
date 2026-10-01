package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import java.util.Collections
import java.util.WeakHashMap
























internal class AudioOutputBlocker(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val declaredStream: MutableMap<Any, Int> =
        Collections.synchronizedMap(WeakHashMap<Any, Int>())

    
    private val declaredIsUsage: MutableMap<Any, Boolean> =
        Collections.synchronizedMap(WeakHashMap<Any, Boolean>())

    
    @Volatile
    private var focusRequested = false

    fun installNow() {
        XpState.Flags.forceAudioOut = true
        install()
    }

    private fun on(): Boolean =
        XpState.Flags.forceAudioOut || snapshot().audioOutEnable

    fun install() {
        if (!on()) return
        hookAudioFocus()
        hookAudioTrack()
        hookMediaPlayer()
        hookSoundPool()
        hookTextToSpeech()
        hookRingtone()
        hookToneGenerator()
        logInfo("audio output blocker installed")
    }

    

    




    private fun blocked(group: String, usage: Int? = null, stream: Int? = null): Boolean {
        val cfg = snapshot()
        if (!cfg.audioOutEnable || group !in cfg.audioOutBlocked) return false

        
        if (group != XpConfig.KEY_AUDIO_OUT_MEDIA) return true

        return when (classifyMedia(usage, stream)) {
            T_FOCUS -> cfg.audioOutFocus      
            T_NO_FOCUS -> cfg.audioOutNoFocus 
            else -> true                      
        }
    }

    private fun classifyMedia(usage: Int?, stream: Int?): Int {
        
        if (usage == 1 || stream == 3) return T_FOCUS
        
        if (usage == 11 || usage == 13 || stream == 10) return T_NO_FOCUS
        
        return if (focusRequested) T_FOCUS else T_OTHER
    }

    

    private fun hookAudioTrack() {
        val at = frameworkCls("android.media.AudioTrack")
            ?: runCatching { Class.forName("android.media.AudioTrack") }.getOrNull() ?: return

        at.declaredMethods.filter { it.name == "play" || it.name == "write" }.forEach { m ->
            hookMethod(m) { chain ->
                val self = chain.getThisObject() ?: return@hookMethod chain.proceed()
                val usage = usageOfAudioTrack(self)
                val stream = streamOfAudioTrack(self)
                val group = if (usage != null) groupOfUsage(usage) else groupOfStream(stream ?: 3)
                if (blocked(group, usage, stream)) {
                    logWarn("blocked audio: AudioTrack.${m.name} (usage=$usage, stream=$stream)")
                    return@hookMethod deniedFor(m)
                }
                chain.proceed()
            }
        }
    }

    
    private fun usageOfAudioTrack(self: Any): Int? {
        val attrs = fieldOfAny(self, "mAudioAttributes", "mAttributes")
        return usageOf(attrs)
    }

    
    private fun streamOfAudioTrack(self: Any): Int? {
        val v = fieldOfInt(self, "mStreamType", "mStream")
        return v
    }

    

    private fun hookMediaPlayer() {
        val mp = frameworkCls("android.media.MediaPlayer")
            ?: runCatching { Class.forName("android.media.MediaPlayer") }.getOrNull() ?: return

        mp.declaredMethods.forEach { m ->
            when (m.name) {
                
                "setAudioStreamType" -> hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val stream = chain.args.filterIsInstance<Int>().firstOrNull()
                    if (self != null && stream != null) {
                        declaredStream[self] = stream
                        declaredIsUsage[self] = false
                    }
                    chain.proceed()
                }

                "setAudioAttributes" -> hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val usage = usageOf(chain.args.firstOrNull())
                    if (self != null && usage != null) {
                        declaredStream[self] = usage
                        declaredIsUsage[self] = true
                    }
                    chain.proceed()
                }

                "start", "prepare", "prepareAsync" -> hookMethod(m) { chain ->
                    val self = chain.getThisObject() ?: return@hookMethod chain.proceed()
                    val isUsage = declaredIsUsage[self] ?: false
                    val v = declaredStream[self]
                    val usage = if (isUsage) v else null
                    val stream = if (isUsage) null else v
                    val group = if (usage != null) groupOfUsage(usage)
                    else groupOfStream(stream ?: 3)
                    if (blocked(group, usage, stream)) {
                        logWarn("blocked audio: MediaPlayer.${m.name} (usage=$usage, stream=$stream)")
                        return@hookMethod deniedFor(m)
                    }
                    chain.proceed()
                }
            }
        }
    }

    

    private fun hookSoundPool() {
        val sp = frameworkCls("android.media.SoundPool")
            ?: runCatching { Class.forName("android.media.SoundPool") }.getOrNull() ?: return
        sp.declaredMethods.filter { it.name == "play" }.forEach { m ->
            hookMethod(m) { chain ->
                val self = chain.getThisObject() ?: return@hookMethod chain.proceed()
                val isUsage = declaredIsUsage[self] ?: true
                val v = declaredStream[self] ?: lastSpUsage
                val usage = if (isUsage) v else null
                val stream = if (isUsage) null else v
                
                if (blocked(XpConfig.KEY_AUDIO_OUT_MEDIA, usage, stream)) {
                    logWarn("blocked audio: SoundPool.play (usage=$usage, stream=$stream)")
                    return@hookMethod deniedFor(m)
                }
                chain.proceed()
            }
        }

        
        val builder = frameworkCls("android.media.SoundPool\$Builder")
        if (builder != null) {
            builder.declaredMethods.filter { it.name == "setAudioAttributes" }.forEach { m ->
                hookMethod(m) { chain ->
                    val usage = usageOf(chain.args.firstOrNull())
                    if (usage != null) lastSpUsage = usage
                    chain.proceed()
                }
            }
        }
    }

    @Volatile
    private var lastSpUsage: Int? = null

    

    private fun hookTextToSpeech() {
        val tts = frameworkCls("android.speech.tts.TextToSpeech")
            ?: runCatching { Class.forName("android.speech.tts.TextToSpeech") }.getOrNull()
            ?: return
        tts.declaredMethods.filter { it.name == "speak" }.forEach { m ->
            hookMethod(m) { chain ->
                val bundle = chain.args.filterIsInstance<android.os.Bundle>().firstOrNull()
                val stream = bundle?.getInt("streamType", -1)?.takeIf { it >= 0 }
                val usage = usageOf(chain.args.firstOrNull { isAudioAttrs(it) })
                val group = when {
                    usage != null -> groupOfUsage(usage)
                    stream != null -> groupOfStream(stream)
                    else -> XpConfig.KEY_AUDIO_OUT_TTS
                }
                if (blocked(group, usage, stream)) {
                    logWarn("blocked audio: TextToSpeech.speak (usage=$usage, stream=$stream)")
                    return@hookMethod deniedFor(m)
                }
                chain.proceed()
            }
        }
    }

    

    private fun hookRingtone() {
        val r = frameworkCls("android.media.Ringtone")
            ?: runCatching { Class.forName("android.media.Ringtone") }.getOrNull() ?: return
        r.declaredMethods.filter { it.name == "play" }.forEach { m ->
            hookMethod(m) { chain ->
                val self = chain.getThisObject() ?: return@hookMethod chain.proceed()
                val stream = declaredStream[self] ?: fieldOfInt(self, "mStreamType")
                val group = stream?.let { groupOfStream(it) } ?: XpConfig.KEY_AUDIO_OUT_RING
                if (blocked(group, null, stream)) {
                    logWarn("blocked audio: Ringtone.play (stream=$stream)")
                    return@hookMethod deniedFor(m)
                }
                chain.proceed()
            }
        }
    }

    private fun hookToneGenerator() {
        val tg = frameworkCls("android.media.ToneGenerator")
            ?: runCatching { Class.forName("android.media.ToneGenerator") }.getOrNull() ?: return
        tg.declaredMethods.filter { it.name == "startTone" }.forEach { m ->
            hookMethod(m) { chain ->
                val stream = chain.args.filterIsInstance<Int>().lastOrNull()
                val group = stream?.let { groupOfStream(it) } ?: XpConfig.KEY_AUDIO_OUT_SYSTEM
                if (blocked(group, null, stream)) {
                    logWarn("blocked audio: ToneGenerator.startTone (stream=$stream)")
                    return@hookMethod deniedFor(m)
                }
                chain.proceed()
            }
        }
    }

    

    private fun hookAudioFocus() {
        val am = frameworkCls("android.media.AudioManager") ?: return
        am.declaredMethods.filter { it.name == "requestAudioFocus" }.forEach { m ->
            hookMethod(m) { chain ->
                val r = chain.proceed()
                
                if ((r as? Int) == 1) {
                    focusRequested = true
                    logWarn("audio focus granted -> 标记为「有焦点型」")
                }
                r
            }
        }
        am.declaredMethods.filter { it.name == "abandonAudioFocus" }.forEach { m ->
            hookMethod(m) { chain ->
                val r = chain.proceed()
                focusRequested = false
                r
            }
        }
        logInfo("audio focus tracker installed")
    }

    

    private fun groupOfUsage(usage: Int): String = when (usage) {
        1, 14, 16 -> XpConfig.KEY_AUDIO_OUT_MEDIA             
        2, 3 -> XpConfig.KEY_AUDIO_OUT_CALL                   
        4 -> XpConfig.KEY_AUDIO_OUT_ALARM                     
        6 -> XpConfig.KEY_AUDIO_OUT_RING                      
        5, 7, 8, 9, 12 -> XpConfig.KEY_AUDIO_OUT_NOTIFY       
        10 -> XpConfig.KEY_AUDIO_OUT_SYSTEM                   
        11 -> XpConfig.KEY_AUDIO_OUT_TTS                      
        13 -> XpConfig.KEY_AUDIO_OUT_SYSTEM                   
        else -> XpConfig.KEY_AUDIO_OUT_MEDIA
    }

    private fun groupOfStream(stream: Int): String = when (stream) {
        0 -> XpConfig.KEY_AUDIO_OUT_CALL                      
        1 -> XpConfig.KEY_AUDIO_OUT_SYSTEM                    
        2 -> XpConfig.KEY_AUDIO_OUT_RING                      
        3 -> XpConfig.KEY_AUDIO_OUT_MEDIA                     
        4 -> XpConfig.KEY_AUDIO_OUT_ALARM                     
        5 -> XpConfig.KEY_AUDIO_OUT_NOTIFY                    
        8 -> XpConfig.KEY_AUDIO_OUT_SYSTEM                    
        10 -> XpConfig.KEY_AUDIO_OUT_TTS                      
        else -> XpConfig.KEY_AUDIO_OUT_MEDIA
    }

    private fun isAudioAttrs(o: Any?): Boolean =
        o?.javaClass?.name?.contains("AudioAttributes") == true

    
    private fun usageOf(attrs: Any?): Int? {
        if (attrs == null) return null
        return fieldOfInt(attrs, "mUsage", "musage", "usage")
    }

    private fun fieldOfInt(target: Any, vararg names: String): Int? {
        val v = fieldOfAny(target, *names) ?: return null
        return (v as? Number)?.toInt()
    }

    private fun fieldOfAny(target: Any, vararg names: String): Any? {
        var c: Class<*>? = target.javaClass
        while (c != null) {
            for (n in names) {
                val v = runCatching {
                    val f = c!!.getDeclaredField(n)
                    f.isAccessible = true
                    f.get(target)
                }.getOrNull()
                if (v != null) return v
            }
            c = c.superclass
        }
        return null
    }

    companion object {
        
        private const val T_FOCUS = 0    
        private const val T_NO_FOCUS = 1 
        private const val T_OTHER = 2    
    }
}
