package com.example.proswing.ui.screens

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.viewmodel.MyBagViewModel
import com.example.proswing.viewmodel.SettingsViewModel

@Composable
fun YardagesScreen(
    bagViewModel: MyBagViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val clubs by bagViewModel.clubs.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val colors = MaterialTheme.colorScheme
    val distanceUnit = if (settings.useMeters) "m" else "yd"

    var selectedClubId by remember { mutableStateOf<Int?>(null) }
    var carryDistance by remember { mutableStateOf("") }
    var totalDistance by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Distances",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a club and enter your distances (${if (settings.useMeters) "meters" else "yards"}):",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (clubs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No clubs found in My Bag.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(clubs, key = { it.id }) { club ->
                    val isSelected = selectedClubId == club.id
                    val carryDisplay = club.carryDistance?.let {
                        if (settings.useMeters) it / 1.094 else it
                    }
                    val totalDisplay = club.totalDistance?.let {
                        if (settings.useMeters) it / 1.094 else it
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                colors.secondary
                            else colors.surface
                        ),
                        onClick = {
                            selectedClubId = if (isSelected) null else club.id
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${club.type} ${club.variant ?: ""}".trim(),
                                color = if (isSelected) colors.tertiary else colors.onSurface
                            )
                            Text(
                                "${club.brand} ${club.model}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            if (carryDisplay != null && totalDisplay != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Carry: %.1f %s  |  Total: %.1f %s".format(
                                        carryDisplay, distanceUnit, totalDisplay, distanceUnit
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            if (selectedClubId != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter distances for selected club ($distanceUnit):",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = carryDistance,
                    onValueChange = { carryDistance = it },
                    label = { Text("Average Carry Distance ($distanceUnit)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onBackground,
                        unfocusedTextColor = colors.onBackground,
                        focusedBorderColor = colors.outline,
                        unfocusedBorderColor = colors.onBackground,
                        focusedLabelColor = colors.outline,
                        unfocusedLabelColor = colors.onBackground,
                        cursorColor = colors.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalDistance,
                    onValueChange = { totalDistance = it },
                    label = { Text("Total Distance ($distanceUnit)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onBackground,
                        unfocusedTextColor = colors.onBackground,
                        focusedBorderColor = colors.outline,
                        unfocusedBorderColor = colors.onBackground,
                        focusedLabelColor = colors.outline,
                        unfocusedLabelColor = colors.onBackground,
                        cursorColor = colors.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val carry = carryDistance.toFloatOrNull()
                        val total = totalDistance.toFloatOrNull()

                        if (carry != null && total != null) {
                            val carryInYards = if (settings.useMeters) carry * 1.094f else carry
                            val totalInYards = if (settings.useMeters) total * 1.094f else total

                            bagViewModel.updateYardages(selectedClubId!!, carryInYards, totalInYards)
                            println("Saved Club $selectedClubId: Carry $carryInYards yd | Total $totalInYards yd")
                        }

                        carryDistance = ""
                        totalDistance = ""
                        selectedClubId = null
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save")
                }
            }
        }
    }
}