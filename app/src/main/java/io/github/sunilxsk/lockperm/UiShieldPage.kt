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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        ActiveShield(cfg, enabled)
        Spacer(Modifier.height(24.dp))
    }
}





@Composable
private fun ActiveShield(cfg: XpConfigState, enabled: Boolean) {
    val active = enabled

    
    FeatureCard(
        title = "悬浮窗式便捷功能",
        subtitle = "在目标软件左上方挂一个悬浮窗，点里面的按钮立即生效",
        checked = cfg.bool(XpConfig.KEY_PANEL_INJECT, true),
        enabled = enabled,
        onCheckedChange = { cfg.put(XpConfig.KEY_PANEL_INJECT, it) }
    ) {
        HintText(
            "😜😜😜"
        )
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
    CameraMicFeatureCard(cfg, active)
    InstallFeatureCard(cfg, active)
    PrintCastFeatureCard(cfg, active)
    NotifyFeatureCard(cfg, active)
    NetworkFilterFeatureCard(cfg, active)
    ScreenCaptureFeatureCard(cfg, active)

    FeatureCard(
        title = "悬浮窗功能",
        subtitle = "Hook 标准的悬浮窗创建方法（WindowManager.addView），阻止系统级悬浮窗出现！🫥",
        checked = cfg.bool(XpConfig.KEY_BLOCK_OVERLAY, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_OVERLAY, it) }
    ) {
        HintText("仅拦截需要「显示在其他应用上层」权限的那一类窗口，普通 Toast、应用内弹窗不受影响。")
    }

    ExecBlockFeatureCard(cfg, active)

    FeatureCard(
        title = "替换壁纸功能",
        subtitle = "阻止应用用标准方式随意替换手机壁纸",
        checked = cfg.bool(XpConfig.KEY_BLOCK_WALLPAPER, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_WALLPAPER, it) }
    ) {
        HintText("Hook WallpaperManager 的 setBitmap / setStream / setResource 等写入接口🧐")
    }

    DeviceAdminFeatureCard(cfg, active)
}




