package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.core.model.PdfFileInfo
import com.example.core.pdf.PdfSplitEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(
    onNavigateBack: () -> Unit,
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<PdfFileInfo?>(null) }
    var rangeText by remember { mutableStateOf("1-1") }
    var isProcessing by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("Splitting pages...") }
    var result by remember { mutableStateOf<OperationResult?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val info = FileUtils.getPdfFileInfo(context, uri)
                selectedFile = info
                rangeText = if (info.pageCount > 1) "1-${info.pageCount / 2}, ${(info.pageCount / 2) + 1}-${info.pageCount}" else "1-1"
                result = null
            }
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "Split PDF",
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
                                .background(ToolSplit),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CallSplit, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Extract Pages or Ranges",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Specify custom page ranges or separate every page into individual files.",
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
                // Multi-file split results
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "PDF Split Successfully!",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfSuccess,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Created ${result!!.outputFiles.size} separate document(s)",
                                color = SmallpdfTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            result!!.outputFiles.forEachIndexed { idx, file ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SmallpdfBackground,
                                    border = BorderStroke(1.dp, SmallpdfBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = ToolCompress, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                            Text("${(file.length() / 1024)} KB", fontSize = 11.sp, color = SmallpdfTextSecondary)
                                        }
                                        IconButton(onClick = { onViewPdf(file) }) {
                                            Icon(Icons.Filled.Visibility, contentDescription = "Open", tint = SmallpdfPrimary)
                                        }
                                        IconButton(onClick = { FileUtils.shareFile(context, file) }) {
                                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = SmallpdfNavy)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    result = null
                                    selectedFile = null
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Split another document", color = SmallpdfPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SmallpdfBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Split Range Configuration",
                                    fontWeight = FontWeight.Bold,
                                    color = SmallpdfNavy,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Total Pages in Document: ${selectedFile!!.pageCount}",
                                    fontSize = 13.sp,
                                    color = SmallpdfTextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = rangeText,
                                    onValueChange = { rangeText = it },
                                    label = { Text("Page Ranges (e.g. 1-2, 3-5)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick presets
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = { rangeText = "1-${selectedFile!!.pageCount}" },
                                        label = { Text("All") }
                                    )
                                    if (selectedFile!!.pageCount > 1) {
                                        AssistChip(
                                            onClick = {
                                                val half = selectedFile!!.pageCount / 2
                                                rangeText = "1-$half, ${half + 1}-${selectedFile!!.pageCount}"
                                            },
                                            label = { Text("Half Split") }
                                        )
                                        AssistChip(
                                            onClick = { rangeText = (1..selectedFile!!.pageCount).joinToString(", ") { "$it" } },
                                            label = { Text("Each Page") }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val file = selectedFile?.localFile ?: return@Button
                                        scope.launch {
                                            isProcessing = true
                                            currentStep = "Extracting and writing page segments..."
                                            val res = PdfSplitEngine.splitPdfByRanges(context, file, rangeText)
                                            isProcessing = false
                                            result = res
                                            if (res.isSuccess && res.outputFiles.isNotEmpty()) {
                                                res.outputFiles.forEach { f ->
                                                    SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                                        HistoryEntity(
                                                            title = f.name,
                                                            toolType = "Split PDF",
                                                            originalSizeBytes = file.length(),
                                                            resultSizeBytes = f.length(),
                                                            filePath = f.absolutePath,
                                                            fileUri = f.toURI().toString()
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("execute_split_button")
                                ) {
                                    Icon(Icons.Filled.CallSplit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Split PDF Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
