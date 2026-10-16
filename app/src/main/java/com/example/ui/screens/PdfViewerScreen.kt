package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.files.FileUtils
import com.example.core.model.PageThumbnail
import com.example.core.pdf.*
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation and Page States
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var rotationAngle by remember { mutableStateOf(0) }

    // Reading progress & initial restore
    LaunchedEffect(pdfFile) {
        val (savedPage, savedScale) = PdfAdvancedEngine.getReadingProgress(context, pdfFile.absolutePath)
        withContext(Dispatchers.IO) {
            val count = PdfRendererHelper.getPageCount(pdfFile).coerceAtLeast(1)
            totalPages = count
            currentPage = savedPage.coerceIn(0, count - 1)
        }
    }

    // Save reading progress on page change
    LaunchedEffect(currentPage) {
        PdfAdvancedEngine.saveReadingProgress(context, pdfFile.absolutePath, currentPage, 1f)
    }

    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 4.0f)
        offset += panChange
    }

    // Search In Document
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<PdfSearchResult?>(null) }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    // Dialogs and Sheets
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showThumbnailsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }

    var thumbnails by remember { mutableStateOf<List<PageThumbnail>>(emptyList()) }
    var bookmarks by remember { mutableStateOf<List<PdfBookmark>>(emptyList()) }

    // Page Renderer effect
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
            Column {
                SmallpdfTopBar(
                    title = pdfFile.nameWithoutExtension,
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack,
                    actions = {
                        // Text Search
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search Text", tint = SmallpdfNavy)
                        }
                        // Table of Contents / Bookmarks
                        IconButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    bookmarks = PdfAdvancedEngine.extractBookmarks(pdfFile)
                                }
                                showBookmarksSheet = true
                            }
                        }) {
                            Icon(Icons.Filled.MenuBook, contentDescription = "Bookmarks", tint = SmallpdfNavy)
                        }
                        // Page Overview Thumbnails
                        IconButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    thumbnails = PdfRendererHelper.renderThumbnails(pdfFile, maxPages = totalPages, thumbWidth = 220)
                                }
                                showThumbnailsSheet = true
                            }
                        }) {
                            Icon(Icons.Filled.GridView, contentDescription = "Thumbnails", tint = SmallpdfNavy)
                        }
                        // More options: Info, Print, Share
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "Info", tint = SmallpdfNavy)
                        }
                        IconButton(onClick = { PdfAdvancedEngine.printPdfFile(context, pdfFile) }) {
                            Icon(Icons.Filled.Print, contentDescription = "Print", tint = SmallpdfNavy)
                        }
                        IconButton(onClick = { FileUtils.shareFile(context, pdfFile) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = SmallpdfNavy)
                        }
                    }
                )

                // Search Bar in Document
                if (isSearchActive) {
                    Surface(
                        color = Color.White,
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Find in document text...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    scope.launch {
                                                        isSearching = true
                                                        val res = PdfAdvancedEngine.searchInPdf(pdfFile, searchQuery)
                                                        searchResult = res
                                                        currentMatchIndex = 0
                                                        isSearching = false
                                                        if (res.matches.isNotEmpty()) {
                                                            currentPage = res.matches[0].pageIndex
                                                        } else {
                                                            snackbarHostState.showSnackbar("No matches found for '$searchQuery'")
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Filled.Search, contentDescription = "Find", tint = SmallpdfPrimary)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchResult = null
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close Search")
                                }
                            }

                            // Match Navigation & Snippet
                            if (searchResult != null) {
                                val matches = searchResult!!.matches
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (matches.isNotEmpty()) {
                                            "Match ${currentMatchIndex + 1} of ${matches.size} (Page ${matches[currentMatchIndex].pageIndex + 1})"
                                        } else {
                                            "0 matches found"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SmallpdfPrimary
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (matches.isNotEmpty() && currentMatchIndex > 0) {
                                                    currentMatchIndex--
                                                    currentPage = matches[currentMatchIndex].pageIndex
                                                }
                                            },
                                            enabled = matches.isNotEmpty() && currentMatchIndex > 0
                                        ) {
                                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous Match", modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                if (matches.isNotEmpty() && currentMatchIndex < matches.size - 1) {
                                                    currentMatchIndex++
                                                    currentPage = matches[currentMatchIndex].pageIndex
                                                }
                                            },
                                            enabled = matches.isNotEmpty() && currentMatchIndex < matches.size - 1
                                        ) {
                                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Next Match", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                if (matches.isNotEmpty() && matches[currentMatchIndex].snippets.isNotEmpty()) {
                                    Text(
                                        text = matches[currentMatchIndex].snippets[0],
                                        fontSize = 11.sp,
                                        color = SmallpdfTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    // Page Scrubber Slider
                    if (totalPages > 1) {
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { currentPage = it.toInt().coerceIn(0, totalPages - 1) },
                            valueRange = 0f..(totalPages - 1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = SmallpdfPrimary,
                                activeTrackColor = SmallpdfPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Page")
                        }

                        // Tapping page badge opens direct Go To Page Dialog
                        Surface(
                            onClick = { showGoToPageDialog = true },
                            color = SmallpdfInfoBg,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Page ${currentPage + 1} of $totalPages  ✎",
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
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next Page")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFE2E8F0)
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    // Double tap to toggle zoom between 1x and 2.5x
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1.2f) 1f else 2.5f
                            offset = Offset.Zero
                        }
                    )
                }
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

            // Quick Floating Toolbar Overlay (Rotate, Copy Text, Zoom In, Reset)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Page Text
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                PdfAdvancedEngine.extractPageText(pdfFile, currentPage)
                            }
                            if (text.isNotBlank()) {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Page Text", text))
                                snackbarHostState.showSnackbar("Copied page ${currentPage + 1} text to clipboard!")
                            } else {
                                snackbarHostState.showSnackbar("No text layer on this page. Run OCR Scanner!")
                            }
                        }
                    },
                    containerColor = Color.White,
                    contentColor = SmallpdfNavy
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Page Text", modifier = Modifier.size(18.dp))
                }

                // Rotate 90
                SmallFloatingActionButton(
                    onClick = { rotationAngle = (rotationAngle + 90) % 360 },
                    containerColor = Color.White,
                    contentColor = SmallpdfNavy
                ) {
                    Icon(Icons.Filled.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(18.dp))
                }

                // Zoom In
                SmallFloatingActionButton(
                    onClick = { scale = (scale + 0.35f).coerceAtMost(4f) },
                    containerColor = Color.White,
                    contentColor = SmallpdfNavy
                ) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                }

                // Reset Zoom
                SmallFloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    containerColor = Color.White,
                    contentColor = SmallpdfNavy
                ) {
                    Icon(Icons.Filled.FitScreen, contentDescription = "Reset Zoom", modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Go To Page Dialog
    if (showGoToPageDialog) {
        var pageInput by remember { mutableStateOf((currentPage + 1).toString()) }
        AlertDialog(
            onDismissRequest = { showGoToPageDialog = false },
            title = { Text("Go to Page") },
            text = {
                Column {
                    Text("Enter page number (1 to $totalPages):", fontSize = 13.sp, color = SmallpdfTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = pageInput.toIntOrNull()
                        if (num != null && num in 1..totalPages) {
                            currentPage = num - 1
                            showGoToPageDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary)
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToPageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Document Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Document Properties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${pdfFile.name}", fontWeight = FontWeight.Bold)
                    Text("Total Pages: $totalPages")
                    Text("File Size: ${(pdfFile.length() / 1024)} KB")
                    Text("Path: ${pdfFile.parent}", fontSize = 11.sp, color = SmallpdfTextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Thumbnails Overview Sheet
    if (showThumbnailsSheet) {
        ModalBottomSheet(onDismissRequest = { showThumbnailsSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Page Thumbnails ($totalPages)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SmallpdfNavy)
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(thumbnails) { thumb ->
                        val isCurr = thumb.pageIndex == currentPage
                        Card(
                            onClick = {
                                currentPage = thumb.pageIndex
                                showThumbnailsSheet = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(if (isCurr) 2.dp else 1.dp, if (isCurr) SmallpdfPrimary else SmallpdfBorder)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                                Image(
                                    bitmap = thumb.bitmap.asImageBitmap(),
                                    contentDescription = "P${thumb.pageIndex + 1}",
                                    modifier = Modifier
                                        .height(100.dp)
                                        .fillMaxWidth()
                                )
                                Text("P${thumb.pageIndex + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Table of Contents / Bookmarks Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Table of Contents", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SmallpdfNavy)
                Spacer(modifier = Modifier.height(12.dp))
                if (bookmarks.isEmpty()) {
                    Text("This document does not contain an embedded outline or bookmarks table.", color = SmallpdfTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                } else {
                    LazyRow(modifier = Modifier.fillMaxWidth()) {
                        items(bookmarks) { bm ->
                            AssistChip(
                                onClick = {
                                    currentPage = bm.pageIndex.coerceIn(0, totalPages - 1)
                                    showBookmarksSheet = false
                                },
                                label = { Text("${bm.title} (P${bm.pageIndex + 1})") }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }
}