@Composable
private fun WifiFakeFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_WIFI_FAKE_ENABLE, false)
    val enabled = on && active

    FeatureCard(
        title = "伪装 WiFi 连接状态与列表",
        subtitle = "断网时让应用以为已经连上了 WiFi，并可自定义扫描到的热点列表",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_ENABLE, it) }
    ) {
        HintText(
            "⚠️ 缺点：这只是把系统上报给应用的状态改掉，并不会真的产生网络通路。" +
                    "所以「必须联网才能进」的界面能进去，但凡是真正发请求的操作依然会失败"
        )

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
            subtitle = "ConnectivityManager / NetworkCapabilities / NetworkInfo 一律回答 WiFi 已连接",
            checked = cfg.bool(XpConfig.KEY_WIFI_FAKE_NETWORK, true),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_NETWORK, it) },
        )
        SwitchRow(
            title = "伪装 WiFi 扫描结果",
            subtitle = "getScanResults() 返回下面列表里的热点",
            checked = cfg.bool(XpConfig.KEY_WIFI_FAKE_SCAN, true),
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_WIFI_FAKE_SCAN, it) },
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
            "不填列表时不改扫描结果（用真机扫到的）。填了就用你的列表完全替换。🙂" +
                    "字段名用 | 分隔，后两项可省略：HOME-WIFI|02:1a:2b:3c:4d:5e|-42|[WPA2-PSK-CCMP][ESS]"
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
        subtitle = "倒计时多少秒后退出",
        checked = exitEnable,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_EXIT_ENABLE, it) }
    ) {
        val enabled = exitEnable && active

        SwitchRow(
            title = "倒计时后退出",
            subtitle = "进入目标应用后开始倒计时，归零时按下方方式退出",
            checked = exitEnable,
            enabled = active,
            onCheckedChange = { cfg.put(XpConfig.KEY_EXIT_ENABLE, it) },
        )

        LabeledTextField(
            label = "倒计时秒数",
            value = seconds.toString(),
            enabled = enabled,
            onValueChange = { raw ->
                val v = raw.filter { it.isDigit() }.take(5).toIntOrNull()
                if (v != null) cfg.put(XpConfig.KEY_EXIT_SECONDS, v)
            },
            modifier = Modifier.width(160.dp),
            fillMax = false,
        )

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
            subtitle = "多种杀法一起运行，没有前后顺序（开启后上面的单项不可选）",
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
        HintText(
            if (parallel) {
                "当前：所有杀法同时启动（用栅栏统一放行，保证真正并行）。"
            } else {
                "当前：按勾选顺序依次执行，每种之间留 80ms。"
            }
        )
        HintText(
            "方式说明：" + XpConfig.EXIT_METHODS.joinToString("、") { (k, label) ->
                "$label = ${XpConfig.EXIT_METHOD_DETAIL[k]}"
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
        subtitle = "拦截 Java 层任何 exec 方式（Runtime.exec / ProcessBuilder / ProcessImpl）",
        checked = blockExec,
        enabled = active,
        onCheckedChange = { on ->
            cfg.put(XpConfig.KEY_BLOCK_EXEC, on)
            if (on) switchExitMethodAwayFromExec(cfg)
        }
    ) {
        if (!exitEnable) {
            HintText("提示：倒计时退出当前未开启，互斥切换只影响退出选项的勾选状态。")
        }
        HintText(
            "注意：目标应用如果靠执行命令行工具实现核心功能（例如ping、ffmpeg、busybox 开启后这些功能会一起失效。"
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
    
    val capEnabled = accEnable && active && scope != 0
    
    val modeEnabled = accEnable && active && scope != 2

    FeatureCard(
        title = "无障碍功能",
        subtitle = "禁止开启无障碍，并破坏无障碍能做到的每一件事",
        checked = accEnable,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_ACC_ENABLE, it) }
    ) {
        SwitchRow(
            title = "禁止开启无障碍",
            subtitle = "捕获无障碍服务实例并反复关闭它",
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
            subtitle = "只改 AccessibilityManager 汇报给应用的值，不影响真正的拦截效果",
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
        HintText(
            if (!statusSpoof) {
                "未开启：应用读到的就是手机上的真实状态。❌"
            } else if (!statusValue) {
                "应用查询时会得到「无障碍未开启」。这只是汇报值，下面勾选的限制功能依然照常拦截，" +
                        "两者互不干扰；和「伪装」页里的无障碍权限伪装也不互斥。"
            } else {
                "应用查询时会得到「无障碍已开启」。同样只是汇报值，不会真的把权限给它。"
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "禁用开启无障碍子功能",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        HintText("以下全部是安卓 Java 标准接口，逐个 Hook 破坏，让目标软件就算拉起服务也拿不到东西。")

        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_SCREEN, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_SCREEN, it) },
            title = "禁用读取屏幕内容 / 页面结构",
            subtitle = "拦截 getRootInActiveWindow、getText、getChild、getBoundsInScreen 等，" +
                    "拿不到任何文字、按钮、输入框、列表项，也拿不到控件位置与可否点击",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_NOTIFY, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_NOTIFY, it) },
            title = "禁用监听通知",
            subtitle = "拦截通知类事件与 getText / getParcelableData，读不到通知标题与正文",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_WINDOW, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_WINDOW, it) },
            title = "禁用监听窗口变化",
            subtitle = "拦截窗口类事件与 getWindows / getClassName / getPackageName，" +
                    "不知道当前打开了哪个应用、弹了什么对话框",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_INPUT, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_INPUT, it) },
            title = "禁用读取输入内容",
            subtitle = "拦截 getBeforeText 与文本变化事件，读不到用户正在输入的文字",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_ACTION, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_ACTION, it) },
            title = "禁用全部模拟操作",
            subtitle = "拦截 performAction / performGlobalAction / dispatchGesture：" +
                    "点击、滑动、返回主页、输入文字、长按拖拽、打开通知栏锁屏截屏全部失效",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_OVERLAY, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_OVERLAY, it) },
            title = "禁用无障碍专属悬浮窗",
            subtitle = "无障碍服务不用申请「显示在其他应用上层」也能弹窗，" +
                    "靠的是系统给它的 TYPE_ACCESSIBILITY_OVERLAY。双重保底：" +
                    "① 不让创建这类窗口；② 每 0.7 秒扫描并强制关掉已存在的",
        )
        CheckRow(
            checked = cfg.bool(XpConfig.KEY_ACC_CAP_CONTROL, true),
            enabled = capEnabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_ACC_CAP_CONTROL, it) },
            title = "禁用按键 / 手势监听与屏幕控制",
            subtitle = "拦截 onKeyEvent（吞音量键、多任务键）、onGesture、onMotionEvent、指纹手势，" +
                    "并抹掉服务声明里的监听按键 / 触摸探测 flag，" +
                    "顺带拦掉放大屏幕、控软键盘、截屏、改动画缩放",
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "禁用时机（互斥，二选一）",
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
        if (scope == 2) {
            HintText("当前是「只运行上面的 hook 点」：不去关服务，所以上面两个时机选项已置灰。")
        }
        if (mode == 1 && !exitEnable) {
            HintText("「倒计时前禁用一次」依赖功能一（倒计时退出）：功能一没开启时它也无法开启，已自动回退为持续禁用。")
        }

        Text(
            "运行范围（互斥，三选一）",
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
                0 -> "只运行纯关闭无障碍：只调用 disableSelf 关掉服务，上面的子功能不可用（已禁用）。"
                1 -> "关闭无障碍 + 全部 hook 破坏：既关服务，又把上面勾选项对应的接口全部破坏掉。"
                else -> "只运行上面的 hook 点：完全不去关服务，只把上面勾选的接口破坏掉，" +
                        "因此上面的「持续禁用 / 倒计时前禁用一次」会置灰。"
            }
        )
    }
}




