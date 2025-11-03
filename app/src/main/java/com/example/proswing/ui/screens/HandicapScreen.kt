package com.example.proswing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandicapScreen() {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Your Handicap",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "This screen will track your handicap calculations and history.",
                color = colors.onBackground.copy(alpha = 0.8f)
            )
        }
    }
}
