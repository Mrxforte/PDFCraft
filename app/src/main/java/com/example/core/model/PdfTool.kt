package com.example.core.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class ToolCategory(val label: String) {
    POPULAR("Popular"),
    CONVERT("Convert & Compress"),
    ORGANIZE("Organize & Edit"),
    SECURITY("Sign & Security"),
    AI("AI & OCR")
}

enum class PdfToolType(
    val title: String,
    val description: String,
    val iconColor: Color,
    val category: ToolCategory,
    val isPopular: Boolean = false,
    val badge: String? = null
) {
    MERGE(
        title = "Merge PDF",
        description = "Combine multiple PDFs into one unified document in your preferred order.",
        iconColor = ToolMerge,
        category = ToolCategory.ORGANIZE,
        isPopular = true
    ),
    COMPRESS(
        title = "Compress PDF",
        description = "Reduce file size up to 90% while preserving maximum document quality.",
        iconColor = ToolCompress,
        category = ToolCategory.CONVERT,
        isPopular = true
    ),
    WATERMARK_DELETE(
        title = "Watermark Deleter",
        description = "Erase unwanted watermarks, headers, stamps, and margins from PDF pages.",
        iconColor = ToolWatermarkDelete,
        category = ToolCategory.ORGANIZE,
        isPopular = true,
        badge = "New"
    ),
    OCR(
        title = "PDF OCR",
        description = "Reliable scanner converting document images into editable Word & text.",
        iconColor = ToolOcr,
        category = ToolCategory.AI,
        isPopular = true,
        badge = "PRO"
    ),
    SPLIT(
        title = "Split PDF",
        description = "Extract pages or separate ranges into distinct individual PDF files.",
        iconColor = ToolSplit,
        category = ToolCategory.ORGANIZE,
        isPopular = true
    ),
    ANNOTATE(
        title = "Annotate PDF",
        description = "Add highlights, freehand drawings, notes, shapes, and comments.",
        iconColor = ToolAnnotate,
        category = ToolCategory.ORGANIZE,
        isPopular = true
    ),
    EDIT(
        title = "Edit PDF",
        description = "Add text overlays, shapes, signatures, and images directly onto pages.",
        iconColor = ToolEditText,
        category = ToolCategory.ORGANIZE,
        isPopular = true
    ),
    PDF_TO_WORD(
        title = "PDF to Word",
        description = "Convert PDFs into fully editable Microsoft Word (.docx) documents.",
        iconColor = ToolPdfToWord,
        category = ToolCategory.CONVERT,
        isPopular = true
    ),
    PDF_TO_IMAGE(
        title = "PDF to Image",
        description = "Convert PDF pages into high-resolution JPG or PNG image files.",
        iconColor = ToolPdfToImage,
        category = ToolCategory.CONVERT,
        isPopular = true
    ),
    IMAGE_TO_PDF(
        title = "Image to PDF",
        description = "Convert photos and gallery images into a clean, paginated PDF document.",
        iconColor = ToolPdfToImage,
        category = ToolCategory.CONVERT,
        isPopular = false
    ),
    WATERMARK_ADD(
        title = "Add Watermark",
        description = "Stamp documents with custom text, confidentiality marks, or logos.",
        iconColor = ToolWatermark,
        category = ToolCategory.SECURITY,
        isPopular = false
    ),
    SIGN(
        title = "Sign PDF",
        description = "Create, draw, or type your signature and place it anywhere on pages.",
        iconColor = ToolSign,
        category = ToolCategory.SECURITY,
        isPopular = true
    ),
    SUMMARIZE(
        title = "Summarize PDF",
        description = "Extract key insights, summaries, and executive takeaways from documents.",
        iconColor = ToolSummarize,
        category = ToolCategory.AI,
        isPopular = true,
        badge = "AI"
    ),
    TRANSLATE(
        title = "Translate PDF",
        description = "Extract and translate document text across dozens of global languages.",
        iconColor = ToolTranslate,
        category = ToolCategory.AI,
        isPopular = true,
        badge = "AI"
    ),
    REQUEST_SIGNATURES(
        title = "Request Signatures",
        description = "Prepare documents and request e-signatures from recipients securely.",
        iconColor = ToolRequestSignatures,
        category = ToolCategory.SECURITY,
        isPopular = true
    ),
    PROTECT(
        title = "Protect PDF",
        description = "Encrypt documents with standard password protection for enhanced privacy.",
        iconColor = ToolProtect,
        category = ToolCategory.SECURITY,
        isPopular = false
    )
}
