package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule











internal class BatteryExtraSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    fun install() {
        val cfg = snapshot()
        if (!cfg.enableBuild || !cfg.batexEnable) {
            logInfo("battery extra spoof skipped (build=${cfg.enableBuild} on=${cfg.batexEnable})")
            return
        }
        var n = 0
        n += hookBatteryManager()
        n += hookStickyIntent()
        n += hookPowerProfile()
        pushSysfs()
        logInfo("battery extra spoof installed (hooks=$n)")
    }

    
    private fun statusCode(): Int = when (snapshot().batStatus) {
        "charging" -> 2      
        "discharging" -> 3   
        "not_charging" -> 4  
        "full" -> 5          
        else -> 3
    }

    




    private fun currentUa(): Int {
        val c = snapshot()
        val ma = kotlin.math.abs(c.batCurrentMa).coerceIn(0, 20000)
        val charging = c.batStatus == "charging"
        return (if (charging) ma else -ma) * 1000
    }

    private fun hookBatteryManager(): Int {
        val bm = loadClassAnywhere("android.os.BatteryManager") ?: return 0
        val idNow = staticInt(bm, "BATTERY_PROPERTY_CURRENT_NOW")
        val idAvg = staticInt(bm, "BATTERY_PROPERTY_CURRENT_AVERAGE")
        val idCounter = staticInt(bm, "BATTERY_PROPERTY_CHARGE_COUNTER")
        var n = 0
        bm.declaredMethods.filter {
            it.name == "getIntProperty" && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == Integer.TYPE
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val id = chain.getArg(0) as? Int ?: return@hookMethod chain.proceed()
                    when (id) {
                        idNow -> currentUa()
                        idAvg -> currentUa()
                        idCounter -> {
                            val c = snapshot()
                            
                            c.batDesignMah.coerceIn(100, 20000) * 1000
                        }
                        else -> chain.proceed()
                    }
                }) n++
        }
        bm.declaredMethods.filter { it.name == "isCharging" && it.parameterTypes.isEmpty() }
            .forEach { m ->
                if (hookMethod(m) { _ -> snapshot().batStatus == "charging" }) n++
            }
        
        
        bm.declaredMethods.filter {
            it.name == "getLongProperty" && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == Integer.TYPE
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val id = chain.getArg(0) as? Int ?: return@hookMethod chain.proceed()
                    when (id) {
                        idNow -> currentUa().toLong()
                        idAvg -> currentUa().toLong()
                        idCounter -> (snapshot().batDesignMah.coerceIn(100, 20000) * 1000).toLong()
                        else -> chain.proceed()
                    }
                }) n++
        }
        return n
    }

    




    private fun hookStickyIntent(): Int {
        val cw = loadClassAnywhere("android.content.ContextWrapper") ?: return 0
        var n = 0
        cw.declaredMethods.filter { m ->
            m.name == "registerReceiver" && m.parameterTypes.size >= 2 &&
                    m.parameterTypes[1].name == "android.content.IntentFilter"
        }.forEach { m ->
            if (hookMethod(m) { chain ->
                    val r = chain.proceed()
                    runCatching {
                        val intent = r as? android.content.Intent ?: return@runCatching
                        if (intent.action != android.content.Intent.ACTION_BATTERY_CHANGED) {
                            return@runCatching
                        }
                        val c = snapshot()
                        intent.putExtra("status", statusCode())
                        intent.putExtra("plugged", if (c.batStatus == "charging") 2 else 0)
                        intent.putExtra("voltage", c.batVoltageMv.coerceIn(2000, 5000))
                        intent.putExtra("health", 2)   
                        if (c.exBatteryEnable) {
                            intent.putExtra("level", c.exBattery.coerceIn(0, 100))
                            intent.putExtra("scale", 100)
                        }
                        if (c.exTempEnable) {
                            intent.putExtra("temperature", c.exTemp.coerceIn(0, 120))
                        }
                    }
                    r
                }) n++
        }
        return n
    }

    
    private fun hookPowerProfile(): Int {
        val pp = loadClassAnywhere("com.android.internal.os.PowerProfile") ?: return 0
        var n = 0
        pp.declaredMethods.filter { it.name == "getBatteryCapacity" && it.parameterTypes.isEmpty() }
            .forEach { m ->
                if (hookMethod(m) { _ ->
                        snapshot().batDesignMah.coerceIn(100, 20000).toDouble()
                    }) n++
            }
        return n
    }

    
    private fun pushSysfs() {
        sysfsFiles(snapshot()).forEach { (p, v) -> FakeFiles.setExtra(p, v) }
    }

    companion object {
        





        fun sysfsFiles(c: XpState.Snapshot): Map<String, String> {
            val base = "/sys/class/power_supply/battery"
            val out = LinkedHashMap<String, String>()
            out["$base/status"] = when (c.batStatus) {
                "charging" -> "Charging"
                "full" -> "Full"
                "not_charging" -> "Not charging"
                else -> "Discharging"
            }
            val ma = kotlin.math.abs(c.batCurrentMa).coerceIn(0, 20000)
            val signed = if (c.batStatus == "charging") ma else -ma
            out["$base/current_now"] = (signed * 1000).toString()
            out["$base/current_avg"] = (signed * 1000).toString()
            out["$base/current_max"] = (kotlin.math.abs(ma) * 1000).toString()
            out["$base/voltage_now"] =
                (c.batVoltageMv.coerceIn(2000, 5000) * 1000).toString()
            out["$base/voltage_max"] =
                (c.batVoltageMv.coerceIn(2000, 5000) * 1000 + 300000).toString()
            val designUah = c.batDesignMah.coerceIn(100, 20000) * 1000
            out["$base/charge_full_design"] = designUah.toString()
            out["$base/charge_full"] = designUah.toString()
            out["$base/charge_counter"] = designUah.toString()
            out["$base/energy_full_design"] = (designUah * 3850 / 1000).toString()
            if (c.exBatteryEnable) {
                out["$base/capacity"] = c.exBattery.coerceIn(0, 100).toString()
            }
            if (c.exTempEnable) {
                out["$base/temp"] = (c.exTemp.coerceIn(0, 120) * 10).toString()
            }
            out["$base/health"] = "Good"
            out["$base/technology"] = "Li-ion"
            return out
        }
    }

    private fun staticInt(clazz: Class<*>, name: String): Int? =
        runCatching {
            val f = clazz.getDeclaredField(name)
            f.isAccessible = true
            f.getInt(null)
        }.getOrNull()
}
