package com.yuivo9999.mybox123.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun NativeAppearanceScreen(
    currentThemeId: String = "sangtian",
    onThemeChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyBoxApplication
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    var theme by remember { mutableStateOf(currentThemeId) }
    var fontSize by remember { mutableStateOf("medium") }
    var cardStyle by remember { mutableStateOf("poster") }
    var density by remember { mutableStateOf("comfortable") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val s = app.awaitServices().userDataRepository.getSettings()
                theme = s.optString("theme", currentThemeId).ifBlank { currentThemeId }
                fontSize = s.optString("fontSize", "medium").ifBlank { "medium" }
                cardStyle = s.optString("cardStyle", "poster").ifBlank { "poster" }
                density = s.optString("density", "comfortable").ifBlank { "comfortable" }
            } catch (_: Exception) {}
        }
    }

    fun save(key: String, value: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val repo = app.awaitServices().userDataRepository
                val s = repo.getSettings()
                s.put(key, value)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Theme Section
        SectionTitle(title = "主题")
        AppearanceMenuItem(
            icon = Icons.Default.Palette,
            title = "主题",
            value = when (theme) {
                "sangtian" -> "桑田山河"
                "dark" -> "深色"
                "light" -> "浅色"
                else -> "桑田山河"
            },
            onClick = {
                val next = when (theme) {
                    "sangtian" -> "dark"
                    "dark" -> "light"
                    "light" -> "sangtian"
                    else -> "sangtian"
                }
                theme = next
                save("theme", next)
                onThemeChanged(next)
            }
        )

        // 2. Font Section
        SectionTitle(title = "字体")
        AppearanceMenuItem(
            icon = Icons.Default.FormatSize,
            title = "字体大小",
            value = when (fontSize) {
                "large" -> "大"
                "small" -> "小"
                else -> "中"
            },
            onClick = {
                val next = when (fontSize) {
                    "small" -> "medium"
                    "medium" -> "large"
                    "large" -> "small"
                    else -> "medium"
                }
                fontSize = next
                save("fontSize", next)
            }
        )

        // 3. Display Section
        SectionTitle(title = "显示")
        AppearanceMenuItem(
            icon = Icons.Default.ViewAgenda,
            title = "卡片显示",
            value = if (cardStyle == "compact") "紧凑" else "海报",
            onClick = {
                val next = if (cardStyle == "compact") "poster" else "compact"
                cardStyle = next
                save("cardStyle", next)
            }
        )

        AppearanceMenuItem(
            icon = Icons.Default.ViewCompact,
            title = "显示密度",
            value = if (density == "compact") "紧凑" else "舒适",
            onClick = {
                val next = if (density == "compact") "comfortable" else "compact"
                density = next
                save("density", next)
            }
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    val colors = AppTheme.colors
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = colors.accent,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun AppearanceMenuItem(
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
