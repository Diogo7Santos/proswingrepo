package com.example.proswing.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.data.ScorecardEntity
import com.example.proswing.viewmodel.Hole
import com.example.proswing.viewmodel.ScorecardViewModel
import com.example.proswing.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    scorecardViewModel: ScorecardViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val settings by settingsViewModel.settings.collectAsState()
    val handicap = settings.handicap
    val savedRounds by scorecardViewModel.savedRounds.collectAsState(emptyList())

    var showNewRound by remember { mutableStateOf(false) }
    var holeCount by remember { mutableStateOf(18) }
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(horizontalAlignment = Alignment.End) {
                        ExtendedFloatingActionButton(
                            icon = {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "9 Holes",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            text = {
                                Text(
                                    "9 Holes",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            onClick = {
                                holeCount = 9
                                showNewRound = true
                                showFabMenu = false
                            },
                            containerColor = colors.secondary
                        )

                        Spacer(Modifier.height(8.dp))

                        ExtendedFloatingActionButton(
                            icon = {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "18 Holes",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            text = {
                                Text(
                                    "18 Holes",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            onClick = {
                                holeCount = 18
                                showNewRound = true
                                showFabMenu = false
                            },
                            containerColor = colors.secondary
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = colors.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Start New Round")
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
                color = colors.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            if (showNewRound) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.background
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "New Round (${holeCount} Holes)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        NewRound(scorecardViewModel, handicap, holeCount) {
                            showNewRound = false
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Text(
                text = "Saved Rounds",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )

            Spacer(Modifier.height(8.dp))

            if (savedRounds.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No saved rounds yet. Start one using the + button.")
                }
            } else {
                ScoreHistory(savedRounds)
            }
        }
    }
}

@Composable
fun NewRound(
    scorecardViewModel: ScorecardViewModel,
    handicap: Int,
    holeCount: Int,
    onFinish: () -> Unit
) {
    var holes by remember { mutableStateOf(List(holeCount) { Hole(it + 1, 4, 1) }) }

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
                    onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                Text("Save Round")
            }
        }
    }
}

@Composable
fun HoleCard(hole: Hole, onUpdate: (Hole) -> Unit) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Hole ${hole.number}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (3..6).forEach { parValue ->
                    FilterChip(
                        selected = hole.par == parValue,
                        onClick = { onUpdate(hole.copy(par = parValue)) },
                        label = { Text("Par $parValue") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.background
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Strokes: ${hole.strokes ?: 1}",
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val newStroke = (hole.strokes ?: 1) - 1
                            if (newStroke >= 1) onUpdate(hole.copy(strokes = newStroke))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.background
                        )
                    ) {
                        Text("-")
                    }

                    Button(
                        onClick = {
                            val newStroke = (hole.strokes ?: 1) + 1
                            onUpdate(hole.copy(strokes = newStroke))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.background
                        )
                    ) {
                        Text("+")
                    }
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Round on ${round.date}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "Gross: ${round.totalScore} | Net: ${round.netScore} | To Par: ${round.toPar}",
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        AnimatedVisibility(expanded) {
                            Column(Modifier.padding(top = 8.dp)) {
                                Text(
                                    "Hole-by-Hole:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "Hole:",
                                        modifier = Modifier.width(48.dp),
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    round.holes.forEachIndexed { index, _ ->
                                        Text(
                                            "${index + 1}",
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "Par:",
                                        modifier = Modifier.width(48.dp),
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    round.pars.forEach { par ->
                                        Text(
                                            par.toString(),
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "Score:",
                                        modifier = Modifier.width(48.dp),
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    round.holes.forEach { score ->
                                        Text(
                                            score.toString(),
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
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