@Composable
private fun DeviceAdminFeatureCard(cfg: XpConfigState, active: Boolean) {
    val daEnable = cfg.bool(XpConfig.KEY_DA_ENABLE, false)
    val master = cfg.bool(XpConfig.KEY_DA_MASTER, false)
    val enabled = daEnable && active
    
    val itemEnabled = enabled && !master

    FeatureCard(
        title = "设备管理员 / Device Owner 防护",
        subtitle = "阻止应用滥用设备管理员（含超级管理员）权限",
        checked = daEnable,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_DA_ENABLE, it) }
    ) {
        HintText(
            "设备管理员能做的事远不止弹窗：锁屏、改密码、擦除数据、禁用相机、静默装卸载应用、" +
                    "配 VPN / Kiosk 模式、替其它应用批权限。下面逐个按安卓标准接口禁用掉。"
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
            subtitle = "直接阻止授权 + 持续 removeActiveAdmin 放弃权限 + 阻止发起授权 + 运行上面全部禁用功能",
            checked = master,
            enabled = enabled,
            onCheckedChange = { cfg.put(XpConfig.KEY_DA_MASTER, it) },
        )
        HintText(
            if (master) {
                "已开启：① isAdminActive 一律返回 false；② ACTION_ADD_DEVICE_ADMIN 之类的授权请求被拦掉；" +
                        "③ 每 4 秒持续调用 removeActiveAdmin() 放弃权限" +
                        "（注入之后这段代码就是应用自己本身，所以调用真的会生效）；" +
                        "④ 上面 8 项全部运行，因此选项已置灰。"
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
        subtitle = "防止应用调节媒体 / 通话 / 铃声 / 闹钟 / 通知等音量",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_ENABLE, it) }
    ) {
        val on = enabled && active
        val lock = cfg.bool(XpConfig.KEY_VOLUME_LOCK, false)
        
        val master = !lock && cfg.bool(XpConfig.KEY_VOLUME_MASTER, false)
        SwitchRow(
            title = "阻止任何方式控制音量（总开关）",
            subtitle = if (lock) {
                "已由「固定音量」接管 —— 开固定音量时这一项自动关闭"
            } else {
                "调高、调低、调满……一律拦掉，不留例外"
            },
            checked = master,
            enabled = on && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_MASTER, it) },
        )
        HintText(
            if (master) {
                "总开关已开：应用的一切音量调用都会被忽略（含响铃模式与静音），下面几项不再另行判断。"
            } else {
                "总开关关闭时，按下面几项的勾选分别拦截。"
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "固定音量",
            subtitle = "进入软件后高频把音量锁在我填的值上，上面几项会自动关闭并置灰",
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
                "当前：每 0.2 秒把所有音量流设到 ${lockValue.coerceIn(1, 100)}%。" +
                        "固定音量不拦应用改音量。"
            } else {
                "固定音量关闭时，按上面几项的勾选分别拦截。"
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "允许调小、不允许调大",
            subtitle = "关闭 = 完全忽略应用的一切音量调用；打开 = 只拦「调大」那一次",
            checked = !lock && !master && cfg.bool(XpConfig.KEY_VOLUME_ALLOW_LOWER, false),
            enabled = on && !master && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_ALLOW_LOWER, it) },
        )
        SwitchRow(
            title = "阻止改变响铃模式",
            subtitle = "禁止在静音 / 振动 / 响铃之间切换：setRingerMode",
            checked = !lock && (master || cfg.bool(XpConfig.KEY_VOLUME_BLOCK_RINGER, true)),
            enabled = on && !master && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_BLOCK_RINGER, it) },
        )
        SwitchRow(
            title = "阻止静音操作",
            subtitle = "禁止 setStreamMute / setMasterMute / setMicrophoneMute",
            checked = !lock && (master || cfg.bool(XpConfig.KEY_VOLUME_BLOCK_MUTE, true)),
            enabled = on && !master && !lock,
            onCheckedChange = { cfg.put(XpConfig.KEY_VOLUME_BLOCK_MUTE, it) },
        )
        HintText(
            "Hook 的是 AudioManager 的 setStreamVolume / adjustStreamVolume / adjustVolume / " +
                    "adjustMasterVolume / setRingerMode 以及静音接口，" +
                    "另外还覆盖了 AudioTrack / MediaPlayer 的 setVolume（应用直接改播放器音量的路子）。"
        )
        HintText("只拦应用主动改音量；自己用系统音量条调不受影响。")
    }
}




