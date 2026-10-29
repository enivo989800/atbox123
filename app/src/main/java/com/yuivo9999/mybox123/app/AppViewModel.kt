package com.yuivo9999.mybox123.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuivo9999.mybox123.MyBoxApplication
import com.yuivo9999.mybox123.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val app = getApplication<MyBoxApplication>()
                    val repo = app.awaitServices().userDataRepository
                    val s = repo.getSettings()
                    val theme = s.optString("theme", "sangtian").ifBlank { "sangtian" }
                    val font = s.optString("fontSize", "medium").ifBlank { "medium" }
                    val card = s.optString("cardStyle", "poster").ifBlank { "poster" }
                    val density = s.optString("density", "comfortable").ifBlank { "comfortable" }
                    val autoResume = s.optBoolean("autoplayResume", true)
                    val fallback = s.optBoolean("fallbackEnabled", true)
                    val hw = s.optBoolean("hwDecode", true)
                    _uiState.update {
                        it.copy(
                            themeId = theme,
                            fontSize = font,
                            cardStyle = card,
                            density = density,
                            autoplayResume = autoResume,
                            fallbackEnabled = fallback,
                            hwDecode = hw
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun setTheme(themeId: String) {
        _uiState.update { it.copy(themeId = themeId) }
        saveSetting("theme", themeId)
    }

    fun cycleTheme() {
        val next = AppTheme.nextThemeId(_uiState.value.themeId)
        setTheme(next)
    }

    fun setFontSize(size: String) {
        _uiState.update { it.copy(fontSize = size) }
        saveSetting("fontSize", size)
    }

    fun setCardStyle(style: String) {
        _uiState.update { it.copy(cardStyle = style) }
        saveSetting("cardStyle", style)
    }

    fun setDensity(density: String) {
        _uiState.update { it.copy(density = density) }
        saveSetting("density", density)
    }

    fun setAutoplayResume(enable: Boolean) {
        _uiState.update { it.copy(autoplayResume = enable) }
        saveSetting("autoplayResume", enable)
    }

    fun setFallbackEnabled(enable: Boolean) {
        _uiState.update { it.copy(fallbackEnabled = enable) }
        saveSetting("fallbackEnabled", enable)
    }

    fun setHwDecode(enable: Boolean) {
        _uiState.update { it.copy(hwDecode = enable) }
        saveSetting("hwDecode", enable)
    }

    private fun saveSetting(key: String, value: Any) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<MyBoxApplication>()
                val repo = app.awaitServices().userDataRepository
                val s = repo.getSettings()
                when (value) {
                    is String -> s.put(key, value)
                    is Boolean -> s.put(key, value)
                    is Int -> s.put(key, value)
                }
                repo.saveSettings(s)
            } catch (_: Exception) {}
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(destination = destination) }
    }

    fun navigateHome() {
        _uiState.update { it.copy(destination = AppDestination.HOME) }
    }
}
