package com.yuivo9999.mybox123.app

data class AppUiState(
    val destination: AppDestination = AppDestination.HOME,
    val themeId: String = "sangtian",
    val fontSize: String = "medium",
    val cardStyle: String = "poster",
    val density: String = "comfortable",
    val autoplayResume: Boolean = true,
    val fallbackEnabled: Boolean = true,
    val hwDecode: Boolean = true,
)
