package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale













data class DeviceTemplate(
    var id: String = newId(),
    var name: String = "未命名模板",
    var note: String = "",
    var updated: Long = System.currentTimeMillis(),
    
    val data: LinkedHashMap<String, String> = LinkedHashMap(),
) {
    
    fun filledCount(): Int = data.values.count { it.isNotBlank() }

    fun get(key: String): String = data[key].orEmpty()

    fun set(key: String, value: String) {
        if (value.isBlank()) data.remove(key) else data[key] = value
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("note", note)
        put("updated", updated)
        val d = JSONObject()
        data.forEach { (k, v) -> if (k.isNotBlank()) d.put(k, v) }
        put("data", d)
    }

    companion object {
        fun newId(): String =
            "t${System.currentTimeMillis().toString(36)}${(0..999).random()}"

        fun fromJson(o: JSONObject): DeviceTemplate {
            val data = LinkedHashMap<String, String>()
            val d = o.optJSONObject("data")
            if (d != null) {
                val it = d.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val v = d.optString(k, "")
                    if (k.isNotBlank()) data[k] = v
                }
            }
            return DeviceTemplate(
                id = o.optString("id").ifBlank { newId() },
                name = o.optString("name").ifBlank { "导入的模板" },
                note = o.optString("note", ""),
                updated = o.optLong("updated", System.currentTimeMillis()),
                data = data,
            )
        }
    }
}









data class TplField(
    val key: String,
    val label: String,
    val hint: String = "",
    
    val multiline: Boolean = false,
)

data class TplGroup(val title: String, val fields: List<TplField>)

object DeviceTemplateSpec {

