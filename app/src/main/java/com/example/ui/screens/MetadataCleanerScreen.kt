package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.HistoryEntity
import com.example.core.database.SmallpdfDatabase
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.example.core.pdf.FileMetadataInfo
import com.example.core.pdf.MetadataEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataCleanerScreen(
    onNavigateBack: () -> Unit,
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var metadataInfo by remember { mutableStateOf<FileMetadataInfo?>(null) }
    var isReadingMetadata by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<OperationResult?>(null) }

    // Pick either PDF or Image
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val temp = FileUtils.copyUriToTemp(context, uri, "meta_clean_")
                selectedFile = temp
                selectedFileName = temp.name
                result = null
                isReadingMetadata = true
                metadataInfo = withContext(Dispatchers.IO) {
                    MetadataEngine.readMetadata(temp)
                }
                isReadingMetadata = false
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val temp = FileUtils.copyUriToTemp(context, uri, "exif_clean_")
                selectedFile = temp
                selectedFileName = temp.name
                result = null
                isReadingMetadata = true
                metadataInfo = withContext(Dispatchers.IO) {
                    MetadataEngine.readMetadata(temp)
                }
                isReadingMetadata = false
            }
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "Delete EXIF & Author",
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
                            Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Remove Author, EXIF & Tracking",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Completely wipe author name, creator tools, camera models, and GPS location from documents and photos.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                            )
                        }
                    }
                }
            }

            if (isProcessing) {
                item {
                    ProcessingProgressCard(currentStep = "Stripping all metadata tags, author, and EXIF headers...")
                }
            } else if (result != null) {
                item {
                    SuccessResultCard(
                        result = result!!,
                        onOpenFile = {
                            result?.outputFile?.let { f ->
                                if (f.name.endsWith(".pdf", ignoreCase = true)) {
                                    onViewPdf(f)
                                } else {
                                    FileUtils.shareFile(context, f)
                                }
                            }
                        },
                        onShareFile = {
                            result?.outputFile?.let { FileUtils.shareFile(context, it) }
                        },
                        onProcessAnother = {
                            result = null
                            selectedFile = null
                            metadataInfo = null
                        }
                    )
                }
            } else {
                // File Selection Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, SmallpdfBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (selectedFile == null) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SmallpdfInfoBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        tint = SmallpdfPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { documentPickerLauncher.launch(arrayOf("application/pdf")) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select PDF", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SmallpdfBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Photo, contentDescription = null, tint = SmallpdfNavy, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select Photo", color = SmallpdfNavy, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PDF documents, JPG photos, and PNG images",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ToolWatermarkDelete),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Description, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedFileName ?: "File",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${(selectedFile!!.length() / 1024)} KB",
                                            fontSize = 12.sp,
                                            color = SmallpdfTextSecondary
                                        )
                                    }
                                    IconButton(onClick = {
                                        selectedFile = null
                                        metadataInfo = null
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        }
                    }
                }

                // Metadata Inspection Card
                if (selectedFile != null && metadataInfo != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SmallpdfBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Detected Metadata Tags",
                                    fontWeight = FontWeight.Bold,
                                    color = SmallpdfNavy,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                MetadataRow("Author / Artist", metadataInfo?.author ?: "Not specified")
                                if (metadataInfo?.isPdf == true) {
                                    MetadataRow("Creator Software", metadataInfo?.creator ?: "Not specified")
                                    MetadataRow("Producer Engine", metadataInfo?.producer ?: "Not specified")
                                    MetadataRow("Document Title", metadataInfo?.title ?: "Not specified")
                                } else {
                                    MetadataRow("Camera / Device", metadataInfo?.cameraDevice ?: "Not specified")
                                    MetadataRow("Software Tool", metadataInfo?.software ?: "Not specified")
                                    MetadataRow("GPS Location", metadataInfo?.gpsCoordinates ?: "None")
                                }
                                MetadataRow("Timestamp", metadataInfo?.creationDate ?: "Not specified")
                            }
                        }
                    }

                    // Delete Metadata Action Button
                    item {
                        Button(
                            onClick = {
                                val file = selectedFile ?: return@Button
                                scope.launch {
                                    isProcessing = true
                                    val res = MetadataEngine.stripAllMetadata(context, file)
                                    isProcessing = false
                                    result = res
                                    if (res.isSuccess && res.outputFile != null) {
                                        SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                            HistoryEntity(
                                                title = res.outputFile.name,
                                                toolType = "Delete EXIF & Author",
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
                                .testTag("execute_delete_metadata_button")
                        ) {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete All EXIF & Author Tags",
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

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = SmallpdfTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = if (value != "Not specified" && value != "None") SmallpdfPrimary else SmallpdfTextSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
