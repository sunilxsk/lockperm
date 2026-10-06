package io.github.sunilxsk.lockperm

import android.util.Log











object NativeBridge {

    private const val TAG = "LockPerm.Native"

    private const val LIB = "lockperm"

    
    val loaded: Boolean by lazy {
        runCatching {
            System.loadLibrary(LIB)
            true
        }.onFailure {
            Log.w(TAG, "load $LIB failed: ${it.message}")
        }.getOrDefault(false)
    }

    






    @Volatile
    var pushed: Boolean = false
        private set

    
    fun apply(payload: String): Boolean {
        if (!loaded) return false
        return runCatching {
            applyConfig(payload)
        }.onSuccess {
            pushed = true
        }.onFailure {
            Log.w(TAG, "applyConfig failed: ${it.message}")
        }.getOrDefault(false)
    }

    
    fun disable() {
        if (!loaded) return
        runCatching { setEnabled(false) }
            .onFailure { Log.w(TAG, "setEnabled failed: ${it.message}") }
    }

    
    fun disableIfPushed() {
        if (pushed) disable()
    }

    
    fun ready(): Boolean {
        if (!loaded) return false
        return runCatching { isReady() }.getOrDefault(false)
    }

    fun version(): String =
        if (!loaded) "" else runCatching { nativeVersion() }.getOrDefault("")

    
    fun hookCount(): Int =
        if (!loaded) 0 else runCatching { nativeHookCount() }.getOrDefault(0)

    






    fun probe(): Map<String, String> {
        if (!loaded) return emptyMap()
        val raw = runCatching { nativeProbe() }.getOrNull().orEmpty()
        if (raw.isEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        raw.split('\n').forEach { line ->
            val i = line.indexOf('\t')
            if (i > 0) out[line.substring(0, i)] = line.substring(i + 1)
        }
        return out
    }

    
    private external fun applyConfig(payload: String): Boolean
    private external fun setEnabled(on: Boolean)
    private external fun isReady(): Boolean
    private external fun nativeVersion(): String
    private external fun nativeHookCount(): Int
    private external fun nativeProbe(): String
}
