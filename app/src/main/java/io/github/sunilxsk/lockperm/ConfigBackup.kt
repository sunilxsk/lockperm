package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject







object ConfigBackup {

    private const val KEY_META = "meta"
    private const val KEY_DATA = "data"
    private const val FORMAT = 1

    fun export(prefs: SharedPreferences, note: String = ""): String {
        val raw = runCatching { prefs.all }.getOrNull() ?: emptyMap()
        val data = JSONObject()
        raw.forEach { (k, v) ->
            if (k.isBlank()) return@forEach
            when (v) {
                null -> data.put(k, JSONObject.NULL)
                is Boolean -> data.put(k, v)
                is Int -> data.put(k, v)
                is Long -> data.put(k, v)
                is Float -> data.put(k, v.toDouble())
                is String -> data.put(k, v)
                is Set<*> -> {
                    val arr = JSONArray()
                    v.filterIsInstance<String>().forEach { arr.put(it) }
                    data.put(k, arr)
                }

                else -> data.put(k, v.toString())
            }
        }
        val meta = JSONObject()
        meta.put("format", FORMAT)
        meta.put("time", System.currentTimeMillis())
        meta.put("count", raw.size)
        meta.put("note", note)
        val root = JSONObject()
        root.put(KEY_META, meta)
        root.put(KEY_DATA, data)
        return root.toString(2)
    }

    
    fun import(prefs: SharedPreferences, text: String): Int {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return -1
        val data = if (root.has(KEY_DATA)) root.optJSONObject(KEY_DATA) else root
            ?: return -1
        val ed = prefs.edit()
        ed.clear()
        var n = 0
        val keys = data.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = data.opt(k)
            when {
                v == null || v === JSONObject.NULL -> Unit
                v is Boolean -> ed.putBoolean(k, v)
                v is Int -> ed.putInt(k, v)
                v is Long -> ed.putLong(k, v)
                v is Double -> ed.putFloat(k, v.toFloat())
                v is String -> ed.putString(k, v)
                v is JSONArray -> {
                    
                    
                    val parts = ArrayList<String>()
                    for (i in 0 until v.length()) {
                        val s = v.optString(i)
                        if (!s.isNullOrEmpty()) parts.add(s)
                    }
                    ed.putString(k, parts.joinToString(","))
                }

                else -> ed.putString(k, v.toString())
            }
            n++
        }
        return if (runCatching { ed.commit() }.getOrDefault(false)) n else -1
    }

    fun summary(text: String): String {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return "不是有效的备份文件"
        val meta = root.optJSONObject(KEY_META)
        val data = if (root.has(KEY_DATA)) root.optJSONObject(KEY_DATA) else root
        val count = data?.length() ?: 0
        val time = meta?.optLong("time", 0L) ?: 0L
        return if (time > 0L) {
            "备份于 ${formatTime(time)}，共 $count 项配置"
        } else {
            "共 $count 项配置"
        }
    }

    private fun formatTime(ms: Long): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return runCatching { f.format(java.util.Date(ms)) }.getOrDefault("$ms")
    }
}
