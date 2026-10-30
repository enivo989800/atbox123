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
        // Add Source Dialog
        if (showAddDialog) {
            AddSourceDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { type, name, ref, liveMode ->
                    scope.launch(Dispatchers.IO) {
                        val repo = app.awaitServices().contentSourceRepository
                        val isContent = ref.contains("#genre#", ignoreCase = true) ||
                            ref.contains("\n") ||
                            ref.startsWith("#EXTM3U", ignoreCase = true) ||
                            ref.startsWith("{") ||
                            ref.startsWith("[")
                        val obj = JSONObject()
                            .put("sourceId", "src_${System.currentTimeMillis()}")
                            .put("name", name)
                            .put("sourceType", type)
                            .put("sourceRef", ref)
                            .put("content", if (isContent) ref else "")
                            .put("liveMode", liveMode)
                            .put("enabled", true)
                        val newSource = NativeContentSource.fromJson(obj)
                        repo.add(newSource)
                        if (type == "live") {
                            val userRepo = app.awaitServices().userDataRepository
                            val settings = userRepo.getSettings()
                            userRepo.saveSettings(settings.put("defaultLiveSource", newSource.sourceId))
                        }
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
                            val trimmed = text.trim()
                            if (trimmed.contains("#genre#", ignoreCase = true) || trimmed.startsWith("#EXTM3U", ignoreCase = true)) {
                                val mode = if (trimmed.contains("#genre#", ignoreCase = true)) "TV1 专用直播 (#genre#)" else "标准 M3U 直播"
                                val obj = JSONObject()
                                    .put("sourceId", "import_${System.currentTimeMillis()}")
                                    .put("name", "导入直播源")
                                    .put("sourceType", "live")
                                    .put("sourceRef", "本地配置内容")
                                    .put("content", trimmed)
                                    .put("liveMode", mode)
                                    .put("enabled", true)
                                val newSrc = NativeContentSource.fromJson(obj)
                                repo.add(newSrc)
                                val userRepo = app.awaitServices().userDataRepository
                                val settings = userRepo.getSettings()
                                userRepo.saveSettings(settings.put("defaultLiveSource", newSrc.sourceId))
                                withContext(Dispatchers.Main) {
                                    showImportDialog = false
                                    refresh()
                                    noticeMessage = "成功导入直播源"
                                }
                            } else {
                                val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                                var count = 0
                                lines.forEach { line ->
                                    if (line.contains("http://") || line.contains("https://")) {
                                        val name = if (line.contains(",")) line.substringBefore(",") else "导入源 ${count + 1}"
                                        val url = if (line.contains(",")) line.substringAfter(",").trim() else line
                                        val type = if (url.endsWith(".m3u") || url.endsWith(".m3u8") || url.contains("live") || url.endsWith(".txt")) "live" else "movie"
                                        val mode = if (url.endsWith(".txt")) "TV1 专用直播 (#genre#)" else "标准 M3U 直播"
                                        val obj = JSONObject()
                                            .put("sourceId", "import_${System.currentTimeMillis()}_$count")
                                            .put("name", name)
                                            .put("sourceType", type)
                                            .put("sourceRef", url)
                                            .put("liveMode", mode)
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
    onAdd: (type: String, name: String, ref: String, liveMode: String) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("live") }
    var liveMode by remember { mutableStateOf("TV1 专用直播 (#genre#)") }
    var name by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    var typeDropdownOpen by remember { mutableStateOf(false) }
    var modeDropdownOpen by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    ref = content
                    val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""
                    if (name.isBlank() && fileName.isNotBlank()) {
                        name = fileName
                    }
                    if (content.contains("#genre#", ignoreCase = true)) {
                        type = "live"
                        liveMode = "TV1 专用直播 (#genre#)"
                    } else if (content.startsWith("#EXTM3U", ignoreCase = true)) {
                        type = "live"
                        liveMode = "标准 M3U 直播"
                    }
                }
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFBF8F2),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2D6C5)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: SOURCE SETUP / 添加内容源 / [ ✕ ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SOURCE SETUP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7A6B5D),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "添加内容源",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29221B)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6C8B4)),
                        modifier = Modifier.size(36.dp).clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color(0xFF4A3B2C), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Callout: ⓘ 输入网络 URL、粘贴 JSON / #genre# 文本，或从本地选择 TXT、M3U、JSON 文件。
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5EFE4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3D6C1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("ⓘ", fontSize = 14.sp, color = Color(0xFF5D4E3E))
                        Text(
                            text = "输入网络 URL、粘贴 JSON / #genre# 文本，或从本地选择 TXT、M3U、JSON 文件。",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4E3E),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Field 1: 源名称 选填
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("源名称", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29221B))
                        Text("选填", fontSize = 11.sp, color = Color(0xFF8F8071))
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("例如：家庭影视源", fontSize = 13.sp, color = Color(0xFFA09283)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B2522),
                            unfocusedBorderColor = Color(0xFFDCCFBD),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF29221B),
                            unfocusedTextColor = Color(0xFF29221B)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Field 2: 源地址或配置内容
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("源地址或配置内容", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29221B))
                    OutlinedTextField(
                        value = ref,
                        onValueChange = { ref = it },
                        placeholder = {
                            Text(
                                "https://example.com/api.php/provide/vod/\n或粘贴 JSON、M3U、#genre# 直播源内容",
                                fontSize = 12.sp,
                                color = Color(0xFFA09283)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B2522),
                            unfocusedBorderColor = Color(0xFFDCCFBD),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF29221B),
                            unfocusedTextColor = Color(0xFF29221B)
                        ),
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Local file import row: 也可以直接导入本地文件 + [ ⇪ 选择文件 ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("也可以直接导入本地文件", fontSize = 11.sp, color = Color(0xFF7A6B5D))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6C8B4)),
                        modifier = Modifier.clickable {
                            filePickerLauncher.launch("*/*")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF29221B), modifier = Modifier.size(14.dp))
                            Text("选择文件", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29221B))
                        }
                    }
                }

                // Dropdowns: 源类型 & 直播模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 源类型
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("源类型", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29221B))
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCCFBD)),
                                modifier = Modifier.fillMaxWidth().clickable { typeDropdownOpen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (type == "live") "Live 源" else "Movie 源", fontSize = 12.sp, color = Color(0xFF29221B))
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF5D4E3E), modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = typeDropdownOpen, onDismissRequest = { typeDropdownOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Live 源") },
                                    onClick = { type = "live"; typeDropdownOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Movie 源") },
                                    onClick = { type = "movie"; typeDropdownOpen = false }
                                )
                            }
                        }
                    }

                    // 直播模式
                    Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("直播模式", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29221B))
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCCFBD)),
                                modifier = Modifier.fillMaxWidth().clickable { modeDropdownOpen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(liveMode, fontSize = 11.sp, color = Color(0xFF29221B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF5D4E3E), modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = modeDropdownOpen, onDismissRequest = { modeDropdownOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("TV1 专用直播 (#genre#)") },
                                    onClick = { liveMode = "TV1 专用直播 (#genre#)"; modeDropdownOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("标准 M3U 直播") },
                                    onClick = { liveMode = "标准 M3U 直播"; modeDropdownOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("影视 TVBox JSON") },
                                    onClick = { liveMode = "影视 TVBox JSON"; modeDropdownOpen = false }
                                )
                            }
                        }
                    }
                }

                // Footnote
                Text(
                    text = "保存前会按源内容类型进行识别与校验。",
                    fontSize = 11.sp,
                    color = Color(0xFF8F8071)
                )

                // Bottom buttons: [ 取消 ]  [ 保存源 ] (crimson red)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCCFBD)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDismiss() }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("取消", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29221B))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF8B2522),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (ref.isNotBlank()) {
                                    val finalName = name.ifBlank { if (type == "live") "自定义直播源" else "自定义影视源" }
                                    onAdd(type, finalName.trim(), ref.trim(), liveMode)
                                }
                            }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("保存源", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
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
