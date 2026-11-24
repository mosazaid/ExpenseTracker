package com.mousa.mizani.core.export

import com.mousa.mizani.data.database.entities.TransactionType
import java.util.Date

data class TransactionExportFilter(
    val startDate: Date? = null,
    val endDate: Date? = null,
    val types: Set<TransactionType>? = null,
    val label: String = "All transactions"
) {
    val isActive: Boolean
        get() = startDate != null || endDate != null || types != null
}
