package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.BudgetProgressCalculator
import com.mousa.mizani.domain.calculator.CategoryBudgetProgress
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.domain.constants.HistoryPeriod
import com.mousa.mizani.domain.repository.IAlertRepository
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import com.mousa.mizani.util.NotificationHelper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertSwitchTest {

    private val userPreferences = mock(UserPreferences::class.java)
    private val notificationHelper = mock(NotificationHelper::class.java)
    private val recurringRepository = mock(IRecurringRepository::class.java)
    private val alertRepository = mock(IAlertRepository::class.java)
    private val categoryRepository = mock(ICategoryRepository::class.java)
    private val budgetProgressCalculator = mock(BudgetProgressCalculator::class.java)
    private val periodCalculator = PeriodCalculator(mock(ITransactionRepository::class.java), categoryRepository)

    private val now = Date(1_700_000_000_000L)

    private fun item(id: Long, autoAdd: Boolean) = RecurringTransaction(
        id = id, amount = 10.0, description = "item$id", type = TransactionType.EXPENSE, categoryId = null,
        accountType = AccountType.BANK, frequency = RecurrenceFrequency.MONTHLY,
        nextDueDate = Date(0), autoAdd = autoAdd
    )

    private suspend fun runRecurringReminder(enabled: Boolean, due: List<RecurringTransaction>) {
        `when`(userPreferences.salaryAlertsEnabled).thenReturn(flowOf(enabled))
        `when`(recurringRepository.getDueReminders(now)).thenReturn(due)

        RecurringReminderUseCase(recurringRepository, userPreferences, notificationHelper)(now)
    }

    @Test
    fun `recurring switch off posts no reminder`() = runTest {
        runRecurringReminder(enabled = false, due = listOf(item(1, autoAdd = false)))

        verifyNoInteractions(notificationHelper)
    }

    @Test
    fun `recurring switch on reminds only reminder-only items`() = runTest {
        val manual = item(2, autoAdd = false)

        runRecurringReminder(enabled = true, due = listOf(item(1, autoAdd = true), manual))

        verify(notificationHelper, times(1)).showSalaryReminderNotification(manual, false)
        verifyNoMoreInteractions(notificationHelper)
    }

    private suspend fun runOverBudgetAlert(enabled: Boolean) {
        `when`(userPreferences.budgetAlertsEnabled).thenReturn(flowOf(enabled))
        `when`(userPreferences.monthMode).thenReturn(flowOf(MonthMode.CALENDAR))
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, now, MonthMode.CALENDAR)
        `when`(budgetProgressCalculator.getProgressForCategory(CATEGORY_ID, bounds.start, bounds.end))
            .thenReturn(CategoryBudgetProgress(CATEGORY_ID, limit = 100.0, spent = 120.0))
        `when`(categoryRepository.getCategoryById(CATEGORY_ID))
            .thenReturn(Category(id = CATEGORY_ID, name = "Food", icon = "", color = "", type = TransactionType.EXPENSE))

        val useCase = BudgetAlertUseCase(
            userPreferences, periodCalculator, budgetProgressCalculator,
            categoryRepository, notificationHelper, alertRepository
        )
        useCase(
            Transaction(
                amount = 120.0, description = "Groceries", date = now, type = TransactionType.EXPENSE,
                categoryId = CATEGORY_ID, accountType = AccountType.CASH
            )
        )
    }

    private suspend fun verifyInboxEntryPostedOnce() {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(now)
        verify(alertRepository, times(1))
            .postBudgetAlert(CATEGORY_ID, "Food", 120.0, 100.0, true, monthKey)
    }

    @Test
    fun `budget switch off keeps the inbox entry but posts no notification`() = runTest {
        runOverBudgetAlert(enabled = false)

        verifyNoInteractions(notificationHelper)
        verifyInboxEntryPostedOnce()
    }

    @Test
    fun `budget switch on posts both`() = runTest {
        runOverBudgetAlert(enabled = true)

        verify(notificationHelper, times(1)).showBudgetNotification("Food", 120.0, 100.0, true)
        verifyInboxEntryPostedOnce()
    }

    private companion object {
        const val CATEGORY_ID = 1L
    }
}
