package com.yuivo9999.mybox123.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 我的 (Profile) 中心：严格对标前端 MyPage.jsx
 * 包含：用户卡片、播放历史、搜索历史、源管理、设置、外观设置、数据管理、关于
 */
@Composable
fun NativeProfileHubScreen(
    selectedPageName: String,
    onSelectedPageNameChange: (String) -> Unit,
    currentThemeId: String = "sangtian",
    onThemeChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as MyBoxApplication
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val selectedPage = ProfileSubpage.entries.firstOrNull { it.name == selectedPageName } ?: ProfileSubpage.MAIN

    var sourceCount by remember { mutableIntStateOf(0) }
    var historyCount by remember { mutableIntStateOf(0) }
    var searchCount by remember { mutableIntStateOf(0) }

    fun refreshCounts() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val services = application.awaitServices()
                    sourceCount = services.contentSourceRepository.getAll().size
                    historyCount = services.userDataRepository.getHistoryRecords().size
                    searchCount = services.userDataRepository.getSearchRecords().size
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshCounts()
    }

    BackHandler(enabled = selectedPage != ProfileSubpage.MAIN) {
        onSelectedPageNameChange(ProfileSubpage.MAIN.name)
        refreshCounts()
    }

    if (selectedPage != ProfileSubpage.MAIN) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
        ) {
            // Subpage Top Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {
                    onSelectedPageNameChange(ProfileSubpage.MAIN.name)
                    refreshCounts()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = subpageTitle(selectedPage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedPage) {
                    ProfileSubpage.SOURCES -> NativeSourceManagementScreen(
                        onNavigateMultiRepo = { onSelectedPageNameChange(ProfileSubpage.REPOSITORIES.name) }
                    )
                    ProfileSubpage.SETTINGS -> NativeSettingsScreen()
                    ProfileSubpage.APPEARANCE -> NativeAppearanceScreen(
                        currentThemeId = currentThemeId,
                        onThemeChanged = onThemeChanged
                    )
                    ProfileSubpage.REPOSITORIES -> NativeRepositoryManagementScreen()
                    ProfileSubpage.DATA -> NativeDataManagementScreen()
                    ProfileSubpage.ABOUT -> NativeAboutScreen()
                    ProfileSubpage.HISTORY -> NativePlaybackHistoryScreen()
                    ProfileSubpage.SEARCH_HISTORY -> NativeSearchHistorySubpage()
                    ProfileSubpage.MAIN -> {}
                }
            }
        }
        return
    }

    // Main Profile Hub
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 84.dp)
    ) {
        // 1. Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "TVBOX 4K 影音",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "我的",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }

        // 2. User Gold Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TVBox 用户",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.accent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "PRO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "本地数据独立存储 · 产品架构版",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Menus strictly matching MyPage.jsx
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.History,
                    title = "播放历史",
                    badge = if (historyCount > 0) "$historyCount" else null,
                    onClick = { onSelectedPageNameChange(ProfileSubpage.HISTORY.name) }
                )
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                ProfileMenuItem(
                    icon = Icons.Default.Search,
                    title = "搜索历史",
                    badge = if (searchCount > 0) "$searchCount" else null,
                    onClick = { onSelectedPageNameChange(ProfileSubpage.SEARCH_HISTORY.name) }
                )
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                ProfileMenuItem(
                    icon = Icons.Default.Storage,
                    title = "源管理",
                    badge = if (sourceCount > 0) "$sourceCount" else null,
                    onClick = { onSelectedPageNameChange(ProfileSubpage.SOURCES.name) }
                )
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "设置",
                    onClick = { onSelectedPageNameChange(ProfileSubpage.SETTINGS.name) }
                )
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                ProfileMenuItem(
                    icon = Icons.Default.Palette,
                    title = "外观设置",
                    badge = colors.name,
                    onClick = { onSelectedPageNameChange(ProfileSubpage.APPEARANCE.name) }
                )
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                ProfileMenuItem(
                    icon = Icons.Default.CleaningServices,
                    title = "数据管理",
                    onClick = { onSelectedPageNameChange(ProfileSubpage.DATA.name) }
                )
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "关于",
                    onClick = { onSelectedPageNameChange(ProfileSubpage.ABOUT.name) }
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NativeSearchHistorySubpage() {
    val context = LocalContext.current
    val app = context.applicationContext as MyBoxApplication
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    var searches by remember { mutableStateOf<List<com.yuivo9999.mybox123.data.storage.SearchRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                searches = app.awaitServices().userDataRepository.getSearchRecords()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("历史搜索词条", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            if (searches.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            app.awaitServices().userDataRepository.saveSearchRecords(emptyList())
                            searches = emptyList()
                        }
                    }
                ) {
                    Text("清空历史", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        }

        if (searches.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("暂无搜索历史记录", color = colors.textMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searches) { record ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                Text(record.keyword, fontSize = 13.sp, color = colors.textPrimary)
                            }
                            Text("${record.count} 次", fontSize = 11.sp, color = colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

private fun subpageTitle(page: ProfileSubpage): String = when (page) {
    ProfileSubpage.SOURCES -> "源管理"
    ProfileSubpage.SETTINGS -> "播放与偏好设置"
    ProfileSubpage.APPEARANCE -> "外观设置"
    ProfileSubpage.REPOSITORIES -> "多仓管理"
    ProfileSubpage.DATA -> "数据管理"
    ProfileSubpage.ABOUT -> "关于"
    ProfileSubpage.HISTORY -> "播放历史"
    ProfileSubpage.SEARCH_HISTORY -> "搜索历史"
    ProfileSubpage.MAIN -> "我的"
}

enum class ProfileSubpage { MAIN, SOURCES, SETTINGS, APPEARANCE, REPOSITORIES, DATA, ABOUT, HISTORY, SEARCH_HISTORY }
