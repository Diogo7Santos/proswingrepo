package com.example.proswing.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.R
import com.example.proswing.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    onCaddieClick: () -> Unit = {},
    onSwingClick: () -> Unit = {},
    onScorecardClick: () -> Unit = {},
    onMyBagClick: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val settings by settingsViewModel.settings.collectAsState()
    val handicap = settings.handicap
    val colors = MaterialTheme.colorScheme

    val tips = remember {
        listOf(
            "Use Learn to follow the course and build solid swing fundamentals.",
            "Analyse lets you crop and add lines to check key swing positions.",
            "Turn Crop ON to pinch and pan the image inside the editor.",
            "Use Template ON to align your setup (DTL or Face-on).",
            "Save exports exactly what you see: crop, overlays, and lines."
        )
    }
    var tipIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5500)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(end = 8.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Virtual Caddie",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onCaddieClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_caddie),
                            contentDescription = "Caddie Logo",
                            modifier = Modifier.size(110.dp),
                            colorFilter = ColorFilter.tint(colors.onPrimary)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(start = 8.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Handicap",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (handicap == 0) "0 (Pro)" else handicap.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.onPrimary
                    )
                }
            }
        }

        Button(
            onClick = { onSwingClick() },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.onBackground),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tigerswing),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                OutlinedOverlayText(
                    text = "Swing Like a Pro",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 90.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tips",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )

                AnimatedContent(
                    targetState = tips[tipIndex],
                    transitionSpec = {
                        (slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(300)))
                            .with(
                                slideOutHorizontally(
                                    animationSpec = tween(250),
                                    targetOffsetX = { -it }
                                ) + fadeOut(animationSpec = tween(250))
                            )
                    },
                    label = "tips"
                ) { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onPrimary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .padding(end = 8.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Virtual Scorecard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onScorecardClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_scorecard),
                            contentDescription = "Scorecard",
                            modifier = Modifier.size(110.dp),
                            colorFilter = ColorFilter.tint(colors.onPrimary)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .padding(start = 8.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "My Bag",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onMyBagClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_mybag),
                            contentDescription = "My Bag",
                            modifier = Modifier.size(110.dp),
                            colorFilter = ColorFilter.tint(colors.onPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlinedOverlayText(
    text: String,
    style: TextStyle
) {
    Box(contentAlignment = Alignment.Center) {
        val outlineOffsets = listOf(
            Pair(-2f, -2f),
            Pair(-2f, 0f),
            Pair(-2f, 2f),
            Pair(0f, -2f),
            Pair(0f, 2f),
            Pair(2f, -2f),
            Pair(2f, 0f),
            Pair(2f, 2f)
        )

        outlineOffsets.forEach { (dx, dy) ->
            Text(
                text = text,
                style = style.copy(
                    color = Color.Black,
                    shadow = Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(dx, dy),
                        blurRadius = 0f
                    )
                )
            )
        }

        Text(
            text = text,
            style = style.copy(color = Color.White)
        )
    }
}