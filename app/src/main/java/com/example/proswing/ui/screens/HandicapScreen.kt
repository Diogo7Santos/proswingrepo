package com.example.proswing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandicapScreen(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val settings by settingsViewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    // Handicap presets
    val handicapLevels = listOf(
        "Beginner (40)" to 40,
        "Casual (25)" to 25,
        "Frequent (15)" to 15,
        "Semi-Pro (5)" to 5,
        "Pro (0)" to 0
    )

    var expanded by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Handicap: ${settings.handicap}",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground
        )

        Divider(thickness = 1.dp)

        Text(
            "Enter your custom handicap:",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = manualInput,
            onValueChange = { manualInput = it },
            label = { Text("Enter custom handicap") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.outline,
                unfocusedTextColor = colors.onBackground,
                focusedBorderColor = colors.outline,
                unfocusedBorderColor = colors.onBackground,
                focusedLabelColor = colors.outline,
                unfocusedLabelColor = colors.onBackground,
                cursorColor = colors.outline
            )
        )

        Button(
            onClick = {
                manualInput.toIntOrNull()?.let {
                    scope.launch { settingsViewModel.setHandicap(it) }
                    manualInput = ""
                }
            },
            enabled = manualInput.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save Handicap")
        }

        Divider(thickness = 1.dp)

        Text(
            "Or select your experience level:",
            style = MaterialTheme.typography.titleMedium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = handicapLevels.firstOrNull { it.second == settings.handicap }?.first
                    ?: "Custom (${settings.handicap})",
                onValueChange = {},
                label = { Text("Select Handicap Level") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.outline,
                    unfocusedTextColor = colors.onBackground,
                    focusedBorderColor = colors.outline,
                    unfocusedBorderColor = colors.onBackground,
                    focusedLabelColor = colors.outline,
                    unfocusedLabelColor = colors.onBackground,
                    focusedTrailingIconColor = colors.outline,
                    unfocusedTrailingIconColor = colors.onBackground,
                    cursorColor = colors.outline
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colors.background
            ) {
                handicapLevels.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = colors.onBackground
                            )
                        },
                        onClick = {
                            scope.launch { settingsViewModel.setHandicap(value) }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}