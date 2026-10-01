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

    
    fun apply(payload: String): Boolean {
        if (!loaded) return false
        return runCatching {
            applyConfig(payload)
        }.onFailure {
            Log.w(TAG, "applyConfig failed: ${it.message}")
        }.getOrDefault(false)
    }

    
    fun disable() {
        if (!loaded) return
        runCatching { setEnabled(false) }
            .onFailure { Log.w(TAG, "setEnabled failed: ${it.message}") }
    }

    
    fun ready(): Boolean {
        if (!loaded) return false
        return runCatching { isReady() }.getOrDefault(false)
    }

    fun version(): String =
        if (!loaded) "" else runCatching { nativeVersion() }.getOrDefault("")

    
    fun hookCount(): Int =
        if (!loaded) 0 else runCatching { nativeHookCount() }.getOrDefault(0)

    
    private external fun applyConfig(payload: String): Boolean
    private external fun setEnabled(on: Boolean)
    private external fun isReady(): Boolean
    private external fun nativeVersion(): String
    private external fun nativeHookCount(): Int
}
