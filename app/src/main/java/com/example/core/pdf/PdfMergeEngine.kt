package com.example.core.pdf

import android.content.Context
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfMergeEngine {

    suspend fun mergePdfs(
        context: Context,
        inputFiles: List<File>,
        outputNamePrefix: String = "Merged_Smallpdf"
    ): OperationResult = withContext(Dispatchers.IO) {
        if (inputFiles.isEmpty()) {
            return@withContext OperationResult(
                isSuccess = false,
                message = "Please select at least 2 PDF files to merge."
            )
        }

        val outputFile = FileUtils.createOutputFile(context, outputNamePrefix, "pdf")
        var totalOriginalSize = 0L
        inputFiles.forEach { totalOriginalSize += it.length() }

        try {
            // Attempt high-fidelity PDFBox vector merge first
            val merger = PDFMergerUtility()
            merger.destinationFileName = outputFile.absolutePath
            val openDocs = mutableListOf<PDDocument>()

            for (file in inputFiles) {
                if (file.exists() && file.length() > 0) {
                    val doc = PDDocument.load(file)
                    openDocs.add(doc)
                    merger.addSource(file)
                }
            }

            merger.mergeDocuments(null)
            openDocs.forEach { try { it.close() } catch (_: Exception) {} }

            if (outputFile.exists() && outputFile.length() > 0) {
                return@withContext OperationResult(
                    isSuccess = true,
                    outputFile = outputFile,
                    originalSizeBytes = totalOriginalSize,
                    outputSizeBytes = outputFile.length(),
                    message = "Successfully merged ${inputFiles.size} documents into 1 PDF!"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Resilient Fallback: PdfRenderer + PdfDocument page synthesis
        try {
            val newPdfDoc = android.graphics.pdf.PdfDocument()
            var pageIndexGlobal = 0

            for (file in inputFiles) {
                val count = PdfRendererHelper.getPageCount(file)
                for (p in 0 until count) {
                    val bmp = PdfRendererHelper.renderPageToBitmap(file, p, scale = 1.5f) ?: continue
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, pageIndexGlobal + 1).create()
                    val page = newPdfDoc.startPage(pageInfo)
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                    newPdfDoc.finishPage(page)
                    bmp.recycle()
                    pageIndexGlobal++
                }
            }

            FileOutputStream(outputFile).use { out ->
                newPdfDoc.writeTo(out)
            }
            newPdfDoc.close()

            if (outputFile.exists() && outputFile.length() > 0) {
                return@withContext OperationResult(
                    isSuccess = true,
                    outputFile = outputFile,
                    originalSizeBytes = totalOriginalSize,
                    outputSizeBytes = outputFile.length(),
                    message = "Merged $pageIndexGlobal pages successfully!"
                )
            }
        } catch (e: Exception) {
            return@withContext OperationResult(
                isSuccess = false,
                errorDetails = e.localizedMessage ?: "Unknown merge error",
                message = "Failed to merge PDF files. Please verify the files are not password-encrypted."
            )
        }

        return@withContext OperationResult(
            isSuccess = false,
            message = "Merge operation failed to generate output."
        )
    }
}
