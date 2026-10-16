package com.example.core.model

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

data class PdfFileInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int = 0,
    val localFile: File? = null
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$sizeBytes B"
            }
        }
}

data class PageThumbnail(
    val pageIndex: Int,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    var isSelected: Boolean = true,
    var rotationDegrees: Int = 0
)

enum class CompressionLevel(val title: String, val reductionEstimate: String, val description: String, val scale: Float, val jpegQuality: Int) {
    BASIC("Basic Compression", "~40% smaller", "Maintains highest visual fidelity and crisp vector graphics.", 0.9f, 85),
    BALANCED("Balanced Compression", "~65% smaller", "Optimal balance between small file size and clear reading quality.", 0.7f, 70),
    STRONG("Strong Compression", "~85% smaller", "Maximum size reduction for fast sharing and low storage limits.", 0.5f, 50)
}

data class WatermarkConfig(
    val text: String = "CONFIDENTIAL",
    val fontSizeSp: Float = 42f,
    val colorHex: Long = 0xFF6366F1,
    val opacity: Float = 0.35f,
    val rotationDegrees: Float = -45f,
    val repeatAcrossPage: Boolean = true
)

data class WatermarkRegion(
    val title: String,
    val topPercent: Float,
    val bottomPercent: Float,
    val leftPercent: Float = 0f,
    val rightPercent: Float = 1f
)

data class OcrBlock(
    val text: String,
    val boundingBox: String? = null
)

data class OcrPageResult(
    val pageNumber: Int,
    val fullText: String,
    val blocks: List<OcrBlock>
)

data class OcrDocumentResult(
    val totalPages: Int,
    val combinedText: String,
    val pageResults: List<OcrPageResult>,
    val outputDocxFile: File? = null,
    val outputTxtFile: File? = null
)

data class OperationResult(
    val isSuccess: Boolean,
    val outputFile: File? = null,
    val outputFiles: List<File> = emptyList(),
    val originalSizeBytes: Long = 0L,
    val outputSizeBytes: Long = 0L,
    val message: String = "",
    val errorDetails: String? = null
) {
    val percentageSaved: Int
        get() {
            if (originalSizeBytes <= 0L || outputSizeBytes <= 0L) return 0
            val saved = ((originalSizeBytes - outputSizeBytes).toDouble() / originalSizeBytes.toDouble()) * 100
            return saved.toInt().coerceIn(0, 99)
        }
}
