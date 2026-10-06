package io.github.sunilxsk.lockperm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.libxposed.service.XposedService





@Composable
fun ShieldConfigContent(cfg: XpConfigState, enabled: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        
        NativeStatusBar(cfg)
        ActiveShield(cfg, enabled)
        Spacer(Modifier.height(24.dp))
    }
}





@Composable
private fun ActiveShield(cfg: XpConfigState, enabled: Boolean) {
    val active = enabled

    
    FeatureCard(
        title = "悬浮窗式便捷功能",
        checked = cfg.bool(XpConfig.KEY_PANEL_INJECT, true),
        enabled = enabled,
        onCheckedChange = { cfg.put(XpConfig.KEY_PANEL_INJECT, it) }
    ) {
    }

    WifiFakeFeatureCard(cfg, active)

    ExitFeatureCard(cfg, active)
    AccessibilityFeatureCard(cfg, active)

    VolumeFeatureCard(cfg, active)
    AudioOutFeatureCard(cfg, active)
    ClipboardFeatureCard(cfg, active)
    TorchVibrateFeatureCard(cfg, active)
    SystemControlFeatureCard(cfg, active)
    SensorFeatureCard(cfg, active)

    FileGuardFeatureCard(cfg, active)
    HideAppsFeatureCard(cfg, active)

    WirelessDebuggingFeatureCard(cfg, active)
    ShizukuFeatureCard(cfg, active)

    JumpFeatureCard(cfg, active)
    BackgroundLaunchFeatureCard(cfg, active)
    CameraMicFeatureCard(cfg, active)
    InstallFeatureCard(cfg, active)
    PrintCastFeatureCard(cfg, active)
    NotifyFeatureCard(cfg, active)
    NetworkFilterFeatureCard(cfg, active)
    ScreenCaptureFeatureCard(cfg, active)

    OverlayFeatureCard(cfg, active)
    WindowFlagFeatureCard(cfg, active)
    WakeLockFeatureCard(cfg, active)
    HideRecentsFeatureCard(cfg, active)
    ScreenOffFeatureCard(cfg, active)
    NotifyHideFeatureCard(cfg, active)
    ProviderFeatureCard(cfg, active)
    ForegroundServiceFeatureCard(cfg, active)

    ExecBlockFeatureCard(cfg, active)

    FeatureCard(
        title = "替换壁纸功能",
        checked = cfg.bool(XpConfig.KEY_BLOCK_WALLPAPER, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_WALLPAPER, it) }
    ) {
        HintText("开启后应用无论是设置图片还是其他方式，都无法更改壁纸。")
    }

    DeviceAdminFeatureCard(cfg, active)

    KeyConsumeFeatureCard(cfg, active)
}




@Composable
private fun OverlayFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_OVERLAY, false)
    
    val tuneEnabled = !on && active

    FeatureCard(
        title = "悬浮窗功能",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_OVERLAY, it) }
    ) {
        HintText("仅拦截需要「显示在其他应用上层」权限的那一类窗口，普通 Toast、应用内弹窗不受影响。")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "下面几项在关闭上面的开关后才生效——允许它显示，但按你的要求改造：",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SwitchRow(
            title = "悬浮窗可穿透点击",
            checked = cfg.bool(XpConfig.KEY_OVERLAY_UNTouchABLE, false),
            enabled = tuneEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_OVERLAY_UNTouchABLE, it) },
        )
        HintText("触摸事件直接穿到下层应用，悬浮窗本身收不到点击。")
        SwitchRow(
            title = "将悬浮窗设为完全透明",
            checked = cfg.bool(XpConfig.KEY_OVERLAY_TRANSPARENT, false),
            enabled = tuneEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_OVERLAY_TRANSPARENT, it) },
        )
        HintText("窗口照常存在，但完全看不见。")
        LabeledTextField(
            label = "限制悬浮窗大小（占屏幕百分比，0 = 不限制）",
            value = cfg.int(XpConfig.KEY_OVERLAY_MAX_PERCENT, 0).toString(),
            onValueChange = {
                cfg.put(
                    XpConfig.KEY_OVERLAY_MAX_PERCENT,
                    it.filter { c -> c.isDigit() }.toIntOrNull()?.coerceIn(0, 100) ?: 0
                )
            },
            enabled = tuneEnabled,
        )
        HintText("超过这个比例会等比缩小，宽高都不越界；填 0 表示不作限制。")
    }
}

@Composable
private fun WindowFlagFeatureCard(cfg: XpConfigState, active: Boolean) {
    val mode = cfg.int(XpConfig.KEY_WIN_SECURE_MODE, 0)
    
    val selected = XpConfig.decodeSet(cfg.str(XpConfig.KEY_WIN_FLAGS, ""))
    var showDialog by remember { mutableStateOf(false) }

    fun write(set: Set<String>) {
        cfg.put(XpConfig.KEY_WIN_FLAGS, XpConfig.encodeSet(set))
    }

    val on = mode != 0 || selected.isNotEmpty()

    FeatureCard(
        title = "窗口标志",
        checked = on,
        enabled = active,
        onCheckedChange = { want ->
            if (want) {
                
                cfg.put(XpConfig.KEY_WIN_SECURE_MODE, 1)
            } else {
                
                cfg.put(XpConfig.KEY_WIN_SECURE_MODE, 0)
                write(emptySet())
            }
        }
    ) {
        Text(
            "FLAG_SECURE（禁止截屏 / 录屏）",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectChips(
            options = listOf("不改", "去除", "加上"),
            selectedIndex = mode.coerceIn(0, 2),
            onSelect = { cfg.put(XpConfig.KEY_WIN_SECURE_MODE, it) },
            enabled = active,
        )
        HintText(
            "「去除」后可以截屏录屏；「加上」则反过来，让应用无法被截屏。" +
                "对应用的所有窗口生效（Window.addFlags / setFlags 与加窗口时的布局参数都改）。"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "自定义标志（加到所有窗口上）",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick = { showDialog = true },
            enabled = active,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (selected.isEmpty()) "选择标志" else "选择标志（已选 ${selected.size} 项）"
            )
        }
        if (selected.isNotEmpty()) {
            Text(
                selected.joinToString("、"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { write(emptySet()) },
                enabled = active,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("清空已选标志") }
        }
        HintText(
            "选中的标志会被并进窗口的 flags 里，与上面的 FLAG_SECURE 叠加生效。" +
                "这些都是系统公开常量，改错了最多是窗口表现异常（点不动、全屏等），不会崩溃。"
        )
    }

    if (showDialog) {
        WindowFlagDialog(
            selected = selected,
            enabled = active,
            onToggle = { id, want ->
                write(if (want) selected + id else selected - id)
            },
            onClear = { write(emptySet()) },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun WindowFlagDialog(
    selected: Set<String>,
    enabled: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "选择窗口标志",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected.isNotEmpty()) {
                        TextButton(onClick = onClear, enabled = enabled) { Text("清空") }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    WindowFlagPresets.ALL.forEach { f ->
                        val checked = f.id in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = enabled) { onToggle(f.id, !checked) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggle(f.id, it) },
                                enabled = enabled,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    f.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (checked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    f.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    HintText("🍉🍉🍉")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun WakeLockFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "禁止应用使屏幕常亮",
        checked = cfg.bool(XpConfig.KEY_BLOCK_WAKELOCK, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_WAKELOCK, it) }
    ) {
        HintText(
            "开启后恢复系统自动熄屏的行为：PowerManager.WakeLock 拿不到、acquire 不生效，" +
                "窗口上的 FLAG_KEEP_SCREEN_ON 会被抹掉，View.setKeepScreenOn(true) 也拦下。" +
                "屏幕什么时候灭，交回系统策略决定。"
        )
        HintText("注意：视频、导航、阅读类应用常靠这个保持屏幕不灭，开启后会恢复自动熄屏。")
    }
}

@Composable
private fun HideRecentsFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "禁止从后台任务中隐藏",
        checked = cfg.bool(XpConfig.KEY_BLOCK_HIDE_RECENTS, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_HIDE_RECENTS, it) }
    ) {
        HintText(
            "开启后应用没法把自己从多任务界面里藏掉：AppTask.setExcludeFromRecents、" +
                "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS、ActivityOptions.setExcludeFromRecents 一并拦掉，" +
                "卡片会一直留在最近任务里。"
        )
    }
}

