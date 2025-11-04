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

@Composable
fun YardagesScreen(viewModel: MyBagViewModel = viewModel()) {
    val clubs by viewModel.clubs.collectAsState()
    val colors = MaterialTheme.colorScheme

    var selectedClubId by remember { mutableStateOf<Int?>(null) }
    var carryDistance by remember { mutableStateOf("") }
    var totalDistance by remember { mutableStateOf("") }

    // Removed Scaffold + TopAppBar — handled by AppNavHost
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Select a club and enter your yardages:",
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                colors.primaryContainer
                            else colors.surface
                        ),
                        onClick = {
                            selectedClubId = if (isSelected) null else club.id
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${club.type} ${club.variant ?: ""}".trim())
                            Text(
                                "${club.brand} ${club.model}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            // ✅ Show saved yardages if available
                            if (club.carryDistance != null && club.totalDistance != null) {
                                Text(
                                    "Carry: ${club.carryDistance} yd | Total: ${club.totalDistance} yd",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
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
                    text = "Enter yardages for selected club:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = carryDistance,
                    onValueChange = { carryDistance = it },
                    label = { Text("Average Carry Distance (yards)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalDistance,
                    onValueChange = { totalDistance = it },
                    label = { Text("Total Distance (yards)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.updateYardages(
                            clubId = selectedClubId!!,
                            carry = carryDistance.toIntOrNull(),
                            total = totalDistance.toIntOrNull()
                        )
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
