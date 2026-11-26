package com.mousa.mizani.domain.calculator

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecurrenceScheduleTest {

    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private fun date(text: String): Date = format.parse("$text 09:00")!!
    private fun Date.day(): String = format.format(this).substring(0, 10)

    private fun item(
        due: String,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        amount: Double = 10.0,
        type: TransactionType = TransactionType.EXPENSE,
        anchorDay: Int? = null
    ) = RecurringTransaction(
        id = 1, amount = amount, description = "x", type = type, categoryId = null,
        accountType = AccountType.BANK, frequency = frequency, nextDueDate = date(due),
        autoAdd = true, anchorDay = anchorDay
    )

    private fun RecurringTransaction.dates(times: Int): List<String> =
        generateSequence(this) { it.advanced() }.take(times + 1).drop(1).map { it.nextDueDate.day() }.toList()

    @Test
    fun `monthly item due on the 31st returns to the 31st after a short month`() {
        assertEquals(listOf("2027-02-28", "2027-03-31", "2027-04-30", "2027-05-31"), item("2027-01-31").dates(4))
    }

    @Test
    fun `yearly item due on leap day returns to leap day`() {
        assertEquals(
            listOf("2029-02-28", "2030-02-28", "2031-02-28", "2032-02-29"),
            item("2028-02-29", RecurrenceFrequency.YEARLY).dates(4)
        )
    }

    @Test
    fun `daily and weekly items keep no anchor day`() {
        assertNull(item("2027-01-31", RecurrenceFrequency.DAILY, anchorDay = 31).advanced().anchorDay)
        assertEquals(listOf("2027-02-07"), item("2027-01-31", RecurrenceFrequency.WEEKLY).dates(1))
    }

    @Test
    fun `every missed period is listed with its own due date`() {
        val occurrences = item("2027-01-31").dueOccurrences(date("2027-04-15"))

        assertEquals(listOf("2027-01-31", "2027-02-28", "2027-03-31"), occurrences.map { it.nextDueDate.day() })
    }

    @Test
    fun `catch up is capped`() {
        val occurrences = item("2020-01-01", RecurrenceFrequency.DAILY).dueOccurrences(date("2027-01-01"), limit = 5)

        assertEquals(5, occurrences.size)
    }

    @Test
    fun `skipping missed periods keeps one due today`() {
        val result = item("2027-01-31").skippingMissedBefore(date("2027-03-31"))

        assertEquals("2027-03-31", result.nextDueDate.day())
    }

    @Test
    fun `auto add stops at the first expense the balance cannot cover`() {
        val plan = planAutoAdd(item("2027-01-10", amount = 40.0), date("2027-04-15"), availableBalance = 100.0)

        assertEquals(listOf("2027-01-10", "2027-02-10"), plan.toRecord.map { it.nextDueDate.day() })
        assertEquals("2027-03-10", plan.blocked?.nextDueDate?.day())
    }

    @Test
    fun `auto add allows spending the exact balance despite decimal rounding`() {
        val plan = planAutoAdd(item("2027-01-10", amount = 0.1), date("2027-03-15"), availableBalance = 0.3)

        assertEquals(3, plan.toRecord.size)
        assertNull(plan.blocked)
    }

    @Test
    fun `income is never blocked by balance`() {
        val plan = planAutoAdd(
            item("2027-01-10", type = TransactionType.INCOME), date("2027-02-15"), availableBalance = 0.0
        )

        assertEquals(2, plan.toRecord.size)
        assertNull(plan.blocked)
    }

    @Test
    fun `editing without changing date or frequency keeps the original anchor`() {
        val drifted = item("2027-02-28", anchorDay = 31)

        assertEquals(31, anchorDayFor(drifted, drifted.nextDueDate, RecurrenceFrequency.MONTHLY))
        assertEquals(15, anchorDayFor(drifted, date("2027-03-15"), RecurrenceFrequency.MONTHLY))
        assertEquals(28, anchorDayFor(null, date("2027-02-28"), RecurrenceFrequency.MONTHLY))
    }
}
