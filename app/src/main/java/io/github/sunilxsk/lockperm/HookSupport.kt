package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Modifier
import java.lang.reflect.Method







internal abstract class HookSupport(
    protected val module: XposedModule,
    protected val prefs: SharedPreferences,
    protected val classLoader: ClassLoader,
) {

    
    private fun logOn(): Boolean = XpState.Flags.logEnabled

    protected fun logInfo(msg: String) {
        if (!logOn()) return
        try {
            module.log(android.util.Log.INFO, TAG, "[$pkg] $msg")
        } catch (_: Throwable) {
        }
    }

    protected fun logWarn(msg: String, t: Throwable? = null) {
        if (!logOn()) return
        try {
            if (t != null) module.log(android.util.Log.WARN, TAG, "[$pkg] $msg", t)
            else module.log(android.util.Log.WARN, TAG, "[$pkg] $msg")
        } catch (_: Throwable) {
        }
    }

    private val pkg: String get() = XpState.packageName

    






    protected fun frameworkCls(name: String): Class<*>? {
        cls(name)?.let { return it }
        runCatching { Class.forName(name) }.getOrNull()?.let { return it }
        return runCatching { Class.forName(name, true, ClassLoader.getSystemClassLoader()) }.getOrNull()
    }

    
    protected fun cls(name: String, cl: ClassLoader = classLoader): Class<*>? =
        runCatching { Class.forName(name, false, cl) }.getOrNull()

    





    protected fun loadClassAnywhere(name: String): Class<*>? {
        cls(name)?.let { return it }
        runCatching { Class.forName(name) }.getOrNull()?.let { return it }
        runCatching { Class.forName(name, true, ClassLoader.getSystemClassLoader()) }.getOrNull()?.let { return it }
        return runCatching { Class.forName(name, true, Object::class.java.classLoader) }.getOrNull()
    }

    
    protected fun tryGetMethod(clazz: Class<*>, name: String, vararg params: Class<*>): Method? {
        runCatching { clazz.getDeclaredMethod(name, *params) }.getOrNull()?.let { return it }
        runCatching { clazz.getMethod(name, *params) }.getOrNull()?.let { return it }
        var c = clazz
        while (c != Any::class.java && c != Object::class.java) {
            runCatching { c.getDeclaredMethod(name, *params) }.getOrNull()?.let { return it }
            c = runCatching { c.superclass }.getOrNull() ?: break
        }
        return null
    }

    
    protected fun makeAccessible(e: Executable) {
        runCatching { e.isAccessible = true }
    }

    
    protected fun hookMethod(
        method: Method,
        priority: Int = XposedInterface.PRIORITY_HIGHEST,
        interceptor: (XposedInterface.Chain) -> Any?,
    ): Boolean {
        
        
        if (Modifier.isAbstract(method.modifiers)) return false
        return runCatching {
            deopt(method)
            module.hook(method)
                .setPriority(priority)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(interceptor)
            true
        }.onFailure { logWarn("hook ${method.declaringClass.name}.${method.name} failed", it) }
            .getOrDefault(false)
    }

    
    protected fun hookCtor(
        ctor: Constructor<*>,
        priority: Int = XposedInterface.PRIORITY_HIGHEST,
        interceptor: (XposedInterface.Chain) -> Any?,
    ): Boolean {
        if (Modifier.isAbstract(ctor.modifiers)) return false
        return runCatching {
            deopt(ctor)
            module.hook(ctor)
                .setPriority(priority)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(interceptor)
            true
        }.onFailure { logWarn("hook ctor ${ctor.declaringClass.name} failed", it) }
            .getOrDefault(false)
    }

    
    protected fun hookAllCtors(
        clazz: Class<*>,
        interceptor: (XposedInterface.Chain) -> Any?,
    ) {
        clazz.declaredConstructors.forEach { c -> hookCtor(c, interceptor = interceptor) }
    }

    
    protected fun hookAllByName(
        clazz: Class<*>,
        name: String,
        interceptor: (XposedInterface.Chain) -> Any?,
    ) {
        clazz.declaredMethods.filter { it.name == name }.forEach { m ->
            hookMethod(m, interceptor = interceptor)
        }
    }

    
    protected fun hookAll(
        clazz: Class<*>,
        predicate: (Method) -> Boolean,
        interceptor: (XposedInterface.Chain) -> Any?,
    ) {
        clazz.declaredMethods.filter(predicate).forEach { m ->
            hookMethod(m, interceptor = interceptor)
        }
    }

    






    protected fun deopt(e: Executable) {
        runCatching { module.deoptimize(e) }
    }

    protected fun snapshot(): XpState.Snapshot = XpState.refresh(prefs)

    
    protected fun toast(msg: String) {
        runCatching {
            val ctx = appContext() ?: return
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                runCatching {
                    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Volatile
    private var cachedApp: android.content.Context? = null

    
    protected fun appContext(): android.content.Context? {
        cachedApp?.let { return it }
        val c = runCatching {
            val at = Class.forName("android.app.ActivityThread")
            val m = at.getDeclaredMethod("currentApplication")
            m.isAccessible = true
            m.invoke(null) as? android.content.Context
        }.getOrNull()
        if (c != null) cachedApp = c
        return c
    }

    





    protected val INT_TYPE: Class<*> = Int::class.javaPrimitiveType!!

    
    protected fun deniedFor(m: Method): Any? = when (m.returnType) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Byte.TYPE -> 0.toByte()
        else -> null
    }

    companion object {
        private const val TAG = "LockPerm"
    }
}
