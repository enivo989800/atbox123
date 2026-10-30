package com.yuivo9999.mybox123.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.ViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.data.movie.NativeMovieItem
import com.yuivo9999.mybox123.data.storage.SearchRecord
import androidx.compose.foundation.BorderStroke
import com.yuivo9999.mybox123.ui.theme.AppTheme
import com.yuivo9999.mybox123.ui.theme.MyBoxAmber
import com.yuivo9999.mybox123.ui.theme.MyBoxBackground
import com.yuivo9999.mybox123.ui.theme.MyBoxBorder
import com.yuivo9999.mybox123.ui.theme.MyBoxGold
import com.yuivo9999.mybox123.ui.theme.MyBoxGoldLight
import com.yuivo9999.mybox123.ui.theme.MyBoxHeart
import com.yuivo9999.mybox123.ui.theme.MyBoxOutline
import com.yuivo9999.mybox123.ui.theme.MyBoxSurface
import com.yuivo9999.mybox123.ui.theme.MyBoxSurfaceRaised
import com.yuivo9999.mybox123.ui.theme.MyBoxTextMuted
import com.yuivo9999.mybox123.ui.theme.MyBoxTextPrimary
import com.yuivo9999.mybox123.ui.theme.MyBoxTextSecondary
import org.json.JSONObject

