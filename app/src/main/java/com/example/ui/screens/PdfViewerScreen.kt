package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.files.FileUtils
import com.example.core.pdf.PdfRendererHelper
import com.example.ui.components.SmallpdfTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfFile: File,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var rotationAngle by remember { mutableStateOf(0) }

    // Zoom & pan
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 4.0f)
        offset += panChange
    }

    LaunchedEffect(pdfFile) {
        withContext(Dispatchers.IO) {
            totalPages = PdfRendererHelper.getPageCount(pdfFile).coerceAtLeast(1)
        }
    }

    LaunchedEffect(pdfFile, currentPage, rotationAngle) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val bmp = PdfRendererHelper.renderPageToBitmap(
                file = pdfFile,
                pageIndex = currentPage,
                scale = 1.8f,
                rotationDegrees = rotationAngle
            )
            currentBitmap = bmp
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = pdfFile.nameWithoutExtension,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { rotationAngle = (rotationAngle + 90) % 360 }
                    ) {
                        Icon(Icons.Filled.RotateRight, contentDescription = "Rotate", tint = SmallpdfNavy)
                    }
                    IconButton(
                        onClick = { FileUtils.shareFile(context, pdfFile) }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = SmallpdfNavy)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                    }

                    Surface(
                        color = SmallpdfInfoBg,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Page ${currentPage + 1} of $totalPages",
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                    }
                }
            }
        },
        containerColor = Color(0xFFE2E8F0)
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .transformable(state = transformState)
                .testTag("pdf_viewer_container"),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = SmallpdfPrimary)
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPage + 1}",
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                )
            } else {
                Text("Failed to render page.", color = SmallpdfTextSecondary)
            }

            // Quick Zoom Controls Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scale = (scale + 0.25f).coerceAtMost(4f)
                    },
                    modifier = Modifier.size(42.dp),
                    containerColor = Color.White,
                    contentColor = SmallpdfNavy
                ) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In")
                }

                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    },
                    modifier = Modifier.size(42.dp),
                    containerColor = Color.White,
                    contentColor = SmallpdfNavy
                ) {
                    Icon(Icons.Filled.FitScreen, contentDescription = "Reset Zoom")
                }
            }
        }
    }
}
