package com.yuivo9999.mybox123.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.app.AppDestination
import com.yuivo9999.mybox123.data.movie.NativeMovieItem
import com.yuivo9999.mybox123.data.movie.StaticCatalogData
import com.yuivo9999.mybox123.data.storage.FavoriteRecord
import com.yuivo9999.mybox123.data.storage.PlaybackHistoryRecord
import com.yuivo9999.mybox123.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@OptIn(UnstableApi::class)
@Composable
fun NativeMoviePlaybackScreen(
    movie: NativeMovieItem,
    sourceId: String,
    onPlaybackFinished: () -> Unit,
    onNavigateDestination: ((AppDestination) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as MyBoxApplication
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    // Fallback if episodes is empty: check static catalog
    var currentMovie by remember {
        mutableStateOf(
            if (movie.episodes.isNotEmpty()) movie
            else {
                StaticCatalogData.movies.firstOrNull {
                    it.title.equals(movie.title, ignoreCase = true) || it.contentId == movie.contentId
                } ?: movie
            }
        )
    }

    var selectedEpisodeIndex by remember { mutableIntStateOf(0) }
    var selectedCandidateIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Dialogs & Drawers
    var showDrawer by remember { mutableStateOf(false) }
    var showOtherSourceSearch by remember { mutableStateOf(false) }
    var activeConsoleTab by remember { mutableStateOf("episodes") } // "episodes" or "info"
    var isSortReversed by remember { mutableStateOf(false) }
    var isFavorited by remember { mutableStateOf(false) }

    val currentEpisode = remember(currentMovie, selectedEpisodeIndex) {
        currentMovie.episodes.getOrNull(selectedEpisodeIndex)
    }

    val candidates = remember(currentEpisode) {
        val list = mutableListOf<Pair<String, String>>()
        if (currentEpisode != null) {
            val arr = currentEpisode.optJSONArray("candidates")
            if (arr != null && arr.length() > 0) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        val label = obj.optString("label").ifBlank { "线路 ${i + 1}" }
                        val url = obj.optString("mediaUrl").ifBlank { obj.optString("url") }
                        if (url.isNotBlank()) list.add(label to url)
                    }
                }
            }
            if (list.isEmpty()) {
                val direct = currentEpisode.optString("url").ifBlank { currentEpisode.optString("playUrl") }
                if (direct.isNotBlank()) list.add("默认线路" to direct)
            }
        }
        list
    }

    val activeCandidateUrl = remember(candidates, selectedCandidateIndex) {
        candidates.getOrNull(selectedCandidateIndex)?.second.orEmpty()
    }

    // Check favorite status
    LaunchedEffect(currentMovie) {
        withContext(Dispatchers.IO) {
            try {
                val repo = application.awaitServices().userDataRepository
                val favs = repo.getFavoriteRecords()
                val id = currentMovie.contentId.ifBlank { currentMovie.sourceItemId }
                isFavorited = favs.any { it.targetId == id }
            } catch (_: Exception) {}
        }
    }

    fun toggleFavorite() {
        scope.launch(Dispatchers.IO) {
            try {
                val repo = application.awaitServices().userDataRepository
                val favs = repo.getFavoriteRecords()
                val id = currentMovie.contentId.ifBlank { currentMovie.sourceItemId }
                if (isFavorited) {
                    val updated = favs.filterNot { it.targetId == id }
                    repo.saveFavoriteRecords(updated)
                    isFavorited = false
                } else {
                    val rec = JSONObject()
                        .put("favoriteId", "fav_movie_${System.currentTimeMillis()}")
                        .put("targetType", "movie")
                        .put("targetId", id)
                        .put("title", currentMovie.title)
                        .put("poster", currentMovie.poster)
                        .put("sourceId", sourceId)
                        .put("createdAt", System.currentTimeMillis())
                    repo.saveFavoriteRecords(favs + FavoriteRecord.fromJson(rec))
                    isFavorited = true
                }
            } catch (_: Exception) {}
        }
    }

    // Media3 ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    errorMessage = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = "视频加载失败，可尝试切换其他线路或全网搜同名"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Prepare media when URL changes
    LaunchedEffect(activeCandidateUrl) {
        if (activeCandidateUrl.isNotBlank()) {
            errorMessage = null
            try {
                val mediaItem = MediaItem.fromUri(activeCandidateUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                errorMessage = "播放初始化错误: ${e.message}"
            }
        }
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Periodic progress record save
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition / 1000L
                val dur = exoPlayer.duration / 1000L
                if (dur > 0) {
                    withContext(Dispatchers.IO) {
                        try {
                            val repo = application.awaitServices().userDataRepository
                            val existing = repo.getHistoryRecords()
                            val id = currentMovie.contentId.ifBlank { currentMovie.sourceItemId }
                            val record = JSONObject()
                                .put("historyId", "hist_${System.currentTimeMillis()}")
                                .put("targetType", "movie")
                                .put("targetId", id)
                                .put("title", currentMovie.title)
                                .put("sourceId", sourceId)
                                .put("positionSeconds", pos)
                                .put("durationSeconds", dur)
                                .put("lastPlayedAt", System.currentTimeMillis())
                                .put("poster", currentMovie.poster)
                                .put("episodeId", currentEpisode?.optString("episodeId") ?: "ep1")
                                .put("episodeTitle", currentEpisode?.optString("title") ?: "正片")
                            val updated = listOf(PlaybackHistoryRecord.fromJson(record)) + existing.filterNot { it.targetId == id }
                            repo.saveHistoryRecords(updated.take(50))
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else if (showDrawer) {
            showDrawer = false
        } else if (showOtherSourceSearch) {
            showOtherSourceSearch = false
        } else {
            onPlaybackFinished()
        }
    }

    val episodesList = remember(currentMovie.episodes, isSortReversed) {
        if (isSortReversed) currentMovie.episodes.reversed() else currentMovie.episodes
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar: SangtianTopBar
            SangtianTopBar(
                title = currentMovie.title,
                subTitle = currentEpisode?.optString("title")?.ifBlank { "第 ${selectedEpisodeIndex + 1} 集" } ?: "正片 4K",
                onHamburger = { showDrawer = true },
                onSearchSameName = { showOtherSourceSearch = true },
                onBack = onPlaybackFinished,
                currentThemeId = colors.id,
                currentMediaUrl = activeCandidateUrl,
                onSelectTheme = { themeId ->
                    // Change theme through ViewModel if applicable
                }
            )

            // 2. Video Player Block (16:9 at top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(colors.playerBg),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering && errorMessage == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(36.dp))
                            Text("正在缓冲极速流…", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(errorMessage ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        exoPlayer.prepare()
                                        exoPlayer.playWhenReady = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("重试", fontSize = 11.sp, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = { showOtherSourceSearch = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("全网搜同名", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Floating Quick Bar (SangtianFloatingBar)
            Surface(
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 选集 Quick Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeConsoleTab == "episodes") colors.accent else colors.surfaceRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (activeConsoleTab == "episodes") colors.accent else colors.border),
                            modifier = Modifier.clickable { activeConsoleTab = "episodes" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, tint = if (activeConsoleTab == "episodes") Color.White else colors.textPrimary, modifier = Modifier.size(14.dp))
                                Text("选集", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (activeConsoleTab == "episodes") Color.White else colors.textPrimary)
                            }
                        }

                        // 换源 Quick Button
                        if (candidates.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceRaised,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                modifier = Modifier.clickable {
                                    selectedCandidateIndex = (selectedCandidateIndex + 1) % candidates.size
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.accent, modifier = Modifier.size(13.dp))
                                    Text("换线 (${selectedCandidateIndex + 1}/${candidates.size})", fontSize = 11.sp, color = colors.textPrimary)
                                }
                            }
                        }

                        // Speed dropdown button
                        var speedExpanded by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceRaised,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                modifier = Modifier.clickable { speedExpanded = true }
                            ) {
                                Text("${playbackSpeed}x 倍速", fontSize = 11.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                            DropdownMenu(expanded = speedExpanded, onDismissRequest = { speedExpanded = false }) {
                                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                    DropdownMenuItem(
                                        text = { Text("${spd}x") },
                                        onClick = {
                                            playbackSpeed = spd
                                            speedExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Right Favorite Button
                    IconButton(onClick = { toggleFavorite() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorited) Color(0xFFE60012) else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 4. SangtianConsoleCard: Tabbed Episodes / Info
            Surface(
                color = colors.surface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Console Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeConsoleTab == "episodes") colors.accent else colors.surfaceRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (activeConsoleTab == "episodes") colors.accent else colors.border),
                            modifier = Modifier.clickable { activeConsoleTab = "episodes" }
                        ) {
                            Text(
                                text = "选集列表 (${currentMovie.episodes.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeConsoleTab == "episodes") Color.White else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeConsoleTab == "info") colors.accent else colors.surfaceRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (activeConsoleTab == "info") colors.accent else colors.border),
                            modifier = Modifier.clickable { activeConsoleTab = "info" }
                        ) {
                            Text(
                                text = "影片简介",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeConsoleTab == "info") Color.White else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    // Tab Content
                    if (activeConsoleTab == "episodes") {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Sub-toolbar: Candidates & Sort Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (candidates.size > 1) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(candidates.size) { idx ->
                                            val isSel = idx == selectedCandidateIndex
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) colors.accent.copy(alpha = 0.15f) else colors.surfaceRaised,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) colors.accent else colors.border),
                                                modifier = Modifier.clickable { selectedCandidateIndex = idx }
                                            ) {
                                                Text(
                                                    text = candidates[idx].first,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) colors.accent else colors.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text("正片剧集", fontSize = 12.sp, color = colors.textSecondary)
                                }

                                TextButton(
                                    onClick = { isSortReversed = !isSortReversed },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isSortReversed) "倒序" else "正序", fontSize = 11.sp, color = colors.accent)
                                }
                            }

                            // Episode Grid
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 70.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(episodesList) { idx, ep ->
                                    val realIdx = if (isSortReversed) currentMovie.episodes.size - 1 - idx else idx
                                    val isCurrent = realIdx == selectedEpisodeIndex
                                    val title = ep.optString("title").ifBlank { ep.optString("name") }.ifBlank { "${realIdx + 1}" }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) colors.accent else colors.surfaceRaised,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCurrent) colors.accent else colors.border
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedEpisodeIndex = realIdx
                                                selectedCandidateIndex = 0
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = title,
                                                fontSize = 12.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) Color.White else colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Info Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (currentMovie.poster.isNotBlank()) {
                                    AsyncImage(
                                        model = currentMovie.poster,
                                        contentDescription = currentMovie.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(88.dp)
                                            .height(124.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = currentMovie.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "分类：${currentMovie.category.ifBlank { "电影" }} · ${currentMovie.sourceCategoryName}",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = "地区/年代：${currentMovie.region.ifBlank { "中国大陆" }} · ${currentMovie.year}",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                    if (currentMovie.rating.isNotBlank()) {
                                        Text(
                                            text = "评分：★ ${currentMovie.rating}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.gold
                                        )
                                    }
                                    if (currentMovie.director.isNotBlank()) {
                                        Text(
                                            text = "导演：${currentMovie.director}",
                                            fontSize = 11.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }

                            if (currentMovie.actors.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("主要演员", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    Text(
                                        text = currentMovie.actors.joinToString(" / "),
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            if (currentMovie.description.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("剧情简介", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    Text(
                                        text = currentMovie.description,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drawer overlay
        SangtianDrawer(
            isOpen = showDrawer,
            onClose = { showDrawer = false },
            onNav = { dest ->
                onNavigateDestination?.invoke(dest)
                if (dest != AppDestination.MOVIE_PLAYBACK) {
                    onPlaybackFinished()
                }
            },
            currentThemeId = colors.id,
            onSelectTheme = { themeId ->
                // Switch theme
            }
        )

        // Other source search dialog
        if (showOtherSourceSearch) {
            OtherSourceSearchDialog(
                title = currentMovie.title,
                currentSourceId = sourceId,
                onClose = { showOtherSourceSearch = false },
                onSelectMovie = { newMovie ->
                    currentMovie = newMovie
                    selectedEpisodeIndex = 0
                    selectedCandidateIndex = 0
                    showOtherSourceSearch = false
                }
            )
        }
    }
}
