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

            HorizontalDividerCompat()
            Text(
                "CPU 伪装",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val cpuOn = cfg.bool(XpConfig.KEY_FAKE_CPU_ENABLE, false)
            SwitchRow(
                title = "伪装 /proc/cpuinfo 与 CPU 信息",
                checked = cpuOn,
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_CPU_ENABLE, it) },
            )
            val cpuMode = cfg.str(XpConfig.KEY_FAKE_CPU_MODE, XpConfig.CPU_MODE_PRESET)
            SingleSelectChips(
                options = listOf("预设", "自定义"),
                selectedIndex = if (cpuMode == XpConfig.CPU_MODE_CUSTOM) 1 else 0,
                onSelect = { cfg.put(XpConfig.KEY_FAKE_CPU_MODE, if (it == 1) XpConfig.CPU_MODE_CUSTOM else XpConfig.CPU_MODE_PRESET) },
                enabled = buildOn && cpuOn,
            )
            if (cpuMode == XpConfig.CPU_MODE_CUSTOM) {
                LabeledTextField(
                    label = "/proc/cpuinfo 完整内容",
                    value = cfg.str(XpConfig.KEY_FAKE_CPU_CUSTOM, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_FAKE_CPU_CUSTOM, it) },
                    enabled = buildOn && cpuOn,
                    singleLine = false,
                    maxLines = 12,
                )
                HintText("填写后完全替换 /proc/cpuinfo 的内容，包含 Java 读取、cat 命令与原生层。")
            } else {
                SingleSelectChips(
                    options = XpConfig.cpuPresetNames(),
                    selectedIndex = cfg.int(XpConfig.KEY_FAKE_CPU_PRESET, 0)
                        .coerceIn(0, XpConfig.CPU_PRESETS.lastIndex),
                    onSelect = { cfg.put(XpConfig.KEY_FAKE_CPU_PRESET, it) },
                    enabled = buildOn && cpuOn,
                )
                LabeledTextField(
                    label = "核心数",
                    value = cfg.int(XpConfig.KEY_FAKE_CPU_CORES, 8).toString(),
                    onValueChange = { raw ->
                        val v = raw.filter { it.isDigit() }.take(2).toIntOrNull() ?: 8
                        cfg.put(XpConfig.KEY_FAKE_CPU_CORES, v.coerceIn(1, 32))
                    },
                    enabled = buildOn && cpuOn,
                )
            }
            SwitchRow(
                title = "伪装设备温度",
                checked = cfg.bool(XpConfig.KEY_FAKE_TEMP_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_TEMP_ENABLE, it) },
            )
            SwitchRow(
                title = "伪装电量",
                checked = cfg.bool(XpConfig.KEY_FAKE_BATTERY_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_BATTERY_ENABLE, it) },
            )
            SwitchRow(
                title = "伪装开发者选项已关闭",
                checked = cfg.bool(XpConfig.KEY_FAKE_DEV_OFF, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_DEV_OFF, it) },
            )
            SwitchRow(
                title = "伪装获取系统时间",
                checked = cfg.bool(XpConfig.KEY_FAKE_TIME_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_TIME_ENABLE, it) },
            )
            SwitchRow(
                title = "伪装设备已运行时间",
                checked = cfg.bool(XpConfig.KEY_FAKE_UPTIME_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_UPTIME_ENABLE, it) },
            )
            HintText("低版本伪装为高版本可能会闪退。")

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
                label = "设备名",
                value = cfg.str(XpConfig.KEY_DEVICE_NAME, ""),
                onValueChange = { cfg.put(XpConfig.KEY_DEVICE_NAME, it) },
                enabled = buildOn,
            )
            SwitchRow(
                title = "隐藏账号与邮箱",
                checked = cfg.bool(XpConfig.KEY_HIDE_ACCOUNTS, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_HIDE_ACCOUNTS, it) },
            )
        }

        
        FeatureCard(
            title = "注入 WebView JavaScript",
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
        }

        SectionTitle("系统属性")

        
        CustomPropsCard(cfg, enabled)

        SectionTitle("Root 与网络痕迹")

        
        FeatureCard(
            title = "伪装 Root（命令返回成功）",
            checked = cfg.bool(XpConfig.KEY_ROOT_FAKE_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_ROOT_FAKE_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_ROOT_FAKE_ENABLE, false)
            SwitchRow(
                title = "伪装 su 文件存在",
                checked = cfg.bool(XpConfig.KEY_ROOT_FAKE_FILE, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_ROOT_FAKE_FILE, it) },
            )
            SwitchRow(
                title = "屏蔽 Permission denied 并强制成功",
                checked = cfg.bool(XpConfig.KEY_ROOT_FAKE_MASK, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_ROOT_FAKE_MASK, it) },
            )
        }

        
        FeatureCard(
            title = "隐藏 VPN / 抓包代理",
            checked = cfg.bool(XpConfig.KEY_VPN_HIDE_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_VPN_HIDE_ENABLE, false)
            SwitchRow(
                title = "隐藏 VPN 网卡接口",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_IFACE, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_IFACE, it) },
            )
            SwitchRow(
                title = "剥离 VPN 传输能力",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_CAPS, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_CAPS, it) },
            )
            SwitchRow(
                title = "修正旧版 NetworkInfo 类型",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_NETINFO, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_NETINFO, it) },
            )
            SwitchRow(
                title = "隐藏 HTTP 代理（抓包）",
                checked = cfg.bool(XpConfig.KEY_VPN_HIDE_PROXY, true),
                enabled = on,
                onCheckedChange = { cfg.put(XpConfig.KEY_VPN_HIDE_PROXY, it) },
            )
            SwitchRow(
                title = "清空 VPN 相关系统设置",
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
        }

        SectionTitle("稳定性")

        
        FeatureCard(
            title = "阻止闪退 / 自杀",
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
                onSelect = { lvl ->
                    cfg.put(XpConfig.KEY_BLOCK_CRASH_LEVEL, lvl)
                    
                    
                    HookCrashBlocker.presetFor(lvl).forEach { (k, v) -> cfg.put(k, v) }
                },
                enabled = enabled,
            )
            HintText(
                when (cfg.int(XpConfig.KEY_BLOCK_CRASH_LEVEL, 1)) {
                    0 -> "低：只拦进程自杀（killProcess / killProcessQuiet / killProcessGroup）。风险最小。"
                    1 -> "中（推荐）：再加退出类（System.exit / Runtime.exit / halt / VMRuntime.exit / " +
                            "Os._exit）、信号类（sendSignal / Os.kill / Os.killpg / Signal.raise）、" +
                            "以及命令层的 kill 系列。大多数应用够用。"
                    2 -> "高：再加系统服务杀进程（ActivityManager / AMS 的 " +
                            "killBackgroundProcesses、forceStopPackage）、Debug.waitForDebugger，" +
                            "以及吞掉消息循环里的异常。"
                    else -> "极高：再加吞掉未捕获异常、拦住退后台与结束任务，以及线程守护。"
                } +
                        "下面每个开关都可以单独改，改完以开关为准。"
            )

            HorizontalDividerCompat()
            Text(
                "分项开关",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            SwitchRow(
                title = "进程自杀",
                checked = cfg.bool(HookCrashBlocker.KEY_SELF_PROC, true),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_SELF_PROC, it) },
            )
            SwitchRow(
                title = "退出指令",
                checked = cfg.bool(HookCrashBlocker.KEY_EXIT, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_EXIT, it) },
            )
            SwitchRow(
                title = "信号",
                checked = cfg.bool(HookCrashBlocker.KEY_SIGNAL, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_SIGNAL, it) },
            )
            SwitchRow(
                title = "杀死命令",
                checked = cfg.bool(HookCrashBlocker.KEY_CMD, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_CMD, it) },
            )
            SwitchRow(
                title = "系统服务杀进程",
                checked = cfg.bool(HookCrashBlocker.KEY_AM, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_AM, it) },
            )
            SwitchRow(
                title = "吞掉未捕获异常",
                checked = cfg.bool(HookCrashBlocker.KEY_UNCAUGHT, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_UNCAUGHT, it) },
            )
            SwitchRow(
                title = "吞掉消息循环异常",
                checked = cfg.bool(HookCrashBlocker.KEY_UI, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_UI, it) },
            )
            SwitchRow(
                title = "退后台 / 结束任务",
                checked = cfg.bool(HookCrashBlocker.KEY_TASK, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_TASK, it) },
            )
            SwitchRow(
                title = "阻止等待调试器",
                checked = cfg.bool(HookCrashBlocker.KEY_DEBUGGER, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_DEBUGGER, it) },
            )
            SwitchRow(
                title = "线程守护",
                checked = cfg.bool(HookCrashBlocker.KEY_THREAD, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(HookCrashBlocker.KEY_THREAD, it) },
            )
            HorizontalDividerCompat()
            Text(
                "线程守护参数",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            val threadOn = enabled && cfg.bool(HookCrashBlocker.KEY_THREAD, false)
            LabeledTextField(
                label = "每秒允许新建的线程数（0 = 不限）",
                value = cfg.int(HookCrashBlocker.KEY_THREAD_PER_SEC, HookCrashBlocker.DEF_THREAD_PER_SEC)
                    .toString(),
                enabled = threadOn,
                onValueChange = { raw ->
                    val v = raw.filter { it.isDigit() }.take(6).toIntOrNull() ?: 0
                    cfg.put(HookCrashBlocker.KEY_THREAD_PER_SEC, v)
                },
            )
            LabeledTextField(
                label = "允许的并发线程上限（0 = 不限）",
                value = cfg.int(HookCrashBlocker.KEY_THREAD_MAX, HookCrashBlocker.DEF_THREAD_MAX)
                    .toString(),
                enabled = threadOn,
                onValueChange = { raw ->
                    val v = raw.filter { it.isDigit() }.take(6).toIntOrNull() ?: 0
                    cfg.put(HookCrashBlocker.KEY_THREAD_MAX, v)
                },
            )
            LabeledTextField(
                label = "超过上限百分之多少就回收（0-100）",
                value = cfg.int(HookCrashBlocker.KEY_THREAD_PCT, HookCrashBlocker.DEF_THREAD_PCT)
                    .toString(),
                enabled = threadOn,
                onValueChange = { raw ->
                    val v = raw.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
                    cfg.put(HookCrashBlocker.KEY_THREAD_PCT, v.coerceIn(0, 100))
                },
            )
            HintText(
                "回收方式是 interrupt 最早的那些线程（Android 上没有安全的 Thread.stop）。" +
                        "线程守护很激进，拦掉的线程创建会让应用出现卡顿或功能缺失，默认关闭，只在需要时开。"
            )
            HorizontalDividerCompat()
            SwitchRow(
                title = "拦截原生层退出指令",
                checked = cfg.bool(XpConfig.KEY_NATIVE_BLOCK_EXIT, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(XpConfig.KEY_NATIVE_BLOCK_EXIT, it) },
            )
        }

        
        FeatureCard(
            title = "异常捕获器",
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
                enabled = enabled,
            )
            CheckRow(
                checked = cfg.bool(XpConfig.KEY_CRASH_INTERCEPT, false),
                onCheckedChange = { cfg.put(XpConfig.KEY_CRASH_INTERCEPT, it) },
                title = "拦截应用抛出的异常（实验性）",
                enabled = enabled,
            )
        }

        SectionTitle("痕迹清理")

        
        HidePathCard(cfg, enabled)

        SectionTitle("权限伪装")

        PermissionFakeCard(cfg, enabled)

        SectionTitle("原生层")

        
        
        NativeAppModeCard(cfg, enabled)

        Spacer(Modifier.height(24.dp))
    }
}



















