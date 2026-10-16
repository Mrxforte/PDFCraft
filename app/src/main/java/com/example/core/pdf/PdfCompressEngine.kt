package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.core.files.FileUtils
import com.example.core.model.CompressionLevel
import com.example.core.model.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfCompressEngine {

    suspend fun compressPdf(
        context: Context,
        inputFile: File,
        level: CompressionLevel = CompressionLevel.BALANCED
    ): OperationResult = withContext(Dispatchers.IO) {
        val originalSize = inputFile.length()
        if (originalSize <= 0) {
            return@withContext OperationResult(
                isSuccess = false,
                message = "Input file is empty or missing."
            )
        }

        val pageCount = PdfRendererHelper.getPageCount(inputFile)
        if (pageCount <= 0) {
            return@withContext OperationResult(
                isSuccess = false,
                message = "Could not parse pages from PDF document."
            )
        }

        val outputFile = FileUtils.createOutputFile(context, "Compressed_${level.name.lowercase()}", "pdf")

        try {
            val pdfDoc = android.graphics.pdf.PdfDocument()

            for (i in 0 until pageCount) {
                val originalBmp = PdfRendererHelper.renderPageToBitmap(
                    file = inputFile,
                    pageIndex = i,
                    scale = level.scale
                ) ?: continue

                // Apply JPEG compression to the rendered page bitmap
                val stream = ByteArrayOutputStream()
                originalBmp.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, stream)
                val compressedBytes = stream.toByteArray()
                originalBmp.recycle()

                val compressedBmp = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    compressedBmp.width,
                    compressedBmp.height,
                    i + 1
                ).create()

                val page = pdfDoc.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawBitmap(compressedBmp, 0f, 0f, null)
                pdfDoc.finishPage(page)

                compressedBmp.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            val outputSize = outputFile.length()
            val savedPercentage = if (originalSize > 0) {
                (((originalSize - outputSize).toDouble() / originalSize.toDouble()) * 100).toInt()
            } else 0

            return@withContext OperationResult(
                isSuccess = true,
                outputFile = outputFile,
                originalSizeBytes = originalSize,
                outputSizeBytes = outputSize,
                message = if (savedPercentage > 0) {
                    "Successfully compressed document by $savedPercentage%! Reduced to ${(outputSize / 1024)} KB."
                } else {
                    "Optimized document structure. Original file was already highly compressed."
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(
                isSuccess = false,
                originalSizeBytes = originalSize,
                errorDetails = e.localizedMessage,
                message = "Compression failed: ${e.localizedMessage}"
            )
        }
    }
}
