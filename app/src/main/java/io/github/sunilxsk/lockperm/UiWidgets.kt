package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.libxposed.service.XposedService







class XpConfigState {

    private var prefs: SharedPreferences? = null

    




    private var pkg: String? = null

    val values: MutableState<Map<String, Any?>> = mutableStateOf(emptyMap())

    val attached: MutableState<Boolean> = mutableStateOf(false)

    fun attach(prefs: SharedPreferences?, pkg: String? = null) {
        this.prefs = prefs
        this.pkg = pkg
        attached.value = prefs != null
        if (prefs == null) {
            values.value = emptyMap()
            return
        }
        val raw = prefs.all ?: emptyMap()
        seedDefaults(raw, prefs)
        values.value = project(
            runCatching { prefs.all }.getOrNull() ?: raw,
            pkg,
        )
    }

    
    private fun project(raw: Map<String, Any?>, pkg: String?): Map<String, Any?> {
        if (pkg.isNullOrBlank()) return raw
        val out = LinkedHashMap<String, Any?>()
        raw.forEach { (k, v) -> if (!k.startsWith(XpConfig.APP_PREFIX)) out[k] = v }
        val p = XpConfig.appPrefix(pkg)
        raw.forEach { (k, v) ->
            if (!k.startsWith(p)) return@forEach
            val stripped = k.removePrefix(p)
            
            if (XpConfig.isGlobalKey(stripped)) return@forEach
            out[stripped] = v
        }
        return out
    }

    
    private fun phys(key: String): String = XpConfig.physicalKey(pkg, key)

    







    private fun seedDefaults(raw: Map<String, Any?>, prefs: SharedPreferences) {
        val ed = runCatching { prefs.edit() }.getOrNull() ?: return
        var dirty = false

        
        
        if (!pkg.isNullOrBlank()) {
            for (key in XpConfig.GLOBAL_KEYS) {
                val stale = XpConfig.appKey(pkg!!, key)
                if (raw.containsKey(stale)) {
                    ed.remove(stale)
                    dirty = true
                }
            }
        }

        for ((key, def) in XpConfig.DEFAULTS) {
            val pk = phys(key)
            if (raw.containsKey(pk)) continue
            when (def) {
                is Boolean -> ed.putBoolean(pk, def)
                is Int -> ed.putInt(pk, def)
                is Long -> ed.putLong(pk, def)
                is String -> ed.putString(pk, def)
                else -> continue
            }
            dirty = true
        }
        if (dirty) runCatching { ed.apply() }
    }

    
    fun rawPrefs(): SharedPreferences? = prefs

    fun bool(key: String, default: Boolean): Boolean = (values.value[key] as? Boolean) ?: default

    fun int(key: String, default: Int): Int = when (val v = values.value[key]) {
        is Int -> v
        is Long -> v.toInt()
        is String -> v.toIntOrNull() ?: default
        else -> default
    }

    fun str(key: String, default: String): String = (values.value[key] as? String) ?: default

    fun strSet(key: String): Set<String> = XpConfig.decodeSet(str(key, ""))

    
    fun strSet(key: String, default: String): Set<String> {
        val raw = str(key, "")
        return XpConfig.decodeSet(raw.ifEmpty { default })
    }

    



    fun verifyString(key: String): String? =
        runCatching { prefs?.getString(phys(key), null) }.getOrNull()

    fun put(key: String, value: Any?) {
        if (value !is Set<*>) {
            values.value = values.value.toMutableMap().apply { this[key] = value }
        }
        val p = prefs ?: return
        val ed = p.edit() ?: return
        val pk = phys(key)
        when (value) {
            null -> ed.remove(pk)
            is Boolean -> ed.putBoolean(pk, value)
            is Int -> ed.putInt(pk, value)
            is String -> ed.putString(pk, value)
            is Set<*> -> {
                
                
                
                val encoded = XpConfig.encodeSet(value.filterIsInstance<String>().toSet())
                values.value = values.value.toMutableMap().apply { this[key] = encoded }
                ed.putString(pk, encoded)
            }
        }
        
        
        
        runCatching { ed.commit() }
    }
}

@Composable
fun rememberXpConfig(service: XposedService?, pkg: String? = null): XpConfigState {
    val state = remember { XpConfigState() }
    androidx.compose.runtime.LaunchedEffect(service, pkg) {
        val p = try {
            service?.getRemotePreferences(XpConfig.PREFS)
        } catch (_: Throwable) {
            null
        }
        state.attach(p, pkg)
    }
    return state
}



@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 4.dp),
    )
}

@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}


@Composable
fun FeatureCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String = "",
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}


@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}


@Composable
fun CheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}




private fun chipRows(size: Int): List<List<Int>> {
    val rows = ArrayList<List<Int>>()
    var i = 0
    while (i < size) {
        rows.add(if (i + 1 < size) listOf(i, i + 1) else listOf(i))
        i += 2
    }
    return rows
}







@Composable
fun MultiSelectChips(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chipRows(options.size).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { i ->
                    val (key, label) = options[i]
                    val isOn = key in selected
                    FilterChip(
                        selected = isOn,
                        onClick = { onToggle(key, !isOn) },
                        enabled = enabled,
                        label = {
                            Text(
                                text = if (isOn) "\u2713 $label" else label,
                                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primaryContainer,
                            selectedLabelColor = cs.onPrimaryContainer,
                            containerColor = cs.surface,
                            labelColor = cs.onSurface,
                        ),
                        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = isOn,
                            borderColor = cs.outline,
                            selectedBorderColor = cs.primary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}





@Composable
fun SingleSelectChips(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chipRows(options.size).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { idx ->
                    val isOn = selectedIndex == idx
                    FilterChip(
                        selected = isOn,
                        onClick = { onSelect(idx) },
                        enabled = enabled,
                        label = {
                            Text(
                                text = if (isOn) "\u2713 ${options[idx]}" else options[idx],
                                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primaryContainer,
                            selectedLabelColor = cs.onPrimaryContainer,
                            containerColor = cs.surface,
                            labelColor = cs.onSurface,
                        ),
                        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = isOn,
                            borderColor = cs.outline,
                            selectedBorderColor = cs.primary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    maxLines: Int = 1,
    fillMax: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = if (fillMax) modifier.fillMaxWidth() else modifier,
        singleLine = singleLine,
        maxLines = maxLines,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
    )
}