@Composable
private fun ScreenOffFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_SCREEN_OFF, false)
    val enabled = on && active

    FeatureCard(
        title = "禁止息屏但不锁屏",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_SCREEN_OFF, it) }
    ) {

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "释放屏幕常亮锁",
            subtitle = "  通话息屏",
            checked = cfg.bool(XpConfig.KEY_SCREEN_OFF_WAKELOCK, false),
            enabled = active,
            onCheckedChange = { cfg.put(XpConfig.KEY_SCREEN_OFF_WAKELOCK, it) },
        )
        SwitchRow(
            title = "钩住反射调用（覆盖 app_process 类方案）",
            subtitle = "挡住反射调 goToSleep / nap；开销偏大，只在需要时开",
            checked = cfg.bool(XpConfig.KEY_SCREEN_OFF_REFLECT, false),
            enabled = active,
            onCheckedChange = { cfg.put(XpConfig.KEY_SCREEN_OFF_REFLECT, it) },
        )
        HintText(
            "第二项钩的是 java.lang.reflect.Method.invoke，属于热点路径，" +
                "会给每次反射调用都加一层判断。默认关闭，确认需要再开。"
        )
    }
}

@Composable
private fun NotifyHideFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "禁止隐藏通知",
        checked = cfg.bool(XpConfig.KEY_BLOCK_NOTIFY_HIDE, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_NOTIFY_HIDE, it) }
    ) {
        HintText(
            "和上面的「通知发送拦截」是两件事：那边是不让它发，这边是不让它把已经发出去的撤掉。" +
                "NotificationManager.cancel / cancelAll 都会被拦，通知栏上的条目留得住。"
        )
    }
}

@Composable
private fun ProviderFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "禁止 BridgeProvider（ContentProvider 暴露）",
        checked = cfg.bool(XpConfig.KEY_BLOCK_PROVIDER, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_PROVIDER, it) }
    ) {
        HintText(
            "开启后即使 manifest 里声明了 provider，进程内也拿不到实例：" +
                "ActivityThread 的 acquire / install 直接返回空，查询侧的 query / insert / update / " +
                "delete / call 和 openInputStream / openOutputStream 等全部空转。" +
                "既不能对外返回数据，也写不了文件、留不下待取缓存。"
        )
    }
}

@Composable
private fun ForegroundServiceFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "禁止前台常驻服务",
        checked = cfg.bool(XpConfig.KEY_BLOCK_FOREGROUND_SERVICE, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_FOREGROUND_SERVICE, it) }
    ) {
        HintText(
            "拦掉 Service.startForeground 和 startForegroundService，服务就只是普通后台服务：" +
                "系统该回收时照样回收，通知栏那个常驻条目也出不来。" +
                "stopForeground 会放行，免得已经起来的前台状态撤不掉。"
        )
    }
}


@Composable
private fun CondSwitch(
    cfg: XpConfigState,
    active: Boolean,
    on: Boolean,
    key: String,
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    SwitchRow(
        title = title,
        subtitle = subtitle,
        checked = cfg.bool(key, false),
        enabled = active,
        onCheckedChange = { cfg.put(key, it) },
    )
    if (cfg.bool(key, false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
        Spacer(Modifier.height(4.dp))
    }
}


@Composable
private fun NumberBox(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw ->
            val v = raw.filter { it.isDigit() }.take(6).toIntOrNull() ?: return@OutlinedTextField
            onChange(v.coerceIn(range.first, range.last))
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        enabled = enabled,
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun KeyConsumeFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_KEY_CONSUME, false)
    val enabled = on && active
    FeatureCard(
        title = "禁止任何方式消费按键事件",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_KEY_CONSUME, it) }
    ) {
        HintText(
            "开启后，音量、静音、相机、耳机、媒体、菜单、搜索等物理键一律交还系统处理：" +
                "应用在前台界面、悬浮窗或无障碍服务里都吞不掉，按音量键正常调系统音量。"
        )
        SwitchRow(
            title = "放行返回键(BACK)",
            checked = cfg.bool(XpConfig.KEY_BLOCK_KEY_PASS_BACK, true),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_KEY_PASS_BACK, it) },
        )
    }
}




@Composable
private fun WifiFakeFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_WIFI_FAKE_ENABLE, false)
    val enabled = on && active

    FeatureCard(
        title = "伪装 WiFi 连接状态与列表",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_ENABLE, it) }
    ) {
        LabeledTextField(
            label = "WiFi 名称 SSID",
            value = cfg.str(XpConfig.KEY_WIFI_FAKE_SSID, ""),
            onValueChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_SSID, it) },
            enabled = enabled,
        )
        LabeledTextField(
            label = "BSSID（路由器 MAC）",
            value = cfg.str(XpConfig.KEY_WIFI_FAKE_BSSID, ""),
            onValueChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_BSSID, it) },
            enabled = enabled,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledTextField(
                label = "信号 dBm",
                value = cfg.int(XpConfig.KEY_WIFI_FAKE_RSSI, -55).toString(),
                onValueChange = {
                    cfg.put(
                        XpConfig.KEY_WIFI_FAKE_RSSI,
                        it.filter { c -> c.isDigit() || c == '-' }.toIntOrNull() ?: -55
                    )
                },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            LabeledTextField(
                label = "速率 Mbps",
                value = cfg.int(XpConfig.KEY_WIFI_FAKE_SPEED, 300).toString(),
                onValueChange = {
                    cfg.put(
                        XpConfig.KEY_WIFI_FAKE_SPEED,
                        it.filter { c -> c.isDigit() }.toIntOrNull() ?: 300
                    )
                },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledTextField(
                label = "IP 地址",
                value = cfg.str(XpConfig.KEY_WIFI_FAKE_IP, "192.168.1.88"),
                onValueChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_IP, it) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            LabeledTextField(
                label = "频率 MHz",
                value = cfg.int(XpConfig.KEY_WIFI_FAKE_FREQ, 5180).toString(),
                onValueChange = {
                    cfg.put(
                        XpConfig.KEY_WIFI_FAKE_FREQ,
                        it.filter { c -> c.isDigit() }.toIntOrNull() ?: 5180
                    )
                },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "把网络状态报成「已连接 WiFi」",
            checked = cfg.bool(XpConfig.KEY_WIFI_FAKE_NETWORK, true),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_NETWORK, it) },
        )
        SwitchRow(
            title = "伪装 WiFi 扫描结果",
            checked = cfg.bool(XpConfig.KEY_WIFI_FAKE_SCAN, true),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_SCAN, it) },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "禁止获取已保存的 WiFi 列表",
            subtitle = "挡掉 getConfiguredNetworks 一类的查询，应用看不到保存过哪些网络",
            checked = cfg.bool(XpConfig.KEY_BLOCK_WIFI_SAVED, false),
            enabled = active,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_WIFI_SAVED, it) },
        )
        HintText(
            "已保存列表里密码拿不到，但网络名字照常泄露（家里、公司的 SSID）。" +
                "开启后整份列表直接返回空。"
        )

        Text(
            "热点列表（每行一个：名称|BSSID|信号|加密）",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        LabeledTextField(
            label = "热点列表",
            value = cfg.str(XpConfig.KEY_WIFI_FAKE_LIST, ""),
            onValueChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_LIST, it) },
            enabled = enabled && cfg.bool(XpConfig.KEY_WIFI_FAKE_SCAN, true),
            singleLine = false,
            maxLines = 10,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { cfg.put(XpConfig.KEY_WIFI_FAKE_LIST, XpConfig.defaultApList()) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { Text("填入示例") }
            OutlinedButton(
                onClick = { cfg.put(XpConfig.KEY_WIFI_FAKE_LIST, "") },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { Text("清空") }
        }
        HintText(
            "留空时不改动扫描结果。"
        )
    }
}

