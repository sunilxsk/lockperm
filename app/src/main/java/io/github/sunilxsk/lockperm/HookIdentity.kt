package io.github.sunilxsk.lockperm

import android.content.ContentResolver
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.WebView
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method




internal class HookIdentity(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun install() {
        val cfg = snapshot()
        if (cfg.enableAndroidId) hookAndroidId()
        if (cfg.enableBuild) {
            hookBuildFields()
            hookDeviceName()
            hookGsfId()
            hookAdvertisingId()
            hookAppSetId()
            hookDrmId()
            hookAccounts()
            hookSystemProperties()
        }
        if (cfg.enableJs) hookWebView()
        if (cfg.enableUa) hookUserAgent()
    }

    

    
    private fun hookDeviceName() {
        val name = snapshot().deviceName
        if (name.isEmpty()) return
        val sg = runCatching { Class.forName("android.provider.Settings\$Global") }.getOrNull()
            ?: cls("android.provider.Settings\$Global") ?: return
        val keys = setOf("device_name", "marketname", "bluetooth_name")
        sg.declaredMethods.filter { it.name == "getString" }.forEach { m ->
            hookMethod(m) { chain ->
                val key = chain.getArg(1) as? String
                if (key in keys) name else chain.proceed()
            }
        }
        logInfo("device name hooked -> $name")
    }

    

    




    private fun hookGsfId() {
        val gsf = snapshot().gsfId
        if (gsf.isEmpty()) return
        val cr = runCatching { Class.forName("android.content.ContentResolver") }.getOrNull()
            ?: cls("android.content.ContentResolver") ?: return
        cr.declaredMethods.filter { it.name == "query" }.forEach { m ->
            hookMethod(m) { chain ->
                val orig = chain.proceed()
                val uri = chain.getArg(0) as? android.net.Uri
                gsfRewrite(uri, orig, gsf)
            }
        }
        logInfo("gsf id hooked")
    }

    private fun gsfRewrite(uri: android.net.Uri?, orig: Any?, gsf: String): Any? {
        val c = orig as? android.database.Cursor ?: return orig
        if (uri == null || !uri.toString().contains("gservices")) return orig
        return runCatching {
            if (c.count <= 0) return@runCatching orig
            val cols = c.columnNames
            var nameCol = -1
            var valCol = -1
            for (i in cols.indices) {
                val l = cols[i].lowercase()
                if (nameCol < 0 && (l.contains("name") || l.contains("key") || l == "_id")) nameCol = i
                if (valCol < 0 && (l.contains("value") || l.contains("val") || l == "data")) valCol = i
            }
            if (nameCol < 0) nameCol = 0
            if (valCol < 1) valCol = minOf(1, cols.size - 1)
            val rows = ArrayList<Array<Any?>>()
            var changed = false
            c.moveToFirst()
            do {
                val row = arrayOfNulls<Any>(cols.size)
                for (i in cols.indices) {
                    row[i] = runCatching { c.getString(i) }.getOrNull()
                }
                val n = runCatching { c.getString(nameCol) }.getOrNull()
                if (n != null && n.contains("android_id")) {
                    row[valCol] = gsf
                    changed = true
                }
                rows.add(row)
            } while (c.moveToNext())
            c.close()
            if (!changed) return@runCatching orig
            val out = android.database.MatrixCursor(cols)
            rows.forEach { out.addRow(it) }
            out
        }.getOrDefault(orig) as Any?
    }

    

    private fun hookAdvertisingId() {
        val id = snapshot().adsId
        if (id.isEmpty()) return
        val info = cls("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info") ?: return
        runCatching {
            info.getDeclaredMethod("getId").let { m -> hookMethod(m) { _ -> id } }
            logInfo("advertising id hooked")
        }
    }

    private fun hookAppSetId() {
        val id = snapshot().appSetId
        if (id.isEmpty()) return
        val info = cls("com.google.android.gms.appset.AppSetIdInfo") ?: return
        runCatching {
            info.getDeclaredMethod("getId").let { m -> hookMethod(m) { _ -> id } }
            logInfo("app set id hooked")
        }
    }

    

    
    private fun hookDrmId() {
        val hex = snapshot().drmId
        if (hex.length != 64 || !hex.matches(Regex("^[0-9A-Fa-f]+$"))) return
        val bytes = hexToBytes(hex)
        runCatching {
            val drm = Class.forName("android.media.MediaDrm")
            drm.getDeclaredMethod("getPropertyByteArray", String::class.java).let { m ->
                hookMethod(m) { chain ->
                    val k = chain.getArg(0)?.toString()
                    if (k == "deviceUniqueId") bytes else chain.proceed()
                }
                logInfo("drm id hooked")
            }
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val n = hex.length / 2
        val out = ByteArray(n)
        for (i in 0 until n) {
            out[i] = ((Character.digit(hex[i * 2], 16) shl 4) +
                    Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
        return out
    }

    

    private fun hookAccounts() {
        if (!snapshot().hideAccounts) return
        val am = runCatching { Class.forName("android.accounts.AccountManager") }.getOrNull()
            ?: cls("android.accounts.AccountManager") ?: return
        am.declaredMethods.forEach { m ->
            val ret = m.returnType
            
            if (ret.isArray && ret.componentType?.name == "android.accounts.Account") {
                hookMethod(m) { _ -> java.lang.reflect.Array.newInstance(ret.componentType, 0) }
            }
        }
        runCatching {
            am.getDeclaredMethod("hasAccount", android.accounts.Account::class.java).let { m ->
                hookMethod(m) { _ -> false }
            }
        }
        logInfo("accounts hidden")
    }

    

    








    private fun hookBuildFields() {
        val cfg = snapshot()
        val values = cfg.buildValues
        if (values.isEmpty()) {
            logInfo("build fields: 没有填任何值，跳过")
            return
        }

        
        val owners = HashMap<String, Class<*>>()
        XpConfig.OWNER_BUILD.let { n ->
            (runCatching { Class.forName(n) }.getOrNull() ?: cls(n))?.let { owners[n] = it }
        }
        XpConfig.OWNER_VERSION.let { n ->
            (runCatching { Class.forName(n) }.getOrNull() ?: cls(n))?.let { owners[n] = it }
        }
        var ok = 0
        XpConfig.BUILD_FIELDS.forEach { f ->
            val v = values[f.field] ?: return@forEach
            val c = owners[f.owner] ?: return@forEach
            if (setStaticString(c, f.field, v)) ok++
        }
        logInfo("build fields: 静态字段改成功 $ok/${values.size}")

        
        val buildCls = owners[XpConfig.OWNER_BUILD]
        val serial = values["SERIAL"]
        if (serial != null && buildCls != null) {
            runCatching {
                buildCls.getDeclaredMethod("getSerial").let { m ->
                    hookMethod(m) { _ -> serial }
                }
                logInfo("build fields: hooked Build.getSerial()")
            }
        }
        val radio = values["RADIO"]
        if (radio != null && buildCls != null) {
            runCatching {
                buildCls.getDeclaredMethod("getRadioVersion").let { m ->
                    hookMethod(m) { _ -> radio }
                }
                logInfo("build fields: hooked Build.getRadioVersion()")
            }
        }

        
    }

    






    private fun setStaticString(clazz: Class<*>, name: String, value: String): Boolean {
        return runCatching {
            val f = clazz.getDeclaredField(name)
            f.isAccessible = true
            runCatching { clearFinal(f) }
            f.set(null, value)
            true
        }.onFailure {
            logWarn("setStaticString $name failed: ${it.message}")
        }.getOrDefault(false)
    }

    private fun clearFinal(f: java.lang.reflect.Field) {
        
        runCatching {
            val m = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
            m.isAccessible = true
            m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        }.onFailure {
            
            runCatching {
                val m = java.lang.reflect.Field::class.java.getDeclaredField("accessFlags")
                m.isAccessible = true
                m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
            }
        }
    }

    
    





    private fun hookSystemProperties() {
        val sp = runCatching { Class.forName("android.os.SystemProperties") }.getOrNull()
            ?: cls("android.os.SystemProperties")
            ?: return

        
        val want = FakeProps.build(snapshot())
        if (want.isEmpty()) return

        var n = 0

        sp.declaredMethods.filter { it.name == "get" }.forEach { m ->
            when (m.parameterTypes.size) {
                1, 2 -> {
                    hookMethod(m) { chain ->
                        val key = chain.getArg(0) as? String
                        val v = want[key]
                        if (v != null) v else chain.proceed()
                    }
                    n++
                }
            }
        }

        
        val typed = mapOf(
            "getInt" to 1, "getLong" to 2, "getBoolean" to 3,
        )
        sp.declaredMethods.filter { it.name in typed.keys }.forEach { m ->
            val kind = typed[m.name] ?: return@forEach
            hookMethod(m) { chain ->
                val key = chain.getArg(0) as? String
                val v = want[key] ?: return@hookMethod chain.proceed()
                when (kind) {
                    1 -> v.toIntOrNull()
                    2 -> v.toLongOrNull()
                    else -> when (v.trim().lowercase()) {
                        "true", "1", "yes", "y", "on" -> true
                        "false", "0", "no", "n", "off" -> false
                        else -> null
                    }
                } ?: chain.proceed()
            }
            n++
        }

        if (n > 0) logInfo("system properties hooked x$n")
    }

    

    private fun hookAndroidId() {
        runCatching {
            val m: Method = Settings.Secure::class.java.getDeclaredMethod(
                "getString", ContentResolver::class.java, String::class.java
            )
            hookMethod(m) { chain ->
                val name = chain.getArg(1) as? String
                
                val v = if (name == Settings.Secure.ANDROID_ID) snapshot().androidId else ""
                if (name == Settings.Secure.ANDROID_ID && v.isNotBlank()) {
                    v
                } else {
                    chain.proceed()
                }
            }
            logInfo("android_id hook installed")
        }
    }

    

    private fun hookWebView() {
        runCatching {
            val wvc = cls("android.webkit.WebViewClient") ?: return
            val m = wvc.getDeclaredMethod(
                "onPageFinished", WebView::class.java, String::class.java
            )
            hookMethod(m) { chain ->
                val result = chain.proceed()
                val v = chain.getThisObject()
                if (v is WebView) evaluateJs(v)
                result
            }
            logInfo("onPageFinished hook installed")
        }

        runCatching {
            val wv = cls("android.webkit.WebView") ?: return
            val m = wv.getDeclaredMethod("loadUrl", String::class.java)
            hookMethod(m) { chain ->
                val result = chain.proceed()
                val self = chain.getThisObject()
                if (self is WebView) {
                    mainHandler.postDelayed({ evaluateJs(self) }, 1500)
                }
                result
            }
            logInfo("loadUrl hook installed")
        }
    }

    private fun evaluateJs(wv: WebView) {
        runCatching {
            val js = snapshot().jsCode
            if (js.isBlank()) return@runCatching
            wv.post { runCatching { wv.evaluateJavascript(js, null) } }
        }
    }

    

    













    private fun hookUserAgent() {
        val ua = snapshot().uaValue.trim()
        if (ua.isEmpty()) return

        val ws = cls("android.webkit.WebSettings")
            ?: runCatching { Class.forName("android.webkit.WebSettings") }.getOrNull()
            ?: return

        runCatching {
            ws.declaredMethods.filter {
                it.name == "getDefaultUserAgent" && it.parameterTypes.size == 1
            }.forEach { m -> hookMethod(m) { _ -> ua } }
        }
        runCatching {
            ws.declaredMethods.filter {
                it.name == "getUserAgentString" && it.parameterTypes.isEmpty()
            }.forEach { m -> hookMethod(m) { _ -> ua } }
        }
        runCatching {
            ws.declaredMethods.filter {
                it.name == "setUserAgentString" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0] == String::class.java
            }.forEach { m ->
                hookMethod(m) { chain ->
                    
                    if (chain.getArg(0) as? String == ua) chain.proceed() else null
                }
            }
        }

        
        runCatching {
            val wv = cls("android.webkit.WebView")
                ?: runCatching { Class.forName("android.webkit.WebView") }.getOrNull()
                ?: return@runCatching
            val apply: (Any?) -> Unit = { self ->
                if (self != null) {
                    runCatching {
                        val g = self.javaClass.getMethod("getSettings")
                        val s = g.invoke(self)
                        val su = s?.javaClass?.getMethod("setUserAgentString", String::class.java)
                        su?.invoke(s, ua)
                    }
                }
            }
            wv.declaredConstructors.forEach { c ->
                hookCtor(c) { chain ->
                    val r = chain.proceed()
                    runCatching { apply(r) }
                    r
                }
            }
        }

        logInfo("user-agent hooked -> $ua")
    }
}
