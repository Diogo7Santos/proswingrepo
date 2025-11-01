package com.example.proswing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.proswing.ui.navigation.AppNavHost
import com.example.proswing.ui.theme.ProswingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProswingTheme {
                // Root of your app – includes Scaffold + Bottom Navigation
                AppNavHost()
            }
        }
    }
}