@Composable
private fun ClipboardFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_CLIP_ENABLE, false)
    val mode = cfg.int(XpConfig.KEY_CLIP_MODE, 2)
    FeatureCard(
        title = "禁止读写剪贴板",
        subtitle = "阻止应用偷偷读取或写入你的剪贴板内容",
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
                0 -> "只禁止读：应用读剪贴板一律拿到空（hasPrimaryClip 返回 false、getText 返回 null），写入不受影响。"
                1 -> "只禁止写：应用写入会被直接丢弃，读取不受影响。"
                else -> "全部禁止：既读不到也写不进。"
            }
        )
        HintText(
            "android.content.ClipboardManager 的 getPrimaryClip / getText / " +
                    "hasPrimaryClip / setPrimaryClip / setText / clearPrimaryClip 等标准接口。"
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
                subtitle = "拦掉 CameraManager.setTorchMode、Camera.Parameters.setFlashMode 及厂商私有开灯接口",
                checked = cfg.bool(XpConfig.KEY_BLOCK_TORCH, false),
                enabled = active,
                onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_TORCH, it) },
            )
            HintText("只拦「开灯」，拍照时的闪光灯 OFF / AUTO 不受影响。")

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "禁止控制手机振动",
                subtitle = "拦掉 Vibrator.vibrate / cancel，以及 Android 12+ 的 VibratorManager",
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
        subtitle = "拦截往公共目录写文件的整套标准做法（MediaStore + File API）",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_FILE_GUARD_ENABLE, it) }
    ) {
        val on = enabled && active
        val all = cfg.bool(XpConfig.KEY_FILE_OP_ALL, false)
        HintText(
            "即使没有权限，Android 10+ 的应用依然能通过 " +
                    "MediaStore.往某些目录写自己的文件。" +
                    "下面勾了哪一项，那一项的能力就会被禁用；" +
                    "比如只想让它读、不想让它写，就只勾「禁用写入」相关的项。"
        )
        XpConfig.FILE_OP_ITEMS.forEach { (key, title, desc) ->
            CheckRow(
                checked = all || cfg.bool(key, true),
                enabled = on && !all,
                onCheckedChange = { cfg.put(key, it) },
                title = title,
                subtitle = desc,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = "全部禁用",
            subtitle = "打开后上面每一项都默认执行禁用（上面的单项会置灰不可改）",
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
        HintText(
            "ContentResolver 只拦媒体库（authority 为 media）的 Uri，" +
                    "应用自己的 ContentProvider 不受影响；File / Files 只拦 /sdcard、/storage、" +
                    "/mnt 下的公共存储，应用私有目录不受影响。"
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
        subtitle = "让目标应用枚举不到你装了哪些软件（PackageManager.getInstalledApplications / getInstalledPackages）",
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
                    "白名单：只有名单里的包名会被返回，其余全部隐藏。默认已填好一批常见系统程序与常用软件。"
                } else {
                    "黑名单：名单里的包名会被隐藏，其余照常返回。黑名单不提供预设，输入框初始为空。"
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
        subtitle = "按声音类别禁用音频输出，被禁用的类别应用就发不出声",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_AUDIO_OUT_ENABLE, it) }
    ) {
        val on = enabled && active
        HintText(
            "下面勾了哪一项，那一项的声音就会被禁用，默认全部关闭。" +
                    "不管应用用的是 MediaPlayer、AudioTrack、SoundPool、TextToSpeech 还是 Ringtone，" +
                    "最终都要走一条出声的调用，模块在这些出口上按声音类别拦。"
        )
        XpConfig.AUDIO_OUT_ITEMS.forEach { (key, title, desc) ->
            CheckRow(
                checked = cfg.bool(key, false),
                enabled = on,
                onCheckedChange = { cfg.put(key, it) },
                title = "禁用$title",
                subtitle = desc,
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
                        subtitle = "先调 AudioManager.requestAudioFocus 拿到焦点、再开始播放的（正规播放器）",
                        checked = cfg.bool(XpConfig.KEY_AUDIO_OUT_FOCUS, true),
                        enabled = mediaOn,
                        onCheckedChange = { cfg.put(XpConfig.KEY_AUDIO_OUT_FOCUS, it) },
                    )
                    SwitchRow(
                        title = "· 禁用无音频焦点并播放",
                        subtitle = "不申请焦点、直接 new MediaPlayer / AudioTrack 就 play 的",
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
        HintText(
            "类别靠 AudioAttributes 的 usage（或老式的 streamType）判定；" +
                    "判定不出来时归到「媒体输出」，所以只勾某一类不会误伤其它类别。"
        )
        HintText(
            "提示：这一项改的是应用自己发出来的声音，来电、通知等由系统播放的声音不受影响。"
        )
    }
}




@Composable
private fun ShizukuFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_SHIZUKU_ENABLE, false)
    FeatureCard(
        title = "阻止使用 Shizuku",
        subtitle = "拦掉向 Shizuku 申请授权的一切途径，并破坏已授权后的调用(🥲)",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_SHIZUKU_ENABLE, it) }
    ) {
        val on = enabled && active
        SwitchRow(
            title = "阻止申请与获取授权",
            subtitle = "requestPermission / checkPermission / 授权结果回调 全部按「已拒绝」处理",
            checked = cfg.bool(XpConfig.KEY_SHIZUKU_BLOCK_AUTH, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_SHIZUKU_BLOCK_AUTH, it) },
        )
        SwitchRow(
            title = "破坏 Shizuku 服务调用",
            subtitle = "getBinder / pingBinder / newProcess 一律返回空或 false",
            checked = cfg.bool(XpConfig.KEY_SHIZUKU_BLOCK_USE, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_SHIZUKU_BLOCK_USE, it) },
        )
        HintText(
            "Shizuku 能让普通应用以 shell 权限执行操作，风险很高。" +
                    "兼容 rikka.shizuku（新）与 moe.shizuku.api（老）两套包名。"
        )
        HintText(
            "拦掉授权申请后，模块会主动回调一次「已拒绝」，" +
                    "避免应用的界面一直卡在等待授权结果上。"
        )
        HintText(
            "注意：如果目标应用把 Shizuku 相关代码做了混淆（类名 / 方法名被改名），" +
                    "按名字匹配的这几层就匹配不上，功能可能不生效。"
        )
    }
}




