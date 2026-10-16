package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.example.core.files.FileUtils
import com.example.core.model.OperationResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class FileMetadataInfo(
    val fileName: String,
    val isPdf: Boolean,
    val author: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val title: String? = null,
    val creationDate: String? = null,
    val cameraDevice: String? = null,
    val gpsCoordinates: String? = null,
    val software: String? = null,
    val totalTagsCount: Int = 0
)

object MetadataEngine {

    suspend fun readMetadata(inputFile: File): FileMetadataInfo = withContext(Dispatchers.IO) {
        val name = inputFile.name
        val isPdf = name.endsWith(".pdf", ignoreCase = true)

        if (isPdf) {
            try {
                PDDocument.load(inputFile).use { doc ->
                    val info = doc.documentInformation
                    val author = info.author?.takeIf { it.isNotBlank() }
                    val creator = info.creator?.takeIf { it.isNotBlank() }
                    val producer = info.producer?.takeIf { it.isNotBlank() }
                    val title = info.title?.takeIf { it.isNotBlank() }
                    val cDate = info.creationDate?.time?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                    }

                    var count = 0
                    if (author != null) count++
                    if (creator != null) count++
                    if (producer != null) count++
                    if (title != null) count++
                    if (cDate != null) count++

                    return@withContext FileMetadataInfo(
                        fileName = name,
                        isPdf = true,
                        author = author,
                        creator = creator,
                        producer = producer,
                        title = title,
                        creationDate = cDate,
                        totalTagsCount = count
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext FileMetadataInfo(fileName = name, isPdf = true)
        } else {
            // Image EXIF
            try {
                val exif = ExifInterface(inputFile.absolutePath)
                val artist = exif.getAttribute(ExifInterface.TAG_ARTIST)?.takeIf { it.isNotBlank() }
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                val camera = if (!make.isNullOrBlank() || !model.isNullOrBlank()) "${make.orEmpty()} ${model.orEmpty()}".trim() else null
                val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.takeIf { it.isNotBlank() }
                val date = exif.getAttribute(ExifInterface.TAG_DATETIME)?.takeIf { it.isNotBlank() }

                val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                val lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                val gps = if (!lat.isNullOrBlank() && !lon.isNullOrBlank()) "GPS Location Recorded" else null

                var count = 0
                if (artist != null) count++
                if (camera != null) count++
                if (software != null) count++
                if (date != null) count++
                if (gps != null) count++

                return@withContext FileMetadataInfo(
                    fileName = name,
                    isPdf = false,
                    author = artist,
                    cameraDevice = camera,
                    software = software,
                    creationDate = date,
                    gpsCoordinates = gps,
                    totalTagsCount = count
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext FileMetadataInfo(fileName = name, isPdf = false)
        }
    }

    suspend fun stripAllMetadata(
        context: Context,
        inputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val isPdf = inputFile.name.endsWith(".pdf", ignoreCase = true)
        val originalSize = inputFile.length()

        if (isPdf) {
            val outFile = FileUtils.createOutputFile(context, "Anonymized_NoMetadata", "pdf")
            try {
                val doc = PDDocument.load(inputFile)
                val info = doc.documentInformation

                val oldAuthor = info.author ?: "Unknown"

                // Strip all document information
                info.author = null
                info.creator = null
                info.producer = null
                info.title = null
                info.subject = null
                info.keywords = null
                info.creationDate = null
                info.modificationDate = null

                // Strip catalog XMP packet
                doc.documentCatalog.metadata = null

                doc.save(outFile)
                doc.close()

                return@withContext OperationResult(
                    isSuccess = true,
                    outputFile = outFile,
                    originalSizeBytes = originalSize,
                    outputSizeBytes = outFile.length(),
                    message = "Deleted Author ('$oldAuthor'), Creator, Producer, and XMP metadata completely!"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext OperationResult(
                    isSuccess = false,
                    message = "Failed to strip PDF metadata: ${e.localizedMessage}"
                )
            }
        } else {
            // Strip Image EXIF
            val extension = if (inputFile.name.endsWith(".png", true)) "png" else "jpg"
            val outFile = FileUtils.createOutputFile(context, "Sanitized_NoExif", extension)

            try {
                // Decoding bitmap and re-saving natively strips all EXIF, GPS, camera, and author tags
                val bmp = BitmapFactory.decodeFile(inputFile.absolutePath)
                if (bmp != null) {
                    val format = if (extension == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    FileOutputStream(outFile).use { out ->
                        bmp.compress(format, 95, out)
                    }
                    bmp.recycle()

                    // Also explicit ExifInterface wipe on the output
                    try {
                        val exif = ExifInterface(outFile.absolutePath)
                        exif.setAttribute(ExifInterface.TAG_ARTIST, null)
                        exif.setAttribute(ExifInterface.TAG_COPYRIGHT, null)
                        exif.setAttribute(ExifInterface.TAG_MAKE, null)
                        exif.setAttribute(ExifInterface.TAG_MODEL, null)
                        exif.setAttribute(ExifInterface.TAG_SOFTWARE, null)
                        exif.setAttribute(ExifInterface.TAG_DATETIME, null)
                        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
                        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
                        exif.saveAttributes()
                    } catch (_: Exception) {}

                    return@withContext OperationResult(
                        isSuccess = true,
                        outputFile = outFile,
                        originalSizeBytes = originalSize,
                        outputSizeBytes = outFile.length(),
                        message = "Wiped all EXIF tags, Author/Artist, Camera device, and GPS location!"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext OperationResult(false, message = "Could not sanitize image metadata.")
        }
    }
}
