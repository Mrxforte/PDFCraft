package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.HistoryEntity
import com.example.core.model.PdfToolType
import com.example.core.model.ToolCategory
import com.example.ui.components.PdfToolCard
import com.example.ui.components.SmallpdfTopBar
import com.example.ui.components.UpgradeBannerCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSelectTool: (PdfToolType) -> Unit,
    onOpenHistoryItem: (HistoryEntity) -> Unit,
    recentHistory: List<HistoryEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }

    val popularTools = remember {
        listOf(
            PdfToolType.MERGE,
            PdfToolType.COMPRESS,
            PdfToolType.WATERMARK_DELETE,
            PdfToolType.OCR,
            PdfToolType.SPLIT,
            PdfToolType.ANNOTATE,
            PdfToolType.EDIT,
            PdfToolType.PDF_TO_WORD,
            PdfToolType.SUMMARIZE,
            PdfToolType.PDF_TO_IMAGE,
            PdfToolType.SIGN,
            PdfToolType.REQUEST_SIGNATURES
        )
    }

    val filteredTools = remember(searchQuery, selectedCategory) {
        val all = PdfToolType.values().toList()
        all.filter { tool ->
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
                title = "Smallpdf",
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp)),
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
        containerColor = SmallpdfBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search PDF tools (Merge, Compress, OCR, Watermark...)") },
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SmallpdfPrimary,
                        unfocusedBorderColor = SmallpdfBorder
                    ),
                    singleLine = true
                )
            }

            // Smallpdf Pro Upgrade Banner
            item {
                UpgradeBannerCard(onUpgradeClick = {})
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
                            label = { Text("All Tools") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmallpdfNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(ToolCategory.values()) { cat ->
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

            // Recent Documents Section (if any exist)
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, SmallpdfBorder),
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

            // Popular PDF Tools Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Search Results" else "Popular PDF Tools",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy
                        )
                    )
                    Text(
                        text = "${filteredTools.size} tools",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SmallpdfTextSecondary
                        )
                    )
                }
            }

            // Tools Responsive Grid (2 columns on mobile, matching Smallpdf layout)
            val toolsToDisplay = if (searchQuery.isNotEmpty() || selectedCategory != null) filteredTools else popularTools
            val rows = toolsToDisplay.chunked(2)
            items(rows) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PdfToolCard(
                        tool = pair[0],
                        onClick = { onSelectTool(pair[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        PdfToolCard(
                            tool = pair[1],
                            onClick = { onSelectTool(pair[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
