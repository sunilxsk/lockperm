package io.github.sunilxsk.lockperm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.libxposed.service.XposedService



@Composable
fun DisguiseConfigContent(cfg: XpConfigState, enabled: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NativeStatusBar(cfg)

        SectionTitle("身份伪装")



        val buildOn = cfg.bool(XpConfig.KEY_ENABLE_BUILD, true)
        val androidIdOn = cfg.bool(XpConfig.KEY_ENABLE_ANDROID_ID, false)
        FeatureCard(
            title = "伪装设备信息（Build 字段）",
            subtitle = "MODEL / BRAND / FINGERPRINT / SERIAL / 系统版本 / GSF / 广告 ID …… 留空表示不修改",
            checked = buildOn,
            onCheckedChange = { cfg.put(XpConfig.KEY_ENABLE_BUILD, it) }
        ) {
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        BuildRandom.generate().forEach { (k, v) -> cfg.put(k, v) }
                    },
                    enabled = buildOn,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                    Text("一键随机")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        XpConfig.BUILD_FIELDS.forEach { f -> cfg.put(f.key, "") }
                        listOf(
                            XpConfig.KEY_DEVICE_NAME, XpConfig.KEY_GSF_ID,
                            XpConfig.KEY_ADS_ID, XpConfig.KEY_APPSET_ID,
                            XpConfig.KEY_DRM_ID,
                        ).forEach { cfg.put(it, "") }
                        listOf(
                            XpConfig.KEY_FAKE_SDK_INT, XpConfig.KEY_FAKE_TIME_OFFSET,
                        ).forEach { cfg.put(it, 0) }
                        listOf(
                            XpConfig.KEY_FAKE_DEV_OFF, XpConfig.KEY_FAKE_TIME_ENABLE,
                            XpConfig.KEY_FAKE_UPTIME_ENABLE, XpConfig.KEY_FAKE_TEMP_ENABLE,
                            XpConfig.KEY_FAKE_BATTERY_ENABLE,
                        ).forEach { cfg.put(it, false) }
                        (XpConfig.EXTRA_FIELDS_NET + XpConfig.EXTRA_FIELDS_SIM +
                                XpConfig.EXTRA_FIELDS_SYS + XpConfig.EXTRA_FIELDS_ID +
                                XpConfig.EXTRA_FIELDS_HW)
                            .forEach { f -> cfg.put(f.key, "") }
                    },
                    enabled = buildOn,
                    modifier = Modifier.weight(1f),
                ) { Text("全部清空") }
            }
            HintText(
                "一键随机 = 生成一套自洽的假设备：品牌型号对得上、指纹由各字段拼出来，" +
                        "Android ID / GSF / 广告 ID / App Set ID / DRM ID 也一起换掉。"
            )

            val filled = XpConfig.BUILD_FIELDS.count { cfg.str(it.key, "").isNotBlank() }
            HintText(
                if (filled == 0) {
                    "当前：全部留空，不修改任何设备信息。"
                } else {
                    "当前已填 $filled 项，其余字段保持原值。"
                }
            )

            Text(
                "标识类",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SwitchRow(
                title = "修改 Android_ID",
                subtitle = "Hook Settings.Secure.getString，替换 android_id 返回值",
                checked = cfg.bool(XpConfig.KEY_ENABLE_ANDROID_ID, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_ENABLE_ANDROID_ID, it) },
            )
            LabeledTextField(
                label = "Android_ID（留空为不修改）",
                value = cfg.str(XpConfig.KEY_ANDROID_ID, ""),
                onValueChange = { cfg.put(XpConfig.KEY_ANDROID_ID, it) },
                enabled = buildOn && cfg.bool(XpConfig.KEY_ENABLE_ANDROID_ID, false),
            )
            LabeledTextField(
                label = "GSF ID（16 位十六进制，留空为不修改）",
                value = cfg.str(XpConfig.KEY_GSF_ID, ""),
                onValueChange = { cfg.put(XpConfig.KEY_GSF_ID, it) },
                enabled = buildOn,
            )
            LabeledTextField(
                label = "广告 ID（Advertising ID，留空为不修改）",
                value = cfg.str(XpConfig.KEY_ADS_ID, ""),
                onValueChange = { cfg.put(XpConfig.KEY_ADS_ID, it) },
                enabled = buildOn,
            )
            LabeledTextField(
                label = "App Set ID（留空为不修改）",
                value = cfg.str(XpConfig.KEY_APPSET_ID, ""),
                onValueChange = { cfg.put(XpConfig.KEY_APPSET_ID, it) },
                enabled = buildOn,
            )
            LabeledTextField(
                label = "Media DRM ID（64 位十六进制，留空为不修改）",
                value = cfg.str(XpConfig.KEY_DRM_ID, ""),
                onValueChange = { cfg.put(XpConfig.KEY_DRM_ID, it) },
                enabled = buildOn,
            )
            HorizontalDividerCompat()
            Text(
                "更多标识 ID",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            XpConfig.EXTRA_FIELDS_ID.forEach { f ->
                LabeledTextField(
                    label = f.label,
                    value = cfg.str(f.key, ""),
                    onValueChange = { cfg.put(f.key, it) },
                    enabled = buildOn,
                )
            }

            HorizontalDividerCompat()
            Text(
                "网络 / MAC / 运营商",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            XpConfig.EXTRA_FIELDS_NET.forEach { f ->
                LabeledTextField(
                    label = if (f.hint.isEmpty()) f.label else "${f.label}（${f.hint}）",
                    value = cfg.str(f.key, ""),
                    onValueChange = { cfg.put(f.key, it) },
                    enabled = buildOn,
                )
            }

            HorizontalDividerCompat()
            Text(
                "SIM 卡 / 手机号 / IMEI",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            XpConfig.EXTRA_FIELDS_SIM.forEach { f ->
                LabeledTextField(
                    label = if (f.hint.isEmpty()) f.label else "${f.label}（${f.hint}）",
                    value = cfg.str(f.key, ""),
                    onValueChange = { cfg.put(f.key, it) },
                    enabled = buildOn,
                )
            }

            HorizontalDividerCompat()
            Text(
                "系统环境",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            XpConfig.EXTRA_FIELDS_SYS.forEach { f ->
                if (f.key == XpConfig.KEY_FAKE_SDK_INT) return@forEach
                LabeledTextField(
                    label = if (f.hint.isEmpty()) f.label else "${f.label}（${f.hint}）",
                    value = cfg.str(f.key, ""),
                    onValueChange = { cfg.put(f.key, it) },
                    enabled = buildOn,
                )
            }

            HorizontalDividerCompat()
            Text(
                "Android 版本（版本与 SDK 一起改，避免对不上）",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            val curRelease = cfg.str(XpConfig.KEY_BUILD_RELEASE, "")
            val curSdk = cfg.int(XpConfig.KEY_FAKE_SDK_INT, 0)
            val curIdx = XpConfig.ANDROID_VERSIONS.indexOfFirst { it.second == curSdk }
            SingleSelectChips(
                options = listOf("不改") + XpConfig.ANDROID_VERSIONS.map { it.first },
                selectedIndex = curIdx + 1,
                enabled = buildOn,
                onSelect = { i ->
                    if (i == 0) {
                        cfg.put(XpConfig.KEY_BUILD_RELEASE, "")
                        cfg.put(XpConfig.KEY_FAKE_SDK_INT, 0)
                    } else {
                        val (r, s) = XpConfig.ANDROID_VERSIONS[i - 1]
                        cfg.put(XpConfig.KEY_BUILD_RELEASE, r)
                        cfg.put(XpConfig.KEY_FAKE_SDK_INT, s)
                    }
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sdkNow = cfg.int(XpConfig.KEY_FAKE_SDK_INT, 0)
                LabeledTextField(
                    label = "版本字符串（自定义）",
                    value = curRelease,
                    onValueChange = { cfg.put(XpConfig.KEY_BUILD_RELEASE, it) },
                    enabled = buildOn,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = "SDK（自定义）",
                    value = if (sdkNow == 0) "" else sdkNow.toString(),
                    onValueChange = {
                        cfg.put(
                            XpConfig.KEY_FAKE_SDK_INT,
                            it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                        )
                    },
                    enabled = buildOn,
                    modifier = Modifier.weight(1f),
                )
            }
            HintText(
                if (curSdk <= 0) {
                    "当前：不修改 Android 版本。想自定义就在上面两个框里直接填，" +
                            "例如版本填 14、SDK 填 34，两个都填才会一起生效。"
                } else {
                    "当前：Android ${curRelease.ifEmpty { XpConfig.releaseFor(curSdk) }} / SDK $curSdk。" +
                            "填了其中一个，另一个会自动补成对应的值；两个都填则以你填的为准。" +
                            "伪装成比自己设备更高的版本时，有的应用会按新版本走代码路径导致闪退。"
                }
            )

            HorizontalDividerCompat()
            Text(
                "硬件与内核",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            XpConfig.EXTRA_FIELDS_HW.forEach { f ->
                LabeledTextField(
                    label = if (f.hint.isEmpty()) f.label else "${f.label}（${f.hint}）",
                    value = cfg.str(f.key, ""),
                    onValueChange = { cfg.put(f.key, it) },
                    enabled = buildOn,
                )
            }
            SwitchRow(
                title = "伪装设备温度",
                subtitle = "/sys/class/thermal 与 BatteryManager 读数一起改",
                checked = cfg.bool(XpConfig.KEY_FAKE_TEMP_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_TEMP_ENABLE, it) },
            )
            SwitchRow(
                title = "伪装电量",
                subtitle = "BatteryManager 与电池广播的 level / scale",
                checked = cfg.bool(XpConfig.KEY_FAKE_BATTERY_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_BATTERY_ENABLE, it) },
            )
            HintText(
                "内核版本同时改 System.getProperty(os.version)、/proc/version 和 shell 的 uname；" +
                        "架构改 os.arch 与 uname -m。GPU 改的是 GLES 的 GL_RENDERER，" +
                        "一键随机时会跟着芯片型号走。"
            )
            SwitchRow(
                title = "伪装开发者选项已关闭",
                subtitle = "Settings.Global / Secure 里 development_settings_enabled、adb_enabled 一律返回 0",
                checked = cfg.bool(XpConfig.KEY_FAKE_DEV_OFF, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_DEV_OFF, it) },
            )
            SwitchRow(
                title = "启用系统时间偏移",
                subtitle = "System.currentTimeMillis() 整体加上上面的分钟数（可填负数）",
                checked = cfg.bool(XpConfig.KEY_FAKE_TIME_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_TIME_ENABLE, it) },
            )
            SwitchRow(
                title = "伪装设备已运行时间",
                subtitle = "SystemClock.uptimeMillis / elapsedRealtime 按上面的小时数上报，且会继续往前走",
                checked = cfg.bool(XpConfig.KEY_FAKE_UPTIME_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_UPTIME_ENABLE, it) },
            )
            HintText(
                "⚠️ Android SDK 版本填比自己设备更高的值时要小心：" +
                        "有的应用会按高版本走新代码路径，可能直接闪退。" +
                        "另外部分 ROM 的 ART 会把 SDK_INT 内联进应用，改了也不一定生效。"
            )

            HorizontalDividerCompat()
            Text(
                "Build 字段",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            XpConfig.BUILD_FIELDS.forEach { f ->
                LabeledTextField(
                    label = f.label,
                    value = cfg.str(f.key, ""),
                    onValueChange = { cfg.put(f.key, it) },
                    enabled = buildOn,
                )
            }

            HorizontalDividerCompat()
            Text(
                "设备名",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LabeledTextField(
                label = "设备名（Settings.Global device_name / marketname）",
                value = cfg.str(XpConfig.KEY_DEVICE_NAME, ""),
                onValueChange = { cfg.put(XpConfig.KEY_DEVICE_NAME, it) },
                enabled = buildOn,
            )
            SwitchRow(
                title = "隐藏账号与邮箱",
                subtitle = "AccountManager 返回账号的方法一律给空数组，hasAccount 返回 false",
                checked = cfg.bool(XpConfig.KEY_HIDE_ACCOUNTS, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_HIDE_ACCOUNTS, it) },
            )
            HintText(
                "DRM ID 必须是 64 位十六进制才会生效；GSF ID 建议 16 位十六进制。" +
                        "留空的项都保持真机原值。"
            )
        }

        
        FeatureCard(
            title = "注入 WebView JavaScript",
            subtitle = "Hook WebViewClient.onPageFinished 和 WebView.loadUrl",
            checked = cfg.bool(XpConfig.KEY_ENABLE_JS, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_ENABLE_JS, it) }
        ) {
            val jsCode = cfg.str(XpConfig.KEY_JS_CODE, XpDefaults.JS)
            OutlinedTextField(
                value = jsCode,
                onValueChange = { cfg.put(XpConfig.KEY_JS_CODE, it) },
                label = { Text("JavaScript 代码") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                maxLines = 12,
                enabled = cfg.bool(XpConfig.KEY_ENABLE_JS, false),
            )
            var showJsEditor by remember { mutableStateOf(false) }
            TextButton(
                onClick = { showJsEditor = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("全屏编辑")
            }
            if (showJsEditor) {
                FullScreenCodeEditor(
                    title = "编辑 JavaScript",
                    value = jsCode,
                    onDismiss = { showJsEditor = false },
                    onConfirm = {
                        cfg.put(XpConfig.KEY_JS_CODE, it)
                        showJsEditor = false
                    }
                )
            }
        }

        
        FeatureCard(
            title = "WebView User-Agent 伪装",
            subtitle = "按预设字段随机生成 UA，或手动填写格式正确的 UA",
            checked = cfg.bool(XpConfig.KEY_ENABLE_UA, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_ENABLE_UA, it) }
        ) {
            val enabled = cfg.bool(XpConfig.KEY_ENABLE_UA, false)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { cfg.put(XpConfig.KEY_UA_VALUE, UaPresets.random()) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                ) { Text("随机生成") }
                OutlinedButton(
                    onClick = { cfg.put(XpConfig.KEY_UA_VALUE, "") },
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                ) { Text("清空") }
            }
            OutlinedTextField(
                value = cfg.str(XpConfig.KEY_UA_VALUE, ""),
                onValueChange = { cfg.put(XpConfig.KEY_UA_VALUE, it) },
                label = { Text("User-Agent") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                enabled = enabled,
            )
            HintText(
                "格式示例：Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        }

        SectionTitle("系统属性")

        
        CustomPropsCard(cfg, enabled)

        SectionTitle("Root 与网络痕迹")

        
        FeatureCard(
            title = "伪装 Root（命令返回成功）",
            subtitle = "应用执行 su / sudo / ls / 等命令时给它一个「成功」的结果",
            checked = cfg.bool(XpConfig.KEY_ROOT_FAKE_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_ROOT_FAKE_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_ROOT_FAKE_ENABLE, false)
            SwitchRow(
                title = "伪装 su 文件存在",
                subtitle = "File.exists() 对 /system/bin/su、/sbin/su 等路径返回 true",
                checked = cfg.bool(XpConfig.KEY_ROOT_FAKE_FILE, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_ROOT_FAKE_FILE, it) },
            )
            SwitchRow(
                title = "屏蔽 Permission denied 并强制成功",
                subtitle = "受限命令的报错被吃掉，退出码一律改成 0",
                checked = cfg.bool(XpConfig.KEY_ROOT_FAKE_MASK, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_ROOT_FAKE_MASK, it) },
            )
            HintText(
                        "「看起来有 Root」，但真去访问受限的东西依然拿不到实际内容，" +
                        "只是不再报 Permission denied。"
            )
        }

        
        FeatureCard(
            title = "隐藏 VPN / 抓包代理",
            subtitle = "让应用检测不到 VPN 网卡、VPN 传输类型和 HTTP 代理",
            checked = cfg.bool(XpConfig.KEY_VPN_HIDE_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_VPN_HIDE_ENABLE, false)
            SwitchRow(
                title = "隐藏 VPN 网卡接口",
                subtitle = "NetworkInterface getName / isUp / isVirtual / 枚举列表 / LinkProperties 接口名",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_IFACE, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_IFACE, it) },
            )
            SwitchRow(
                title = "剥离 VPN 传输能力",
                subtitle = "NetworkCapabilities hasTransport(VPN)=false、NOT_VPN=true、getTransportTypes 过滤、toString 去 VPN",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_CAPS, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_CAPS, it) },
            )
            SwitchRow(
                title = "修正旧版 NetworkInfo 类型",
                subtitle = "getType / getTypeName / isConnected：VPN 一律伪造成 MOBILE",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_NETINFO, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_NETINFO, it) },
            )
            SwitchRow(
                title = "隐藏 HTTP 代理（抓包）",
                subtitle = "System.getProperty(http.proxy*) / ConnectivityManager.getDefaultProxy / android.net.Proxy 全部清空",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_PROXY, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_PROXY, it) },
            )
            SwitchRow(
                title = "清空 VPN 相关系统设置",
                subtitle = "Settings.Secure / Global 里含 vpn 的键（always_on_vpn_app 等）返回空",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_SETTINGS, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_SETTINGS, it) },
            )
            HorizontalDividerCompat()
            LabeledTextField(
                label = "VPN 接口名（逗号分隔，支持前缀匹配）",
                value = cfg.str(XpConfig.KEY_VPN_IFACES, XpConfig.DEF_VPN_IFACES),
                onValueChange = { cfg.put(XpConfig.KEY_VPN_IFACES, it) },
                enabled = on,
                singleLine = false,
                maxLines = 3,
            )
            OutlinedButton(
                onClick = { cfg.put(XpConfig.KEY_VPN_IFACES, XpConfig.DEF_VPN_IFACES) },
                enabled = on,
            ) { Text("恢复默认接口名") }
            HintText(
                "默认已经覆盖 tun / ppp / utun / wg / tap / ipsec 这几类常见 VPN 网卡。" +
                        "如果你的 VPN 用的是别的接口名，加进去即可（写 tun 就能匹配 tun0、tun1 …）。"
            )
            HintText(
                "Java 层与 native 层都会拦：NetworkInterface / LinkProperties / " +
                        "NetworkCapabilities / NetworkInfo / 代理三件套，" +
                        "native 侧再补 getifaddrs、if_nametoindex，以及 /proc/net/dev、if_inet6、route " +
                        "这些文件里的网卡名（会预先读一遍剔掉 VPN 接口）。" +
                        "native 部分需要关于页的「Native 层 Hook」开启。"
            )
        }

        SectionTitle("稳定性")

        
        FeatureCard(
            title = "阻止闪退 / 自杀",
            subtitle = "拦截目标应用主动调用 killProcess / exit / halt / 信号 等自杀行为",
            checked = cfg.bool(XpConfig.KEY_BLOCK_CRASH_ENABLE, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CRASH_ENABLE, it) }
        ) {
            val enabled = cfg.bool(XpConfig.KEY_BLOCK_CRASH_ENABLE, true)
            Text(
                "强度等级",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            SingleSelectChips(
                options = listOf("低", "中", "高", "极高"),
                selectedIndex = cfg.int(XpConfig.KEY_BLOCK_CRASH_LEVEL, 1),
                onSelect = { cfg.put(XpConfig.KEY_BLOCK_CRASH_LEVEL, it) },
                enabled = enabled,
            )
            HintText(
                when (cfg.int(XpConfig.KEY_BLOCK_CRASH_LEVEL, 1)) {
                    0 -> "低：仅拦截 Process.killProcess / killProcessQuiet(自身)。风险最小。"
                    1 -> "中（推荐）：额外拦截 System.exit / Runtime.exit。大多数应用足够。"
                    2 -> "高：额外拦截 Runtime.halt、Process.sendSignal、Os.kill。"
                    else -> "极高：额外拦截 killBackgroundProcesses，并吞掉未捕获异常。" +
                            "所有拦截点都会先 deoptimize，避免被内联 / AOT 编译绕过。"
                }
            )
            HorizontalDividerCompat()
            SwitchRow(
                title = "拦截原生层退出指令",
                subtitle = "拦住 native 的 exit / _exit / abort / kill(自身)，以及 kill / pkill / am force-stop 命令",
                checked = cfg.bool(XpConfig.KEY_NATIVE_BLOCK_EXIT, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(XpConfig.KEY_NATIVE_BLOCK_EXIT, it) },
            )
            HintText(
                "上面这个开关需要关于页的「Native 层 Hook」开启才生效。" +
                        "开启后应用自己调用 native 退出也会被拦住，可能导致它卡在前台退不掉，按需打开。" +
                        "kill 类命令的识别方式与 Java 层保持一致。"
            )
        }

        
        FeatureCard(
            title = "异常捕获器",
            subtitle = "Hook 默认未捕获异常处理器，记录崩溃信息或拦截闪退（默认开启）",
            checked = cfg.bool(XpConfig.KEY_CRASH_CATCH_ENABLE, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_CRASH_CATCH_ENABLE, it) }
        ) {
            val enabled = cfg.bool(XpConfig.KEY_CRASH_CATCH_ENABLE, true)
            CheckRow(
                checked = cfg.bool(XpConfig.KEY_CRASH_COPY_CLIPBOARD, false),
                onCheckedChange = { cfg.put(XpConfig.KEY_CRASH_COPY_CLIPBOARD, it) },
                title = "崩溃时复制堆栈到剪贴板",
                enabled = enabled,
            )
            CheckRow(
                checked = cfg.bool(XpConfig.KEY_CRASH_WRITE_FILE, true),
                onCheckedChange = { cfg.put(XpConfig.KEY_CRASH_WRITE_FILE, it) },
                title = "崩溃时写入应用私有目录（默认开启）",
                subtitle = "/storage/emulated/0/Android/data/{包名}/files/${XpConfig.CRASH_LOG_NAME}",
                enabled = enabled,
            )
            CheckRow(
                checked = cfg.bool(XpConfig.KEY_CRASH_INTERCEPT, false),
                onCheckedChange = { cfg.put(XpConfig.KEY_CRASH_INTERCEPT, it) },
                title = "拦截应用抛出的异常",
                subtitle = "开启后会额外接管主线程消息循环，把崩溃吃掉让应用继续跑。" +
                        "可能让应用进入不一致状态，请谨慎开启。",
                enabled = enabled,
            )
        }

        SectionTitle("痕迹清理")

        
        HidePathCard(cfg, enabled)

        SectionTitle("权限伪装")

        PermissionFakeCard(cfg, enabled)

        Spacer(Modifier.height(24.dp))
    }
}










