package com.example.proswing.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private data class Lesson(
    val id: String,
    val title: String,
    val youtubeUrl: String? = null,
    val startSec: Int? = null,
    val endSec: Int? = null
)

private data class Chapter(
    val number: Int,
    val title: String,
    val lessons: List<Lesson>
)

private fun parseTimeToSeconds(time: String): Int? {
    val t = time.trim()
    val parts = t.split(":")
    return try {
        when (parts.size) {
            2 -> {
                val m = parts[0].toInt()
                val s = parts[1].toInt()
                (m * 60) + s
            }
            3 -> {
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                val s = parts[2].toInt()
                (h * 3600) + (m * 60) + s
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun extractYoutubeId(url: String): String? {
    return try {
        val uri = Uri.parse(url)
        if (uri.host?.contains("youtu.be") == true) {
            uri.lastPathSegment
        } else {
            uri.getQueryParameter("v")
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun YoutubeEmbed(
    youtubeUrl: String,
    startSeconds: Int? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoId = remember(youtubeUrl) { extractYoutubeId(youtubeUrl) }

    if (videoId == null) {
        Text(
            text = "Invalid YouTube link.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    val start = (startSeconds ?: 0).coerceAtLeast(0)

    // Reset error state whenever a new URL is selected
    var embedFailed by remember(youtubeUrl) { mutableStateOf(false) }

    // Keep refs so we can cue/load when lesson changes
    var playerView: YouTubePlayerView? by remember { mutableStateOf(null) }
    var youTubePlayer: YouTubePlayer? by remember { mutableStateOf(null) }

    // If the player never becomes ready (common when init fails), show fallback after a few seconds.
    LaunchedEffect(videoId, start) {
        embedFailed = false
        youTubePlayer = null

        // Give it a moment to initialise; if still not ready, show fallback.
        delay(4000)
        if (youTubePlayer == null) {
            embedFailed = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            playerView?.let { view ->
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            }
            playerView = null
            youTubePlayer = null
        }
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    YouTubePlayerView(ctx).also { view ->
                        playerView = view

                        // MANUAL INITIALIZATION (more reliable in Compose than auto init)
                        view.enableAutomaticInitialization = false

                        val iFramePlayerOptions = IFramePlayerOptions.Builder()
                            .controls(1)
                            .build()

                        view.initialize(
                            object : AbstractYouTubePlayerListener() {
                                override fun onReady(player: YouTubePlayer) {
                                    youTubePlayer = player
                                    // cueVideo avoids autoplay restrictions
                                    player.cueVideo(videoId, start.toFloat())
                                }

                                override fun onError(
                                    player: YouTubePlayer,
                                    error: PlayerConstants.PlayerError
                                ) {
                                    embedFailed = true
                                }
                            },
                            true,
                            iFramePlayerOptions
                        )

                        lifecycleOwner.lifecycle.addObserver(view)
                    }
                },
                update = {
                    // When user selects a different lesson, update the mounted player
                    youTubePlayer?.cueVideo(videoId, start.toFloat())
                }
            )

            if (embedFailed) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "This video can’t be played inside the app (player init failed or embedding disabled).",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val ctx = playerView?.context
                            if (ctx != null) {
                                val sep = if (youtubeUrl.contains("?")) "&" else "?"
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    Uri.parse(youtubeUrl + sep + "t=${start}s")
                                )
                                ctx.startActivity(intent)
                            }
                        }
                    ) {
                        Text("Open in YouTube")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen() {

    val chapters = remember {
        listOf(
            Chapter(
                number = 1,
                title = "Chapter 1: Basics",
                lessons = listOf(
                    Lesson(
                        id = "1.1",
                        title = "The grip",
                        youtubeUrl = "https://www.youtube.com/watch?v=hhnhHgvOaB0"
                    ),
                    Lesson(
                        id = "1.2",
                        title = "Athletic setup",
                        youtubeUrl = "https://www.youtube.com/watch?v=3fpzZr_w56M",
                        startSec = parseTimeToSeconds("1:31"),
                        endSec = parseTimeToSeconds("2:57")
                    ),
                    Lesson(
                        id = "1.3",
                        title = "A bit more detail on the set up.",
                        youtubeUrl = "https://www.youtube.com/watch?v=oandn2z-KwA"
                    ),
                    Lesson(
                        id = "1.3b",
                        title = "Coordination",
                        youtubeUrl = "https://www.youtube.com/watch?v=3fpzZr_w56M",
                        startSec = parseTimeToSeconds("2:57"),
                        endSec = parseTimeToSeconds("4:00")
                    ),
                    Lesson(
                        id = "1.4",
                        title = "Move your feet",
                        youtubeUrl = "https://www.youtube.com/watch?v=3fpzZr_w56M",
                        startSec = parseTimeToSeconds("4:00"),
                        endSec = parseTimeToSeconds("5:14")
                    ),
                    Lesson(
                        id = "1.5",
                        title = "Strip the swing down",
                        youtubeUrl = "https://www.youtube.com/watch?v=3fpzZr_w56M",
                        startSec = parseTimeToSeconds("5:14"),
                        endSec = parseTimeToSeconds("6:44")
                    ),
                    Lesson(
                        id = "1.6",
                        title = "Include the golf club",
                        youtubeUrl = "https://www.youtube.com/watch?v=3fpzZr_w56M",
                        startSec = parseTimeToSeconds("6:44"),
                        endSec = parseTimeToSeconds("8:04")
                    ),
                    Lesson(
                        id = "1.7",
                        title = "Putting everything together",
                        youtubeUrl = "https://www.youtube.com/watch?v=3fpzZr_w56M",
                        startSec = parseTimeToSeconds("8:04"),
                        endSec = parseTimeToSeconds("11:02")
                    ),
                    Lesson(
                        id = "1.8",
                        title = "Recap with a pro",
                        youtubeUrl = "https://www.youtube.com/watch?v=24OoFmZiYbU",
                        startSec = 0
                    ),
                    Lesson(
                        id = "1.9",
                        title = "Don't forget to warm up",
                        youtubeUrl = "https://www.youtube.com/watch?v=pZRBJkvrlz4",
                        startSec = parseTimeToSeconds("0:38"),
                        endSec = parseTimeToSeconds("1:36")
                    ),
                    Lesson(
                        id = "1.10",
                        title = "Time to practice!",
                        youtubeUrl = null
                    )
                )
            )
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val expandedChapters = remember { mutableStateMapOf<Int, Boolean>() }

    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(340.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Course Content",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Divider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chapters) { chapter ->
                        val isExpanded = expandedChapters[chapter.number] == true

                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedChapters[chapter.number] = !isExpanded }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        chapter.lessons.forEach { lesson ->
                                            val isSelected = lesson.id == selectedLesson?.id

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedLesson = lesson
                                                        scope.launch { drawerState.close() }
                                                    }
                                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${lesson.id}  ${lesson.title}",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                                Icon(
                                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                                    contentDescription = "Open lesson",
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
    ) {
        val scrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedLesson == null) {
                        Text(
                            text = "Learn",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "Chapter 1 is wired with titles + YouTube clips.\n\nUse the right-edge handle to open the chapter list and select a lesson.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Nothing selected yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val lesson = selectedLesson!!

                        Text(
                            text = "Lesson ${lesson.id}: ${lesson.title}",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        if (lesson.youtubeUrl != null) {
                            val start = lesson.startSec
                            val end = lesson.endSec
                            val rangeText = when {
                                start != null && end != null -> "Clip: ${start}s → ${end}s"
                                start != null && end == null -> "Clip starts at: ${start}s"
                                else -> "Full video"
                            }
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            YoutubeEmbed(
                                youtubeUrl = lesson.youtubeUrl,
                                startSeconds = lesson.startSec
                            )
                        } else {
                            Text(
                                text = "This lesson has no video yet.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        TextButton(onClick = { selectedLesson = null }) {
                            Text("Back to introduction")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tip: Use the right-edge handle to open the lesson list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right-edge "handle" to open the drawer
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = 6.dp)
                    .width(18.dp)
                    .height(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { scope.launch { drawerState.open() } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Open course contents",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

        }
    }
}
