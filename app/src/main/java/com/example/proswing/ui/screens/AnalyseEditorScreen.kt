package com.example.proswing.ui.screens

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

private data class LineOverlay(
    val id: Long,
    val centerX: Float,
    val centerY: Float,
    val lengthPx: Float,
    val rotationDeg: Float,
    val colorArgb: Int,
    val strokePx: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyseEditorScreen(
    /**
     * OPTIONAL hook:
     * Wire this up in your AppNavHost/ModalNavigationDrawer so that when cropMode is ON,
     * you set drawer gesturesEnabled = false and/or close the drawer.
     */
    onRequestLockDrawerGestures: (lock: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var frames by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFrame by remember { mutableStateOf<Uri?>(null) }

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
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

    // Image transform state (pinch zoom + pan) — only active while Crop mode is ON
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    fun resetImageTransforms() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Toolbox state
    var templateEnabled by remember { mutableStateOf(false) }
    var cropMode by remember { mutableStateOf(true) } // start in crop mode
    var toolsVisible by remember { mutableStateOf(true) }

    // Ask host to lock drawer gestures while cropping
    LaunchedEffect(cropMode) {
        onRequestLockDrawerGestures(cropMode)
    }

    // Overlay lines state
    var lines by remember { mutableStateOf<List<LineOverlay>>(emptyList()) }
    var selectedLineId by remember { mutableStateOf<Long?>(null) }

    val lineColors = listOf(
        0xFFFF3B30.toInt(), // red
        0xFF34C759.toInt(), // green
        0xFF007AFF.toInt(), // blue
        0xFFFFCC00.toInt(), // yellow
        0xFFFFFFFF.toInt()  // white
    )

    fun addLine() {
        val w = editorSizePx.width.toFloat().coerceAtLeast(1f)
        val h = editorSizePx.height.toFloat().coerceAtLeast(1f)

        val new = LineOverlay(
            id = System.currentTimeMillis(),
            centerX = w / 2f,
            centerY = h / 2f,
            lengthPx = min(w, h) * 0.55f,
            rotationDeg = 0f,
            colorArgb = lineColors.first(),
            strokePx = max(6f, min(w, h) * 0.008f)
        )
        lines = lines + new
        selectedLineId = new.id
        cropMode = false
    }

    fun updateSelectedLine(transform: (LineOverlay) -> LineOverlay) {
        val id = selectedLineId ?: return
        lines = lines.map { if (it.id == id) transform(it) else it }
    }

    fun deleteSelectedLine() {
        val id = selectedLineId ?: return
        lines = lines.filterNot { it.id == id }
        selectedLineId = null
    }

    fun selectedLine(): LineOverlay? = lines.firstOrNull { it.id == selectedLineId }

    // --- MediaStore / loading helpers ---
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

    fun loadTemplateBitmap(ctx: Context, perspective: String?): Bitmap? {
        val resId = when (perspective) {
            "Down-the-line" -> com.example.proswing.R.drawable.setup_template_dtl
            "Face-on" -> com.example.proswing.R.drawable.setup_template_fo
            else -> null
        } ?: return null

        return try {
            BitmapFactory.decodeResource(ctx.resources, resId)
        } catch (_: Exception) {
            null
        }
    }

    fun renderEditedBitmap(
        src: Bitmap,
        template: Bitmap?,
        viewportW: Int,
        viewportH: Int,
        userScale: Float,
        userOffsetX: Float,
        userOffsetY: Float,
        drawTemplate: Boolean,
        templateAlpha: Int,
        linesToDraw: List<LineOverlay>
    ): Bitmap {
        val out = Bitmap.createBitmap(viewportW, viewportH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)

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
        m.postScale(baseScale, baseScale)
        m.postTranslate(baseTx, baseTy)

        m.postTranslate(-cx, -cy)
        m.postScale(userScale, userScale)
        m.postTranslate(cx, cy)

        m.postTranslate(userOffsetX, userOffsetY)

        canvas.drawBitmap(src, m, null)

        if (drawTemplate && template != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = templateAlpha }
            val dst = Rect(0, 0, viewportW, viewportH)
            canvas.drawBitmap(template, null, dst, paint)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        for (line in linesToDraw) {
            paint.color = line.colorArgb
            paint.strokeWidth = line.strokePx

            val rad = Math.toRadians(line.rotationDeg.toDouble())
            val dx = (cos(rad) * (line.lengthPx / 2f)).toFloat()
            val dy = (sin(rad) * (line.lengthPx / 2f)).toFloat()

            val x1 = line.centerX - dx
            val y1 = line.centerY - dy
            val x2 = line.centerX + dx
            val y2 = line.centerY + dy

            canvas.drawLine(x1, y1, x2, y2, paint)
        }

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

    // Load bitmap when selection changes
    LaunchedEffect(selectedFrame) {
        val uri = selectedFrame ?: run {
            selectedBitmap = null
            return@LaunchedEffect
        }
        selectedBitmap = loadBitmapFromUri(context, uri)
        resetImageTransforms()
        lines = emptyList()
        selectedLineId = null
        templateEnabled = false
        cropMode = true
        toolsVisible = true
    }

    suspend fun getTemplateForSelectedPerspective(): Bitmap? {
        return withContext(Dispatchers.IO) {
            loadTemplateBitmap(context, selectedPerspective)
        }
    }

    // --- Line hit test (tap near a line to select it) ---
    fun distancePointToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay
        val abLenSq = abx * abx + aby * aby
        if (abLenSq <= 0.0001f) return hypot(px - ax, py - ay)
        val t = (apx * abx + apy * aby) / abLenSq
        val clamped = t.coerceIn(0f, 1f)
        val cx = ax + abx * clamped
        val cy = ay + aby * clamped
        return hypot(px - cx, py - cy)
    }

    fun pickLineAt(pos: Offset): Long? {
        val threshold = 28f
        var bestId: Long? = null
        var bestDist = Float.MAX_VALUE

        for (line in lines) {
            val rad = Math.toRadians(line.rotationDeg.toDouble())
            val dx = (cos(rad) * (line.lengthPx / 2f)).toFloat()
            val dy = (sin(rad) * (line.lengthPx / 2f)).toFloat()
            val x1 = line.centerX - dx
            val y1 = line.centerY - dy
            val x2 = line.centerX + dx
            val y2 = line.centerY + dy

            val d = distancePointToSegment(pos.x, pos.y, x1, y1, x2, y2)
            if (d < bestDist) {
                bestDist = d
                bestId = line.id
            }
        }
        return if (bestDist <= threshold) bestId else null
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 52.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Editor area
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
                        // WRAP: put the visibility button ABOVE the card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.92f)
                        ) {
                            // Visibility toggle row (top-right) ABOVE the editor card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { toolsVisible = !toolsVisible }) {
                                    Icon(
                                        imageVector = if (toolsVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (toolsVisible) "Hide tools" else "Show tools"
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                // IMPORTANT: clip here so the whole editor behaves like your old version
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.large)
                                        .onSizeChanged { editorSizePx = it }
                                ) {
                                    val bmp = selectedBitmap
                                    if (bmp == null) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    } else {
                                        // --- IMAGE layer: ONLY this receives transform gestures when cropMode is true ---
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(selectedFrame, cropMode) {
                                                    if (!cropMode) return@pointerInput
                                                    detectTransformGestures { _, pan, zoom, _ ->
                                                        scale = (scale * zoom).coerceIn(1f, 6f)
                                                        offsetX += pan.x
                                                        offsetY += pan.y
                                                    }
                                                }
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawContext.canvas.nativeCanvas.apply {
                                                    val canvasW = size.width
                                                    val canvasH = size.height

                                                    val srcW = bmp.width.toFloat()
                                                    val srcH = bmp.height.toFloat()

                                                    val baseScale = min(canvasW / srcW, canvasH / srcH)
                                                    val drawnW = srcW * baseScale
                                                    val drawnH = srcH * baseScale
                                                    val baseTx = (canvasW - drawnW) / 2f
                                                    val baseTy = (canvasH - drawnH) / 2f

                                                    val cx = canvasW / 2f
                                                    val cy = canvasH / 2f

                                                    val m = Matrix()
                                                    m.postScale(baseScale, baseScale)
                                                    m.postTranslate(baseTx, baseTy)

                                                    // zoom about center
                                                    m.postTranslate(-cx, -cy)
                                                    m.postScale(scale, scale)
                                                    m.postTranslate(cx, cy)

                                                    // pan
                                                    m.postTranslate(offsetX, offsetY)

                                                    drawBitmap(bmp, m, null)
                                                }
                                            }
                                        }

                                        // --- TEMPLATE overlay: never intercept touches ---
                                        if (templateEnabled) {
                                            val resId = when (selectedPerspective) {
                                                "Down-the-line" -> com.example.proswing.R.drawable.setup_template_dtl
                                                "Face-on" -> com.example.proswing.R.drawable.setup_template_fo
                                                else -> null
                                            }
                                            if (resId != null) {
                                                androidx.compose.foundation.Image(
                                                    painter = androidx.compose.ui.res.painterResource(id = resId),
                                                    contentDescription = "Template overlay",
                                                    modifier = Modifier.fillMaxSize(),
                                                    alpha = 1f
                                                )
                                            }
                                        }

                                        // --- LINES overlay ---
                                        // CRITICAL FIX:
                                        // Don't keep a full-size pointerInput layer on top when cropMode is ON.
                                        // We conditionally add pointerInput only when cropMode is OFF.
                                        if (!cropMode) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .pointerInput(lines) {
                                                        detectTapGestures { tap ->
                                                            selectedLineId = pickLineAt(tap)
                                                        }
                                                    }
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    for (line in lines) {
                                                        val rad = Math.toRadians(line.rotationDeg.toDouble())
                                                        val dx = (cos(rad) * (line.lengthPx / 2f)).toFloat()
                                                        val dy = (sin(rad) * (line.lengthPx / 2f)).toFloat()
                                                        val p1 = Offset(line.centerX - dx, line.centerY - dy)
                                                        val p2 = Offset(line.centerX + dx, line.centerY + dy)

                                                        drawLine(
                                                            color = Color(line.colorArgb),
                                                            start = p1,
                                                            end = p2,
                                                            strokeWidth = line.strokePx,
                                                            cap = StrokeCap.Round
                                                        )

                                                        if (line.id == selectedLineId) {
                                                            drawCircle(
                                                                color = Color(line.colorArgb),
                                                                radius = max(10f, line.strokePx * 1.2f),
                                                                center = Offset(line.centerX, line.centerY)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // cropMode ON: draw only (no pointerInput)
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                for (line in lines) {
                                                    val rad = Math.toRadians(line.rotationDeg.toDouble())
                                                    val dx = (cos(rad) * (line.lengthPx / 2f)).toFloat()
                                                    val dy = (sin(rad) * (line.lengthPx / 2f)).toFloat()
                                                    val p1 = Offset(line.centerX - dx, line.centerY - dy)
                                                    val p2 = Offset(line.centerX + dx, line.centerY + dy)

                                                    drawLine(
                                                        color = Color(line.colorArgb),
                                                        start = p1,
                                                        end = p2,
                                                        strokeWidth = line.strokePx,
                                                        cap = StrokeCap.Round
                                                    )

                                                    if (line.id == selectedLineId) {
                                                        drawCircle(
                                                            color = Color(line.colorArgb),
                                                            radius = max(10f, line.strokePx * 1.2f),
                                                            center = Offset(line.centerX, line.centerY)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // --- TOOLBOX (scrollable) ---
                                        if (toolsVisible) {
                                            Card(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .padding(10.dp)
                                                    .width(170.dp)
                                                    .heightIn(max = 520.dp),
                                                shape = RoundedCornerShape(18.dp)
                                            ) {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    item { Text("Tools", style = MaterialTheme.typography.titleSmall) }

                                                    item {
                                                        OutlinedButton(
                                                            onClick = { cropMode = !cropMode },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            enabled = selectedFrame != null
                                                        ) {
                                                            Text(if (cropMode) "Crop: ON" else "Crop: OFF",
                                                                color = MaterialTheme.colorScheme.onBackground)
                                                        }
                                                    }

                                                    item {
                                                        Button(
                                                            onClick = { addLine() },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            enabled = editorSizePx.width > 0 && editorSizePx.height > 0
                                                        ) { Text("Add line") }
                                                    }

                                                    item {
                                                        OutlinedButton(
                                                            onClick = {
                                                                templateEnabled = !templateEnabled
                                                                cropMode = false
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            enabled = selectedPerspective != null
                                                        ) {
                                                            Text(if (templateEnabled) "Template: ON" else "Template: OFF",
                                                                color = MaterialTheme.colorScheme.onBackground)
                                                        }
                                                    }

                                                    val sel = selectedLine()
                                                    if (sel != null) {
                                                        item {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    val idx = lineColors.indexOf(sel.colorArgb).let { if (it < 0) 0 else it }
                                                                    val next = lineColors[(idx + 1) % lineColors.size]
                                                                    updateSelectedLine { it.copy(colorArgb = next) }
                                                                    cropMode = false
                                                                },
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) { Text("Line colour") }
                                                        }

                                                        item { Text("Move X", style = MaterialTheme.typography.labelSmall) }
                                                        item {
                                                            val maxX = editorSizePx.width.toFloat().coerceAtLeast(1f)
                                                            Slider(
                                                                value = sel.centerX.coerceIn(0f, maxX),
                                                                onValueChange = { v ->
                                                                    updateSelectedLine { it.copy(centerX = v.coerceIn(0f, maxX)) }
                                                                },
                                                                valueRange = 0f..maxX
                                                            )
                                                        }

                                                        item { Text("Move Y", style = MaterialTheme.typography.labelSmall) }
                                                        item {
                                                            val maxY = editorSizePx.height.toFloat().coerceAtLeast(1f)
                                                            Slider(
                                                                value = sel.centerY.coerceIn(0f, maxY),
                                                                onValueChange = { v ->
                                                                    updateSelectedLine { it.copy(centerY = v.coerceIn(0f, maxY)) }
                                                                },
                                                                valueRange = 0f..maxY
                                                            )
                                                        }

                                                        item { Text("Rotate", style = MaterialTheme.typography.labelSmall) }
                                                        item {
                                                            Slider(
                                                                value = sel.rotationDeg,
                                                                onValueChange = { v -> updateSelectedLine { it.copy(rotationDeg = v) } },
                                                                valueRange = -180f..180f
                                                            )
                                                        }

                                                        item { Text("Length", style = MaterialTheme.typography.labelSmall) }
                                                        item {
                                                            Slider(
                                                                value = sel.lengthPx,
                                                                onValueChange = { v ->
                                                                    updateSelectedLine { it.copy(lengthPx = v.coerceIn(80f, 2000f)) }
                                                                },
                                                                valueRange = 80f..2000f
                                                            )
                                                        }

                                                        item {
                                                            OutlinedButton(
                                                                onClick = { deleteSelectedLine() },
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) { Text("Delete")}
                                                        }
                                                    } else {
                                                        item {
                                                            Text(
                                                                text = if (cropMode) "Crop mode: pinch + pan image"
                                                                else "Tap a line to edit",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    item { Spacer(modifier = Modifier.height(6.dp)) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Save button
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedFrame != null &&
                            selectedBitmap != null &&
                            editorSizePx.width > 0 &&
                            editorSizePx.height > 0,
                    onClick = {
                        val bmp = selectedBitmap ?: return@Button
                        val w = editorSizePx.width
                        val h = editorSizePx.height

                        scope.launch {
                            val templateBmp = if (templateEnabled) getTemplateForSelectedPerspective() else null

                            val rendered = withContext(Dispatchers.Default) {
                                renderEditedBitmap(
                                    src = bmp,
                                    template = templateBmp,
                                    viewportW = w,
                                    viewportH = h,
                                    userScale = scale,
                                    userOffsetX = offsetX,
                                    userOffsetY = offsetY,
                                    drawTemplate = templateEnabled,
                                    templateAlpha = 64,
                                    linesToDraw = lines
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

            // Picker dialog
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
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
        }
    }
}