@Composable
private fun WirelessDebuggingFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_WDBG_ENABLE, false)
    FeatureCard(
        title = "阻止获取无线调试",
        subtitle = "从各种标准与变种入口堵死无线调试（ADB over Wi-Fi）",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_ENABLE, it) }
    ) {
        val on = enabled && active
        SwitchRow(
            title = "阻止启用 / 查询开关",
            subtitle = "AdbManager 与 IAdbManager 的 isAdbWifiEnabled / enableAdbWireless / disableAdbWireless",
            checked = cfg.bool(XpConfig.KEY_WDBG_TOGGLE, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_TOGGLE, it) },
        )
        SwitchRow(
            title = "阻止配对与连接",
            subtitle = "pair / unpair / connect / disconnect / getPairedDevices 一律失败",
            checked = cfg.bool(XpConfig.KEY_WDBG_PAIR, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_PAIR, it) },
        )
        SwitchRow(
            title = "阻止发现无线调试服务",
            subtitle = "拦掉 mDNS 里 _adb-tls-connect / _adb-tls-pairing 的发现、解析与注册",
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
            subtitle = "拦掉跳「无线调试」/「开发者选项」/ adb 相关页面的 Intent，让它跳不过去",
            checked = cfg.bool(XpConfig.KEY_WDBG_BLOCK_JUMP, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_BLOCK_JUMP, it) },
        )
        SwitchRow(
            title = "阻止跳转到「设置」（范围较大）",
            subtitle = "凡是 com.android.settings 的跳转一律拦掉，可能影响应用内其它设置入口",
            checked = cfg.bool(XpConfig.KEY_WDBG_BLOCK_SETTINGS, false),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_BLOCK_SETTINGS, it) },
        )
        HintText(
            "「阻止跳转到设置」管得太广，默认关闭 —— 打开后应用里所有跳系统设置的入口都会失效，" +
                    "只在你需要彻底封死时再开。"
        )
        SwitchRow(
            title = "顺带拦掉 adb 系统属性",
            subtitle = "SystemProperties 里 persist.adb.* / service.adb.* 等属性的读写，以及 Settings 的 adb_wifi_enabled",
            checked = cfg.bool(XpConfig.KEY_WDBG_PROP, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_WDBG_PROP, it) },
        )
        HintText(
            "无线调试一旦被打开，持有者就相当于拿到了 adb shell 权限，风险极高。" +
                    "这里堵的是应用进程里能碰到的入口；系统设置里手动开关不受影响。"
        )
    }
}




