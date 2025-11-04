package com.example.proswing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val settings by settingsViewModel.settings.collectAsState()

    // Removed the TopAppBar, since AppNavHost already handles it
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Units", style = MaterialTheme.typography.titleMedium)

        // Distance units
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Distance (Yards / Meters)")
            Switch(
                checked = settings.useMeters,
                onCheckedChange = {
                    scope.launch { settingsViewModel.setUseMeters(it) }
                }
            )
        }

        // Temperature units
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Temperature (°C / °F)")
            Switch(
                checked = settings.useCelsius,
                onCheckedChange = {
                    scope.launch { settingsViewModel.setUseCelsius(it) }
                }
            )
        }

        Divider()

        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dark Theme")
            Switch(
                checked = settings.darkTheme,
                onCheckedChange = {
                    scope.launch { settingsViewModel.setDarkTheme(it) }
                }
            )
        }
    }
}