@Composable
fun NativeMovieCatalogScreen(
    modifier: Modifier = Modifier,
    favoriteNavigationJson: String? = null,
    onFavoriteNavigationConsumed: () -> Unit = {},
    pendingSearchKeyword: String? = null,
    onSearchNavigationConsumed: () -> Unit = {},
    returnToHomeAfterDetail: Boolean = false,
    onReturnToHomeAfterDetail: () -> Unit = {},
    onPlayMovie: (NativeMovieItem) -> Unit = {},
) {
    val application = LocalContext.current.applicationContext as MyBoxApplication
    val factory = remember(application) { movieCatalogViewModelFactory(application) }
    val viewModel: MovieCatalogViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var confirmClearSearchHistory by rememberSaveable { mutableStateOf(false) }

    // BackHandler for Detail or Search History
    BackHandler(enabled = state.detailOpen || showHistory || showSearchBar) {
        if (state.detailOpen) {
            viewModel.closeDetail()
            if (returnToHomeAfterDetail) onReturnToHomeAfterDetail()
        } else if (showHistory) {
            showHistory = false
        } else if (showSearchBar) {
            showSearchBar = false
        }
    }

    LaunchedEffect(viewModel) { viewModel.loadSources() }
    LaunchedEffect(viewModel, pendingSearchKeyword) {
        pendingSearchKeyword?.let { rawKeyword ->
            val kw = rawKeyword.trim()
            if (kw.isNotEmpty()) {
                showSearchBar = true
                viewModel.searchFromNavigation(kw)
            }
            onSearchNavigationConsumed()
        }
    }
    LaunchedEffect(viewModel, showHistory) {
        if (showHistory) viewModel.loadSearchHistory()
    }
    LaunchedEffect(viewModel, favoriteNavigationJson, state.sourcesLoaded) {
        val pending = favoriteNavigationJson
        if (pending != null && state.sourcesLoaded) {
            viewModel.openFavoriteJson(pending)
            onFavoriteNavigationConsumed()
        }
    }

    if (confirmClearSearchHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearSearchHistory = false },
            containerColor = AppTheme.colors.card,
            title = { Text("清空搜索历史？", color = AppTheme.colors.textPrimary) },
            text = { Text("此操作将清空本地保存的所有影视搜索关键词记录。", color = AppTheme.colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClearSearchHistory = false
                        viewModel.clearSearchHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearSearchHistory = false }) { Text("取消", color = AppTheme.colors.textSecondary) }
            },
        )
    }

    // Detail Screen (Sangtian Style)
    if (state.detailOpen && state.detailMovie != null) {
        SangtianMovieDetailView(
            movie = state.detailMovie!!,
            sourceName = state.sources.firstOrNull { it.sourceId == state.selectedSourceId }?.name ?: "内置源",
            loading = state.detailLoading,
            error = state.detailError,
            favoriteSaved = state.detailFavoriteSaved,
            favoriteSaving = state.detailFavoriteSaving,
            onBack = {
                viewModel.closeDetail()
                if (returnToHomeAfterDetail) onReturnToHomeAfterDetail()
            },
            onPlay = { onPlayMovie(state.detailMovie!!) },
            onPlayMovie = onPlayMovie,
            onFavorite = viewModel::addDetailToFavorites,
            modifier = modifier
        )
        return
    }

    // Main Catalog Screen
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .statusBarsPadding()
    ) {
        // 1. Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "TVBOX 4K 影音",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.accent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "影视库",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Selector Pill
                Box {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AppTheme.colors.card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier.clickable { sourceMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.accent)
                            )
                            Text(
                                text = state.sources.firstOrNull { it.sourceId == state.selectedSourceId }?.name?.take(6) ?: "内置源",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = { sourceMenuExpanded = false }
                    ) {
                        state.sources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    sourceMenuExpanded = false
                                    viewModel.selectSource(source.sourceId)
                                }
                            )
                        }
                    }
                }

                // Search Icon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (showSearchBar) AppTheme.colors.accent else AppTheme.colors.card,
                    border = if (showSearchBar) null else androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.clickable { showSearchBar = !showSearchBar }
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = if (showSearchBar) Color.White else AppTheme.colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // History Icon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (showHistory) AppTheme.colors.accent else AppTheme.colors.card,
                    border = if (showHistory) null else androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.clickable { showHistory = !showHistory }
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "搜索历史",
                            tint = if (showHistory) Color.White else AppTheme.colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Search Input (if expanded)
        if (showSearchBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = viewModel::updateKeyword,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜索全部影视内容…", color = AppTheme.colors.textMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppTheme.colors.card,
                        unfocusedContainerColor = AppTheme.colors.card,
                        focusedBorderColor = AppTheme.colors.accent,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (state.keyword.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateKeyword(""); viewModel.search() }) {
                                Icon(Icons.Default.Close, contentDescription = "清空", tint = AppTheme.colors.textMuted)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() })
                )
                Button(
                    onClick = { viewModel.search() },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("搜索", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Search History Panel
        if (showHistory) {
            CatalogSearchHistoryPanel(
                records = state.searchHistory,
                loading = state.searchHistoryLoading,
                error = state.searchHistoryError,
                onRetry = viewModel::loadSearchHistory,
                onUse = { kw ->
                    showHistory = false
                    showSearchBar = true
                    viewModel.useSearchKeyword(kw)
                },
                onDelete = viewModel::deleteSearchRecord,
                onClearAll = { confirmClearSearchHistory = true },
                modifier = Modifier.weight(1f)
            )
            return
        }

        // 4. Categories Horizontal Chip Bar
        if (state.categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "全部" chip
                item {
                    val isAllSelected = state.selectedCategoryId == null || state.selectedCategoryId == "all"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllSelected) AppTheme.colors.accent else AppTheme.colors.card,
                        border = if (isAllSelected) null else androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier.clickable { viewModel.selectCategory(null) }
                    ) {
                        Text(
                            text = "全部",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAllSelected) Color.White else AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                items(state.categories, key = { it.id }) { cat ->
                    val isSelected = state.selectedCategoryId == cat.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AppTheme.colors.accent else AppTheme.colors.card,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier.clickable { viewModel.selectCategory(cat.id) }
                    ) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 5. Status / Content Count Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已展示 ${state.items.size} 部内容",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textMuted
            )
            if (state.loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = AppTheme.colors.accent)
                    Text("加载中…", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.accent)
                }
            }
        }

        // 6. 3-Column Poster Grid
        when {
            state.loading && state.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = AppTheme.colors.accent)
                        Text("正在读取影视内容…", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    }
                }
            }
            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = AppTheme.colors.textMuted, modifier = Modifier.size(36.dp))
                        Text(
                            if (state.keyword.isBlank()) "暂无影视条目" else "未找到与“${state.keyword}”相关的影片",
                            color = AppTheme.colors.textMuted,
                            fontSize = 14.sp
                        )
                        OutlinedButton(
                            onClick = { viewModel.refresh() },
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border)
                        ) {
                            Text("刷新重试", color = AppTheme.colors.textPrimary)
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 84.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.items, key = { it.sourceId + ":" + it.sourceItemId.ifBlank { it.contentId } }) { movie ->
                        SangtianMovieCard(
                            movie = movie,
                            onClick = { viewModel.openDetail(movie) }
                        )
                    }

                    if (state.items.size.toLong() < state.total) {
                        item(span = { GridItemSpan(3) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.loadNextPage() },
                                    enabled = !state.loading,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border)
                                ) {
                                    Text(if (state.loading) "正在载入…" else "载入更多", color = AppTheme.colors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 3-Column Poster Card in Sangtian Theme */
@Composable
private fun SangtianMovieCard(
    movie: NativeMovieItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(AppTheme.colors.card)
                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = movie.poster.ifBlank { movie.toJson().optString("vod_pic") },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top-left quality tag
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "4K",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.accent
                )
            }

            // Top-right rating or update tag
            val score = movie.toJson().optString("score").ifBlank { movie.toJson().optString("rating") }
            if (score.isNotBlank() && score != "null") {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppTheme.colors.accent)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = score,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.title.ifBlank { "未命名" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = movie.category.ifBlank { "影视" },
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Rich Sangtian Movie Detail Screen */
@Composable
private fun SangtianMovieDetailView(
    movie: NativeMovieItem,
    sourceName: String,
    loading: Boolean,
    error: String?,
    favoriteSaved: Boolean,
    favoriteSaving: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPlayMovie: ((NativeMovieItem) -> Unit)? = null,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val json = movie.toJson()
    val backdrop = json.optString("backdrop").ifBlank { movie.poster }.ifBlank { json.optString("vod_pic") }
    val director = json.optString("director").ifBlank { json.optString("vod_director") }
    val actor = json.optString("actor").ifBlank { json.optString("vod_actor") }
    val score = json.optString("score").ifBlank { json.optString("rating") }.ifBlank { "8.2" }
    val year = json.optString("year").ifBlank { json.optString("vod_year") }.ifBlank { "2023" }
    val region = json.optString("area").ifBlank { json.optString("region") }.ifBlank { "中国大陆" }
    val description = movie.description.ifBlank { json.optString("summary") }.ifBlank { json.optString("content") }.ifBlank { "暂无剧情简介。" }

    var showOtherSourcesDialog by remember { mutableStateOf(false) }

    if (showOtherSourcesDialog) {
        OtherSourceSearchDialog(
            title = movie.title,
            currentSourceId = movie.sourceId,
            onClose = { showOtherSourcesDialog = false },
            onSelectMovie = { otherMovie: NativeMovieItem ->
                showOtherSourcesDialog = false
                if (onPlayMovie != null) {
                    onPlayMovie(otherMovie)
                } else {
                    onPlay()
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        contentPadding = PaddingValues(bottom = 84.dp)
    ) {
        // Hero Backdrop with back button & overlay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f),
                                    AppTheme.colors.background
                                )
                            )
                        )
                )

                // Back Button & source tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = sourceName,
                            fontSize = 11.sp,
                            color = AppTheme.colors.accent
                        )
                    }
                }

                // Poster & Title Info in Hero bottom
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, AppTheme.colors.border, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = movie.poster.ifBlank { json.optString("vod_pic") },
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AppTheme.colors.accent, modifier = Modifier.size(14.dp))
                                Text(score, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                            }
                            Text("·", color = AppTheme.colors.textMuted)
                            Text(year, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Text("·", color = AppTheme.colors.textMuted)
                            Text(region, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Text("·", color = AppTheme.colors.textMuted)
                            Text(movie.category.ifBlank { "4K超清" }, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }

        // Action Buttons Row: 立即播放 + 搜同名 + 加入收藏
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("立即播放", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = { showOtherSourcesDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AppTheme.colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("搜同名", color = AppTheme.colors.accent, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onFavorite,
                    enabled = !favoriteSaving,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (favoriteSaved) MyBoxHeart else AppTheme.colors.border)
                ) {
                    Icon(
                        imageVector = if (favoriteSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (favoriteSaved) MyBoxHeart else AppTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (favoriteSaved) "已收藏" else "收藏",
                        color = if (favoriteSaved) MyBoxHeart else AppTheme.colors.textPrimary
                    )
                }
            }
        }

        // Episodes Selector
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选集播放 (${movie.episodes.size.coerceAtLeast(1)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Text("4K超清线路", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.accent)
                }

                if (movie.episodes.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(movie.episodes.size) { index ->
                            val ep = movie.episodes[index]
                            val label = ep.optString("title").ifBlank { ep.optString("name") }.ifBlank { "第 ${index + 1} 集" }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (index == 0) AppTheme.colors.accent.copy(alpha = 0.15f) else AppTheme.colors.card,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (index == 0) AppTheme.colors.accent else AppTheme.colors.border),
                                modifier = Modifier.clickable { onPlay() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (index == 0) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (index == 0) AppTheme.colors.accent else AppTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppTheme.colors.card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier.clickable { onPlay() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AppTheme.colors.accent)
                            Text("正片 4K", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Director & Cast info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (director.isNotBlank() && director != "null") {
                    Text("导演：$director", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                }
                if (actor.isNotBlank() && actor != "null") {
                    Text("主演：$actor", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                }
            }
        }

        // Synopsis
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "剧情简介",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(14.dp),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

/** Search History Panel for Catalog */
@Composable
private fun CatalogSearchHistoryPanel(
    records: List<SearchRecord>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onUse: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
            if (records.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("清空历史", color = Color(0xFFEF4444))
                }
            }
        }

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无搜索历史", color = AppTheme.colors.textMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(records, key = { it.searchId }) { rec ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppTheme.colors.card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUse(rec.keyword) }
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = AppTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                                Text(rec.keyword, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                            }
                            IconButton(onClick = { onDelete(rec.searchId) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "删除", tint = AppTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun movieCatalogViewModelFactory(application: MyBoxApplication): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovieCatalogViewModel::class.java)) return MovieCatalogViewModel(application) as T
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