@Composable
private fun ExitFeatureCard(cfg: XpConfigState, active: Boolean) {
    val exitEnable = cfg.bool(XpConfig.KEY_EXIT_ENABLE, false)
    val parallel = cfg.bool(XpConfig.KEY_EXIT_PARALLEL, false)
    val blockExec = cfg.bool(XpConfig.KEY_BLOCK_EXEC, false)
    
    val methods = cfg.strSet(XpConfig.KEY_EXIT_METHODS, XpConfig.DEF_EXIT_METHODS)
    val seconds = cfg.int(XpConfig.KEY_EXIT_SECONDS, XpConfig.DEF_EXIT_SECONDS)

    FeatureCard(
        title = "退出功能",
        checked = exitEnable,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_EXIT_ENABLE, it) }
    ) {
        val enabled = exitEnable && active

        Text(
            "退出条件",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        HintText("勾中的条件任意一个达成即退出。一个都没勾的话只有手动触发才会退出。")

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_EXIT_COUNTDOWN,
            title = "倒计时后退出",
            subtitle = "从目标应用启动开始计时",
        ) {
            NumberBox(
                label = "秒", value = seconds, range = 1..86400,
                enabled = enabled,
                onChange = { cfg.put(XpConfig.KEY_EXIT_SECONDS, it) },
                modifier = Modifier.width(140.dp),
            )
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_AT_TIME,
            title = "指定时刻退出",
            subtitle = "每天到了这个点就退出（24 小时制）",
        ) {
            val hh = cfg.int(XpConfig.KEY_COND_AT_TIME_HH, 18)
            val mm = cfg.int(XpConfig.KEY_COND_AT_TIME_MM, 0)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberBox(
                    label = "时", value = hh, range = 0..23,
                    enabled = enabled,
                    onChange = { cfg.put(XpConfig.KEY_COND_AT_TIME_HH, it) },
                    modifier = Modifier.width(96.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(":", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(12.dp))
                NumberBox(
                    label = "分", value = mm, range = 0..59,
                    enabled = enabled,
                    onChange = { cfg.put(XpConfig.KEY_COND_AT_TIME_MM, it) },
                    modifier = Modifier.width(96.dp),
                )
            }
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_MEM,
            title = "内存占用超过阈值",
            subtitle = "本进程实际占用达到设定值时退出",
        ) {
            NumberBox(
                label = "MB", value = cfg.int(XpConfig.KEY_COND_MEM_MB, 1024),
                range = 1..8192, enabled = enabled,
                onChange = { cfg.put(XpConfig.KEY_COND_MEM_MB, it) },
                modifier = Modifier.width(140.dp),
            )
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_CPU,
            title = "CPU 占用超过阈值",
            subtitle = "单位与 top 的 %CPU 列一致：100% = 占满一个核",
        ) {
            NumberBox(
                label = "%", value = cfg.int(XpConfig.KEY_COND_CPU_PCT, 80),
                range = 1..1600, enabled = enabled,
                onChange = { cfg.put(XpConfig.KEY_COND_CPU_PCT, it) },
                modifier = Modifier.width(140.dp),
            )
            HintText(
                "跟面板里 top 看到的 %CPU 是同一套单位：八核机器跑满所有核约等于 800%。" +
                    "含本进程 fork 出去的子进程（面板命令、应用起的后台进程都算）。" +
                    "要求连续两次采样都超，是为了躲开启动、切页面那一瞬间的尖峰。"
            )
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_DISK,
            title = "剩余存储空间低于阈值",
            subtitle = "看的是本应用数据目录所在分区",
        ) {
            NumberBox(
                label = "MB", value = cfg.int(XpConfig.KEY_COND_DISK_MB, 500),
                range = 1..65536, enabled = enabled,
                onChange = { cfg.put(XpConfig.KEY_COND_DISK_MB, it) },
                modifier = Modifier.width(140.dp),
            )
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_NET,
            title = "网络状态变化",
            subtitle = "断网即退出，或一联网就退出",
        ) {
            SingleSelectChips(
                options = listOf("断开时退出", "连接时退出"),
                selectedIndex = cfg.int(XpConfig.KEY_COND_NET_MODE, 0).coerceIn(0, 1),
                onSelect = { cfg.put(XpConfig.KEY_COND_NET_MODE, it) },
                enabled = enabled,
            )
            HintText(
                "以系统广播为准，不受本模块自身的网络伪装影响；" +
                    "广播还没来过时不会判定，避免刚启动就误退出。"
            )
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_BATT,
            title = "电池电量 / 充电状态",
            subtitle = "电量低于阈值，或一开始充电就退出",
        ) {
            val mode = cfg.int(XpConfig.KEY_COND_BATT_MODE, 0).coerceIn(0, 2)
            SingleSelectChips(
                options = listOf("电量低于", "开始充电", "结束充电"),
                selectedIndex = mode,
                onSelect = { cfg.put(XpConfig.KEY_COND_BATT_MODE, it) },
                enabled = enabled,
            )
            if (mode == 0) {
                NumberBox(
                    label = "%", value = cfg.int(XpConfig.KEY_COND_BATT_PCT, 10),
                    range = 0..100, enabled = enabled,
                    onChange = { cfg.put(XpConfig.KEY_COND_BATT_PCT, it) },
                    modifier = Modifier.width(140.dp),
                )
            }
            HintText(
                "电池走系统广播，不轮询，几乎不耗电。" +
                    "「开始 / 结束充电」只在插拔那一刻触发，" +
                    "如果应用启动时就已经插着（或拔着），不会补触发。"
            )
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_FILE,
            title = "指定文件被创建 / 修改 / 删除",
            subtitle = "事件级监听，每行一个路径",
        ) {
            LabeledTextField(
                label = "文件路径（每行一个）",
                value = cfg.str(XpConfig.KEY_COND_FILE_PATHS, ""),
                onValueChange = { cfg.put(XpConfig.KEY_COND_FILE_PATHS, it) },
                enabled = enabled,
                singleLine = false,
                maxLines = 6,
            )
            SingleSelectChips(
                options = listOf("任意", "创建", "修改", "删除"),
                selectedIndex = cfg.int(XpConfig.KEY_COND_FILE_EVENT, 0).coerceIn(0, 3),
                onSelect = { cfg.put(XpConfig.KEY_COND_FILE_EVENT, it) },
                enabled = enabled,
            )
            HintText(
                "写目录就监听这个目录下的变动，写文件就只认这个文件。" +
                    "路径必须存在且本应用有权限访问，否则监听不会生效。"
            )
        }

        
        CondSwitch(
            cfg = cfg, active = active, on = exitEnable,
            key = XpConfig.KEY_COND_IDLE,
            title = "长时间未触摸屏幕",
            subtitle = "适合清理挂在那儿没人管的界面",
        ) {
            NumberBox(
                label = "分钟", value = cfg.int(XpConfig.KEY_COND_IDLE_MIN, 10),
                range = 1..1440, enabled = enabled,
                onChange = { cfg.put(XpConfig.KEY_COND_IDLE_MIN, it) },
                modifier = Modifier.width(140.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            "退出选项",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        MultiSelectChips(
            options = XpConfig.EXIT_METHODS,
            selected = methods,
            enabled = enabled && !parallel,
            onToggle = { key, on ->
                var next = if (on) methods + key else methods - key
                
                if (on && key == XpConfig.EXIT_METHOD_EXEC && blockExec) {
                    cfg.put(XpConfig.KEY_BLOCK_EXEC, false)
                }
                if (next.isEmpty()) next = setOf("kill")
                cfg.put(XpConfig.KEY_EXIT_METHODS, next)
            },
        )
        SwitchRow(
            title = "全部并行运行",
            checked = parallel,
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_EXIT_PARALLEL, it) },
        )
        val shown = if (parallel) XpConfig.EXIT_METHOD_KEYS else methods.toList()
        HintText(
            "已选 ${shown.size} 项：" +
                    shown.joinToString("、") { key ->
                        XpConfig.EXIT_METHODS.firstOrNull { it.first == key }?.second ?: key
                    }
        )
        
    }
}




@Composable
private fun ExecBlockFeatureCard(cfg: XpConfigState, active: Boolean) {
    val blockExec = cfg.bool(XpConfig.KEY_BLOCK_EXEC, false)
    val exitEnable = cfg.bool(XpConfig.KEY_EXIT_ENABLE, false)

    FeatureCard(
        title = "阻止应用执行命令",
        checked = blockExec,
        enabled = active,
        onCheckedChange = { on ->
            cfg.put(XpConfig.KEY_BLOCK_EXEC, on)
            if (on) switchExitMethodAwayFromExec(cfg)
        }
    ) {
        HintText(
            "开启后应用无法执行任何命令行指令，包括间接触发的方式。"
        )
    }
}


private fun switchExitMethodAwayFromExec(cfg: XpConfigState) {
    val current = cfg.strSet(XpConfig.KEY_EXIT_METHODS, XpConfig.DEF_EXIT_METHODS)
    if (XpConfig.EXIT_METHOD_EXEC !in current) return
    val next = (current - XpConfig.EXIT_METHOD_EXEC).toMutableSet()
    if (next.isEmpty()) next.add("kill")
    cfg.put(XpConfig.KEY_EXIT_METHODS, next)
}




@Composable
private fun AccessibilityFeatureCard(cfg: XpConfigState, active: Boolean) {
    val accEnable = cfg.bool(XpConfig.KEY_ACC_ENABLE, false)
    val mode = cfg.int(XpConfig.KEY_ACC_MODE, 0)
    val scope = cfg.int(XpConfig.KEY_ACC_SCOPE, 1)
    val exitEnable = cfg.bool(XpConfig.KEY_EXIT_ENABLE, false)

    
    val onceEnabled = exitEnable && active && accEnable
    val effectiveMode = if (mode == 1 && !onceEnabled) 0 else mode
    
    val capEnabled = accEnable && active && scope != XpConfig.ACC_SCOPE_CLOSE_ONLY
    
    val modeEnabled = accEnable && active && scope != XpConfig.ACC_SCOPE_HOOK_ONLY

    FeatureCard(
        title = "无障碍功能",
        checked = accEnable,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_ACC_ENABLE, it) }
    ) {
        SwitchRow(
            title = "禁止开启无障碍",
            checked = accEnable,
            enabled = active,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_ENABLE, it) },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "返回给应用的无障碍状态",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        val statusSpoof = cfg.bool(XpConfig.KEY_ACC_STATUS_SPOOF, true)
        val statusValue = cfg.bool(XpConfig.KEY_ACC_STATUS_VALUE, false)
        SwitchRow(
            title = "伪装无障碍状态",
            checked = statusSpoof,
            enabled = active,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_STATUS_SPOOF, it) },
        )
        SingleSelectChips(
            options = listOf("关闭", "开启"),
            selectedIndex = if (statusValue) 1 else 0,
            enabled = statusSpoof && active,
            onSelect = { cfg.put(XpConfig.KEY_ACC_STATUS_VALUE, it == 1) },
        )


        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "拦截方式",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectChips(
            options = XpConfig.ACC_MODE_OPTIONS,
            selectedIndex = cfg.int(XpConfig.KEY_ACC_FAKE_MODE, XpConfig.ACC_MODE_DEFAULT),
            enabled = active,
            onSelect = { cfg.put(XpConfig.KEY_ACC_FAKE_MODE, it) },
        )
        HintText(
            if (cfg.int(XpConfig.KEY_ACC_FAKE_MODE, XpConfig.ACC_MODE_DEFAULT) ==
                XpConfig.ACC_MODE_FAKE_SUCCESS
            ) {
                "伪装成功：应用调用无障碍接口时一律得到「成功了」，" +
                    "但什么都不会真的执行。只影响接口返回值 —— " +
                    "下面的禁用时机、运行方式照常生效，持续关闭服务不受影响。"
            } else {
                "默认：接口返回失败 / 空，并按下面的时机真正关掉无障碍服务。"
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "禁用开启无障碍子功能",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_SCREEN, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_SCREEN, it) },
            title = "禁用读取屏幕内容 / 页面结构",
            subtitle = "拿不到界面文字与控件树",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_NOTIFY, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_NOTIFY, it) },
            title = "禁用监听通知",
            subtitle = "收不到通知内容",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_WINDOW, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_WINDOW, it) },
            title = "禁用监听窗口变化",
            subtitle = "感知不到窗口切换",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_INPUT, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_INPUT, it) },
            title = "禁用读取输入内容",
            subtitle = "读不到输入框文字",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_ACTION, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_ACTION, it) },
            title = "禁用全部模拟操作",
            subtitle = "无法代为点击与滑动",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_OVERLAY, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_OVERLAY, it) },
            title = "禁用无障碍专属悬浮窗",
            subtitle = "不能靠无障碍浮窗遮挡",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_CONTROL, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_CONTROL, it) },
            title = "禁用按键 / 手势监听与屏幕控制",
            subtitle = "按键与手势均失效",
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "禁用时机",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        SingleSelectChips(
            options = listOf("持续禁用", "倒计时前禁用一次"),
            selectedIndex = effectiveMode,
            enabled = modeEnabled,
            onSelect = { idx ->
                if (idx == 1 && !onceEnabled) return@SingleSelectChips
                cfg.put(XpConfig.KEY_ACC_MODE, idx)
            },
        )
        if (scope == XpConfig.ACC_SCOPE_HOOK_ONLY) {
            HintText("当前只运行钩子，不会关闭服务本身，因此上面的时机选项已置灰。")
        }
        if (mode == 1 && !exitEnable) {
            HintText("「倒计时前禁用一次」依赖功能一（倒计时退出）：功能一没开启时它也无法开启，已自动回退为持续禁用。")
        }

        Text(
            "运行范围",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectChips(
            options = XpConfig.ACC_SCOPE_OPTIONS,
            selectedIndex = scope,
            enabled = accEnable && active,
            onSelect = { cfg.put(XpConfig.KEY_ACC_SCOPE, it) },
        )
        HintText(
            when (scope) {
                XpConfig.ACC_SCOPE_HOOK_ONLY ->
                    "只运行钩子：拦截查询、改返回值，但绝不调用 disableSelf，服务不会被关掉。"
                XpConfig.ACC_SCOPE_CLOSE_AND_HOOK ->
                    "关闭服务 + 运行钩子：既拦截查询，也按上面的禁用时机真正关掉服务。"
                else ->
                    "只关闭服务：只负责关掉服务，不装任何钩子，接口一律如实返回。"
            }
        )
    }
}




