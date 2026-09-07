package com.example.expensetracker.domain

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.presentation.theme.CurrencyUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Styled spreadsheet export as HTML — opens in Excel, Sheets, or a browser with colors and branding.
 * Saved as .html because legacy HTML-as-.xls is blocked by modern Microsoft Office.
 */
@Singleton
class StyledExcelExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val generatedFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun exportToFile(
        rows: List<TransactionExportRow>,
        filterLabel: String,
        outputFile: File
    ) {
        outputFile.writeText(buildHtml(rows, filterLabel), Charsets.UTF_8)
    }

    private fun buildHtml(rows: List<TransactionExportRow>, filterLabel: String): String {
        val iconBase64 = loadIconBase64()
        val periodText = filterLabel.ifBlank { "All transactions" }
        val sb = StringBuilder()
        sb.append(
            """
            <!DOCTYPE html>
            <html xmlns:o="urn:schemas-microsoft-com:office:office"
                  xmlns:x="urn:schemas-microsoft-com:office:excel">
            <head>
              <meta charset="UTF-8"/>
              <style>
                body { font-family: Arial, sans-serif; margin: 24px; color: #37474F; }
                .header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
                .title { font-size: 24px; font-weight: bold; color: #1565C0; margin: 0; }
                .subtitle { font-size: 12px; color: #546E7A; margin: 4px 0 0 0; }
                table { border-collapse: collapse; width: 100%; }
                th {
                  background: #1976D2; color: #FFFFFF; font-weight: bold;
                  padding: 10px 8px; text-align: left; border: 1px solid #1565C0;
                }
                td {
                  padding: 8px; border: 1px solid #ECEFF1; font-size: 12px;
                }
                tr:nth-child(even) td { background: #F5F7FA; }
                .income { color: #2E7D32; font-weight: bold; }
                .expense { color: #C62828; font-weight: bold; }
                .transfer { color: #EF6C00; font-weight: bold; }
                .wallet { color: #6A1B9A; font-weight: bold; }
              </style>
            </head>
            <body>
            """.trimIndent()
        )

        sb.append("<div class=\"header\">")
        if (iconBase64 != null) {
            sb.append("<img src=\"data:image/png;base64,$iconBase64\" width=\"48\" height=\"48\" alt=\"icon\"/>")
        }
        sb.append("<div>")
        sb.append("<p class=\"title\">${escapeHtml(context.getString(R.string.app_name))}</p>")
        sb.append("<p class=\"subtitle\">${escapeHtml(periodText)}</p>")
        sb.append("<p class=\"subtitle\">Generated ${escapeHtml(generatedFormat.format(java.util.Date()))}</p>")
        sb.append("</div></div>")

        sb.append("<table><thead><tr>")
        listOf("Date", "Type", "Category", "Amount", "Account", "Description", "Sub-description").forEach {
            sb.append("<th>${escapeHtml(it)}</th>")
        }
        sb.append("</tr></thead><tbody>")

        if (rows.isEmpty()) {
            sb.append("<tr><td colspan=\"7\">No transactions in this export.</td></tr>")
        } else {
            rows.forEach { row ->
                val css = typeCss(row.type)
                sb.append("<tr>")
                sb.append("<td>${escapeHtml(dateFormat.format(row.date))}</td>")
                sb.append("<td class=\"$css\">${escapeHtml(row.type.name)}</td>")
                sb.append("<td>${escapeHtml(row.categoryName.ifBlank { "—" })}</td>")
                sb.append("<td class=\"$css\">${escapeHtml(CurrencyUtils.formatCurrency(row.amount))}</td>")
                sb.append("<td>${escapeHtml(formatAccount(row))}</td>")
                sb.append("<td>${escapeHtml(row.description.ifBlank { "—" })}</td>")
                sb.append("<td>${escapeHtml(row.subDescription.ifBlank { "—" })}</td>")
                sb.append("</tr>")
            }
        }

        sb.append("</tbody></table></body></html>")
        return sb.toString()
    }

    private fun formatAccount(row: TransactionExportRow): String {
        return if (row.toAccount != null) {
            "${row.account.name}→${row.toAccount.name}"
        } else {
            row.account.name
        }
    }

    private fun typeCss(type: TransactionType): String = when (type) {
        TransactionType.INCOME -> "income"
        TransactionType.EXPENSE -> "expense"
        TransactionType.TRANSFER -> "transfer"
        TransactionType.WALLET_MOVE -> "wallet"
    }

    private fun loadIconBase64(): String? {
        return runCatching {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
                ?: return null
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }
        }.getOrNull()
    }

    private fun escapeHtml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
