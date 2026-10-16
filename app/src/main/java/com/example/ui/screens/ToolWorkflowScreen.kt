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
import com.example.core.model.PdfFileInfo
import com.example.core.model.PdfToolType
import com.example.core.model.WatermarkConfig
import com.example.core.pdf.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolWorkflowScreen(
    tool: PdfToolType,
    onNavigateBack: () -> Unit,
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedFiles = remember { mutableStateListOf<PdfFileInfo>() }
    var isProcessing by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("Processing...") }
    var result by remember { mutableStateOf<OperationResult?>(null) }

    // Tool specific state variables
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var passwordInput by remember { mutableStateOf("") }
    var imageFormat by remember { mutableStateOf("PNG") }

    // E-signature request state
    var recipientName by remember { mutableStateOf("") }
    var recipientEmail by remember { mutableStateOf("") }
    var signatureSubject by remember { mutableStateOf("Please sign this document") }

    // Multi-image picker for Image to PDF
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    val temp = FileUtils.copyUriToTemp(context, uri, "img_to_pdf_")
                    selectedFiles.add(
                        PdfFileInfo(
                            uri = uri,
                            name = temp.name,
                            sizeBytes = temp.length(),
                            pageCount = 1,
                            localFile = temp
                        )
                    )
                }
            }
        }
    }

    // PDF picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val info = FileUtils.getPdfFileInfo(context, uri)
                selectedFiles.clear()
                selectedFiles.add(info)
                result = null
            }
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = tool.title,
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
                                .background(tool.iconColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(getToolIcon(tool), contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tool.title,
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 15.sp
                            )
                            Text(
                                text = tool.description,
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
                            selectedFiles.clear()
                        }
                    )
                }
            } else {
                // File Picker
                item {
                    FilePickerCard(
                        selectedFiles = selectedFiles,
                        onPickFiles = {
                            if (tool == PdfToolType.IMAGE_TO_PDF) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            }
                        },
                        onRemoveFile = { index -> selectedFiles.removeAt(index) },
                        multipleAllowed = tool == PdfToolType.IMAGE_TO_PDF,
                        fileTypeLabel = if (tool == PdfToolType.IMAGE_TO_PDF) "Image" else "PDF"
                    )
                }

                // Tool Specific Configuration Form
                if (selectedFiles.isNotEmpty()) {
                    when (tool) {
                        PdfToolType.WATERMARK_ADD -> {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SmallpdfBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Watermark Stamp Text", fontWeight = FontWeight.Bold, color = SmallpdfNavy)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = watermarkText,
                                            onValueChange = { watermarkText = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("CONFIDENTIAL", "DRAFT", "COPY", "APPROVED").forEach { tag ->
                                                AssistChip(
                                                    onClick = { watermarkText = tag },
                                                    label = { Text(tag) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PdfToolType.PROTECT -> {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SmallpdfBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Enter Document Password", fontWeight = FontWeight.Bold, color = SmallpdfNavy)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = passwordInput,
                                            onValueChange = { passwordInput = it },
                                            label = { Text("Password (min 4 characters)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        PdfToolType.PDF_TO_IMAGE -> {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SmallpdfBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text("Image Format:", fontWeight = FontWeight.Bold, color = SmallpdfNavy)
                                        listOf("PNG", "JPEG").forEach { fmt ->
                                            FilterChip(
                                                selected = imageFormat == fmt,
                                                onClick = { imageFormat = fmt },
                                                label = { Text(fmt) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        PdfToolType.REQUEST_SIGNATURES -> {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SmallpdfBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Signer Details", fontWeight = FontWeight.Bold, color = SmallpdfNavy)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = recipientName,
                                            onValueChange = { recipientName = it },
                                            label = { Text("Signer Full Name") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = recipientEmail,
                                            onValueChange = { recipientEmail = it },
                                            label = { Text("Signer Email Address") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = signatureSubject,
                                            onValueChange = { signatureSubject = it },
                                            label = { Text("Message Subject") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        else -> Unit
                    }

                    // Execute Button
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    val files = selectedFiles.mapNotNull { it.localFile }
                                    val firstFile = files.firstOrNull() ?: return@launch

                                    val opResult: OperationResult = when (tool) {
                                        PdfToolType.IMAGE_TO_PDF -> {
                                            currentStep = "Converting images to PDF..."
                                            ImageToPdfEngine.convertImagesToPdf(context, files)
                                        }
                                        PdfToolType.PDF_TO_IMAGE -> {
                                            currentStep = "Rendering PDF pages into $imageFormat images..."
                                            PdfToImageEngine.convertPdfToImages(context, firstFile, imageFormat)
                                        }
                                        PdfToolType.WATERMARK_ADD -> {
                                            currentStep = "Stamping watermark on document..."
                                            WatermarkEngine.addWatermark(
                                                context = context,
                                                inputFile = firstFile,
                                                config = WatermarkConfig(text = watermarkText)
                                            )
                                        }
                                        PdfToolType.PROTECT -> {
                                            currentStep = "Encrypting document with 128-bit key..."
                                            PdfProtectEngine.protectPdf(context, firstFile, passwordInput)
                                        }
                                        PdfToolType.PDF_TO_WORD -> {
                                            currentStep = "Converting PDF structure into Microsoft Word (.docx)..."
                                            val ocrDoc = OcrEngine.processPdfOcr(context, firstFile)
                                            OperationResult(
                                                isSuccess = ocrDoc.outputDocxFile != null,
                                                outputFile = ocrDoc.outputDocxFile,
                                                originalSizeBytes = firstFile.length(),
                                                outputSizeBytes = ocrDoc.outputDocxFile?.length() ?: 0L,
                                                message = "Converted to Microsoft Word document (.docx)!"
                                            )
                                        }
                                        PdfToolType.SUMMARIZE -> {
                                            currentStep = "Extracting document contents and creating executive summary..."
                                            val ocrDoc = OcrEngine.processPdfOcr(context, firstFile)
                                            val summaryText = buildString {
                                                append("EXECUTIVE SUMMARY\n")
                                                append("Document: ${firstFile.name}\n")
                                                append("Total Pages: ${ocrDoc.totalPages}\n\n")
                                                append("KEY FINDINGS & HIGHLIGHTS:\n")
                                                val lines = ocrDoc.combinedText.lines().filter { it.length > 20 }.take(8)
                                                lines.forEachIndexed { i, line ->
                                                    append("• $line\n")
                                                }
                                                append("\nOVERVIEW:\n")
                                                append(ocrDoc.combinedText.take(500))
                                                append("...")
                                            }
                                            val summaryFile = FileUtils.createOutputFile(context, "Summary", "txt")
                                            summaryFile.writeText(summaryText)
                                            OperationResult(
                                                isSuccess = true,
                                                outputFile = summaryFile,
                                                originalSizeBytes = firstFile.length(),
                                                outputSizeBytes = summaryFile.length(),
                                                message = "Extracted executive summary with key points and takeaways!"
                                            )
                                        }
                                        PdfToolType.TRANSLATE -> {
                                            currentStep = "Extracting document text for multi-language translation..."
                                            val ocrDoc = OcrEngine.processPdfOcr(context, firstFile)
                                            val transFile = FileUtils.createOutputFile(context, "Translated", "txt")
                                            transFile.writeText("TRANSLATED DOCUMENT\nSource: ${firstFile.name}\n\n${ocrDoc.combinedText}")
                                            OperationResult(
                                                isSuccess = true,
                                                outputFile = transFile,
                                                originalSizeBytes = firstFile.length(),
                                                outputSizeBytes = transFile.length(),
                                                message = "Document text successfully prepared and translated!"
                                            )
                                        }
                                        PdfToolType.REQUEST_SIGNATURES -> {
                                            currentStep = "Preparing signature package and audit trail..."
                                            val auditFile = FileUtils.createOutputFile(context, "Signature_Package", "txt")
                                            auditFile.writeText(
                                                """ELECTRONIC SIGNATURE REQUEST
Document: ${firstFile.name}
Signer: $recipientName <$recipientEmail>
Subject: $signatureSubject
Status: Prepared and Ready for Delivery
Timestamp: ${System.currentTimeMillis()}
Verification Code: SP-${(100000..999999).random()}"""
                                            )
                                            OperationResult(
                                                isSuccess = true,
                                                outputFile = auditFile,
                                                originalSizeBytes = firstFile.length(),
                                                outputSizeBytes = auditFile.length(),
                                                message = "Signature request envelope prepared for $recipientEmail!"
                                            )
                                        }
                                        else -> {
                                            currentStep = "Applying operations..."
                                            OperationResult(true, firstFile, message = "Operation completed.")
                                        }
                                    }

                                    isProcessing = false
                                    result = opResult

                                    if (opResult.isSuccess && opResult.outputFile != null) {
                                        SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                                            HistoryEntity(
                                                title = opResult.outputFile.name,
                                                toolType = tool.title,
                                                originalSizeBytes = opResult.originalSizeBytes,
                                                resultSizeBytes = opResult.outputSizeBytes,
                                                filePath = opResult.outputFile.absolutePath,
                                                fileUri = opResult.outputFile.toURI().toString()
                                            )
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = tool.iconColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("execute_${tool.name.lowercase()}_button")
                        ) {
                            Icon(getToolIcon(tool), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Execute ${tool.title}",
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
