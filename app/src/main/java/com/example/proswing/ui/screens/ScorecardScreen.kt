package com.example.proswing.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proswing.data.ScorecardEntity
import com.example.proswing.viewmodel.Hole
import com.example.proswing.viewmodel.ScorecardViewModel
import com.example.proswing.viewmodel.SettingsViewModel
import java.io.File
import java.io.FileOutputStream

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
    val context = LocalContext.current

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Round on ${round.date}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    "Gross: ${round.totalScore} | Net: ${round.netScore} | To Par: ${round.toPar}",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            IconButton(
                                onClick = { shareScorecardImage(context, round) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share scorecard",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

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

private fun shareScorecardImage(context: Context, round: ScorecardEntity) {
    val bitmap = createScorecardBitmap(round)
    val uri = saveBitmapToCache(context, bitmap) ?: return

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Golf Scorecard - ${round.date}")
        putExtra(Intent.EXTRA_TEXT, "My golf round from ${round.date}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share scorecard"))
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val cachePath = File(context.cacheDir, "shared_scorecards")
        cachePath.mkdirs()

        val file = File(cachePath, "scorecard_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createScorecardBitmap(round: ScorecardEntity): Bitmap {
    val width = 1200
    val rowHeight = 90
    val headerHeight = 220
    val summaryHeight = 120
    val tableRows = 3
    val height = headerHeight + summaryHeight + (tableRows * rowHeight) + 80

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 34f
    }

    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 3f
    }

    canvas.drawText("Golf Scorecard", 40f, 70f, titlePaint)
    canvas.drawText("Date: ${round.date}", 40f, 130f, bodyPaint)
    canvas.drawText(
        "Gross: ${round.totalScore}    Net: ${round.netScore}    To Par: ${round.toPar}",
        40f,
        190f,
        bodyPaint
    )

    val startY = 260f
    val labelWidth = 140f
    val availableWidth = width - 80f - labelWidth
    val colWidth = availableWidth / round.holes.size

    fun drawRow(label: String, values: List<String>, rowIndex: Int) {
        val yTop = startY + rowIndex * rowHeight
        val yText = yTop + 55f

        canvas.drawText(label, 40f, yText, boldPaint)

        values.forEachIndexed { index, value ->
            val x = 40f + labelWidth + (index * colWidth) + (colWidth / 2)
            val textWidth = bodyPaint.measureText(value)
            canvas.drawText(value, x - textWidth / 2, yText, bodyPaint)
        }

        canvas.drawLine(40f, yTop + rowHeight - 10f, width - 40f, yTop + rowHeight - 10f, linePaint)
    }

    drawRow("Hole", round.holes.indices.map { (it + 1).toString() }, 0)
    drawRow("Par", round.pars.map { it.toString() }, 1)
    drawRow("Score", round.holes.map { it.toString() }, 2)

    return bitmap
}