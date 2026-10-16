package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.HistoryEntity
import com.example.core.database.SmallpdfDatabase
import com.example.core.files.FileUtils
import com.example.core.model.CompressionLevel
import com.example.core.model.OperationResult
import com.example.core.model.PdfFileInfo
import com.example.core.pdf.PdfCompressEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(
    onNavigateBack: () -> Unit,
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<PdfFileInfo?>(null) }
    var selectedLevel by remember { mutableStateOf(CompressionLevel.BALANCED) }
    var isProcessing by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("Analyzing document...") }
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
                title = "Compress PDF",
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
                                .background(ToolCompress),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Compress, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart PDF Compression",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Reduce file size significantly while retaining high visual clarity.",
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
                    item {
                        Text(
                            text = "Choose Compression Level",
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy,
                            fontSize = 15.sp
                        )
                    }

                    CompressionLevel.values().forEach { level ->
                        val isSelected = selectedLevel == level
                        item {
                            Card(
                                onClick = { selectedLevel = level },
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
                                    .testTag("compression_level_${level.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedLevel = level },
                                        colors = RadioButtonDefaults.colors(selectedColor = SmallpdfPrimary)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = level.title,
                                                fontWeight = FontWeight.Bold,
                                                color = SmallpdfNavy,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = if (level == CompressionLevel.STRONG) Color(0xFFFEF08A) else SmallpdfBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = level.reductionEstimate,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (level == CompressionLevel.STRONG) Color(0xFF854D0E) else SmallpdfPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = level.description,
                                            style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                                        )
                                    }
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
                                    currentStep = "Optimizing page images and stream objects..."
                                    val res = PdfCompressEngine.compressPdf(context, file, selectedLevel)
                                    isProcessing = false
                                    result = res
                                    if (res.isSuccess && res.outputFile != null) {
                                        SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                            HistoryEntity(
                                                title = res.outputFile.name,
                                                toolType = "Compress PDF",
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
                            colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("execute_compress_button")
                        ) {
                            Icon(Icons.Filled.Compress, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compress PDF",
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
