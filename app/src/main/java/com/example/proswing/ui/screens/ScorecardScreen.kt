package com.example.proswing.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.viewmodel.ScorecardViewModel
import com.example.proswing.viewmodel.SettingsViewModel
import com.example.proswing.data.ScorecardEntity
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.example.proswing.viewmodel.Hole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    scorecardViewModel: ScorecardViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val settings by settingsViewModel.settings.collectAsState()
    val handicap = settings.handicap

    var mode by remember { mutableStateOf("none") } // "none", "new", "history"
    var showFabMenu by remember { mutableStateOf(false) }

    val savedRounds by scorecardViewModel.savedRounds.collectAsState(emptyList())

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(horizontalAlignment = Alignment.End) {
                        ExtendedFloatingActionButton(
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "New Round") },
                            text = { Text("Start New Round") },
                            onClick = {
                                mode = "new"
                                showFabMenu = false
                            },
                            containerColor = colors.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        ExtendedFloatingActionButton(
                            icon = { Icon(Icons.Default.List, contentDescription = "View History") },
                            text = { Text("Saved Rounds") },
                            onClick = {
                                mode = "history"
                                showFabMenu = false
                            },
                            containerColor = colors.secondary
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = colors.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Scorecard Menu")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Virtual Golf Scorecard",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primary
            )

            Spacer(Modifier.height(12.dp))

            when (mode) {
                "none" -> Text("Choose an option to begin.")
                "new" -> NewRound(scorecardViewModel, handicap)
                "history" -> ScoreHistory(savedRounds)
            }
        }
    }
}

@Composable
fun NewRound(
    scorecardViewModel: ScorecardViewModel,
    handicap: Int
) {
    var holes by remember { mutableStateOf(List(18) { Hole(it + 1, 4, null) }) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(holes) { hole ->
            HoleCard(hole) { updatedHole ->
                holes = holes.map {
                    if (it.number == hole.number) updatedHole else it
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scorecardViewModel.saveRound(holes, handicap)
                },
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                Text("Save Round")
            }
        }
    }
}

@Composable
fun HoleCard(hole: Hole, onUpdate: (Hole) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Hole ${hole.number}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Select Par (buttons 3–6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (3..6).forEach { parValue ->
                    FilterChip(
                        selected = hole.par == parValue,
                        onClick = { onUpdate(hole.copy(par = parValue)) },
                        label = { Text("Par $parValue") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Stroke input (buttons + and –)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Strokes: ${hole.strokes ?: "-"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val newStroke = (hole.strokes ?: hole.par) - 1
                        if (newStroke >= 1) onUpdate(hole.copy(strokes = newStroke))
                    }) { Text("-") }

                    Button(onClick = {
                        val newStroke = (hole.strokes ?: hole.par) + 1
                        onUpdate(hole.copy(strokes = newStroke))
                    }) { Text("+") }
                }
            }
        }
    }
}

@Composable
fun ScoreHistory(rounds: List<ScorecardEntity>) {
    if (rounds.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No saved rounds yet.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rounds) { round ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Round on ${round.date}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Gross: ${round.totalScore} | Net: ${round.netScore} | To Par: ${round.toPar}")

                        AnimatedVisibility(expanded) {
                            Column(Modifier.padding(top = 8.dp)) {
                                Text("Hole-by-Hole:")
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    round.holes.forEachIndexed { index, score ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${index + 1}", style = MaterialTheme.typography.bodySmall)
                                            Text(score.toString(), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
