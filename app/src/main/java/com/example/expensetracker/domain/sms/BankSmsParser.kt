package com.example.expensetracker.domain.sms

import com.example.expensetracker.data.database.entities.TransactionType
import java.util.Date
import java.util.regex.Pattern

data class ParsedBankSms(
    val rawText: String,
    val bankName: String? = null,
    val amount: Double,
    val currency: String = "JOD",
    val merchant: String = "",
    val cardOrAccountEnding: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val suggestedCategoryName: String? = null,
    val date: Date = Date()
)

object BankSmsParser {

    private val BANK_IDENTIFIERS = mapOf(
        "Arab Bank" to listOf("arab bank", "البنك العربي", "arabbank"),
        "Bank al Etihad" to listOf("etihad", "بنك الاتحاد", "bank al etihad"),
        "Jordan Kuwait Bank" to listOf("jkb", "الأردني الكويتي", "jordan kuwait"),
        "Cairo Amman Bank" to listOf("cab", "قاهرة عمان", "cairo amman"),
        "Capital Bank" to listOf("capital bank", "كابيتال بنك"),
        "Housing Bank" to listOf("housing bank", "بنك الإسكان", "hbtf"),
        "Islamic International Arab Bank" to listOf("iiabank", "العربي الإسلامي"),
        "Jordan Islamic Bank" to listOf("jib", "الإسلامي الأردني"),
        "Al Rajhi Bank" to listOf("alrajhi", "الراجحي", "rajhi"),
        "Emirates NBD" to listOf("enbd", "emirates nbd", "الإمارات دبي الوطني"),
        "ADCB" to listOf("adcb", "أبوظبي التجاري"),
        "FAB" to listOf("fab", "أبوظبي الأول")
    )

    fun parse(smsText: String, sender: String = ""): ParsedBankSms? {
        val trimmed = smsText.trim()
        if (trimmed.isBlank()) return null

        val detectedBank = detectBank(trimmed, sender)
        val (amount, currency) = extractAmountAndCurrency(trimmed) ?: return null
        val merchant = extractMerchant(trimmed)
        val cardEnding = extractCardOrAccount(trimmed)
        val type = detectTransactionType(trimmed)
        val suggestedCategory = guessCategory(merchant, trimmed)

        return ParsedBankSms(
            rawText = trimmed,
            bankName = detectedBank,
            amount = amount,
            currency = currency,
            merchant = merchant,
            cardOrAccountEnding = cardEnding,
            transactionType = type,
            suggestedCategoryName = suggestedCategory
        )
    }

    private fun detectBank(text: String, sender: String): String? {
        val combined = "$sender $text".lowercase()
        for ((bank, keywords) in BANK_IDENTIFIERS) {
            if (keywords.any { combined.contains(it) }) return bank
        }
        return sender.ifBlank { null }
    }

    private fun extractAmountAndCurrency(text: String): Pair<Double, String>? {
        val regexPatterns = listOf(
            // Currency code followed by amount: JOD 25.50, USD 100.00
            Pattern.compile("""(?i)\b(JOD|USD|AED|SAR|EUR|GBP|KWD|QAR|BHD)\s*[:=]?\s*([0-9]+(?:[\.,][0-9]{1,3})?)"""),
            // Amount followed by currency code: 25.50 JOD, 100.00 USD
            Pattern.compile("""(?i)([0-9]+(?:[\.,][0-9]{1,3})?)\s*(JOD|USD|AED|SAR|EUR|GBP|KWD|QAR|BHD)\b"""),
            // Arabic currency: د.أ, دينار, ريال, درهم
            Pattern.compile("""(?:بمبلغ|بقيمة|مبلغ|قيمة|بـ)?\s*([0-9]+(?:[\.,][0-9]{1,3})?)\s*(د\.أ|دينار|ريال|درهم|دولار)"""),
            Pattern.compile("""(د\.أ|دينار|ريال|درهم|دولار)\s*([0-9]+(?:[\.,][0-9]{1,3})?)"""),
            // Fallback generic number after purchase/خصم keywords
            Pattern.compile("""(?i)(?:purchase of|payment of|debited by|amount of|خصم بمبلغ|بقيمة|مبلغ)\s*([0-9]+(?:[\.,][0-9]{1,3})?)""")
        )

        for (pattern in regexPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val g1 = matcher.group(1)?.trim() ?: ""
                val g2 = if (matcher.groupCount() >= 2) matcher.group(2)?.trim() ?: "" else ""

                val (rawNum, rawCurr) = if (g1.matches(Regex("""[0-9]+(?:[\.,][0-9]+)?"""))) {
                    Pair(g1, g2)
                } else {
                    Pair(g2, g1)
                }

                val amount = rawNum.replace(",", "").toDoubleOrNull() ?: continue
                if (amount <= 0.0) continue

                val normalizedCurrency = normalizeCurrency(rawCurr)
                return Pair(amount, normalizedCurrency)
            }
        }

