package com.yuivo9999.mybox123.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.json.JSONObject

@Composable
fun NativeSettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyBoxApplication
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    var autoResume by remember { mutableStateOf(true) }
    var fallbackEnabled by remember { mutableStateOf(true) }
    var moviePlaybackScheme by remember { mutableStateOf("ExoPlayer 硬件加速") }
    var livePlaybackScheme by remember { mutableStateOf("ExoPlayer 低延迟直播") }
    var hwDecode by remember { mutableStateOf(true) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var speedMemory by remember { mutableStateOf(true) }

    val movieSchemes = listOf("ExoPlayer 硬件加速", "ExoPlayer 标准", "MediaCodec 原生解码")
    val liveSchemes = listOf("ExoPlayer 低延迟直播", "ExoPlayer 稳定流", "原生流媒体通道")

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val s = app.awaitServices().userDataRepository.getSettings()
                autoResume = s.optBoolean("autoplayResume", true)
                fallbackEnabled = s.optBoolean("fallbackEnabled", true)
                moviePlaybackScheme = s.optString("moviePlaybackScheme", "ExoPlayer 硬件加速").ifBlank { "ExoPlayer 硬件加速" }
                livePlaybackScheme = s.optString("livePlaybackScheme", "ExoPlayer 低延迟直播").ifBlank { "ExoPlayer 低延迟直播" }
                hwDecode = s.optBoolean("hwDecode", true)
                autoPlayNext = s.optBoolean("autoPlayNext", true)
                speedMemory = s.optBoolean("speedMemory", true)
            } catch (_: Exception) {}
        }
    }

    fun save(key: String, value: Any) {
        scope.launch(Dispatchers.IO) {
            try {
                val repo = app.awaitServices().userDataRepository
                val s = repo.getSettings()
                when (value) {
                    is Boolean -> s.put(key, value)
                    is String -> s.put(key, value)
                }
                repo.saveSettings(s)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. 播放设置
        Text("播放设置", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(top = 8.dp))

        SettingRow(
            icon = Icons.Default.PlayArrow,
            title = "自动继续播放",
            value = if (autoResume) "开启" else "关闭",
            onClick = {
                autoResume = !autoResume
                save("autoplayResume", autoResume)
            }
        )

        SettingRow(
            icon = Icons.Default.PlayArrow,
            title = "默认影视播放方案",
            value = moviePlaybackScheme,
            onClick = {
                val idx = movieSchemes.indexOf(moviePlaybackScheme)
                val next = movieSchemes[(idx + 1) % movieSchemes.size]
                moviePlaybackScheme = next
                save("moviePlaybackScheme", next)
            }
        )

        SettingRow(
            icon = Icons.Default.Radio,
            title = "默认直播播放方案",
            value = livePlaybackScheme,
            onClick = {
                val idx = liveSchemes.indexOf(livePlaybackScheme)
                val next = liveSchemes[(idx + 1) % liveSchemes.size]
                livePlaybackScheme = next
                save("livePlaybackScheme", next)
            }
        )

        SettingRow(
            icon = Icons.Default.Radio,
            title = "失败自动切换线路",
            value = if (fallbackEnabled) "开启" else "关闭",
            onClick = {
                fallbackEnabled = !fallbackEnabled
                save("fallbackEnabled", fallbackEnabled)
            }
        )

        // Info Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceRaised,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("可选播放方案", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary)
                    Text(
                        "影视与 Live 统一使用 Android 原生 Media3 ExoPlayer 渲染；遇到异常时自动探测并无缝切换同一频道或剧集的备用线路。",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. 解码与特性
        Text("解码与特性", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(top = 8.dp))

        SettingRow(
            icon = Icons.Default.PlayArrow,
            title = "硬件加速解码 (MediaCodec)",
            value = if (hwDecode) "开启" else "关闭",
            onClick = {
                hwDecode = !hwDecode
                save("hwDecode", hwDecode)
            }
        )

        SettingRow(
            icon = Icons.Default.PlayArrow,
            title = "自动播放下一集",
            value = if (autoPlayNext) "开启" else "关闭",
            onClick = {
                autoPlayNext = !autoPlayNext
                save("autoPlayNext", autoPlayNext)
            }
        )

        SettingRow(
            icon = Icons.Default.PlayArrow,
            title = "记忆倍速设置",
            value = if (speedMemory) "开启" else "关闭",
            onClick = {
                speedMemory = !speedMemory
                save("speedMemory", speedMemory)
            }
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
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
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
