package com.example.expensetracker.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
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
        val rowHeight = 22f

        // 770f total width fits exactly in 842 - (36 * 2) = 770
        val columns = listOf(
            ColumnSpec("Date & Time", 96f),
            ColumnSpec("Type", 66f),
            ColumnSpec("Category", 95f),
            ColumnSpec("Amount", 80f),
            ColumnSpec("Account", 65f),
            ColumnSpec("Description", 170f),
            ColumnSpec("Details", 198f)
        )

        val totalIncome = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        val pages = mutableListOf<List<TransactionExportRow>>()
        if (rows.isEmpty()) {
            pages.add(emptyList())
        } else {
            val firstPageRows = rows.take(17)
            pages.add(firstPageRows)
            var remaining = rows.drop(17)
            while (remaining.isNotEmpty()) {
                pages.add(remaining.take(20))
                remaining = remaining.drop(20)
            }
        }

        pages.forEachIndexed { pageIndex, pageRows ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val isFirstPage = pageIndex == 0
            drawPageHeader(canvas, margin, pageWidth, filterLabel, pageIndex + 1, pages.size)

            var y: Float
            if (isFirstPage) {
                drawKpiSummaryCards(canvas, margin, pageWidth - margin * 2, totalIncome, totalExpense, netBalance, rows.size)
                y = 135f
            } else {
                y = 80f
            }

            drawTableHeader(canvas, margin, y, columns)
            y += 20f

            pageRows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
                drawRowBackground(canvas, margin, y, pageWidth - margin * 2, rowHeight, bg)
                drawDataRow(canvas, margin, y + 3f, columns, row, rowHeight)
                y += rowHeight
            }

            if (pageRows.isEmpty() && isFirstPage) {
                drawEmptyState(canvas, margin, y + 24f)
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
            val iconSize = 40
            val scaled = Bitmap.createScaledBitmap(icon, iconSize, iconSize, true)
            canvas.drawBitmap(scaled, margin, 18f, null)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val leftX = if (icon != null) margin + 48f else margin
        canvas.drawText(context.getString(R.string.app_name), leftX, 34f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
        }
        val periodText = if (filterLabel.isBlank()) "All Transactions" else filterLabel
        canvas.drawText("$periodText  •  Generated: ${generatedFormat.format(Date())}", leftX, 48f, subtitlePaint)

        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $pageNumber of $totalPages", pageWidth - margin, 34f, pagePaint)
    }

    private fun drawKpiSummaryCards(
        canvas: Canvas,
        margin: Float,
        availableWidth: Float,
        income: Double,
        expense: Double,
        net: Double,
        count: Int
    ) {
        val cardY = 64f
        val cardHeight = 54f
        val gap = 12f
        val cardWidth = (availableWidth - (gap * 3)) / 4f

        val cards = listOf(
            KpiCardSpec("Total Income", CurrencyUtils.formatCurrency(income), "#DCFCE7", "#15803D", "#166534"),
            KpiCardSpec("Total Expense", CurrencyUtils.formatCurrency(expense), "#FEE2E2", "#B91C1C", "#991B1B"),
            KpiCardSpec("Net Balance", CurrencyUtils.formatCurrency(net), "#DBEAFE", "#1D4ED8", "#1E40AF"),
            KpiCardSpec("Transactions", "$count recorded", "#F1F5F9", "#475569", "#334155")
        )

        cards.forEachIndexed { i, card ->
            val cardX = margin + i * (cardWidth + gap)
            val rect = RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(card.bgColor)
            }
            canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(card.labelColor)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(card.label.uppercase(), cardX + 10f, cardY + 18f, labelPaint)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(card.valColor)
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(card.value, cardX + 10f, cardY + 38f, valPaint)
        }
    }

    private fun drawTableHeader(canvas: Canvas, margin: Float, y: Float, columns: List<ColumnSpec>) {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerBg = Paint().apply { color = Color.parseColor("#1E293B") }
        val tableWidth = columns.sumOf { it.width.toDouble() }.toFloat()

        val headerRect = RectF(margin, y, margin + tableWidth, y + 20f)
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBg)

        var x = margin + 6f
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
            this.color = Color.parseColor("#F1F5F9")
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
        row: TransactionExportRow,
        rowHeight: Float
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

        var x = margin + 6f
        columns.forEachIndexed { index, column ->
            if (index == 1) {
                // Type badge pill
                val pillWidth = (column.width - 12f).coerceAtMost(56f)
                val pillRect = RectF(x, y, x + pillWidth, y + rowHeight - 6f)
                val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = typeBgColor(row.type) }
                canvas.drawRoundRect(pillRect, 4f, 4f, pillBg)

                val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 8f
                    color = typeColor
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val typeName = row.type.name.take(7)
                canvas.drawText(typeName, x + 4f, y + 10.5f, pillTextPaint)
            } else {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 8.5f
                    color = when (index) {
                        3 -> typeColor
                        else -> Color.parseColor("#334155")
                    }
                    if (index == 3) {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                }
                val text = truncate(values[index], column.width - 8f, paint)
                canvas.drawText(text, x, y + 10f, paint)
            }
            x += column.width
        }
    }

    private fun drawEmptyState(canvas: Canvas, margin: Float, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 12f
        }
        canvas.drawText("No transactions found in this period.", margin, y, paint)
    }

    private fun loadAppIcon(): Bitmap? {
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }.getOrNull()
    }

    private fun formatAccount(account: AccountType, to: AccountType?): String {
        return if (to != null) "${account.name} → ${to.name}" else account.name
    }

    private fun typeColor(type: TransactionType): Int = when (type) {
        TransactionType.INCOME -> Color.parseColor("#15803D")
        TransactionType.EXPENSE -> Color.parseColor("#B91C1C")
        TransactionType.TRANSFER -> Color.parseColor("#C2410C")
        TransactionType.WALLET_MOVE -> Color.parseColor("#7E22CE")
    }

    private fun typeBgColor(type: TransactionType): Int = when (type) {
        TransactionType.INCOME -> Color.parseColor("#DCFCE7")
        TransactionType.EXPENSE -> Color.parseColor("#FEE2E2")
        TransactionType.TRANSFER -> Color.parseColor("#FFEDD5")
        TransactionType.WALLET_MOVE -> Color.parseColor("#F3E8FF")
    }

    private fun truncate(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed…"
    }

    private data class ColumnSpec(val title: String, val width: Float)
    private data class KpiCardSpec(val label: String, val value: String, val bgColor: String, val labelColor: String, val valColor: String)
}