@Composable
private fun DeviceAdminFeatureCard(cfg: XpConfigState, active: Boolean) {
    val daEnable = cfg.bool(XpConfig.KEY_DA_ENABLE, false)
    val master = cfg.bool(XpConfig.KEY_DA_MASTER, false)
    val enabled = daEnable && active
    val daScope = cfg.int(XpConfig.KEY_DA_SCOPE, XpConfig.DA_SCOPE_CLOSE_AND_HOOK)
    val exitEnable = cfg.bool(XpConfig.KEY_EXIT_ENABLE, false)
    val closeMode = cfg.int(XpConfig.KEY_DA_CLOSE_MODE, XpConfig.DA_CLOSE_CONTINUOUS)

    
    
    val hooksEnabled = enabled && daScope != XpConfig.DA_SCOPE_CLOSE_ONLY
    val itemEnabled = hooksEnabled && !master

    
    
    val onceEnabled = exitEnable && enabled
    val effectiveCloseMode = if (closeMode == 1 && !onceEnabled) 0 else closeMode

    FeatureCard(
        title = "设备管理员 / Device Owner 防护",
        checked = daEnable,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_DA_ENABLE, it) }
    ) {

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "拦截方式",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectChips(
            options = XpConfig.DA_MODE_OPTIONS,
            selectedIndex = cfg.int(XpConfig.KEY_DA_FAKE_MODE, XpConfig.DA_MODE_DEFAULT),
            enabled = enabled,
            onSelect = { cfg.put(XpConfig.KEY_DA_FAKE_MODE, it) },
        )
        HintText(
            if (cfg.int(XpConfig.KEY_DA_FAKE_MODE, XpConfig.DA_MODE_DEFAULT) ==
                XpConfig.DA_MODE_FAKE_SUCCESS
            ) {
                "伪装成功：isAdminActive 之类的查询一律回答「有」，" +
                    "锁屏 / 清数据 / 改密码这类操作一律返回成功但不会真的执行。" +
                    "只影响接口返回值 —— 下面的关闭时机、运行方式照常生效，" +
                    "持续放弃权限不受影响。"
            } else {
                "默认：查询一律回答「没有」，操作被拦掉，并会尝试摘除设备管理员。"
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "关闭时机",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectChips(
            options = XpConfig.DA_CLOSE_MODE_OPTIONS,
            selectedIndex = effectiveCloseMode,
            enabled = enabled && daScope != XpConfig.DA_SCOPE_HOOK_ONLY,
            onSelect = { idx ->
                if (idx == 1 && !onceEnabled) return@SingleSelectChips
                cfg.put(XpConfig.KEY_DA_CLOSE_MODE, idx)
            },
        )
        if (daScope == XpConfig.DA_SCOPE_HOOK_ONLY) {
            HintText("当前只运行钩子，不会关闭服务本身，因此上面的时机选项已置灰。")
        }
        if (closeMode == 1 && !exitEnable) {
            HintText("「退出前关闭一次」依赖功能一（倒计时退出）：功能一没开启时它也无法开启，已自动回退为持续关闭。")
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "运行方式",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectChips(
            options = XpConfig.DA_SCOPE_OPTIONS,
            selectedIndex = daScope,
            enabled = enabled,
            onSelect = { cfg.put(XpConfig.KEY_DA_SCOPE, it) },
        )
        HintText(
            when (daScope) {
                XpConfig.DA_SCOPE_HOOK_ONLY ->
                    "只运行钩子：拦截查询、改返回值，但绝不调用 removeActiveAdmin，权限不会被摘除。"
                XpConfig.DA_SCOPE_CLOSE_AND_HOOK ->
                    "关闭服务 + 运行钩子：既拦截查询，也按上面的关闭时机真正放弃管理员权限。"
                else ->
                    "只关闭服务：只负责放弃管理员权限，不装任何钩子，接口一律如实返回。"
            }
        )
        

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "申请拦截",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SwitchRow(
            title = "禁止主动申请成为设备管理员",
            subtitle = "应用想跳转到系统激活页时直接拦下，页面弹不出来",
            checked = cfg.bool(XpConfig.KEY_DA_BLOCK_REQUEST, false),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_DA_BLOCK_REQUEST, it) },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "可禁用的选项",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        XpConfig.DA_ITEMS.forEach { (key, title, desc) ->
            SwitchRow(
                title = title,
                subtitle = desc,
                checked = if (master) true else cfg.bool(key, true),
                enabled = itemEnabled,
                onCheckedChange = { cfg.put(key, it) },
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SwitchRow(
            title = "全部合一（总开关）",
            checked = master,
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_DA_MASTER, it) },
        )
        HintText(
            if (master) {
                        "上面 8 项全部运行，因此选项已置灰。"
            } else {
                "关闭时可按上面的选项逐项禁用；打开后上面选项会自动全部生效并置灰。"
            }
        )
    }
}




@Composable
private fun VolumeFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_VOLUME_ENABLE, false)
    FeatureCard(
        title = "阻止控制音量",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_ENABLE, it) }
    ) {
        val on = enabled && active
        val lock = cfg.bool(XpConfig.KEY_VOLUME_LOCK, false)
        
        val master = !lock && cfg.bool(XpConfig.KEY_VOLUME_MASTER, false)
        SwitchRow(
            title = "阻止任何方式控制音量（总开关）",
            checked = master,
            enabled = on && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_MASTER, it) },
        )
        HintText(
            if (master) {
                "总开关已开：应用的一切音量调用都会被忽略，下面几项不再另行判断。"
            } else {
                "总开关关闭时，按下面几项的勾选分别拦截。"
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "固定音量",
            checked = lock,
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_LOCK, it) },
        )
        val lockValue = cfg.int(XpConfig.KEY_VOLUME_LOCK_VALUE, 50)
        LabeledTextField(
            label = "固定音量值（1~100）",
            value = lockValue.toString(),
            onValueChange = { raw ->
                val v = raw.filter { it.isDigit() }.take(3)
                if (v.isEmpty()) {
                    cfg.put(XpConfig.KEY_VOLUME_LOCK_VALUE, 1)
                } else {
                    cfg.put(XpConfig.KEY_VOLUME_LOCK_VALUE, v.toInt().coerceIn(1, 100))
                }
            },
            enabled = on && lock,
        )
        HintText(
            if (lock) {
                "当前为固定音量 ${lockValue.coerceIn(1, 100)}%。"
            } else {
                "固定音量关闭时，按上面几项的勾选分别拦截。"
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "允许调小、不允许调大",
            checked = !lock && !master && cfg.bool(XpConfig.KEY_VOLUME_ALLOW_LOWER, false),
            enabled = on && !master && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_ALLOW_LOWER, it) },
        )
        SwitchRow(
            title = "阻止改变响铃模式",
            checked = !lock && (master || cfg.bool(XpConfig.KEY_VOLUME_BLOCK_RINGER, true)),
            enabled = on && !master && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_BLOCK_RINGER, it) },
        )
        SwitchRow(
            title = "阻止静音操作",
            checked = !lock && (master || cfg.bool(XpConfig.KEY_VOLUME_BLOCK_MUTE, true)),
            enabled = on && !master && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_BLOCK_MUTE, it) },
        )
        HintText("仅拦截应用主动调节；通过系统音量条手动调节不受影响（系统进程不在作用域内）。")
    }
}




