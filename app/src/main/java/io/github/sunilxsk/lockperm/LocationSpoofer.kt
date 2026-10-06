package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule























internal class LocationSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private fun on(): Boolean = snapshot().locEnable

    
    
    
    private val state = SpoofState()

    private class SpoofState {
        var ready = false
        var lat = 0.0
        var lon = 0.0
        var alt = 0.0
        var acc = 10f
        var speed = 0f
        var bearing = 0f
        var vertAcc = 10f
        var speedAcc = 1f
        var bearingAcc = 10f
        var mslAlt = 0.0
        var mslAcc = 10f
    }

    private fun ensureState() {
        if (state.ready) return
        val c = snapshot()
        val loc = FakeData.locationFor("fused", c)
        state.lat = loc.latitude
        state.lon = loc.longitude
        state.alt = loc.altitude
        state.acc = loc.accuracy
        state.speed = loc.speed
        state.bearing = loc.bearing
        state.vertAcc = FakeData.intPublic(5, 30).toFloat()
        state.speedAcc = FakeData.intPublic(1, 5).toFloat()
        state.bearingAcc = FakeData.intPublic(5, 45).toFloat()
        state.mslAlt = loc.altitude
        state.mslAcc = FakeData.intPublic(5, 30).toFloat()
        state.ready = true
    }

    fun install() {
        if (!on()) return
        ensureState()
        hookLocationFields()
        hookLocationManager()
        hookGnss()
        hookFused()
        hookTelephony()
        hookWifiScan()
        hookStore()
        logInfo("location spoofer installed")
    }

    
    
    
    private fun hookLocationFields() {
        if (!snapshot().locFields) return
        val c = cls("android.location.Location") ?: return

        
        
        
        
        
        
        
        val doubles = mapOf<String, () -> Any>(
            "getLatitude" to { state.lat },
            "getLongitude" to { state.lon },
            "getAltitude" to { state.alt },
            "getMslAltitudeMeters" to { state.mslAlt },
        )
        val floats = mapOf<String, () -> Any>(
            "getAccuracy" to { state.acc },
            "getSpeed" to { state.speed },
            "getBearing" to { state.bearing },
            "getVerticalAccuracyMeters" to { state.vertAcc },
            "getSpeedAccuracyMetersPerSecond" to { state.speedAcc },
            "getBearingAccuracyDegrees" to { state.bearingAcc },
            "getMslAltitudeAccuracyMeters" to { state.mslAcc },
        )

        
        
        c.declaredMethods.forEach { m ->
            val d = doubles[m.name]
            val f = floats[m.name]
            if (d == null && f == null) return@forEach
            
            
            val wantDouble = m.returnType == Double::class.javaPrimitiveType
            val wantFloat = m.returnType == Float::class.javaPrimitiveType
            if (d != null && !wantDouble) return@forEach
            if (f != null && !wantFloat) return@forEach
            hookMethod(m) { chain ->
                if (!on() || !snapshot().locFields) return@hookMethod chain.proceed()
                when {
                    d != null -> d()
                    else -> f!!()
                }
            }
        }
        logInfo("location field getters hooked")
    }

    
    
    
    private fun hookLocationManager() {
        val cfg = snapshot()
        if (!cfg.locGps && !cfg.locNet) return
        val lm = cls("android.location.LocationManager") ?: return

        
        hookAll(lm, { it.name == "getLastKnownLocation" }) { chain ->
            if (!on()) return@hookAll chain.proceed()
            val provider = chain.args.filterIsInstance<String>().firstOrNull() ?: "fused"
            val real = runCatching { chain.proceed() }.getOrNull() as? android.location.Location
            real ?: FakeData.locationFor(provider, snapshot())
        }

        
        hookAll(lm, { it.name == "getCurrentLocation" }) { chain ->
            if (!on()) return@hookAll chain.proceed()
            logWarn("blocked LocationManager.getCurrentLocation")
            null
        }

        hookAll(lm, { it.name == "getBestProvider" }) { chain ->
            if (!on()) return@hookAll chain.proceed()
            val real = runCatching { chain.proceed() }.getOrNull() as? String
            real ?: "gps"
        }

        hookAll(lm, { it.name == "isProviderEnabled" || it.name == "isLocationEnabled" }) { chain ->
            if (!on()) return@hookAll chain.proceed()
            true
        }

        
        
        
        logInfo("location manager spoofed")
    }

    
    
    
    
    
    private fun hookGnss() {
        val lm = cls("android.location.LocationManager") ?: return
        val names = setOf(
            "registerGnssStatusCallback",
            "unregisterGnssStatusCallback",
            "addGnssMeasurementsListener",
            "addGnssNavigationMessageListener",
            "addGnssAntennaInfoListener",
            "addGnssBatchingCallback",
            "registerGnssNmeaCallback",
            "registerAntennaInfoListener",
        )
        hookAll(lm, { it.name in names }) { chain ->
            if (!on()) return@hookAll chain.proceed()
            logWarn("blocked gnss ${runCatching { (chain.executable as? java.lang.reflect.Method)?.name }.getOrNull()}")
            null
        }
    }

    
    
    
    private fun hookFused() {
        if (!snapshot().locFused) return
        val names = listOf(
            "com.google.android.gms.location.FusedLocationProviderClient",
            "com.google.android.gms.location.LocationServices",
            "com.google.android.gms.location.SettingsClient",
        )
        for (n in names) {
            val c = runCatching { Class.forName(n, false, classLoader) }.getOrNull()
                ?: runCatching { Class.forName(n) }.getOrNull() ?: continue
            c.declaredMethods.filter { m ->
                m.name == "getLastLocation" || m.name == "getCurrentLocation" ||
                    m.name == "requestLocationUpdates"
            }.forEach { m ->
                hookMethod(m) { chain ->
                    if (!on()) return@hookMethod chain.proceed()
                    when (m.name) {
                        "requestLocationUpdates" -> {
                            logWarn("blocked gms requestLocationUpdates")
                            null
                        }
                        else -> {
                            
                            
                            
                            val task = fakeTask()
                            if (task != null) {
                                logWarn("gms ${m.name} -> 伪造位置")
                                task
                            } else {
                                chain.proceed()
                            }
                        }
                    }
                }
            }
            logInfo("fused location spoofed: $n")
        }
    }

    
    private fun fakeTask(): Any? {
        return runCatching {
            val tasks = Class.forName("com.google.android.gms.tasks.Tasks")
            val m = tasks.getMethod("forResult", Object::class.java)
            m.invoke(null, FakeData.locationFor("fused", snapshot()))
        }.getOrNull()
    }

    
    
    
    private fun hookTelephony() {
        val cfg = snapshot()
        if (!cfg.locTelephony && !cfg.locCell) return
        val tm = cls("android.telephony.TelephonyManager") ?: return

        hookAll(tm, { it.name == "getCellLocation" }) { chain ->
            if (!on() || !snapshot().locCell) return@hookAll chain.proceed()
            buildCellLocation()
        }

        
        
        hookAll(tm, { it.name == "getAllCellInfo" || it.name == "getNeighboringCellInfo" }) { chain ->
            if (!on() || !snapshot().locCell) return@hookAll chain.proceed()
            logWarn("cleared cell info")
            java.util.ArrayList<Any>()
        }

        hookAll(tm, { it.name == "requestCellInfoUpdate" }) { chain ->
            if (!on() || !snapshot().locCell) return@hookAll chain.proceed()
            logWarn("blocked requestCellInfoUpdate")
            null
        }

        hookAll(tm, { it.name == "getNetworkOperator" || it.name == "getSimOperator" }) { chain ->
            if (!on() || !snapshot().locTelephony) return@hookAll chain.proceed()
            val c = snapshot()
            if (c.cellMcc.isNotBlank() && c.cellMnc.isNotBlank()) {
                "${c.cellMcc}${c.cellMnc}"
            } else {
                chain.proceed()
            }
        }
        logInfo("telephony/cell spoofed")
    }

    
    private fun buildCellLocation(): Any? {
        val c = snapshot()
        return runCatching {
            val clazz = Class.forName("android.telephony.gsm.GsmCellLocation")
            val o = clazz.getDeclaredConstructor().newInstance()
            val lac = c.cellLac.toIntOrNull() ?: (1000..60000).random()
            val cid = c.cellCid.toIntOrNull() ?: (1000..600000).random()
            runCatching {
                clazz.getMethod(
                    "setLacAndCid",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                ).invoke(o, lac, cid)
            }
            o
        }.getOrNull()
    }

    
    
    
    
    private fun hookWifiScan() {
        val c = snapshot()
        if (!c.locNet) return

        
        
        
        
        if (wifiScanHandledByWifiFake(c)) {
            logInfo("wifi scan: 交给防护功能页的 WiFi 伪装处理，此处不装钩子")
            return
        }

        val wm = cls("android.net.wifi.WifiManager") ?: return
        hookAll(wm, { it.name == "getScanResults" }) { chain ->
            val cur = snapshot()
            if (!on() || !cur.locNet) return@hookAll chain.proceed()
            if (wifiScanHandledByWifiFake(cur)) return@hookAll chain.proceed()
            logWarn("cleared wifi scan results（AP 定位）")
            java.util.ArrayList<Any>()
        }
    }

    





    private fun wifiScanHandledByWifiFake(c: XpState.Snapshot): Boolean =
        c.wifiFakeEnable && c.wifiFakeScan && c.wifiFakeList.isNotEmpty()

    
    
    
    private fun hookStore() {
        if (!snapshot().locStore) return
        val cr = cls("android.content.ContentResolver") ?: return
        hookAll(cr, { it.name == "query" }) { chain ->
            if (!on() || !snapshot().locStore) return@hookAll chain.proceed()
            val uri = chain.args.filterIsInstance<android.net.Uri>().firstOrNull()
            val a = uri?.authority ?: return@hookAll chain.proceed()
            if (a == "sms" || a == "mms" || a == "call_log" || a == "telephony" ||
                a.endsWith(".telephony") || a.endsWith(".sms") || a.endsWith(".call_log")
            ) {
                logWarn("blocked location-ish provider: $a")
                return@hookAll null
            }
            chain.proceed()
        }
        logInfo("telephony/sms store blocked")
    }
}
