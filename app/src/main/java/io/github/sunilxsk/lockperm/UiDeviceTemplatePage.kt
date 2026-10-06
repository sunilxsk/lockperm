package io.github.sunilxsk.lockperm

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp










@Composable
fun DeviceTemplatePage(cfg: XpConfigState, enabled: Boolean) {
    val context = LocalContext.current
    val prefs = cfg.rawPrefs()

    var list by remember(prefs) { mutableStateOf(DeviceTemplateStore.load(prefs)) }
    
    val columns = UiSettings.templateColumns
    var editing by remember { mutableStateOf<DeviceTemplate?>(null) }
    var pasteOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    
    val exportAllLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            val text = DeviceTemplateStore.encodeAll(list)
            val os = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
            os.use {
                it.write(text.toByteArray(Charsets.UTF_8))
                it.flush()
            }
            true
        }.getOrDefault(false)
        message = if (ok) "已导出 ${list.size} 份模板" else "导出失败"
    }

    
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8)
        }.getOrNull()
        if (text.isNullOrBlank()) {
            message = "读取文件失败"
            return@rememberLauncherForActivityResult
        }
        val parsed = DeviceTemplateStore.decodeAny(text)
        if (parsed.isEmpty()) {
            message = "没认出这是设备模板"
            return@rememberLauncherForActivityResult
        }
        
        val merged = DeviceTemplateStore.load(prefs).toMutableList()
        var added = 0
        parsed.forEach { t ->
            val idx = merged.indexOfFirst { it.id == t.id }
            if (idx >= 0) merged[idx] = t else merged.add(0, t)
            added++
        }
        DeviceTemplateStore.save(prefs, merged)
        list = DeviceTemplateStore.load(prefs)
        message = if (added == 1) "已导入「${parsed[0].name}」" else "已导入 $added 份模板"
    }

    editing?.let { tpl ->
        DeviceTemplateEditor(
            initial = tpl,
            onSave = { saved ->
                DeviceTemplateStore.upsert(prefs, saved)
                list = DeviceTemplateStore.load(prefs)
                editing = null
                message = "已保存「${saved.name}」"
            },
            onCancel = { editing = null },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        
        
        
        InfoCard {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "设备模板",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "每行显示几个",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        listOf("每行 1 个", "每行 2 个", "每行 3 个")
                            .forEachIndexed { idx, label ->
                                val on = columns == idx + 1
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (on) "\u2713 $label" else label,
                                            fontWeight = if (on) FontWeight.Bold
                                            else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        UiSettings.setTemplateColumns(context, idx + 1)
                                    },
                                )
                            }
                    }
                }
            }
            if (!enabled) {
                Text(
                    "⚠️ 未连接到框架，模板无法保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        
        Box(Modifier.weight(1f)) {
            if (list.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "还没有模板",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    HintText("点下面的「导出」把当前这台机器存成一份，之后再改成想要的模样")
                }
            } else {
                
                
                if (columns == 1) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list, key = { it.id }) { tpl ->
                            TemplateRow(
                                tpl = tpl,
                                columns = columns,
                                enabled = enabled,
                                onApply = {
                                    DeviceTemplateSpec.apply(cfg, tpl)
                                    message = "已套用「${tpl.name}」"
                                },
                                onEdit = { editing = tpl },
                                onCopy = {
                                    val c = tpl.copy(
                                        id = DeviceTemplate.newId(),
                                        name = "${tpl.name} 副本",
                                    )
                                    DeviceTemplateStore.upsert(prefs, c)
                                    list = DeviceTemplateStore.load(prefs)
                                },
                                onDelete = {
                                    DeviceTemplateStore.delete(prefs, tpl.id)
                                    list = DeviceTemplateStore.load(prefs)
                                },
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list, key = { it.id }) { tpl ->
                            TemplateRow(
                                tpl = tpl,
                                columns = columns,
                                enabled = enabled,
                                onApply = {
                                    DeviceTemplateSpec.apply(cfg, tpl)
                                    message = "已套用「${tpl.name}」"
                                },
                                onEdit = { editing = tpl },
                                onCopy = {
                                    val c = tpl.copy(
                                        id = DeviceTemplate.newId(),
                                        name = "${tpl.name} 副本",
                                    )
                                    DeviceTemplateStore.upsert(prefs, c)
                                    list = DeviceTemplateStore.load(prefs)
                                },
                                onDelete = {
                                    DeviceTemplateStore.delete(prefs, tpl.id)
                                    list = DeviceTemplateStore.load(prefs)
                                },
                            )
                        }
                    }
                }
            }
        }

        message?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(msg, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { message = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                }
            }
        }
        
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        editing = runCatching { DeviceProbe.collect(context) }
                            .getOrDefault(DeviceTemplate(name = "本设备"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" 导出", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { pasteOpen = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.Filled.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" 导入", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { editing = DeviceTemplate(name = "新模板") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" 新建", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(
                onClick = {
                    if (list.isEmpty()) {
                        message = "模板库是空的，没有可导出的内容"
                        return@OutlinedButton
                    }
                    runCatching { exportAllLauncher.launch("LockPerm_模板库.json") }
                        .onFailure { message = "无法打开系统文件选择器" }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(" 导出全部模板（保存到文件）", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }


    if (pasteOpen) {
        PasteTemplateDialog(
            context = context,
            onDismiss = { pasteOpen = false },
            onParsed = { tpl ->
                pasteOpen = false
                editing = tpl
            },
            onPickFile = {
                runCatching { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    .onFailure { message = "无法打开系统文件选择器" }
            },
        )
    }
}





@Composable
private fun TemplateRow(
    tpl: DeviceTemplate,
    columns: Int,
    enabled: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (columns >= 2) {
            
            
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    tpl.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${tpl.filledCount()} 项 · ${tplTime(tpl.updated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = onApply,
                        enabled = enabled,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp, vertical = 0.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 34.dp),
                    ) {
                        Text("套用", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "复制",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tpl.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${tpl.filledCount()} 项 · ${tplTime(tpl.updated)}" +
                                if (tpl.note.isNotBlank()) " · ${tpl.note}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApply,
                        enabled = enabled,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("套用到本应用")
                    }
                    OutlinedButton(
                        onClick = onCopy,
                        enabled = enabled,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("复制")
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除模板") },
            text = { Text("确定删除「${tpl.name}」？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}





@Composable
private fun PasteTemplateDialog(
    context: Context,
    onDismiss: () -> Unit,
    onParsed: (DeviceTemplate) -> Unit,
    
    onPickFile: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入模板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HintText("粘贴模板文本，或者直接选一份导出的 JSON 文件（支持整个模板库）")
                
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPickFile()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" 选择文件", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                LabeledTextField(
                    label = "模板内容",
                    value = text,
                    onValueChange = { text = it; error = null },
                    singleLine = false,
                    maxLines = 8,
                )
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tpl = DeviceTemplateStore.decode(text.trim())
                if (tpl == null) {
                    error = "没认出这是一份设备模板"
                } else {
                    onParsed(tpl)
                }
            }) { Text("解析并编辑") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    val clip = runCatching {
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    }.getOrNull()
                    val t = clip?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                    if (t.isNotBlank()) text = t
                }) { Text("从剪贴板") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}












@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTemplateEditor(
    initial: DeviceTemplate,
    onSave: (DeviceTemplate) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val draft = remember {
        mutableStateOf(
            initial.copy(data = LinkedHashMap(initial.data))
        )
    }
    var rawMode by remember { mutableStateOf(false) }
    var rawText by remember { mutableStateOf(DeviceTemplateStore.encode(initial)) }
    var rawError by remember { mutableStateOf<String?>(null) }

    fun leave() {
        if (rawMode) {
            
            val parsed = DeviceTemplateStore.decode(rawText)
            if (parsed == null) {
                rawError = "JSON 无法解析，请检查后再退出"
                return
            }
            parsed.id = draft.value.id
            onSave(parsed)
            return
        }
        onSave(draft.value)
    }

    BackHandler { leave() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (initial.name.isBlank()) "编辑模板" else initial.name) },
            navigationIcon = {
                IconButton(onClick = { leave() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "保存并返回")
                }
            },
            actions = {
                IconButton(onClick = {
                    if (rawMode) {
                        val parsed = DeviceTemplateStore.decode(rawText)
                        if (parsed == null) {
                            rawError = "JSON 无法解析"
                            return@IconButton
                        }
                        parsed.id = draft.value.id
                        draft.value = parsed
                        rawMode = false
                        rawError = null
                    } else {
                        rawText = DeviceTemplateStore.encode(draft.value)
                        rawMode = true
                    }
                }) {
                    Icon(
                        if (rawMode) Icons.Filled.Edit else Icons.Filled.Code,
                        contentDescription = "切换源码模式",
                    )
                }
                IconButton(onClick = { leave() }) {
                    Icon(Icons.Filled.Check, contentDescription = "保存")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "放弃")
                }
            },
        )

        if (rawMode) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabeledTextField(
                    label = "模板 JSON",
                    value = rawText,
                    onValueChange = { rawText = it; rawError = null },
                    singleLine = false,
                    maxLines = 24,
                )
                rawError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            return
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LabeledTextField(
                        label = "模板名称",
                        value = draft.value.name,
                        onValueChange = { draft.value = draft.value.copy(name = it) },
                    )
                    LabeledTextField(
                        label = "备注",
                        value = draft.value.note,
                        onValueChange = { draft.value = draft.value.copy(note = it) },
                    )
                    HintText("已填 ${draft.value.filledCount()} 项")
                }
            }

            DeviceTemplateSpec.GROUPS.forEach { group ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionTitle(group.title)
                        group.fields.forEach { f ->
                            LabeledTextField(
                                label = if (f.hint.isBlank()) f.label else "${f.label}（${f.hint}）",
                                value = draft.value.data[f.key].orEmpty(),
                                onValueChange = {
                                    draft.value = draft.value.copy(
                                        data = LinkedHashMap(draft.value.data).apply {
                                            if (it.isBlank()) remove(f.key) else put(f.key, it)
                                        }
                                    )
                                },
                                singleLine = !f.multiline,
                                maxLines = if (f.multiline) 10 else 1,
                            )
                        }
                    }
                }
            }

            
            var exported by remember { mutableStateOf<String?>(null) }
            Button(
                onClick = { exported = DeviceTemplateStore.encode(draft.value) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("生成模板文本")
            }
            exported?.let { txt ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            txt,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                runCatching {
                                    val cm = context.getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                    ) as? ClipboardManager
                                    cm?.setPrimaryClip(
                                        android.content.ClipData.newPlainText("lockperm template", txt)
                                    )
                                }
                                exported = null
                            }) { Text("复制") }
                            OutlinedButton(onClick = { exported = null }) { Text("收起") }
                        }
                    }
                }
            }

            Box(Modifier.padding(bottom = 24.dp))
        }
    }
}
