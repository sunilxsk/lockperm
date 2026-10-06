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
fun PermFakeConfigContent(cfg: XpConfigState, enabled: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("权限与授权")

        PermissionFakeCard(cfg, enabled)

        SectionTitle("系统状态伪装")

        DevOptionFakeRow(cfg, buildOn = true)
        AccountsHideRow(cfg, buildOn = true)

        Spacer(Modifier.height(8.dp))

        SectionTitle("Root 与网络痕迹")

        RootFakeCard(cfg)
        VpnHideCard(cfg)

        Spacer(Modifier.height(24.dp))
    }
}