@Composable
fun NativeStatusBar(cfg: XpConfigState) {
    val (active, hint) = XpConfig.nativeResolution(cfg)
    val soOk = remember { NativeBridge.loaded }
    val cs = MaterialTheme.colorScheme

    val color = when {
        !active -> cs.onSurfaceVariant
        !soOk -> cs.error
        else -> cs.primary
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
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
    if (!soOk && active) {
        Text(
            "so 加载失败，请检查 APK 是否完整",
            style = MaterialTheme.typography.bodySmall,
            color = cs.error,
        )
    }
}







@Composable
fun NativeAppModeCard(cfg: XpConfigState, enabled: Boolean) {
    val globalOn = cfg.bool(XpConfig.KEY_NATIVE_HOOK, XpConfig.DEF_NATIVE_HOOK)
    val mode = cfg.int(XpConfig.KEY_NATIVE_APP_MODE, XpConfig.NATIVE_APP_DEFAULT)
    val cs = MaterialTheme.colorScheme

    FeatureCard(
        title = "本应用的原生层钩子",
        checked = mode != XpConfig.NATIVE_APP_OFF,
        enabled = enabled,
        onCheckedChange = {
            cfg.put(
                XpConfig.KEY_NATIVE_APP_MODE,
                if (it) XpConfig.NATIVE_APP_ON else XpConfig.NATIVE_APP_OFF
            )
        },
    ) {
        Text(
            "针对当前这一个应用单独设置，不影响其它应用。",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
        SingleSelectChips(
            options = listOf("跟随全局", "强制开启", "强制关闭"),
            selectedIndex = mode,
            enabled = enabled,
            onSelect = { cfg.put(XpConfig.KEY_NATIVE_APP_MODE, it) },
        )
        Text(
            buildString {
                append("当前为")
                append(if (globalOn) "开启" else "关闭")
            },
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
        HorizontalDividerCompat()
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
                "左边填键，右边填值。",
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
                "· 值填 null 表示返回空字符串。\n" +
                        "· 这里优先级最高，会覆盖「伪装设备信息」推导出的同名属性。"
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
                "部分生效",
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
                "当前已隐藏 $count 条。"
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
                "让应用读取权限状态时返回已授予，不再弹出申请。勾选哪些即伪装哪些，仅对当前应用生效。",
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
                    checked = g.id in grant,
                    enabled = enabled,
                    onCheckedChange = {
                        cfg.put(XpConfig.KEY_PERM_GRANT, toggled(grant, g.id, it))
                    },
                )
                Text(
                    g.perms.joinToString(" / ") {
                        it.removePrefix("android.permission.")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (g.fakeData) {
                    SwitchRow(
                        title = "· ${g.label}返回伪造数据",
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
                "第 2 个开关决定「真去读时拿到什么」：勾上返回虚构内容（通讯录假人、存储假文件、短信假记录），不勾可能空值闪退。" +
                        "摄像头、麦克风无伪装，故无此开关。"
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
