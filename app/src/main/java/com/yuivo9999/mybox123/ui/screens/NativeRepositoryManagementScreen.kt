package com.yuivo9999.mybox123.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.ui.theme.AppTheme

@Composable
fun NativeRepositoryManagementScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyBoxApplication
    val vm: RepositoryManagementViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                RepositoryManagementViewModel(app) as T
        }
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRepo by remember { mutableStateOf<RepositoryEntry?>(null) }
    var detailRepo by remember { mutableStateOf<RepositoryEntry?>(null) }

    LaunchedEffect(Unit) {
        vm.load()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    val totalCount = state.repositories.size
    val enabledCount = state.repositories.count { it.enabled }
    val allSelected = enabledCount == totalCount && totalCount > 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header (Image 5):
        // Breadcrumb: 源管理 / 多仓管理
        // Title: 多仓聚合中心
        // Subtitle: 支持管理多仓配置、自动展开主备地址并在主界面中无缝调用对应源。
        // Top right buttons: [ + 添加多仓 ] (crimson red)  [ ✕ ]
        item {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "源管理 / 多仓管理",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "多仓聚合中心",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "支持管理多仓配置、自动展开主备地址并在主界面中无缝调用对应源。",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    // Action buttons: [ + 添加多仓 ]  [ ✕ ]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF8B2522), // Crimson red
                            modifier = Modifier.clickable {
                                editingRepo = null
                                showAddDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("添加多仓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (onBack != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.card,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.size(36.dp).clickable { onBack() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Section 1: SOURCE WORKSPACE (Image 5)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Text(
                        text = "SOURCE WORKSPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        letterSpacing = 1.sp
                    )
                }

                // 4 Capability Cards in 2x2 Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspaceCard(
                        title = "中文域名转换",
                        desc = "自动完成 Punycode 转换",
                        modifier = Modifier.weight(1f)
                    )
                    WorkspaceCard(
                        title = "主备地址回退",
                        desc = "主地址超时自动切备用",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspaceCard(
                        title = "失败重试",
                        desc = "内置指数避退多轮重试",
                        modifier = Modifier.weight(1f)
                    )
                    WorkspaceCard(
                        title = "重复项合并",
                        desc = "多仓源合并并自动去重",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Section 2: YOUR SOURCES Header & Batch Toolbar (Image 5)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "YOUR SOURCES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("仓源列表", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceRaised,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text("$totalCount", fontSize = 11.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text("已启用 $enabledCount 个，管理当前项目订阅的多仓规则。", fontSize = 11.sp, color = colors.textSecondary)
                    }

                    // Action buttons: [ ☑ 取消全选 ]  [ 🔄 恢复预置仓 ]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable {
                                val target = !allSelected
                                state.repositories.forEach { vm.toggle(it, target) }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                                Text(if (allSelected) "取消全选" else "全选", fontSize = 11.sp, color = colors.textPrimary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable { vm.restoreDefaults() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                                Text("恢复预置仓", fontSize = 11.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }

        // 4. Repository Cards List (Image 5)
        if (state.loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
        } else {
            items(state.repositories, key = { it.id }) { repo ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Top info row: Checkbox, Globe avatar, Title, Status badge, Backup count badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Checkbox
                                Checkbox(
                                    checked = repo.enabled,
                                    onCheckedChange = { vm.toggle(repo, it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF8B2522),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )

                                // Globe Circle Icon
                                Surface(
                                    shape = CircleShape,
                                    color = colors.surfaceRaised,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Public, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = repo.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )

                                        // Status pill: ● 未同步 / ● 已同步
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.surfaceRaised,
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(5.dp).clip(CircleShape).background(
                                                        if (repo.siteCount > 0) Color(0xFF10B981) else colors.textMuted
                                                    )
                                                )
                                                Text(
                                                    text = if (repo.siteCount > 0) "已同步" else "未同步",
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }

                                        // Backup address count badge: 备用地址 X
                                        if (repo.backupUrls.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = colors.surfaceRaised,
                                                border = BorderStroke(1.dp, colors.border)
                                            ) {
                                                Text(
                                                    text = "备用地址 ${repo.backupUrls.size}",
                                                    fontSize = 10.sp,
                                                    color = colors.textMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Subtitle: 主地址: http://...
                                    Text(
                                        text = "主地址: ${repo.primaryUrl}",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Bottom action buttons row: [ 👁 查看详情 ]  [ 🔄 同步 ]  [ ✎ ]  [ 🗑 ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 查看详情
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceRaised,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.clickable { detailRepo = repo }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(13.dp))
                                        Text("查看详情", fontSize = 11.sp, color = colors.textPrimary)
                                    }
                                }

                                // 同步
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceRaised,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.clickable { vm.syncPreview(repo) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(13.dp))
                                        Text("同步", fontSize = 11.sp, color = colors.textPrimary)
                                    }
                                }
                            }

                            // Edit and Delete icons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceRaised,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.size(32.dp).clickable {
                                        editingRepo = repo
                                        showAddDialog = true
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceRaised,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.size(32.dp).clickable { vm.remove(repo) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "删除", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Repo Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf(editingRepo?.name.orEmpty()) }
        var primaryUrl by remember { mutableStateOf(editingRepo?.primaryUrl.orEmpty()) }
        var backupUrls by remember { mutableStateOf(editingRepo?.backupUrls?.joinToString("\n").orEmpty()) }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFBF8F2),
                border = BorderStroke(1.dp, Color(0xFFE2D6C5)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (editingRepo != null) "编辑多仓" else "添加多仓",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF29221B)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("仓库名称") },
                        placeholder = { Text("例如：饭太硬") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = primaryUrl,
                        onValueChange = { primaryUrl = it },
                        label = { Text("主地址 (URL)") },
                        placeholder = { Text("http://饭太硬.top/tv") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = backupUrls,
                        onValueChange = { backupUrls = it },
                        label = { Text("备用地址 (每行一个)") },
                        placeholder = { Text("http://backup.top/tv") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFDCCFBD)),
                            modifier = Modifier.weight(1f).clickable { showAddDialog = false }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text("取消", color = Color(0xFF29221B), fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF8B2522),
                            modifier = Modifier.weight(1f).clickable {
                                if (name.isNotBlank() && primaryUrl.isNotBlank()) {
                                    vm.save(name, primaryUrl, backupUrls, editingRepo?.id)
                                    showAddDialog = false
                                }
                            }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text("保存", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    if (detailRepo != null) {
        val repo = detailRepo!!
        AlertDialog(
            onDismissRequest = { detailRepo = null },
            title = { Text(repo.name, fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("主地址：${repo.primaryUrl}", fontSize = 12.sp, color = colors.textSecondary)
                    if (repo.backupUrls.isNotEmpty()) {
                        Text("备用地址：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        repo.backupUrls.forEach {
                            Text("• $it", fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }
                    Text("站点统计：影视 ${repo.siteCount} · 直播 ${repo.liveCount} · 解析 ${repo.parseCount}", fontSize = 11.sp, color = colors.textMuted)
                }
            },
            confirmButton = {
                TextButton(onClick = { detailRepo = null }) { Text("关闭", color = colors.accent) }
            }
        )
    }
}

@Composable
private fun WorkspaceCard(
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(desc, fontSize = 10.sp, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