@Composable
private fun ClipboardFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_CLIP_ENABLE, false)
    val mode = cfg.int(XpConfig.KEY_CLIP_MODE, 2)
    FeatureCard(
        title = "禁止读写剪贴板",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_CLIP_ENABLE, it) }
    ) {
        SingleSelectChips(
            options = XpConfig.CLIP_MODE_OPTIONS,
            selectedIndex = mode,
            enabled = enabled && active,
            onSelect = { cfg.put(XpConfig.KEY_CLIP_MODE, it) },
        )
        HintText(
            when (mode) {
                0 -> "只禁止读"
                1 -> "只禁止写"
                else -> "全部禁止"
            }
        )
    }
}




@Composable
private fun TorchVibrateFeatureCard(cfg: XpConfigState, active: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "闪光灯 / 振动",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SwitchRow(
                title = "禁止控制闪光灯（手电筒）",
                checked = cfg.bool(XpConfig.KEY_BLOCK_TORCH, false),
                enabled = active,
                onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_TORCH, it) },
            )
            HintText("只拦「开灯」，拍照时的闪光灯 OFF / AUTO 不受影响。")

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "禁止控制手机振动",
                checked = cfg.bool(XpConfig.KEY_BLOCK_VIBRATE, false),
                enabled = active,
                onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_VIBRATE, it) },
            )
            HintText("开启后目标应用完全无法让手机震动；来电、通知等系统级振动不受影响。")
        }
    }
}




