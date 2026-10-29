package com.yuivo9999.mybox123.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppThemeColors = staticCompositionLocalOf { SangtianThemeColors }

object AppTheme {
    val colors: AppThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemeColors.current

    fun getThemeById(id: String?): AppThemeColors {
        return when (id?.lowercase()) {
            "dark" -> DarkThemeColors
            "light" -> LightThemeColors
            else -> SangtianThemeColors
        }
    }

    fun nextThemeId(currentId: String?): String {
        return when (currentId?.lowercase()) {
            "sangtian" -> "dark"
            "dark" -> "light"
            "light" -> "sangtian"
            else -> "dark"
        }
    }
}

@Composable
fun MyBoxTheme(
    themeId: String = "sangtian",
    content: @Composable () -> Unit
) {
    val themeColors = AppTheme.getThemeById(themeId)
    val m3ColorScheme = if (themeColors.isDark) {
        darkColorScheme(
            primary = themeColors.accent,
            secondary = themeColors.gold,
            background = themeColors.background,
            surface = themeColors.surface,
            surfaceVariant = themeColors.surfaceVariant,
            onPrimary = themeColors.background,
            onSecondary = themeColors.background,
            onBackground = themeColors.textPrimary,
            onSurface = themeColors.textPrimary,
            onSurfaceVariant = themeColors.textSecondary,
            outline = themeColors.border,
        )
    } else {
        lightColorScheme(
            primary = themeColors.accent,
            secondary = themeColors.gold,
            background = themeColors.background,
            surface = themeColors.surface,
            surfaceVariant = themeColors.surfaceVariant,
            onPrimary = themeColors.background,
            onSecondary = themeColors.background,
            onBackground = themeColors.textPrimary,
            onSurface = themeColors.textPrimary,
            onSurfaceVariant = themeColors.textSecondary,
            outline = themeColors.border,
        )
    }

    CompositionLocalProvider(LocalAppThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            content = content
        )
    }
}
