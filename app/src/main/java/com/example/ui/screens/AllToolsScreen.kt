package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PdfToolType
import com.example.core.model.ToolCategory
import com.example.ui.components.PdfToolCard
import com.example.ui.components.SmallpdfTopBar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllToolsScreen(
    onSelectTool: (PdfToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val allTools = remember { PdfToolType.values().toList() }
    val categories = remember { ToolCategory.values().filter { it != ToolCategory.POPULAR } }

    Scaffold(
        topBar = {
            SmallpdfTopBar(title = "All PDF Tools")
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
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search all PDF tools...") },
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
                        .testTag("all_tools_search_input"),
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

            if (searchQuery.isNotEmpty()) {
                val matches = allTools.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                }

                item {
                    Text(
                        text = "Results (${matches.size})",
                        fontWeight = FontWeight.Bold,
                        color = SmallpdfNavy,
                        fontSize = 16.sp
                    )
                }

                items(matches.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PdfToolCard(tool = pair[0], onClick = { onSelectTool(pair[0]) }, modifier = Modifier.weight(1f))
                        if (pair.size > 1) {
                            PdfToolCard(tool = pair[1], onClick = { onSelectTool(pair[1]) }, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                categories.forEach { cat ->
                    val toolsInCat = allTools.filter { it.category == cat }
                    if (toolsInCat.isNotEmpty()) {
                        item {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SmallpdfNavy
                                )
                            )
                        }

                        items(toolsInCat.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PdfToolCard(tool = pair[0], onClick = { onSelectTool(pair[0]) }, modifier = Modifier.weight(1f))
                                if (pair.size > 1) {
                                    PdfToolCard(tool = pair[1], onClick = { onSelectTool(pair[1]) }, modifier = Modifier.weight(1f))
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