@Composable
private fun FileGuardFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_FILE_GUARD_ENABLE, false)
    FeatureCard(
        title = "禁止随意创建文件",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_FILE_GUARD_ENABLE, it) }
    ) {
        val on = enabled && active
        val all = cfg.bool(XpConfig.KEY_FILE_OP_ALL, false)
        HintText(
            "即使没有文件访问权限，应用依然能通过MediaStore写自己的文件。"
        )
        XpConfig.FILE_OP_ITEMS.forEach { (key, title, desc) ->
            CheckRow(
                checked = all || cfg.bool(key, true),
                enabled = on && !all,
                onCheckedChange = { cfg.put(key, it) },
                title = title,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "全部禁用",
            checked = all,
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_FILE_OP_ALL, it) },
        )
        HintText(
            if (all) {
                "当前：全部禁用中 —— 创建、写入、读取、查询、更新、删除、" +
                        "java.io.File、java.nio.file.Files 一律拦掉。"
            } else {
                "当前：按上面的勾选逐项禁用。"
            }
        )
    }
}




@Composable
private fun HideAppsFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_HIDE_APPS_ENABLE, false)
    val mode = cfg.int(XpConfig.KEY_HIDE_APPS_MODE, 1)
    val listMode = cfg.int(XpConfig.KEY_HIDE_APPS_LIST_MODE, 0)
    var text by remember(cfg.values.value[XpConfig.KEY_HIDE_APPS_LIST]) {
        mutableStateOf(
            (cfg.values.value[XpConfig.KEY_HIDE_APPS_LIST] as? String)
                ?: XpConfig.HIDE_APPS_PRESET
        )
    }

    FeatureCard(
        title = "隐藏应用列表",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_HIDE_APPS_ENABLE, it) }
    ) {
        val on = enabled && active

        SingleSelectChips(
            options = listOf("返回空", "按名单返回"),
            selectedIndex = mode,
            enabled = on,
            onSelect = { cfg.put(XpConfig.KEY_HIDE_APPS_MODE, it) },
        )
        HintText(
            if (mode == 0) {
                "返回空：目标应用调用这两个接口时拿到一个空列表，一个应用都看不到。"
            } else {
                "按名单返回：先拿到真实列表，再按下面的白 / 黑名单过滤。"
            }
        )

        if (mode == 1) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SingleSelectChips(
                options = listOf("白名单", "黑名单"),
                selectedIndex = listMode,
                enabled = on,
                onSelect = { idx ->
                    cfg.put(XpConfig.KEY_HIDE_APPS_LIST_MODE, idx)
                    
                    if (idx == 1) {
                        text = ""
                        cfg.put(XpConfig.KEY_HIDE_APPS_LIST, "")
                    } else if (text.isBlank()) {
                        text = XpConfig.HIDE_APPS_PRESET
                        cfg.put(XpConfig.KEY_HIDE_APPS_LIST, XpConfig.HIDE_APPS_PRESET)
                    }
                },
            )
            HintText(
                if (listMode == 0) {
                    "白名单：只有名单里的包名会被返回，其余全部隐藏。"
                } else {
                    "黑名单：名单里的包名会被隐藏，其余照常返回。"
                }
            )

            Text(
                "包名（一行一个，也可以用逗号 / 空格分隔）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                enabled = on,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                minLines = 8,
                maxLines = 20,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                shape = RoundedCornerShape(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { cfg.put(XpConfig.KEY_HIDE_APPS_LIST, text) },
                    enabled = on,
                ) { Text("保存名单") }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        text = XpConfig.HIDE_APPS_PRESET
                        cfg.put(XpConfig.KEY_HIDE_APPS_LIST, XpConfig.HIDE_APPS_PRESET)
                    },
                    enabled = on,
                ) { Text("恢复预设") }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        text = ""
                        cfg.put(XpConfig.KEY_HIDE_APPS_LIST, "")
                    },
                    enabled = on,
                ) { Text("清空") }
            }
            HintText("当前生效 ${XpConfig.decodeLines(text).size} 个包名。改完记得点「保存名单」。")
        }
    }
}




@Composable
private fun AudioOutFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_AUDIO_OUT_ENABLE, false)
    FeatureCard(
        title = "阻止播放 / 发声音方式",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_AUDIO_OUT_ENABLE, it) }
    ) {
        val on = enabled && active
        XpConfig.AUDIO_OUT_ITEMS.forEach { (key, title, desc) ->
            CheckRow(
                checked = cfg.bool(key, false),
                enabled = on,
                onCheckedChange = { cfg.put(key, it) },
                title = "禁用$title",
            )
            
            if (key == XpConfig.KEY_AUDIO_OUT_MEDIA) {
                val mediaOn = on && cfg.bool(key, false)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 2.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    SwitchRow(
                        title = "· 禁用已请求音频焦点播放",
                        checked = cfg.bool(XpConfig.KEY_AUDIO_OUT_FOCUS, true),
                        enabled = mediaOn,
                        onCheckedChange = { cfg.put(XpConfig.KEY_AUDIO_OUT_FOCUS, it) },
                    )
                    SwitchRow(
                        title = "· 禁用无音频焦点并播放",
                        checked = cfg.bool(XpConfig.KEY_AUDIO_OUT_NO_FOCUS, true),
                        enabled = mediaOn,
                        onCheckedChange = { cfg.put(XpConfig.KEY_AUDIO_OUT_NO_FOCUS, it) },
                    )
                    HintText(
                        "两个都开 = 媒体输出一律禁掉；只开一个 = 只禁对应那种播放方式。"
                    )
                }
            }
        }
        val picked = XpConfig.AUDIO_OUT_ITEMS.filter { cfg.bool(it.first, false) }
        HintText(
            if (picked.isEmpty()) {
                "当前：没有禁用任何声音类别（应用可以正常出声）。"
            } else {
                "当前已禁用 ${picked.size} 类：" + picked.joinToString("、") { it.second }
            }
        )
    }
}




