package com.example.proswing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    var currentStep by remember { mutableStateOf(0) }
    var showRecommendationDialog by remember { mutableStateOf(false) }

    val totalSteps = 5
    val colors = MaterialTheme.colorScheme

    fun resetForm() {
        currentStep = 0
        distanceInput = ""
        selectedElement = CourseElement.FAIRWAY
        windType = WindType.NONE
        windStrength = 0
        elevationType = ElevationType.FLAT
        lieType = LieType.NORMAL
        showRecommendationDialog = false
    }

    Scaffold(
        containerColor = colors.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (clubList.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Virtual Caddie",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "No clubs with saved carry yardages were found.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Add or update your club yardages in My Bag first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onBackground.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { resetForm() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colors.onBackground
                                )
                            ) {
                                Text("Reset")
                            }
                        }

                        Text(
                            text = "Virtual Caddie",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Step ${currentStep + 1} of $totalSteps",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary
                        )

                        LinearProgressIndicator(
                            progress = { (currentStep + 1) / totalSteps.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        when (currentStep) {
                            0 -> {
                                StepContainer(
                                    title = "How far is it to the pin?",
                                    subtitle = "Enter the distance in yards."
                                ) {
                                    OutlinedTextField(
                                        value = distanceInput,
                                        onValueChange = {
                                            distanceInput = it.filter(Char::isDigit)
                                        },
                                        label = { Text("Distance to pin (yards)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            1 -> {
                                StepContainer(
                                    title = "What is the course element?",
                                    subtitle = "This helps filter the clubs realistically."
                                ) {
                                    CaddieDropdown(
                                        label = "Course Element",
                                        options = CourseElement.entries,
                                        selected = selectedElement,
                                        onSelected = { selectedElement = it },
                                        optionLabel = { it.label }
                                    )
                                }
                            }

                            2 -> {
                                StepContainer(
                                    title = "What is the wind doing?",
                                    subtitle = "Choose the wind type and strength."
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CaddieDropdown(
                                            label = "Wind",
                                            options = WindType.entries,
                                            selected = windType,
                                            onSelected = { windType = it },
                                            optionLabel = { it.label }
                                        )

                                        if (windType != WindType.NONE) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Wind strength: $windStrength yds adjustment",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Slider(
                                                    value = windStrength.toFloat(),
                                                    onValueChange = { windStrength = it.toInt() },
                                                    valueRange = 0f..20f,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                StepContainer(
                                    title = "What is the elevation?",
                                    subtitle = "Is the shot flat, uphill, or downhill?"
                                ) {
                                    CaddieDropdown(
                                        label = "Elevation",
                                        options = ElevationType.entries,
                                        selected = elevationType,
                                        onSelected = { elevationType = it },
                                        optionLabel = { it.label }
                                    )
                                }
                            }

                            4 -> {
                                StepContainer(
                                    title = "What is the lie?",
                                    subtitle = "Choose the ball lie before getting the recommendation."
                                ) {
                                    CaddieDropdown(
                                        label = "Lie",
                                        options = LieType.entries,
                                        selected = lieType,
                                        onSelected = { lieType = it },
                                        optionLabel = { it.label }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                enabled = currentStep > 0,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colors.onBackground,
                                    disabledContentColor = colors.onBackground.copy(alpha = 0.4f)
                                )
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    if (currentStep < totalSteps - 1) {
                                        currentStep++
                                    } else {
                                        viewModel.generateRecommendation(
                                            distanceToPin = distanceInput.toIntOrNull() ?: 0,
                                            courseElement = selectedElement,
                                            windType = windType,
                                            windStrength = windStrength,
                                            elevationType = elevationType,
                                            lieType = lieType
                                        )
                                        showRecommendationDialog = true
                                    }
                                },
                                enabled = when (currentStep) {
                                    0 -> distanceInput.isNotBlank()
                                    else -> true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (currentStep == totalSteps - 1) "Get Recommendation" else "Next")
                            }
                        }
                    }
                }
            }
        }

        if (showRecommendationDialog && recommendation != null) {
            AlertDialog(
                onDismissRequest = { showRecommendationDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = { showRecommendationDialog = false }
                    ) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { resetForm() }
                    ) {
                        Text("Start Over")
                    }
                },
                title = {
                    Text(
                        text = "Club Recommendation",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    val result = recommendation!!
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            )
        }
    }
}

@Composable
private fun StepContainer(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    )
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