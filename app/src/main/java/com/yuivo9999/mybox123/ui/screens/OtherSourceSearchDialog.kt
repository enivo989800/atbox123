package com.yuivo9999.mybox123.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.data.movie.NativeMovieItem
import com.yuivo9999.mybox123.data.source.NativeContentSource
import com.yuivo9999.mybox123.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SourceSearchResultGroup(
    val sourceId: String,
    val sourceName: String,
    val items: List<NativeMovieItem>
)

@Composable
fun OtherSourceSearchDialog(
    title: String,
    currentSourceId: String,
    onClose: () -> Unit,
    onSelectMovie: (NativeMovieItem) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyBoxApplication
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    var loading by remember { mutableStateOf(true) }
    var resultGroups by remember { mutableStateOf<List<SourceSearchResultGroup>>(emptyList()) }
    var failedSourcesCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(title) {
        scope.launch {
            loading = true
            withContext(Dispatchers.IO) {
                try {
                    val services = app.awaitServices()
                    val sources = services.contentSourceRepository.getAll()
                        .filter { it.sourceType == "movie" && it.enabled && it.sourceId != currentSourceId }

                    val groups = mutableListOf<SourceSearchResultGroup>()
                    var fails = 0

                    for (src in sources.take(8)) {
                        try {
                            val page = services.movieSourceService.search(src.sourceId, title)
                            val items = page.items
                            if (items.isNotEmpty()) {
                                groups.add(
                                    SourceSearchResultGroup(
                                        sourceId = src.sourceId,
                                        sourceName = src.name,
                                        items = items.take(4)
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            fails++
                        }
                    }
                    resultGroups = groups
                    failedSourcesCount = fails
                } catch (_: Exception) {
                } finally {
                    loading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clickable(enabled = false) {},
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                            Text(
                                text = "全网搜同名",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Text(
                            text = "正在搜索《$title》的其他可用影视源",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.textSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = colors.border)

                // Results / Loading
                if (loading && resultGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(32.dp))
                            Text("正在全网并发检索多个影视站…", fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                } else if (resultGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
                            Text("没有找到《$title》的其他线路来源", fontSize = 13.sp, color = colors.textSecondary)
                            if (failedSourcesCount > 0) {
                                Text("部分影视站连接超时或无结果", fontSize = 11.sp, color = colors.textMuted)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        resultGroups.forEach { group ->
                            item(key = group.sourceId) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = colors.accent.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = group.sourceName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.accent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${group.items.size} 条结果",
                                            fontSize = 11.sp,
                                            color = colors.textMuted
                                        )
                                    }

                                    group.items.forEach { item ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.surfaceRaised,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSelectMovie(item)
                                                    onClose()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                if (item.poster.isNotBlank()) {
                                                    AsyncImage(
                                                        model = item.poster,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .width(42.dp)
                                                            .height(58.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = colors.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${item.category.ifBlank { "影视" }} · ${item.updateInfo.ifBlank { item.year }}",
                                                        fontSize = 11.sp,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "播放",
                                                    tint = colors.accent,
                                                    modifier = Modifier.size(20.dp)
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
        }
    }
}
