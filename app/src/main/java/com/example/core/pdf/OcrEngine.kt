package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.core.files.FileUtils
import com.example.core.model.OcrBlock
import com.example.core.model.OcrDocumentResult
import com.example.core.model.OcrPageResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeBitmapText(bitmap: Bitmap): List<OcrBlock> = suspendCancellableCoroutine { cont ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        OcrBlock(
                            text = block.text,
                            boundingBox = block.boundingBox?.toShortString()
                        )
                    }
                    cont.resume(blocks)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    cont.resume(emptyList())
                }
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resume(emptyList())
        }
    }

    suspend fun processPdfOcr(
        context: Context,
        inputFile: File,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): OcrDocumentResult = withContext(Dispatchers.IO) {
        val totalPages = PdfRendererHelper.getPageCount(inputFile)
        val pageResults = mutableListOf<OcrPageResult>()
        val fullDocTextBuilder = StringBuilder()

        // First attempt PDFBox text stripper (for native vector text)
        var pdfBoxText: String? = null
        try {
            PDDocument.load(inputFile).use { doc ->
                val stripper = PDFTextStripper()
                pdfBoxText = stripper.getText(doc)
            }
        } catch (_: Exception) {}

        for (i in 0 until totalPages) {
            progressCallback?.invoke(i + 1, totalPages)
            val bmp = PdfRendererHelper.renderPageToBitmap(inputFile, i, scale = 1.5f)
            if (bmp != null) {
                val blocks = recognizeBitmapText(bmp)
                val pageText = blocks.joinToString("\n") { it.text }

                // If OCR detected text or we had native text
                val finalText = if (pageText.isNotBlank()) {
                    pageText
                } else if (!pdfBoxText.isNullOrBlank()) {
                    // Fallback to text stripper slice if empty image scan
                    pdfBoxText?.lines()?.take(50)?.joinToString("\n") ?: ""
                } else {
                    "[No text detected on page ${i + 1}]"
                }

                pageResults.add(
                    OcrPageResult(
                        pageNumber = i + 1,
                        fullText = finalText,
                        blocks = blocks
                    )
                )
                fullDocTextBuilder.append("--- Page ${i + 1} ---\n").append(finalText).append("\n\n")
                bmp.recycle()
            }
        }

        val combinedText = fullDocTextBuilder.toString()

        // Generate editable Word (.docx) file
        val docxFile = FileUtils.createOutputFile(context, "OCR_Converted", "docx")
        try {
            val paragraphs = combinedText.split("\n\n").filter { it.isNotBlank() }
            DocxGenerator.createDocxFile(docxFile, paragraphs, title = inputFile.nameWithoutExtension)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Generate Plain Text (.txt) file
        val txtFile = FileUtils.createOutputFile(context, "OCR_Extracted", "txt")
        try {
            txtFile.writeText(combinedText)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext OcrDocumentResult(
            totalPages = totalPages,
            combinedText = combinedText,
            pageResults = pageResults,
            outputDocxFile = if (docxFile.exists()) docxFile else null,
            outputTxtFile = if (txtFile.exists()) txtFile else null
        )
    }

    suspend fun processImageOcr(
        context: Context,
        imageFile: File
    ): OcrDocumentResult = withContext(Dispatchers.IO) {
        val bmp = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: return@withContext OcrDocumentResult(0, "Failed to load image file.", emptyList())

        val blocks = recognizeBitmapText(bmp)
        val imageText = blocks.joinToString("\n") { it.text }
        bmp.recycle()

        val pageResult = OcrPageResult(
            pageNumber = 1,
            fullText = imageText,
            blocks = blocks
        )

        // Generate editable Word (.docx) file
        val docxFile = FileUtils.createOutputFile(context, "Image_OCR_Word", "docx")
        try {
            val paragraphs = imageText.split("\n\n").filter { it.isNotBlank() }
            DocxGenerator.createDocxFile(docxFile, paragraphs, title = imageFile.nameWithoutExtension)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val txtFile = FileUtils.createOutputFile(context, "Image_OCR_Text", "txt")
        try {
            txtFile.writeText(imageText)
        } catch (_: Exception) {}

        return@withContext OcrDocumentResult(
            totalPages = 1,
            combinedText = imageText,
            pageResults = listOf(pageResult),
            outputDocxFile = if (docxFile.exists()) docxFile else null,
            outputTxtFile = if (txtFile.exists()) txtFile else null
        )
    }
}
