package io.github.sunilxsk.lockperm

import java.util.UUID
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.libxposed.service.XposedService



@Composable
fun DisguiseConfigContent(
    cfg: XpConfigState,
    enabled: Boolean,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NativeStatusBar(cfg)

        SectionTitle("身份伪装")



        
        var pendingVersion by remember { mutableStateOf<Pair<String, Int>?>(null) }
        if (pendingVersion != null) {
            val (r, sv) = pendingVersion!!
            val realSdkNow = runCatching { android.os.Build.VERSION.SDK_INT }.getOrDefault(0)
            AlertDialog(
                onDismissRequest = { pendingVersion = null },
                title = { Text("伪装成更高的系统版本？") },
                text = {
                    Text(
                        "你要伪装成 Android $r（SDK $sv），而本机只有 SDK $realSdkNow。\n\n" +
                            "整个 Android 生态都按 SDK_INT 做版本分支，伪装成更高的版本后，" +
                            "目标应用会去调用你这台机器上根本不存在的接口，最常见的表现是\n" +
                            "NoSuchFieldError\n\n" +
                            "然后一进应用就闪退或卡死。确定要用吗？出问题把版本改回不高于本机即可。"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        cfg.put(XpConfig.KEY_BUILD_RELEASE, r)
                        cfg.put(XpConfig.KEY_FAKE_SDK_INT, sv)
                        pendingVersion = null
                    }) { Text("仍然使用") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingVersion = null }) { Text("取消") }
                },
            )
        }

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
                "eSIM / 移动数据 / 漫游",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SwitchRow(
                title = "伪装支持 eSIM",
                subtitle = "EuiccManager.isEnabled() 返回 true",
                checked = cfg.bool(XpConfig.KEY_FAKE_SIM_ESIM, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_SIM_ESIM, it) },
            )
            SwitchRow(
                title = "伪装已有 eSIM",
                subtitle = "激活的订阅数量 +1",
                checked = cfg.bool(XpConfig.KEY_FAKE_SIM_ESIM_ACTIVE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_SIM_ESIM_ACTIVE, it) },
            )
            val dataOn = cfg.bool(XpConfig.KEY_FAKE_SIM_DATA, false)
            
                
                
                
                
            
                
            
            SwitchRow(
                title = "伪装漫游状态",
                subtitle = "开启后按下面的开关决定「是否漫游」",
                checked = cfg.bool(XpConfig.KEY_FAKE_SIM_ROAM_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_SIM_ROAM_ENABLE, it) },
            )
            if (cfg.bool(XpConfig.KEY_FAKE_SIM_ROAM_ENABLE, false)) {
                SingleSelectChips(
                    options = listOf("未漫游", "漫游中"),
                    selectedIndex = if (cfg.bool(XpConfig.KEY_FAKE_SIM_ROAM, false)) 1 else 0,
                    onSelect = { i -> cfg.put(XpConfig.KEY_FAKE_SIM_ROAM, i == 1) },
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
                "Android 版本",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            val curRelease = cfg.str(XpConfig.KEY_BUILD_RELEASE, "")
            val curSdk = cfg.int(XpConfig.KEY_FAKE_SDK_INT, 0)
            val realSdk = runCatching { android.os.Build.VERSION.SDK_INT }.getOrDefault(0)
            
            
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
                        val (r, sv) = XpConfig.ANDROID_VERSIONS[i - 1]
                        if (realSdk > 0 && sv > realSdk) {
                            
                            pendingVersion = r to sv
                        } else {
                            cfg.put(XpConfig.KEY_BUILD_RELEASE, r)
                            cfg.put(XpConfig.KEY_FAKE_SDK_INT, sv)
                        }
                    }
                },
            )
            HintText("本机是 Android $realSdk。伪装成更高的版本有风险，选到时会先提示一次。")
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
                title = "伪装小部分 CPU 信息",
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
                    label = "/proc/cpuinfo 内容",
                    value = cfg.str(XpConfig.KEY_FAKE_CPU_CUSTOM, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_FAKE_CPU_CUSTOM, it) },
                    enabled = buildOn && cpuOn,
                    singleLine = false,
                    maxLines = 12,
                )

            } else {
                SingleSelectChips(
                    options = XpConfig.cpuPresetNames(),
                    selectedIndex = cfg.int(XpConfig.KEY_FAKE_CPU_PRESET, 0)
                        .coerceIn(0, XpConfig.CPU_PRESETS.lastIndex),
                    onSelect = {
                        cfg.put(XpConfig.KEY_FAKE_CPU_PRESET, it)
                        
                        
                        cfg.put(XpConfig.KEY_FAKE_CPU_ENABLE, true)
                        
                        XpConfig.CPU_PRESETS.getOrNull(it)?.let { p ->
                            val n = p.clusters.sumOf { c -> c.cores }.coerceIn(1, 32)
                            cfg.put(XpConfig.KEY_FAKE_CPU_CORES, n)
                        }
                    },
                    enabled = buildOn,
                )
                
                    
                    
                    
                        
                        
                    
                    
                
            }
            HorizontalDividerCompat()
            Text(
                "每核频率（kHz，可留空）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            LabeledTextField(
                label = "最低频率 min_freq",
                value = cfg.str(XpConfig.KEY_FAKE_CPU_MIN_FREQ, ""),
                onValueChange = { cfg.put(XpConfig.KEY_FAKE_CPU_MIN_FREQ, it) },
                enabled = buildOn && cpuOn,
                singleLine = false,
                maxLines = 2,
            )
            LabeledTextField(
                label = "最高频率 max_freq",
                value = cfg.str(XpConfig.KEY_FAKE_CPU_MAX_FREQ, ""),
                onValueChange = { cfg.put(XpConfig.KEY_FAKE_CPU_MAX_FREQ, it) },
                enabled = buildOn && cpuOn,
                singleLine = false,
                maxLines = 2,
            )
            LabeledTextField(
                label = "当前频率 scaling_cur_freq",
                value = cfg.str(XpConfig.KEY_FAKE_CPU_CUR_FREQ, ""),
                onValueChange = { cfg.put(XpConfig.KEY_FAKE_CPU_CUR_FREQ, it) },
                enabled = buildOn && cpuOn,
                singleLine = false,
                maxLines = 2,
            )
            cpuFreqPreview(cfg, cpuOn)
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
            
            val drainOn = cfg.bool(XpConfig.KEY_FAKE_BATTERY_DRAIN, false)
            SwitchRow(
                title = "自动掉电（更真实）",
                subtitle = "按设定间隔往下掉，重启目标应用后从设定值重新开始",
                checked = drainOn,
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_BATTERY_DRAIN, it) },
            )
            if (drainOn) {
                LabeledTextField(
                    label = "每多少分钟掉 1 格电",
                    value = cfg.int(XpConfig.KEY_FAKE_BATTERY_DRAIN_MIN, 5).toString(),
                    onValueChange = { raw ->
                        cfg.put(
                            XpConfig.KEY_FAKE_BATTERY_DRAIN_MIN,
                            raw.filter { it.isDigit() }.take(4).toIntOrNull()
                                ?.coerceIn(1, 1440) ?: 5
                        )
                    },
                    enabled = buildOn,
                )
                HintText(
                    "按目标进程启动后经过的时间算。比如设 5 分钟、起始 80%，" +
                        "运行 20 分钟后就会报 76%。"
                )
            }
            HorizontalDividerCompat()
            SwitchRow(
                title = "伪装电池细节（充电状态 / 电压 / 设计容量）",
                checked = cfg.bool(XpConfig.KEY_FAKE_BATEX_ENABLE, false),
                enabled = buildOn,
                onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_BATEX_ENABLE, it) },
            )
            if (cfg.bool(XpConfig.KEY_FAKE_BATEX_ENABLE, false)) {
                val batOn = buildOn
                val statuses = listOf("充电中", "放电中", "已充满", "未充电")
                val values = XpConfig.BAT_STATUSES
                Text(
                    "充电状态",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                SingleSelectChips(
                    options = statuses,
                    selectedIndex = values.indexOf(
                        cfg.str(XpConfig.KEY_FAKE_BAT_STATUS, XpConfig.DEF_BAT_STATUS)
                    ).coerceAtLeast(0),
                    onSelect = { i -> cfg.put(XpConfig.KEY_FAKE_BAT_STATUS, values[i]) },
                    enabled = batOn,
                )
                LabeledTextField(
                    label = "电压（mV）",
                    value = cfg.int(
                        XpConfig.KEY_FAKE_BAT_VOLTAGE_MV, XpConfig.DEF_BAT_VOLTAGE_MV
                    ).toString(),
                    onValueChange = { raw ->
                        cfg.put(
                            XpConfig.KEY_FAKE_BAT_VOLTAGE_MV,
                            raw.filter { it.isDigit() }.take(5).toIntOrNull()
                                ?.coerceIn(2000, 5000) ?: XpConfig.DEF_BAT_VOLTAGE_MV
                        )
                    },
                    enabled = batOn,
                )
                LabeledTextField(
                    label = "电池设计容量（mAh）",
                    value = cfg.int(
                        XpConfig.KEY_FAKE_BAT_DESIGN_MAH, XpConfig.DEF_BAT_DESIGN_MAH
                    ).toString(),
                    onValueChange = { raw ->
                        cfg.put(
                            XpConfig.KEY_FAKE_BAT_DESIGN_MAH,
                            raw.filter { it.isDigit() }.take(6).toIntOrNull()
                                ?.coerceIn(100, 20000) ?: XpConfig.DEF_BAT_DESIGN_MAH
                        )
                    },
                    enabled = batOn,
                )
                HorizontalDividerCompat()
            }
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
            HintText("Android低版本伪装为高版本可能会闪退。")

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

        SectionTitle("显示")

        
        FeatureCard(
            title = "修改应用 DPI",
            subtitle = " ",
            checked = cfg.bool(XpConfig.KEY_FAKE_DPI_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_DPI_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_FAKE_DPI_ENABLE, false)
            var dpi by remember { mutableIntStateOf(cfg.int(XpConfig.KEY_FAKE_DPI, XpConfig.DEF_DPI)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$dpi dpi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp),
                )
                Slider(
                    value = dpi.toFloat(),
                    onValueChange = {
                        dpi = it.toInt().coerceIn(XpConfig.MIN_DPI, XpConfig.MAX_DPI)
                        cfg.put(XpConfig.KEY_FAKE_DPI, dpi)
                    },
                    valueRange = XpConfig.MIN_DPI.toFloat()..XpConfig.MAX_DPI.toFloat(),
                    modifier = Modifier.weight(1f),
                    enabled = on,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("小 320", "默认 420", "大 480", "超大 560").forEachIndexed { i, label ->
                    val v = when (i) { 0 -> 320; 1 -> 420; 2 -> 480; else -> 560 }
                    OutlinedButton(
                        onClick = {
                            dpi = v
                            cfg.put(XpConfig.KEY_FAKE_DPI, v)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = on,
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }

        }

        
        FeatureCard(
            title = "伪装屏幕分辨率与刷新率",
            subtitle = " ",
            checked = cfg.bool(XpConfig.KEY_FAKE_DISPLAY_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_DISPLAY_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_FAKE_DISPLAY_ENABLE, false)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledTextField(
                    label = "宽（px）",
                    value = cfg.int(XpConfig.KEY_FAKE_RES_W, XpConfig.DEF_RES_W).toString(),
                    onValueChange = { raw ->
                        cfg.put(
                            XpConfig.KEY_FAKE_RES_W,
                            raw.filter { it.isDigit() }.take(5).toIntOrNull()
                                ?.coerceIn(240, 7680) ?: XpConfig.DEF_RES_W
                        )
                    },
                    enabled = buildOn && on,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = "高（px）",
                    value = cfg.int(XpConfig.KEY_FAKE_RES_H, XpConfig.DEF_RES_H).toString(),
                    onValueChange = { raw ->
                        cfg.put(
                            XpConfig.KEY_FAKE_RES_H,
                            raw.filter { it.isDigit() }.take(5).toIntOrNull()
                                ?.coerceIn(240, 7680) ?: XpConfig.DEF_RES_H
                        )
                    },
                    enabled = buildOn && on,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1080×2400" to (1080 to 2400), "1440×3200" to (1440 to 3200),
                    "1080×2340" to (1080 to 2340)).forEach { (label, pair) ->
                    OutlinedButton(
                        onClick = {
                            cfg.put(XpConfig.KEY_FAKE_RES_W, pair.first)
                            cfg.put(XpConfig.KEY_FAKE_RES_H, pair.second)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = buildOn && on,
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }
            HorizontalDividerCompat()
            LabeledTextField(
                label = "当前刷新率（Hz）",
                value = cfg.str(XpConfig.KEY_FAKE_REFRESH, XpConfig.DEF_REFRESH.toString()),
                onValueChange = { raw ->
                    val v = raw.filter { it.isDigit() || it == '.' }
                    if (v.toFloatOrNull() != null) cfg.put(XpConfig.KEY_FAKE_REFRESH, v)
                },
                enabled = buildOn && on,
            )
            LabeledTextField(
                label = "支持的刷新率（逗号分隔）",
                value = cfg.str(XpConfig.KEY_FAKE_REFRESH_LIST, XpConfig.DEF_REFRESH_LIST),
                onValueChange = { cfg.put(XpConfig.KEY_FAKE_REFRESH_LIST, it) },
                enabled = buildOn && on,
            )

        }

        
        FeatureCard(
            title = "伪装内存与存储",
            subtitle = "总内存 / 可用内存 / 存储总容量 / 可用容量",
            checked = cfg.bool(XpConfig.KEY_FAKE_MEM2_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_MEM2_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_FAKE_MEM2_ENABLE, false)
            LabeledTextField(
                label = "总运行内存（MB）",
                value = cfg.int(XpConfig.KEY_FAKE_MEM_TOTAL_MB, XpConfig.DEF_MEM_TOTAL_MB).toString(),
                onValueChange = { raw ->
                    cfg.put(
                        XpConfig.KEY_FAKE_MEM_TOTAL_MB,
                        raw.filter { it.isDigit() }.take(7).toIntOrNull()
                            ?.coerceIn(256, 262144) ?: XpConfig.DEF_MEM_TOTAL_MB
                    )
                },
                enabled = buildOn && on,
            )
            LabeledTextField(
                label = "可用运行内存（MB）",
                value = cfg.int(XpConfig.KEY_FAKE_MEM_AVAIL_MB, XpConfig.DEF_MEM_AVAIL_MB).toString(),
                onValueChange = { raw ->
                    cfg.put(
                        XpConfig.KEY_FAKE_MEM_AVAIL_MB,
                        raw.filter { it.isDigit() }.take(7).toIntOrNull()
                            ?.coerceIn(0, 262144) ?: XpConfig.DEF_MEM_AVAIL_MB
                    )
                },
                enabled = buildOn && on,
            )
            LabeledTextField(
                label = "存储总容量（GB）",
                value = cfg.int(XpConfig.KEY_FAKE_STOR_TOTAL_GB, XpConfig.DEF_STOR_TOTAL_GB).toString(),
                onValueChange = { raw ->
                    cfg.put(
                        XpConfig.KEY_FAKE_STOR_TOTAL_GB,
                        raw.filter { it.isDigit() }.take(5).toIntOrNull()
                            ?.coerceIn(1, 8192) ?: XpConfig.DEF_STOR_TOTAL_GB
                    )
                },
                enabled = buildOn && on,
            )
            LabeledTextField(
                label = "存储可用容量（GB）",
                value = cfg.int(XpConfig.KEY_FAKE_STOR_AVAIL_GB, XpConfig.DEF_STOR_AVAIL_GB).toString(),
                onValueChange = { raw ->
                    cfg.put(
                        XpConfig.KEY_FAKE_STOR_AVAIL_GB,
                        raw.filter { it.isDigit() }.take(5).toIntOrNull()
                            ?.coerceIn(0, 8192) ?: XpConfig.DEF_STOR_AVAIL_GB
                    )
                },
                enabled = buildOn && on,
            )
        }

        
        FeatureCard(
            title = "伪装相机分辨率",
            subtitle = "按像素数生成前后摄支持的拍照尺寸",
            checked = cfg.bool(XpConfig.KEY_FAKE_CAM_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_CAM_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_FAKE_CAM_ENABLE, false)
            LabeledTextField(
                label = "后置主摄（MP）",
                value = cfg.int(XpConfig.KEY_FAKE_CAM_BACK_MP, XpConfig.DEF_CAM_BACK_MP).toString(),
                onValueChange = { raw ->
                    cfg.put(
                        XpConfig.KEY_FAKE_CAM_BACK_MP,
                        raw.filter { it.isDigit() }.take(4).toIntOrNull()
                            ?.coerceIn(1, 400) ?: XpConfig.DEF_CAM_BACK_MP
                    )
                },
                enabled = buildOn && on,
            )
            LabeledTextField(
                label = "前置摄像头（MP）",
                value = cfg.int(XpConfig.KEY_FAKE_CAM_FRONT_MP, XpConfig.DEF_CAM_FRONT_MP).toString(),
                onValueChange = { raw ->
                    cfg.put(
                        XpConfig.KEY_FAKE_CAM_FRONT_MP,
                        raw.filter { it.isDigit() }.take(4).toIntOrNull()
                            ?.coerceIn(1, 400) ?: XpConfig.DEF_CAM_FRONT_MP
                    )
                },
                enabled = buildOn && on,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("50MP" to 50, "108MP" to 108, "200MP" to 200).forEach { (label, v) ->
                    OutlinedButton(
                        onClick = { cfg.put(XpConfig.KEY_FAKE_CAM_BACK_MP, v) },
                        modifier = Modifier.weight(1f),
                        enabled = buildOn && on,
                    ) { Text("后置 $label", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }

        SectionTitle("网络身份")

        

        

        FeatureCard(
            title = "伪装内网IP",
            subtitle = "部分生效 ",
            checked = cfg.bool(XpConfig.KEY_FAKE_NETP_ENABLE, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_NETP_ENABLE, it) }
        ) {
            val on = cfg.bool(XpConfig.KEY_FAKE_NETP_ENABLE, false)
            val en = buildOn && on
            LabeledTextField(
                label = "内网 IPv4",
                value = cfg.str(XpConfig.KEY_FAKE_IPV4, ""),
                onValueChange = { cfg.put(XpConfig.KEY_FAKE_IPV4, it) },
                enabled = en,
            )
            LabeledTextField(
                label = "IPv6",
                value = cfg.str(XpConfig.KEY_FAKE_IPV6, ""),
                onValueChange = { cfg.put(XpConfig.KEY_FAKE_IPV6, it) },
                enabled = en,
                singleLine = false,
                maxLines = 2,
            )

        }


        SectionTitle("伪装设备拥有 Google / 华为服务")

        GmsSpoofCard(cfg)
        HmsSpoofCard(cfg)
        LocationSpoofCard(cfg)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun LocationSpoofCard(cfg: XpConfigState) {
    val on = cfg.bool(XpConfig.KEY_LOC_ENABLE, false)
    val mode = cfg.int(XpConfig.KEY_LOC_MODE, 0)

    FeatureCard(
        title = "位置与基站伪装",
        checked = on,
        onCheckedChange = { cfg.put(XpConfig.KEY_LOC_ENABLE, it) }
    ) {
        HintText(
            "GPS、网络定位、GMS 融合定位、" +
                "电话服务的基站信息、电话与短信存储。"
        )

        Text("坐标来源", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        SingleSelectChips(
            options = listOf("随机城市", "自定义坐标"),
            selectedIndex = mode.coerceIn(0, 1),
            onSelect = { cfg.put(XpConfig.KEY_LOC_MODE, it) },
            enabled = on,
        )
        if (mode == 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledTextField(
                    label = "纬度",
                    value = cfg.str(XpConfig.KEY_LOC_LAT, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_LOC_LAT, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = "经度",
                    value = cfg.str(XpConfig.KEY_LOC_LON, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_LOC_LON, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledTextField(
                    label = "海拔（米）",
                    value = cfg.str(XpConfig.KEY_LOC_ALT, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_LOC_ALT, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = "精度（米）",
                    value = cfg.str(XpConfig.KEY_LOC_ACC, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_LOC_ACC, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
            }
            HintText("留空则用 0 / 10。填错不会崩，只影响报出去的数值。")
        }

        HorizontalDividerCompat()
        SwitchRow(
            title = "改写所有 Location 对象的字段读取",
            subtitle = "不管坐标是回调推来的、getLastKnownLocation 取的、还是别处传来的，读出来都是伪装值",
            checked = cfg.bool(XpConfig.KEY_LOC_FIELDS, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_LOC_FIELDS, it) },
        )

        HorizontalDividerCompat()
        Text("拦截通道", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        SwitchRow(
            title = "GPS 定位",
            subtitle = "含 GNSS 原始数据：卫星状态、测量值、NMEA（能反推真实位置，单独堵）",
            checked = cfg.bool(XpConfig.KEY_LOC_GPS, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_LOC_GPS, it) },
        )
        SwitchRow(
            title = "网络定位",
            subtitle = "含处理 WiFi 扫描结果（附近热点的 SSID/BSSID 可反查位置）",
            checked = cfg.bool(XpConfig.KEY_LOC_NET, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_LOC_NET, it) },
        )
        if (cfg.bool(XpConfig.KEY_LOC_NET, true)) {
            val wifiFakeOn = cfg.bool(XpConfig.KEY_WIFI_FAKE_ENABLE, false)
            val wifiScanOn = cfg.bool(XpConfig.KEY_WIFI_FAKE_SCAN, true)
            val wifiListFilled = cfg.str(XpConfig.KEY_WIFI_FAKE_LIST, "").isNotBlank()
            val takenOver = wifiFakeOn && wifiScanOn && wifiListFilled
            HintText(
                if (takenOver) {
                    "WiFi 扫描结果：已交给「防护功能 → 伪装 WiFi 连接状态与列表」，" +
                        "会用那里填的热点清单作为结果，这里不再清空。"
                } else {
                    "WiFi 扫描结果：当前直接清空。若在「防护功能 → 网络功能」里开了" +
                        "「伪装 WiFi 连接状态与列表」并填写了热点清单，这里就不会清空，" +
                        "而是用那边填的数据作为扫描结果。"
                }
            )
        }
        SwitchRow(
            title = "融合定位（GMS Fused）",
            checked = cfg.bool(XpConfig.KEY_LOC_FUSED, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_LOC_FUSED, it) },
        )
        SwitchRow(
            title = "电话服务（基站 / 运营商）",
            checked = cfg.bool(XpConfig.KEY_LOC_TELEPHONY, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_LOC_TELEPHONY, it) },
        )
        SwitchRow(
            title = "电话与短信存储",
            subtitle = "挡掉 content://sms、mms、call_log 的查询，会连带影响短信类功能",
            checked = cfg.bool(XpConfig.KEY_LOC_STORE, false),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_LOC_STORE, it) },
        )

        HorizontalDividerCompat()
        SwitchRow(
            title = "伪装基站信息",
            checked = cfg.bool(XpConfig.KEY_LOC_CELL, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_LOC_CELL, it) },
        )
        if (cfg.bool(XpConfig.KEY_LOC_CELL, true)) {
            HintText(
                "留空随机生成一套看起来合理的，但位置不对应。"
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledTextField(
                    label = "MCC（国家码）",
                    value = cfg.str(XpConfig.KEY_CELL_MCC, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_CELL_MCC, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = "MNC（运营商）",
                    value = cfg.str(XpConfig.KEY_CELL_MNC, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_CELL_MNC, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledTextField(
                    label = "LAC / TAC",
                    value = cfg.str(XpConfig.KEY_CELL_LAC, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_CELL_LAC, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = "CID",
                    value = cfg.str(XpConfig.KEY_CELL_CID, ""),
                    onValueChange = { cfg.put(XpConfig.KEY_CELL_CID, it) },
                    enabled = on,
                    modifier = Modifier.weight(1f),
                )
            }
            LabeledTextField(
                label = "PCI / PSC（可选）",
                value = cfg.str(XpConfig.KEY_CELL_PCI, ""),
                onValueChange = { cfg.put(XpConfig.KEY_CELL_PCI, it) },
                enabled = on,
            )
        }
    }
}

@Composable
internal fun GmsSpoofCard(cfg: XpConfigState) {
    val on = cfg.bool(XpConfig.KEY_GMS_ENABLE, false)
    FeatureCard(
        title = "伪装设备拥有 Google Play 服务",
        subtitle = "GMS：包查询 / 可用性检查 / 广告 ID",
        checked = on,
        onCheckedChange = { cfg.put(XpConfig.KEY_GMS_ENABLE, it) }
    ) {
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_GMS_INSTALLED, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_GMS_INSTALLED, it) },
            title = "伪装已安装",
            subtitle = "查 com.google.android.gms / vending / gsf 时不再报「未安装」",
            enabled = on,
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_GMS_AVAILABLE, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_GMS_AVAILABLE, it) },
            title = "可用性检查返回成功",
            subtitle = "isGooglePlayServicesAvailable 等一律返回 0（可用）",
            enabled = on,
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_GMS_ADID_ENABLE, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_GMS_ADID_ENABLE, it) },
            title = "提供广告 ID（GAID）",
            enabled = on,
        )
        val adidOn = on && cfg.bool(XpConfig.KEY_GMS_ADID_ENABLE, true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { cfg.put(XpConfig.KEY_GMS_ADID, UUID.randomUUID().toString()) },
                modifier = Modifier.weight(1f),
                enabled = adidOn,
            ) { Text("随机生成") }
            OutlinedButton(
                onClick = { cfg.put(XpConfig.KEY_GMS_ADID, XpConfig.DEF_GMS_ADID) },
                modifier = Modifier.weight(1f),
                enabled = adidOn,
            ) { Text("用默认值") }
        }
        LabeledTextField(
            label = "GAID",
            value = cfg.str(XpConfig.KEY_GMS_ADID, ""),
            onValueChange = { cfg.put(XpConfig.KEY_GMS_ADID, it) },
            enabled = adidOn,
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_GMS_ADID_LIMIT, false),
            onCheckedChange = { cfg.put(XpConfig.KEY_GMS_ADID_LIMIT, it) },
            title = "限制广告跟踪",
            enabled = adidOn,
        )
        LabeledTextField(
            label = "版本名",
            value = cfg.str(XpConfig.KEY_GMS_VERSION, XpConfig.DEF_GMS_VERSION),
            onValueChange = { cfg.put(XpConfig.KEY_GMS_VERSION, it) },
            enabled = on,
        )
        LabeledTextField(
            label = "版本号（versionCode）",
            value = cfg.int(XpConfig.KEY_GMS_VERSION_CODE, XpConfig.DEF_GMS_VERSION_CODE).toString(),
            onValueChange = {
                it.trim().toLongOrNull()?.let { v ->
                    cfg.put(XpConfig.KEY_GMS_VERSION_CODE, v.toInt())
                }
            },
            enabled = on,
        )
        HintText(
            "设备本来就装了 GMS 时，上面填的版本号会覆盖真实值，其余字段保持真实。" +
                "签名无法伪造（给的是占位值），做签名校验的检测仍会失败，但不会崩溃。"
        )
    }
}

@Composable
internal fun HmsSpoofCard(cfg: XpConfigState) {
    val on = cfg.bool(XpConfig.KEY_HMS_ENABLE, false)
    FeatureCard(
        title = "伪装设备拥有 Huawei Mobile Services",
        subtitle = "HMS：包查询 / 可用性检查 / 广告 ID（OAID）",
        checked = on,
        onCheckedChange = { cfg.put(XpConfig.KEY_HMS_ENABLE, it) }
    ) {
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_HMS_INSTALLED, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_HMS_INSTALLED, it) },
            title = "伪装已安装",
            subtitle = "查 com.huawei.hwid / appmarket / pushagent 时不再报「未安装」",
            enabled = on,
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_HMS_AVAILABLE, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_HMS_AVAILABLE, it) },
            title = "可用性检查返回成功",
            subtitle = "isHuaweiMobileServicesAvailable 等一律返回 0（可用）",
            enabled = on,
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_HMS_ADID_ENABLE, true),
            onCheckedChange = { cfg.put(XpConfig.KEY_HMS_ADID_ENABLE, it) },
            title = "提供广告 ID（OAID）",
            subtitle = "沿用身份伪装里的 OAID，留空则随机",
            enabled = on,
        )
        LabeledTextField(
            label = "版本名",
            value = cfg.str(XpConfig.KEY_HMS_VERSION, XpConfig.DEF_HMS_VERSION),
            onValueChange = { cfg.put(XpConfig.KEY_HMS_VERSION, it) },
            enabled = on,
        )
        LabeledTextField(
            label = "版本号（versionCode）",
            value = cfg.int(XpConfig.KEY_HMS_VERSION_CODE, XpConfig.DEF_HMS_VERSION_CODE).toString(),
            onValueChange = {
                it.trim().toLongOrNull()?.let { v ->
                    cfg.put(XpConfig.KEY_HMS_VERSION_CODE, v.toInt())
                }
            },
            enabled = on,
        )
        HintText(
            "设备本来就装了 HMS 时，上面填的版本号会覆盖真实值，其余字段保持真实。" +
                "签名无法伪造（给的是占位值），做签名校验的检测仍会失败，但不会崩溃。"
        )
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
internal fun PermissionFakeCard(cfg: XpConfigState, enabled: Boolean) {
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
                    permSummary(g),
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
                
                
                
                if (g.id == XpConfig.PERM_LOCATION && g.id in fake) {
                    OutlinedButton(
                        onClick = { UiNav.requestLocation() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("设置具体位置（设备伪装 → 位置与基站伪装）")
                    }
                }
                HorizontalDividerCompat()
            }
            HintText(
                "第 2 个开关决定「真去读时拿到什么」：勾上后，本来会拿到空的地方改为返回一批虚构内容，避免应用因空数据闪退。每次启动重新随机生成"
            )
        }
    }
}







