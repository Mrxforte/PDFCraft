package com.example.core.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.core.model.PageThumbnail
import java.io.File

object PdfRendererHelper {

    fun getPageCount(file: File): Int {
        var count = 0
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    count = renderer.pageCount
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    fun renderPageToBitmap(
        file: File,
        pageIndex: Int,
        scale: Float = 1.0f,
        rotationDegrees: Int = 0
    ): Bitmap? {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                    renderer.openPage(pageIndex).use { page ->
                        val targetWidth = (page.width * scale).toInt().coerceAtLeast(100)
                        val targetHeight = (page.height * scale).toInt().coerceAtLeast(100)
                        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        if (rotationDegrees % 360 != 0) {
                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        } else {
                            bitmap
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renderThumbnails(
        file: File,
        maxPages: Int = 50,
        thumbWidth: Int = 300
    ): List<PageThumbnail> {
        val list = mutableListOf<PageThumbnail>()
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount.coerceAtMost(maxPages)
                    for (i in 0 until total) {
                        renderer.openPage(i).use { page ->
                            val scale = thumbWidth.toFloat() / page.width.toFloat()
                            val h = (page.height * scale).toInt().coerceAtLeast(100)
                            val bitmap = Bitmap.createBitmap(thumbWidth, h, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            list.add(PageThumbnail(i, bitmap, page.width, page.height))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
