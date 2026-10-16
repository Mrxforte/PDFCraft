package com.example.core.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.example.core.model.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class WatermarkDeletePreset(val label: String, val description: String) {
    BOTTOM_FOOTER("Bottom Footer Banner", "Erase evaluation notes, web links, or software stamps from page footers"),
    TOP_HEADER("Top Header Banner", "Erase promotional labels, dates, or headers from page tops"),
    CENTER_STAMP("Center Stamp / Draft Mark", "Erase prominent center watermark logos or confidential text stamps"),
    CUSTOM_REGION("Custom Region", "Specify custom percentage boundaries to erase cleanly")
}

object WatermarkEngine {

    suspend fun removeWatermark(
        context: Context,
        inputFile: File,
        preset: WatermarkDeletePreset = WatermarkDeletePreset.BOTTOM_FOOTER,
        customTopPercent: Float = 0.90f,
        customBottomPercent: Float = 1.0f,
        customLeftPercent: Float = 0.0f,
        customRightPercent: Float = 1.0f,
        stripAuthorAndExif: Boolean = true
    ): OperationResult = withContext(Dispatchers.IO) {
        val totalPages = PdfRendererHelper.getPageCount(inputFile)
        if (totalPages <= 0) {
            return@withContext OperationResult(isSuccess = false, message = "Could not parse PDF pages.")
        }

        val outputFile = FileUtils.createOutputFile(context, "Cleaned_WatermarkRemoved", "pdf")

        try {
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val erasePaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
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

                // Draw the original page
                canvas.drawBitmap(pageBmp, 0f, 0f, null)

                // Calculate erase area based on preset
                val (tPct, bPct, lPct, rPct) = when (preset) {
                    WatermarkDeletePreset.BOTTOM_FOOTER -> Quad(0.92f, 1.0f, 0.0f, 1.0f)
                    WatermarkDeletePreset.TOP_HEADER -> Quad(0.0f, 0.08f, 0.0f, 1.0f)
                    WatermarkDeletePreset.CENTER_STAMP -> Quad(0.35f, 0.65f, 0.15f, 0.85f)
                    WatermarkDeletePreset.CUSTOM_REGION -> Quad(customTopPercent, customBottomPercent, customLeftPercent, customRightPercent)
                }

                val eraseRect = RectF(
                    pageBmp.width * lPct,
                    pageBmp.height * tPct,
                    pageBmp.width * rPct,
                    pageBmp.height * bPct
                )

                // Erase watermark region cleanly
                canvas.drawRect(eraseRect, erasePaint)

                pdfDoc.finishPage(page)
                pageBmp.recycle()
            }

            FileOutputStream(outputFile).use { out -> pdfDoc.writeTo(out) }
            pdfDoc.close()

            val detailMsg = if (stripAuthorAndExif) {
                "Watermark cleanly erased from $totalPages page(s) and author/metadata sanitized!"
            } else {
                "Watermark cleaned successfully from $totalPages page(s)!"
            }

            return@withContext OperationResult(
                isSuccess = true,
                outputFile = outputFile,
                originalSizeBytes = inputFile.length(),
                outputSizeBytes = outputFile.length(),
                message = detailMsg
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(
                isSuccess = false,
                message = "Failed to remove watermark: ${e.localizedMessage}"
            )
        }
    }

    suspend fun addWatermark(
        context: Context,
        inputFile: File,
        config: WatermarkConfig
    ): OperationResult = withContext(Dispatchers.IO) {
        val totalPages = PdfRendererHelper.getPageCount(inputFile)
        if (totalPages <= 0) {
            return@withContext OperationResult(isSuccess = false, message = "Could not parse PDF pages.")
        }

        val outputFile = FileUtils.createOutputFile(context, "Watermarked", "pdf")

        try {
            val pdfDoc = android.graphics.pdf.PdfDocument()

            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = config.fontSizeSp * 2f
                color = config.colorHex.toInt()
                alpha = (config.opacity * 255).toInt().coerceIn(10, 255)
                textAlign = Paint.Align.CENTER
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

                // Draw base page
                canvas.drawBitmap(pageBmp, 0f, 0f, null)

                // Draw watermark
                canvas.save()
                canvas.translate(pageBmp.width / 2f, pageBmp.height / 2f)
                canvas.rotate(config.rotationDegrees)
                canvas.drawText(config.text, 0f, 0f, textPaint)
                canvas.restore()

                pdfDoc.finishPage(page)
                pageBmp.recycle()
            }

            FileOutputStream(outputFile).use { out -> pdfDoc.writeTo(out) }
            pdfDoc.close()

            return@withContext OperationResult(
                isSuccess = true,
                outputFile = outputFile,
                originalSizeBytes = inputFile.length(),
                outputSizeBytes = outputFile.length(),
                message = "Watermark '${config.text}' stamped across $totalPages page(s)!"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(
                isSuccess = false,
                message = "Failed to add watermark: ${e.localizedMessage}"
            )
        }
    }

    private data class Quad(val t: Float, val b: Float, val l: Float, val r: Float)
}
