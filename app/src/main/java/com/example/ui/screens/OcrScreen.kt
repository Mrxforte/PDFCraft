package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
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
import com.example.core.model.OcrDocumentResult
import com.example.core.model.PdfFileInfo
import com.example.core.pdf.OcrEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

enum class OcrInputMode(val label: String) {
    IMAGE("Image / Photo"),
    PDF("PDF Document")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var inputMode by remember { mutableStateOf(OcrInputMode.IMAGE) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var currentProgressText by remember { mutableStateOf("Initializing ML Kit OCR...") }
    var ocrResult by remember { mutableStateOf<OcrDocumentResult?>(null) }
    var textSearchQuery by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val temp = FileUtils.copyUriToTemp(context, uri, "ocr_img_")
                selectedFile = temp
                selectedFileName = "Scanned_Image_${System.currentTimeMillis()}.jpg"
                ocrResult = null
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val info = FileUtils.getPdfFileInfo(context, uri)
                selectedFile = info.localFile
                selectedFileName = info.name
                ocrResult = null
            }
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "OCR Scanner",
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
                                .background(ToolOcr),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Optical Character Recognition",
                                    fontWeight = FontWeight.Bold,
                                    color = SmallpdfNavy,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFEF08A),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF854D0E),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Convert scanned pages & camera images into fully editable Word (.docx) & text.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                            )
                        }
                    }
                }
            }

            if (isProcessing) {
                item {
                    ProcessingProgressCard(currentStep = currentProgressText)
                }
            } else if (ocrResult != null) {
                // OCR Result Display
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Text Recognized Successfully",
                                        fontWeight = FontWeight.Bold,
                                        color = SmallpdfSuccess,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "${ocrResult!!.totalPages} page(s) processed",
                                        fontSize = 12.sp,
                                        color = SmallpdfTextSecondary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Recognized Text", ocrResult!!.combinedText)
                                        clipboard.setPrimaryClip(clip)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied all text to clipboard!")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Text", tint = SmallpdfPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Download Word / Text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        ocrResult?.outputDocxFile?.let { docx ->
                                            FileUtils.shareFile(context, docx, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ToolPdfToWord),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Word (.docx)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        ocrResult?.outputTxtFile?.let { txt ->
                                            FileUtils.shareFile(context, txt, "text/plain")
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SmallpdfBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Article, contentDescription = null, tint = SmallpdfNavy, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Text (.txt)", color = SmallpdfNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Search bar in recognized text
                            OutlinedTextField(
                                value = textSearchQuery,
                                onValueChange = { textSearchQuery = it },
                                placeholder = { Text("Search recognized text...") },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SmallpdfTextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Text Container
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SmallpdfBackground,
                                border = BorderStroke(1.dp, SmallpdfBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 320.dp)
                            ) {
                                SelectionContainer {
                                    val textToShow = if (textSearchQuery.isEmpty()) {
                                        ocrResult!!.combinedText
                                    } else {
                                        ocrResult!!.combinedText.lines()
                                            .filter { it.contains(textSearchQuery, ignoreCase = true) }
                                            .joinToString("\n")
                                            .ifEmpty { "No matching lines found for '$textSearchQuery'" }
                                    }
                                    Text(
                                        text = textToShow,
                                        modifier = Modifier.padding(14.dp),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = SmallpdfTextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    ocrResult = null
                                    selectedFile = null
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Scan another document", color = SmallpdfPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Mode Selector (Image vs PDF)
                item {
                    TabRow(
                        selectedTabIndex = inputMode.ordinal,
                        containerColor = Color.White,
                        contentColor = SmallpdfPrimary
                    ) {
                        OcrInputMode.values().forEach { mode ->
                            Tab(
                                selected = inputMode == mode,
                                onClick = {
                                    inputMode = mode
                                    selectedFile = null
                                    selectedFileName = null
                                },
                                text = {
                                    Text(
                                        text = mode.label,
                                        fontWeight = if (inputMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // File Picker
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
                                        imageVector = if (inputMode == OcrInputMode.IMAGE) Icons.Filled.AddPhotoAlternate else Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        tint = SmallpdfPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (inputMode == OcrInputMode.IMAGE) {
                                            imagePickerLauncher.launch("image/*")
                                        } else {
                                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (inputMode == OcrInputMode.IMAGE) "Select Image" else "Select PDF",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (inputMode == OcrInputMode.IMAGE) "JPG, PNG, WebP images" else "Scanned or digital PDF documents",
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
                                            .background(ToolOcr),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DocumentScanner,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedFileName ?: "document",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${(selectedFile!!.length() / 1024)} KB",
                                            fontSize = 12.sp,
                                            color = SmallpdfTextSecondary
                                        )
                                    }
                                    IconButton(onClick = { selectedFile = null }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val file = selectedFile ?: return@Button
                                        scope.launch {
                                            isProcessing = true
                                            currentProgressText = "Recognizing text with on-device ML Kit..."

                                            val res = if (inputMode == OcrInputMode.IMAGE) {
                                                OcrEngine.processImageOcr(context, file)
                                            } else {
                                                OcrEngine.processPdfOcr(context, file) { curr, tot ->
                                                    currentProgressText = "Recognizing page $curr of $tot..."
                                                }
                                            }

                                            isProcessing = false
                                            ocrResult = res

                                            // Save to history
                                            res.outputDocxFile?.let { docx ->
                                                SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                                    HistoryEntity(
                                                        title = docx.name,
                                                        toolType = "PDF OCR",
                                                        originalSizeBytes = file.length(),
                                                        resultSizeBytes = docx.length(),
                                                        filePath = docx.absolutePath,
                                                        fileUri = docx.toURI().toString()
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("run_ocr_button")
                                ) {
                                    Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Run OCR Scanner", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