@Composable
private fun SystemControlFeatureCard(cfg: XpConfigState, active: Boolean) {
    val enabled = cfg.bool(XpConfig.KEY_BLOCK_CONN_ENABLE, false)
    FeatureCard(
        title = "禁止控制 WiFi / 蓝牙 与 屏幕亮度",
        subtitle = "阻止应用随意开关 WiFi、蓝牙，或擅自调节屏幕亮度",
        checked = enabled,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_ENABLE, it) }
    ) {
        val on = enabled && active
        SwitchRow(
            title = "禁止开关 WiFi（含热点）",
            subtitle = "WifiManager.setWifiEnabled / setWifiApEnabled / 本地热点 一律拦掉",
            checked = cfg.bool(XpConfig.KEY_BLOCK_CONN_WIFI, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_WIFI, it) },
        )
        SwitchRow(
            title = "禁止开关蓝牙",
            subtitle = "BluetoothAdapter.enable() / disable() 一律拦掉",
            checked = cfg.bool(XpConfig.KEY_BLOCK_CONN_BT, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_BT, it) },
        )
        SwitchRow(
            title = "禁止调节屏幕亮度",
            subtitle = "拦掉写系统亮度设置、以及改窗口 screenBrightness 的两种做法",
            checked = cfg.bool(XpConfig.KEY_BLOCK_CONN_BRIGHT, true),
            enabled = on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CONN_BRIGHT, it) },
        )
        HintText(
            "只拦应用主动改；下拉通知栏或进设置开关不受影响。"
        )
    }
}




