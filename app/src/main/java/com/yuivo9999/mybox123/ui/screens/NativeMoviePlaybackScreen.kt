package com.yuivo9999.mybox123.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
import androidx.media3.ui.AspectRatioFrameLayout
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
import java.util.Locale

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

    var currentMovie by remember {
        mutableStateOf(
            if (movie.episodes.isNotEmpty()) movie
            else if (sourceId == "builtin" || sourceId.isBlank()) {
                StaticCatalogData.movies.firstOrNull {
                    it.title.equals(movie.title, ignoreCase = true) || it.contentId == movie.contentId
                } ?: movie
            } else movie
        )
    }

    var selectedEpisodeIndex by remember { mutableIntStateOf(0) }
    var selectedCandidateIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isDetailLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentMovie.sourceId, currentMovie.sourceItemId) {
        val effectiveSourceId = currentMovie.sourceId.ifBlank { sourceId }
        val lookupId = currentMovie.sourceItemId.ifBlank { currentMovie.contentId }
        if (currentMovie.episodes.isEmpty() && effectiveSourceId.isNotBlank() && effectiveSourceId != "builtin" && lookupId.isNotBlank()) {
            isDetailLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val full = application.awaitServices().movieSourceService.getDetail(effectiveSourceId, lookupId)
                    if (full != null && full.episodes.isNotEmpty()) {
                        currentMovie = full
                    }
                } catch (e: Exception) {
                    errorMessage = "获取剧集信息失败: ${e.message}"
                } finally {
                    isDetailLoading = false
                }
            }
        }
    }

    // Dialogs & Drawers
    var showDrawer by remember { mutableStateOf(false) }
    var showOtherSourceSearch by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var activeConsoleTab by remember { mutableStateOf("info") } // Default to "info" like Image 6!
    var isSortReversed by remember { mutableStateOf(false) }
    var isFavorited by remember { mutableStateOf(false) }

    // Media position / duration state
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentAspectRatioMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    fun formatTime(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

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
                if (direct.isNotBlank()) list.add("默认专线" to direct)
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
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
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

    // Update position ticker
    LaunchedEffect(Unit) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            }
            delay(500)
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

    // Save history periodically
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

    val episodeTitle = currentEpisode?.optString("title")?.ifBlank { "正片 IMAX" } ?: "正片 IMAX"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar (Image 6): [ ☰ ]  星际穿越  正片 IMAX  [ ⋮ ]
            Surface(
                color = colors.topBarBg,
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { showDrawer = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "主菜单", tint = colors.textPrimary)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentMovie.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = episodeTitle,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = !showMoreMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = colors.textPrimary)
                        }

                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("搜同名资源") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = { showMoreMenu = false; showOtherSourceSearch = true }
                            )
                            DropdownMenuItem(
                                text = { Text("复制当前视频直链") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("url", activeCandidateUrl))
                                    Toast.makeText(context, "已复制当前播放地址", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isFavorited) "取消收藏" else "加入收藏") },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                onClick = { showMoreMenu = false; toggleFavorite() }
                            )
                        }
                    }
                }
            }

            // 2. 16:9 Video Player Surface & In-Player Overlay (Image 6)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = currentAspectRatioMode
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top in-player bar (Image 6):
                // Left: VOD DECODE pill, 路线 ⑤ [Web ... 点播 · 主界]
                // Right: [ ⏹ 停止 ] [ ⧉ 复制 ] [ ↺ 横屏 ] [ ⧉ 比例 ] [ ⤢ 全屏 ]
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x992B221B),
                            border = BorderStroke(1.dp, Color(0xFF4B3B2E))
                        ) {
                            Text(
                                "VOD DECODE",
                                color = Color(0xFFD4C5A9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "路线 ⑤ [Web 点播 · 主界]",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Top right action buttons: [ ⏹ 停止 ] [ ⧉ 复制 ] [ ↺ 横屏 ] [ ⧉ 比例 ] [ ⤢ 全屏 ]
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                exoPlayer.stop()
                                onPlaybackFinished()
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(Color.White))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("停止", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("url", activeCandidateUrl))
                                Toast.makeText(context, "已复制地址", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("复制", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                isFullscreen = !isFullscreen
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("横屏", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                currentAspectRatioMode = if (currentAspectRatioMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL else AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("比例", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { isFullscreen = true }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("全屏", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // In-player floating badges (Image 6):
                // Left: [ 星际穿越 ] [ 正片 IMAX ] [ 线路 1 ]
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 34.dp, start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color.Black.copy(alpha = 0.6f)) {
                        Text(currentMovie.title, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color.Black.copy(alpha = 0.6f)) {
                        Text(episodeTitle, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color.Black.copy(alpha = 0.6f)) {
                        Text("线路 ${selectedCandidateIndex + 1}", color = Color(0xFFFBBF24), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }

                // Center spinner or error
                if ((isBuffering || isDetailLoading) && errorMessage == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(32.dp))
                        Text(if (isDetailLoading) "正在解析剧集线路…" else "正在缓冲…", color = Color.White, fontSize = 11.sp)
                    }
                }

                if (errorMessage != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(errorMessage ?: "", color = Color(0xFFEF4444), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("重试", fontSize = 10.sp)
                            }
                            Button(
                                onClick = { showOtherSourceSearch = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3B2E))
                            ) {
                                Text("全网搜同名", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Bottom in-player scrubber row: 00:00, slider, 00:00, [ ▶ ], [ 1x ], [ ⤢ ]
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(formatTime(currentPositionMs), color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                    Slider(
                        value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f,
                        onValueChange = { frac ->
                            if (durationMs > 0) {
                                val target = (frac * durationMs).toLong()
                                exoPlayer.seekTo(target)
                            }
                        },
                        modifier = Modifier.weight(1f).height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFF59E0B),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Text(formatTime(durationMs), color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                    IconButton(
                        onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放/暂停",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        "${playbackSpeed}x",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                2.0f -> 0.75f
                                else -> 1.0f
                            }
                        }
                    )

                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "全屏", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 3. Quick Pills Row Below Player (Image 6):
            // Left: [ - 1x + ]
            // Right: [ ✨ 00:00 / 00:00 ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Speed Stepper Pill: [ - 1x + ]
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "-",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.clickable {
                                playbackSpeed = (playbackSpeed - 0.25f).coerceAtLeast(0.5f)
                            }
                        )
                        Text(
                            "${playbackSpeed}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            "+",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.clickable {
                                playbackSpeed = (playbackSpeed + 0.25f).coerceAtMost(3.0f)
                            }
                        )
                    }
                }

                // Glow Time Pill: [ ✨ 00:00 / 00:00 ]
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✨", fontSize = 11.sp)
                        Text(
                            "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 4. Console Sheet Card (Image 6):
            // Top Tabs: [ < 返回 ]  [ ♡ 收藏 ]  [ 🔍 搜同名 ]  [ ⊞ 选集 ]  [ 📄 简介 ]
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Tabs Row (Exact tabs from Image 6!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // [ < 返回 ]
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surfaceRaised,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable { onPlaybackFinished() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("<", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Text("返回", fontSize = 11.sp, color = colors.textPrimary)
                            }
                        }

                        // [ ♡ 收藏 ]
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isFavorited) Color(0x33EF4444) else colors.surfaceRaised,
                            border = BorderStroke(1.dp, if (isFavorited) Color(0xFFEF4444) else colors.border),
                            modifier = Modifier.clickable { toggleFavorite() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(if (isFavorited) "♥" else "♡", fontSize = 11.sp, color = if (isFavorited) Color(0xFFEF4444) else colors.textPrimary)
                                Text(if (isFavorited) "已收藏" else "收藏", fontSize = 11.sp, color = colors.textPrimary)
                            }
                        }

                        // [ 🔍 搜同名 ]
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surfaceRaised,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable { showOtherSourceSearch = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("🔍", fontSize = 10.sp)
                                Text("搜同名", fontSize = 11.sp, color = colors.textPrimary)
                            }
                        }

                        // [ ⊞ 选集 ]
                        val isEpisodesTab = activeConsoleTab == "episodes"
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isEpisodesTab) Color(0xFF2B221B) else colors.surfaceRaised,
                            border = BorderStroke(1.dp, if (isEpisodesTab) Color(0xFFD4A373) else colors.border),
                            modifier = Modifier.clickable { activeConsoleTab = "episodes" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("⊞", fontSize = 11.sp, color = if (isEpisodesTab) Color(0xFFD4A373) else colors.textPrimary)
                                Text("选集", fontSize = 11.sp, fontWeight = if (isEpisodesTab) FontWeight.Bold else FontWeight.Normal, color = if (isEpisodesTab) Color(0xFFF7EFE1) else colors.textPrimary)
                            }
                        }

                        // [ 📄 简介 ] (Image 6 active state has dark filled background)
                        val isInfoTab = activeConsoleTab == "info"
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isInfoTab) Color(0xFF2B221B) else colors.surfaceRaised,
                            border = BorderStroke(1.dp, if (isInfoTab) Color(0xFFD4A373) else colors.border),
                            modifier = Modifier.clickable { activeConsoleTab = "info" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("📄", fontSize = 10.sp)
                                Text("简介", fontSize = 11.sp, fontWeight = if (isInfoTab) FontWeight.Bold else FontWeight.Normal, color = if (isInfoTab) Color(0xFFF7EFE1) else colors.textPrimary)
                            }
                        }
                    }

                    // Dotted divider line (Image 6)
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 1.dp)

                    // Tab Content
                    if (activeConsoleTab == "info") {
                        // Overview Detail (Image 6)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "OVERVIEW · 详细资料",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4A373),
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = currentMovie.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            Text(
                                text = "${currentMovie.year.ifBlank { "2014" }} · ${currentMovie.category.ifBlank { "电影" }} · ${episodeTitle}",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )

                            // Synopsis text
                            Text(
                                text = if (currentMovie.description.isNotBlank()) currentMovie.description
                                else "近未来的地球黄沙遍野，小麦、秋葵等基础农作物相继因枯萎病灭绝，人类不再需要科学家，反而需要更多农夫。前NASA宇航员库珀在机缘巧合下发现一个神秘重力异常，并由此踏上穿越虫洞寻找人类新家园的伟大星际征程。",
                                fontSize = 13.sp,
                                color = colors.textPrimary,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // CAST & CREW
                            Text(
                                text = "CAST & CREW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4A373),
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = "导演：${currentMovie.director.ifBlank { "克里斯托弗·诺兰" }}",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )

                            Text(
                                text = "主演：${if (currentMovie.actors.isNotEmpty()) currentMovie.actors.joinToString(" / ") else "马修·麦康纳 / 安妮·海瑟薇 / 杰西卡·查斯坦 / 迈克尔·凯恩"}",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        // Episodes Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("EPISODES · 选集列表", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4A373))
                                    Text("共 ${currentMovie.episodes.size} 集", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceRaised,
                                    modifier = Modifier.clickable { isSortReversed = !isSortReversed }
                                ) {
                                    Text(
                                        if (isSortReversed) "⇅ 倒序" else "⇅ 正序",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Line selector pills
                            if (candidates.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    items(candidates.size) { idx ->
                                        val isSel = idx == selectedCandidateIndex
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) colors.accent else colors.surfaceRaised,
                                            border = BorderStroke(1.dp, if (isSel) colors.accent else colors.border),
                                            modifier = Modifier.clickable { selectedCandidateIndex = idx }
                                        ) {
                                            Text(
                                                candidates[idx].first,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else colors.textPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Episode Grid
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 72.dp),
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
                                        border = BorderStroke(1.dp, if (isCurrent) colors.accent else colors.border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedEpisodeIndex = realIdx
                                                selectedCandidateIndex = 0
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
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
            onSelectTheme = { }
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
