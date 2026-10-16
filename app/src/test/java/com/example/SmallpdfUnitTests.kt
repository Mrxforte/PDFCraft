package com.example

import com.example.core.database.HistoryEntity
import com.example.core.model.CompressionLevel
import com.example.core.model.OperationResult
import com.example.core.model.PdfToolType
import com.example.core.pdf.PdfSplitEngine
import org.junit.Assert.*
import org.junit.Test

class SmallpdfUnitTests {

    @Test
    fun testPageRangeParsing_singleAndRanges() {
        val parsed = PdfSplitEngine.parsePageRanges("1-3, 5, 7-8", maxPages = 10)
        assertEquals(3, parsed.size)
        assertEquals(listOf(0, 1, 2), parsed[0])
        assertEquals(listOf(4), parsed[1])
        assertEquals(listOf(6, 7), parsed[2])
    }

    @Test
    fun testPageRangeParsing_outOfBoundsHandled() {
        val parsed = PdfSplitEngine.parsePageRanges("1-20", maxPages = 4)
        assertEquals(1, parsed.size)
        assertEquals(listOf(0, 1, 2, 3), parsed[0])
    }

    @Test
    fun testPercentageSavedCalculation() {
        val result = OperationResult(
            isSuccess = true,
            originalSizeBytes = 10_000_000L,
            outputSizeBytes = 3_000_000L
        )
        assertEquals(70, result.percentageSaved)
    }

    @Test
    fun testHistoryEntityFormattedSize() {
        val itemMb = HistoryEntity(
            title = "sample.pdf",
            toolType = "Compress PDF",
            originalSizeBytes = 5_242_880L,
            resultSizeBytes = 2_097_152L,
            filePath = "/path/sample.pdf",
            fileUri = "content://path/sample.pdf"
        )
        assertEquals("2.00 MB", itemMb.formattedSize)

        val itemKb = HistoryEntity(
            title = "small.pdf",
            toolType = "Merge PDF",
            originalSizeBytes = 50_000L,
            resultSizeBytes = 51_200L,
            filePath = "/path/small.pdf",
            fileUri = "content://path/small.pdf"
        )
        assertEquals("50.0 KB", itemKb.formattedSize)
    }

    @Test
    fun testToolRegistryPopularItems() {
        val popular = PdfToolType.values().filter { it.isPopular }
        assertTrue(popular.contains(PdfToolType.MERGE))
        assertTrue(popular.contains(PdfToolType.COMPRESS))
        assertTrue(popular.contains(PdfToolType.WATERMARK_DELETE))
        assertTrue(popular.contains(PdfToolType.DELETE_METADATA))
        assertTrue(popular.contains(PdfToolType.OCR))
        assertTrue(popular.contains(PdfToolType.SPLIT))
    }

    @Test
    fun testPdfSearchResultModel() {
        val pageMatch = com.example.core.pdf.PdfPageMatch(
            pageIndex = 0,
            snippets = listOf("...Invoice total is $150..."),
            count = 1
        )
        val result = com.example.core.pdf.PdfSearchResult(
            query = "Invoice",
            matches = listOf(pageMatch),
            totalOccurrences = 1
        )
        assertEquals("Invoice", result.query)
        assertEquals(1, result.totalOccurrences)
        assertEquals(0, result.matches[0].pageIndex)
    }

    @Test
    fun testPdfBookmarkModel() {
        val bm = com.example.core.pdf.PdfBookmark(title = "Chapter 1: Overview", pageIndex = 3)
        assertEquals("Chapter 1: Overview", bm.title)
        assertEquals(3, bm.pageIndex)
    }
}