@Composable
private fun SensorFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "禁止获取传感器数据",
        subtitle = "拦掉注册监听，传感器列表返回空，默认传感器返回 null",
        checked = cfg.bool(XpConfig.KEY_BLOCK_SENSOR, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_SENSOR, it) }
    ) {
        HintText(
            "加速度、陀螺仪、光线、距离、磁场、计步、心率……全部拿不到数据。"
        )
        HintText(
            "拦掉了 registerListener —— 注册不了回调不会来；" +
                    "getSensorList 给空列表、getDefaultSensor 给 null，让应用连查询都查不到。"
        )
    }
}




@Composable
private fun JumpFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_JUMP_ENABLE, false)
    FeatureCard(
        title = "阻止跳转到其他应用",
        subtitle = "拦掉 startActivity 等跳转，应用跳不到别的程序",
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
private fun CameraMicFeatureCard(cfg: XpConfigState, active: Boolean) {
    val cam = cfg.bool(XpConfig.KEY_BLOCK_CAMERA_ENABLE, false)
    val mic = cfg.bool(XpConfig.KEY_BLOCK_MIC_ENABLE, false)
    FeatureCard(
        title = "摄像头 / 麦克风实时拦截",
        subtitle = "即使应用已经拿到权限，也让它在运行时打不开硬件",
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
            subtitle = "CameraManager.openCamera 抛异常，Camera.open 返回 null",
            checked = cam,
            enabled = active && on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_CAMERA_ENABLE, it) },
        )
        SwitchRow(
            title = "拦截麦克风",
            subtitle = "AudioRecord.startRecording / MediaRecorder.start 一律拦",
            checked = mic,
            enabled = active && on,
            onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_MIC_ENABLE, it) },
        )
        HintText(
            "和「权限伪装」互补：权限伪装是骗它以为没权限，" +
                    "这个是真的拦掉打开硬件的接口 —— 权限伪装拦不住的情况可以靠这里。"
        )
    }
}




@Composable
private fun InstallFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "应用安装拦截",
        subtitle = "阻止静默安装 / 引导安装 APK",
        checked = cfg.bool(XpConfig.KEY_BLOCK_INSTALL_ENABLE, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_INSTALL_ENABLE, it) }
    ) {
        HintText(
            "拦三类：PackageInstaller 的 createSession / commit、" +
                    "DevicePolicyManager 的安装接口、以及 ACTION_VIEW(apk) 与 ACTION_INSTALL_PACKAGE 的跳转。"
        )
        HintText("data 以 .apk 结尾的 Intent 也会拦。")
    }
}




@Composable
private fun PrintCastFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "打印 / 投屏拦截",
        subtitle = "拦掉打印与媒体路由（投屏）",
        checked = cfg.bool(XpConfig.KEY_BLOCK_PRINT_CAST, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_PRINT_CAST, it) }
    ) {
        HintText(
            "PrintManager.print / PrintJob.start 拦掉；" +
                    "MediaRouter 的 selectRoute / addCallback 也拦（含 androidx 与 support 版本）。"
        )
    }
}




