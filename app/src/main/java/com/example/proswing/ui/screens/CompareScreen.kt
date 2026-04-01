package com.example.proswing.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CompareReferenceImage(
    val label: String,
    @DrawableRes val resId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val editedBitmap = CompareState.editedBitmap

    val referenceImages = listOf(
        CompareReferenceImage("FO setup", com.example.proswing.R.drawable.adamscott_fo_setup),
        CompareReferenceImage("DTL setup", com.example.proswing.R.drawable.adamscott_dtl_setup),
        CompareReferenceImage("FO impact", com.example.proswing.R.drawable.adamscott_fo_impact),
        CompareReferenceImage("DTL impact", com.example.proswing.R.drawable.adamscott_dtl_impact),
        CompareReferenceImage("FO backswing", com.example.proswing.R.drawable.adamscott_fo_backswing),
        CompareReferenceImage("DTL backswing", com.example.proswing.R.drawable.adamscott_dtl_backswing)
    )

    var selectedReference by remember { mutableStateOf<CompareReferenceImage?>(null) }
    var selectedReferenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(selectedReference) {
        selectedReferenceBitmap = selectedReference?.let {
            loadBitmapFromDrawable(context, it.resId)
        }
    }

    val previewBitmap = remember(editedBitmap, selectedReferenceBitmap) {
        val left = editedBitmap
        val right = selectedReferenceBitmap
        if (left != null && right != null) {
            combineBitmapsSideBySide(left, right)
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (editedBitmap == null) {
                Text("No edited image available.")
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back",
                        color = MaterialTheme.colorScheme.onBackground)
                }
                return@Column
            }

            Text(
                text = "Choose a reference. Note: FO = Face On, DTL = Down The Line",
                style = MaterialTheme.typography.bodyLarge
            )

            Card(
                shape = MaterialTheme.shapes.large
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(referenceImages) { item ->
                        val isSelected = selectedReference?.resId == item.resId
                        Surface(
                            tonalElevation = if (isSelected) 4.dp else 0.dp,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReference = item
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.label)
                                if (isSelected) {
                                    Text(
                                        text = "Selected",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (previewBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Comparison preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Select a reference image to preview the side by side comparison.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back",
                        color = MaterialTheme.colorScheme.onBackground)
                }

                Button(
                    onClick = {
                        val finalBitmap = previewBitmap ?: return@Button
                        scope.launch {
                            val saved = saveBitmapToGallery(context, finalBitmap)
                            if (saved != null) {
                                snackbarHostState.showSnackbar("Saved comparison image to gallery.")
                            } else {
                                snackbarHostState.showSnackbar("Failed to save comparison image.")
                            }
                        }
                    },
                    enabled = previewBitmap != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private suspend fun loadBitmapFromDrawable(
    context: Context,
    @DrawableRes resId: Int
): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (_: Exception) {
            null
        }
    }
}

private fun drawBitmapFit(
    canvas: android.graphics.Canvas,
    bitmap: Bitmap,
    dst: RectF
) {
    val srcW = bitmap.width.toFloat()
    val srcH = bitmap.height.toFloat()
    if (srcW <= 0f || srcH <= 0f || dst.width() <= 0f || dst.height() <= 0f) return

    val scale = minOf(dst.width() / srcW, dst.height() / srcH)
    val drawnW = srcW * scale
    val drawnH = srcH * scale
    val left = dst.left + (dst.width() - drawnW) / 2f
    val top = dst.top + (dst.height() - drawnH) / 2f

    val outRect = RectF(left, top, left + drawnW, top + drawnH)
    canvas.drawBitmap(bitmap, null, outRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun combineBitmapsSideBySide(
    leftBitmap: Bitmap,
    rightBitmap: Bitmap
): Bitmap {
    val panelWidth = leftBitmap.width
    val outWidth = panelWidth * 2
    val outHeight = leftBitmap.height

    val out = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawColor(android.graphics.Color.WHITE)

    drawBitmapFit(
        canvas = canvas,
        bitmap = leftBitmap,
        dst = RectF(0f, 0f, panelWidth.toFloat(), outHeight.toFloat())
    )

    drawBitmapFit(
        canvas = canvas,
        bitmap = rightBitmap,
        dst = RectF(panelWidth.toFloat(), 0f, outWidth.toFloat(), outHeight.toFloat())
    )

    return out
}

private suspend fun saveBitmapToGallery(
    ctx: Context,
    bitmap: Bitmap
): Uri? {
    return withContext(Dispatchers.IO) {
        try {
            val resolver = ctx.contentResolver
            val fileName = "proswing_compare_${System.currentTimeMillis()}.jpg"

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