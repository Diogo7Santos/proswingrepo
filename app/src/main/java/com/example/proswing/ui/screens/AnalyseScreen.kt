package com.example.proswing.ui.screens

import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.max

@Composable
fun AnalyseScreen() {
    // Holds the selected video Uri
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery picker (video only)
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedVideoUri = uri
        }
    )

    // Create / manage ExoPlayer tied to the selected Uri
    val player = remember { mutableStateOf<ExoPlayer?>(null) }

    // Step size used for "frame stepping" (approx 1 frame at 30fps)
    val stepMs = 33L

    fun stepBy(deltaMs: Long) {
        val exo = player.value ?: return
        exo.playWhenReady = false // pause to make stepping precise
        val newPos = max(0L, exo.currentPosition + deltaMs)
        exo.seekTo(newPos)
    }

    // Whenever the selected uri changes, (re)build the player media item
    LaunchedEffect(selectedVideoUri) {
        val uri = selectedVideoUri ?: return@LaunchedEffect

        val existing = player.value
        if (existing != null) {
            existing.setMediaItem(MediaItem.fromUri(uri))
            existing.prepare()
            existing.playWhenReady = true
        }
    }

    // Release the player when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            player.value?.release()
            player.value = null
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Analyse",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    pickVideoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            ) {
                Text("Import video")
            }

            OutlinedButton(
                onClick = {
                    selectedVideoUri = null
                    player.value?.stop()
                },
                enabled = selectedVideoUri != null
            ) {
                Text("Clear")
            }
        }

        if (selectedVideoUri == null) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No video selected.\nTap “Import video” to choose a swing video from your gallery.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            // Player container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = MaterialTheme.shapes.large
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        // Create player on first render
                        val exo = ExoPlayer.Builder(context).build().also { exoPlayer ->
                            player.value = exoPlayer

                            // Set initial media
                            selectedVideoUri?.let { uri ->
                                exoPlayer.setMediaItem(MediaItem.fromUri(uri))
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            }
                        }

                        PlayerView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            this.player = exo
                            useController = true
                        }
                    },
                    update = { playerView ->
                        playerView.player = player.value
                    }
                )
            }

            // Frame-step controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { stepBy(-stepMs) },
                    enabled = player.value != null
                ) {
                    Text("◀ Frame back")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { stepBy(stepMs) },
                    enabled = player.value != null
                ) {
                    Text("Frame forward ▶")
                }
            }

            Text(
                text = "Selected: ${selectedVideoUri.toString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Tip: Frame step uses ~${stepMs}ms increments (approx 1 frame at ~30fps).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
