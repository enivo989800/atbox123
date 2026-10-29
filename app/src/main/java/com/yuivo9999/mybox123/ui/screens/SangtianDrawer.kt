package com.yuivo9999.mybox123.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuivo9999.mybox123.app.AppDestination
import com.yuivo9999.mybox123.ui.theme.AppTheme

@Composable
fun SangtianDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNav: (AppDestination) -> Unit,
    onNavSubpage: ((ProfileSubpage) -> Unit)? = null,
    currentThemeId: String = "sangtian",
    onSelectTheme: (String) -> Unit = {},
) {
    if (!isOpen) return

    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClose)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(290.dp)
                .clickable(enabled = false) {}, // prevent clicks from closing
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Header
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.accent.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = colors.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "TVBox Native",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭菜单",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Navigation Items
                    DrawerNavItem(
                        icon = Icons.Default.Home,
                        title = "影视首页",
                        onClick = { onNav(AppDestination.HOME); onClose() }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Movie,
                        title = "影视库与分类",
                        onClick = { onNav(AppDestination.MOVIES); onClose() }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Radio,
                        title = "Live 直播中心",
                        onClick = { onNav(AppDestination.LIVE); onClose() }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Favorite,
                        title = "我的收藏",
                        onClick = { onNav(AppDestination.FAVORITES); onClose() }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.History,
                        title = "播放历史",
                        onClick = {
                            if (onNavSubpage != null) onNavSubpage(ProfileSubpage.HISTORY)
                            onNav(AppDestination.PROFILE)
                            onClose()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Storage,
                        title = "内容源配置",
                        onClick = {
                            if (onNavSubpage != null) onNavSubpage(ProfileSubpage.SOURCES)
                            onNav(AppDestination.PROFILE)
                            onClose()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Settings,
                        title = "系统设置",
                        onClick = {
                            if (onNavSubpage != null) onNavSubpage(ProfileSubpage.SETTINGS)
                            onNav(AppDestination.PROFILE)
                            onClose()
                        }
                    )
                }

                // 3. Footer: Theme Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "切换界面主题",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemePillButton(
                            id = "sangtian",
                            label = "桑田山河",
                            isSelected = currentThemeId == "sangtian",
                            onSelect = { onSelectTheme("sangtian") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemePillButton(
                            id = "dark",
                            label = "深色",
                            isSelected = currentThemeId == "dark",
                            onSelect = { onSelectTheme("dark") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemePillButton(
                            id = "light",
                            label = "浅色",
                            isSelected = currentThemeId == "light",
                            onSelect = { onSelectTheme("light") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ThemePillButton(
    id: String,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) colors.accent else colors.surfaceRaised,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.accent else colors.border
        ),
        modifier = modifier.clickable(onClick = onSelect)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else colors.textPrimary,
                maxLines = 1
            )
        }
    }
}
