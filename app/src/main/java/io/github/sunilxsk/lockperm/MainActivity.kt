package io.github.sunilxsk.lockperm

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale






class MainActivity : ComponentActivity() {
    









    private fun setupPredictiveBack() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        if (!UiSettings.predictiveBack) return
        runCatching {
            val dispatcher = onBackInvokedDispatcher
            val cb = object : android.window.OnBackInvokedCallback {
                override fun onBackInvoked() {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
            dispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                cb,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiSettings.load(this)
        setupPredictiveBack()
        enableEdgeToEdge()
        setContent {
            XpTheme {
                MainScreen()
            }
        }
    }
}



@Composable
fun XpTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val mode = UiSettings.themeMode
    val seed = Color(UiSettings.themeColor)
    val scheme = remember(dark, mode, UiSettings.themeColor) {
        buildColorScheme(context, dark, mode, seed)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@android.annotation.SuppressLint("NewApi")
private fun buildColorScheme(
    context: android.content.Context,
    dark: Boolean,
    mode: Int,
    seed: Color,
): ColorScheme {
    val dynamic = android.os.Build.VERSION.SDK_INT >= 31
    return when {
        mode == XpConfig.THEME_DYNAMIC && dynamic -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        mode == XpConfig.THEME_CUSTOM -> schemeFromSeed(seed, dark)
        mode == XpConfig.THEME_DYNAMIC -> schemeFromSeed(seed, dark)
        else -> if (dark) defaultDarkScheme() else defaultLightScheme()
    }
}

private fun defaultDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFF9CCAFF),
    secondary = Color(0xFFBBC7DB),
    tertiary = Color(0xFFD7BDE9),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF1C1F24),
)

private fun defaultLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF415F91),
    secondary = Color(0xFF565F71),
    tertiary = Color(0xFF705575),
    background = Color(0xFFF9F9FF),
    surface = Color(0xFFF9F9FF),
    surfaceVariant = Color(0xFFE1E2EC),
)













private fun schemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val r = (seed.red * 255f).toInt().coerceIn(0, 255)
    val g = (seed.green * 255f).toInt().coerceIn(0, 255)
    val b = (seed.blue * 255f).toInt().coerceIn(0, 255)

    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    val h = hsv[0]
    val v = hsv[2]
    
    
    val gray = hsv[1] < 0.08f
    val s = if (gray) 0f else hsv[1].coerceIn(0.25f, 0.95f)

    fun tone(value: Float, sat: Float = s, hue: Float = h): Color =
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue % 360f, sat, value)))

    
    val picked = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    val bg = if (dark) Color(0xFF111318) else Color(0xFFF9F9FF)
    val primary = keepVisible(picked, bg)
    val onBg = if (dark) Color(0xFFE4E2E9) else Color(0xFF1B1B21)
    val surfaceVar = if (dark) tone(0.18f, s * 0.45f) else tone(0.94f, s * 0.22f)

    val secondary = tone(if (dark) 0.72f else 0.48f, s * 0.55f, h + 28f)
    val tertiary = tone(if (dark) 0.74f else 0.46f, s * 0.60f, h + 62f)

    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = tone(0.32f),
            onPrimaryContainer = tone(0.92f, s * 0.35f),
            secondary = secondary,
            onSecondary = readableOn(secondary),
            secondaryContainer = tone(0.28f, s * 0.45f, h + 28f),
            onSecondaryContainer = tone(0.90f, s * 0.30f, h + 28f),
            tertiary = tertiary,
            onTertiary = readableOn(tertiary),
            tertiaryContainer = tone(0.30f, s * 0.45f, h + 62f),
            onTertiaryContainer = tone(0.92f, s * 0.30f, h + 62f),
            background = bg,
            surface = bg,
            surfaceVariant = surfaceVar,
            onBackground = onBg,
            onSurface = onBg,
            onSurfaceVariant = tone(0.72f, 0.12f),
            outline = tone(0.42f, 0.18f),
            outlineVariant = tone(0.30f, 0.16f),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = tone(0.90f, s * 0.55f),
            onPrimaryContainer = tone(0.24f),
            secondary = secondary,
            onSecondary = readableOn(secondary),
            secondaryContainer = tone(0.92f, s * 0.30f, h + 28f),
            onSecondaryContainer = tone(0.22f, s * 0.4f, h + 28f),
            tertiary = tertiary,
            onTertiary = readableOn(tertiary),
            tertiaryContainer = tone(0.92f, s * 0.32f, h + 62f),
            onTertiaryContainer = tone(0.22f, s * 0.4f, h + 62f),
            background = bg,
            surface = bg,
            surfaceVariant = surfaceVar,
            onBackground = onBg,
            onSurface = onBg,
            onSurfaceVariant = tone(0.36f, 0.14f),
            outline = tone(0.52f, 0.16f),
            outlineVariant = tone(0.74f, 0.12f),
        )
    }
}


