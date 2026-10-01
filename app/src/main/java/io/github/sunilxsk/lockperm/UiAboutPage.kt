package io.github.sunilxsk.lockperm

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



private const val DOC_README = "README"

private const val URL_README =
    "https://raw.githubusercontent.com/sunilxsk/LockPerm/main/README.md"



@Composable
fun AboutPage(service: XposedService?) {
    val context = LocalContext.current

    val version = remember {
        try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    
    var showDoc by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        
        SelfAppIcon(modifier = Modifier.size(96.dp))

        Text(
            "LockPerm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "版本 ${version.ifEmpty { "1.0.0" }}  ·  io.github.sunilxsk.lockperm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        
        val cfg = rememberXpConfig(service, null)
        val logOn = cfg.bool(XpConfig.KEY_LOG_ENABLE, false)
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "显示日志",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "默认关闭：模块不会往 logcat 输出任何日志。" +
                                "排查问题时打开，即可看到配置解析、Hook 安装等详细信息。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = logOn,
                    onCheckedChange = { cfg.put(XpConfig.KEY_LOG_ENABLE, it) },
                )
            }
        }

        
        
        AppearanceCard()

        
        IconCard()

        
        ScaleCard()

        
        BackupCard(service)

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "介绍",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "一个基于 libxposed API 101 的 Xposed 模块。" +
                            "它把「伪装」和「防护」两件事放在一起：" +
                            "伪装负责让应用看到你想让它看到的东西（Android_ID、UA、注入脚本、权限状态），" +
                            "防护负责阻止应用滥用权限（无障碍、悬浮窗、壁纸、自杀式闪退）。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "所有功能都以作用域为单位生效：在「应用」页勾选目标应用，" +
                            "再到「伪装 / 防护」页配置即可，不需要为每个应用单独配一套。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "一些字",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "· 模块只在目标应用进程里工作，不会常驻任何后台服务。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "· 修改配置后需要强制停止目标应用再打开才会生效。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "· 崩溃日志写在 /storage/emulated/0/Android/data/{目标包名}/files/${XpConfig.CRASH_LOG_NAME}。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "· 防护类功能可能会影响目标应用的正常使用，请按需开启。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "· 🚫禁止对系统进程、金融、游戏、社交类 App 使用，否则后果自负🈲",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "注意事项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NOTICES.forEach { (title, body) ->
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "项目地址",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                
                LicenseRow(
                    name = "LockPerm",
                    license = SELF_LICENSE_NAME,
                    url = SELF_ABC_URL,
                    accent = true,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                
                Text(
                    "开源许可",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OPEN_SOURCE_LICENSES.forEach { item ->
                    LicenseRow(
                        name = item.name,
                        license = item.license,
                        url = item.url,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                
                OutlinedButton(
                    onClick = { showDoc = DOC_README },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("说明", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "许可证",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "本软件采用 GNU AGPL v3.0 许可证",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    SELF_LICENSE_URL,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { openUrl(context, SELF_LICENSE_URL) }
                        .padding(vertical = 4.dp),
                )
                Text(
                    "详见 $SELF_LICENSE_URL",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NativeHookCard(cfg)

        Spacer(Modifier.height(16.dp))
    }

    
    showDoc?.let { which ->
        AboutDocDialog(context = context, which = which, onDismiss = { showDoc = null })
    }
}




private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}




@Composable
private fun LicenseRow(
    name: String,
    license: String,
    url: String,
    accent: Boolean = false,
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val container = if (accent) cs.primaryContainer else cs.surfaceVariant
    val onContainer = if (accent) cs.onPrimaryContainer else cs.onSurface

    Surface(
        onClick = { openUrl(context, url) },
        shape = RoundedCornerShape(12.dp),
        color = container.copy(alpha = if (accent) 1f else 0.45f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = cs.primary.copy(alpha = 0.16f),
                    ) {
                        Text(
                            license,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = cs.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        url.removePrefix("https://"),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurfaceVariant,
            )
        }
    }
}








@Composable
private fun AboutDocDialog(
    context: Context,
    which: String,
    onDismiss: () -> Unit,
) {
    
    val title = "说明"
    val fileName = "README.md"
    val url = URL_README

    
    val content: String? = remember(which) {
        runCatching {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    
    if (content == null) {
        LaunchedEffect(which) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            onDismiss()
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onDismiss) { Text("确认") }
                }
            }
        }
    }
}









private data class OssLicense(
    val name: String,
    val license: String,
    val url: String,
)

private val OPEN_SOURCE_LICENSES: List<OssLicense> = listOf(
    OssLicense(
        "Dobby（inline hook 框架）",
        "Apache-2.0",
        "https://github.com/jmpews/Dobby",
    ),
    OssLicense(
        "libxposed api / service",
        "Apache-2.0",
        "https://github.com/libxposed/libxposed",
    ),
    OssLicense(
        "AndroidX Core / Activity / Lifecycle",
        "Apache-2.0",
        "https://github.com/androidx/androidx",
    ),
    OssLicense(
        "Jetpack Compose（UI / Foundation / Material3）",
        "Apache-2.0",
        "https://github.com/androidx/androidx",
    ),
    OssLicense(
        "Material Icons Extended",
        "Apache-2.0",
        "https://github.com/google/material-design-icons",
    ),
    OssLicense(
        "kotlinx.coroutines",
        "Apache-2.0",
        "https://github.com/Kotlin/kotlinx.coroutines",
    ),
    OssLicense(
        "AndroidX Media3",
        "Apache-2.0",
        "https://github.com/androidx/media",
    ),
    OssLicense(
        "Coil",
        "Apache-2.0",
        "https://github.com/coil-kt/coil",
    ),
)






private val PRESET_COLORS: List<Int> = listOf(
    0xFF24A0ED, 0xFF00BFA5, 0xFF7C4DFF, 0xFFE91E63,
    0xFFFF9800, 0xFF4CAF50, 0xFFFF5722, 0xFF0091EA,
).map { it.toInt() }

@Composable
private fun NativeHookCard(cfg: XpConfigState) {
    val on = cfg.bool(XpConfig.KEY_NATIVE_HOOK, XpConfig.DEF_NATIVE_HOOK)
    val soOk = remember { NativeBridge.loaded }
    val ver = remember { NativeBridge.version() }

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Native 层 Hook",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "启用 liblockperm.so",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "关掉后 native 侧完全不拦截，伪装只走 Java 层",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = on, onCheckedChange = { cfg.put(XpConfig.KEY_NATIVE_HOOK, it) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "反 hook 检测",
                subtitle = "让读取磁盘库文件的结果与内存一致，通过「内存 / 磁盘比对」类检测",
                checked = cfg.bool(XpConfig.KEY_NATIVE_ANTI_DETECT, XpConfig.DEF_NATIVE_ANTI_DETECT),
                onCheckedChange = { cfg.put(XpConfig.KEY_NATIVE_ANTI_DETECT, it) },
            )
            val hookN = remember { NativeBridge.hookCount() }
            Text(
                buildString {
                    append("so 加载：")
                    append(if (soOk) "成功" else "失败")
                    if (ver.isNotEmpty()) append("　$ver")
                    
                    if (hookN > 0) append("　已装 $hookN 个钩子")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (soOk) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error,
            )
            HintText(
                "反 hook 检测：没啥用😇"
            )
            HintText(
                "Native 层补的是 Java 钩不到的那部分：\n" +
                        "· __system_property_get —— NDK 读属性、toybox 的 getprop\n" +
                        "· uname —— 内核版本与架构\n" +
                        "· open / openat / fopen / access / stat —— 隐藏路径、伪造 /proc 文件内容\n" +
                        "配置与 Java 层完全同源：伪装页里没开的项，native 侧也不会拦。" +
                        "改完需要重启目标应用才生效。"
            )
        }
    }
}

