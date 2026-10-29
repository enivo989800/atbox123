package com.yuivo9999.mybox123.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Three authentic MyBox / TVBox themes:
 * 1. SANGTIAN ("桑田山河") - Default oriental warm parchment aesthetics
 * 2. DARK ("深色") - Sleek cinema obsidian palette
 * 3. LIGHT ("浅色") - Crisp daylight clean palette
 */

// Legacy fallback colors for backward compatibility
val MyBoxBackground = Color(0xFFFCF9F2)
val MyBoxSurface = Color(0xFFF6EFE2)
val MyBoxSurfaceRaised = Color(0xFFF0E6D2)
val MyBoxSurfaceVariant = Color(0xFFEADBCE)
val MyBoxBorder = Color(0xFFD4C5A9)
val MyBoxOutline = Color(0xFFC7B696)
val MyBoxTextPrimary = Color(0xFF261F18)
val MyBoxTextSecondary = Color(0xFF7C6F62)
val MyBoxTextMuted = Color(0xFF9C8E80)
val MyBoxGold = Color(0xFFC83C23)
val MyBoxAmber = Color(0xFFD97706)
val MyBoxGoldLight = Color(0xFFFEF3C7)
val MyBoxHeart = Color(0xFFE60012)
val MyBoxAccent = Color(0xFFC83C23)

// Theme Palette Definitions
data class AppThemeColors(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val screen: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceVariant: Color,
    val border: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val gold: Color,
    val heart: Color,
    val playerBg: Color,
    val topBarBg: Color,
    val cardBg: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val liveDot: Color,
) {
    val card: Color get() = cardBg
    val primary: Color get() = accent
}

// 1. 桑田山河 (Sangtian Shanhe) - Default
val SangtianThemeColors = AppThemeColors(
    id = "sangtian",
    name = "桑田山河",
    isDark = false,
    background = Color(0xFFFCF9F2),
    screen = Color(0xFFFCF9F2),
    surface = Color(0xFFF6EFE2),
    surfaceRaised = Color(0xFFF0E6D2),
    surfaceVariant = Color(0xFFEADBCE),
    border = Color(0xFFD4C5A9),
    outline = Color(0xFFC7B696),
    textPrimary = Color(0xFF261F18),
    textSecondary = Color(0xFF7C6F62),
    textMuted = Color(0xFF9C8E80),
    accent = Color(0xFFC83C23), // 朱砂红 / 陶土赤
    gold = Color(0xFFC83C23),
    heart = Color(0xFFE60012),
    playerBg = Color(0xFF140F0B),
    topBarBg = Color(0xFFF6EFE2),
    cardBg = Color(0xFFF6EFE2),
    badgeBg = Color(0xFFEADBCE),
    badgeText = Color(0xFF261F18),
    liveDot = Color(0xFFE60012),
)

// 2. 深色 (Dark Obsidian Cinema)
val DarkThemeColors = AppThemeColors(
    id = "dark",
    name = "深色",
    isDark = true,
    background = Color(0xFF07080B),
    screen = Color(0xFF0B0D12),
    surface = Color(0xFF11141B),
    surfaceRaised = Color(0xFF171B27),
    surfaceVariant = Color(0xFF252B38),
    border = Color(0xFF252B37),
    outline = Color(0xFF303744),
    textPrimary = Color(0xFFF5F7FB),
    textSecondary = Color(0xFF9DA5B5),
    textMuted = Color(0xFF8F98AA),
    accent = Color(0xFFF2F4F8),
    gold = Color(0xFFF59E0B),
    heart = Color(0xFFFF4D6D),
    playerBg = Color(0xFF000000),
    topBarBg = Color(0xFF0B0D12),
    cardBg = Color(0xFF11141B),
    badgeBg = Color(0xFF171B27),
    badgeText = Color(0xFFF5F7FB),
    liveDot = Color(0xFF10B981),
)

// 3. 浅色 (Light Daylight)
val LightThemeColors = AppThemeColors(
    id = "light",
    name = "浅色",
    isDark = false,
    background = Color(0xFFEEF1F5),
    screen = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF0F3F8),
    surfaceVariant = Color(0xFFE2E7EE),
    border = Color(0xFFD9DEE7),
    outline = Color(0xFFBCC5D3),
    textPrimary = Color(0xFF151820),
    textSecondary = Color(0xFF788292),
    textMuted = Color(0xFF8E97A8),
    accent = Color(0xFF1A73E8),
    gold = Color(0xFFD97706),
    heart = Color(0xFFE60012),
    playerBg = Color(0xFF0A0D12),
    topBarBg = Color(0xFFFFFFFF),
    cardBg = Color(0xFFFFFFFF),
    badgeBg = Color(0xFFE2E7EE),
    badgeText = Color(0xFF151820),
    liveDot = Color(0xFF10B981),
)