private fun lumOf(c: Color): Float =
    0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue


private fun readableOn(c: Color): Color =
    if (lumOf(c) > 0.55f) Color.Black else Color.White






private fun keepVisible(c: Color, bg: Color): Color {
    val lc = lumOf(c)
    val lb = lumOf(bg)
    if (kotlin.math.abs(lc - lb) >= 0.22f) return c
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (c.red * 255f).toInt().coerceIn(0, 255),
        (c.green * 255f).toInt().coerceIn(0, 255),
        (c.blue * 255f).toInt().coerceIn(0, 255),
        hsv,
    )
    
    val want = if (lb > 0.5f) lb - 0.28f else lb + 0.28f
    val nv = if (lc <= 0.02f) want.coerceIn(0f, 1f)
    else (hsv[2] * (want / lc)).coerceIn(0.05f, 1f)
    return Color(
        android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], nv))
    )
}

@Composable
fun MainScreen() {
    val context = LocalContext.current

    
    var showSplash by remember { mutableStateOf(true) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var serviceState by remember { mutableStateOf<XposedService?>(null) }
    
    var configPkg by rememberSaveable { mutableStateOf<String?>(null) }
    var configLabel by rememberSaveable { mutableStateOf("") }

    
    
    val appList = remember { AppListState() }
    LaunchedEffect(Unit) { appList.load(context) }

    
    
    
    var autoResult by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    LaunchedEffect(Unit) {
        if (!UiSettings.autoUpdate) return@LaunchedEffect
        val (code, name) = runCatching {
            @Suppress("DEPRECATION")
            val i = context.packageManager.getPackageInfo(context.packageName, 0)
            i.versionCode to (i.versionName ?: "")
        }.getOrDefault(0 to "")
        val r = runCatching {
            withContext(Dispatchers.IO) { UpdateChecker.fetch(code, name) }
        }.getOrNull() ?: return@LaunchedEffect
        if (r.hasUpdate) autoResult = r
    }

    
    androidx.activity.compose.BackHandler(enabled = configPkg != null) {
        if (configPkg != null) {
            configPkg = null
            selectedTab = 1
        }
    }

    
    
    
    
    
    
    
    
    var serviceUsable by remember { mutableStateOf<Boolean?>(null) }
    var verifyTick by remember { mutableIntStateOf(0) }

    
    
    val listener = remember {
        object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                serviceState = service
                verifyTick++
            }

            override fun onServiceDied(service: XposedService) {
                serviceState = null
                serviceUsable = false
            }
        }
    }

    LaunchedEffect(verifyTick, serviceState) {
        val svc = serviceState
        if (svc == null) {
            serviceUsable = false
            return@LaunchedEffect
        }
        serviceUsable = null
        serviceUsable = probeService(svc)
    }

    DisposableEffect(Unit) {
        XposedServiceHelper.registerListener(listener)
        onDispose { }
    }

    
    
    val currentService by androidx.compose.runtime.rememberUpdatedState(serviceState)
    val activity = context as? androidx.activity.ComponentActivity
    DisposableEffect(activity) {
        var observer: androidx.lifecycle.LifecycleEventObserver? = null
        if (activity != null) {
            observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event != androidx.lifecycle.Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
                
                
                runCatching { XposedServiceHelper.registerListener(listener) }
                if (currentService != null) verifyTick++
            }
            activity.lifecycle.addObserver(observer)
        }
        onDispose {
            observer?.let { activity?.lifecycle?.removeObserver(it) }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            
            
            if (configPkg == null) NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("主页") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                    label = { Text("应用") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("关于") }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            val tab = selectedTab.coerceIn(0, 2)
            if (configPkg != null) {
                AppConfigPage(
                    service = serviceState,
                    pkg = configPkg!!,
                    label = configLabel,
                    onBack = { configPkg = null },
                )
            } else {
                
                
                
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (fadeIn(animationSpec = tween(160)) +
                            slideInHorizontally(
                                animationSpec = tween(200)
                            ) { (it / 8) * dir }) togetherWith
                            (fadeOut(animationSpec = tween(160)) +
                                slideOutHorizontally(
                                    animationSpec = tween(200)
                                ) { (-it / 8) * dir })
                    },
                    contentAlignment = Alignment.TopStart,
                    label = "main_tab",
                ) { t ->
                    when (t) {
                        0 -> HomePage(serviceState, serviceUsable)
                        1 -> AppsPage(serviceState, appList) { pkg, label ->
                            configLabel = label
                            configPkg = pkg
                        }
                        else -> AboutPage(serviceState)
                    }
                }
            }
        }
    }

    autoResult?.let { r ->
        AutoUpdateDialog(
            result = r,
            onDismiss = { autoResult = null },
            onNeverAsk = {
                UiSettings.setAutoUpdate(context, false)
                autoResult = null
            },
            onUpdateNow = {
                autoResult = null
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(updateUrlOf(r)))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
        )
    }

    
    AnimatedVisibility(
        visible = showSplash,
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        SplashOverlay { showSplash = false }
    }
    }
}








