package io.github.sunilxsk.lockperm

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL











internal object UpdateChecker {

    const val API = "https://api.github.com/repos/sunilxsk/lockperm/releases/latest"
    private const val REPO_PAGE = "https://github.com/sunilxsk/lockperm/releases/latest"

    data class Asset(
        val name: String,
        val url: String,
        val size: Long,
    )

    data class Result(
        val tagName: String,
        val name: String,
        val versionCode: Int,
        val versionName: String,
        val publishedAt: String,
        val notes: String,
        val assets: List<Asset>,
        
        val currentCode: Int,
        val currentName: String,
        val htmlUrl: String = REPO_PAGE,
    ) {
        val hasUpdate: Boolean get() = versionCode > currentCode
        val isNewerOrEqual: Boolean get() = !hasUpdate
    }

    




    fun fetch(currentCode: Int, currentName: String): Result {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            
            setRequestProperty("User-Agent", "LockPerm/$currentName")
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            parse(text, currentCode, currentName)
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    
    fun parse(text: String, currentCode: Int, currentName: String): Result {
        val o = runCatching { JSONObject(text) }.getOrNull()
            ?: throw IllegalStateException("返回内容不是 JSON")

        val tag = o.optString("tag_name").orEmpty()
        val (vCode, vName) = splitTag(tag)
        val assets = ArrayList<Asset>()
        val arr = o.optJSONArray("assets")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val a = arr.optJSONObject(i) ?: continue
                val url = a.optString("browser_download_url")
                val nm = a.optString("name")
                if (nm.isBlank()) continue
                assets.add(Asset(nm, url, a.optLong("size", 0L)))
            }
        }
        return Result(
            tagName = tag,
            name = o.optString("name").ifBlank { vName },
            versionCode = vCode,
            versionName = vName,
            publishedAt = o.optString("published_at").orEmpty(),
            notes = o.optString("body").orEmpty().trim(),
            assets = assets,
            currentCode = currentCode,
            currentName = currentName,
        )
    }

    
    private fun splitTag(tag: String): Pair<Int, String> {
        val t = tag.trim().removePrefix("v")
        val i = t.indexOf('-')
        if (i <= 0) return 0 to t
        val code = t.substring(0, i).toIntOrNull() ?: 0
        return code to t.substring(i + 1)
    }

    fun sizeText(bytes: Long): String = when {
        bytes <= 0L -> ""
        bytes < 1024 * 1024 -> "${(bytes / 1024).coerceAtLeast(1)} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576.0)
    }

    
    fun dateText(iso: String): String {
        if (iso.isBlank()) return ""
        val v = iso.replace("Z", "+0000")
        val fmts = listOf("yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        for (f in fmts) {
            runCatching {
                val d = java.text.SimpleDateFormat(f, java.util.Locale.US).parse(v)
                if (d != null) {
                    return java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm", java.util.Locale.getDefault()
                    ).format(d)
                }
            }
        }
        return iso.take(16).replace("T", " ")
    }
}