        return null
    }

    private fun normalizeCurrency(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("jod") || lower.contains("د.أ") || lower.contains("دينار") -> "JOD"
            lower.contains("usd") || lower.contains("دولار") || lower.contains("$") -> "USD"
            lower.contains("aed") || lower.contains("درهم") -> "AED"
            lower.contains("sar") || lower.contains("ريال") -> "SAR"
            lower.contains("eur") || lower.contains("يورو") || lower.contains("€") -> "EUR"
            lower.contains("gbp") || lower.contains("جنيه") || lower.contains("£") -> "GBP"
            else -> "JOD"
        }
    }

    private fun extractMerchant(text: String): String {
        val patterns = listOf(
            Pattern.compile("""(?i)(?:at|to|for payment to)\s+([A-Za-z0-9&'\s\-]{2,30}?)(?:\s+(?:with|on|using|date|ref|card|\.|$))"""),
            Pattern.compile("""(?:لدى|في|من متجر|من محل)\s+([^,\.\n\r]{2,30}?)(?:\s+(?:من|بواسطة|عبر|بتاريخ|بطاقة|حساب|في|$))""")
        )

        for (p in patterns) {
            val m = p.matcher(text)
            if (m.find()) {
                val candidate = m.group(1)?.trim()?.replace(Regex("""^[,\.\-:]+|[,\.\-:]+$"""), "") ?: ""
                if (candidate.isNotBlank() && candidate.length > 1) {
                    return candidate
                }
            }
        }

        return "Card Purchase"
    }

    private fun extractCardOrAccount(text: String): String? {
        val pattern = Pattern.compile("""(?i)(?:ending|card|بطاقة|account|حساب|بـ)?\s*[*xX]*([0-9]{4})\b""")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun detectTransactionType(text: String): TransactionType {
        val lower = text.lowercase()
        if (lower.contains("salary") || lower.contains("deposit") || lower.contains("credited") ||
            lower.contains("إيداع") || lower.contains("راتب") || lower.contains("تحويل وارد")
        ) {
            return TransactionType.INCOME
        }
        return TransactionType.EXPENSE
    }

    private fun guessCategory(merchant: String, rawText: String): String {
        val combined = "$merchant $rawText".lowercase()
        return when {
            combined.containsAny("carrefour", "cozmo", "safeer", "hypermax", "lulu", "tamimi", "panda", "supermarket", "grocery", "خضار", "بقالة", "كارفور", "سامح", "دكان", "بندة", "بنده", "عثيم", "تميمي", "ماركت", "هايبر") ->
                "Groceries & Shopping"
            combined.containsAny("starbucks", "costa", "dunkin", "coffee", "cafe", "mcdonald", "kfc", "burger", "shawarma", "restaurant", "مطعم", "كافيه", "قهوة", "شاورما", "برجر") ->
                "Restaurants & Cafés"
            combined.containsAny("total", "shell", "manaseer", "jopetrol", "aramco", "adnoc", "enoc", "gas", "fuel", "uber", "careem", "taxi", "بترول", "محطة", "بنزين", "وقود") ->
                "Car & Transportation"
            combined.containsAny("zain", "orange", "umniah", "stc", "etisalat", "du", "ooredoo", "electricity", "water", "كهرباء", "مياه", "فواتير", "اتصالات") ->
                "Bills & Utilities"
            combined.containsAny("pharmacy", "hospital", "clinic", "صيدلية", "مستشفى", "عيادة", "دواء") ->
                "Healthcare"
            combined.containsAny("zara", "h&m", "amazon", "noon", "centerpoint", "ikea", "shein", "shopping", "ملابس", "تسوق") ->
                "Groceries & Shopping"
            else -> "Other"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