@Composable
private fun NotifyFeatureCard(cfg: XpConfigState, active: Boolean) {
    FeatureCard(
        title = "通知发送拦截",
        subtitle = "即使有权限也发不出通知",
        checked = cfg.bool(XpConfig.KEY_BLOCK_NOTIFY, false),
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_BLOCK_NOTIFY, it) }
    ) {
        HintText(
            "NotificationManager.notify / createNotificationChannel" +
                    "androidx 的 NotificationManagerCompat 拦掉。"
        )
        HintText(
            "和「通知权限伪装」互补"
        )
    }
}




@Composable
private fun NetworkFilterFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_NET_FILTER_ENABLE, false)
    FeatureCard(
        title = "网络请求域名过滤",
        subtitle = "按域名黑名单 / 白名单拦掉网络请求",
        checked = on,
        enabled = active,
        onCheckedChange = { cfg.put(XpConfig.KEY_NET_FILTER_ENABLE, it) }
    ) {
        val enabled = on && active
        val wl = cfg.bool(XpConfig.KEY_NET_FILTER_WHITELIST, false)
        SwitchRow(
            title = "白名单模式",
            subtitle = "关闭 = 黑名单模式",
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
        HintText("共 ${XpConfig.decodeLines(list).size} 条。子域名自动匹配（填 example.com 也会命中 a.example.com）。")
        HintText(
            "覆盖 URL.openConnection / openStream、HttpURLConnection.connect，" +
                    "以及 OkHttp 的 newCall（应用真的用了 OkHttp 才生效）。"
        )
    }
}




@Composable
private fun ScreenCaptureFeatureCard(cfg: XpConfigState, active: Boolean) {
    val on = cfg.bool(XpConfig.KEY_BLOCK_SCREEN_CAPTURE, false)
    val mode = cfg.int(XpConfig.KEY_SCREEN_CAPTURE_MODE, XpConfig.SC_MODE_BLANK)
    FeatureCard(
        title = "拦截屏幕捕获",
        subtitle = "录屏 / 投屏 / 截图：让对方拿不到真实画面或拿不到授权",
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
        HintText(
            when (mode) {
                XpConfig.SC_MODE_IMAGE ->
                    "(a) 把一张静态图/视频帧画进 Surface，同时 ImageReader  —— 对方看到的是我们的图/视频。\n目录：/storage/emulated/0/Android/data/{包名}/files/\n文件名：media[_-]?projection.(jpg|jpeg|png|mp4)，忽略大小写"
                XpConfig.SC_MODE_BLANK ->
                    "(b) Surface 画纯文字、ImageReader —— 对方看到的就是灰色背景中间文字。"
                else ->
                    "(c) 弹不出授权框、getMediaProjection 返回 null —— 拿不到令牌，根本开始不了。"
            }
        )
        if (mode == XpConfig.SC_MODE_DENY) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "(c) 的子选项",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SwitchRow(
                title = "全部方面拦截并拒绝",
                subtitle = "授权结果直接给 RESULT_CANCELED，令牌给 null，彻底不给",
                checked = !cfg.bool(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, !it) },
            )
            SwitchRow(
                title = "开启捕获但返回授权成功",
                subtitle = "resultCode 给 RESULT_OK 骗它以为授权了，但令牌仍为 null（避免反复弹框）",
                checked = cfg.bool(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, false),
                enabled = enabled,
                onCheckedChange = { cfg.put(XpConfig.KEY_SCREEN_CAPTURE_GRANT_OK, it) },
            )
        }
        HintText(
            "另外拦掉 SurfaceControl.screenshot 与测试库那套截图接口；" +
                    "Android 的屏幕内容走 MediaProjection → VirtualDisplay → Surface，" +
                    "伪造画面已基本实现。"
        )
    }
}