private fun permSummary(g: XpConfig.PermGroup): String {
    if (g.perms.isEmpty()) return "（无对应权限串，靠系统接口伪装）"
    val names = g.perms.map { it.removePrefix("android.permission.") }
    val shown = names.take(4)
    val rest = names.size - shown.size
    return if (rest > 0) "${shown.joinToString(" / ")} 等 ${names.size} 项" else shown.joinToString(" / ")
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
private fun cpuFreqPreview(cfg: XpConfigState, cpuOn: Boolean) {
    if (!cpuOn) return
    val text = remember(
        cfg.str(XpConfig.KEY_FAKE_CPUINFO_HW, ""),
        cfg.str(XpConfig.KEY_FAKE_CPU_PRESET, ""),
        cfg.int(XpConfig.KEY_FAKE_CPU_CORES, 8),
        cfg.str(XpConfig.KEY_FAKE_CPU_MIN_FREQ, ""),
        cfg.str(XpConfig.KEY_FAKE_CPU_MAX_FREQ, ""),
    ) {
        runCatching {
            val snap = XpState.current()
            val hw = FakeProps.cpuHardware(snap)
            val n = XpConfig.cpuCoreCount(hw).takeIf { it > 0 }
                ?: cfg.int(XpConfig.KEY_FAKE_CPU_CORES, 8).coerceIn(1, 32)
            val (mins, maxs, curs) = FakeProps.coreFreqsForPreview(snap, n)
            buildString {
                for (i in 0 until n) {
                    append("cpu$i: ${mins[i]} ~ ${maxs[i]}（当前 ${curs[i]}）\n")
                }
            }.trim()
        }.getOrDefault("")
    }
    if (text.isBlank()) return
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "频率预览（每核 kHz）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
            )
        }
    }
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