private const val SPLASH_TOTAL_MS = 2000L









@Composable
private fun SplashOverlay(onFinish: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val primary = cs.primary
    val secondary = cs.tertiary

    
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
        delay(SPLASH_TOTAL_MS)
        onFinish()
    }

    
    val badgeScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "badgeScale",
    )
    val badgeAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(420),
        label = "badgeAlpha",
    )
    
    val textAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(520, delayMillis = 260),
        label = "textAlpha",
    )
    val textOffset by animateDpAsState(
        targetValue = if (entered) 0.dp else 18.dp,
        animationSpec = tween(520, delayMillis = 260, easing = FastOutSlowInEasing),
        label = "textOffset",
    )
    
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(SPLASH_TOTAL_MS.toInt(), easing = LinearEasing),
        label = "progress",
    )
    
    val infinite = rememberInfiniteTransition(label = "spin")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
        ),
        label = "angle",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        cs.background,
                        cs.surfaceVariant,
                        cs.background,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier
                        .size(148.dp)
                        .scale(badgeScale)
                        .alpha(badgeAlpha)
                ) {
                    val r = size.minDimension / 2f
                    val c = center

                    
                    rotate(angle) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(primary, secondary, primary),
                                center = c,
                            ),
                            startAngle = 0f,
                            sweepAngle = 110f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(secondary, primary, secondary),
                                center = c,
                            ),
                            startAngle = 180f,
                            sweepAngle = 110f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    
                    drawCircle(
                        color = primary.copy(alpha = 0.18f),
                        radius = r * 0.72f,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )

                    
                    val w = r * 0.46f
                    val top = c.y - r * 0.42f
                    val bottom = c.y + r * 0.46f
                    val mid = c.y + r * 0.06f
                    val shield = Path().apply {
                        moveTo(c.x - w, top)
                        lineTo(c.x + w, top)
                        lineTo(c.x + w, mid)
                        lineTo(c.x, bottom)
                        lineTo(c.x - w, mid)
                        close()
                    }
                    drawPath(
                        path = shield,
                        brush = Brush.linearGradient(
                            listOf(primary, secondary),
                            start = Offset(c.x, top),
                            end = Offset(c.x, bottom),
                        ),
                    )

                    
                    val check = Path().apply {
                        moveTo(c.x - w * 0.42f, c.y + r * 0.02f)
                        lineTo(c.x - w * 0.08f, c.y + r * 0.22f)
                        lineTo(c.x + w * 0.46f, c.y - r * 0.20f)
                    }
                    drawPath(
                        path = check,
                        color = Color.White.copy(alpha = 0.95f),
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            
            Text(
                "LockPerm",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground,
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffset),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "按应用为单位的伪装与防护",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffset),
            )
        }

        
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 56.dp, vertical = 52.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(cs.onSurface.copy(alpha = 0.10f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(primary, secondary))),
            )
        }
    }
}













