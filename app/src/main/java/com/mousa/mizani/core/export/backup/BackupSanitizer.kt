package com.mousa.mizani.core.export.backup

/** Result of [BackupSanitizer.sanitize]: the cleaned snapshot plus a human-readable note per fix. */
data class SanitizedBackup(
    val snapshot: BackupSnapshot,
    val fixes: List<String>
) {
    val hasFixes: Boolean get() = fixes.isNotEmpty()
}

/**
 * Makes a backup safe to insert under the database's foreign keys.
 *
 * Older databases can hold rows whose parent was deleted (for example a subcategory whose
 * category is gone). Inserting such a row fails with SQLITE_CONSTRAINT_FOREIGNKEY (787) and
 * aborts the whole restore. Each rule below mirrors what the database itself would have done
 * when the parent was deleted:
 *  - subcategories, budgets → CASCADE: dropped
 *  - recurring items, transactions → category reference cleared (they keep their data)
 *  - splits → CASCADE on their transaction: dropped
 *  - loan payments → CASCADE on their loan: dropped
 */
object BackupSanitizer {

    fun sanitize(snapshot: BackupSnapshot): SanitizedBackup {
        val fixes = mutableListOf<String>()
        val categoryIds = snapshot.categories.map { it.id }.toSet()
        val transactionIds = snapshot.transactions.map { it.id }.toSet()
        val loanIds = snapshot.configuredLoans.map { it.id }.toSet()

        val (subCategories, orphanSubs) = snapshot.subCategories.partition { it.categoryId in categoryIds }
        orphanSubs.forEach {
            fixes += "Skipped subcategory '${it.name}' (id=${it.id}): its category ${it.categoryId} no longer exists"
        }

        val (budgets, orphanBudgets) = snapshot.budgets.partition { it.categoryId in categoryIds }
        orphanBudgets.forEach {
            fixes += "Skipped budget id=${it.id}: its category ${it.categoryId} no longer exists"
        }

        val recurring = snapshot.recurringTransactions.map { r ->
            val categoryId = r.categoryId
            if (categoryId == null || categoryId in categoryIds) {
                r
            } else {
                fixes += "Cleared missing category $categoryId on recurring item '${r.description}' (id=${r.id})"
                r.copy(categoryId = null)
            }
        }

        val transactions = snapshot.transactions.map { t ->
            val categoryId = t.categoryId
            if (categoryId == null || categoryId in categoryIds) {
                t
            } else {
                fixes += "Cleared missing category $categoryId on transaction id=${t.id}"
                t.copy(categoryId = null)
            }
        }

        val (splits, orphanSplits) = snapshot.transactionSplits.partition { it.transactionId in transactionIds }
        orphanSplits.forEach {
            fixes += "Skipped split id=${it.id}: its transaction ${it.transactionId} no longer exists"
        }

        val (payments, orphanPayments) = snapshot.monthlyLoanPayments.partition { it.loanConfigId in loanIds }
        orphanPayments.forEach {
            fixes += "Skipped loan payment id=${it.id} (${it.monthKey}): its loan ${it.loanConfigId} no longer exists"
        }

        val cleaned = snapshot.copy(
            subCategories = subCategories,
            budgets = budgets,
            recurringTransactions = recurring,
            transactions = transactions,
            transactionSplits = splits,
            monthlyLoanPayments = payments
        )
        return SanitizedBackup(cleaned, fixes.toList())
    }
}