@Composable
internal fun DevOptionFakeRow(cfg: XpConfigState, buildOn: Boolean = true) {
    SwitchRow(
        title = "伪装开发者选项已关闭",
        checked = cfg.bool(XpConfig.KEY_FAKE_DEV_OFF, false),
        enabled = buildOn,
        onCheckedChange = { cfg.put(XpConfig.KEY_FAKE_DEV_OFF, it) },
    )
}

@Composable
internal fun AccountsHideRow(cfg: XpConfigState, buildOn: Boolean = true) {
    SwitchRow(
        title = "隐藏账号与邮箱",
        checked = cfg.bool(XpConfig.KEY_HIDE_ACCOUNTS, false),
        enabled = buildOn,
        onCheckedChange = { cfg.put(XpConfig.KEY_HIDE_ACCOUNTS, it) },
    )
}

@Composable
internal fun WebViewJsCard(cfg: XpConfigState) {
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
}

@Composable
internal fun RootFakeCard(cfg: XpConfigState) {
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
}

@Composable
internal fun VpnHideCard(cfg: XpConfigState) {
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
}

@Composable
internal fun CrashBlockCard(cfg: XpConfigState) {
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
            "回收方式是 interrupt 最早的那些线程。" +
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
}

@Composable
internal fun CrashCatcherCard(cfg: XpConfigState) {
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
            title = "拦截应用抛出的异常",
            enabled = enabled,
        )
    }
}