    val GROUPS: List<TplGroup> = listOf(
        TplGroup(
            "Build 字段",
            XpConfig.BUILD_FIELDS.map { TplField(it.key, it.label) }
        ),
        TplGroup(
            "标识 ID",
            listOf(
                TplField(XpConfig.KEY_ANDROID_ID, "Android ID", "16 位十六进制"),
                TplField(XpConfig.KEY_GSF_ID, "GSF ID", "Google 服务框架 ID"),
                TplField(XpConfig.KEY_ADS_ID, "广告 ID", "UUID"),
                TplField(XpConfig.KEY_APPSET_ID, "App Set ID", "UUID"),
                TplField(XpConfig.KEY_DRM_ID, "DRM 设备 ID", "64 位十六进制"),
                TplField(XpConfig.KEY_OAID, "OAID", "匿名设备标识符"),
                TplField(XpConfig.KEY_FAKE_HW_SERIAL, "硬件序列号"),
                TplField(XpConfig.KEY_FAKE_FB_FID, "Firebase 安装 ID"),
                TplField(XpConfig.KEY_FAKE_FB_IID, "Firebase 实例 ID"),
                TplField(XpConfig.KEY_DEVICE_NAME, "设备名称", "设置里的「设备名」"),
            )
        ),
        TplGroup(
            "内核 / CPU",
            listOf(
                TplField(XpConfig.KEY_FAKE_KERNEL, "内核版本", "如 6.6.28-android15-8-xxx"),
                TplField(XpConfig.KEY_FAKE_ARCH, "内核架构", "aarch64 / x86_64"),
                TplField(XpConfig.KEY_FAKE_CPUINFO_HW, "cpuinfo Hardware", "SoC 名"),
                TplField(XpConfig.KEY_FAKE_PLATFORM, "ro.board.platform"),
                TplField(XpConfig.KEY_FAKE_CPU_CORES, "CPU 核心数"),
                TplField(XpConfig.KEY_FAKE_CPU_MODE, "CPU 模式", "preset / custom"),
                TplField(XpConfig.KEY_FAKE_CPU_PRESET, "CPU 预设编号", "0~4"),
                TplField(XpConfig.KEY_FAKE_CPU_CUSTOM, "cpuinfo 全文", "自定义模式时用", multiline = true),
            )
        ),
        TplGroup(
            "GPU",
            listOf(
                TplField(XpConfig.KEY_FAKE_GPU, "GPU 渲染器名", "如 Adreno (TM) 750"),
                TplField(XpConfig.KEY_FAKE_GPU_VENDOR, "GPU 厂商"),
                TplField(XpConfig.KEY_FAKE_GPU_GL_VERSION, "OpenGL ES 版本"),
                TplField(XpConfig.KEY_FAKE_GPU_GLSL, "GLSL 版本"),
                TplField(XpConfig.KEY_FAKE_GPU_VK_API, "Vulkan API 版本", "如 1.1.0"),
                TplField(XpConfig.KEY_FAKE_GPU_DRIVER, "驱动版本（十六进制）"),
                TplField(XpConfig.KEY_FAKE_GPU_VENDOR_ID, "厂商 ID（十六进制）"),
                TplField(XpConfig.KEY_FAKE_GPU_DEVICE_ID, "设备 ID（十六进制）"),
                TplField(XpConfig.KEY_FAKE_GPU_MEMORY_MB, "显存（MB）"),
                TplField(XpConfig.KEY_FAKE_GPU_MAX_TEX, "最大图像尺寸"),
                TplField(XpConfig.KEY_FAKE_GPU_MAX_CUBE, "最大 Cube 尺寸"),
                TplField(XpConfig.KEY_FAKE_GPU_MAX_LAYERS, "最大图像层数"),
                TplField(XpConfig.KEY_FAKE_GPU_PUSH, "Max Push Constants"),
            )
        ),
        TplGroup(
            "内存 / 温度 / 电量",
            listOf(
                TplField(XpConfig.KEY_FAKE_MEM_MB, "运行内存（MB）", "如 8192"),
                TplField(XpConfig.KEY_FAKE_TEMP, "设备温度（摄氏度）"),
                TplField(XpConfig.KEY_FAKE_BATTERY, "电量百分比"),
            )
        ),
        TplGroup(
            "SIM / 电话",
            listOf(
                TplField(XpConfig.KEY_FAKE_SIM_OPERATOR, "SIM 运营商代码 MCC+MNC"),
                TplField(XpConfig.KEY_FAKE_SIM_OPERATOR_NAME, "SIM 运营商名称"),
                TplField(XpConfig.KEY_FAKE_SIM_COUNTRY, "SIM 国家码"),
                TplField(XpConfig.KEY_FAKE_SIM_SERIAL, "ICCID"),
                TplField(XpConfig.KEY_FAKE_SIM_SUBSCRIBER, "IMSI"),
                TplField(XpConfig.KEY_FAKE_PHONE_NUMBER, "本机手机号"),
                TplField(XpConfig.KEY_FAKE_IMEI, "IMEI"),
                TplField(XpConfig.KEY_FAKE_MEID, "MEID"),
                TplField(XpConfig.KEY_FAKE_ICCID, "ICCID（独立字段）"),
                TplField(XpConfig.KEY_FAKE_CARRIER, "网络运营商名称"),
            )
        ),
        TplGroup(
            "网络",
            listOf(
                TplField(XpConfig.KEY_FAKE_WIFI_SSID, "WiFi 名称 SSID"),
                TplField(XpConfig.KEY_FAKE_WIFI_BSSID, "WiFi BSSID"),
                TplField(XpConfig.KEY_FAKE_WIFI_MAC, "WiFi MAC"),
                TplField(XpConfig.KEY_FAKE_BT_MAC, "蓝牙 MAC"),
            )
        ),
        TplGroup(
            "系统",
            listOf(
                TplField(XpConfig.KEY_FAKE_TIMEZONE, "时区", "Asia/Shanghai"),
                TplField(XpConfig.KEY_FAKE_LOCALE, "语言 / 地区", "zh_CN"),
                TplField(XpConfig.KEY_FAKE_SDK_INT, "Android SDK 版本", "0 表示不改"),
                TplField(XpConfig.KEY_FAKE_UPTIME_HOURS, "已运行时间（小时）"),
            )
        ),
    )

    
    val ALL_FIELDS: List<TplField> = GROUPS.flatMap { it.fields }

    val ALL_KEYS: Set<String> = ALL_FIELDS.map { it.key }.toSet()

    





