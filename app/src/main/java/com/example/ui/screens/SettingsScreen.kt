package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CompressionLevel
import com.example.ui.components.SmallpdfTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cacheSizeBytes by remember { mutableStateOf(0L) }
    var selectedDefaultCompression by remember { mutableStateOf(CompressionLevel.BALANCED) }

    fun refreshCacheSize() {
        scope.launch(Dispatchers.IO) {
            val total = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            cacheSizeBytes = total
        }
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    Scaffold(
        topBar = { SmallpdfTopBar(title = "Settings") },
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
            // Appearance Section
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SmallpdfNavy
                    )
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SmallpdfBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SmallpdfInfoBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = null,
                                    tint = SmallpdfPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dark Theme",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SmallpdfTextPrimary
                                )
                                Text(
                                    text = if (darkTheme) "Deep Navy & Charcoal" else "Clean Blue & Crisp White",
                                    fontSize = 12.sp,
                                    color = SmallpdfTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = darkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SmallpdfPrimary
                            )
                        )
                    }
                }
            }

            // Storage & Cache Section
            item {
                Text(
                    text = "Storage & Cache",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SmallpdfNavy
                    )
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SmallpdfBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Temporary Cache Files",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SmallpdfTextPrimary
                                )
                                Text(
                                    text = "${(cacheSizeBytes / 1024)} KB currently occupied",
                                    fontSize = 12.sp,
                                    color = SmallpdfTextSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        context.cacheDir.walkTopDown().forEach {
                                            if (it.isFile) it.delete()
                                        }
                                        refreshCacheSize()
                                        snackbarHostState.showSnackbar("Temporary cache cleared!")
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SmallpdfInfoBg),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Clear Cache", color = SmallpdfPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Privacy & Security Disclosure
            item {
                Text(
                    text = "Security & Privacy",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SmallpdfNavy
                    )
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SmallpdfBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = SmallpdfSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% On-Device Processing",
                                fontWeight = FontWeight.Bold,
                                color = SmallpdfNavy,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "PDF merging, splitting, compressing, watermark deleting, and OCR scanning are performed locally on your device. Your confidential files are never uploaded to any remote server.",
                            style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary, lineHeight = 18.sp)
                        )
                    }
                }
            }

            // About Section
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SmallpdfBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Smallpdf for Android",
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Version 1.0.0 • Production Build",
                            fontSize = 12.sp,
                            color = SmallpdfTextSecondary
                        )
                    }
                }
            }
        }
    }
}
