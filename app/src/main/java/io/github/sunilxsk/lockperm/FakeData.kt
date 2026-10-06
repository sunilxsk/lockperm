package io.github.sunilxsk.lockperm

import android.database.Cursor
import android.database.MatrixCursor
import android.location.Location
import android.net.Uri
import java.util.Random
import kotlin.math.abs















internal object FakeData {

    private val RND = Random()

    
    
    

    private fun <T> pick(list: List<T>): T = list[RND.nextInt(list.size)]

    private fun int(from: Int, to: Int): Int =
        if (to <= from) from else from + RND.nextInt(to - from + 1)

    private fun long(from: Long, to: Long): Long =
        if (to <= from) from else from + (abs(RND.nextLong()) % (to - from + 1))

    private fun digits(n: Int): String = buildString {
        repeat(n) { append(RND.nextInt(10)) }
    }

    
    private fun recentMillis(spanMs: Long = 30L * 24 * 3600 * 1000): Long =
        System.currentTimeMillis() - long(60_000L, spanMs)

    private fun hex(n: Int, upper: Boolean = true): String = buildString {
        repeat(n) {
            val v = RND.nextInt(16)
            append(if (upper) "0123456789ABCDEF"[v] else "0123456789abcdef"[v])
        }
    }

    
    private fun imei(): String {
        val body = digits(14)
        var sum = 0
        for (i in 0 until 14) {
            var d = body[i] - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        val check = (10 - sum % 10) % 10
        return body + check
    }

    
    private val OUI = listOf(
        "A4:5E:60", "3C:15:C2", "B8:27:EB", "00:1A:7D", "8C:85:90",
        "F0:18:98", "54:60:09", "D8:BB:C1", "28:6C:07", "AC:BC:32",
        "00:0C:29", "1C:1B:0D", "74:D0:2B", "C8:3A:35", "E4:5F:01",
    )

    private fun mac(): String =
        pick(OUI) + ":" + hex(2) + ":" + hex(2) + ":" + hex(2)

    
    
    

    private val SURNAMES = listOf(
        "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
        "徐", "孙", "马", "朱", "胡", "林", "郭", "何", "高", "罗",
        "郑", "梁", "谢", "宋", "唐", "许", "邓", "冯", "韩", "曹",
    )
    private val GIVEN = listOf(
        "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "洋", "艳",
        "勇", "军", "杰", "娟", "涛", "明", "超", "秀英", "霞", "平",
        "刚", "桂英", "建国", "晨", "雨", "嘉怡", "子涵", "一鸣", "思远", "若曦",
    )
    private val MOBILE_PREFIX = listOf(
        "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
        "150", "151", "152", "157", "158", "159", "176", "177", "178",
        "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
        "199", "166",
    )

    private fun personName(): String = pick(SURNAMES) + pick(GIVEN)

    private fun mobile(): String = pick(MOBILE_PREFIX) + digits(8)

    private val CITIES = listOf(
        "北京市朝阳区", "北京市海淀区", "上海市浦东新区", "上海市徐汇区",
        "广州市天河区", "广州市越秀区", "深圳市南山区", "深圳市福田区",
        "杭州市西湖区", "杭州市余杭区", "成都市武侯区", "成都市高新区",
        "武汉市洪山区", "南京市鼓楼区", "西安市雁塔区", "重庆市渝中区",
        "苏州市工业园区", "长沙市岳麓区", "青岛市市南区", "天津市和平区",
    )
    private val STREETS = listOf(
        "建国路", "人民路", "中山路", "解放大道", "科技园路", "文化街",
        "滨江大道", "长安街", "南京路", "天河路", "深南大道", "文一西路",
    )

    private fun address(): String =
        pick(CITIES) + pick(STREETS) + int(1, 999) + "号"

    private val EMAIL_DOMAIN = listOf(
        "gmail.com", "outlook.com", "qq.com", "163.com", "126.com",
        "sina.com", "hotmail.com", "foxmail.com", "icloud.com",
    )

    private fun email(name: String? = null): String {
        val user = name ?: ("user" + digits(int(5, 8)))
        return "$user@${pick(EMAIL_DOMAIN)}"
    }

    
    private val CITY_COORDS = listOf(
        39.9042 to 116.4074, 
        31.2304 to 121.4737, 
        23.1291 to 113.2644, 
        22.5431 to 114.0579, 
        30.2741 to 120.1551, 
        30.5728 to 104.0668, 
        30.5928 to 114.3055, 
        32.0603 to 118.7969, 
        34.3416 to 108.9398, 
        29.5630 to 106.5516, 
    )

    private fun coord(): Pair<Double, Double> {
        val (lat, lon) = pick(CITY_COORDS)
        
        val jitter = { (RND.nextDouble() - 0.5) * 0.1 }
        return (lat + jitter()) to (lon + jitter())
    }

    
    
    

    private val cache = HashMap<String, List<Map<String, Any?>>>()

    private fun batch(group: String, size: Int, make: (Int) -> Map<String, Any?>): List<Map<String, Any?>> =
        cache.getOrPut(group) {
            val list = ArrayList<Map<String, Any?>>(size)
            for (i in 0 until size) list.add(make(i))
            list
        }

    
    internal fun invalidate() {
        synchronized(cache) { cache.clear() }
    }

    
    
    

    private fun contactRow(i: Int): Map<String, Any?> {
        val name = personName()
        val num = mobile()
        return mapOf(
            "_id" to (i + 1).toLong(),
            "id" to (i + 1).toLong(),
            "display_name" to name,
            "display_name_alt" to name,
            "lookup" to "0r${i + 1}-${hex(8, false)}",
            "photo_uri" to null,
            "photo_thumb_uri" to null,
            "data1" to num,
            "data2" to int(1, 3),       
            "data3" to null,
            "data4" to address(),
            "data5" to email(),         
            "starred" to if (RND.nextInt(5) == 0) 1 else 0,
            "has_phone_number" to 1,
            "contact_id" to (i + 1).toLong(),
            "in_visible_group" to 1,
            "times_contacted" to int(0, 40),
            "last_time_contacted" to recentMillis(90L * 24 * 3600 * 1000),
            "sort_key" to name,
        )
    }

    private val SMS_BODIES = listOf(
        "【中国移动】您本月话费余额为%d元，祝您生活愉快。",
        "【验证码】您的验证码是%s，5分钟内有效，请勿泄露给他人。",
        "【顺丰速运】您的快递已到达%s，请凭取件码%s领取。",
        "妈妈：晚上回来吃饭吗？我买了你爱吃的菜。",
        "【招商银行】您尾号%s的储蓄卡于%s支出人民币%d元，余额%d元。",
        "好的，我大概下午三点到，到时候电话联系。",
        "【京东】您的订单已发货，预计明日送达，请注意查收。",
        "收到，谢谢！",
        "【12306】您购买的%s次列车已于%s发车，祝您旅途愉快。",
        "明天上午十点的会议改到十一点半了，会议室不变。",
    )

    private fun smsRow(i: Int): Map<String, Any?> {
        val body = pick(SMS_BODIES).let { t ->
            when {
                t.contains("%d") && t.contains("%s") -> String.format(
                    t, digits(4), int(10, 999)
                )
                t.contains("%d") -> String.format(t, int(10, 999))
                t.contains("%s") -> String.format(t, digits(6))
                else -> t
            }
        }
        return mapOf(
            "_id" to (i + 1).toLong(),
            "thread_id" to (i + 1).toLong(),
            "address" to if (RND.nextInt(3) == 0) pick(
                listOf("10086", "10010", "10000", "95555", "1069" + digits(4))
            ) else mobile(),
            "person" to if (RND.nextInt(2) == 0) (i + 1).toLong() else null,
            "date" to recentMillis(),
            "date_sent" to recentMillis(),
            "protocol" to if (RND.nextInt(2) == 0) 0 else 1,
            "read" to if (RND.nextInt(4) == 0) 0 else 1,
            "status" to -1,
            "type" to if (RND.nextBoolean()) 1 else 2,  
            "body" to body,
            "subject" to null,
            "locked" to 0,
            "seen" to 1,
            "service_center" to null,
        )
    }

    private fun callLogRow(i: Int): Map<String, Any?> {
        
        val type = int(1, 3)
        val name = if (RND.nextInt(3) == 0) personName() else null
        return mapOf(
            "_id" to (i + 1).toLong(),
            "number" to mobile(),
            "date" to recentMillis(14L * 24 * 3600 * 1000),
            "duration" to if (type == 3) 0 else int(3, 900),
            "type" to type,
            "name" to name,
            "numbertype" to 1,
            "numberlabel" to null,
            "cached_name" to name,
            "cached_number_type" to 1,
            "countryiso" to "CN",
            "geocoded_location" to if (RND.nextInt(3) == 0) pick(CITIES) else null,
            "new" to 0,
            "is_read" to 1,
            "via_number" to null,
        )
    }

    private val EVENT_TITLES = listOf(
        "部门周会", "项目评审", "客户拜访", "体检预约", "牙医复诊",
        "团队聚餐", "健身训练", "航班起飞", "酒店入住", "朋友生日",
        "季度汇报", "培训讲座", "家长会", "车辆保养", "电影票",
    )
    private val EVENT_PLACES = listOf(
        "三楼会议室", "A 座 1801", "客户公司", "市中心医院",
        "健身房", "机场 T2", "", "", "线上会议", "咖啡厅",
    )

    private fun calendarRow(i: Int): Map<String, Any?> {
        val start = recentMillis(60L * 24 * 3600 * 1000)
        return mapOf(
            "_id" to (i + 1).toLong(),
            "calendar_id" to 1L,
            "title" to pick(EVENT_TITLES),
            "description" to "",
            "eventLocation" to pick(EVENT_PLACES),
            "dtstart" to start,
            "dtend" to start + int(30, 180) * 60_000L,
            "duration" to "PT${int(30, 180)}M",
            "allDay" to 0,
            "hasAlarm" to if (RND.nextInt(3) == 0) 1 else 0,
            "organizer" to email(),
            "availability" to 0,
            "eventTimezone" to "Asia/Shanghai",
        )
    }

    private val MEDIA_MIME = listOf(
        "image/jpeg" to "IMG_%s.jpg",
        "image/png" to "Screenshot_%s.png",
        "video/mp4" to "VID_%s.mp4",
        "audio/mpeg" to "recording_%s.mp3",
        "text/plain" to "notes_%s.txt",
        "application/pdf" to "document_%s.pdf",
    )

    private fun storageRow(i: Int): Map<String, Any?> {
        val (mime, pattern) = pick(MEDIA_MIME)
        val name = String.format(pattern, hex(8, false))
        val dir = when {
            mime.startsWith("image") -> "/storage/emulated/0/DCIM/Camera"
            mime.startsWith("video") -> "/storage/emulated/0/Movies"
            mime.startsWith("audio") -> "/storage/emulated/0/Music"
            else -> "/storage/emulated/0/Documents"
        }
        val dateAdded = recentMillis(180L * 24 * 3600 * 1000) / 1000
        return mapOf(
            "_id" to (i + 1).toLong(),
            "_display_name" to name,
            "_size" to long(2_000, 80_000_000),
            "mime_type" to mime,
            "date_added" to dateAdded,
            "date_modified" to dateAdded,
            "_data" to "$dir/$name",
            "bucket_display_name" to dir.substringAfterLast('/'),
            "bucket_id" to int(1000, 999999),
            "width" to if (mime.startsWith("image") || mime.startsWith("video")) int(720, 4096) else 0,
            "height" to if (mime.startsWith("image") || mime.startsWith("video")) int(720, 4096) else 0,
            "orientation" to 0,
            "duration" to if (mime.startsWith("video") || mime.startsWith("audio")) int(1000, 600_000) else 0,
            "is_pending" to 0,
            "is_trashed" to 0,
        )
    }

    
    private val BT_NAMES = listOf(
        "Xiaomi Buds 4", "HUAWEI FreeBuds", "AirPods Pro", "Galaxy Buds2",
        "Redmi Watch 4", "OPPO Enco X2", "vivo TWS 3", "Sony WH-1000XM5",
        "Mi Band 8", "JBL Flip 6", "Beats Studio Buds", "OnePlus Buds Pro",
        "车载蓝牙", "iWatch", "Nintendo Switch",
    )

    private fun bluetoothRow(i: Int): Map<String, Any?> {
        val name = pick(BT_NAMES)
        val cls = if (RND.nextInt(3) == 0) int(0, 31) else 0
        return mapOf(
            "_id" to (i + 1).toLong(),
            "name" to name,
            "address" to mac(),
            "mac" to mac(),
            "rssi" to -int(30, 95),
            "type" to int(1, 3),
            "bond_state" to int(10, 12),
            "device_class" to cls,
            "major_class" to int(0, 31),
            "paired" to if (RND.nextInt(3) == 0) 1 else 0,
            "connected" to if (RND.nextInt(4) == 0) 1 else 0,
        )
    }

    



    private fun healthRow(i: Int): Map<String, Any?> = mapOf(
        "_id" to (i + 1).toLong(),
        "heart_rate" to int(58, 102),
        "resting_heart_rate" to int(52, 72),
        "heart_rate_variability" to (int(200, 900) / 10.0),
        "oxygen_saturation" to (int(950, 1000) / 10.0),       
        "respiratory_rate" to int(12, 20),
        "body_temperature" to (int(360, 375) / 10.0),         
        "basal_body_temperature" to (int(360, 372) / 10.0),
        "blood_glucose" to (int(39, 110) / 10.0),             
        "systolic" to int(105, 135),
        "diastolic" to int(65, 88),
        "steps" to int(1200, 18000),
        "distance" to long(500, 15000),                       
        "floors_climbed" to int(0, 30),
        "active_calories" to int(100, 900),
        "total_calories" to int(1200, 3200),
        "weight" to (int(450, 900) / 10.0),                   
        "height" to (int(1500, 1900) / 10.0),                 
        "body_fat" to (int(80, 300) / 10.0),
        "vo2_max" to (int(250, 550) / 10.0),
        "sleep_duration" to long(18_000_000, 32_000_000),     
        "wheelchair_pushes" to int(0, 5000),
        "time" to recentMillis(7L * 24 * 3600 * 1000),
        "start_time" to recentMillis(7L * 24 * 3600 * 1000),
        "end_time" to System.currentTimeMillis(),
    )

    private val APP_PKGS = listOf(
        "com.tencent.mm" to "微信",
        "com.tencent.mobileqq" to "QQ",
        "com.taobao.taobao" to "淘宝",
        "com.ss.android.ugc.aweme" to "抖音",
        "com.xiaomi.market" to "小米应用商店",
        "com.android.settings" to "设置",
        "com.alipay.moblie" to "支付宝",
        "com.sina.weibo" to "微博",
        "com.baidu.searchbox" to "百度",
        "com.netease.cloudmusic" to "网易云音乐",
    )

    
    private fun usageRow(i: Int): Map<String, Any?> {
        val (pkg, label) = pick(APP_PKGS)
        val end = recentMillis(3L * 24 * 3600 * 1000)
        return mapOf(
            "_id" to (i + 1).toLong(),
            "package_name" to pkg,
            "app_label" to label,
            "first_time" to recentMillis(60L * 24 * 3600 * 1000),
            "last_time" to end,
            "total_time" to long(60_000, 4 * 3600_000),
            "launch_count" to int(1, 300),
        )
    }

    private val NOTI_TITLES = listOf(
        "新消息", "系统更新", "快递通知", "日程提醒", "评论回复",
        "账号安全提醒", "账单提醒", "天气预警", "订阅更新", "好友申请",
    )
    private val NOTI_TEXTS = listOf(
        "您有 3 条未读消息",
        "系统有新版本可用，建议及时更新",
        "您的包裹已到达驿站，请及时取件",
        "15 分钟后有会议",
        "有人回复了你的评论",
        "检测到新设备登录，如非本人请及时修改密码",
        "本月账单已生成，点击查看",
        "明日有雨，出行请携带雨具",
        "你关注的作者更新了内容",
        "有人请求添加你为好友",
    )

    
    private fun notificationRow(i: Int): Map<String, Any?> {
        val (pkg, label) = pick(APP_PKGS)
        return mapOf(
            "_id" to (i + 1).toLong(),
            "package_name" to pkg,
            "app_label" to label,
            "title" to pick(NOTI_TITLES),
            "text" to pick(NOTI_TEXTS),
            "sub_text" to "",
            "post_time" to recentMillis(24L * 3600 * 1000),
            "when" to recentMillis(24L * 3600 * 1000),
            "icon" to 0,
            "flags" to 0,
            "number" to 0,
            "is_ongoing" to 0,
            "is_clearable" to 1,
            "category" to null,
            "channel_id" to "default",
            "group_key" to null,
        )
    }

    
    private fun activityRow(i: Int): Map<String, Any?> {
        val type = int(0, 7)  
        val start = recentMillis(24L * 3600 * 1000)
        return mapOf(
            "_id" to (i + 1).toLong(),
            "activity_type" to type,
            "confidence" to int(30, 100),
            "steps" to int(500, 20000),
            "start_time" to start,
            "end_time" to start + long(600_000, 7_200_000),
            "duration" to long(600_000, 7_200_000),
            "distance" to long(300, 20000),
            "calories" to int(50, 1200),
        )
    }

    
    private fun sensorRow(i: Int): Map<String, Any?> = mapOf(
        "_id" to (i + 1).toLong(),
        "sensor_type" to int(1, 20),
        "heart_rate" to int(58, 102),
        "spo2" to (int(950, 1000) / 10.0),
        "temperature" to (int(360, 375) / 10.0),
        "steps" to int(1000, 18000),
        "timestamp" to recentMillis(24L * 3600 * 1000),
        "accuracy" to int(1, 3),
    )

    private val MAKERS: Map<String, (Int) -> Map<String, Any?>> = mapOf(
        XpConfig.PERM_CONTACTS to ::contactRow,
        XpConfig.PERM_SMS to ::smsRow,
        XpConfig.PERM_CALL_LOG to ::callLogRow,
        XpConfig.PERM_CALENDAR to ::calendarRow,
        XpConfig.PERM_STORAGE to ::storageRow,
        XpConfig.PERM_NEARBY to ::bluetoothRow,
        XpConfig.PERM_HEALTH to ::healthRow,
        XpConfig.PERM_USAGE_STATS to ::usageRow,
        XpConfig.PERM_NOTIFICATION to ::notificationRow,
        XpConfig.PERM_NOTIFICATION_LISTENER to ::notificationRow,
        XpConfig.PERM_ACTIVITY to ::activityRow,
        XpConfig.PERM_SENSORS to ::sensorRow,
    )

    
    fun rowsOf(group: String): List<Map<String, Any?>> {
        val make = MAKERS[group] ?: return emptyList()
        return batch(group, ROWS_OF[group] ?: 5, make)
    }

    
    private val ROWS_OF: Map<String, Int> = mapOf(
        XpConfig.PERM_CONTACTS to 8,
        XpConfig.PERM_SMS to 6,
        XpConfig.PERM_CALL_LOG to 6,
        XpConfig.PERM_CALENDAR to 3,
        XpConfig.PERM_STORAGE to 10,
        XpConfig.PERM_NEARBY to 5,
        XpConfig.PERM_HEALTH to 5,
        XpConfig.PERM_USAGE_STATS to 8,
        XpConfig.PERM_NOTIFICATION to 4,
        XpConfig.PERM_NOTIFICATION_LISTENER to 4,
        XpConfig.PERM_ACTIVITY to 3,
        XpConfig.PERM_SENSORS to 3,
    )

    
    
    

    fun groupOfUri(uri: Uri?): String? {
        val auth = uri?.authority.orEmpty()
        val path = uri?.path.orEmpty()
        val s = uri?.toString().orEmpty()
        return when {
            auth.contains("contacts", ignoreCase = true) -> XpConfig.PERM_CONTACTS
            auth == "sms" || auth == "mms" || auth == "mms-sms" ||
                    path.contains("sms", ignoreCase = true) -> XpConfig.PERM_SMS
            auth.contains("call_log", ignoreCase = true) ||
                    path.contains("call_log", ignoreCase = true) -> XpConfig.PERM_CALL_LOG
            auth.contains("calendar", ignoreCase = true) -> XpConfig.PERM_CALENDAR
            auth.contains("media", ignoreCase = true) ||
                    auth.startsWith("com.android.providers.media") ||
                    auth == "com.android.externalstorage.documents" ||
                    auth == "com.android.providers.downloads.documents" ||
                    path.contains("media", ignoreCase = true) -> XpConfig.PERM_STORAGE
            
            auth.contains("health", ignoreCase = true) ||
                    s.contains("health", ignoreCase = true) -> XpConfig.PERM_HEALTH
            
            auth.contains("usage", ignoreCase = true) -> XpConfig.PERM_USAGE_STATS
            
            auth.contains("bluetooth", ignoreCase = true) -> XpConfig.PERM_NEARBY
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
        XpConfig.PERM_NEARBY to arrayOf("_id", "name", "address", "rssi", "type", "bond_state"),
        XpConfig.PERM_HEALTH to arrayOf("_id", "heart_rate", "steps", "weight", "time"),
        XpConfig.PERM_USAGE_STATS to arrayOf("_id", "package_name", "last_time", "total_time"),
        XpConfig.PERM_NOTIFICATION to arrayOf("_id", "package_name", "title", "text", "post_time"),
        XpConfig.PERM_NOTIFICATION_LISTENER to
                arrayOf("_id", "package_name", "title", "text", "post_time"),
        XpConfig.PERM_ACTIVITY to arrayOf("_id", "activity_type", "steps", "start_time"),
        XpConfig.PERM_SENSORS to arrayOf("_id", "sensor_type", "heart_rate", "timestamp"),
    )

    
    fun hasRows(group: String): Boolean = MAKERS.containsKey(group)

    fun cursor(group: String, projection: Array<String>?): Cursor {
        val cols = when {
            !projection.isNullOrEmpty() -> projection
            else -> DEFAULT_COLUMNS[group] ?: arrayOf("_id")
        }
        val mc = MatrixCursor(cols)
        val make = MAKERS[group] ?: return mc
        val rows = batch(group, ROWS_OF[group] ?: 5, make)
        for (row in rows) {
            val values = ArrayList<Any?>(cols.size)
            for (c in cols) values.add(row[c] ?: defaultFor(c))
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
            c.contains("duration") -> 0L
            c.contains("latitude") || c.contains("longitude") -> 0.0
            c.contains("rate") || c.contains("saturation") || c.contains("temperature") ||
                    c.contains("glucose") || c.contains("weight") || c.contains("height") ||
                    c.contains("fat") || c.contains("vo2") || c.contains("confidence") -> 0.0
            c.contains("type") || c.contains("read") || c.contains("flag") ||
                    c.contains("seen") || c.contains("starred") || c.contains("accuracy") -> 0
            c.contains("number") || c.contains("phone") -> ""
            c.contains("address") || c.contains("email") || c.contains("body") -> ""
            c.contains("name") || c.contains("title") || c.contains("label") ||
                    c.contains("text") || c.contains("location") || c.contains("organizer") -> ""
            c.contains("mime") -> ""
            c.contains("data") || c.contains("path") || c.contains("channel") -> ""
            else -> null
        }
    }

    
    
    

    private var cachedLoc: Location? = null

    
    fun coordPublic(): Pair<Double, Double> = coord()

    



    fun locationFor(provider: String, c: XpState.Snapshot): Location {
        val loc = Location(provider)
        if (c.locMode == 1) {
            val lat = c.locLat.toDoubleOrNull()
            val lon = c.locLon.toDoubleOrNull()
            if (lat != null && lon != null) {
                loc.latitude = lat
                loc.longitude = lon
                loc.altitude = c.locAlt.toDoubleOrNull() ?: 0.0
                loc.accuracy = c.locAcc.toFloatOrNull() ?: 10f
                loc.time = System.currentTimeMillis()
                loc.elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                return loc
            }
        }
        val (la, lo) = coord()
        loc.latitude = la
        loc.longitude = lo
        loc.altitude = int(2, 300).toDouble()
        loc.accuracy = int(5, 65).toFloat()
        loc.bearing = int(0, 359).toFloat()
        loc.speed = int(0, 12).toFloat()
        loc.time = System.currentTimeMillis()
        loc.elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
        return loc
    }

    fun intPublic(from: Int, to: Int): Int = int(from, to)

    fun fakeLocation(provider: String?): Location {
        val cached = cachedLoc
        if (cached != null) return Location(cached).apply { time = System.currentTimeMillis() }

        val (lat, lon) = coord()
        val loc = Location(provider ?: "fused").apply {
            latitude = lat
            longitude = lon
            
            altitude = int(2, 300).toDouble()
            time = System.currentTimeMillis()
            accuracy = int(5, 65).toFloat()
            bearing = int(0, 359).toFloat()
            speed = int(0, 12).toFloat()
            elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
        }
        cachedLoc = loc
        return loc
    }

    
    
    

    private val OPERATORS = listOf(
        "46000" to "中国移动",
        "46001" to "中国联通",
        "46003" to "中国电信",
    )

    
    val TELEPHONY_FAKE: Map<String, String> by lazy {
        val (mccmnc, opName) = pick(OPERATORS)
        val imei1 = imei()
        val num = mobile()
        mapOf(
            "getLine1Number" to num,
            "getLine1AlphaTag" to "",
            "getDeviceId" to imei1,
            "getImei" to imei1,
            "getDeviceId(int)" to imei1,
            "getImei(int)" to imei1,
            "getMeid" to hex(14),
            "getSubscriberId" to mccmnc + digits(10),
            "getSimSerialNumber" to "8986" + digits(16),
            "getVoiceMailNumber" to "",
            "getVoiceMailAlphaTag" to "",
            "getNai" to "",
            "getSimCountryIso" to "cn",
            "getSimOperator" to mccmnc,
            "getSimOperatorName" to opName,
            "getNetworkOperator" to mccmnc,
            "getNetworkOperatorName" to opName,
            "getNetworkCountryIso" to "cn",
            "getNetworkSpecifier" to mccmnc,
            "getGroupIdLevel1" to "",
        )
    }

    
    
    

    private val ACCOUNT_TYPES = listOf(
        "com.google", "com.osp.app.signin", "com.xiaomi", "com.tencent.mm",
        "com.sina.weibo", "com.baidu", "com.alipay",
    )

    val ACCOUNTS: List<Pair<String, String>> by lazy {
        val n = int(1, 3)
        val used = HashSet<String>()
        val out = ArrayList<Pair<String, String>>(n)
        repeat(n) {
            val t = pick(ACCOUNT_TYPES)
            if (used.add(t)) out.add(t to email())
        }
        out
    }
}
