package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class PdfBookmark(
    val title: String,
    val pageIndex: Int
)

data class PdfSearchResult(
    val query: String,
    val matches: List<PdfPageMatch>,
    val totalOccurrences: Int
)

data class PdfPageMatch(
    val pageIndex: Int,
    val snippets: List<String>,
    val count: Int
)

object PdfAdvancedEngine {

    fun extractBookmarks(file: File): List<PdfBookmark> {
        val result = mutableListOf<PdfBookmark>()
        try {
            PDDocument.load(file).use { doc ->
                val outline = doc.documentCatalog.documentOutline ?: return emptyList()
                var item = outline.firstChild
                var fallbackIndex = 0
                while (item != null) {
                    var pageIndex = fallbackIndex
                    try {
                        val page = item.findDestinationPage(doc)
                        if (page != null) {
                            pageIndex = doc.pages.indexOf(page)
                        }
                    } catch (_: Exception) {}

                    val title = item.title?.takeIf { it.isNotBlank() } ?: "Section ${fallbackIndex + 1}"
                    result.add(PdfBookmark(title, pageIndex.coerceAtLeast(0)))
                    fallbackIndex++
                    item = item.nextSibling
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    suspend fun searchInPdf(file: File, query: String): PdfSearchResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext PdfSearchResult(query, emptyList(), 0)

        val matches = mutableListOf<PdfPageMatch>()
        var total = 0

        try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                val totalPages = doc.numberOfPages

                for (p in 0 until totalPages) {
                    stripper.startPage = p + 1
                    stripper.endPage = p + 1
                    val text = stripper.getText(doc)

                    if (text.contains(trimmed, ignoreCase = true)) {
                        val parts = text.split(Regex("(?i)" + Regex.escape(trimmed)))
                        val count = (parts.size - 1).coerceAtLeast(1)
                        total += count

                        val idx = text.indexOf(trimmed, ignoreCase = true)
                        val start = (idx - 35).coerceAtLeast(0)
                        val end = (idx + trimmed.length + 35).coerceAtMost(text.length)
                        val snippet = "..." + text.substring(start, end).replace("\n", " ").trim() + "..."

                        matches.add(PdfPageMatch(pageIndex = p, snippets = listOf(snippet), count = count))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext PdfSearchResult(trimmed, matches, total)
    }

    fun extractPageText(file: File, pageIndex: Int): String {
        return try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                stripper.getText(doc).trim()
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun printPdfFile(context: Context, file: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val pInfo = PrintDocumentInfo.Builder(file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(pInfo, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print("Smallpdf - ${file.name}", printAdapter, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveReadingProgress(context: Context, filePath: String, pageIndex: Int, zoomScale: Float) {
        try {
            val prefs = context.getSharedPreferences("smallpdf_reading_progress", Context.MODE_PRIVATE)
            val key = "progress_" + filePath.hashCode()
            prefs.edit()
                .putInt("${key}_page", pageIndex)
                .putFloat("${key}_scale", zoomScale)
                .apply()
        } catch (_: Exception) {}
    }

    fun getReadingProgress(context: Context, filePath: String): Pair<Int, Float> {
        return try {
            val prefs = context.getSharedPreferences("smallpdf_reading_progress", Context.MODE_PRIVATE)
            val key = "progress_" + filePath.hashCode()
            val page = prefs.getInt("${key}_page", 0)
            val scale = prefs.getFloat("${key}_scale", 1f)
            Pair(page, scale)
        } catch (_: Exception) {
            Pair(0, 1f)
        }
    }

    suspend fun addAnnotationToPdf(
        context: Context,
        inputFile: File,
        targetPageIndex: Int,
        noteText: String,
        noteColorHex: Long = 0xFF0B57F9,
        xPercent: Float = 0.5f,
        yPercent: Float = 0.2f
    ): OperationResult = withContext(Dispatchers.IO) {
        val totalPages = PdfRendererHelper.getPageCount(inputFile)
        if (totalPages <= 0) return@withContext OperationResult(false, message = "Could not parse document.")

        val outFile = FileUtils.createOutputFile(context, "Annotated", "pdf")

        try {
            val pdfDoc = android.graphics.pdf.PdfDocument()

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val bgPaint = Paint().apply {
                color = noteColorHex.toInt()
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            for (i in 0 until totalPages) {
                val pageBmp = PdfRendererHelper.renderPageToBitmap(inputFile, i, scale = 1.5f) ?: continue

                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    pageBmp.width,
                    pageBmp.height,
                    i + 1
                ).create()

                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawBitmap(pageBmp, 0f, 0f, null)

                if (i == targetPageIndex) {
                    val boxW = 400f
                    val boxH = 90f
                    val centerX = pageBmp.width * xPercent
                    val centerY = pageBmp.height * yPercent

                    val rect = RectF(
                        centerX - boxW / 2f,
                        centerY - boxH / 2f,
                        centerX + boxW / 2f,
                        centerY + boxH / 2f
                    )

                    canvas.drawRoundRect(rect, 16f, 16f, bgPaint)
                    canvas.drawText(noteText, centerX, centerY + 10f, textPaint)
                }

                pdfDoc.finishPage(page)
                pageBmp.recycle()
            }

            FileOutputStream(outFile).use { out -> pdfDoc.writeTo(out) }
            pdfDoc.close()

            return@withContext OperationResult(
                isSuccess = true,
                outputFile = outFile,
                originalSizeBytes = inputFile.length(),
                outputSizeBytes = outFile.length(),
                message = "Annotation stamp saved to page ${targetPageIndex + 1}!"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(false, message = "Annotation failed: ${e.localizedMessage}")
        }
    }
}