@Composable
private fun ShizukuFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_SHIZUKU_ENABLE, false)
    FeatureCard(
        title = "阻止使用 Shizuku",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_SHIZUKU_ENABLE, it) }
    ) {
        val on = enabled && active
        SwitchRow(
            title = "阻止申请与获取授权",
            checked = cfg.bool(XpConfig.KEY_SHIZUKU_BLOCK_AUTH, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_SHIZUKU_BLOCK_AUTH, it) },
        )
        SwitchRow(
            title = "禁用 Shizuku 服务调用",
            checked = cfg.bool(XpConfig.KEY_SHIZUKU_BLOCK_USE, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_SHIZUKU_BLOCK_USE, it) },
        )
        HintText(
                    "兼容 rikka.shizuku与 moe.shizuku.api。混淆使功能失效"
        )
    }
}




@Composable
private fun WirelessDebuggingFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_WDBG_ENABLE, false)
    FeatureCard(
        title = "阻止获取无线调试",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_ENABLE, it) }
    ) {
        val on = enabled && active
        SwitchRow(
            title = "阻止启用 / 查询开关",
            checked = cfg.bool(XpConfig.KEY_WDBG_TOGGLE, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_TOGGLE, it) },
        )
        SwitchRow(
            title = "阻止配对与连接",
            checked = cfg.bool(XpConfig.KEY_WDBG_PAIR, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_PAIR, it) },
        )
        SwitchRow(
            title = "阻止发现无线调试服务",
            checked = cfg.bool(XpConfig.KEY_WDBG_DISCOVER, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_DISCOVER, it) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "阻止跳转",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SwitchRow(
            title = "阻止跳转到无线调试等页面",
            checked = cfg.bool(XpConfig.KEY_WDBG_BLOCK_JUMP, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_BLOCK_JUMP, it) },
        )
        SwitchRow(
            title = "阻止跳转到设置",
            checked = cfg.bool(XpConfig.KEY_WDBG_BLOCK_SETTINGS, false),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_BLOCK_SETTINGS, it) },
        )
        SwitchRow(
            title = "拦掉 adb 系统属性",
            checked = cfg.bool(XpConfig.KEY_WDBG_PROP, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_PROP, it) },
        )
    }
}




@Composable
private fun SystemControlFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_BLOCK_CONN_ENABLE, false)
    FeatureCard(
        title = "禁止控制 WiFi / 蓝牙 与 屏幕亮度",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_ENABLE, it) }
    ) {
        val on = enabled && active
        HintText("开关、断开/重连、忘网、改网络配置、开热点都会被拦；纯查询不受影响。")
        SwitchRow(
            title = "禁止开关 WiFi",
            checked = cfg.bool(XpConfig.KEY_BLOCK_CONN_WIFI, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_WIFI, it) },
        )
        SwitchRow(
            title = "禁止开关蓝牙",
            checked = cfg.bool(XpConfig.KEY_BLOCK_CONN_BT, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_BT, it) },
        )
        SwitchRow(
            title = "禁止调节屏幕亮度",
            checked = cfg.bool(XpConfig.KEY_BLOCK_CONN_BRIGHT, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_BRIGHT, it) },
        )
    }
}




@Composable
private fun SensorFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "禁止获取传感器数据",
        checked = cfg.bool(XpConfig.KEY_BLOCK_SENSOR, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_SENSOR, it) }
    ) {
        HintText(
            "加速度、陀螺仪、光线、距离、磁场、计步、心率……。" +
                "除了常规的 SensorManager 注册，也一并堵住绕过它的几条路：" +
                "内部实现方法直调、事件队列派发、SensorDirectChannel 共享内存读取，" +
                "以及 native 层直接打开 /sys、/dev 下传感器节点的做法。"
        )
    }
}




@Composable
private fun JumpFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_JUMP_ENABLE, false)
    FeatureCard(
        title = "阻止跳转到其他应用",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_JUMP_ENABLE, it) }
    ) {
        val enabled = on && active
        val white = cfg.str(XpConfig.KEY_BLOCK_JUMP_WHITELIST, "")
        val count = XpConfig.decodeLines(white).size
        HintText(
            if (count == 0) {
                "当前：白名单为空 —— 全部拦截，应用发起的跳转一个都去不了。"
            } else {
                "当前：只允许跳白名单里的 $count 个包名，其它一律拦。"
            }
        )
        LabeledTextField(
            label = "白名单包名（一行一个，留空 = 全部拦截）",
            value = white,
            onValueChange = { cfg.put(XpConfig.KEY_BLOCK_JUMP_WHITELIST, it) },
            enabled = enabled,
            singleLine = false,
        )
        HintText("判断不出来目标包名的隐式跳转，一律按拦截处理。")
    }
}




@Composable
private fun BackgroundLaunchFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_BG_LAUNCH, false)
    val enabled = on && active
    FeatureCard(
        title = "禁止后台弹出界面",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_BG_LAUNCH, it) }
    ) {
        HintText(
            "退到后台后，应用靠定时器、推送、前台服务悄悄弹 Activity 的行为（广告页、唤醒页）会被直接拦掉，" +
                "连启动调用都不会执行。前台正常打开页面不受影响。"
        )
        SwitchRow(
            title = "同时拦截 PendingIntent 弹窗",
            checked = cfg.bool(XpConfig.KEY_BLOCK_BG_LAUNCH_PENDING, true),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_BG_LAUNCH_PENDING, it) },
        )
        HintText("通知、闹钟、定时任务触发的界面也拦；服务类和广播类的 PendingIntent 不受影响。")
        SwitchRow(
            title = "严格模式（有前台服务也算后台）",
            checked = cfg.bool(XpConfig.KEY_BLOCK_BG_LAUNCH_STRICT, true),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_BG_LAUNCH_STRICT, it) },
        )
        HintText(
            "开启：只要没有可见页面就拦，挂着前台服务也拦。\n" +
                "关闭：应用有前台服务在跑时放行（来电、导航等场景可以正常弹界面）。"
        )
    }
}




@Composable
private fun CameraMicFeatureCard(cfg: XpConfigState, active: Boolean) {
    val cam = cfg.bool(XpConfig.KEY_BLOCK_CAMERA_ENABLE, false)
    val mic = cfg.bool(XpConfig.KEY_BLOCK_MIC_ENABLE, false)
    FeatureCard(
        title = "摄像头 / 麦克风实时拦截",
        checked = cam || mic,
        enabled = active,
        onCheckedChange = { v ->
            cfg.put(XpConfig.KEY_BLOCK_CAMERA_ENABLE, v)
            cfg.put(XpConfig.KEY_BLOCK_MIC_ENABLE, v)
        }
    ) {
        val on = cam || mic
        SwitchRow(
            title = "拦截摄像头",
            checked = cam,
            enabled = active && on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CAMERA_ENABLE, it) },
        )
        SwitchRow(
            title = "拦截麦克风",
            checked = mic,
            enabled = active && on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_MIC_ENABLE, it) },
        )
    }
}