@Composable
private fun AppearanceCard() {
    val context = LocalContext.current
    val mode = UiSettings.themeMode
    val cs = MaterialTheme.colorScheme
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "主题颜色",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SingleSelectChips(
                options = listOf("默认", "动态取色", "自定义"),
                selectedIndex = mode,
                onSelect = { UiSettings.setThemeMode(context, it) },
            )
            HintText(
                if (android.os.Build.VERSION.SDK_INT < 31) {
                    "当前系统在 Android 12 以下，不支持动态取色，选它会自动回退到下面的自定义色。"
                } else {
                    "动态取色会跟随系统壁纸取色（Android 12+）；自定义可以自己挑一个主题色，" +
                            "整套界面的主色、容器色、次级色都会跟着变。"
                }
            )
            if (mode != XpConfig.THEME_DEFAULT) {
                HorizontalDivider(color = cs.outlineVariant)
                Text(
                    "预设颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                PRESET_COLORS.chunked(4).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { c ->
                            ColorSwatch(
                                color = c,
                                selected = c == UiSettings.themeColor,
                                onClick = { UiSettings.setThemeColor(context, c) },
                            )
                        }
                    }
                }
                Text(
                    "自定义色相",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                HueSlider(context)
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val mod = Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(Color(color))
    Box(
        (if (selected) mod.border(3.dp, cs.onSurface, CircleShape) else mod)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun HueSlider(context: android.content.Context) {
    val seed = Color(UiSettings.themeColor)
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (seed.red * 255f).toInt().coerceIn(0, 255),
        (seed.green * 255f).toInt().coerceIn(0, 255),
        (seed.blue * 255f).toInt().coerceIn(0, 255),
        hsv,
    )
    var hue by remember { mutableStateOf(hsv[0]) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(UiSettings.themeColor))
        )
        Spacer(Modifier.width(12.dp))
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                UiSettings.setThemeColor(
                    context,
                    android.graphics.Color.HSVToColor(floatArrayOf(it, 0.62f, 0.92f)),
                )
            },
            valueRange = 0f..359f,
            modifier = Modifier.weight(1f),
        )
    }
}





