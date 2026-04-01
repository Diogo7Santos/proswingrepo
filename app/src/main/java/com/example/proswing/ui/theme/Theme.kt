package com.example.proswing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ObsidianGray,
    primaryContainer = DarkGreen,
    onPrimary = White,
    onPrimaryContainer = Gray,
    secondary = Beige,
    onSecondary = Black,
    onSecondaryContainer = Beige,
    surfaceVariant = LightGold,
    outline = Green,
    background = CharcoalBlack,
    onBackground = White
)

private val LightColorScheme = lightColorScheme(
    primary = LightGreen,
    onPrimary = GraphiteGray,
    onPrimaryContainer = PorcelainWhite,
    secondary = Beige,
    outline = Green,
    background = PorcelainWhite,
    onBackground = Black
)

@Composable
fun ProswingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}