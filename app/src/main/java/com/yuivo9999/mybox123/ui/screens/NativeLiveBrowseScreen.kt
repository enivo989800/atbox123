package com.yuivo9999.mybox123.ui.screens

import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.app.AppDestination
import com.yuivo9999.mybox123.data.storage.FavoriteRecord
import com.yuivo9999.mybox123.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun NativeLiveBrowseScreen(
    modifier: Modifier = Modifier,
    favoriteNavigationJson: String? = null,
    onFavoriteNavigationConsumed: () -> Unit = {},
    onNavigateTab: ((AppDestination) -> Unit)? = null,
) {
    val context = LocalContext.current
    val application = context.applicationContext as MyBoxApplication
    val vm: LiveBrowseViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                LiveBrowseViewModel(application) as T
        }
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()

    // Menus
    var showLiveMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCustomUrl by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }

    // Channel selection and player states
    var activeChannel by remember { mutableStateOf<JSONObject?>(null) }
    var activeStreams by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var activeStreamIndex by remember { mutableIntStateOf(0) }
    var isPlayingChannel by remember { mutableStateOf(false) }
    var isImmersive by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showChannelSelectorDialog by remember { mutableStateOf(false) }
    var showLineSelectorDialog by remember { mutableStateOf(false) }

    // Stats and Collapsible States (for Image 2 portrait playback)
    var showStats by remember { mutableStateOf(true) }
    var showShortcuts by remember { mutableStateOf(true) }
    var watchedSeconds by remember { mutableLongStateOf(0L) }
    var currentDateString by remember { mutableStateOf("") }
    var currentTimeString by remember { mutableStateOf("") }

    // Audio & Aspect Ratio
    var isMuted by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(0.7f) }
    var currentAspectRatioMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var aspectRatioName by remember { mutableStateOf("16:9") }

    // Favorites
    var favoriteChannelIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Aspect ratio toggle
    fun toggleAspectRatio() {
        when (currentAspectRatioMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                currentAspectRatioMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                aspectRatioName = "铺满"
            }
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> {
                currentAspectRatioMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                aspectRatioName = "裁剪"
            }
            else -> {
                currentAspectRatioMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                aspectRatioName = "16:9"
            }
        }
    }

    // Clock and watch timer update loop
    LaunchedEffect(isPlayingChannel) {
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
        while (true) {
            val now = Date()
            currentDateString = dateFormat.format(now)
            currentTimeString = timeFormat.format(now)
            if (isPlayingChannel && exoPlayer.isPlaying) {
                watchedSeconds++
            }
            delay(1000)
        }
    }

    // Favorite IDs loader
    fun refreshFavorites() {
        scope.launch(Dispatchers.IO) {
            try {
                val repo = application.awaitServices().userDataRepository
                val favs = repo.getFavoriteRecords()
                favoriteChannelIds = favs.filter { it.targetType == "live" }.mapNotNull { it.targetId?.toString() }.toSet()
            } catch (_: Exception) {}
        }
    }

    fun toggleFavorite(channel: JSONObject) {
        scope.launch(Dispatchers.IO) {
            try {
                val repo = application.awaitServices().userDataRepository
                val favs = repo.getFavoriteRecords()
                val chId = channel.optString("channelId")
                val isFav = favoriteChannelIds.contains(chId)
                if (isFav) {
                    val updated = favs.filterNot { it.targetType == "live" && it.targetId == chId }
                    repo.saveFavoriteRecords(updated)
                } else {
                    val rec = JSONObject()
                        .put("favoriteId", "fav_live_${System.currentTimeMillis()}")
                        .put("targetType", "live")
                        .put("targetId", chId)
                        .put("title", channel.optString("name"))
                        .put("poster", channel.optString("logo"))
                        .put("sourceId", channel.optString("sourceId"))
                        .put("createdAt", System.currentTimeMillis())
                    repo.saveFavoriteRecords(favs + FavoriteRecord.fromJson(rec))
                }
                refreshFavorites()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = "直播连接失败，可尝试切换其他线路"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Play stream
    fun playCurrentStream() {
        playbackError = null
        val stream = activeStreams.getOrNull(activeStreamIndex)
        val url = stream?.optString("url")?.ifBlank { stream.optString("playUrl") }?.trim().orEmpty()
        if (url.isNotBlank()) {
            try {
                val mediaItem = MediaItem.fromUri(url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                isPlayingChannel = true
            } catch (e: Exception) {
                playbackError = "流媒体解析错误: ${e.message}"
            }
        }
    }

    LaunchedEffect(activeStreams, activeStreamIndex) {
        if (activeStreams.isNotEmpty() && isPlayingChannel) {
            playCurrentStream()
        }
    }

    // Channel selection handler
    fun selectChannelToPlay(channel: JSONObject, forceImmersive: Boolean = false) {
        activeChannel = channel
        playbackError = null
        isPlayingChannel = true
        watchedSeconds = 0L

        scope.launch {
            val streams = withContext(Dispatchers.IO) {
                val rawStreams = channel.optJSONArray("streams")
                if (rawStreams != null && rawStreams.length() > 0) {
                    buildList {
                        for (i in 0 until rawStreams.length()) {
                            rawStreams.optJSONObject(i)?.let { add(it) }
                        }
                    }
                } else {
                    val fallbackUrl = channel.optString("url")
                    if (fallbackUrl.isNotBlank()) {
                        listOf(JSONObject().put("name", "默认专线").put("url", fallbackUrl))
                    } else emptyList()
                }
            }

            activeStreams = streams
            activeStreamIndex = 0
            if (forceImmersive) {
                isImmersive = true
            }
        }
    }

    LaunchedEffect(vm) {
        vm.loadSources()
        refreshFavorites()
    }

    LaunchedEffect(vm, favoriteNavigationJson, state.sourcesLoaded) {
        val pending = favoriteNavigationJson
        if (pending != null && state.sourcesLoaded) {
            vm.openFavoriteChannel(pending)
            onFavoriteNavigationConsumed()
        }
    }

    // Back handler
    BackHandler(enabled = isImmersive || isPlayingChannel) {
        if (isImmersive) {
            isImmersive = false
        } else if (isPlayingChannel) {
            isPlayingChannel = false
            exoPlayer.stop()
        }
    }

    val channels = state.channels
    var selectedCategoryTab by remember { mutableStateOf("全部") }
    val categories = remember(channels) {
        val list = channels.map { it.optString("category").trim().ifBlank { "央视" } }.distinct()
        if (list.isEmpty()) listOf("央视", "卫视", "全部")
        else listOf("央视", "卫视", "全部")
    }

    val filteredChannels = remember(channels, selectedCategoryTab) {
        if (selectedCategoryTab == "全部") channels
        else channels.filter { it.optString("category").trim() == selectedCategoryTab }
    }

    val currentChannelIndex = remember(channels, activeChannel) {
        channels.indexOfFirst { it.optString("channelId") == activeChannel?.optString("channelId") }
    }

    fun playPreviousChannel() {
        if (channels.isEmpty()) return
        val prevIndex = if (currentChannelIndex > 0) currentChannelIndex - 1 else channels.size - 1
        selectChannelToPlay(channels[prevIndex], forceImmersive = isImmersive)
    }

    fun playNextChannel() {
        if (channels.isEmpty()) return
        val nextIndex = if (currentChannelIndex >= 0 && currentChannelIndex < channels.size - 1) currentChannelIndex + 1 else 0
        selectChannelToPlay(channels[nextIndex], forceImmersive = isImmersive)
    }

    // Format seconds to HH:mm:ss
    val formattedDuration = remember(watchedSeconds) {
        val hours = watchedSeconds / 3600
        val mins = (watchedSeconds % 3600) / 60
        val secs = watchedSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
    }

    // -------------------------------------------------------------
    // SCREEN VARIANT 1: Immersive / Fullscreen Mode (Image 1)
    // -------------------------------------------------------------
    if (isImmersive) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
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

            // Top immersive bar (Dropdown pill)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: [ 选台 CCTV-1 综合 v ]
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                    modifier = Modifier.clickable { showChannelSelectorDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = "选台  ${activeChannel?.optString("name") ?: "CCTV-1 综合"}",
                            color = Color(0xFFF7EFE1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Row 2: [ 线路 ] [ 播放配置 ] [ 比例 ] [ 静音 ] [ 音量滑条 ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable { showLineSelectorDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("线路", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable { showMoreMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("播放配置", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable { toggleAspectRatio() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("比例", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else volumeLevel
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(if (isMuted) "恢复" else "静音", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Volume slider indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Slider(
                                value = volumeLevel,
                                onValueChange = {
                                    volumeLevel = it
                                    if (!isMuted) exoPlayer.volume = it
                                },
                                modifier = Modifier.weight(1f).height(20.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD4A373),
                                    activeTrackColor = Color(0xFFD4A373),
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }

                // Row 3: 播放导航: [ ▶ 播放 ] [ ⏸ 暂停 ] [ ■ 停止 ] [ ↗ 退出 ] [ 🔄 刷新 ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("播放导航", color = Color(0xFFD4C5A9), fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x992B221B),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable { exoPlayer.play() }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("播放", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x992B221B),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable { exoPlayer.pause() }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("暂停", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x992B221B),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable {
                            exoPlayer.stop()
                            isImmersive = false
                            isPlayingChannel = false
                        }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("停止", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x996B2121),
                        border = BorderStroke(1.dp, Color(0xFF993B3B)),
                        modifier = Modifier.clickable { isImmersive = false }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NorthEast, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("退出", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x992B221B),
                        border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                        modifier = Modifier.clickable { playCurrentStream() }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("刷新", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Center large circular play/pause button (as seen in Image 1)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)), CircleShape)
                    .clickable {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "播放/暂停",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Bottom pill dock (Image 1 bottom dock)
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color(0xEE1A1F26),
                border = BorderStroke(1.dp, Color(0xFF333D4B)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Line switcher pill: < 线路 1 / 2 >
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF4E3D2A)),
                        modifier = Modifier.clickable {
                            if (activeStreams.isNotEmpty()) {
                                activeStreamIndex = (activeStreamIndex + 1) % activeStreams.size
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("<", color = Color(0xFFE2C48E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "线路 ${activeStreamIndex + 1} / ${maxOf(activeStreams.size, 1)}",
                                color = Color(0xFFF7EFE1),
                                fontSize = 11.sp
                            )
                            Text(">", color = Color(0xFFE2C48E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Channel Name
                    Text(
                        text = (activeChannel?.optString("name") ?: "CCTV-1 综合").take(10) + "…",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Live green dot badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x3310B981),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Text("直播", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Time
                    Text(
                        text = if (currentTimeString.length >= 5) currentTimeString.substring(0, 5) else "19:32",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        return
    }

    // -------------------------------------------------------------
    // SCREEN VARIANT 2: Portrait Playing Mode (Image 2)
    // -------------------------------------------------------------
    if (isPlayingChannel && activeChannel != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // Top Bar: < icon, thumbnail + Channel Name + category, ● 直播中 badge
            Surface(
                color = colors.topBarBg,
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isPlayingChannel = false
                                exoPlayer.stop()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.textPrimary)
                        }

                        // Channel logo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceRaised,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                            }
                        }

                        Column {
                            Text(
                                text = activeChannel?.optString("name") ?: "电视直播",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = activeChannel?.optString("category") ?: "央视",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Red badge: ● 直播中
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33EF4444),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Text("直播中", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // In-page 16:9 Player container with 播放导航 strip (as seen in Image 2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                // Strip: 播放导航 on left, [ ■ 停止 ] [ 🔄 刷新 ] on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("播放导航", color = Color(0xFFD4C5A9), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2A231C),
                            border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                            modifier = Modifier.clickable {
                                isPlayingChannel = false
                                exoPlayer.stop()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(Color.White))
                                Text("停止", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2A231C),
                            border = BorderStroke(1.dp, Color(0xFF4B3B2E)),
                            modifier = Modifier.clickable { playCurrentStream() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Text("刷新", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 16:9 Video view
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

                    if (isBuffering && playbackError == null) {
                        CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(36.dp))
                    }

                    if (playbackError != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(playbackError ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                            Button(
                                onClick = { playCurrentStream() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("重试", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Below player controls and cards (Image 2)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: 观看统计
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("观看统计", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surfaceRaised,
                                modifier = Modifier.clickable { showStats = !showStats }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (showStats) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(if (showStats) "隐藏统计" else "显示统计", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                        }

                        if (showStats) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 本次观看
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.surfaceRaised,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text("本次观看", fontSize = 11.sp, color = colors.textSecondary)
                                        Text(formattedDuration, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                // 网络速度 (估算)
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.surfaceRaised,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text("网络速度 (估算)", fontSize = 11.sp, color = colors.textSecondary)
                                        Text("暂不可用", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Row: Date & Time (Image 2)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                        Text(currentDateString.ifBlank { "2026年10月29日星期四" }, fontSize = 12.sp, color = colors.textSecondary)
                    }
                    Text(currentTimeString.ifBlank { "19:32:03" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                }

                // Row: Quick Player Action Buttons (Image 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Blue pill: [ ▶ ● 正在缓冲 / 播放中 ]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2563EB),
                        modifier = Modifier
                            .weight(1.3f)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFBBF24)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isBuffering) "正在缓冲" else if (exoPlayer.isPlaying) "播放中" else "已暂停",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Volume
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else volumeLevel
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Aspect ratio: [ ⧉ 比例 ]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { toggleAspectRatio() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("比例", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Fullscreen: [ ⤢ 全屏 ]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isImmersive = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("全屏", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Card 2: 快捷操作 (Image 2)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("快捷操作", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surfaceRaised,
                                modifier = Modifier.clickable { showShortcuts = !showShortcuts }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (showShortcuts) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(if (showShortcuts) "收起" else "展开", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                        }

                        if (showShortcuts) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // 2x2 Grid:
                            // Row 1: 线路选择 > | 选台 >
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.surfaceRaised,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showLineSelectorDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.AltRoute, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("线路选择", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                                Text("${activeStreamIndex + 1} / ${maxOf(activeStreams.size, 1)} 条线路", fontSize = 10.sp, color = colors.textSecondary)
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(12.dp))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.surfaceRaised,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showChannelSelectorDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Tv, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("选台", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                                Text("搜索并切换频道", fontSize = 10.sp, color = colors.textSecondary)
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Row 2: < 上一台 | 下一台 >
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.surfaceRaised,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { playPreviousChannel() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("< 上一台", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.surfaceRaised,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { playNextChannel() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("下一台 >", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // -------------------------------------------------------------
    // SCREEN VARIANT 3: Idle / Main Channel Guide Screen (Image 3)
    // -------------------------------------------------------------
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Bar (Image 3): [ ☰ ]  ● 直播  📺 电视直播 / 选择频道开始观看  📶 在线  [ ⋮ ]
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
                // Hamburger icon
                IconButton(onClick = { showLiveMenu = !showLiveMenu }) {
                    Icon(Icons.Default.Menu, contentDescription = "主菜单", tint = colors.textPrimary)
                }

                // Green dot pill: ● 直播
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceRaised,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Text("直播", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }

                // Middle box: 📺 电视直播 / 选择频道开始观看
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceRaised,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Column {
                            Text("电视直播", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text("选择频道开始观看", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }
                }

                // Right online signal chip: 📶 在线
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceRaised,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                        Text("在线", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }

                // Overflow more icon
                IconButton(onClick = { showMoreMenu = !showMoreMenu }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = colors.textPrimary)
                }
            }
        }

        // Dropdown menus
        DropdownMenu(expanded = showLiveMenu, onDismissRequest = { showLiveMenu = false }) {
            DropdownMenuItem(
                text = { Text("首页") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                onClick = { showLiveMenu = false; onNavigateTab?.invoke(AppDestination.HOME) }
            )
            DropdownMenuItem(
                text = { Text("影视") },
                leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
                onClick = { showLiveMenu = false; onNavigateTab?.invoke(AppDestination.MOVIES) }
            )
            DropdownMenuItem(
                text = { Text("收藏") },
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                onClick = { showLiveMenu = false; onNavigateTab?.invoke(AppDestination.FAVORITES) }
            )
            DropdownMenuItem(
                text = { Text("我的") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                onClick = { showLiveMenu = false; onNavigateTab?.invoke(AppDestination.PROFILE) }
            )
        }

        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
            DropdownMenuItem(
                text = { Text("自定义直播地址") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                onClick = { showMoreMenu = false; showCustomUrl = true }
            )
            DropdownMenuItem(
                text = { Text("管理源配置") },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                onClick = { showMoreMenu = false; onNavigateTab?.invoke(AppDestination.PROFILE) }
            )
        }

        // Custom URL input drawer (collapsible)
        AnimatedVisibility(visible = showCustomUrl) {
            Surface(
                color = colors.card,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("自定义直播地址", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            placeholder = { Text("粘贴 m3u8 或直播流地址", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    val customChannel = JSONObject()
                                        .put("channelId", "custom_${System.currentTimeMillis()}")
                                        .put("name", "自定义直播流")
                                        .put("category", "自定义")
                                        .put("streams", JSONArray().put(JSONObject().put("url", customUrlInput.trim()).put("name", "自定义专线")))
                                    selectChannelToPlay(customChannel)
                                    showCustomUrl = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text("加载", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Main content in scrollable column
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Idle Player Container (Image 3)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E282D),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Subtle abstract geometric background (as in Image 3)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF19252A), Color(0xFF283A42), Color(0xFF152024))
                                    )
                                )
                        )

                        // Top right: [ ⧉ 比例 ] [ ⤢ 全屏 ]
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { toggleAspectRatio() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text("比例", color = Color.White, fontSize = 11.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { isImmersive = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text("全屏", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        // Top left: 播放导航: [ ▶ 播放 ] [ ⏸ 暂停 ] [ ■ 停止 ] [ 🔄 刷新 ]
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("播放导航", color = Color(0xFFD4C5A9), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable {
                                    if (activeChannel != null) playCurrentStream()
                                    else if (channels.isNotEmpty()) selectChannelToPlay(channels.first())
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("播放", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { exoPlayer.pause() }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("暂停", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { exoPlayer.stop() }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(Color.White))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("停止", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { playCurrentStream() }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("刷新", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        // Center: [ 请从下方选择频道开始观看 ]
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "请从下方选择频道开始观看",
                                color = Color(0xFFF7EFE1),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 2. Middle Info Card (Image 3)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                // Green / Muted badge: ● 信号待接入
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x3310B981)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                        Text("信号待接入", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = activeChannel?.optString("name") ?: "尚未选择频道",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "从频道列表选择一个频道，播放器与沉浸播放入口会保持在此页面。",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 16.sp
                                )
                            }

                            // Right: [ ▷ 沉浸播放 ] and down arrow [ v ]
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceRaised,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.clickable {
                                        if (channels.isNotEmpty()) {
                                            val target = activeChannel ?: channels.first()
                                            selectChannelToPlay(target, forceImmersive = true)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                        Text("沉浸播放", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceRaised,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.clickable { showLineSelectorDialog = true }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CHANNEL GUIDE Header & Category Chips (Image 3)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CHANNEL GUIDE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "频道列表",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceRaised,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = "${channels.size} 个频道",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text("正在读取: 0", fontSize = 11.sp, color = colors.textMuted)
                        }
                    }

                    // Chips row: [ 📺 央视 ]  [ ⭐ 卫视 ]  [ ⊙ 全部 6 ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 央视
                        val isCctv = selectedCategoryTab == "央视"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCctv) colors.accent else colors.card,
                            border = BorderStroke(1.dp, if (isCctv) colors.accent else colors.border),
                            modifier = Modifier.clickable { selectedCategoryTab = "央视" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = if (isCctv) Color.White else colors.textPrimary, modifier = Modifier.size(16.dp))
                                Text("央视", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCctv) Color.White else colors.textPrimary)
                            }
                        }

                        // 卫视
                        val isWeishi = selectedCategoryTab == "卫视"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isWeishi) colors.accent else colors.card,
                            border = BorderStroke(1.dp, if (isWeishi) colors.accent else colors.border),
                            modifier = Modifier.clickable { selectedCategoryTab = "卫视" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.StarOutline, contentDescription = null, tint = if (isWeishi) Color.White else colors.textPrimary, modifier = Modifier.size(16.dp))
                                Text("卫视", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isWeishi) Color.White else colors.textPrimary)
                            }
                        }

                        // 全部
                        val isAll = selectedCategoryTab == "全部"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAll) colors.accent else colors.card,
                            border = BorderStroke(1.dp, if (isAll) colors.accent else colors.border),
                            modifier = Modifier.clickable { selectedCategoryTab = "全部" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).border(1.dp, if (isAll) Color.White else colors.textPrimary, CircleShape))
                                Text("全部 ${channels.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isAll) Color.White else colors.textPrimary)
                            }
                        }
                    }
                }
            }

            // 4. Channel Items List (Image 3)
            items(filteredChannels, key = { it.optString("channelId") }) { ch ->
                val chId = ch.optString("channelId")
                val isFav = favoriteChannelIds.contains(chId)
                val isCurrent = activeChannel?.optString("channelId") == chId

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, if (isCurrent) colors.accent else colors.border.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectChannelToPlay(ch) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Channel Logo White Badge: CCTV 1 + 中央电视台 (as in Image 3)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                modifier = Modifier.width(68.dp).height(42.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val name = ch.optString("name")
                                    val shortCode = if (name.contains("CCTV-")) {
                                        name.substringBefore(" ").replace("-", " ")
                                    } else name.take(4)

                                    Text(
                                        text = shortCode,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (shortCode.contains("CCTV")) Color(0xFFE60012) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (ch.optString("category") == "央视") "中央电视台" else "卫视频道",
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ch.optString("name"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ch.optString("currentProgram").ifBlank { "精彩直播内容" },
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right actions: ♡, Play circle [ ▶ ], Arrow > (Image 3)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { toggleFavorite(ch) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (isFav) Color(0xFFEF4444) else colors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Circular Play Button
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceRaised,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { selectChannelToPlay(ch) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "播放",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Channel Selector Dialog
    if (showChannelSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showChannelSelectorDialog = false },
            title = { Text("选择直播频道", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(channels) { ch ->
                        val isCurr = ch.optString("channelId") == activeChannel?.optString("channelId")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurr) colors.accent.copy(alpha = 0.2f) else colors.surfaceRaised,
                            border = BorderStroke(1.dp, if (isCurr) colors.accent else colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectChannelToPlay(ch, forceImmersive = isImmersive)
                                    showChannelSelectorDialog = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ch.optString("name"), fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal, color = colors.textPrimary)
                                Text(ch.optString("category"), fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelSelectorDialog = false }) { Text("关闭", color = colors.accent) }
            }
        )
    }

    // Line Selector Dialog
    if (showLineSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showLineSelectorDialog = false },
            title = { Text("切换播放线路", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeStreams.isEmpty()) {
                        Text("当前频道仅有 1 条默认线路", color = colors.textSecondary, fontSize = 13.sp)
                    } else {
                        activeStreams.forEachIndexed { idx, st ->
                            val isCurr = idx == activeStreamIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurr) colors.accent else colors.surfaceRaised,
                                border = BorderStroke(1.dp, if (isCurr) colors.accent else colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeStreamIndex = idx
                                        showLineSelectorDialog = false
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(st.optString("name").ifBlank { "线路 ${idx + 1}" }, color = if (isCurr) Color.White else colors.textPrimary, fontWeight = FontWeight.Bold)
                                    if (isCurr) Text("当前使用", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLineSelectorDialog = false }) { Text("关闭", color = colors.accent) }
            }
        )
    }
}
