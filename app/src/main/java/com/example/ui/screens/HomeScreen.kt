package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.HistoryEntity
import com.example.core.model.PdfToolType
import com.example.core.model.ToolCategory
import com.example.ui.components.PdfToolCard
import com.example.ui.components.PdfToolGridTile
import com.example.ui.components.SmallpdfTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSelectTool: (PdfToolType) -> Unit,
    onOpenHistoryItem: (HistoryEntity) -> Unit,
    onOpenPdf: (File) -> Unit = {},
    recentHistory: List<HistoryEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }
    var isGridMode by remember { mutableStateOf(true) }
    var showProInfoDialog by remember { mutableStateOf(false) }

    val documentPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            scope.launch {
                val info = com.example.core.files.FileUtils.getPdfFileInfo(context, uri)
                info.localFile?.let { file ->
                    com.example.core.database.SmallpdfDatabase.getInstance(context).historyDao().insertHistory(
                        com.example.core.database.HistoryEntity(
                            title = file.name,
                            toolType = "Opened Document",
                            originalSizeBytes = file.length(),
                            resultSizeBytes = file.length(),
                            filePath = file.absolutePath,
                            fileUri = file.toURI().toString()
                        )
                    )
                    onOpenPdf(file)
                }
            }
        }
    }

    val allTools = remember { PdfToolType.values().toList() }

    val popularTools = remember {
        listOf(
            PdfToolType.MERGE,
            PdfToolType.COMPRESS,
            PdfToolType.WATERMARK_DELETE,
            PdfToolType.DELETE_METADATA,
            PdfToolType.OCR,
            PdfToolType.SPLIT,
            PdfToolType.PDF_TO_WORD,
            PdfToolType.SIGN,
            PdfToolType.ANNOTATE,
            PdfToolType.EDIT,
            PdfToolType.PDF_TO_IMAGE,
            PdfToolType.SUMMARIZE
        )
    }

    val displayedTools = remember(searchQuery, selectedCategory) {
        allTools.filter { tool ->
            val matchesQuery = searchQuery.isEmpty() ||
                    tool.title.contains(searchQuery, ignoreCase = true) ||
                    tool.description.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || tool.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "MultiTools Hub",
                actions = {
                    IconButton(onClick = { isGridMode = !isGridMode }) {
                        Icon(
                            imageVector = if (isGridMode) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                            contentDescription = "Toggle Grid/List",
                            tint = SmallpdfNavy
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showProInfoDialog = true },
                        color = SmallpdfInfoBg
                    ) {
                        Text(
                            text = "PRO",
                            color = SmallpdfPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { documentPickerLauncher.launch(arrayOf("application/pdf")) },
                icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                text = { Text("Open PDF", fontWeight = FontWeight.Bold) },
                containerColor = SmallpdfPrimary,
                contentColor = Color.White
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
            // Hero Hub Banner matching user flyer design
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF0B57F9), Color(0xFF06245B))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "★ ALL-IN-ONE TOOLKIT",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "16+ TOOLS IN ONE APP",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = Color.White,
                                lineHeight = 28.sp
                            )

                            Text(
                                text = "All Your Daily PDF, Image & OCR Tools in One Place",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "⚡ 100% On-Device",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "🔒 Zero Cloud Upload",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { documentPickerLauncher.launch(arrayOf("application/pdf")) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = SmallpdfPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open PDF Document", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tools (Merge, Compress, OCR, Watermark, EXIF...)") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = SmallpdfTextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = SmallpdfTextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SmallpdfPrimary,
                        unfocusedBorderColor = SmallpdfBorder
                    ),
                    singleLine = true
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Tools (${allTools.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmallpdfNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(ToolCategory.values().filter { it != ToolCategory.POPULAR }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmallpdfNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Recent Documents Section (if any)
            if (recentHistory.isNotEmpty() && searchQuery.isEmpty()) {
                item {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy
                        )
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentHistory) { item ->
                            Card(
                                onClick = { onOpenHistoryItem(item) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, SmallpdfBorder),
                                modifier = Modifier.width(200.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ToolCompress),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${item.toolType} • ${item.formattedSize}",
                                            fontSize = 11.sp,
                                            color = SmallpdfTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Search Results" else "Daily Tools Suite",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy
                        )
                    )
                    Text(
                        text = "${displayedTools.size} tools",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SmallpdfTextSecondary
                        )
                    )
                }
            }

            // MultiTools Hub Grid Layout (3 columns in Hub Grid Mode like user photo, or 2 columns in List Mode)
            if (isGridMode) {
                val chunks = displayedTools.chunked(3)
                items(chunks) { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (tool in rowTools) {
                            Box(modifier = Modifier.weight(1f)) {
                                PdfToolGridTile(
                                    tool = tool,
                                    onClick = { onSelectTool(tool) }
                                )
                            }
                        }
                        // Fill empty slots in row
                        if (rowTools.size < 3) {
                            for (i in 0 until (3 - rowTools.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                val chunks = displayedTools.chunked(2)
                items(chunks) { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PdfToolCard(
                            tool = rowTools[0],
                            onClick = { onSelectTool(rowTools[0]) },
                            modifier = Modifier.weight(1f)
                        )
                        if (rowTools.size > 1) {
                            PdfToolCard(
                                tool = rowTools[1],
                                onClick = { onSelectTool(rowTools[1]) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Feature Checklist Footer inspired by the user's uploaded flyer
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SmallpdfBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "One App, Many Solutions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SmallpdfNavy
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val highlights = listOf(
                            Pair("PDF Tools", "Merge, Split, Compress, Protect, Sign"),
                            Pair("Watermark Deleter", "Clean footers, stamps, and margins"),
                            Pair("Delete EXIF & Author", "Wipe metadata, creator, and GPS"),
                            Pair("Reliable OCR Scanner", "Extract text & export to Word (.docx)"),
                            Pair("Image Tools", "High-res PDF to Image & Image to PDF"),
                            Pair("100% On-Device", "Secure, private, and works offline")
                        )

                        highlights.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { (title, subtitle) ->
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = SmallpdfPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SmallpdfTextPrimary)
                                            Text(subtitle, fontSize = 10.sp, color = SmallpdfTextSecondary, maxLines = 1)
                                        }
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showProInfoDialog) {
        AlertDialog(
            onDismissRequest = { showProInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ Smallpdf Pro")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("100% On-Device, Privacy-First PDF Powerhouse", fontWeight = FontWeight.Bold, color = SmallpdfNavy)
                    Text("• Unlimited multi-file PDF Merging and Splitting")
                    Text("• Deep compression reducing files up to 90%")
                    Text("• High-speed on-device OCR Scanner to Word (.docx)")
                    Text("• Watermark, stamp & header cleaner")
                    Text("• Delete EXIF, Author, GPS & tracking metadata")
                    Text("• 100% private: no documents are ever sent to remote servers")
                }
            },
            confirmButton = {
                TextButton(onClick = { showProInfoDialog = false }) {
                    Text("Got It", color = SmallpdfPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
