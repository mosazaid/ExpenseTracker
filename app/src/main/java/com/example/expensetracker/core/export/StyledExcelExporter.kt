package com.example.expensetracker.core.export

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.data.database.entities.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Styled spreadsheet export as HTML — opens in Excel, Sheets, or a browser with rich executive colors and branding.
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

        val totalIncome = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        val sb = StringBuilder()
        sb.append(
            """
            <!DOCTYPE html>
            <html xmlns:o="urn:schemas-microsoft-com:office:office"
                  xmlns:x="urn:schemas-microsoft-com:office:excel">
            <head>
              <meta charset="UTF-8"/>
              <title>${escapeHtml(context.getString(R.string.app_name))} Statement</title>
              <style>
                * { box-sizing: border-box; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                  background-color: #F8FAFC;
                  color: #0F172A;
                  margin: 0;
                  padding: 32px 24px;
                }
                .container {
                  max-width: 1100px;
                  margin: 0 auto;
                  background: #FFFFFF;
                  border-radius: 12px;
                  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
                  padding: 32px;
                  border: 1px solid #E2E8F0;
                }
                .header {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  border-bottom: 2px solid #F1F5F9;
                  padding-bottom: 24px;
                  margin-bottom: 24px;
                }
                .brand {
                  display: flex;
                  align-items: center;
                  gap: 16px;
                }
                .brand img {
                  border-radius: 10px;
                }
                .title {
                  font-size: 24px;
                  font-weight: 800;
                  color: #0F172A;
                  margin: 0;
                  letter-spacing: -0.5px;
                }
                .subtitle {
                  font-size: 13px;
                  color: #64748B;
                  margin: 4px 0 0 0;
                }
                .meta-right {
                  text-align: right;
                  font-size: 12px;
                  color: #94A3B8;
                }
                .kpi-grid {
                  display: grid;
                  grid-template-columns: repeat(4, 1fr);
                  gap: 16px;
                  margin-bottom: 28px;
                }
                .kpi-card {
                  border-radius: 8px;
                  padding: 16px;
                  border: 1px solid transparent;
                }
                .kpi-card.income { background: #F0FDF4; border-color: #BBF7D0; }
                .kpi-card.expense { background: #FEF2F2; border-color: #FECACA; }
                .kpi-card.balance { background: #EFF6FF; border-color: #BFDBFE; }
                .kpi-card.count { background: #F8FAFC; border-color: #E2E8F0; }
                .kpi-label {
                  font-size: 11px;
                  font-weight: 700;
                  text-transform: uppercase;
                  letter-spacing: 0.5px;
                  margin-bottom: 6px;
                }
                .kpi-card.income .kpi-label { color: #166534; }
                .kpi-card.expense .kpi-label { color: #991B1B; }
                .kpi-card.balance .kpi-label { color: #1E40AF; }
                .kpi-card.count .kpi-label { color: #475569; }
                .kpi-value {
                  font-size: 20px;
                  font-weight: 800;
                  margin: 0;
                  font-variant-numeric: tabular-nums;
                }
                .kpi-card.income .kpi-value { color: #15803D; }
                .kpi-card.expense .kpi-value { color: #B91C1C; }
                .kpi-card.balance .kpi-value { color: #1D4ED8; }
                .kpi-card.count .kpi-value { color: #334155; }
                table {
                  border-collapse: separate;
                  border-spacing: 0;
                  width: 100%;
                  font-size: 13px;
                  border-radius: 8px;
                  overflow: hidden;
                  border: 1px solid #E2E8F0;
                }
                thead th {
                  background: #1E293B;
                  color: #F8FAFC;
                  font-weight: 600;
                  padding: 12px 14px;
                  text-align: left;
                  letter-spacing: 0.2px;
                  border-bottom: 1px solid #0F172A;
                }
                tbody td {
                  padding: 12px 14px;
                  border-bottom: 1px solid #F1F5F9;
                  color: #334155;
                  vertical-align: middle;
                }
                tbody tr:last-child td { border-bottom: none; }
                tbody tr:nth-child(even) td { background: #F8FAFC; }
                tbody tr:hover td { background: #F1F5F9; }
                tfoot td {
                  padding: 12px 14px;
                  font-weight: 700;
                  background: #F1F5F9;
                  border-top: 2px solid #CBD5E1;
                  color: #0F172A;
                }
                .badge {
                  display: inline-block;
                  padding: 3px 8px;
                  border-radius: 9999px;
                  font-size: 11px;
                  font-weight: 700;
                  text-transform: capitalize;
                }
                .badge.income { background: #DCFCE7; color: #15803D; }
                .badge.expense { background: #FEE2E2; color: #B91C1C; }
                .badge.transfer { background: #FFEDD5; color: #C2410C; }
                .badge.wallet { background: #F3E8FF; color: #7E22CE; }
                .amount-col {
                  font-weight: 700;
                  font-variant-numeric: tabular-nums;
                }
                .amount-col.income { color: #15803D; }
                .amount-col.expense { color: #B91C1C; }
                .amount-col.transfer { color: #C2410C; }
                .amount-col.wallet { color: #7E22CE; }
                .empty-cell {
                  text-align: center;
                  padding: 40px !important;
                  color: #94A3B8;
                  font-size: 14px;
                }
              </style>
            </head>
            <body>
            <div class="container">
            """.trimIndent()
        )

        sb.append("<div class=\"header\">")
        sb.append("<div class=\"brand\">")
        if (iconBase64 != null) {
            sb.append("<img src=\"data:image/png;base64,$iconBase64\" width=\"44\" height=\"44\" alt=\"icon\"/>")
        }
        sb.append("<div>")
        sb.append("<p class=\"title\">${escapeHtml(context.getString(R.string.app_name))}</p>")
        sb.append("<p class=\"subtitle\">${escapeHtml(periodText)}</p>")
        sb.append("</div></div>")
        sb.append("<div class=\"meta-right\">")
        sb.append("<div>Exported Statement</div>")
        sb.append("<div>Generated: ${escapeHtml(generatedFormat.format(Date()))}</div>")
        sb.append("</div></div>")

        // ── Executive KPI Cards
        sb.append(
            """
            <div class="kpi-grid">
              <div class="kpi-card income">
                <div class="kpi-label">Total Income</div>
                <div class="kpi-value">${escapeHtml(CurrencyUtils.formatCurrency(totalIncome))}</div>
              </div>
              <div class="kpi-card expense">
                <div class="kpi-label">Total Expense</div>
                <div class="kpi-value">${escapeHtml(CurrencyUtils.formatCurrency(totalExpense))}</div>
              </div>
              <div class="kpi-card balance">
                <div class="kpi-label">Net Balance</div>
                <div class="kpi-value">${escapeHtml(CurrencyUtils.formatCurrency(netBalance))}</div>
              </div>
              <div class="kpi-card count">
                <div class="kpi-label">Transactions</div>
                <div class="kpi-value">${rows.size}</div>
              </div>
            </div>
            """.trimIndent()
        )

        // ── Table
        sb.append("<table><thead><tr>")
        listOf("Date & Time", "Type", "Category", "Amount", "Account", "Description", "Details").forEach {
            sb.append("<th>${escapeHtml(it)}</th>")
        }
        sb.append("</tr></thead><tbody>")

        if (rows.isEmpty()) {
            sb.append("<tr><td colspan=\"7\" class=\"empty-cell\">No transactions found in this period.</td></tr>")
        } else {
            rows.forEach { row ->
                val css = typeCss(row.type)
                val details = row.subDescription.ifBlank { row.debtorNote.ifBlank { "—" } }
                sb.append("<tr>")
                sb.append("<td>${escapeHtml(dateFormat.format(row.date))}</td>")
                sb.append("<td><span class=\"badge $css\">${escapeHtml(row.type.name)}</span></td>")
                sb.append("<td><strong>${escapeHtml(row.categoryName.ifBlank { "—" })}</strong></td>")
                sb.append("<td class=\"amount-col $css\">${escapeHtml(CurrencyUtils.formatCurrency(row.amount))}</td>")
                sb.append("<td>${escapeHtml(formatAccount(row))}</td>")
                sb.append("<td>${escapeHtml(row.description.ifBlank { "—" })}</td>")
                sb.append("<td>${escapeHtml(details)}</td>")
                sb.append("</tr>")
            }
        }

        sb.append("</tbody>")
        if (rows.isNotEmpty()) {
            sb.append("<tfoot><tr>")
            sb.append("<td colspan=\"3\">Summary Total (Income vs Expense)</td>")
            sb.append("<td class=\"amount-col\">Net: ${escapeHtml(CurrencyUtils.formatCurrency(netBalance))}</td>")
            sb.append("<td colspan=\"3\">${rows.size} Total Transactions</td>")
            sb.append("</tr></tfoot>")
        }
        sb.append("</table>")
        sb.append("</div></body></html>")
        return sb.toString()
    }

    private fun formatAccount(row: TransactionExportRow): String {
        return if (row.toAccount != null) {
            "${row.account.name} → ${row.toAccount.name}"
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
