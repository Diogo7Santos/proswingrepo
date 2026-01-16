package com.example.proswing.ui.screens

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyseEditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var frames by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFrame by remember { mutableStateOf<Uri?>(null) }

    // Load the selected frame bitmap (used for both preview + saving)
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Track the editor viewport (card) size in pixels
    var editorSizePx by remember { mutableStateOf(IntSize(0, 0)) }

    // Tagging selections
    val clubTypes = listOf("Driver", "Wood", "Hybrid", "Iron")
    val positions = listOf("Set up", "Top of Backswing", "Impact")
    val perspectives = listOf("Down-the-line", "Face-on")

    var selectedClubType by remember { mutableStateOf<String?>(null) }
    var selectedPosition by remember { mutableStateOf<String?>(null) }
    var selectedPerspective by remember { mutableStateOf<String?>(null) }

    var clubExpanded by remember { mutableStateOf(false) }
    var positionExpanded by remember { mutableStateOf(false) }
    var perspectiveExpanded by remember { mutableStateOf(false) }

    var showPickerDialog by remember { mutableStateOf(false) }

    // Compare dialog
    var showCompareDialog by remember { mutableStateOf(false) }
    var golferQuery by remember { mutableStateOf("") }
    var selectedGolfer by remember { mutableStateOf<String?>(null) }

    val golfers = remember {
        listOf(
            "Adam Scott",
            "Arnold Palmer",
            "Ben Hogan",
            "Bobby Jones",
            "Brooks Koepka",
            "Bryson DeChambeau",
            "Byron Nelson",
            "Dustin Johnson",
            "Ernie Els",
            "Gene Sarazen",
            "GM Golf",
            "Grant Horvat",
            "Gary Player",
            "Hideki Matsuyama",
            "Jack Nicklaus",
            "Jon Rahm",
            "Justin Rose",
            "Lee Trevino",
            "Louis Oosthuizen",
            "Nelly Korda",
            "Nick Faldo",
            "Peter Finch",
            "Phil Mickelson",
            "Rick Shiels",
            "Rory McIlroy",
            "Sam Snead",
            "Scottie Scheffler",
            "Seve Ballesteros",
            "Tiger Woods",
            "Tom Watson",
            "Vijay Singh",
            "Walter Hagen",
            "Xander Schauffele"
        )
    }

    // Editor transform state
    var scale by remember { mutableStateOf(1f) }      // user zoom
    var offsetX by remember { mutableStateOf(0f) }    // user pan in px
    var offsetY by remember { mutableStateOf(0f) }

    fun resetTransforms() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun loadFramesFromProSwingFolder(ctx: Context): List<Uri> {
        val resolver = ctx.contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATA
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection =
                "${MediaStore.Images.Media.RELATIVE_PATH} = ? OR ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
            selectionArgs = arrayOf("Pictures/ProSwing/", "Pictures/ProSwing")
        } else {
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            selectionArgs = arrayOf("%/Pictures/ProSwing/%")
        }

        val result = mutableListOf<Uri>()
        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result.add(ContentUris.withAppendedId(collection, id))
            }
        }
        return result
    }

    suspend fun loadBitmapFromUri(ctx: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun renderEditedBitmap(
        src: Bitmap,
        viewportW: Int,
        viewportH: Int,
        userScale: Float,
        userOffsetX: Float,
        userOffsetY: Float
    ): Bitmap {
        // Output bitmap matches the card size
        val out = Bitmap.createBitmap(viewportW, viewportH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE) // JPEG-friendly background

        // Fit-center base transform (like ContentScale.Fit inside the card)
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val viewW = viewportW.toFloat()
        val viewH = viewportH.toFloat()

        val baseScale = min(viewW / srcW, viewH / srcH)
        val drawnW = srcW * baseScale
        val drawnH = srcH * baseScale
        val baseTx = (viewW - drawnW) / 2f
        val baseTy = (viewH - drawnH) / 2f

        val cx = viewW / 2f
        val cy = viewH / 2f

        val m = Matrix()

        // 1) base fit-center
        m.postScale(baseScale, baseScale)
        m.postTranslate(baseTx, baseTy)

        // 2) user zoom around the center of the viewport
        m.postTranslate(-cx, -cy)
        m.postScale(userScale, userScale)
        m.postTranslate(cx, cy)

        // 3) user pan
        m.postTranslate(userOffsetX, userOffsetY)

        canvas.drawBitmap(src, m, null)
        return out
    }

    suspend fun saveBitmapToGallery(ctx: Context, bitmap: Bitmap): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val resolver = ctx.contentResolver
                val fileName = "proswing_edit_${System.currentTimeMillis()}.jpg"

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProSwing")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext null

                resolver.openOutputStream(uri)?.use { out ->
                    val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    if (!ok) {
                        resolver.delete(uri, null, null)
                        return@withContext null
                    }
                } ?: run {
                    resolver.delete(uri, null, null)
                    return@withContext null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                uri
            } catch (_: Exception) {
                null
            }
        }
    }

    // When frame changes: load bitmap + reset transforms
    LaunchedEffect(selectedFrame) {
        val uri = selectedFrame ?: run {
            selectedBitmap = null
            return@LaunchedEffect
        }
        selectedBitmap = loadBitmapFromUri(context, uri)
        resetTransforms()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Top-left button
            Button(
                onClick = {
                    frames = loadFramesFromProSwingFolder(context)
                    showPickerDialog = true
                },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text("Load swing position")
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 52.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Editor preview area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedFrame == null) {
                        Text(
                            text = "No frame selected.\nTap “Load swing position” to choose a saved frame.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.92f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            // Keep it clipped in the card & measure viewport for saving
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(selectedFrame) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (scale * zoom).coerceIn(1f, 6f)
                                            scale = newScale
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        }
                                    }
                                    .onSizeChanged { editorSizePx = it },
                                contentAlignment = Alignment.Center
                            ) {
                                val bmp = selectedBitmap
                                if (bmp == null) {
                                    CircularProgressIndicator()
                                } else {
                                    // Draw bitmap using Compose so our save matrix matches what you see
                                    androidx.compose.foundation.Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Selected frame",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offsetX,
                                                translationY = offsetY
                                            )
                                    )

                                    // Later: overlay mould here (low opacity) inside the same box.
                                }
                            }
                        }
                    }
                }

                // Bottom row: Compare (left) and Save (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = selectedFrame != null,
                        onClick = {
                            golferQuery = ""
                            showCompareDialog = true
                        }
                    ) {
                        Text("Compare")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = selectedFrame != null && selectedBitmap != null && editorSizePx.width > 0 && editorSizePx.height > 0,
                        onClick = {
                            val bmp = selectedBitmap ?: return@Button
                            val w = editorSizePx.width
                            val h = editorSizePx.height

                            scope.launch {
                                val rendered = withContext(Dispatchers.Default) {
                                    renderEditedBitmap(
                                        src = bmp,
                                        viewportW = w,
                                        viewportH = h,
                                        userScale = scale,
                                        userOffsetX = offsetX,
                                        userOffsetY = offsetY
                                    )
                                }

                                val saved = saveBitmapToGallery(context, rendered)
                                if (saved != null) {
                                    snackbarHostState.showSnackbar("Saved edited image to gallery.")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to save edited image.")
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }

            // Dialog: choose frame + tag it
            if (showPickerDialog) {
                val ready =
                    selectedFrame != null &&
                            selectedClubType != null &&
                            selectedPosition != null &&
                            selectedPerspective != null

                AlertDialog(
                    onDismissRequest = { showPickerDialog = false },
                    title = { Text("Select swing position") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Choose a saved frame:", style = MaterialTheme.typography.bodyMedium)

                            if (frames.isEmpty()) {
                                Text(
                                    text = "No saved frames found in Pictures/ProSwing.\nGo back and press “Save current frame as picture” first.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Card(shape = MaterialTheme.shapes.medium) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp),
                                        contentPadding = PaddingValues(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(frames) { uri ->
                                            val isSelected = uri == selectedFrame
                                            Surface(
                                                tonalElevation = if (isSelected) 4.dp else 0.dp,
                                                shape = MaterialTheme.shapes.small,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedFrame = uri
                                                        selectedClubType = null
                                                        selectedPosition = null
                                                        selectedPerspective = null
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = uri.lastPathSegment ?: uri.toString(),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    if (isSelected) {
                                                        Text(
                                                            text = "Selected",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val hasFrame = selectedFrame != null

                            ExposedDropdownMenuBox(
                                expanded = clubExpanded,
                                onExpandedChange = { if (hasFrame) clubExpanded = !clubExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedClubType ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = hasFrame,
                                    label = { Text("Club type") },
                                    placeholder = { Text("Select club type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clubExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = clubExpanded,
                                    onDismissRequest = { clubExpanded = false }
                                ) {
                                    clubTypes.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedClubType = option
                                                clubExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = positionExpanded,
                                onExpandedChange = { if (hasFrame) positionExpanded = !positionExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedPosition ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = hasFrame,
                                    label = { Text("Key position") },
                                    placeholder = { Text("Select key position") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = positionExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = positionExpanded,
                                    onDismissRequest = { positionExpanded = false }
                                ) {
                                    positions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedPosition = option
                                                positionExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = perspectiveExpanded,
                                onExpandedChange = { if (hasFrame) perspectiveExpanded = !perspectiveExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedPerspective ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = hasFrame,
                                    label = { Text("Video perspective") },
                                    placeholder = { Text("Select perspective") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = perspectiveExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = perspectiveExpanded,
                                    onDismissRequest = { perspectiveExpanded = false }
                                ) {
                                    perspectives.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedPerspective = option
                                                perspectiveExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (!ready) {
                                Text(
                                    text = "Select a frame, then choose club type, key position, and perspective.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPickerDialog = false }) { Text("Cancel") }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = ready,
                            onClick = { showPickerDialog = false }
                        ) { Text("Continue") }
                    }
                )
            }

            // Compare dialog: searchable golfer picker
            if (showCompareDialog) {
                val filtered = remember(golferQuery) {
                    val q = golferQuery.trim()
                    if (q.isEmpty()) golfers else golfers.filter { it.contains(q, ignoreCase = true) }
                }

                AlertDialog(
                    onDismissRequest = { showCompareDialog = false },
                    title = { Text("Compare with a golfer") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = golferQuery,
                                onValueChange = { golferQuery = it },
                                label = { Text("Search") },
                                placeholder = { Text("Type a name…") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Card(shape = MaterialTheme.shapes.medium) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 260.dp),
                                    contentPadding = PaddingValues(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filtered) { name ->
                                        val isSelected = name == selectedGolfer
                                        Surface(
                                            tonalElevation = if (isSelected) 4.dp else 0.dp,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedGolfer = name }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = name, style = MaterialTheme.typography.bodyMedium)
                                                if (isSelected) {
                                                    Text(
                                                        text = "Selected",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Next step: show side-by-side comparison (your frame vs chosen golfer).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCompareDialog = false }) { Text("Cancel") }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = selectedGolfer != null,
                            onClick = { showCompareDialog = false }
                        ) { Text("Continue") }
                    }
                )
            }
        }
    }
}
