package com.example.core.pdf

import android.content.Context
import android.graphics.Color
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfSplitEngine {

    /**
     * Parses ranges like "1-3, 5, 7-10" into list of 0-based page indices.
     */
    fun parsePageRanges(rangeString: String, maxPages: Int): List<List<Int>> {
        val result = mutableListOf<List<Int>>()
        val parts = rangeString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        for (part in parts) {
            if (part.contains("-")) {
                val rangeParts = part.split("-").map { it.trim() }
                if (rangeParts.size == 2) {
                    val start = (rangeParts[0].toIntOrNull() ?: 1).coerceIn(1, maxPages)
                    val end = (rangeParts[1].toIntOrNull() ?: maxPages).coerceIn(1, maxPages)
                    val pages = if (start <= end) {
                        (start..end).map { it - 1 }
                    } else {
                        (end..start).map { it - 1 }
                    }
                    if (pages.isNotEmpty()) result.add(pages)
                }
            } else {
                val single = part.toIntOrNull()
                if (single != null && single in 1..maxPages) {
                    result.add(listOf(single - 1))
                }
            }
        }
        return result
    }

    suspend fun splitPdfByRanges(
        context: Context,
        inputFile: File,
        rangeExpression: String
    ): OperationResult = withContext(Dispatchers.IO) {
        val totalPages = PdfRendererHelper.getPageCount(inputFile)
        if (totalPages <= 0) {
            return@withContext OperationResult(isSuccess = false, message = "Could not read pages from PDF.")
        }

        val parsedRanges = parsePageRanges(rangeExpression, totalPages)
        if (parsedRanges.isEmpty()) {
            return@withContext OperationResult(
                isSuccess = false,
                message = "Invalid page range. E.g. enter '1-2, 3-5' or '1, 3'."
            )
        }

        val generatedFiles = mutableListOf<File>()
        var totalOutputSize = 0L

        try {
            // First attempt with PDFBox for clean vector page extraction
            var sourceDoc: PDDocument? = null
            try {
                sourceDoc = PDDocument.load(inputFile)
            } catch (_: Exception) {}

            for ((index, pageIndices) in parsedRanges.withIndex()) {
                val outFile = FileUtils.createOutputFile(context, "Split_Part_${index + 1}", "pdf")

                if (sourceDoc != null) {
                    try {
                        val newDoc = PDDocument()
                        for (pIndex in pageIndices) {
                            if (pIndex in 0 until sourceDoc.numberOfPages) {
                                newDoc.addPage(sourceDoc.getPage(pIndex))
                            }
                        }
                        newDoc.save(outFile)
                        newDoc.close()
                    } catch (_: Exception) {
                        outFile.delete()
                    }
                }

                // If PDFBox did not create it or threw, use PdfRenderer fallback
                if (!outFile.exists() || outFile.length() == 0L) {
                    val fallbackDoc = android.graphics.pdf.PdfDocument()
                    for ((pNum, pIndex) in pageIndices.withIndex()) {
                        val bmp = PdfRendererHelper.renderPageToBitmap(inputFile, pIndex, scale = 1.5f) ?: continue
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, pNum + 1).create()
                        val page = fallbackDoc.startPage(pageInfo)
                        page.canvas.drawColor(Color.WHITE)
                        page.canvas.drawBitmap(bmp, 0f, 0f, null)
                        fallbackDoc.finishPage(page)
                        bmp.recycle()
                    }
                    FileOutputStream(outFile).use { out -> fallbackDoc.writeTo(out) }
                    fallbackDoc.close()
                }

                if (outFile.exists() && outFile.length() > 0) {
                    generatedFiles.add(outFile)
                    totalOutputSize += outFile.length()
                }
            }

            sourceDoc?.close()

            if (generatedFiles.isNotEmpty()) {
                return@withContext OperationResult(
                    isSuccess = true,
                    outputFile = generatedFiles.first(),
                    outputFiles = generatedFiles,
                    originalSizeBytes = inputFile.length(),
                    outputSizeBytes = totalOutputSize,
                    message = "Split into ${generatedFiles.size} PDF document(s) successfully!"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(
                isSuccess = false,
                message = "Split operation failed: ${e.localizedMessage}"
            )
        }

        return@withContext OperationResult(isSuccess = false, message = "Could not split PDF.")
    }
}
