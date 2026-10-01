package io.github.sunilxsk.lockperm

import android.database.Cursor
import android.database.MatrixCursor
import android.location.Location
import android.net.Uri








internal object FakeData {

    
    fun groupOfUri(uri: Uri?): String? {
        val auth = uri?.authority ?: return null
        val path = uri.path.orEmpty()
        return when {
            auth.contains("contacts", ignoreCase = true) -> XpConfig.PERM_CONTACTS
            auth == "sms" || auth == "mms" || auth == "mms-sms" -> XpConfig.PERM_SMS
            auth.contains("call_log", ignoreCase = true) -> XpConfig.PERM_CALL_LOG
            auth.contains("calendar", ignoreCase = true) -> XpConfig.PERM_CALENDAR
            auth.contains("media", ignoreCase = true) ||
                    auth.startsWith("com.android.providers.media") ||
                    auth == "com.android.externalstorage.documents" ||
                    auth == "com.android.providers.downloads.documents" -> XpConfig.PERM_STORAGE
            path.contains("telephony", ignoreCase = true) && path.contains("sms") -> XpConfig.PERM_SMS
            else -> null
        }
    }

    private val DEFAULT_COLUMNS: Map<String, Array<String>> = mapOf(
        XpConfig.PERM_CONTACTS to arrayOf("_id", "display_name", "lookup", "photo_uri", "data1"),
        XpConfig.PERM_SMS to arrayOf("_id", "address", "body", "date", "type", "read"),
        XpConfig.PERM_CALL_LOG to arrayOf("_id", "number", "date", "duration", "type", "name"),
        XpConfig.PERM_CALENDAR to arrayOf("_id", "title", "dtstart", "dtend", "eventLocation"),
        XpConfig.PERM_STORAGE to arrayOf(
            "_id", "_display_name", "_size", "mime_type", "date_added", "_data"
        ),
    )

    private val ROWS: Map<String, List<Map<String, Any?>>> = mapOf(
        XpConfig.PERM_CONTACTS to listOf(
            mapOf(
                "_id" to 1L, "display_name" to "示例联系人", "lookup" to "0r1-0",
                "photo_uri" to null, "data1" to "13800000001",
            ),
            mapOf(
                "_id" to 2L, "display_name" to "测试用户", "lookup" to "0r2-0",
                "photo_uri" to null, "data1" to "13800000002",
            ),
        ),
        XpConfig.PERM_SMS to listOf(
            mapOf(
                "_id" to 1L, "address" to "10086", "body" to "这是一条虚构短信",
                "date" to 1700000000000L, "type" to 1, "read" to 1,
            ),
        ),
        XpConfig.PERM_CALL_LOG to listOf(
            mapOf(
                "_id" to 1L, "number" to "13800000001", "date" to 1700000000000L,
                "duration" to 42, "type" to 1, "name" to "示例联系人",
            ),
        ),
        XpConfig.PERM_CALENDAR to listOf(
            mapOf(
                "_id" to 1L, "title" to "虚构日程", "dtstart" to 1700000000000L,
                "dtend" to 1700003600000L, "eventLocation" to "未知地点",
            ),
        ),
        XpConfig.PERM_STORAGE to listOf(
            mapOf(
                "_id" to 1L, "_display_name" to "sample.jpg", "_size" to 102400L,
                "mime_type" to "image/jpeg", "date_added" to 1700000000L,
                "_data" to "/storage/emulated/0/DCIM/sample.jpg",
            ),
            mapOf(
                "_id" to 2L, "_display_name" to "notes.txt", "_size" to 512L,
                "mime_type" to "text/plain", "date_added" to 1700000100L,
                "_data" to "/storage/emulated/0/Documents/notes.txt",
            ),
        ),
    )

    
    fun cursor(group: String, projection: Array<String>?): Cursor {
        val cols = when {
            !projection.isNullOrEmpty() -> projection
            else -> DEFAULT_COLUMNS[group] ?: arrayOf("_id")
        }
        val mc = MatrixCursor(cols)
        val rows = ROWS[group].orEmpty()
        for (row in rows) {
            val values = ArrayList<Any?>(cols.size)
            for (c in cols) {
                values.add(row[c] ?: defaultFor(c))
            }
            mc.addRow(values)
        }
        return mc
    }

    private fun defaultFor(column: String): Any? {
        val c = column.lowercase()
        return when {
            c == "_id" || c == "id" -> 0L
            c == "_count" -> 0
            c == "_size" || c == "size" -> 0L
            c.endsWith("_id") -> 0L
            c.contains("count") -> 0
            c.contains("date") || c.contains("time") || c.startsWith("dt") -> 0L
            c.contains("duration") -> 0
            c.contains("type") || c.contains("read") || c.contains("flag") -> 0
            c.contains("number") || c.contains("phone") -> ""
            c.contains("address") || c.contains("email") || c.contains("body") -> ""
            c.contains("name") || c.contains("title") || c.contains("label") -> ""
            c.contains("mime") -> ""
            c.contains("data") || c.contains("path") -> ""
            c.contains("latitude") || c.contains("longitude") -> 0.0
            else -> null
        }
    }

    

    fun fakeLocation(provider: String?): Location {
        val loc = Location(provider ?: "fake")
        loc.latitude = 39.9042
        loc.longitude = 116.4074
        loc.altitude = 0.0
        loc.time = System.currentTimeMillis()
        loc.accuracy = 10f
        return loc
    }

    val TELEPHONY_FAKE: Map<String, String> = mapOf(
        "getLine1Number" to "13800138000",
        "getDeviceId" to "000000000000000",
        "getImei" to "000000000000000",
        "getMeid" to "00000000000000",
        "getSubscriberId" to "460000000000000",
        "getSimSerialNumber" to "89860000000000000000",
        "getVoiceMailNumber" to "",
        "getNai" to "",
        "getSimCountryIso" to "cn",
        "getSimOperator" to "46000",
        "getSimOperatorName" to "FakeOperator",
        "getNetworkOperator" to "46000",
        "getNetworkOperatorName" to "FakeOperator",
        "getNetworkCountryIso" to "cn",
    )
}
