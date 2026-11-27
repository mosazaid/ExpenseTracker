package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.BudgetProgressCalculator
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.domain.constants.HistoryPeriod
import com.mousa.mizani.domain.repository.IAlertRepository
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/** After an expense is saved, warns if its category budget is nearly used up or exceeded. */
class BudgetAlertUseCase @Inject constructor(
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val budgetProgressCalculator: BudgetProgressCalculator,
    private val categoryRepository: ICategoryRepository,
    private val notificationHelper: NotificationHelper,
    private val alertRepository: IAlertRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        val categoryId = transaction.categoryId ?: return
        if (transaction.type != TransactionType.EXPENSE) return

        val bounds = periodCalculator.getBounds(
            HistoryPeriod.MONTH, transaction.date, userPreferences.monthMode.first()
        )
        val progress = budgetProgressCalculator.getProgressForCategory(categoryId, bounds.start, bounds.end) ?: return
        if (!progress.isOverBudget && !progress.isNearLimit) return

        val categoryName = categoryRepository.getCategoryById(categoryId)?.name ?: "Category"
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(transaction.date)
        if (userPreferences.budgetAlertsEnabled.first()) {
            notificationHelper.showBudgetNotification(
                categoryName, progress.spent, progress.limit, isExceeded = progress.isOverBudget
            )
        }
        alertRepository.postBudgetAlert(
            categoryId, categoryName, progress.spent, progress.limit,
            isExceeded = progress.isOverBudget, periodKey = monthKey
        )
    }
}
