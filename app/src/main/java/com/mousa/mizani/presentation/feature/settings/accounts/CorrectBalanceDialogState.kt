package com.mousa.mizani.presentation.feature.settings.accounts

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.domain.calculator.BalanceCorrection
import com.mousa.mizani.domain.calculator.evaluateBalanceCorrection
import java.math.BigDecimal

/** The open correction dialog. A closed dialog has no state. */
internal data class CorrectionDialogState(
    val account: AccountType,
    val input: String = "",
    val isEdited: Boolean = false
) {
    fun typed(text: String): CorrectionDialogState =
        copy(input = text, isEdited = isEdited || text != input)
}

/** Why the dialog cannot save. */
internal enum class CorrectionProblem { BALANCE_UNAVAILABLE, INVALID, TOO_MANY_DECIMALS, UNCHANGED }

/** What the dialog shows. Only a non-null [change] may be confirmed. */
internal data class CorrectionPreview(
    val old: Double?,
    val change: BalanceCorrection.Change? = null,
    val problem: CorrectionProblem? = null
)

internal data class ConfirmedCorrection(
    val account: AccountType,
    val change: BalanceCorrection.Change
)

internal fun previewCorrection(
    state: CorrectionDialogState,
    balances: Map<AccountType, Double>,
    decimals: Int
): CorrectionPreview {
    val current = balances[state.account]
        ?: return CorrectionPreview(old = null, problem = CorrectionProblem.BALANCE_UNAVAILABLE)
    return when (val result = evaluateBalanceCorrection(state.input, current, decimals)) {
        // Saving more decimals than are shown would store an amount the preview did not show.
        is BalanceCorrection.Change -> if (result.new.hasMoreDecimalsThan(decimals)) {
            CorrectionPreview(old = current, problem = CorrectionProblem.TOO_MANY_DECIMALS)
        } else {
            CorrectionPreview(old = current, change = result)
        }
        BalanceCorrection.Unchanged ->
            CorrectionPreview(old = current, problem = CorrectionProblem.UNCHANGED)
        // An untouched empty field is not an error yet.
        BalanceCorrection.Invalid -> CorrectionPreview(
            old = current,
            problem = CorrectionProblem.INVALID.takeIf { state.isEdited }
        )
    }
}

// Trailing zeros are not decimals: "10.1300" is 10.13.
private fun Double.hasMoreDecimalsThan(decimals: Int): Boolean =
    BigDecimal.valueOf(this).stripTrailingZeros().scale() > decimals

/**
 * Returns what to save, or null when the dialog is already closed. The caller passes the state as
 * it is at the moment of the tap and closes the dialog, so a second tap saves nothing.
 */
internal fun confirmCorrection(
    state: CorrectionDialogState?,
    change: BalanceCorrection.Change
): ConfirmedCorrection? = state?.let { ConfirmedCorrection(it.account, change) }

/** The number of decimals `CurrencyUtils.formatCurrency` shows, which is not always the currency's own. */
internal fun shownDecimals(currencyCode: String): Int =
    if (CurrencyUtils.getCurrencyInfo(currencyCode).decimalPlaces == 3) 3 else 2

/** Keeps the open dialog and its typed text across rotation. An empty list is not saved. */
internal val CorrectionDialogStateSaver: Saver<CorrectionDialogState?, Any> = listSaver(
    save = { state ->
        if (state == null) emptyList() else listOf(state.account.name, state.input, state.isEdited)
    },
    restore = { saved ->
        CorrectionDialogState(
            account = AccountType.valueOf(saved[0] as String),
            input = saved[1] as String,
            isEdited = saved[2] as Boolean
        )
    }
)
