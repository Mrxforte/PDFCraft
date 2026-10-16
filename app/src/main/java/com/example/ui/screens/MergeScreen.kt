package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.HistoryEntity
import com.example.core.database.SmallpdfDatabase
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.example.core.model.PdfFileInfo
import com.example.core.pdf.PdfMergeEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(
    onNavigateBack: () -> Unit,
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedFiles = remember { mutableStateListOf<PdfFileInfo>() }
    var isProcessing by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("Preparing documents...") }
    var result by remember { mutableStateOf<OperationResult?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    val info = FileUtils.getPdfFileInfo(context, uri)
                    selectedFiles.add(info)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "Merge PDF",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                .background(ToolMerge),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CallMerge, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Combine Multiple PDFs",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Arrange your documents in the exact order you want them merged.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                            )
                        }
                    }
                }
            }

            // Processing State
            if (isProcessing) {
                item {
                    ProcessingProgressCard(currentStep = currentStep)
                }
            } else if (result != null) {
                // Success State
                item {
                    SuccessResultCard(
                        result = result!!,
                        onOpenFile = {
                            result?.outputFile?.let { onViewPdf(it) }
                        },
                        onShareFile = {
                            result?.outputFile?.let { FileUtils.shareFile(context, it) }
                        },
                        onProcessAnother = {
                            result = null
                            selectedFiles.clear()
                        }
                    )
                }
            } else {
                // File Selection & List
                item {
                    FilePickerCard(
                        selectedFiles = selectedFiles,
                        onPickFiles = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        onRemoveFile = { index -> selectedFiles.removeAt(index) },
                        multipleAllowed = true
                    )
                }

                if (selectedFiles.isNotEmpty()) {
                    item {
                        Text(
                            text = "Arrange Sequence",
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy,
                            fontSize = 15.sp
                        )
                    }

                    itemsIndexed(selectedFiles) { index, file ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SmallpdfBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SmallpdfNavy,
                                    shape = CircleShape,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${file.pageCount} page(s) • ${file.formattedSize}",
                                        fontSize = 12.sp,
                                        color = SmallpdfTextSecondary
                                    )
                                }

                                // Move Up
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val item = selectedFiles.removeAt(index)
                                            selectedFiles.add(index - 1, item)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move Up")
                                }

                                // Move Down
                                IconButton(
                                    onClick = {
                                        if (index < selectedFiles.size - 1) {
                                            val item = selectedFiles.removeAt(index)
                                            selectedFiles.add(index + 1, item)
                                        }
                                    },
                                    enabled = index < selectedFiles.size - 1
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move Down")
                                }
                            }
                        }
                    }

                    // Action Button
                    item {
                        Button(
                            onClick = {
                                if (selectedFiles.size < 2) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please add at least 2 files to merge.")
                                    }
                                    return@Button
                                }
                                scope.launch {
                                    isProcessing = true
                                    currentStep = "Merging ${selectedFiles.size} PDF files..."
                                    val files = selectedFiles.mapNotNull { it.localFile }
                                    val res = PdfMergeEngine.mergePdfs(context, files)
                                    isProcessing = false
                                    result = res
                                    if (res.isSuccess && res.outputFile != null) {
                                        // Save to history
                                        SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                            HistoryEntity(
                                                title = res.outputFile.name,
                                                toolType = "Merge PDF",
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
                                .testTag("execute_merge_button")
                        ) {
                            Icon(Icons.Filled.CallMerge, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Merge ${selectedFiles.size} PDFs",
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
