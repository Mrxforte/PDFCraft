package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.core.pdf.PdfRendererHelper
import com.example.core.pdf.WatermarkDeletePreset
import com.example.core.pdf.WatermarkEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkDeleterScreen(
    onNavigateBack: () -> Unit,
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<PdfFileInfo?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPreset by remember { mutableStateOf(WatermarkDeletePreset.BOTTOM_FOOTER) }

    // Custom slider values
    var customTop by remember { mutableStateOf(0.90f) }
    var customBottom by remember { mutableStateOf(1.0f) }

    var isProcessing by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("Preparing document...") }
    var result by remember { mutableStateOf<OperationResult?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val info = FileUtils.getPdfFileInfo(context, uri)
                selectedFile = info
                result = null
                info.localFile?.let { file ->
                    withContext(Dispatchers.IO) {
                        previewBitmap = PdfRendererHelper.renderPageToBitmap(file, 0, scale = 0.6f)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "Watermark Deleter",
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
                                .background(ToolWatermarkDelete),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Erase PDF Watermarks & Stamps",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Remove evaluation banners, footer notes, draft stamps, and unwanted margins.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                            )
                        }
                    }
                }
            }

            if (isProcessing) {
                item {
                    ProcessingProgressCard(currentStep = currentStep)
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
                            previewBitmap = null
                        }
                    )
                }
            } else {
                item {
                    FilePickerCard(
                        selectedFiles = if (selectedFile != null) listOf(selectedFile!!) else emptyList(),
                        onPickFiles = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        onRemoveFile = {
                            selectedFile = null
                            previewBitmap = null
                        },
                        multipleAllowed = false,
                        fileTypeLabel = "PDF"
                    )
                }

                if (selectedFile != null) {
                    // Page Preview
                    if (previewBitmap != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SmallpdfBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Document Page 1 Preview",
                                        fontWeight = FontWeight.Bold,
                                        color = SmallpdfNavy,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Image(
                                        bitmap = previewBitmap!!.asImageBitmap(),
                                        contentDescription = "Page Preview",
                                        modifier = Modifier
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }

                    // Region Presets
                    item {
                        Text(
                            text = "Select Watermark / Stamp Location",
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy,
                            fontSize = 15.sp
                        )
                    }

                    WatermarkDeletePreset.values().forEach { preset ->
                        val isSelected = selectedPreset == preset
                        item {
                            Card(
                                onClick = { selectedPreset = preset },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SmallpdfInfoBg else Color.White
                                ),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) SmallpdfPrimary else SmallpdfBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("watermark_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPreset = preset },
                                        colors = RadioButtonDefaults.colors(selectedColor = SmallpdfPrimary)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            fontWeight = FontWeight.Bold,
                                            color = SmallpdfNavy,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = preset.description,
                                            style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // If custom region selected, show sliders
                    if (selectedPreset == WatermarkDeletePreset.CUSTOM_REGION) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SmallpdfBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Custom Boundary Sliders",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SmallpdfNavy
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Top Margin Cut: ${(customTop * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        color = SmallpdfTextSecondary
                                    )
                                    Slider(
                                        value = customTop,
                                        onValueChange = { customTop = it },
                                        valueRange = 0f..1f
                                    )
                                    Text(
                                        text = "Bottom Margin Cut: ${(customBottom * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        color = SmallpdfTextSecondary
                                    )
                                    Slider(
                                        value = customBottom,
                                        onValueChange = { customBottom = it },
                                        valueRange = 0f..1f
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val file = selectedFile?.localFile ?: return@Button
                                scope.launch {
                                    isProcessing = true
                                    currentStep = "Erase watermark areas across ${selectedFile?.pageCount} page(s)..."
                                    val res = WatermarkEngine.removeWatermark(
                                        context = context,
                                        inputFile = file,
                                        preset = selectedPreset,
                                        customTopPercent = customTop,
                                        customBottomPercent = customBottom
                                    )
                                    isProcessing = false
                                    result = res
                                    if (res.isSuccess && res.outputFile != null) {
                                        SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                            HistoryEntity(
                                                title = res.outputFile.name,
                                                toolType = "Watermark Deleter",
                                                originalSizeBytes = res.originalSizeBytes,
                                                resultSizeBytes = res.outputSizeBytes,
                                                filePath = res.outputFile.absolutePath,
                                                fileUri = res.outputFile.toURI().toString()
                                            )
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ToolWatermarkDelete),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("execute_watermark_delete_button")
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clean & Erase Watermark",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
