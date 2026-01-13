package com.example.proswing.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class Chapter(
    val number: Int,
    val title: String,
    val lessons: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen() {
    val chapters = remember {
        (1..6).map { chapterNumber ->
            Chapter(
                number = chapterNumber,
                title = "Chapter $chapterNumber",
                lessons = (1..10).map { lessonNumber -> "${chapterNumber}.${lessonNumber}" }
            )
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Track which chapters are expanded inside the drawer
    val expandedChapters = remember { mutableStateMapOf<Int, Boolean>() }

    // Selected lesson (null means "Introduction screen")
    var selectedLesson by remember { mutableStateOf<String?>(null) }
    var lessonContent by remember { mutableStateOf("") }

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
                                        .clickable {
                                            expandedChapters[chapter.number] = !isExpanded
                                        }
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
                                            val isSelected = lesson == selectedLesson

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedLesson = lesson
                                                        lessonContent = "Content for lesson $lesson (placeholder)."

                                                        // Close the drawer after selection (feels nicer)
                                                        scope.launch { drawerState.close() }
                                                    }
                                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = lesson,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                                Icon(
                                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                                    contentDescription = "Open lesson"
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
                            text = "Welcome to the ProSwing course template.\n\nUse the right-edge handle to open the chapter list. Select a lesson to start learning. Each lesson will display its content here.",
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
                        Text(
                            text = "Lesson ${selectedLesson!!}",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = lessonContent,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        TextButton(
                            onClick = {
                                selectedLesson = null
                                lessonContent = ""
                            }
                        ) {
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
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .width(18.dp)
                    .height(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        scope.launch { drawerState.open() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Open course contents",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
