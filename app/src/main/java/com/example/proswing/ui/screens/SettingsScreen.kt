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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Units Section
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Units",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Distance Units", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (settings.useMeters) "Currently using Meters" else "Currently using Yards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useMeters,
                    onCheckedChange = {
                        scope.launch { settingsViewModel.setUseMeters(it) }
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Temperature Units", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (settings.useCelsius) "Currently using Celsius" else "Currently using Fahrenheit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useCelsius,
                    onCheckedChange = {
                        scope.launch { settingsViewModel.setUseCelsius(it) }
                    }
                )
            }
        }

        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Theme Section
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (settings.darkTheme) "Dark mode enabled" else "Light mode enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.darkTheme,
                    onCheckedChange = {
                        scope.launch { settingsViewModel.setDarkTheme(it) }
                    }
                )
            }
        }

        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Handicap Section
        var expanded by remember { mutableStateOf(false) }
        val handicapLevels = listOf(
            "Beginner (40)" to 40,
            "Casual (25)" to 25,
            "Frequent (15)" to 15,
            "Semi-Pro (5)" to 5,
            "Pro (0)" to 0
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Handicap",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    readOnly = true,
                    value = "Current: ${settings.handicap}",
                    onValueChange = {},
                    label = {
                        Text(
                            "Select Handicap Level",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    handicapLevels.forEach { (label, value) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                scope.launch {
                                    settingsViewModel.setHandicap(value)
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}