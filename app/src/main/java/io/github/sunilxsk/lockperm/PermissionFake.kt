package io.github.sunilxsk.lockperm

import android.app.Activity
import android.content.ContentResolver
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method












internal class PermissionFake(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun install() {
        hookCheckPermissions()
        hookRequestPermissions()
        hookFakeData()
        val cfg = snapshot()
        logInfo(
            "permission fake installed (grant=${cfg.effectiveGrant().size} 组, " +
                    "fake=${cfg.effectiveFakeData().size} 组, " +
                    "appConfig=${cfg.permGrantApp != null})"
        )
    }

    

    
    private fun permArgOf(m: java.lang.reflect.Method, chain: io.github.libxposed.api.XposedInterface.Chain): String? {
        val args = runCatching { chain.args }.getOrNull() ?: return null
        
        val byIndex = args.getOrNull(0) as? String
        if (byIndex != null) return byIndex
        
        return args.filterIsInstance<String>().firstOrNull()
    }

    private val PERM_CHECK_NAMES = setOf(
        "checkSelfPermission",
        "checkCallingOrSelfPermission",
        "checkCallingPermission",
        "checkCallingOrSelfUriPermission",
        "checkUriPermission",
    )

    private var logged = 0

    





    private fun shouldGrant(permission: String?): Boolean {
        if (permission.isNullOrBlank()) return false
        val cfg = snapshot()
        if (!cfg.permEnable) return false
        val group = XpConfig.groupOf(permission)
        if (group == null) {
            
            if (logged < 6) {
                logged++
                logWarn("未收录的权限（不会伪装）: $permission")
            }
            return false
        }
        val on = group.id in cfg.effectiveGrant()
        if (logged < 12) {
            logged++
            val msg = "check ${group.id} (${group.label}) -> ${if (on) "伪装已授权" else "按真实结果"}"
            logWarn(msg)
            
            if (logged <= 3) toast("XP: $msg")
        }
        return on
    }

    private fun fakeDataEnabled(group: String?): Boolean {
        if (group == null) return false
        val cfg = snapshot()
        if (!cfg.permEnable) return false
        return group in cfg.effectiveFakeData()
    }

    private fun hookCheckPermissions() {
        
        
        
        
        
        val ctxImpl = frameworkCls("android.app.ContextImpl")
        if (ctxImpl != null) {
            runCatching {
                val m = ctxImpl.getDeclaredMethod("checkSelfPermission", String::class.java)
                hookMethod(m) { chain ->
                    val result = chain.proceed()
                    val perm = chain.getArg(0) as? String
                    if (shouldGrant(perm)) PackageManager.PERMISSION_GRANTED else result
                }
            }
            runCatching {
                val m = ctxImpl.getDeclaredMethod(
                    "checkPermission", String::class.java,
                    Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                )
                hookMethod(m) { chain ->
                    val result = chain.proceed()
                    val perm = chain.getArg(0) as? String
                    if (shouldGrant(perm)) PackageManager.PERMISSION_GRANTED else result
                }
            }
        }

        
        
        val wrapper = frameworkCls("android.content.ContextWrapper")
        if (wrapper != null) {
            PERM_CHECK_NAMES.forEach { name ->
                wrapper.declaredMethods.filter { it.name == name }.forEach { m ->
                    hookMethod(m) { chain ->
                        val result = chain.proceed()
                        val perm = permArgOf(m, chain)
                        if (shouldGrant(perm)) PackageManager.PERMISSION_GRANTED else result
                    }
                }
            }
        }

        
        runCatching {
            val apm = frameworkCls("android.app.ApplicationPackageManager") ?: return@runCatching
            val m = apm.getDeclaredMethod(
                "checkPermission", String::class.java, String::class.java
            )
            hookMethod(m) { chain ->
                val result = chain.proceed()
                val perm = chain.getArg(0) as? String
                val pkg = chain.getArg(1) as? String
                if (pkg == XpState.packageName && shouldGrant(perm)) {
                    PackageManager.PERMISSION_GRANTED
                } else {
                    result
                }
            }
        }

        
        val contextCompat = cls("androidx.core.content.ContextCompat")
        if (contextCompat != null) {
            hookAll(contextCompat, { it.name == "checkSelfPermission" }) { chain ->
                val result = chain.proceed()
                val perm = chain.args.getOrNull(1) as? String
                if (shouldGrant(perm)) PackageManager.PERMISSION_GRANTED else result
            }
        }
        val checker = cls("androidx.core.content.PermissionChecker")
        if (checker != null) {
            hookAll(checker, { it.name == "checkSelfPermission" || it.name == "checkPermission" }) { chain ->
                val result = chain.proceed()
                val perm = chain.args.getOrNull(1) as? String
                if (shouldGrant(perm)) 0 else result
            }
        }
    }

    

    private fun hookRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        runCatching {
            val m = Activity::class.java.getDeclaredMethod(
                "requestPermissions", Array<String>::class.java, Int::class.javaPrimitiveType
            )
            hookMethod(m) { chain ->
                @Suppress("UNCHECKED_CAST")
                val perms = chain.getArg(0) as? Array<String> ?: return@hookMethod chain.proceed()
                if (perms.isEmpty() || !perms.all { shouldGrant(it) }) return@hookMethod chain.proceed()

                val code = chain.getArg(1) as? Int ?: 0
                val act = chain.getThisObject() as? Activity ?: return@hookMethod chain.proceed()
                
                val newArgs = chain.args.toTypedArray()
                newArgs[0] = emptyArray<String>()
                val result = chain.proceed(newArgs)
                
                val grants = IntArray(perms.size) { PackageManager.PERMISSION_GRANTED }
                mainHandler.post { deliverResult(act, code, perms, grants) }
                result
            }
        }

        val compat = cls("androidx.core.app.ActivityCompat")
        if (compat != null) {
            hookAll(compat, { it.name == "requestPermissions" }) { chain ->
                @Suppress("UNCHECKED_CAST")
                val perms = chain.args.getOrNull(1) as? Array<String>
                    ?: return@hookAll chain.proceed()
                if (perms.isEmpty() || !perms.all { shouldGrant(it) }) return@hookAll chain.proceed()
                val act = chain.args.getOrNull(0) as? Activity ?: return@hookAll chain.proceed()
                val code = chain.args.getOrNull(2) as? Int ?: 0
                val newArgs = chain.args.toTypedArray()
                newArgs[1] = emptyArray<String>()
                val result = chain.proceed(newArgs)
                val grants = IntArray(perms.size) { PackageManager.PERMISSION_GRANTED }
                mainHandler.post { deliverResult(act, code, perms, grants) }
                result
            }
        }

        val contracts = cls(
            "androidx.activity.result.contract.ActivityResultContracts\$RequestMultiplePermissions"
        )
        if (contracts != null) {
            hookAll(contracts, { it.name == "parseResult" }) { chain ->
                val result = chain.proceed()
                @Suppress("UNCHECKED_CAST")
                val map = (result as? Map<String, Boolean>)?.toMutableMap()
                    ?: return@hookAll result
                var changed = false
                map.keys.toList().forEach { perm ->
                    if (shouldGrant(perm)) {
                        map[perm] = true
                        changed = true
                    }
                }
                if (changed) map else result
            }
        }
    }

    private fun deliverResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        runCatching {
            val m: Method = Activity::class.java.getDeclaredMethod(
                "onRequestPermissionsResult",
                Int::class.javaPrimitiveType,
                Array<String>::class.java,
                IntArray::class.java,
            )
            m.isAccessible = true
            m.invoke(activity, requestCode, permissions, grantResults)
        }
    }

    

    private fun hookFakeData() {
        
        runCatching {
            hookAll(ContentResolver::class.java, { it.name == "query" }) { chain ->
                val uri = chain.args.getOrNull(0) as? Uri
                val group = FakeData.groupOfUri(uri)
                if (group != null && fakeDataEnabled(group)) {
                    @Suppress("UNCHECKED_CAST")
                    val projection = chain.args.getOrNull(1) as? Array<String>
                    return@hookAll FakeData.cursor(group, projection)
                }
                chain.proceed()
            }
        }

        
        val tm = cls("android.telephony.TelephonyManager")
        if (tm != null) {
            FakeData.TELEPHONY_FAKE.keys.forEach { name ->
                hookAll(tm, { it.name == name }) { chain ->
                    val result = chain.proceed()
                    if (fakeDataEnabled(XpConfig.PERM_PHONE)) {
                        FakeData.TELEPHONY_FAKE[name] ?: result
                    } else {
                        result
                    }
                }
            }
        }

        
        val lm = cls("android.location.LocationManager")
        if (lm != null) {
            hookAll(lm, { it.name == "getLastKnownLocation" }) { chain ->
                val result = chain.proceed()
                if (result == null && fakeDataEnabled(XpConfig.PERM_LOCATION)) {
                    FakeData.fakeLocation(chain.args.getOrNull(0) as? String)
                } else {
                    result
                }
            }
        }
    }
}
