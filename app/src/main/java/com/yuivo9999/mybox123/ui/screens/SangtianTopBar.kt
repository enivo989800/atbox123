package com.yuivo9999.mybox123.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuivo9999.mybox123.ui.theme.AppTheme

@Composable
fun SangtianTopBar(
    title: String,
    subTitle: String,
    onHamburger: () -> Unit,
    onSearchSameName: (() -> Unit)? = null,
    onBack: () -> Unit,
    currentThemeId: String = "sangtian",
    onSelectTheme: (String) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    currentMediaUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Left Hamburger Menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onHamburger) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "功能菜单",
                    tint = colors.textPrimary
                )
            }
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.textPrimary
                )
            }
        }

        // 2. Center: Title + Subtitle
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.ifBlank { "影片播放" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (subTitle.isNotBlank()) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3. Right: Search Same Name + More Menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onSearchSameName != null) {
                IconButton(onClick = onSearchSameName) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "全网搜同名",
                        tint = colors.accent
                    )
                }
            }

            Box {
                IconButton(onClick = { moreMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多操作",
                        tint = colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("复制当前播放链接") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            moreMenuExpanded = false
                            if (!currentMediaUrl.isNullOrBlank()) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("URL", currentMediaUrl))
                                Toast.makeText(context, "播放地址已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "暂无播放地址", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("切换主题 (当前: ${colors.name})") },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        onClick = {
                            val next = AppTheme.nextThemeId(currentThemeId)
                            onSelectTheme(next)
                            moreMenuExpanded = false
                        }
                    )

                    if (onOpenSettings != null) {
                        DropdownMenuItem(
                            text = { Text("播放与系统设置") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                onOpenSettings()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("返回上一页") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                        onClick = {
                            moreMenuExpanded = false
                            onBack()
                        }
                    )
                }
            }
        }
    }
}
