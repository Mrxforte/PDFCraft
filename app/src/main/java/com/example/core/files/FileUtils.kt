package com.example.core.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.core.model.PdfFileInfo
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun getPdfFileInfo(context: Context, uri: Uri): PdfFileInfo {
        var name = "document.pdf"
        var size: Long = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: "document.pdf"
                }
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        // Copy to temp to get page count safely if needed
        val tempFile = copyUriToTemp(context, uri, "probe_")
        if (size <= 0L && tempFile.exists()) {
            size = tempFile.length()
        }

        var pages = 1
        try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                pages = renderer.pageCount
                renderer.close()
            }
        } catch (_: Exception) {}

        return PdfFileInfo(
            uri = uri,
            name = name,
            sizeBytes = size,
            pageCount = pages,
            localFile = tempFile
        )
    }

    fun copyUriToTemp(context: Context, uri: Uri, prefix: String = "temp_"): File {
        val extension = when {
            uri.toString().endsWith(".jpg", true) || uri.toString().endsWith(".jpeg", true) -> ".jpg"
            uri.toString().endsWith(".png", true) -> ".png"
            else -> ".pdf"
        }
        val tempFile = File(context.cacheDir, "${prefix}${System.currentTimeMillis()}$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun createOutputFile(context: Context, namePrefix: String, extension: String = "pdf"): File {
        val safeExt = if (extension.startsWith(".")) extension else ".$extension"
        val outputDir = File(context.filesDir, "processed_pdfs").apply { mkdirs() }
        val fileName = "${namePrefix}_${System.currentTimeMillis()}$safeExt"
        return File(outputDir, fileName)
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Document via Smallpdf"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun viewFileIntent(context: Context, file: File, mimeType: String = "application/pdf"): Intent {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
