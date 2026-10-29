package com.yuivo9999.mybox123.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.data.storage.PlaybackHistoryRecord
import com.yuivo9999.mybox123.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NativePlaybackHistoryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyBoxApplication
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<PlaybackHistoryRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = app.awaitServices().userDataRepository.getHistoryRecords()
                .sortedByDescending { it.lastPlayedAt }
            records = list
            loading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("播放历史", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
            if (records.isNotEmpty()) {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            app.awaitServices().userDataRepository.saveHistoryRecords(emptyList())
                        }
                        records = emptyList()
                    }
                }) {
                    Text("清空", color = AppTheme.colors.accent, fontSize = 14.sp)
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.accent)
            }
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无播放历史记录", color = AppTheme.colors.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(records, key = { it.historyId.ifBlank { "${it.targetId}_${it.lastPlayedAt}" } }) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppTheme.colors.card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 64.dp, height = 80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppTheme.colors.background)
                            ) {
                                if (item.poster.isNotBlank()) {
                                    AsyncImage(
                                        model = item.poster,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = AppTheme.colors.accent,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title.ifBlank { "未命名" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppTheme.colors.textPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    item.episodeTitle.ifBlank { "正片" },
                                    fontSize = 13.sp,
                                    color = AppTheme.colors.accent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val mins = (item.positionSeconds / 60.0).toInt()
                                Text(
                                    if (mins > 0) "观看至 $mins 分钟" else "刚开始观看",
                                    fontSize = 11.sp,
                                    color = AppTheme.colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
