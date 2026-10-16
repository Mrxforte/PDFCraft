package com.example.core.pdf

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxGenerator {

    /**
     * Generates a genuine, standard OpenXML .docx Microsoft Word document
     * containing the paragraphs extracted from OCR or PDF text stripping.
     */
    fun createDocxFile(outputFile: File, paragraphs: List<String>, title: String = "Smallpdf Document") {
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
            zip.write(contentTypes.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
            zip.write(rels.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3. word/document.xml
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val docXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
                append("<w:body>")

                // Title header
                append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"36\"/><w:color w:val=\"0B57F9\"/></w:rPr><w:t>${escapeXml(title)}</w:t></w:r></w:p>")
                append("<w:p><w:r><w:rPr><w:sz w:val=\"16\"/><w:color w:val=\"5B6472\"/></w:rPr><w:t>Converted by Smallpdf Android</w:t></w:r></w:p>")
                append("<w:p/>")

                // Paragraphs
                for (p in paragraphs) {
                    val trimmed = p.trim()
                    if (trimmed.isEmpty()) {
                        append("<w:p/>")
                    } else {
                        append("<w:p><w:r><w:rPr><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">${escapeXml(trimmed)}</w:t></w:r></w:p>")
                    }
                }

                append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/></w:sectPr>")
                append("</w:body></w:document>")
            }
            zip.write(docXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