@Composable
private fun InstallFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "应用安装拦截",
        checked = cfg.bool(XpConfig.KEY_BLOCK_INSTALL_ENABLE, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_INSTALL_ENABLE, it) }
    ) 

}




@Composable
private fun PrintCastFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "打印 / 投屏拦截",
        checked = cfg.bool(XpConfig.KEY_BLOCK_PRINT_CAST, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_PRINT_CAST, it) }
    ) 

    
}




@Composable
private fun NotifyFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "通知发送拦截",
        checked = cfg.bool(XpConfig.KEY_BLOCK_NOTIFY, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_NOTIFY, it) }
    ) 
    
}




@Composable
private fun NetworkFilterFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_NET_FILTER_ENABLE, false)
    FeatureCard(
        title = "网络请求域名过滤(部分生效)",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_NET_FILTER_ENABLE, it) }
    ) {
        val enabled = on && active
        val wl = cfg.bool(XpConfig.KEY_NET_FILTER_WHITELIST, false)
        SwitchRow(
            title = "白名单模式（只放行名单里的）",
            checked = wl,
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_NET_FILTER_WHITELIST, it) },
        )
        val list = cfg.str(XpConfig.KEY_NET_FILTER_LIST, "")
        LabeledTextField(
            label = "${if (wl) "白" else "黑"}名单域名（一行一个，如 example.com）",
            value = list,
            onValueChange = { cfg.put(XpConfig.KEY_NET_FILTER_LIST, it) },
            enabled = enabled,
            singleLine = false,
        )
    }
}




@Composable
private fun ScreenCaptureFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_SCREEN_CAPTURE, false)
    val mode = cfg.int(XpConfig.KEY_SCREEN_CAPTURE_MODE, XpConfig.SC_MODE_BLANK)
    var showGuide by remember { mutableStateOf(false) }
    FeatureCard(
        title = "拦截屏幕捕获",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_SCREEN_CAPTURE, it) }
    ) {
        val enabled = on && active
        Text(
            "拦截策略",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectChips(
            options = listOf("(a) 图片/视频", "(b) 静态文字", "(c) 拒绝授权"),
            selectedIndex = mode.coerceIn(0, 2),
            onSelect = { cfg.put(XpConfig.KEY_SCREEN_CAPTURE_MODE, it) },
            enabled = enabled,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            HintText(
                when (mode) {
                    XpConfig.SC_MODE_IMAGE ->
                        "(a) 把素材画进 Surface，并接管 ImageReader —— 对方看到你放的图/视频/音频。"
                    XpConfig.SC_MODE_BLANK ->
                        "(b) Surface 画纯文字、ImageReader 拿不到帧 —— 对方只看到灰底文字。"
                    else ->
                        "(c) 弹不出授权框、getMediaProjection 返回 null —— 拿不到令牌，根本开始不了。"
                },
                Modifier.weight(1f),
            )
            TextButton(onClick = { showGuide = true }, enabled = enabled) {
                Text("详细说明", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (showGuide) ScreenCaptureGuideDialog(mode = mode, onDismiss = { showGuide = false })
        if (mode == XpConfig.SC_MODE_IMAGE) {
            SwitchRow(
                title = "画面自适应",
                checked = cfg.bool(XpConfig.KEY_SCREEN_CAPTURE_FIT, true),
                enabled = enabled,
                onCheckedChange = { cfg.put(XpConfig.KEY_SCREEN_CAPTURE_FIT, it) },
            )
            HintText(
                "开启时保持素材原始比例居中显示，多余部分补黑边；关闭则拉伸铺满整个画面。"
            )
        }
        if (mode == XpConfig.SC_MODE_DENY) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "(c) 的子选项",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SwitchRow(
                title = "全部方面拦截并拒绝",
                checked = !cfg.bool(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, !it) },
            )
            SwitchRow(
                title = "开启捕获但返回授权成功",
                checked = cfg.bool(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, it) },
            )
        }
    }
}






private val SC_GUIDE_A = """
素材放哪儿
· 目录：/storage/emulated/0/Android/data/{目标应用包名}/files/
· 命名：media_projection.mp4、media_projection_2.mp4、media_projection_3.mp4 …
  下划线可有可无（mediaprojection1.mp4 同样识别），数字决定顺序，没数字算 1。
· 后缀：mp4 / jpg·png / aac·m4a·mp3·wav·ogg·opus·flac 等。
  实际类型按文件内容探测。

自动播放
· 以第一个「有进度」的素材的类型为准，只在同一类型内按顺序循环：
  视频1 + 音频2 + 视频3 → 播 1、3（跳过 2）
  音频1 + 图片2 + 音频3 → 播 1、3（跳过 2）
· 图片没有进度，不参与自动切换，只能手动切。
· 只有一个素材就是它自己循环；素材是音频时，画面显示方案(b)的灰底文字，中间显示该文件名。

音量键手动切换（录制中生效）
· 音量下 = 下一个素材，音量上 = 上一个素材，在所有素材（含图片）里按顺序翻。
  音量变化即触发；触发后 3 秒内不重复响应，期间音量照常变化、不影响系统音量。
· 切到图片 → 一直静态显示；切到音频/视频 → 播完这一个后，自动序列接着播下一个。
· 手动切换期间自动计时暂停，不会被自动切走。

音频
· 录屏音轨（MediaProjection）和 AudioPlaybackCapture 都会被替换成素材音频。
· 画面素材本身没有音轨时，用目录里第一个有音轨的文件补上。

画面比例
· 「画面自适应」开启时保持素材原始比例居中显示，多余部分补黑边；关闭则拉伸铺满。
· 每次开始录制都从第 1 个素材的开头播放，不会接着上次的位置。
""".trimIndent()

private val SC_GUIDE_B = """
方案(b) 不使用任何素材：
· 捕获到的 Surface 被画成灰底 + 中间一行「内容已被拦截」。
· ImageReader 拿不到帧，SurfaceControl.screenshot / PixelCopy 等系统截图通道也一并拦掉。
· 音频仍然会被替换（若目录里有可用音频素材）或输出静音，保证录出来的时长正常。
""".trimIndent()

private val SC_GUIDE_C = """
方案(c) 从源头掐断：
· createScreenCaptureIntent 弹不出授权框，getMediaProjection 返回 null，
  对方拿不到令牌，根本开始不了录制。
· 子选项「开启捕获但返回授权成功」：返回一个空的授权 Intent，
  让对方以为自己拿到了权限（适合对付拿不到权限就崩溃/卡死的 App）。
""".trimIndent()

@Composable
private fun ScreenCaptureGuideDialog(mode: Int, onDismiss: () -> Unit) {
    val title = when (mode) {
        XpConfig.SC_MODE_IMAGE -> "(a) 图片/视频 —— 详细说明"
        XpConfig.SC_MODE_BLANK -> "(b) 静态文字 —— 详细说明"
        else -> "(c) 拒绝授权 —— 详细说明"
    }
    val body = when (mode) {
        XpConfig.SC_MODE_IMAGE -> SC_GUIDE_A
        XpConfig.SC_MODE_BLANK -> SC_GUIDE_B
        else -> SC_GUIDE_C
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) { Text("知道了") }
                }
            }
        }
    }
}
