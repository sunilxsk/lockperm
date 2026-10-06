package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule











internal class SimExtraSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg = snapshot()
        if (!cfg.enableBuild) {
            logInfo("sim extra spoof skipped (master off)")
            return
        }
        var n = 0
        if (cfg.simEsim || cfg.simEsimActive) n += hookEsim(cfg)
        if (cfg.simData) n += hookData()
        if (cfg.simRoamEnable) n += hookRoaming()
        if (n > 0) logInfo("sim extra spoof installed (hooks=$n)")
    }

    

    private fun hookEsim(cfg: XpState.Snapshot): Int {
        var n = 0
        if (cfg.simEsim) {
            val em = loadClassAnywhere("android.telephony.euicc.EuiccManager")
            em?.declaredMethods?.filter { it.name == "isEnabled" && it.parameterTypes.isEmpty() }
                ?.forEach { m -> if (hookMethod(m) { _ -> true }) n++ }
        }
        if (cfg.simEsimActive) {
            
            
            
            val sm = loadClassAnywhere("android.telephony.SubscriptionManager")
            sm?.declaredMethods?.filter {
                (it.name == "getActiveSubscriptionInfoCount" ||
                        it.name == "getActiveSubscriptionInfoCountMax") &&
                        it.parameterTypes.isEmpty()
            }?.forEach { m ->
                
                if (hookMethod(m) { chain ->
                        val r = chain.proceed() as? Int ?: return@hookMethod chain.proceed()
                        (r + 1).coerceAtMost(4)
                    }) n++
            }
        }
        return n
    }

    

    private fun hookData(): Int {
        val tm = loadClassAnywhere("android.telephony.TelephonyManager") ?: return 0
        var n = 0
        
        
        tm.declaredMethods.filter { m ->
            m.name == "getDataState" || m.name == "isDataEnabled" ||
                    m.name == "isDataConnectionAllowed"
        }.forEach { m ->
            
            
            if (hookMethod(m) { _ ->
                    if (m.returnType == Integer.TYPE) 2
                    else if (m.returnType == java.lang.Boolean.TYPE) true
                    else 2
                }) n++
        }
        return n
    }

    

    private fun hookRoaming(): Int {
        var n = 0
        
        val tm = loadClassAnywhere("android.telephony.TelephonyManager")
        tm?.declaredMethods?.filter { it.name == "isNetworkRoaming" }
            ?.forEach { m ->
                if (hookMethod(m) { _ ->
                        if (m.returnType == java.lang.Boolean.TYPE) snapshot().simRoam
                        else java.lang.Boolean.valueOf(snapshot().simRoam)
                    }) n++
            }
        
        
        val ss = loadClassAnywhere("android.telephony.ServiceState")
        ss?.declaredMethods?.filter {
            it.name == "getRoaming" || it.name == "getVoiceRoamingType" ||
                    it.name == "getDataRoamingType" ||
                    it.name == "getDataRoamingFromRegistration"
        }?.forEach { m ->
            if (hookMethod(m) { _ ->
                    if (m.returnType == java.lang.Boolean.TYPE) snapshot().simRoam
                    else if (snapshot().simRoam) 2 else 0
                }) n++
        }
        
        val si = loadClassAnywhere("android.telephony.SubscriptionInfo")
        si?.declaredMethods?.filter { it.name == "getDataRoaming" }?.forEach { m ->
            if (hookMethod(m) { _ ->
                    if (m.returnType == java.lang.Boolean.TYPE) snapshot().simRoam
                    else if (snapshot().simRoam) 2 else 0
                }) n++
        }
        return n
    }
}
