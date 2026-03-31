package com.example.proswing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.data.AppDatabase
import com.example.proswing.viewmodel.CaddieViewModel
import com.example.proswing.viewmodel.CaddieViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaddieScreen(
    database: AppDatabase
) {
    val viewModel: CaddieViewModel = viewModel(
        factory = CaddieViewModelFactory(database.golfClubDao())
    )

    val clubList by viewModel.clubUiState.collectAsState()
    val recommendation by viewModel.recommendation.collectAsState()

    var distanceInput by remember { mutableStateOf("") }
    var selectedElement by remember { mutableStateOf(CourseElement.FAIRWAY) }
    var windType by remember { mutableStateOf(WindType.NONE) }
    var windStrength by remember { mutableStateOf(0) }
    var elevationType by remember { mutableStateOf(ElevationType.FLAT) }
    var lieType by remember { mutableStateOf(LieType.NORMAL) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Virtual Caddie",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Get a club recommendation using your saved yardages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )

            OutlinedTextField(
                value = distanceInput,
                onValueChange = { distanceInput = it.filter(Char::isDigit) },
                label = { Text("Distance to pin (yards)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            CaddieDropdown(
                label = "Course Element",
                options = CourseElement.entries,
                selected = selectedElement,
                onSelected = { selectedElement = it },
                optionLabel = { it.label }
            )

            CaddieDropdown(
                label = "Wind",
                options = WindType.entries,
                selected = windType,
                onSelected = { windType = it },
                optionLabel = { it.label }
            )

            if (windType != WindType.NONE) {
                Column {
                    Text("Wind strength: $windStrength yds adjustment")
                    Slider(
                        value = windStrength.toFloat(),
                        onValueChange = { windStrength = it.toInt() },
                        valueRange = 0f..20f
                    )
                }
            }

            CaddieDropdown(
                label = "Elevation",
                options = ElevationType.entries,
                selected = elevationType,
                onSelected = { elevationType = it },
                optionLabel = { it.label }
            )

            CaddieDropdown(
                label = "Lie",
                options = LieType.entries,
                selected = lieType,
                onSelected = { lieType = it },
                optionLabel = { it.label }
            )

            Button(
                onClick = {
                    viewModel.generateRecommendation(
                        distanceToPin = distanceInput.toIntOrNull() ?: 0,
                        courseElement = selectedElement,
                        windType = windType,
                        windStrength = windStrength,
                        elevationType = elevationType,
                        lieType = lieType
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = distanceInput.isNotBlank() && clubList.isNotEmpty()
            ) {
                Text("Get Recommendation")
            }

            HorizontalDivider()

            Text(
                text = "Saved Yardages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (clubList.isEmpty()) {
                Text(
                    text = "No clubs with saved carry yardages found. Add or update yardages in My Bag first.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            } else {
                clubList.forEach { club ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(club.displayName, fontWeight = FontWeight.Medium)
                            Text("${club.carryDistance} yds")
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Recommendation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            recommendation?.let { result ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = result.recommendedClub,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Effective distance: ${result.effectiveDistance} yds")
                        Text("Saved carry distance: ${result.matchedCarryDistance} yds")
                        Text("Reason: ${result.reason}")

                        result.alternativeClub?.let {
                            Text("Alternative: $it")
                        }
                    }
                }
            } ?: Text(
                text = "Enter the shot details and tap Get Recommendation.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CaddieDropdown(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    optionLabel: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}