@Composable
private fun IconCard() {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "桌面图标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SingleSelectChips(
                options = XpConfig.APP_ICON_LABELS,
                selectedIndex = UiSettings.appIcon,
                onSelect = { UiSettings.setAppIcon(context, it) },
            )
            HintText(
                "利用系统自带的 activity-alias 机制切换，不需要重新安装应用。" +
                        "切换后桌面一般几秒内会刷新，个别桌面需要重启或重新加载才能看到。"
            )
        }
    }
}





@Composable
private fun ScaleCard() {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(UiSettings.uiScale) }
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "配置页组件缩放",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(52.dp),
                )
                Slider(
                    value = scale,
                    onValueChange = {
                        scale = it
                        UiSettings.setUiScale(context, it)
                    },
                    valueRange = 0.6f..1.5f,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("小 75%", "默认 93%", "原始 100%", "大 120%").forEachIndexed { i, label ->
                    val v = when (i) { 0 -> 0.75f; 1 -> 0.93f; 2 -> 1.0f; else -> 1.2f }
                    OutlinedButton(
                        onClick = {
                            scale = v
                            UiSettings.setUiScale(context, v)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            HintText(
                "只影响「防护功能 / 伪装」这两个配置页里组件的显示大小，可以缩小也可以放大，" +
                        "默认 93%（跟原来的大小一致）。"
            )
        }
    }
}





@Composable
private fun BackupCard(service: XposedService?) {
    val context = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    val prefs = remember(service) {
        runCatching { service?.getRemotePreferences(XpConfig.PREFS) }.getOrNull()
    }

    fun toast(text: String) {
        msg = text
        runCatching {
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val p = prefs
        if (p == null) {
            toast("模块服务未连接，无法导出")
            return@rememberLauncherForActivityResult
        }
        val ok = runCatching {
            val json = ConfigBackup.export(p)
            val os = context.contentResolver.openOutputStream(uri)
                ?: return@runCatching false
            os.use {
                it.write(json.toByteArray(Charsets.UTF_8))
                it.flush()
            }
            true
        }.getOrDefault(false)
        toast(if (ok) "已导出备份" else "导出失败")
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val p = prefs
        if (p == null) {
            toast("模块服务未连接，无法恢复")
            return@rememberLauncherForActivityResult
        }
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8)
        }.getOrNull()
        if (text.isNullOrBlank()) {
            toast("读取备份文件失败")
            return@rememberLauncherForActivityResult
        }
        val n = ConfigBackup.import(p, text)
        toast(if (n >= 0) "已恢复 $n 项配置，重启目标应用生效" else "恢复失败：文件格式不对")
    }

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "备份与恢复",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "把整套配置（含每个应用单独的设置）导出成一个 JSON 文件，" +
                        "换机或重装后直接恢复，不用一条条重配。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        runCatching { exportLauncher.launch("LockPerm_备份.json") }
                            .onFailure { toast("无法打开系统文件选择器") }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = prefs != null,
                ) { Text("导出备份") }
                OutlinedButton(
                    onClick = {
                        runCatching { importLauncher.launch(arrayOf("application/json", "*/*")) }
                            .onFailure { toast("无法打开系统文件选择器") }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = prefs != null,
                ) { Text("导入恢复") }
            }
            if (prefs == null) {
                HintText("模块服务未连接：请先在「应用」页确认作用域已勾选，再回到这里操作。")
            }
            if (msg != null) {
                HintText("上次操作：$msg")
            }
            HintText("恢复会覆盖当前全部配置，且需要强制停止目标应用后才会生效。")
        }
    }
}

private const val SELF_LICENSE_NAME = "GNU AGPL v3.0"
private const val SELF_LICENSE_URL = "https://www.gnu.org/licenses/agpl-3.0.html"
private const val SELF_ABC_URL = "https://github.com/sunilxsk/LockPerm"

