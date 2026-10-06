package io.github.sunilxsk.lockperm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp







@Composable
fun MiscConfigContent(cfg: XpConfigState, enabled: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        
        NativeStatusBar(cfg)

        SectionTitle("稳定性")

        CrashBlockCard(cfg)
        CrashCatcherCard(cfg)

        Spacer(Modifier.height(8.dp))

        SectionTitle("脚本与注入")

        WebViewJsCard(cfg)

        Spacer(Modifier.height(8.dp))

        SectionTitle("原生层")

        NativeAppModeCard(cfg, enabled)

        Spacer(Modifier.height(24.dp))
    }
}
