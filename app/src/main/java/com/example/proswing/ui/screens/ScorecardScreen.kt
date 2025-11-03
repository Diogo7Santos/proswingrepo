package com.example.proswing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen() {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Add new round */ }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Scorecards",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Track and review your rounds here.",
                    fontSize = 16.sp,
                    color = colors.onBackground.copy(alpha = 0.8f)
                )
            }

            // Placeholder for round entries
            items(5) { index ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface,
                        contentColor = colors.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Round ${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Text("Date: TBD")
                        Text("Score: --")
                    }
                }
            }
        }
    }
}
