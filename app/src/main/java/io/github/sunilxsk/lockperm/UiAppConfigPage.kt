package io.github.sunilxsk.lockperm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.launch


private val CONFIG_TABS = listOf(
    "防护功能", "权限伪装", "设备伪装", "设备模板", "其他功能",
)


private const val CONFIG_TAB_DISGUISE = 2




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigPage(
    service: XposedService?,
    pkg: String,
    label: String,
    onBack: () -> Unit,
) {
    val cfg = rememberXpConfig(service, pkg)
    
    
    val enabled = service != null || UiSettings.lspatchActivate

    
    var savedTab by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = savedTab,
        pageCount = { CONFIG_TABS.size },
    )
    val scope = rememberCoroutineScope()

    
    LaunchedEffect(pagerState.currentPage) {
        savedTab = pagerState.currentPage
    }

    
    
    val disguiseScroll = rememberScrollState()

    
    
    val jump = UiNav.jumpToLocation
    LaunchedEffect(jump) {
        if (jump == 0) return@LaunchedEffect
        if (pagerState.currentPage != CONFIG_TAB_DISGUISE) {
            pagerState.animateScrollToPage(CONFIG_TAB_DISGUISE)
            
            kotlinx.coroutines.delay(260)
        }
        disguiseScroll.animateScrollTo(disguiseScroll.maxValue)
    }

    
    
    val density = LocalDensity.current
    val scale = UiSettings.uiScale
    val scaled = remember(density, scale) {
        Density(
            density.density * scale,
            density.fontScale * scale,
        )
    }

    Column(Modifier.fillMaxSize()) {
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    pkgName = pkg,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        pkg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.Close, contentDescription = "返回")
                }
            }
        }

        
        
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
        ) {
            CONFIG_TABS.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            title,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    },
                )
            }
        }

        if (!enabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
            ) {
                Text(
                    "⚠️ 未连接到 LSPosed 框架，设置无法保存。请先在管理器中启用模块。",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalDensity provides scaled
            ) {
                
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    
                    
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (page) {
                        0 -> ShieldConfigContent(cfg, enabled)
                        1 -> PermFakeConfigContent(cfg, enabled)
                        2 -> DisguiseConfigContent(cfg, enabled, disguiseScroll)
                        3 -> DeviceTemplatePage(cfg = cfg, enabled = enabled)
                        else -> MiscConfigContent(cfg, enabled)
                    }
                }
            }
        }
    }
}
