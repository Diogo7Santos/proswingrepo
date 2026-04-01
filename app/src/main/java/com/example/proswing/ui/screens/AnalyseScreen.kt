package com.example.proswing.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun AnalyseScreen(
    onAnalyseClick: () -> Unit = {} // Hook for navigation (you will wire this in AppNavHost)
) {
    val context = LocalContext.current

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedVideoUri = uri
        }
    )

    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }

    // ~1 frame at 30fps. You can change to 16L for 60fps videos.
    val stepMs = 33L

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun stepBy(deltaMs: Long) {
        val exo = playerState.value ?: return
        exo.playWhenReady = false
        val newPos = max(0L, exo.currentPosition + deltaMs)
        exo.seekTo(newPos)
    }

    LaunchedEffect(selectedVideoUri) {
        val uri = selectedVideoUri ?: return@LaunchedEffect
        val exo = playerState.value ?: return@LaunchedEffect

        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.prepare()
        exo.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            playerState.value?.release()
            playerState.value = null
        }
    }

    suspend fun saveCurrentFrameToGallery(
        context: Context,
        videoUri: Uri,
        positionMs: Long
    ): Uri? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)

                val timeUs = positionMs * 1000L
                val bitmap: Bitmap? = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap == null) return@withContext null

                val fileName = "proswing_frame_${System.currentTimeMillis()}.jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProSwing")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val imageUri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext null

                resolver.openOutputStream(imageUri)?.use { out ->
                    val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    if (!ok) {
                        resolver.delete(imageUri, null, null)
                        return@withContext null
                    }
                } ?: run {
                    resolver.delete(imageUri, null, null)
                    return@withContext null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, values, null, null)
                }

                imageUri
            } catch (_: Exception) {
                null
            } finally {
                retriever.release()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Removed the big "Analyse" title so the player has more space.

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
                        playerState.value?.stop()
                    },
                    enabled = selectedVideoUri != null
                ) {
                    Text(
                        "Clear",
                        color = if (selectedVideoUri != null)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            if (selectedVideoUri == null) {
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp), // slightly bigger now that we removed the title
                    shape = MaterialTheme.shapes.large
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val exo = ExoPlayer.Builder(ctx).build().also { exoPlayer ->
                                playerState.value = exoPlayer
                                selectedVideoUri?.let { uri ->
                                    exoPlayer.setMediaItem(MediaItem.fromUri(uri))
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                }
                            }

                            PlayerView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = exo
                                useController = true
                            }
                        },
                        update = { playerView ->
                            playerView.player = playerState.value
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { stepBy(-stepMs) },
                        enabled = playerState.value != null
                    ) {
                        Text("◀ Frame back",
                            color = MaterialTheme.colorScheme.onBackground)
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { stepBy(stepMs) },
                        enabled = playerState.value != null
                    ) {
                        Text("Frame forward ▶",
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = playerState.value != null && selectedVideoUri != null,
                    onClick = {
                        val exo = playerState.value ?: return@Button
                        val uri = selectedVideoUri ?: return@Button

                        exo.playWhenReady = false
                        val positionMs = exo.currentPosition

                        scope.launch {
                            val saved = saveCurrentFrameToGallery(context, uri, positionMs)
                            if (saved != null) {
                                snackbarHostState.showSnackbar("Saved frame to gallery.")
                            } else {
                                snackbarHostState.showSnackbar("Failed to save frame.")
                            }
                        }
                    }
                ) {
                    Text("Save current frame as picture")
                }

                // New Analyse button -> navigates to AnalyseEditorScreen
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    onClick = onAnalyseClick
                ) {
                    Text("Analyse")
                }

                Text(
                    text = "Tip: Frame step uses ~${stepMs}ms increments (approx 1 frame at ~30fps).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
