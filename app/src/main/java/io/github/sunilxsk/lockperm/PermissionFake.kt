package io.github.sunilxsk.lockperm

import android.app.Activity
import android.content.ContentResolver
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
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
        hookEnforcePermissions()
        hookRationale()
        hookRequestedPermissionsFlags()
        hookRequestPermissions()
        hookFakeData()
        val cfg = snapshot()
        logInfo(
            "permission fake installed (grant=${cfg.effectiveGrant().size} 组, " +
                    "fake=${cfg.effectiveFakeData().size} 组, " +
                    "appConfig=${cfg.permGrantApp != null})"
        )
    }

    

    
    private fun permArgOf(m: java.lang.reflect.Method?, chain: io.github.libxposed.api.XposedInterface.Chain): String? {
        val args = runCatching { chain.args }.getOrNull() ?: return null
        
        if (m != null && m.parameterTypes.firstOrNull() == android.content.Context::class.java) {
            return args.filterIsInstance<String>().firstOrNull()
        }
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
            
            
            hookAll(checker, { it.name in PERMISSION_CHECKER_NAMES }) { chain ->
                val result = chain.proceed()
                val perm = permArgOf(null, chain)
                if (shouldGrant(perm)) PackageManager.PERMISSION_GRANTED else result
            }
        }
    }

    private val PERMISSION_CHECKER_NAMES = setOf(
        "checkSelfPermission", "checkPermission",
        "checkCallingPermission", "checkCallingOrSelfPermission",
    )

    



    private fun hookEnforcePermissions() {
        val names = setOf(
            "enforcePermission",
            "enforceCallingPermission",
            "enforceCallingOrSelfPermission",
        )
        val ctxImpl = frameworkCls("android.app.ContextImpl")
        if (ctxImpl != null) {
            ctxImpl.declaredMethods.filter { it.name in names }.forEach { m ->
                hookMethod(m) { chain ->
                    val perm = permArgOf(m, chain)
                    if (shouldGrant(perm)) {
                        logWarn("enforce ${m.name} -> 放行 ($perm)")
                        null
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
        val wrapper = frameworkCls("android.content.ContextWrapper")
        if (wrapper != null) {
            wrapper.declaredMethods.filter { it.name in names }.forEach { m ->
                hookMethod(m) { chain ->
                    val perm = permArgOf(m, chain)
                    if (shouldGrant(perm)) {
                        logWarn("enforce ${m.name} -> 放行 ($perm)")
                        null
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
    }

    



    private fun hookRationale() {
        runCatching {
            val m = Activity::class.java.getDeclaredMethod(
                "shouldShowRequestPermissionRationale", String::class.java
            )
            hookMethod(m) { chain ->
                val perm = chain.getArg(0) as? String
                if (shouldGrant(perm)) false else chain.proceed()
            }
        }
        val compat = cls("androidx.core.app.ActivityCompat")
        if (compat != null) {
            hookAll(compat, { it.name == "shouldShowRequestPermissionRationale" }) { chain ->
                val perm = chain.args.filterIsInstance<String>().lastOrNull()
                if (shouldGrant(perm)) false else chain.proceed()
            }
        }
    }

    




    private fun hookRequestedPermissionsFlags() {
        val pmNames = listOf(
            "android.app.ApplicationPackageManager",
            "android.content.pm.PackageManager",
        )
        pmNames.forEach { name ->
            val c = frameworkCls(name) ?: return@forEach
            c.declaredMethods
                .filter { it.name == "getPackageInfo" || it.name == "getPackageInfoAsUser" }
                .forEach { m ->
                    hookMethod(m) { chain ->
                        val result = chain.proceed()
                        patchFlags(result)
                    }
                }
        }
    }

    private fun patchFlags(result: Any?): Any? {
        val pi = result as? android.content.pm.PackageInfo ?: return result
        val names = pi.requestedPermissions ?: return result
        val flags = pi.requestedPermissionsFlags ?: return result
        if (names.isEmpty() || flags.size < names.size) return result

        var changed = false
        for (i in names.indices) {
            if (shouldGrant(names[i]) &&
                flags[i] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED == 0
            ) {
                flags[i] = flags[i] or android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
                changed = true
            }
        }
        if (changed) logWarn("requestedPermissionsFlags -> 已补齐授予位")
        return pi
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
        hookQuery()
        hookTelephony()
        hookLocation()
        hookAccounts()
        hookUsageStats()
        hookActiveNotifications()
        hookBluetooth()
    }

    
    private fun hookQuery() {
        runCatching {
            hookAll(ContentResolver::class.java, { it.name == "query" }) { chain ->
                val uri = chain.args.getOrNull(0) as? Uri ?: return@hookAll chain.proceed()
                val group = FakeData.groupOfUri(uri) ?: return@hookAll chain.proceed()
                if (!fakeDataEnabled(group)) return@hookAll chain.proceed()
                
                
                val real = runCatching { chain.proceed() }.getOrNull()
                if (real != null) {
                    
                    
                    val empty = runCatching { (real as? Cursor)?.count == 0 }.getOrNull() ?: false
                    if (!empty) return@hookAll real
                    runCatching { (real as? Cursor)?.close() }
                }
                @Suppress("UNCHECKED_CAST")
                val projection = chain.args.getOrNull(1) as? Array<String>
                logWarn("query $group -> 返回伪造数据")
                FakeData.cursor(group, projection)
            }
        }
    }

    
    private fun hookTelephony() {
        val tm = cls("android.telephony.TelephonyManager") ?: return
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

    
    private fun hookLocation() {
        val lm = cls("android.location.LocationManager") ?: return
        
        hookAll(lm, { it.name == "getLastKnownLocation" }) { chain ->
            if (!fakeDataEnabled(XpConfig.PERM_LOCATION)) return@hookAll chain.proceed()
            val provider = chain.args.filterIsInstance<String>().firstOrNull()
            val real = runCatching { chain.proceed() }.getOrNull() as? Location
            
            real ?: FakeData.locationFor(provider ?: "fused", snapshot())
        }
        
        hookAll(lm, { it.name == "getBestProvider" }) { chain ->
            if (!fakeDataEnabled(XpConfig.PERM_LOCATION)) return@hookAll chain.proceed()
            val real = runCatching { chain.proceed() }.getOrNull() as? String
            real ?: "gps"
        }
        hookAll(lm, { it.name == "isProviderEnabled" || it.name == "isLocationEnabled" }) { chain ->
            if (!fakeDataEnabled(XpConfig.PERM_LOCATION)) return@hookAll chain.proceed()
            val real = runCatching { chain.proceed() }.getOrNull() as? java.lang.Boolean
            real?.booleanValue() ?: true
        }
    }

    
    private fun hookAccounts() {
        val am = cls("android.accounts.AccountManager") ?: return
        val names = setOf(
            "getAccounts", "getAccountsByType", "getAccountsByTypeForPackage",
            "getAccountsForPackage",
        )
        hookAll(am, { it.name in names }) { chain ->
            val group = XpConfig.PERM_CONTACTS
            val on = fakeDataEnabled(group)
            if (!on) return@hookAll chain.proceed()
            @Suppress("UNCHECKED_CAST")
            val real = runCatching { chain.proceed() }.getOrNull() as? Array<android.accounts.Account>
            
            if (!real.isNullOrEmpty()) return@hookAll real

            val list = FakeData.ACCOUNTS.map { (type, name) ->
                android.accounts.Account(name, type)
            }
            logWarn("getAccounts -> 返回 ${list.size} 个虚构账户")
            list.toTypedArray()
        }
    }

    



    private fun hookUsageStats() {
        val um = cls("android.app.usage.UsageStatsManager") ?: return
        hookAll(um, { it.name == "queryUsageStats" || it.name == "queryAndAggregateUsageStats" }) { chain ->
            if (!fakeDataEnabled(XpConfig.PERM_USAGE_STATS)) return@hookAll chain.proceed()
            val real = runCatching { chain.proceed() }.getOrNull()
            if (real is Map<*, *>) {
                if (real.isNotEmpty()) return@hookAll real
            } else if (real is List<*>) {
                if (real.isNotEmpty()) return@hookAll real
            } else if (real != null) return@hookAll real

            val rows = FakeData.rowsOf(XpConfig.PERM_USAGE_STATS)
            val out = ArrayList<Any>(rows.size)
            for (r in rows) {
                val st = runCatching { makeUsageStats(r) }.getOrNull() ?: continue
                out.add(st)
            }
            if (out.isEmpty()) return@hookAll real
            logWarn("queryUsageStats -> 返回 ${out.size} 条虚构记录")
            out
        }
    }

    private fun makeUsageStats(r: Map<String, Any?>): Any {
        val cls = Class.forName("android.app.usage.UsageStats")
        val ctor = cls.getDeclaredConstructor()
        ctor.isAccessible = true
        val obj = ctor.newInstance()
        val pkg = r["package_name"] as? String ?: return obj
        setField(cls, obj, "mPackageName", pkg)
        setField(cls, obj, "mLastTimeUsed", r["last_time"] as? Long ?: 0L)
        setField(cls, obj, "mTotalTimeInForeground", r["total_time"] as? Long ?: 0L)
        setField(cls, obj, "mLaunchCount", r["launch_count"] as? Int ?: 0)
        setField(cls, obj, "mBeginTimeStamp", r["first_time"] as? Long ?: 0L)
        setField(cls, obj, "mEndTimeStamp", r["last_time"] as? Long ?: 0L)
        return obj
    }

    private fun setField(cls: Class<*>, obj: Any, name: String, value: Any) {
        runCatching {
            cls.getDeclaredField(name).apply { isAccessible = true }.set(obj, value)
        }
    }

    
    private fun hookActiveNotifications() {
        val nm = cls("android.app.NotificationManager") ?: return
        hookAll(nm, { it.name == "getActiveNotifications" }) { chain ->
            val on = fakeDataEnabled(XpConfig.PERM_NOTIFICATION) ||
                    fakeDataEnabled(XpConfig.PERM_NOTIFICATION_LISTENER)
            if (!on) return@hookAll chain.proceed()
            @Suppress("UNCHECKED_CAST")
            val real = runCatching { chain.proceed() }.getOrNull()
                    as? Array<android.service.notification.StatusBarNotification>
            if (!real.isNullOrEmpty()) return@hookAll real

            val rows = FakeData.rowsOf(XpConfig.PERM_NOTIFICATION)
            val out = ArrayList<Any>(rows.size)
            for (r in rows) {
                val sbn = runCatching { makeStatusBarNotification(r) }.getOrNull() ?: continue
                out.add(sbn)
            }
            if (out.isEmpty()) return@hookAll real
            logWarn("getActiveNotifications -> 返回 ${out.size} 条虚构通知")
            out.toTypedArray()
        }
    }

    private fun makeStatusBarNotification(r: Map<String, Any?>): Any {
        val cls = Class.forName("android.service.notification.StatusBarNotification")
        val pkg = r["package_name"] as? String ?: "com.android.systemui"
        val title = r["title"] as? String ?: ""
        val text = r["text"] as? String ?: ""
        val whenMs = r["post_time"] as? Long ?: System.currentTimeMillis()

        
        val ctx = appContext() ?: throw IllegalStateException("no context")
        val notif = android.app.Notification.Builder(ctx, "default")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setWhen(whenMs)
            .build()

        
        val ctor = runCatching {
            cls.getDeclaredConstructor(
                String::class.java, String::class.java, Int::class.javaPrimitiveType,
                String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                android.app.Notification::class.java, android.os.UserHandle::class.java,
                Long::class.javaPrimitiveType
            )
        }.getOrNull()

        val obj = if (ctor != null) {
            ctor.isAccessible = true
            ctor.newInstance(
                pkg, pkg, 1, null, 0, 0, notif, android.os.Process.myUserHandle(), whenMs
            )
        } else {
            val c2 = cls.getDeclaredConstructor(
                String::class.java, Int::class.javaPrimitiveType,
                android.app.Notification::class.java, android.os.UserHandle::class.java,
                Long::class.javaPrimitiveType
            )
            c2.isAccessible = true
            c2.newInstance(pkg, 1, notif, android.os.Process.myUserHandle(), whenMs)
        }
        return obj
    }

    




    private fun hookBluetooth() {
        val ba = cls("android.bluetooth.BluetoothAdapter") ?: return
        hookAll(ba, { it.name == "getBondedDevices" }) { chain ->
            if (!fakeDataEnabled(XpConfig.PERM_NEARBY)) return@hookAll chain.proceed()
            val real = runCatching { chain.proceed() }.getOrNull()
            real ?: java.util.Collections.emptySet<Any>()
        }
        
        hookAll(ba, { it.name == "startDiscovery" || it.name == "startLeScan" }) { chain ->
            if (!fakeDataEnabled(XpConfig.PERM_NEARBY)) return@hookAll chain.proceed()
            val real = runCatching { chain.proceed() }.getOrNull() as? java.lang.Boolean
            real?.booleanValue() ?: true
        }
    }
}