    fun apply(cfg: XpConfigState, tpl: DeviceTemplate) {
        ALL_KEYS.forEach { key ->
            val raw = tpl.data[key] ?: return@forEach
            cfg.put(key, coerce(key, raw))
        }
        
        cfg.put(XpConfig.KEY_ENABLE_BUILD, true)
        val cpuOn = tpl.get(XpConfig.KEY_FAKE_CPU_CUSTOM).isNotBlank() ||
            tpl.get(XpConfig.KEY_FAKE_CPUINFO_HW).isNotBlank() ||
            tpl.get(XpConfig.KEY_FAKE_CPU_PRESET).isNotBlank()
        if (cpuOn) cfg.put(XpConfig.KEY_FAKE_CPU_ENABLE, true)
        if (tpl.get(XpConfig.KEY_FAKE_TEMP).isNotBlank()) {
            cfg.put(XpConfig.KEY_FAKE_TEMP_ENABLE, true)
        }
        if (tpl.get(XpConfig.KEY_FAKE_BATTERY).isNotBlank()) {
            cfg.put(XpConfig.KEY_FAKE_BATTERY_ENABLE, true)
        }
        if (tpl.get(XpConfig.KEY_FAKE_MEM_MB).isNotBlank()) {
            cfg.put(XpConfig.KEY_FAKE_MEM_ENABLE, true)
        }
        if (tpl.get(XpConfig.KEY_ANDROID_ID).isNotBlank()) {
            cfg.put(XpConfig.KEY_ENABLE_ANDROID_ID, true)
        }
    }

    
    fun coerce(key: String, raw: String): Any? = when (val d = XpConfig.DEFAULTS[key]) {
        is Boolean -> raw == "1" || raw.equals("true", ignoreCase = true)
        is Int -> raw.trim().toIntOrNull() ?: d
        is Long -> raw.trim().toLongOrNull() ?: d
        else -> raw
    }
}





object DeviceTemplateStore {

    private const val V = 1

    fun load(prefs: SharedPreferences?): MutableList<DeviceTemplate> {
        val raw = runCatching { prefs?.getString(XpConfig.KEY_DEVICE_TEMPLATES, null) }
            .getOrNull().orEmpty()
        if (raw.isBlank()) return ArrayList()
        val list = ArrayList<DeviceTemplate>()
        runCatching {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("list") ?: return@runCatching
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(DeviceTemplate.fromJson(o))
            }
        }
        return list
    }

    fun save(prefs: SharedPreferences?, list: List<DeviceTemplate>) {
        val p = prefs ?: return
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val root = JSONObject().apply {
            put("v", V)
            put("list", arr)
        }
        runCatching { p.edit().putString(XpConfig.KEY_DEVICE_TEMPLATES, root.toString()).commit() }
    }

    fun upsert(prefs: SharedPreferences?, tpl: DeviceTemplate) {
        val list = load(prefs)
        val idx = list.indexOfFirst { it.id == tpl.id }
        tpl.updated = System.currentTimeMillis()
        if (idx >= 0) list[idx] = tpl else list.add(0, tpl)
        save(prefs, list)
    }

    fun delete(prefs: SharedPreferences?, id: String) {
        val list = load(prefs)
        if (list.removeAll { it.id == id }) save(prefs, list)
    }

    
    fun encode(tpl: DeviceTemplate): String = JSONObject().apply {
        put("type", "lockperm.device.template")
        put("v", V)
        put("template", tpl.toJson())
    }.toString(2)

    





    fun encodeAll(list: List<DeviceTemplate>): String = JSONObject().apply {
        put("type", "lockperm.device.templates")
        put("v", V)
        put("count", list.size)
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        put("templates", arr)
    }.toString(2)

    
    fun decode(text: String): DeviceTemplate? {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val t = root.optJSONObject("template") ?: root
        if (t.optJSONObject("data") == null && !t.has("name")) return null
        return runCatching { DeviceTemplate.fromJson(t) }.getOrNull()
    }

    



    fun decodeAny(text: String): List<DeviceTemplate> {
        val root = runCatching { JSONObject(text.trim()) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("templates")
        if (arr != null) {
            val out = ArrayList<DeviceTemplate>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                runCatching { out.add(DeviceTemplate.fromJson(o)) }
            }
            return out
        }
        val one = decode(text)
        return if (one != null) listOf(one) else emptyList()
    }

    fun summary(text: String): String {
        val t = decode(text) ?: return "不是有效的设备模板"
        return "「${t.name}」共 ${t.filledCount()} 项" +
            if (t.note.isNotBlank()) "，备注：${t.note}" else ""
    }

    
    fun summaryAny(text: String): String {
        val list = decodeAny(text)
        return when {
            list.isEmpty() -> "不是有效的设备模板"
            list.size == 1 -> "「${list[0].name}」共 ${list[0].filledCount()} 项"
            else -> "${list.size} 份模板：" + list.take(3).joinToString("、") { it.name } +
                if (list.size > 3) " 等" else ""
        }
    }


}


internal fun tplTime(ms: Long): String {
    if (ms <= 0L) return ""
    val f = java.text.SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return runCatching { f.format(java.util.Date(ms)) }.getOrDefault("")
}
