package io.github.sunilxsk.lockperm

import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import io.github.libxposed.api.XposedModule




































internal class ShizukuDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val mainHandler = Handler(Looper.getMainLooper())

    
    private val CANDIDATES = listOf(
        
        "rikka.shizuku.Shizuku",
        "rikka.shizuku.ShizukuProvider",
        "rikka.shizuku.ShizukuService",
        "rikka.shizuku.ShizukuBinder",
        "rikka.shizuku.ShizukuSystemApis",
        "rikka.shizuku.ShizukuManager",
        "rikka.shizuku.ShizukuApiConstants",
        "rikka.shizuku.ShizukuRemoteProcess",
        "rikka.shizuku.ShizukuBinderWrapper",
        "rikka.shizuku.ShizukuServiceConnection",
        "rikka.shizuku.ShizukuServiceConnections",
        
        "moe.shizuku.api.Shizuku",
        "moe.shizuku.api.ShizukuProvider",
        "moe.shizuku.api.ShizukuService",
        "moe.shizuku.api.ShizukuBinder",
        "moe.shizuku.api.ShizukuSystemApis",
        "moe.shizuku.api.ShizukuManager",
        "moe.shizuku.api.ShizukuApiConstants",
        
        "rikka.sui.Sui",
        
        "moe.shizuku.server.IShizukuService\$Stub",
        "moe.shizuku.server.IShizukuService\$Stub\$Proxy",
    )

    
    private val done = java.util.Collections.synchronizedSet(HashSet<String>())

    
    private val listeners = java.util.Collections.synchronizedList(ArrayList<Any>())

    





    @Volatile private var blockAuth = false
    @Volatile private var blockUse = false

    fun installNow() {
        XpState.Flags.forceShizuku = true
        install()
    }

    private fun on(): Boolean =
        XpState.Flags.forceShizuku || snapshot().shizukuEnable

    fun install() {
        if (!on()) return

        
        val cfg = snapshot()
        
        blockAuth = XpState.Flags.forceShizuku || (cfg.shizukuEnable && cfg.shizukuBlockAuth)
        blockUse = XpState.Flags.forceShizuku || (cfg.shizukuEnable && cfg.shizukuBlockUse)

        
        var n = 0
        CANDIDATES.forEach { name ->
            val c = cls(name) ?: return@forEach
            if (done.add(name) && hookShizukuClass(c)) n++
        }

        
        hookClassLoading()

        
        hookJump()

        logInfo("shizuku defender installed (classes=$n, blockAuth=$blockAuth, blockUse=$blockUse)")
    }

    private fun authOn(): Boolean = blockAuth
    private fun useOn(): Boolean = blockUse

    

    






    private fun hookShizukuClass(c: Class<*>): Boolean {
        var n = 0
        val methods = runCatching { c.declaredMethods }.getOrNull() ?: return false
        val isProvider = c.name.lowercase().contains("provider")
        val tag = c.simpleName

        methods.forEach { m ->
            val name = m.name.lowercase()

            when {
                
                
                
                name == "onbinderreceived" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!useOn()) {
                                chain.proceed()
                            } else {
                                logWarn("blocked binder attach: $tag.${m.name}")
                                null 
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                
                name == "requireservice" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!useOn()) {
                                chain.proceed()
                            } else {
                                logWarn("blocked $tag.${m.name} -> throw IllegalStateException")
                                throw IllegalStateException("Shizuku unavailable")
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                
                
                isProvider && name == "call" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!useOn()) {
                                chain.proceed()
                            } else {
                                val method = chain.args.getOrNull(0) as? String
                                if (method?.lowercase()?.contains("sendbinder") == true) {
                                    logWarn("blocked $tag.call(sendBinder)")
                                    null 
                                } else {
                                    chain.proceed()
                                }
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                
                isProvider && name == "oncreate" -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!useOn()) {
                                chain.proceed()
                            } else {
                                logWarn("blocked $tag.onCreate (provider init)")
                                true 
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                
                
                
                (name.contains("add") && name.contains("requestpermissionresultlistener")) ||
                        (name.contains("add") && name.contains("permission") && name.contains("listener")) -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!authOn()) {
                                chain.proceed()
                            } else {
                                val l = findListener(chain.args)
                                if (l != null) listeners.add(l)
                                logWarn("intercepted $tag.${m.name}")
                                null 
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                
                
                name.contains("requestpermission") && !name.contains("listener") -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!authOn()) {
                                chain.proceed()
                            } else {
                                val code = chain.args.filterIsInstance<Int>().firstOrNull() ?: 0
                                logWarn("blocked $tag.${m.name}($code)")
                                deliverDenied(code)
                                null 
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                
                
                
                (name.contains("check") && name.contains("permission")) ||
                        name.contains("haspermission") ||
                        name.contains("ispermissiongranted") -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!authOn()) {
                                chain.proceed()
                            } else {
                                logWarn("$tag.${m.name} -> DENIED")
                                
                                if (m.returnType == java.lang.Boolean.TYPE) false else PERMISSION_DENIED
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                name.contains("shouldshow") && name.contains("permission") -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!authOn()) {
                                chain.proceed()
                            } else {
                                false
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                (name.contains("binder") && (name.startsWith("get") || name.startsWith("peek") || name.startsWith("acquire"))) ||
                        name.contains("newprocess") ||
                        name.contains("newremoteprocess") ||
                        name.contains("getuserservice") ||
                        name.contains("binduserservice") ||
                        (name.contains("transact") && name.contains("remote")) -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!useOn()) {
                                chain.proceed()
                            } else {
                                logWarn("blocked $tag.${m.name} -> null/noop")
                                if (m.returnType == java.lang.Boolean.TYPE) false else null
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }

                
                name.contains("pingbinder") || name.contains("isprev11") -> {
                    runCatching {
                        hookMethod(m) { chain ->
                            if (!useOn()) {
                                chain.proceed()
                            } else {
                                false
                            }
                        }
                        n++
                    }.onFailure {
                        logWarn("hook failed: $tag.${m.name} -> ${it.javaClass.simpleName}: ${it.message}")
                    }
                }
            }
        }
        return n > 0
    }

    




    private fun findListener(args: List<Any?>): Any? {
        args.forEach { arg ->
            if (arg != null) {
                
                arg.javaClass.interfaces.forEach { iface ->
                    val iname = iface.simpleName.lowercase()
                    if (iname.contains("requestpermissionresult") ||
                        iname.contains("permissionresultlistener")) {
                        return arg
                    }
                }
                
                if (arg.javaClass.simpleName.lowercase().contains("listener")) {
                    return arg
                }
            }
        }
        
        return args.firstOrNull { it != null }
    }

    

    






    private fun hookClassLoading() {
        runCatching {
            val m = ClassLoader::class.java.getDeclaredMethod("loadClass", String::class.java)
            hookMethod(m) { chain ->
                val r = chain.proceed()
                runCatching {
                    val name = chain.getArg(0) as? String
                    if (name != null && isShizuku(name) && done.add(name) && r is Class<*>) {
                        if (hookShizukuClass(r)) {
                            logInfo("shizuku: 动态补 hook -> $name")
                        }
                    }
                }
                r
            }
            logInfo("shizuku: class loading hook installed")
        }.onFailure {
            logWarn("shizuku: class loading hook failed: ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    private fun isShizuku(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("shizuku")
    }

    

    







    private fun hookJump() {
        if (!authOn() && !useOn()) return
        listOf(
            "android.app.Activity",
            "android.app.ContextImpl",
            "android.content.ContextWrapper",
        ).forEach { className ->
            val c = frameworkCls(className) ?: return@forEach
            c.declaredMethods.filter { it.name in START_METHODS }.forEach { m ->
                runCatching {
                    hookMethod(m) { chain ->
                        val intent = runCatching { findIntent(chain) }.getOrNull()
                        if (intent != null && targetsShizuku(intent)) {
                            logWarn("blocked jump to Shizuku: $intent")
                            deliverDenied(0)
                            null
                        } else {
                            chain.proceed()
                        }
                    }
                }
            }
        }
        
        val inst = frameworkCls("android.app.Instrumentation")
        if (inst != null) {
            inst.declaredMethods.filter { it.name == "execStartActivity" }.forEach { m ->
                runCatching {
                    hookMethod(m) { chain ->
                        val intent = runCatching { chain.getArg(2) as? Intent }.getOrNull()
                        if (intent != null && targetsShizuku(intent)) {
                            logWarn("blocked jump to Shizuku (Instrumentation)")
                            deliverDenied(0)
                            null
                        } else {
                            chain.proceed()
                        }
                    }
                }
            }
        }
    }

    private fun findIntent(chain: io.github.libxposed.api.XposedInterface.Chain): Intent? {
        val args = runCatching { chain.args }.getOrNull() ?: return null
        args.filterIsInstance<Intent>().firstOrNull()?.let { return it }
        val arr = args.firstOrNull { it is Array<*> } as? Array<*> ?: return null
        return arr.filterIsInstance<Intent>().firstOrNull()
    }

    private fun targetsShizuku(intent: Intent): Boolean {
        val text = buildString {
            append(intent.action ?: "")
            append(' ')
            append(intent.component?.className ?: "")
            append(' ')
            append(intent.component?.packageName ?: "")
            append(' ')
            append(intent.`package` ?: "")
            append(' ')
            append(intent.dataString ?: "")
        }.lowercase()
        return text.contains("shizuku")
    }

    

    private fun deliverDenied(requestCode: Int) {
        val list = listeners.toList()
        if (list.isEmpty()) return
        mainHandler.post {
            list.forEach { l ->
                runCatching {
                    val m = l.javaClass.getMethod(
                        "onRequestPermissionResult",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                    )
                    m.isAccessible = true
                    m.invoke(l, requestCode, PERMISSION_DENIED)
                    logWarn("delivered DENIED to Shizuku listener")
                }.onFailure {
                    
                    runCatching {
                        val m = l.javaClass.getMethod(
                            "onRequestPermissionResult",
                            java.lang.Integer::class.java,
                            java.lang.Integer::class.java,
                        )
                        m.isAccessible = true
                        m.invoke(l, requestCode, PERMISSION_DENIED)
                    }
                }
            }
        }
    }

    companion object {
        
        private const val PERMISSION_DENIED = -1

        private val START_METHODS = setOf(
            "startActivity", "startActivityForResult", "startActivities",
            "startActivityIfNeeded", "startActivityFromChild",
        )
    }
}