package com.shoptimemaster.app.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object ReportExporter {
    fun exportCsv(context: Context, filename: String, rows: List<List<String>>, excelBom: Boolean): File {
        val file = File(context.cacheDir, filename)
        val body = rows.joinToString("\r\n") { row ->
            row.joinToString(",") { cell ->
                if (cell.contains(",") || cell.contains("\"")) "\"${cell.replace("\"", "\"\"")}\"" else cell
            }
        }
        val content = if (excelBom) "\uFEFF$body" else body
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun exportPdfReport(context: Context, filename: String, title: String, lines: List<String>): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }

        canvas.drawText(title, 40f, 52f, titlePaint)
        var y = 84f
        for (line in lines) {
            if (y > 800f) break
            canvas.drawText(line, 40f, y, bodyPaint)
            y += 22f
        }
        doc.finishPage(page)

        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