private val NOTICES: List<Pair<String, String>> = listOf(
    "阻止闪退 与 定时退出 不要一起开" to
            "「阻止闪退 / 自杀」从「中」档起就会吞掉未捕获异常，" +
            "而「定时退出」里的空指针闪退正是靠异常退出的，两者同时开会互相打架：" +
            "异常被吞掉、退不出去，最后只能靠硬杀兜底。要定时退出就先关掉阻止闪退。",
    "掌控悬浮窗只在目标软件自己的界面上生效" to
            "它不是安卓「显示在其他应用上层」权限创建的悬浮窗，不需要任何权限；" +
            "代价是切到后台、锁屏或换一个应用就看不见了。",
    "权限伪装的第 2 个开关只影响「读取」" to
            "它决定应用真的用标准方法去读时拿到什么：勾上就返回虚构内容" +
            "（通讯录假人、存储假文件、短信假记录、电话假号码、位置固定坐标），" +
            "不勾则可能拿到空值。摄像头要真实画面、麦克风要真实音频做不到，所以这两项没有第 2 个开关。",
    "设备管理员「全部合一」会持续解除身份" to
            "开启后每 4 秒尝试一次 removeActiveAdmin / clearDeviceOwnerApp，" +
            "如果目标本身就是正经的设备管理器应用，可能出现反复去激活、日志刷屏。",
    "无障碍破坏依赖安卓标准接口" to
            "Hook 的是 AccessibilityService / AccessibilityNodeInfo / AccessibilityEvent 的公开接口，" +
            "深度定制 ROM 或被目标应用自己做了混淆绕过的场景，可能部分子功能失效。",
    "防护类功能可能影响目标应用正常使用" to
            "禁用无障碍能力、拦截悬浮窗、阻断壁纸接口都可能让目标应用的部分功能不可用，请按需开启。",
    "阻止应用执行命令 与 退出方式里的「执行命令」互斥" to
            "两者只能开一个：开启前者后，退出方式会自动换成「杀死进程」；" +
            "反过来勾选「执行命令」，前者也会被自动关掉（退出时模块会临时放行自己的命令）。",
    "掌控面板里的一键功能是点一下才生效" to
            "面板上的「立即阻止修改壁纸 / 立即阻止创建悬浮窗 / 无障碍全部合一 / 设备管理员全部合一」" +
            "都是点击那一刻才把 Hook 装上去，进入应用时不会预先开启，也不需要重启目标应用。",
    "无障碍的按键 / 屏幕控制是重点防护对象" to
            "无障碍服务可以声明 FLAG_REQUEST_FILTER_KEY_EVENTS 优先拿到按键，" +
            "也能靠 FLAG_REQUEST_TOUCH_EXPLORATION_MODE 让系统进入触摸探测模式，" +
            "于是出现「按音量键没反应、系统音量条不弹、屏幕点不动」。" +
            "模块除了拦 onKeyEvent / onGesture / onMotionEvent，还会把服务声明里的这些 flag 抹掉。",
    "权限伪装支持按应用单独配置" to
            "伪装页只留总开关；打开后到「应用」页，已加入作用域的应用会多出一个「权限配置」按钮，" +
            "里面可以单独勾选它要伪装哪些权限。没加入作用域的应用不会有这个按钮。",
    "阻止控制音量 / 闪光灯 / 振动 只针对应用" to
            "Hook 的是目标应用进程里的调用，你自己用系统音量条、系统手电筒、来电振动都不受影响。",
    "禁止创建文件 会按类目分别拦截" to
            "MediaStore 那条链路（insert / openOutputStream / update / query / delete）" +
            "只作用于媒体库 Uri，应用自己的 ContentProvider 不受影响；" +
            "java.io.File 与 java.nio.file.Files 只作用于 /sdcard、/storage、/mnt 下的公共存储。" +
            "只想让它读不想让它写的话，把「写入」以外的关掉即可。",
    "隐藏应用列表 改的是枚举结果" to
            "只影响目标应用调用 getInstalledApplications / getInstalledPackages 拿到的列表，" +
            "不是真的卸载或冻结应用；白名单模式下名单里没有的都会被隐藏，请注意别把自己需要的也漏掉。",
    "改完配置要强制停止目标应用再打开" to
            "模块代码与配置的生效都需要重启目标应用进程。",
    "崩溃日志位置" to
            "/storage/emulated/0/Android/data/{目标包名}/files/${XpConfig.CRASH_LOG_NAME}，" +
            "需要「崩溃时写入应用私有目录」处于开启状态。",
)


@Composable
fun SelfAppIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                val d = context.packageManager.getApplicationIcon(context.packageName)
                val bmp = if (d is BitmapDrawable && d.bitmap != null) {
                    d.bitmap
                } else {
                    val w = d.intrinsicWidth.takeIf { it > 0 } ?: 96
                    val h = d.intrinsicHeight.takeIf { it > 0 } ?: 96
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b ->
                        val c = android.graphics.Canvas(b)
                        d.setBounds(0, 0, c.width, c.height)
                        d.draw(c)
                    }
                }
                bmp.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
        icon = loaded
    }

    if (icon != null) {
        Image(
            bitmap = icon!!,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(24.dp)),
        )
    } else {
        Spacer(modifier)
    }
}