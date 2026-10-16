package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.OperationResult
import com.example.core.model.PdfFileInfo
import com.example.ui.theme.*

@Composable
fun FilePickerCard(
    selectedFiles: List<PdfFileInfo>,
    onPickFiles: () -> Unit,
    onRemoveFile: (Int) -> Unit,
    modifier: Modifier = Modifier,
    multipleAllowed: Boolean = true,
    fileTypeLabel: String = "PDF"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_picker_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, SmallpdfBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedFiles.isEmpty()) {
                // Dropzone Illustration & Call to Action
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SmallpdfInfoBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = "Upload",
                        tint = SmallpdfPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPickFiles,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("choose_files_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (multipleAllowed) "Choose Files" else "Choose $fileTypeLabel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (multipleAllowed) "Select one or multiple $fileTypeLabel documents" else "Select a $fileTypeLabel file to process",
                    style = MaterialTheme.typography.bodySmall.copy(color = SmallpdfTextSecondary)
                )
            } else {
                // Selected Files List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Files (${selectedFiles.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmallpdfNavy
                        )
                    )

                    if (multipleAllowed) {
                        TextButton(onClick = onPickFiles) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add More", color = SmallpdfPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                selectedFiles.forEachIndexed { index, file ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = SmallpdfBackground,
                        border = BorderStroke(1.dp, SmallpdfBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ToolCompress),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = SmallpdfTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${file.formattedSize} • ${file.pageCount} page(s)",
                                    fontSize = 12.sp,
                                    color = SmallpdfTextSecondary
                                )
                            }

                            IconButton(
                                onClick = { onRemoveFile(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = SmallpdfTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingProgressCard(
    currentStep: String,
    progress: Float? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("processing_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SmallpdfBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SmallpdfInfoBg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SmallpdfPrimary,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Processing Document...",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SmallpdfNavy
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = currentStep,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SmallpdfTextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = SmallpdfPrimary,
                    trackColor = SmallpdfBorder
                )
            }

            if (onCancel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = SmallpdfError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SuccessResultCard(
    result: OperationResult,
    onOpenFile: () -> Unit,
    onShareFile: () -> Unit,
    onProcessAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("success_result_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SmallpdfSuccess,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Operation Completed!",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SmallpdfNavy
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SmallpdfTextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            if (result.percentageSaved > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Text(
                        text = "🎉 Saved ${result.percentageSaved}% in file size!",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = SmallpdfSuccess,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenFile,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SmallpdfPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_result_button")
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShareFile,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SmallpdfBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_result_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = SmallpdfNavy, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", color = SmallpdfNavy, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onProcessAnother) {
                Text("Process another file", color = SmallpdfPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