@Composable
private fun NativeStatusBar(cfg: XpConfigState) {
    val on = cfg.bool(XpConfig.KEY_NATIVE_HOOK, XpConfig.DEF_NATIVE_HOOK)
    val soOk = remember { NativeBridge.loaded }
    val cs = MaterialTheme.colorScheme

    val (text, color) = when {
        !on -> "Native 层 Hook：已关闭（只有 Java 层生效）" to cs.onSurfaceVariant
        !soOk -> "Native 层 Hook：so 加载失败，检查 APK 是否包含 liblockperm.so" to cs.error
        else -> "Native 层 Hook：已开启" to cs.primary
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CustomPropsCard(cfg: XpConfigState, enabled: Boolean) {
    val raw = cfg.str(XpConfig.KEY_CUSTOM_PROPS, "")

    
    
    val rows = remember { mutableStateListOf<Pair<String, String>>() }
    var lastPushed by remember { mutableStateOf<String?>(null) }

    
    LaunchedEffect(raw) {
        if (raw != lastPushed) {
            rows.clear()
            rows.addAll(XpConfig.decodeProps(raw))
        }
    }

    fun commit(list: List<Pair<String, String>>) {
        val encoded = XpConfig.encodeProps(list)
        lastPushed = encoded
        cfg.put(XpConfig.KEY_CUSTOM_PROPS, encoded)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "自定义系统属性",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "左边填键，右边填值。" +
                        "同时作用于 Java 的 SystemProperties.get 和 shell 的 getprop 命令",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDividerCompat()

            if (rows.isEmpty()) {
                HintText("还没有添加任何属性。点下面的「添加一行」开始。")
            }

            rows.forEachIndexed { i, (k, v) ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedTextField(
                        value = k,
                        onValueChange = { nv ->
                            rows[i] = nv to rows[i].second
                            commit(rows)
                        },
                        label = { Text("属性名", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1.1f),
                        singleLine = true,
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedTextField(
                        value = v,
                        onValueChange = { nv ->
                            rows[i] = rows[i].first to nv
                            commit(rows)
                        },
                        label = { Text("值", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(12.dp),
                    )
                    IconButton(
                        onClick = {
                            rows.removeAt(i)
                            commit(rows)
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除这一行",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        rows.add("" to "")
                        commit(rows)
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { Text("添加一行", style = MaterialTheme.typography.labelMedium) }
                OutlinedButton(
                    onClick = {
                        rows.clear()
                        commit(rows)
                    },
                    enabled = enabled && rows.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text("全部清空", style = MaterialTheme.typography.labelMedium) }
            }

            HintText(
                "· null 表示返回空字符串\n" +
                        "· 键写 ro.product.model 这类会自动连带改 ro.product.system.model、" +
                        "vendor / product / odm / system_ext 等整套变体。\n" +
                        "· 这里填的优先级最高，会覆盖「伪装设备信息」里推导出来的同名属性。"
            )
        }
    }
}

@Composable
private fun HidePathCard(cfg: XpConfigState, enabled: Boolean) {
    val raw = cfg.str(XpConfig.KEY_HIDE_PATHS, "")
    val count = XpConfig.decodePathLines(raw).size

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "隐藏路径 / 文件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "让目标应用用任何方式都探测不到这些路径：Java 的 File.exists / listFiles、" +
                        "各种输入输出流、java.nio.file.Files，以及 shell 命令——" +
                        "命令里只要出现该路径就返回 No such file or directory 且退出码 1。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDividerCompat()
            LabeledTextField(
                label = "要隐藏的路径（每行一个）",
                value = raw,
                onValueChange = { cfg.put(XpConfig.KEY_HIDE_PATHS, it) },
                enabled = enabled,
                singleLine = false,
                maxLines = 8,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { cfg.put(XpConfig.KEY_HIDE_PATHS, "") },
                    enabled = enabled && raw.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("全部清空", style = MaterialTheme.typography.labelMedium) }
            }
            HintText(
                "当前已隐藏 $count 条。填 /sdcard/Magisk 会连它的子路径一起隐藏。" +
                        "Java 层与 native 层都会拦：open / openat / fopen / access / faccessat / " +
                        "stat / lstat / fstatat / readlink / realpath，以及 opendir + readdir " +
                        "过滤目录项；shell 命令里出现该路径则返回 No such file or directory。" +
                        "native 部分需要关于页的「Native 层 Hook」开启。"
            )
        }
    }
}

@Composable
private fun PermissionFakeCard(cfg: XpConfigState, enabled: Boolean) {
    val grant = cfg.strSet(XpConfig.KEY_PERM_GRANT, "")
    val fake = cfg.strSet(XpConfig.KEY_PERM_FAKE_DATA, "")

    fun toggled(set: Set<String>, id: String, on: Boolean): String =
        XpConfig.encodeSet(if (on) set + id else set - id)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "权限伪装",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "让应用以为你授予了权限，不再弹出申请权限。勾了哪些就伪装哪些，只对当前这个应用生效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDividerCompat()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        cfg.put(
                            XpConfig.KEY_PERM_GRANT,
                            XpConfig.encodeSet(XpConfig.ALL_GROUP_IDS)
                        )
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { Text("全选授权", style = MaterialTheme.typography.labelMedium) }
                OutlinedButton(
                    onClick = {
                        cfg.put(XpConfig.KEY_PERM_GRANT, "")
                        cfg.put(XpConfig.KEY_PERM_FAKE_DATA, "")
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) { Text("全部清空", style = MaterialTheme.typography.labelMedium) }
            }
            HintText("已选 ${grant.size} / ${XpConfig.PERM_GROUPS.size} 项")

            XpConfig.PERM_GROUPS.forEach { g ->
                SwitchRow(
                    title = "${g.label}（伪装已授权）",
                    subtitle = if (g.special) {
                        "特殊权限：靠系统接口查询，伪装成已获得（${g.perms.firstOrNull() ?: ""}）"
                    } else {
                        g.perms.joinToString("、")
                    },
                    checked = g.id in grant,
                    enabled = enabled,
                    onCheckedChange = {
                        cfg.put(XpConfig.KEY_PERM_GRANT, toggled(grant, g.id, it))
                    },
                )
                if (g.fakeData) {
                    SwitchRow(
                        title = "· ${g.label}返回伪造数据",
                        subtitle = g.fakeHint,
                        checked = g.id in fake,
                        enabled = enabled && g.id in grant,
                        onCheckedChange = {
                            cfg.put(XpConfig.KEY_PERM_FAKE_DATA, toggled(fake, g.id, it))
                        },
                    )
                }
                HorizontalDividerCompat()
            }

            HintText(
                "第 2 个开关决定\"真的用标准方法去读时拿到什么\"：勾上返回虚构内容" +
                        "（通讯录假人、存储假文件、短信假记录），不勾可能拿到空值而闪退。" +
                        "摄像头要真实画面、麦克风要真实音频做不到，所以这两项没有第 2 个开关。"
            )
        }
    }
}

@Composable
private fun SwitchColumn(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun HorizontalDividerCompat() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant
    )
}



@Composable
fun FullScreenCodeEditor(
    title: String,
    value: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by remember(value) { mutableStateOf(value) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onConfirm(draft) }) { Text("保存") }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}



private object UaPresets {
    private val androidVersions = listOf("10", "11", "12", "13", "14", "15", "16")
    private val devices = listOf(
        "Pixel 5", "Pixel 6", "Pixel 7", "Pixel 8", "Pixel 9",
        "SM-G991B", "SM-S908B", "Mi 11", "M2101K9C",
        "ONEPLUS A6000", "CPH2451", "V2312DA"
    )
    private val chromeVersions = listOf(
        "118.0.0.0", "119.0.0.0", "120.0.0.0", "121.0.0.0",
        "122.0.0.0", "123.0.0.0", "124.0.0.0"
    )

    fun random(): String {
        val android = androidVersions.random()
        val device = devices.random()
        val chrome = chromeVersions.random()
        return "Mozilla/5.0 (Linux; Android $android; $device) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$chrome Mobile Safari/537.36"
    }
}
