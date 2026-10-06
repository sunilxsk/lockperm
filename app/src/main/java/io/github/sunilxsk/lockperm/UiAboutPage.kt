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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    val versionCode = remember {
        try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            info.versionCode
        } catch (_: Throwable) {
            0
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
                            "伪装负责改变应用读到的内容（Android_ID、UA、注入脚本、权限状态），" +
                            "防护负责阻止应用滥用权限（无障碍、悬浮窗、壁纸、闪退）。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "在「应用」页勾选目标应用，再到「伪装 / 防护」页配置即可。",
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
                    "· 已激活，但显示未激活、作用域不同步或未更新，重启本模块尝试解决",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "· 🚫请勿对系统进程及涉及金融、账号安全、财产、隐私等关键场景的应用使用本模块，" +
                            "由此产生的一切后果由使用者自行承担，作者概不负责🈲",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
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

        AppSelfCard()

        
        
        UpdateCard(versionCode = versionCode, versionName = version)

        LspatchActivateCard()

        Spacer(Modifier.height(16.dp))
    }

    
    showDoc?.let { which ->
        AboutDocDialog(context = context, which = which, onDismiss = { showDoc = null })
    }
}








@Composable
private fun AppSelfCard() {
    val context = LocalContext.current
    val api = android.os.Build.VERSION.SDK_INT
    var confirmHide by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "模块自身",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            var hide by remember { mutableStateOf(UiSettings.hideLauncher) }
            SwitchRow(
                title = "隐藏桌面图标",
                subtitle = "禁用所有启动图标，桌面上不再显示本模块",
                checked = hide,
                onCheckedChange = { on ->
                    if (on) {
                        confirmHide = true
                    } else {
                        hide = false
                        UiSettings.setHideLauncher(context, false)
                    }
                },
            )
            HintText(
                "隐藏后桌面图标消失，但 LSPosed 管理器里仍能打开本模块"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            var back by remember { mutableStateOf(UiSettings.predictiveBack) }
            val backSupported = api >= 33
            SwitchRow(
                title = "预测性返回手势",
                subtitle = if (backSupported) {
                    "Android 13+：返回时显示返回目标预览动画"
                } else {
                    "需要 Android 13 及以上，当前系统 $api 不支持"
                },
                checked = back && backSupported,
                enabled = backSupported,
                onCheckedChange = { on ->
                    back = on
                    UiSettings.setPredictiveBack(context, on)
                },
            )
            if (backSupported) {
                HintText("改完重启本模块应用生效。")
            }
        }
    }

    if (confirmHide) {
        AlertDialog(
            onDismissRequest = { confirmHide = false },
            title = { Text("隐藏桌面图标？") },
            text = {
                Text(
                    "隐藏后桌面上就找不到本模块了。要再打开，只能去 LSPosed 管理器里" +
                        "点进本模块再关掉这一项。\n\n确定要隐藏吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    UiSettings.setHideLauncher(context, true)
                    confirmHide = false
                }) { Text("隐藏") }
            },
            dismissButton = {
                TextButton(onClick = { confirmHide = false }) { Text("取消") }
            },
        )
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
        "Dobby",
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
                "Native 层 Hook（全局默认）",
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
                title = "测试检测",
                checked = cfg.bool(XpConfig.KEY_NATIVE_ANTI_DETECT, XpConfig.DEF_NATIVE_ANTI_DETECT),
                onCheckedChange = { cfg.put(XpConfig.KEY_NATIVE_ANTI_DETECT, it) },
            )
            val hookN = remember { NativeBridge.hookCount() }
            Text(
                buildString {
                    append("so 加载：")
                    append(if (soOk) "成功" else "失败")
                    if (ver.isNotEmpty()) append("$ver")
                    
                    if (hookN > 0) append("　已装 $hookN 个钩子")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (soOk) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error,
            )
        }
    }
}










@Composable
private fun LspatchActivateCard() {
    val context = LocalContext.current
    val on = UiSettings.lspatchActivate

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "使用 LSPatch 激活",
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
                        "用 LSPatch 激活使用",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "(免Root模式)开启后应用页可直接配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = on,
                    onCheckedChange = { UiSettings.setLspatchActivate(context, it) },
                )
            }
            Text(
                " ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (on) {
                Text(
                    "当前：LSPatch 模式。作用域在 LSPatch 里添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}







@Composable
private fun UpdateCard(versionCode: Int, versionName: String) {
    val context = LocalContext.current
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val autoUpdate = UiSettings.autoUpdate

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "检查更新",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "当前 v${versionName.ifEmpty { "?" }}（$versionCode）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    if (checking) return@Button
                    checking = true
                    error = null
                    result = null
                    scope.launch {
                        val r = runCatching {
                            withContext(Dispatchers.IO) {
                                UpdateChecker.fetch(versionCode, versionName)
                            }
                        }.getOrNull()
                        
                        checking = false
                        if (r == null) {
                            error = "检查失败：网络不可用或接口未响应"
                        } else {
                            result = r
                            showDialog = true
                        }
                    }
                },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (checking) "正在检查…" else "检查更新")
            }
            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "自动检查更新",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "默认关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = autoUpdate,
                    onCheckedChange = { UiSettings.setAutoUpdate(context, it) },
                )
            }
            HintText("关闭时只有手动检查更新才会联网。")
        }
    }

    if (showDialog) {
        result?.let { r -> UpdateResultDialog(r, onDismiss = { showDialog = false }) }
    }
}











