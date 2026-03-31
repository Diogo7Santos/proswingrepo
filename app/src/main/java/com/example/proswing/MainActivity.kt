package com.example.proswing

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.data.AppDatabase
import com.example.proswing.ui.navigation.AppNavHost
import com.example.proswing.ui.theme.ProswingTheme
import com.example.proswing.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)

        val database = AppDatabase.getDatabase(applicationContext)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsState()

            ProswingTheme(darkTheme = settings.darkTheme) {
                AppNavHost(database = database)
            }
        }
    }
}