private suspend fun probeService(service: XposedService?): Boolean =
    withContext(Dispatchers.IO) {
        if (service == null) return@withContext false
        try {
            val p = service.getRemotePreferences(XpConfig.PREFS)
            p.all
            true
        } catch (e: Throwable) {
            val msg = (e.message ?: "") + e.javaClass.name
            val dead = e is android.os.DeadObjectException ||
                e is SecurityException ||
                msg.contains("DeadObject", true) ||
                msg.contains("dead", true) ||
                msg.contains("Transaction failed", true) ||
                msg.contains("RemoteException", true)
            !dead
        }
    }

@Composable
fun HomePage(service: XposedService?, serviceUsable: Boolean? = null) {
    val context = LocalContext.current
    
    
    
    
    val lspatch = UiSettings.lspatchActivate
    val activated = lspatch || serviceUsable == true
    
    
    val checking = !lspatch && serviceUsable == null

    val deviceInfo = remember {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
        listOf(
            "设备型号" to Build.MODEL,
            "品牌" to Build.BRAND,
            "制造商" to Build.MANUFACTURER,
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "指纹" to Build.FINGERPRINT,
            "Android ID" to androidId,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    activated -> MaterialTheme.colorScheme.primaryContainer
                    checking -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activated) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = when {
                            activated -> MaterialTheme.colorScheme.onPrimaryContainer
                            checking -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            activated -> "模块已激活"
                            checking -> "正在检测激活状态…"
                            else -> "模块未激活"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            lspatch -> "使用 LSPatch 激活，作用域请在 LSPatch 里勾选"
                            checking -> "正在确认框架服务是否可用"
                            activated -> "LSPosed 服务已连接，可在「应用」页管理作用域"
                            else -> "请先在 LSPosed 管理器中启用模块"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(
            "设备信息",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                deviceInfo.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            k,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            v,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        InfoCard {
            Text("使用步骤", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("1. 在 LSPosed 管理器中启用本模块。", style = MaterialTheme.typography.bodySmall)
            Text("2. 在「应用」页为目标应用申请作用域。", style = MaterialTheme.typography.bodySmall)
            Text("3. 在「应用」页给已加入作用域的应用点「配置」，进入它的专属配置页。", style = MaterialTheme.typography.bodySmall)
            Text("4. 在「防护功能」「权限伪装」「设备伪装」「设备模板」「其他功能」五个页签里勾选这个应用需要的功能，页签可左右滑动。", style = MaterialTheme.typography.bodySmall)
            Text("5. 每个应用的配置互相独立，互不干扰。", style = MaterialTheme.typography.bodySmall)
            Text("6. 修改配置后强制停止目标应用再打开即可生效。", style = MaterialTheme.typography.bodySmall)
        }
    }
}




private data class AppInfo(
    val info: ApplicationInfo,
    val label: String,
)


private val appIconCache = ConcurrentHashMap<String, ImageBitmap?>()






private class AppListState {
    var apps: List<AppInfo>? by mutableStateOf(null)
    var loading: Boolean by mutableStateOf(false)
    var reloadToken: Int by mutableStateOf(0)

    suspend fun load(context: Context, force: Boolean = false) {
        if (loading) return
        if (apps != null && !force) return
        loading = true
        val pm = context.packageManager
        val self = context.packageName
        val loaded = withContext(Dispatchers.IO) {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != self }
                .map { info ->
                    val label = try {
                        info.loadLabel(pm).toString()
                    } catch (_: Throwable) {
                        info.packageName
                    }
                    AppInfo(info, label)
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
        apps = loaded
        loading = false
    }
}

@Composable
private fun AppsPage(
    service: XposedService?,
    appList: AppListState,
    onOpenConfig: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val allApps = appList.apps
    val reloading = appList.loading
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var scopeSet by remember { mutableStateOf<Set<String>>(emptySet()) }

    
    val lspatch = UiSettings.lspatchActivate
    var scopeHint by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    var scopeLoaded by remember { mutableStateOf(false) }
    
    var showSystem by rememberSaveable { mutableStateOf(false) }

    
    LaunchedEffect(appList.reloadToken) {
        if (appList.reloadToken > 0) appList.load(context, force = true)
    }

    
    LaunchedEffect(Unit) { appList.load(context) }

    
    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    

    LaunchedEffect(service, appList.reloadToken) {
        scopeSet = try {
            service?.scope?.toSet() ?: emptySet()
        } catch (_: Throwable) {
            emptySet()
        }
        
        if (!scopeLoaded && scopeSet.isNotEmpty()) {
            scopeLoaded = true
            listState.scrollToItem(0)
        }
    }

    val filteredList: List<AppInfo> by remember(allApps, searchQuery, scopeSet, showSystem) {
        derivedStateOf {
            val base = (allApps ?: return@derivedStateOf emptyList<AppInfo>())
                .let { list ->
                    if (showSystem) list else list.filter { !isSystemApp(it.info) }
                }
            val q = searchQuery.trim().lowercase()
            val matched = if (q.isEmpty()) {
                base
            } else {
                base.filter {
                    it.label.lowercase().contains(q) || it.info.packageName.lowercase().contains(q)
                }
            }
            val (inScope, outScope) = matched.partition { it.info.packageName in scopeSet }
            inScope + outScope
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            allApps == null -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            else -> {
                Column(Modifier.fillMaxSize()) {
                    
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "显示系统应用",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.Switch(
                            checked = showSystem,
                            onCheckedChange = { showSystem = it },
                        )
                        SpacerCompat()
                        androidx.compose.material3.IconButton(
                            onClick = {
                                if (!reloading) {
                                    appList.apps = null
                                    appList.reloadToken++
                                }
                            },
                            enabled = !reloading,
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "重新加载")
                        }
                    }

                    androidx.compose.material3.OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("搜索应用名或包名") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "清空"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )

                    if (filteredList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (reloading) "正在加载…"
                                else if (searchQuery.isBlank()) "未获取到应用列表"
                                else "没有匹配的应用",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                        ) {
                            items(
                                items = filteredList,
                                key = { it.info.packageName },
                                contentType = { "app" },
                            ) { app ->
                                AppRow(
                                    app = app,
                                    isInScope = app.info.packageName in scopeSet,
                                    enabled = service != null || lspatch,
                                    lspatch = lspatch,
                                    onAdd = {
                                        val pkg = app.info.packageName
                                        service?.requestScope(
                                            listOf(pkg),
                                            object : XposedService.OnScopeEventListener {
                                                override fun onScopeRequestApproved(approvedPackages: List<String>) {
                                                    scopeSet = scopeSet + approvedPackages
                                                    coroutineScope.launch {
                                                        snackbarHost.showSnackbar(
                                                            "已添加作用域: ${approvedPackages.joinToString()}"
                                                        )
                                                    }
                                                }

                                                override fun onScopeRequestFailed(reason: String) {
                                                    coroutineScope.launch {
                                                        snackbarHost.showSnackbar("添加失败: $reason")
                                                    }
                                                }
                                            }
                                        )
                                    },
                                    onRemove = {
                                        val pkg = app.info.packageName
                                        try {
                                            service?.removeScope(listOf(pkg))
                                            scopeSet = scopeSet - pkg
                                            coroutineScope.launch {
                                                snackbarHost.showSnackbar("已移出作用域: $pkg")
                                            }
                                        } catch (t: Throwable) {
                                            coroutineScope.launch {
                                                snackbarHost.showSnackbar("移出失败: $t")
                                            }
                                        }
                                    },
                                    onConfig = {
                                        val pkg = app.info.packageName
                                        
                                        
                                        if (lspatch && pkg !in UiSettings.lspatchScopeNoticed) {
                                            scopeHint = pkg to app.label
                                        } else {
                                            onOpenConfig(pkg, app.label)
                                        }
                                    }
                                )
                                HorizontalDivider(
                                    Modifier.padding(start = 72.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter))
    }

    
    scopeHint?.let { (pkg, label) ->
        AlertDialog(
            onDismissRequest = { scopeHint = null },
            title = { Text("作用域需要在 LSPatch 里添加") },
            text = {
                Text(
                    "$label\n$pkg\n\n${XpConfig.LSPATCH_SCOPE_HINT}\n\n\n"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    UiSettings.markLspatchScopeNoticed(context, pkg)
                    scopeHint = null
                    onOpenConfig(pkg, label)
                }) { Text("知道了，继续配置") }
            },
            dismissButton = {
                TextButton(onClick = {
                    UiSettings.markLspatchScopeNoticed(context, pkg)
                    scopeHint = null
                }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    isInScope: Boolean,
    enabled: Boolean,
    lspatch: Boolean = false,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onConfig: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIcon(
            pkgName = app.info.packageName,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                app.info.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        
        if (lspatch) {
            androidx.compose.material3.OutlinedButton(onClick = onConfig, enabled = enabled) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                Text("配置")
            }
        } else if (isInScope) {
            androidx.compose.material3.FilledTonalButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                Text("移出")
            }
            
            androidx.compose.material3.OutlinedButton(
                onClick = onConfig,
                enabled = enabled,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                Text("配置")
            }
        } else {
            androidx.compose.material3.FilledTonalButton(onClick = onAdd, enabled = enabled) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                Text("添加")
            }
        }
    }
}


@Composable
fun AppIcon(pkgName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var icon: ImageBitmap? by remember(pkgName) {
        mutableStateOf(appIconCache[pkgName])
    }

    LaunchedEffect(pkgName) {
        if (appIconCache.containsKey(pkgName)) {
            icon = appIconCache[pkgName]
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            try {
                val d = context.packageManager.getApplicationIcon(pkgName)
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
        appIconCache[pkgName] = loaded
        icon = loaded
    }

    if (icon != null) {
        Image(
            bitmap = icon!!,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Android,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpacerCompat() {
    androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
}


private fun isSystemApp(info: ApplicationInfo): Boolean {
    val f = info.flags
    return (f and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (f and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}



@Composable
private fun PermissionConfigDialog(
    pkg: String,
    cfg: XpConfigState,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val grantKey = XpConfig.keyPermGrantFor(pkg)
    val fakeKey = XpConfig.keyPermFakeFor(pkg)

    
    var grant by remember(pkg) { mutableStateOf(cfg.strSet(grantKey)) }
    var fake by remember(pkg) { mutableStateOf(cfg.strSet(fakeKey)) }
    var showSpecial by remember(pkg) { mutableStateOf(true) }
    var normalFirst by remember(pkg) { mutableStateOf(true) }

    val groups = if (normalFirst) {
        XpConfig.PERM_GROUPS.sortedWith(compareBy({ it.special }, { it.label }))
    } else {
        XpConfig.PERM_GROUPS.sortedBy { it.label }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
        ) {
            Column(Modifier.fillMaxSize()) {
                
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        "权限伪装配置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        pkg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val saved = cfg.strSet(grantKey)
                    Text(
                        if (saved.isEmpty()) {
                            "上次保存：无（该应用回退到「设备伪装」页的规则）"
                        } else {
                            "上次保存：${saved.size} 组 —— " +
                                    saved.mapNotNull { XpConfig.groupById(it)?.label }.joinToString("、")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "记得设置可滚动 —— 列表较长，上下滑动查看全部权限。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    groups.forEach { g ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    g.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (g.special) "特殊权限" else "${g.perms.size} 项权限",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            SwitchColumnCompat(
                                label = "授权",
                                checked = g.id in grant,
                                onCheckedChange = { on ->
                                    grant = if (on) grant + g.id else grant - g.id
                                    if (!on) fake = fake - g.id
                                },
                            )
                            if (g.fakeData) {
                                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                                SwitchColumnCompat(
                                    label = "假数据",
                                    checked = g.id in fake,
                                    enabled = g.id in grant,
                                    onCheckedChange = { on ->
                                        fake = if (on) fake + g.id else fake - g.id
                                    },
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "已选 ${grant.size} 项 / 共 ${XpConfig.PERM_GROUPS.size} 项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            androidx.compose.material3.TextButton(
                                onClick = { grant = XpConfig.ALL_GROUP_IDS },
                            ) { Text("全选", style = MaterialTheme.typography.labelSmall) }
                            androidx.compose.material3.TextButton(
                                onClick = { grant = emptySet(); fake = emptySet() },
                            ) { Text("全不选", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    androidx.compose.material3.Button(
                        onClick = {
                            
                            if (grant.isEmpty()) cfg.put(grantKey, null) else cfg.put(grantKey, grant)
                            if (fake.isEmpty()) cfg.put(fakeKey, null) else cfg.put(fakeKey, fake)

                            
                            val back = cfg.verifyString(grantKey)
                            val backSet = XpConfig.decodeSet(back)
                            val msg = if (grant.isEmpty()) {
                                "已清除该应用的专属配置"
                            } else if (backSet == grant) {
                                "已保存 ${grant.size} 组（校验通过）—— 强制停止该应用后生效"
                            } else {
                                "保存异常：写入 ${grant.size} 组，读回 ${backSet.size} 组，请重开模块再试"
                            }
                            onSaved(msg)
                            onDismiss()
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}


@Composable
private fun SwitchColumnCompat(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}