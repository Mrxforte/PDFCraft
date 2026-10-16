package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.HistoryEntity
import com.example.core.database.SmallpdfDatabase
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.example.core.model.PdfFileInfo
import com.example.core.pdf.PdfSignEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

data class DrawPoint(val offset: Offset, val isStart: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignScreen(
    onNavigateBack: () -> Unit,
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<PdfFileInfo?>(null) }
    var targetPageIndex by remember { mutableStateOf(0) }
    var sigXPercent by remember { mutableStateOf(0.5f) }
    var sigYPercent by remember { mutableStateOf(0.85f) }
    var sigWidthPercent by remember { mutableStateOf(0.35f) }

    // Touch signature paths
    val signaturePoints = remember { mutableStateListOf<DrawPoint>() }

    var isProcessing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<OperationResult?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                selectedFile = FileUtils.getPdfFileInfo(context, uri)
                result = null
            }
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "Sign PDF",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = SmallpdfBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SmallpdfInfoBg),
                    border = BorderStroke(1.dp, SmallpdfBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ToolSign),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Gesture, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Create & Place Your Signature",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Draw your e-signature and place it on any page with exact positioning.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                            )
                        }
                    }
                }
            }

            if (isProcessing) {
                item {
                    ProcessingProgressCard(currentStep = "Stamping signature onto PDF page...")
                }
            } else if (result != null) {
                item {
                    SuccessResultCard(
                        result = result!!,
                        onOpenFile = { result?.outputFile?.let { onViewPdf(it) } },
                        onShareFile = { result?.outputFile?.let { FileUtils.shareFile(context, it) } },
                        onProcessAnother = {
                            result = null
                            selectedFile = null
                            signaturePoints.clear()
                        }
                    )
                }
            } else {
                item {
                    FilePickerCard(
                        selectedFiles = if (selectedFile != null) listOf(selectedFile!!) else emptyList(),
                        onPickFiles = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        onRemoveFile = { selectedFile = null },
                        multipleAllowed = false
                    )
                }

                if (selectedFile != null) {
                    // Signature Drawing Pad
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SmallpdfBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Draw Your Signature",
                                        fontWeight = FontWeight.Bold,
                                        color = SmallpdfNavy,
                                        fontSize = 15.sp
                                    )
                                    TextButton(onClick = { signaturePoints.clear() }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear", color = SmallpdfError, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Drawing Canvas Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    signaturePoints.add(DrawPoint(offset, isStart = true))
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    signaturePoints.add(DrawPoint(change.position, isStart = false))
                                                }
                                            )
                                        }
                                        .testTag("signature_canvas")
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        for (i in 0 until signaturePoints.size - 1) {
                                            val p1 = signaturePoints[i]
                                            val p2 = signaturePoints[i + 1]
                                            if (!p2.isStart) {
                                                drawLine(
                                                    color = Color.Black,
                                                    start = p1.offset,
                                                    end = p2.offset,
                                                    strokeWidth = 5f,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }

                                    if (signaturePoints.isEmpty()) {
                                        Text(
                                            text = "✍️ Sign your name here using touch",
                                            color = SmallpdfTextSecondary,
                                            fontSize = 13.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Placement Controls
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SmallpdfBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Signature Placement",
                                    fontWeight = FontWeight.Bold,
                                    color = SmallpdfNavy,
                                    fontSize = 15.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Page Selection
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Apply on Page:", fontSize = 13.sp, color = SmallpdfTextSecondary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    for (p in 0 until selectedFile!!.pageCount.coerceAtMost(5)) {
                                        FilterChip(
                                            selected = targetPageIndex == p,
                                            onClick = { targetPageIndex = p },
                                            label = { Text("P${p + 1}") }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Horizontal Position: ${(sigXPercent * 100).toInt()}%", fontSize = 12.sp, color = SmallpdfTextSecondary)
                                Slider(value = sigXPercent, onValueChange = { sigXPercent = it }, valueRange = 0.1f..0.9f)

                                Text("Vertical Position: ${(sigYPercent * 100).toInt()}%", fontSize = 12.sp, color = SmallpdfTextSecondary)
                                Slider(value = sigYPercent, onValueChange = { sigYPercent = it }, valueRange = 0.1f..0.9f)
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val file = selectedFile?.localFile ?: return@Button
                                if (signaturePoints.isEmpty()) return@Button

                                scope.launch {
                                    isProcessing = true

                                    // Render drawn points into a Bitmap
                                    val sigBitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(sigBitmap)
                                    val paint = Paint().apply {
                                        color = android.graphics.Color.BLACK
                                        strokeWidth = 6f
                                        style = Paint.Style.STROKE
                                        strokeCap = Paint.Cap.ROUND
                                        strokeJoin = Paint.Join.ROUND
                                        isAntiAlias = true
                                    }

                                    val path = Path()
                                    var started = false
                                    for (pt in signaturePoints) {
                                        if (pt.isStart || !started) {
                                            path.moveTo(pt.offset.x, pt.offset.y)
                                            started = true
                                        } else {
                                            path.lineTo(pt.offset.x, pt.offset.y)
                                        }
                                    }
                                    canvas.drawPath(path, paint)

                                    val res = PdfSignEngine.applySignatureToPdf(
                                        context = context,
                                        inputFile = file,
                                        signatureBitmap = sigBitmap,
                                        targetPageIndex = targetPageIndex,
                                        xPercent = sigXPercent,
                                        yPercent = sigYPercent,
                                        widthPercent = sigWidthPercent
                                    )

                                    isProcessing = false
                                    result = res

                                    if (res.isSuccess && res.outputFile != null) {
                                        SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                            HistoryEntity(
                                                title = res.outputFile.name,
                                                toolType = "Sign PDF",
                                                originalSizeBytes = file.length(),
                                                resultSizeBytes = res.outputSizeBytes,
                                                filePath = res.outputFile.absolutePath,
                                                fileUri = res.outputFile.toURI().toString()
                                            )
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ToolSign),
                            enabled = signaturePoints.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("apply_signature_button")
                        ) {
                            Icon(Icons.Filled.Gesture, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign & Export PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
