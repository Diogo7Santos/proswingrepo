package com.example.proswing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.ui.navigation.AppNavHost
import com.example.proswing.ui.theme.ProswingTheme
import com.example.proswing.viewmodel.SettingsViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import android.webkit.WebView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        setContent {
            // Observe theme preference from DataStore via SettingsViewModel
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsState()

            // Apply theme dynamically
            ProswingTheme(darkTheme = settings.darkTheme) {
                // Root of your app – includes Scaffold + Navigation Drawer
                AppNavHost()
            }
        }
    }
}
