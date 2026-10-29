package com.yuivo9999.mybox123.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.app.AppDestination
import com.yuivo9999.mybox123.data.movie.NativeMovieItem
import com.yuivo9999.mybox123.ui.theme.AppTheme

@Composable
fun NativeHomeScreen(
    onNavigate: (AppDestination) -> Unit,
    onSearchMovies: (String) -> Unit = {},
    onOpenHistoryMovieDetail: (String) -> Unit = {},
    onOpenContentPreviewDetail: (String) -> Unit = {},
    onPlayMovie: (NativeMovieItem) -> Unit = {},
    initialShowHistory: Boolean = false,
    onHistoryDismissed: () -> Unit = {},
) {
    val context = LocalContext.current
    val application = context.applicationContext as MyBoxApplication
    val colors = AppTheme.colors
    val factory = remember(application) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                HomeContentPreviewViewModel(application) as T
        }
    }
    val previewViewModel: HomeContentPreviewViewModel = viewModel(factory = factory)
    val state by previewViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        previewViewModel.load(force = true)
    }

    var sourceMenuExpanded by remember { mutableStateOf(false) }

    val allMovies = state.allMovies
    val heroMovie = allMovies.firstOrNull()

    // Sub-category lists for carousels
    val movieSectionList = remember(allMovies) { allMovies.filter { it.category == "电影" } }
    val seriesSectionList = remember(allMovies) { allMovies.filter { it.category == "电视剧" } }
    val animeSectionList = remember(allMovies) { allMovies.filter { it.category == "动漫" } }
    val varietySectionList = remember(allMovies) { allMovies.filter { it.category == "综艺" } }
    val shortDramaList = remember(allMovies) { allMovies.filter { it.category == "短剧" } }

    val activeCategory = state.selectedCategoryId
    val filteredMovies = remember(allMovies, activeCategory) {
        if (activeCategory == "all") allMovies
        else allMovies.filter {
            val cat = when (activeCategory) {
                "movie" -> "电影"
                "series" -> "电视剧"
                "anime" -> "动漫"
                "variety" -> "综艺"
                "short_drama" -> "短剧"
                else -> activeCategory
            }
            it.category == cat || it.category.contains(cat)
        }.ifEmpty { allMovies }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "TVBOX 4K 影音",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "精选首页",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // MovieSourcePill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surfaceRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
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
                                        .background(colors.accent)
                                )
                                Text(
                                    text = state.sourceName?.take(6) ?: "内置源",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
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
                                        previewViewModel.selectSource(source.sourceId)
                                        sourceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Search Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceRaised,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.clickable { onNavigate(AppDestination.MOVIES) }
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Hero Banner
        if (heroMovie != null) {
            item {
                HomeHeroBanner(
                    movie = heroMovie,
                    onPlay = { onPlayMovie(heroMovie) },
                    onDetail = { onOpenContentPreviewDetail(heroMovie.rawJson) }
                )
            }
        }

        // 3. Continue Watching
        if (state.continueWatching.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "继续观看",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(state.continueWatching) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = { onPlayMovie(item.movie) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Category Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "内容专区",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "影视库全览 >",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigate(AppDestination.MOVIES) }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(state.categories) { category ->
                        val isSelected = activeCategory == category.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colors.accent else colors.surfaceRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                            modifier = Modifier.clickable { previewViewModel.selectCategory(category.id) }
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Hot Movies Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 热门精选",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${filteredMovies.size} 部内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }

                MovieThreeColumnGrid(
                    movies = filteredMovies.take(6),
                    onMovieClick = { onOpenContentPreviewDetail(it.rawJson) }
                )
            }
        }

        // 6. 院线与高清电影
        if (movieSectionList.isNotEmpty()) {
            item {
                MovieSectionRow(
                    title = "🎬 院线与高清电影",
                    actionLabel = "查看全部电影 >",
                    movies = movieSectionList.take(10),
                    onAction = { onNavigate(AppDestination.MOVIES) },
                    onMovieClick = { onOpenContentPreviewDetail(it.rawJson) }
                )
            }
        }

        // 7. 同步热播剧集
        if (seriesSectionList.isNotEmpty()) {
            item {
                MovieSectionRow(
                    title = "📺 同步热播剧集",
                    actionLabel = "查看全部剧集 >",
                    movies = seriesSectionList.take(10),
                    onAction = { onNavigate(AppDestination.MOVIES) },
                    onMovieClick = { onOpenContentPreviewDetail(it.rawJson) }
                )
            }
        }

        // 8. 动漫
        if (animeSectionList.isNotEmpty()) {
            item {
                MovieSectionRow(
                    title = "🎨 热血与国创动漫",
                    actionLabel = "查看全部动漫 >",
                    movies = animeSectionList.take(10),
                    onAction = { onNavigate(AppDestination.MOVIES) },
                    onMovieClick = { onOpenContentPreviewDetail(it.rawJson) }
                )
            }
        }

        // 9. 综艺
        if (varietySectionList.isNotEmpty()) {
            item {
                MovieSectionRow(
                    title = "🎤 欢笑综艺娱乐",
                    actionLabel = "查看全部综艺 >",
                    movies = varietySectionList.take(10),
                    onAction = { onNavigate(AppDestination.MOVIES) },
                    onMovieClick = { onOpenContentPreviewDetail(it.rawJson) }
                )
            }
        }

        // 10. 短剧
        if (shortDramaList.isNotEmpty()) {
            item {
                MovieSectionRow(
                    title = "⚡ 爆款热门短剧",
                    actionLabel = "查看全部短剧 >",
                    movies = shortDramaList.take(10),
                    onAction = { onNavigate(AppDestination.MOVIES) },
                    onMovieClick = { onOpenContentPreviewDetail(it.rawJson) }
                )
            }
        }

        // 11. 更多内容推荐
        if (filteredMovies.size > 6) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "✨ 更多内容推荐",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    MovieThreeColumnGrid(
                        movies = filteredMovies.drop(6),
                        onMovieClick = { onOpenContentPreviewDetail(it.rawJson) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeHeroBanner(
    movie: NativeMovieItem,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            val imgUrl = movie.toJson().optString("backdrop").ifBlank { movie.poster }
            NativePosterImage(
                url = imgUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.background.copy(alpha = 0.6f),
                                colors.background.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("● 今日焦点", style = MaterialTheme.typography.labelSmall, color = colors.accent, fontWeight = FontWeight.Bold)
                    Text("·", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Text(movie.category.ifBlank { "4K超清" }, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    if (movie.description.isNotBlank()) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        Text("4K超清国语", style = MaterialTheme.typography.labelSmall, color = colors.gold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (movie.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = movie.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("立即播放", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDetail,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("查看详情", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            NativePosterImage(
                url = item.movie.poster,
                modifier = Modifier
                    .width(48.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.movie.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.episodeTitle} · ${item.positionMinutes} 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MovieThreeColumnGrid(
    movies: List<NativeMovieItem>,
    onMovieClick: (NativeMovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val chunked = remember(movies) { movies.chunked(3) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { movie ->
                    Box(modifier = Modifier.weight(1f)) {
                        HomePosterCard(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MovieSectionRow(
    title: String,
    actionLabel: String,
    movies: List<NativeMovieItem>,
    onAction: () -> Unit,
    onMovieClick: (NativeMovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colors.accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(movies) { movie ->
                Box(modifier = Modifier.width(115.dp)) {
                    HomePosterCard(movie = movie, onClick = { onMovieClick(movie) })
                }
            }
        }
    }
}

@Composable
fun HomePosterCard(
    movie: NativeMovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceRaised)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
        ) {
            NativePosterImage(
                url = movie.poster,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.7f)
                        )
                    )
            )

            // Quality Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.accent)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "4K",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Description / Remarks
            val remarks = movie.toJson().optString("updateInfo").ifBlank { movie.category }
            if (remarks.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = remarks,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = movie.category.ifBlank { "影视" },
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
