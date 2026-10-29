package com.yuivo9999.mybox123.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.data.source.NativeContentSource
import com.yuivo9999.mybox123.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun NativeSourceManagementScreen(
    onNavigateMultiRepo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyBoxApplication
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    var sources by remember { mutableStateOf<List<NativeContentSource>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sourceQuery by remember { mutableStateOf("") }
    var sourceFilter by remember { mutableStateOf("all") } // "all", "movie", "live"

    // Dialogs & menus
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var noticeMessage by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            loading = true
            val list = withContext(Dispatchers.IO) {
                app.awaitServices().contentSourceRepository.getAll()
            }
            sources = list
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val totalCount = sources.size
    val enabledCount = sources.count { it.enabled }
    val movieCount = sources.count { it.sourceType == "movie" }
    val liveCount = sources.count { it.sourceType == "live" }

    val filteredSources = remember(sources, sourceQuery, sourceFilter) {
        val q = sourceQuery.trim().lowercase()
        sources.filter { src ->
            val matchFilter = when (sourceFilter) {
                "movie" -> src.sourceType == "movie"
                "live" -> src.sourceType == "live"
                else -> true
            }
            val matchQuery = q.isEmpty() || src.name.lowercase().contains(q) || src.sourceRef.lowercase().contains(q)
            matchFilter && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("全部源", fontSize = 11.sp, color = colors.textSecondary)
                    Text("$totalCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("影视 $movieCount · 直播 $liveCount", fontSize = 10.sp, color = colors.textMuted)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("已启用", fontSize = 11.sp, color = colors.textSecondary)
                    Text("$enabledCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                    Text("允许参与内容检索与播放", fontSize = 10.sp, color = colors.textMuted)
                }
            }
        }

        // 2. Action Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加源", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onNavigateMultiRepo?.invoke() },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("多仓管理", fontSize = 12.sp, color = colors.textPrimary)
            }

            OutlinedButton(
                onClick = { showBatchDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Checklist, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(15.dp))
            }

            OutlinedButton(
                onClick = { showImportDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(15.dp))
            }

            Box {
                OutlinedButton(
                    onClick = { showMoreMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(15.dp))
                }

                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("恢复内置默认源") },
                        leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val repo = app.awaitServices().contentSourceRepository
                                    repo.resetToBuiltinDefaults()
                                    withContext(Dispatchers.Main) {
                                        refresh()
                                        Toast.makeText(context, "已恢复内置默认源", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "重置失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("清空全部内容源") },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red) },
                        onClick = {
                            showMoreMenu = false
                            scope.launch(Dispatchers.IO) {
                                val repo = app.awaitServices().contentSourceRepository
                                sources.forEach { repo.remove(it.sourceId) }
                                withContext(Dispatchers.Main) {
                                    refresh()
                                    Toast.makeText(context, "已清空所有内容源", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // Notice Bar
        if (noticeMessage.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.accent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(noticeMessage, fontSize = 11.sp, color = colors.accent)
                    IconButton(onClick = { noticeMessage = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 3. Search Bar
        OutlinedTextField(
            value = sourceQuery,
            onValueChange = { sourceQuery = it },
            placeholder = { Text("搜索源名称或接口地址…", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (sourceQuery.isNotEmpty()) {
                    IconButton(onClick = { sourceQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "清除", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
        )

        // 4. Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTabButton(
                label = "全部",
                count = totalCount,
                isSelected = sourceFilter == "all",
                onClick = { sourceFilter = "all" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                label = "影视",
                count = movieCount,
                isSelected = sourceFilter == "movie",
                onClick = { sourceFilter = "movie" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                label = "Live",
                count = liveCount,
                isSelected = sourceFilter == "live",
                onClick = { sourceFilter = "live" },
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Source Cards List
        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else if (filteredSources.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("未找到符合条件的内容源", color = colors.textSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredSources, key = { it.sourceId }) { src ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = src.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (src.sourceType == "live") Color(0xFF10B981).copy(alpha = 0.15f) else colors.accent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (src.sourceType == "live") "Live" else "影视",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (src.sourceType == "live") Color(0xFF10B981) else colors.accent,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Switch(
                                    checked = src.enabled,
                                    onCheckedChange = { checked ->
                                        scope.launch(Dispatchers.IO) {
                                            val repo = app.awaitServices().contentSourceRepository
                                            repo.setEnabled(src.sourceId, checked)
                                            withContext(Dispatchers.Main) { refresh() }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.accent,
                                        uncheckedThumbColor = colors.textMuted,
                                        uncheckedTrackColor = colors.surfaceRaised
                                    )
                                )
                            }

                            Text(
                                text = src.sourceRef.ifBlank { "内置扩展" },
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (src.enabled) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (src.enabled) "● 正常可用" else "● 已停用",
                                        fontSize = 10.sp,
                                        color = if (src.enabled) Color(0xFF10B981) else colors.textMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                val repo = app.awaitServices().contentSourceRepository
                                                repo.remove(src.sourceId)
                                                withContext(Dispatchers.Main) {
                                                    refresh()
                                                    Toast.makeText(context, "已删除源: ${src.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "删除", tint = colors.textMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Source Dialog
        if (showAddDialog) {
            AddSourceDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { type, name, ref ->
                    scope.launch(Dispatchers.IO) {
                        val repo = app.awaitServices().contentSourceRepository
                        val obj = JSONObject()
                            .put("sourceId", "src_${System.currentTimeMillis()}")
                            .put("name", name)
                            .put("sourceType", type)
                            .put("sourceRef", ref)
                            .put("enabled", true)
                        repo.add(NativeContentSource.fromJson(obj))
                        withContext(Dispatchers.Main) {
                            showAddDialog = false
                            refresh()
                            noticeMessage = "已成功添加内容源: $name"
                        }
                    }
                }
            )
        }

        // Import Dialog
        if (showImportDialog) {
            ImportSourceDialog(
                onDismiss = { showImportDialog = false },
                onImport = { text ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            val repo = app.awaitServices().contentSourceRepository
                            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                            var count = 0
                            lines.forEach { line ->
                                if (line.contains("http://") || line.contains("https://")) {
                                    val name = if (line.contains(",")) line.substringBefore(",") else "导入源 ${count + 1}"
                                    val url = if (line.contains(",")) line.substringAfter(",").trim() else line
                                    val type = if (url.endsWith(".m3u") || url.endsWith(".m3u8") || url.contains("live")) "live" else "movie"
                                    val obj = JSONObject()
                                        .put("sourceId", "import_${System.currentTimeMillis()}_$count")
                                        .put("name", name)
                                        .put("sourceType", type)
                                        .put("sourceRef", url)
                                        .put("enabled", true)
                                    repo.add(NativeContentSource.fromJson(obj))
                                    count++
                                }
                            }
                            withContext(Dispatchers.Main) {
                                showImportDialog = false
                                refresh()
                                noticeMessage = "成功导入 $count 个内容源"
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }

        // Batch Manager Dialog
        if (showBatchDialog) {
            BatchSourceDialog(
                sources = sources,
                onDismiss = { showBatchDialog = false },
                onToggleAll = { enable ->
                    scope.launch(Dispatchers.IO) {
                        val repo = app.awaitServices().contentSourceRepository
                        sources.forEach { repo.setEnabled(it.sourceId, enable) }
                        withContext(Dispatchers.Main) {
                            showBatchDialog = false
                            refresh()
                            noticeMessage = if (enable) "已批量启用全部源" else "已批量停用全部源"
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterTabButton(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) colors.accent else colors.surfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$label ",
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else colors.textPrimary
            )
            Text(
                text = "$count",
                fontSize = 10.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else colors.textMuted
            )
        }
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, name: String, ref: String) -> Unit
) {
    var type by remember { mutableStateOf("movie") }
    var name by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    val colors = AppTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加内容源", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { type = "movie" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "movie") colors.accent else colors.surfaceRaised)
                    ) {
                        Text("影视源", color = if (type == "movie") Color.White else colors.textPrimary)
                    }
                    Button(
                        onClick = { type = "live" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "live") colors.accent else colors.surfaceRaised)
                    ) {
                        Text("Live 直播源", color = if (type == "live") Color.White else colors.textPrimary)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("源名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("接口地址 (JSON / M3U)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && ref.isNotBlank()) onAdd(type, name.trim(), ref.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("确定添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun ImportSourceDialog(
    onDismiss: () -> Unit,
    onImport: (text: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val colors = AppTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入单仓或批量地址", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("支持粘贴 TVBox 订阅地址或 M3U 播放列表 URL，每行一条：", fontSize = 11.sp, color = colors.textSecondary)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("例如：源名称,https://example.com/api.json\nhttps://example.com/live.m3u", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onImport(text.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("开始导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun BatchSourceDialog(
    sources: List<NativeContentSource>,
    onDismiss: () -> Unit,
    onToggleAll: (enable: Boolean) -> Unit
) {
    val colors = AppTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量源管理", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("共有 ${sources.size} 个内容源，可一键批量设置其启用状态：", fontSize = 12.sp, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onToggleAll(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("一键启用全部")
                    }
                    OutlinedButton(
                        onClick = { onToggleAll(false) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("一键停用全部", color = colors.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成", color = colors.accent) }
        }
    )
}
