package com.yuivo9999.mybox123.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.app.AppDestination
import com.yuivo9999.mybox123.data.movie.StaticCatalogData
import com.yuivo9999.mybox123.data.storage.FavoriteRecord
import com.yuivo9999.mybox123.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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
    val factory = remember(application) { liveBrowseViewModelFactory(application) }
    val vm: LiveBrowseViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()

    // Top menu popovers & custom URL states
    var showLiveMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCustomUrl by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }

    // Current playing state & bar expansion
    var activeChannel by remember { mutableStateOf<JSONObject?>(null) }
    var activeStreams by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var activeStreamIndex by remember { mutableIntStateOf(0) }
    var isCurrentBarExpanded by remember { mutableStateOf(false) }
    var isImmersive by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showImmersiveChannelDrawer by remember { mutableStateOf(false) }

    // ExoPlayer instance for embedded and immersive live playback
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
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
                playbackError = "播放连接失败，可尝试切换其他线路或检查网络"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Prepare media when activeChannel or activeStreamIndex changes
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
            } catch (e: Exception) {
                playbackError = "流媒体解析错误: ${e.message}"
            }
        }
    }

    LaunchedEffect(activeStreams, activeStreamIndex) {
        if (activeStreams.isNotEmpty()) {
            playCurrentStream()
        }
    }

    // Function to load and play a channel
    fun selectChannelToPlay(channel: JSONObject, forceImmersive: Boolean = false) {
        activeChannel = channel
        playbackError = null
        val sourceId = channel.optString("sourceId").ifBlank { state.selectedSourceId.orEmpty() }

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
                    try {
                        val source = state.sources.firstOrNull { it.sourceId == sourceId }
                        if (source != null) {
                            application.awaitServices().liveDescriptorService.loadStreams(source, channel, forceRefresh = true)
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }

            activeStreams = streams
            activeStreamIndex = 0
            if (forceImmersive) {
                isImmersive = true
            }
        }
    }

    // Auto load sources and channels
    LaunchedEffect(vm) {
        vm.loadSources()
        vm.loadFavoriteIds()
    }

    // Default select first channel if none playing yet
    LaunchedEffect(state.channels) {
        if (activeChannel == null && state.channels.isNotEmpty()) {
            selectChannelToPlay(state.channels.first(), forceImmersive = false)
        }
    }

    LaunchedEffect(vm, favoriteNavigationJson, state.sourcesLoaded) {
        val pending = favoriteNavigationJson
        if (pending != null && state.sourcesLoaded) {
            vm.openFavoriteChannel(pending)
            onFavoriteNavigationConsumed()
        }
    }

    // Handle back button when in immersive mode
    BackHandler(enabled = isImmersive) {
        if (showImmersiveChannelDrawer) {
            showImmersiveChannelDrawer = false
        } else {
            isImmersive = false
        }
    }

    val selectedSource = state.sources.firstOrNull { it.sourceId == state.selectedSourceId }
    val categoriesList = remember(state.channels) {
        listOf("全部") + state.channels.map { it.optString("category").trim().ifBlank { "未分类" } }.distinct()
    }
    var selectedCategoryTab by remember { mutableStateOf("全部") }

    val filteredChannels = remember(state.channels, selectedCategoryTab, state.keyword) {
        val kw = state.keyword.trim().lowercase()
        state.channels.filter { ch ->
            val cat = ch.optString("category").trim().ifBlank { "未分类" }
            (selectedCategoryTab == "全部" || cat == selectedCategoryTab) &&
                (kw.isBlank() || ch.optString("name").lowercase().contains(kw) || cat.lowercase().contains(kw))
        }
    }

    val activeStream = activeStreams.getOrNull(activeStreamIndex)
    val activeUrl = activeStream?.optString("url")?.ifBlank { activeStream?.optString("playUrl") }.orEmpty()

    // If in full immersive playback mode:
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

            // Top immersive OSD header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { isImmersive = false },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出沉浸模式", tint = Color.White)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE60012))
                            )
                            Text(
                                text = activeChannel?.optString("name") ?: "电视直播",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${activeChannel?.optString("category") ?: "直播"} · ${activeStream?.optString("name") ?: activeStream?.optString("label") ?: "高清线路"}",
                            fontSize = 11.sp,
                            color = Color(0xFFD4C5A9)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showImmersiveChannelDrawer = !showImmersiveChannelDrawer },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("换台", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            // Quick channel switcher overlay on right side
            if (showImmersiveChannelDrawer) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(260.dp),
                    color = Color(0xEE161310),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4B3B2E))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("快速换台", fontWeight = FontWeight.Bold, color = Color(0xFFF7EFE1), fontSize = 15.sp)
                            IconButton(onClick = { showImmersiveChannelDrawer = false }) {
                                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color(0xFFD6CBBE))
                            }
                        }
                        HorizontalDivider(color = Color(0xFF392C23), modifier = Modifier.padding(vertical = 8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredChannels) { ch ->
                                val isCurrent = ch.optString("channelId") == activeChannel?.optString("channelId")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) Color(0xFF392C23) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectChannelToPlay(ch, forceImmersive = true)
                                            showImmersiveChannelDrawer = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isCurrent) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE60012)))
                                        }
                                        Column {
                                            Text(ch.optString("name"), fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) Color(0xFFF7EFE1) else Color(0xFFD6CBBE), fontSize = 13.sp)
                                            Text(ch.optString("currentProgram").ifBlank { ch.optString("category") }, fontSize = 10.sp, color = Color(0xFF8E97A8))
                                        }
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

    // Main Live Browse Screen (Embedded layout strictly matching LiveFeature.jsx!)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. Live Home TopBar
        Surface(
            color = colors.topBarBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left hamburger icon
                Box {
                    IconButton(onClick = { showLiveMenu = !showLiveMenu; showMoreMenu = false }) {
                        Icon(Icons.Default.Menu, contentDescription = "打开主导航菜单", tint = colors.textPrimary)
                    }

                    DropdownMenu(
                        expanded = showLiveMenu,
                        onDismissRequest = { showLiveMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("首页") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            onClick = {
                                showLiveMenu = false
                                onNavigateTab?.invoke(AppDestination.HOME)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("收藏") },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                            onClick = {
                                showLiveMenu = false
                                onNavigateTab?.invoke(AppDestination.FAVORITES)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("我的与源管理") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showLiveMenu = false
                                onNavigateTab?.invoke(AppDestination.PROFILE)
                            }
                        )
                    }
                }

                // Section Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE60012))
                        )
                        Text(
                            text = "直播",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                // Current Title & Status
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            text = activeChannel?.optString("name") ?: "电视直播",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeChannel != null) "${activeChannel?.optString("category") ?: "直播"} · 正在播放" else "选择频道开始观看",
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Network chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                        Text("在线", fontSize = 10.sp, color = colors.textSecondary)
                    }
                }

                // More Menu Button
                Box {
                    IconButton(onClick = { showMoreMenu = !showMoreMenu; showLiveMenu = false }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多快捷操作", tint = colors.textPrimary)
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("输入自定义直播地址") },
                            leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                showCustomUrl = !showCustomUrl
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("管理直播源") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onNavigateTab?.invoke(AppDestination.PROFILE)
                            }
                        )
                    }
                }
            }
        }

        // Custom URL input drawer (collapsible)
        AnimatedVisibility(
            visible = showCustomUrl,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                color = colors.surfaceRaised,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("自定义直播地址", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary)
                        Text("支持 HTTP / HTTPS / RTMP / M3U8", fontSize = 10.sp, color = colors.textMuted)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            placeholder = { Text("粘贴 m3u8、flv 或直播流地址", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        Button(
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    val customChannel = JSONObject()
                                        .put("channelId", "custom_${System.currentTimeMillis()}")
                                        .put("name", "自定义直播流")
                                        .put("category", "自定义")
                                        .put("streams", JSONArray().put(JSONObject().put("url", customUrlInput.trim()).put("label", "自定义专线")))
                                    selectChannelToPlay(customChannel)
                                    showCustomUrl = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("加载", fontSize = 12.sp)
                        }
                        IconButton(onClick = { showCustomUrl = false }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.textSecondary)
                        }
                    }
                }
            }
        }

        // 2. Embedded 16:9 Video Player (Positioned at top of screen exactly like zip!)
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
                        useController = false // Custom overlay
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Buffering spinner
            if (isBuffering && playbackError == null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(32.dp))
                        Text("正在连接频道线路…", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // Error notice
            if (playbackError != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(playbackError ?: "", color = Color(0xFFEF4444), fontSize = 12.sp, maxLines = 2)
                        Button(
                            onClick = { playCurrentStream() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("重新连接", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // In-player top/bottom overlay controls
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top tag: LIVE · Channel Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LIVE · ${activeChannel?.optString("name") ?: "等待频道"}",
                            color = Color(0xFFF7EFE1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Fullscreen / Immersive Expand Icon
                    IconButton(
                        onClick = { isImmersive = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "全屏沉浸播放", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // Bottom bar inside player
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "播放暂停",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (activeStreams.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.clickable {
                                    activeStreamIndex = (activeStreamIndex + 1) % activeStreams.size
                                }
                            ) {
                                Text(
                                    text = "切线 (${activeStreamIndex + 1}/${activeStreams.size})",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF7EFE1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Immersive button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.accent,
                        modifier = Modifier.clickable { isImmersive = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("沉浸模式", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 3. live-current-bar (当前直播与沉浸播放入口)
        Surface(
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (activeChannel != null) "● 正在直播" else "● 信号待接入",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeChannel != null) Color(0xFF10B981) else colors.textMuted
                            )
                            Text(
                                text = activeChannel?.optString("name") ?: "尚未选择频道",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (activeChannel != null)
                                "${activeChannel?.optString("category") ?: "未分类"} · ${activeStream?.optString("name") ?: activeStream?.optString("label") ?: "默认线路"} · ${activeChannel?.optString("currentProgram")?.ifBlank { "实时电视广播" } ?: "实时电视广播"}"
                            else "从频道列表选择一个频道，播放器与沉浸播放入口会保持在此页面。",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Favorite channel button
                        if (activeChannel != null) {
                            val isFav = vm.isFavorite(activeChannel!!)
                            IconButton(
                                onClick = { vm.addFavorite(activeChannel!!) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏频道",
                                    tint = if (isFav) Color(0xFFE60012) else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Immersive Play Button
                        Button(
                            onClick = { isImmersive = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("沉浸播放", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Expand Details Chevron
                        IconButton(
                            onClick = { isCurrentBarExpanded = !isCurrentBarExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isCurrentBarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "展开线路与播放地址",
                                tint = colors.textSecondary
                            )
                        }
                    }
                }

                // Expanded stream switcher & raw URL
                AnimatedVisibility(visible = isCurrentBarExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeStreams.isNotEmpty()) {
                            Text("线路 (${activeStreams.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(activeStreams.size) { idx ->
                                    val isSelected = idx == activeStreamIndex
                                    val stream = activeStreams[idx]
                                    val label = stream.optString("name").ifBlank { stream.optString("label") }.ifBlank { "线路 ${idx + 1}" }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) colors.accent else colors.surfaceRaised,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                        modifier = Modifier.clickable { activeStreamIndex = idx }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else colors.textPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (activeUrl.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("播放地址:", fontSize = 10.sp, color = colors.textMuted)
                                Surface(
                                    color = colors.surfaceRaised,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = activeUrl,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. CHANNEL GUIDE (频道列表)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Heading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CHANNEL GUIDE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.accent, letterSpacing = 1.sp)
                    Text("频道列表", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Text("${state.channels.size} 个频道", fontSize = 11.sp, color = colors.textMuted)
            }

            // Category Tabs
            if (categoriesList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categoriesList) { cat ->
                        val isSelected = cat == selectedCategoryTab
                        val count = if (cat == "全部") state.channels.size else state.channels.count { it.optString("category").trim().ifBlank { "未分类" } == cat }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colors.accent else colors.surfaceRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                            modifier = Modifier.clickable { selectedCategoryTab = cat }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
                }
            }

            // Channel cards list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredChannels, key = { it.optString("channelId").ifBlank { it.optString("name") } }) { channel ->
                    val isCurrent = channel.optString("channelId") == activeChannel?.optString("channelId")
                    val isFav = vm.isFavorite(channel)
                    val prog = channel.optString("currentProgram").ifBlank { channel.optString("category").ifBlank { "电视广播" } }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) colors.surfaceRaised else colors.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrent) colors.accent else colors.border.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectChannelToPlay(channel) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Logo badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) colors.accent.copy(alpha = 0.2f) else colors.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = if (isCurrent) colors.accent else colors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Title & Subtitle
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = channel.optString("name"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE60012)
                                        ) {
                                            Text(
                                                text = "直播中",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = prog,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Favorite Button
                            IconButton(
                                onClick = { vm.addFavorite(channel) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (isFav) Color(0xFFE60012) else colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Play Immersive Button
                            IconButton(
                                onClick = { selectChannelToPlay(channel, forceImmersive = true) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "沉浸播放",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun liveBrowseViewModelFactory(application: MyBoxApplication): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LiveBrowseViewModel::class.java)) return LiveBrowseViewModel(application) as T
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
