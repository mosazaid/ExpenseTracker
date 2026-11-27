package com.mousa.mizani.presentation

import androidx.compose.runtime.saveable.SaverScope
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.domain.calculator.BalanceCorrection
import com.mousa.mizani.presentation.feature.settings.accounts.ConfirmedCorrection
import com.mousa.mizani.presentation.feature.settings.accounts.CorrectionDialogState
import com.mousa.mizani.presentation.feature.settings.accounts.CorrectionDialogStateSaver
import com.mousa.mizani.presentation.feature.settings.accounts.CorrectionProblem
import com.mousa.mizani.presentation.feature.settings.accounts.confirmCorrection
import com.mousa.mizani.presentation.feature.settings.accounts.previewCorrection
import com.mousa.mizani.presentation.feature.settings.accounts.shownDecimals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorrectBalanceDialogStateTest {

    private val balances = mapOf(AccountType.CASH to 100.0, AccountType.BANK to 250.0)
    private val opened = CorrectionDialogState(AccountType.CASH)

    @Test
    fun `a newly opened dialog shows the old amount without an error`() {
        val preview = previewCorrection(opened, balances, decimals = 3)

        assertEquals(100.0, preview.old)
        assertNull(preview.change)
        assertNull(preview.problem)
    }

    @Test
    fun `typed text is kept exactly as typed`() {
        assertEquals("12,5", opened.typed("12,5").input)
    }

    @Test
    fun `unreadable text is reported as invalid`() {
        val preview = previewCorrection(opened.typed("12,5"), balances, decimals = 3)

        assertEquals(CorrectionProblem.INVALID, preview.problem)
        assertNull(preview.change)
    }

    @Test
    fun `clearing the field after typing is reported as invalid`() {
        val preview = previewCorrection(opened.typed("1").typed(""), balances, decimals = 3)

        assertEquals(CorrectionProblem.INVALID, preview.problem)
    }

    @Test
    fun `the current amount is reported as unchanged`() {
        val preview = previewCorrection(opened.typed("100"), balances, decimals = 3)

        assertEquals(CorrectionProblem.UNCHANGED, preview.problem)
        assertNull(preview.change)
    }

    @Test
    fun `a new amount is previewed against the chosen account`() {
        val state = CorrectionDialogState(AccountType.BANK).typed("200")

        val preview = previewCorrection(state, balances, decimals = 3)

        assertEquals(BalanceCorrection.Change(250.0, 200.0, -50.0), preview.change)
        assertEquals(250.0, preview.old)
        assertNull(preview.problem)
    }

    @Test
    fun `more decimals than a 2 decimal currency shows cannot be saved`() {
        val preview = previewCorrection(opened.typed("10.12999"), balances, decimals = 2)

        assertEquals(CorrectionProblem.TOO_MANY_DECIMALS, preview.problem)
        assertNull(preview.change)
    }

    @Test
    fun `a third decimal is rejected at 2 decimals`() {
        val preview = previewCorrection(opened.typed("10.125"), balances, decimals = 2)

        assertEquals(CorrectionProblem.TOO_MANY_DECIMALS, preview.problem)
        assertNull(preview.change)
    }

    @Test
    fun `exactly 2 decimals are saved as typed`() {
        val preview = previewCorrection(opened.typed("10.13"), balances, decimals = 2)

        assertEquals(10.13, requireNotNull(preview.change).new, 0.0)
        assertNull(preview.problem)
    }

    @Test
    fun `trailing zeros do not count as decimals`() {
        val preview = previewCorrection(opened.typed("10.1300"), balances, decimals = 2)

        assertEquals(10.13, requireNotNull(preview.change).new, 0.0)
        assertNull(preview.problem)
    }

    @Test
    fun `a whole amount ending in zeros is accepted`() {
        val preview = previewCorrection(opened.typed("1000"), balances, decimals = 2)

        assertEquals(1000.0, requireNotNull(preview.change).new, 0.0)
    }

    @Test
    fun `exactly 3 decimals are saved as typed`() {
        val preview = previewCorrection(opened.typed("10.125"), balances, decimals = 3)

        assertEquals(10.125, requireNotNull(preview.change).new, 0.0)
        assertNull(preview.problem)
    }

    @Test
    fun `a fourth decimal is rejected at 3 decimals`() {
        val preview = previewCorrection(opened.typed("10.1299"), balances, decimals = 3)

        assertEquals(CorrectionProblem.TOO_MANY_DECIMALS, preview.problem)
        assertNull(preview.change)
    }

    @Test
    fun `Arabic digits with too many decimals are rejected`() {
        // 10.129 typed with Arabic-Indic digits and the Arabic decimal separator.
        val input = "١٠٫١٢٩"

        val preview = previewCorrection(opened.typed(input), balances, decimals = 2)

        assertEquals(CorrectionProblem.TOO_MANY_DECIMALS, preview.problem)
        assertNull(preview.change)
    }

    @Test
    fun `typed text is evaluated again when the balance changes`() {
        val state = opened.typed("120")

        val before = previewCorrection(state, balances, decimals = 3)
        val after = previewCorrection(state, mapOf(AccountType.CASH to 120.0), decimals = 3)

        assertEquals(BalanceCorrection.Change(100.0, 120.0, 20.0), before.change)
        assertEquals(CorrectionProblem.UNCHANGED, after.problem)
        assertNull(after.change)
    }

    @Test
    fun `nothing can be saved while the balance is missing`() {
        val preview = previewCorrection(opened.typed("120"), emptyMap(), decimals = 3)

        assertEquals(CorrectionProblem.BALANCE_UNAVAILABLE, preview.problem)
        assertNull(preview.old)
        assertNull(preview.change)
    }

    @Test
    fun `confirming uses the open account and the previewed change`() {
        val change = BalanceCorrection.Change(100.0, 120.0, 20.0)

        assertEquals(
            ConfirmedCorrection(AccountType.CASH, change),
            confirmCorrection(opened.typed("120"), change)
        )
    }

    @Test
    fun `a second confirm after the dialog closed does nothing`() {
        val change = BalanceCorrection.Change(100.0, 120.0, 20.0)

        assertNull(confirmCorrection(null, change))
    }

    @Test
    fun `comparison precision matches the formatted balance`() {
        CurrencyUtils.SUPPORTED_CURRENCIES.forEach { currency ->
            val shown = CurrencyUtils.formatCurrency(1.0, currency.code)
                .substringBefore(' ')
                .substringAfter('.')

            assertEquals(currency.code, shown.length, shownDecimals(currency.code))
        }
    }

    @Test
    fun `the open account and typed text are restored after saving`() {
        val state = CorrectionDialogState(AccountType.BANK).typed("5").typed("")
        val saved = with(CorrectionDialogStateSaver) { SaverScope { true }.save(state) }

        assertEquals(state, CorrectionDialogStateSaver.restore(requireNotNull(saved)))
    }

    @Test
    fun `a closed dialog saves nothing`() {
        val saved = with(CorrectionDialogStateSaver) { SaverScope { true }.save(null) }

        assertNull(saved)
    }
}
