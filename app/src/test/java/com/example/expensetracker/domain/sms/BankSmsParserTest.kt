package com.example.expensetracker.domain.sms

import com.example.expensetracker.data.database.entities.TransactionType
import org.junit.Assert.*
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun parse_arabBankEnglishSms() {
        val sms = "Purchase of JOD 25.50 with card ending 1234 at Carrefour on 2026-10-15"
        val result = BankSmsParser.parse(sms, sender = "ArabBank")

        assertNotNull(result)
        assertEquals("Arab Bank", result!!.bankName)
        assertEquals(25.50, result.amount, 0.001)
        assertEquals("JOD", result.currency)
        assertEquals("Carrefour", result.merchant)
        assertEquals("1234", result.cardOrAccountEnding)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals("Groceries & Shopping", result.suggestedCategoryName)
    }

    @Test
    fun parse_bankAlEtihadArabicSms() {
        val sms = "حركة شراء بقيمة 15.75 د.أ لدى Cozmo بطاقة 5678"
        val result = BankSmsParser.parse(sms, sender = "Etihad")

        assertNotNull(result)
        assertEquals("Bank al Etihad", result!!.bankName)
        assertEquals(15.75, result.amount, 0.001)
        assertEquals("JOD", result.currency)
        assertEquals("Cozmo", result.merchant)
        assertEquals("5678", result.cardOrAccountEnding)
        assertEquals("Groceries & Shopping", result.suggestedCategoryName)
    }

    @Test
    fun parse_alRajhiSaudiSms() {
        val sms = "عملية شراء عبر مدى بمبلغ 150.00 ريال لدى متجر بندة من حساب 9876"
        val result = BankSmsParser.parse(sms, sender = "AlRajhi")

        assertNotNull(result)
        assertEquals("Al Rajhi Bank", result!!.bankName)
        assertEquals(150.00, result.amount, 0.001)
        assertEquals("SAR", result.currency)
        assertEquals("Groceries & Shopping", result.suggestedCategoryName)
    }

    @Test
    fun parse_uaeCoffeePurchase() {
        val sms = "Purchase of AED 85.00 with Card ending 4321 at Starbucks on 12/11/2026"
        val result = BankSmsParser.parse(sms, sender = "ENBD")

        assertNotNull(result)
        assertEquals("Emirates NBD", result!!.bankName)
        assertEquals(85.00, result.amount, 0.001)
        assertEquals("AED", result.currency)
        assertEquals("Starbucks", result.merchant)
        assertEquals("4321", result.cardOrAccountEnding)
        assertEquals("Restaurants & Cafés", result.suggestedCategoryName)
    }

    @Test
    fun parse_fuelPurchase() {
        val sms = "Payment of 35.00 JOD at Shell Gas Station using card *1122"
        val result = BankSmsParser.parse(sms)

        assertNotNull(result)
        assertEquals(35.00, result!!.amount, 0.001)
        assertEquals("JOD", result.currency)
        assertEquals("Car & Transportation", result.suggestedCategoryName)
    }
}

