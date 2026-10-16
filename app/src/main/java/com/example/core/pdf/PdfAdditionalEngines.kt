package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfToImageEngine {

    suspend fun convertPdfToImages(
        context: Context,
        inputFile: File,
        format: String = "PNG",
        scale: Float = 1.5f
    ): OperationResult = withContext(Dispatchers.IO) {
        val totalPages = PdfRendererHelper.getPageCount(inputFile)
        if (totalPages <= 0) return@withContext OperationResult(false, message = "Could not read pages.")

        val outputFiles = mutableListOf<File>()
        val ext = format.lowercase()
        val compressFormat = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        for (i in 0 until totalPages) {
            val bmp = PdfRendererHelper.renderPageToBitmap(inputFile, i, scale) ?: continue
            val imgFile = FileUtils.createOutputFile(context, "Page_${i + 1}", ext)
            FileOutputStream(imgFile).use { out ->
                bmp.compress(compressFormat, 90, out)
            }
            bmp.recycle()
            outputFiles.add(imgFile)
        }

        // Also create a single ZIP if multiple pages
        var zipFile: File? = null
        if (outputFiles.size > 1) {
            zipFile = FileUtils.createOutputFile(context, "PDF_Pages_Images", "zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                outputFiles.forEach { f ->
                    zip.putNextEntry(ZipEntry(f.name))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }

        val primaryFile = zipFile ?: outputFiles.firstOrNull()

        return@withContext OperationResult(
            isSuccess = outputFiles.isNotEmpty(),
            outputFile = primaryFile,
            outputFiles = outputFiles,
            originalSizeBytes = inputFile.length(),
            outputSizeBytes = primaryFile?.length() ?: 0L,
            message = "Exported $totalPages page(s) to ${format.uppercase()} images!"
        )
    }
}

object ImageToPdfEngine {

    suspend fun convertImagesToPdf(
        context: Context,
        imageFiles: List<File>,
        outputName: String = "ImagesToPdf"
    ): OperationResult = withContext(Dispatchers.IO) {
        if (imageFiles.isEmpty()) return@withContext OperationResult(false, message = "No images selected.")

        val outFile = FileUtils.createOutputFile(context, outputName, "pdf")
        var totalInputSize = 0L

        try {
            val pdfDoc = android.graphics.pdf.PdfDocument()

            for ((idx, file) in imageFiles.withIndex()) {
                totalInputSize += file.length()
                val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: continue

                // Standard A4 aspect or bitmap size
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    bmp.width,
                    bmp.height,
                    idx + 1
                ).create()

                val page = pdfDoc.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdfDoc.finishPage(page)
                bmp.recycle()
            }

            FileOutputStream(outFile).use { out -> pdfDoc.writeTo(out) }
            pdfDoc.close()

            return@withContext OperationResult(
                isSuccess = true,
                outputFile = outFile,
                originalSizeBytes = totalInputSize,
                outputSizeBytes = outFile.length(),
                message = "Converted ${imageFiles.size} images to 1 PDF document!"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(false, message = "Conversion error: ${e.localizedMessage}")
        }
    }
}

object PdfSignEngine {

    suspend fun applySignatureToPdf(
        context: Context,
        inputFile: File,
        signatureBitmap: Bitmap,
        targetPageIndex: Int,
        xPercent: Float,
        yPercent: Float,
        widthPercent: Float = 0.35f
    ): OperationResult = withContext(Dispatchers.IO) {
        val totalPages = PdfRendererHelper.getPageCount(inputFile)
        if (totalPages <= 0) return@withContext OperationResult(false, message = "Invalid PDF file.")

        val outFile = FileUtils.createOutputFile(context, "Signed_Document", "pdf")

        try {
            val pdfDoc = android.graphics.pdf.PdfDocument()

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
                    val sigW = (pageBmp.width * widthPercent).toInt().coerceAtLeast(100)
                    val aspectRatio = signatureBitmap.height.toFloat() / signatureBitmap.width.toFloat()
                    val sigH = (sigW * aspectRatio).toInt().coerceAtLeast(40)

                    val scaledSig = Bitmap.createScaledBitmap(signatureBitmap, sigW, sigH, true)
                    val posX = pageBmp.width * xPercent - (sigW / 2f)
                    val posY = pageBmp.height * yPercent - (sigH / 2f)

                    canvas.drawBitmap(scaledSig, posX.coerceAtLeast(0f), posY.coerceAtLeast(0f), null)
                    scaledSig.recycle()
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
                message = "Signature successfully applied to Page ${targetPageIndex + 1}!"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(false, message = "Signing failed: ${e.localizedMessage}")
        }
    }
}

object PdfProtectEngine {

    suspend fun protectPdf(
        context: Context,
        inputFile: File,
        password: String
    ): OperationResult = withContext(Dispatchers.IO) {
        if (password.length < 4) return@withContext OperationResult(false, message = "Password must be at least 4 characters.")

        val outFile = FileUtils.createOutputFile(context, "Protected", "pdf")

        try {
            val doc = PDDocument.load(inputFile)
            val ap = AccessPermission()
            val spp = StandardProtectionPolicy(password, password, ap).apply {
                encryptionKeyLength = 128
                permissions = ap
            }
            doc.protect(spp)
            doc.save(outFile)
            doc.close()

            return@withContext OperationResult(
                isSuccess = true,
                outputFile = outFile,
                originalSizeBytes = inputFile.length(),
                outputSizeBytes = outFile.length(),
                message = "Document password protected successfully!"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OperationResult(false, message = "Protection failed: ${e.localizedMessage}")
        }
    }
}