@Composable
internal fun AutoUpdateDialog(
    result: UpdateChecker.Result,
    onDismiss: () -> Unit,
    onNeverAsk: () -> Unit,
    onUpdateNow: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "发现新版本",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoChip(
                        "当前", "v${result.currentName}（${result.currentCode}）",
                        modifier = Modifier.weight(1f),
                    )
                    InfoChip(
                        "最新", "v${result.versionName}（${result.versionCode}）",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (result.notes.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "更新说明",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            result.notes,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                TextButton(
                    onClick = onNeverAsk,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("以后都不再提示（关闭自动检查）") }
                Button(
                    onClick = onUpdateNow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("立刻更新") }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("取消") }
            }
        }
    }
}


internal fun updateUrlOf(result: UpdateChecker.Result): String {
    val apk = result.assets.firstOrNull {
        it.name.endsWith(".apk", ignoreCase = true)
    }
    return apk?.url ?: result.htmlUrl
}

@Composable
private fun UpdateResultDialog(result: UpdateChecker.Result, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (result.hasUpdate) "发现新版本" else "已是最新版本",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoChip(
                        "当前", "v${result.currentName}（${result.currentCode}）",
                        modifier = Modifier.weight(1f),
                    )
                    InfoChip(
                        "最新", "v${result.versionName}（${result.versionCode}）",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (result.publishedAt.isNotBlank()) {
                    Text(
                        "发布于 ${UpdateChecker.dateText(result.publishedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (result.notes.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("更新说明", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            result.notes,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                        )
                    }
                }

                if (result.assets.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("下载", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    result.assets.forEach { a ->
                        Surface(
                            onClick = { openUrl(context, a.url) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        a.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val sz = UpdateChecker.sizeText(a.size)
                                    if (sz.isNotBlank()) {
                                        Text(
                                            sz,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    "›",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { openUrl(context, result.htmlUrl) }) {
                        Text("发布页")
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    
    
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppearanceCard() {
    val context = LocalContext.current
    val mode = UiSettings.themeMode
    val cs = MaterialTheme.colorScheme
    var customOpen by remember { mutableStateOf(false) }
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
                    "自定义颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                HuePreviewRow(onClick = { customOpen = true })
            }
        }
    }

    if (customOpen) {
        HsvColorDialog(
            initial = UiSettings.themeColor,
            onDismiss = { customOpen = false },
            onConfirm = { color ->
                UiSettings.setThemeColor(context, color)
                customOpen = false
            },
        )
    }
}


@Composable
private fun HuePreviewRow(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(UiSettings.themeColor))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "#" + (UiSettings.themeColor.toLong() and 0xFFFFFF)
                .toString(16).uppercase().padStart(6, '0'),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
            Text("自定义", style = MaterialTheme.typography.labelMedium)
        }
    }
}








@Composable
private fun HsvColorDialog(
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val hsv = remember(initial) {
        val f = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (initial shr 16) and 0xFF, (initial shr 8) and 0xFF, initial and 0xFF, f
        )
        f
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    val current = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val hex = "#" + (current.toLong() and 0xFFFFFF)
        .toString(16).uppercase().padStart(6, '0')

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "自定义颜色",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(current))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            hex,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "H ${hue.toInt()}°　S ${(sat * 100).toInt()}%　V ${(value * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                HsvSlider(
                    label = "H 色相",
                    value = hue,
                    range = 0f..360f,
                    onValueChange = { hue = it },
                )
                HsvSlider(
                    label = "S 饱和度",
                    value = sat,
                    range = 0f..1f,
                    onValueChange = { sat = it },
                )
                HsvSlider(
                    label = "V 明度",
                    value = value,
                    range = 0f..1f,
                    onValueChange = { value = it },
                ) 

                
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("鲜艳" to floatArrayOf(hue, 0.85f, 0.95f),
                        "柔和" to floatArrayOf(hue, 0.45f, 0.92f),
                        "深色" to floatArrayOf(hue, 0.70f, 0.62f))
                        .forEach { (label, f) ->
                            OutlinedButton(
                                onClick = { sat = f[1]; value = f[2] },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onConfirm(current) }) { Text("应用") }
                }
            }
        }
    }
}

@Composable
private fun HsvSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(64.dp),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.weight(1f),
            )
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