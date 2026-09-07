package com.example.expensetracker.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.presentation.theme.CurrencyUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfTransactionExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val generatedFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun exportToFile(
        rows: List<TransactionExportRow>,
        filterLabel: String,
        outputFile: File
    ) {
        val document = PdfDocument()
        val pageWidth = 842 // A4 landscape @ 72dpi
        val pageHeight = 595
        val margin = 36f
        val headerHeight = 110f
        val rowHeight = 22f
        val rowsPerPage = ((pageHeight - headerHeight - margin - 40f) / rowHeight).toInt().coerceAtLeast(1)

        val columns = listOf(
            ColumnSpec("Date", 88f),
            ColumnSpec("Type", 62f),
            ColumnSpec("Category", 78f),
            ColumnSpec("Amount", 72f),
            ColumnSpec("Account", 58f),
            ColumnSpec("Description", 120f),
            ColumnSpec("Details", 110f)
        )

        val chunks = if (rows.isEmpty()) listOf(emptyList()) else rows.chunked(rowsPerPage)
        chunks.forEachIndexed { pageIndex, pageRows ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawPageHeader(canvas, margin, pageWidth, filterLabel, pageIndex + 1, chunks.size)
            var y = headerHeight
            drawTableHeader(canvas, margin, y, columns)
            y += rowHeight

            pageRows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 0) Color.WHITE else Color.parseColor("#F5F7FA")
                drawRowBackground(canvas, margin, y, pageWidth - margin * 2, rowHeight, bg)
                drawDataRow(canvas, margin, y + 4f, columns, row)
                y += rowHeight
            }

            if (pageRows.isEmpty() && pageIndex == 0) {
                drawEmptyState(canvas, margin, y + 20f)
            }

            document.finishPage(page)
        }

        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
    }

    private fun drawPageHeader(
        canvas: Canvas,
        margin: Float,
        pageWidth: Int,
        filterLabel: String,
        pageNumber: Int,
        totalPages: Int
    ) {
        val icon = loadAppIcon()
        if (icon != null) {
            val iconSize = 48
            val scaled = Bitmap.createScaledBitmap(icon, iconSize, iconSize, true)
            canvas.drawBitmap(scaled, margin, 24f, null)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1565C0")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(context.getString(R.string.app_name), margin + 58f, 48f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#546E7A")
            textSize = 11f
        }
        val periodText = if (filterLabel.isBlank()) "All transactions" else filterLabel
        canvas.drawText(periodText, margin + 58f, 66f, subtitlePaint)
        canvas.drawText(
            "Generated ${generatedFormat.format(java.util.Date())}",
            margin + 58f,
            80f,
            subtitlePaint
        )

        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#78909C")
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $pageNumber of $totalPages", pageWidth - margin, 48f, pagePaint)
    }

    private fun drawTableHeader(canvas: Canvas, margin: Float, y: Float, columns: List<ColumnSpec>) {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerBg = Paint().apply { color = Color.parseColor("#1976D2") }
        val tableWidth = columns.sumOf { it.width.toDouble() }.toFloat()
        canvas.drawRect(margin, y, margin + tableWidth, y + 20f, headerBg)

        var x = margin + 4f
        columns.forEach { column ->
            canvas.drawText(column.title, x, y + 14f, headerPaint)
            x += column.width
        }
    }

    private fun drawRowBackground(
        canvas: Canvas,
        margin: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int
    ) {
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(margin, y, margin + width, y + height, paint)
        val border = Paint().apply {
            this.color = Color.parseColor("#ECEFF1")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(margin, y, margin + width, y + height, border)
    }

    private fun drawDataRow(
        canvas: Canvas,
        margin: Float,
        y: Float,
        columns: List<ColumnSpec>,
        row: TransactionExportRow
    ) {
        val values = listOf(
            dateFormat.format(row.date),
            row.type.name,
            row.categoryName.ifBlank { "—" },
            CurrencyUtils.formatCurrency(row.amount),
            formatAccount(row.account, row.toAccount),
            row.description.ifBlank { "—" },
            row.subDescription.ifBlank { row.debtorNote.ifBlank { "—" } }
        )
        val typeColor = typeColor(row.type)

        var x = margin + 4f
        columns.forEachIndexed { index, column ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                color = when (index) {
                    1 -> typeColor
                    3 -> typeColor
                    else -> Color.parseColor("#37474F")
                }
                if (index == 1 || index == 3) {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
            val text = truncate(values[index], column.width, paint)
            canvas.drawText(text, x, y + 10f, paint)
            x += column.width
        }
    }

    private fun drawEmptyState(canvas: Canvas, margin: Float, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#78909C")
            textSize = 12f
        }
        canvas.drawText("No transactions in this export.", margin, y, paint)
    }

    private fun loadAppIcon(): Bitmap? {
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }.getOrNull()
    }

    private fun formatAccount(account: com.example.expensetracker.data.database.entities.AccountType, to: com.example.expensetracker.data.database.entities.AccountType?): String {
        return if (to != null) "${account.name}→${to.name}" else account.name
    }

    private fun typeColor(type: TransactionType): Int = when (type) {
        TransactionType.INCOME -> Color.parseColor("#2E7D32")
        TransactionType.EXPENSE -> Color.parseColor("#C62828")
        TransactionType.TRANSFER -> Color.parseColor("#EF6C00")
        TransactionType.WALLET_MOVE -> Color.parseColor("#6A1B9A")
    }

    private fun truncate(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth - 8f) return text
        var trimmed = text
        while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth - 8f) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed…"
    }

    private data class ColumnSpec(val title: String, val width: Float)
}
