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

    
    














    
    private val installerKey: String get() = javaClass.name

    private fun logOn(): Boolean = XpState.Flags.logEnabled

    







    private object LogThrottle {
        private const val WINDOW_MS = 2000L
        private val last = java.util.concurrent.ConcurrentHashMap<String, Long>()
        private val dropped = java.util.concurrent.ConcurrentHashMap<String, Int>()

        
        fun pass(key: String): Pair<Boolean, Int> {
            val now = System.currentTimeMillis()
            val prev = last[key]
            if (prev == null || now - prev > WINDOW_MS) {
                last[key] = now
                val n = dropped.remove(key) ?: 0
                return true to n
            }
            val n = (dropped[key] ?: 0) + 1
            dropped[key] = n
            return false to n
        }
    }

    private fun emit(level: Int, msg: String, t: Throwable?) {
        if (!logOn()) return
        val (pass, skipped) = LogThrottle.pass(msg)
        if (!pass) return
        val text = if (skipped > 0) "$msg（另有 $skipped 条相同日志已省略）" else msg
        try {
            if (t != null) module.log(level, TAG, "[$pkg] $text", t)
            else module.log(level, TAG, "[$pkg] $text")
        } catch (_: Throwable) {
        }
    }

    protected fun logInfo(msg: String) {
        emit(android.util.Log.INFO, msg, null)
    }

    protected fun logWarn(msg: String, t: Throwable? = null) {
        emit(android.util.Log.WARN, msg, t)
    }

    private val pkg: String get() = XpState.packageName

    






    protected fun frameworkCls(name: String): Class<*>? {
        cls(name)?.let { return it }
        runCatching { Class.forName(name) }.getOrNull()?.let { return it }
        return runCatching { Class.forName(name, true, ClassLoader.getSystemClassLoader()) }.getOrNull()
    }

    
    protected fun cls(name: String, cl: ClassLoader = classLoader): Class<*>? =
        runCatching { Class.forName(name, false, cl) }.getOrNull()

    





    








    private object ClassCache {
        private val map = java.util.concurrent.ConcurrentHashMap<String, Class<*>?>()

        fun get(name: String, find: () -> Class<*>?): Class<*>? {
            map[name]?.let { return it }
            if (map.containsKey(name)) return null
            val c = find()
            map[name] = c
            return c
        }
    }

    protected fun loadClassAnywhere(name: String): Class<*>? =
        ClassCache.get(name) {
            cls(name)
                ?: runCatching { Class.forName(name) }.getOrNull()
                ?: runCatching { Class.forName(name, true, ClassLoader.getSystemClassLoader()) }.getOrNull()
                ?: runCatching { Class.forName(name, true, Object::class.java.classLoader) }.getOrNull()
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
        deoptimize: Boolean = false,
        interceptor: (XposedInterface.Chain) -> Any?,
    ): Boolean {
        
        
        if (Modifier.isAbstract(method.modifiers)) return false
        if (!claimHook(installerKey, method)) return false
        return runCatching {
            if (deoptimize) deopt(method)
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
        deoptimize: Boolean = false,
        interceptor: (XposedInterface.Chain) -> Any?,
    ): Boolean {
        if (Modifier.isAbstract(ctor.modifiers)) return false
        if (!claimHook(installerKey, ctor)) return false
        return runCatching {
            if (deoptimize) deopt(ctor)
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

    








    protected fun orProceed(chain: XposedInterface.Chain, value: Any?): Any? =
        if (value == null) chain.proceed() else value

    
    protected fun orElse(value: Any?, fallback: Any?): Any? = value ?: fallback

    
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
        java.lang.Character.TYPE -> 0.toChar()
        android.os.Bundle::class.java -> android.os.Bundle()
        String::class.java, CharSequence::class.java -> ""
        
        
        
        List::class.java, java.util.ArrayList::class.java,
        java.util.Collection::class.java -> java.util.ArrayList<Any>()
        Map::class.java, java.util.HashMap::class.java -> java.util.HashMap<Any, Any>()
        else -> null
    }

    companion object {
        private const val TAG = "LockPerm"

        




        private val hooked = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

        
        internal fun claimHook(installer: String, e: Executable): Boolean {
            val params = runCatching {
                (e as? Method)?.parameterTypes
                    ?: (e as Constructor<*>).parameterTypes
            }.getOrNull()
            val key = installer + '#' + e.declaringClass.name + '#' + e.name +
                '#' + java.util.Arrays.toString(params)
            return hooked.putIfAbsent(key, true) == null
        }
    }
}
