package com.yuivo9999.mybox123

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuivo9999.mybox123.app.AppViewModel
import com.yuivo9999.mybox123.ui.navigation.MyBoxApp
import com.yuivo9999.mybox123.ui.theme.MyBoxTheme

/**
 * Native Android entry point supporting Android 10 (API 29) through Android 15 (API 35+).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
            MyBoxTheme(themeId = uiState.themeId) {
                MyBoxApp(
                    onExit = ::finish,
                    appViewModel = appViewModel
                )
            }
        }
    }
}
