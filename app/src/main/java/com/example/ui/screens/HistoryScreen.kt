package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteSweep
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
import com.example.ui.components.SmallpdfTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onViewPdf: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { SmallpdfDatabase.getInstance(context).historyDao() }
    val historyList by dao.getAllHistory().collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            SmallpdfTopBar(
                title = "Processing History",
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear All", tint = SmallpdfNavy)
                        }
                    }
                }
            )
        },
        containerColor = SmallpdfBackground
    ) { innerPadding ->
        if (historyList.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SmallpdfInfoBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = SmallpdfPrimary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No History Yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Documents you merge, compress, or edit will appear here.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    val file = File(item.filePath)
                    val fileExists = file.exists()

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SmallpdfBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            item.toolType.contains("Merge") -> ToolMerge
                                            item.toolType.contains("Compress") -> ToolCompress
                                            item.toolType.contains("OCR") -> ToolOcr
                                            item.toolType.contains("Watermark") -> ToolWatermarkDelete
                                            else -> SmallpdfPrimary
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SmallpdfTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.toolType} • ${item.formattedSize}",
                                    fontSize = 12.sp,
                                    color = SmallpdfPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = dateFormat.format(Date(item.createdAt)),
                                    fontSize = 11.sp,
                                    color = SmallpdfTextSecondary
                                )
                            }

                            // Open Button
                            if (fileExists) {
                                IconButton(onClick = { onViewPdf(file) }) {
                                    Icon(Icons.Filled.Visibility, contentDescription = "View", tint = SmallpdfPrimary)
                                }
                                IconButton(onClick = { FileUtils.shareFile(context, file) }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = SmallpdfNavy)
                                }
                            }

                            IconButton(
                                onClick = {
                                    scope.launch { dao.deleteById(item.id) }
                                }
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will remove all document records from your processing history log.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { dao.clearAll() }
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = SmallpdfError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
