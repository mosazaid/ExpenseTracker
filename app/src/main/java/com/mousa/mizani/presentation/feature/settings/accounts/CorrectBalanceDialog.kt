package com.mousa.mizani.presentation.feature.settings.accounts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.mousa.mizani.R
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.domain.calculator.BalanceCorrection

private val MinTouchTarget = 48.dp

@get:StringRes
internal val AccountType.correctionLabelRes: Int
    get() = if (this == AccountType.BANK) R.string.account_bank else R.string.account_cash

@Composable
internal fun CorrectBalanceDialog(
    account: AccountType,
    input: String,
    preview: CorrectionPreview,
    decimals: Int,
    onInputChange: (String) -> Unit,
    onConfirm: (BalanceCorrection.Change) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.correct_balance_title,
                    stringResource(account.correctionLabelRes)
                )
            )
        },
        text = { CorrectionFields(input, preview, decimals, onInputChange) },
        confirmButton = {
            TextButton(
                onClick = { preview.change?.let(onConfirm) },
                enabled = preview.change != null,
                modifier = Modifier.heightIn(min = MinTouchTarget)
            ) {
                Text(stringResource(R.string.correct_balance_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = MinTouchTarget)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CorrectionFields(
    input: String,
    preview: CorrectionPreview,
    decimals: Int,
    onInputChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.correct_balance_explanation),
            style = MaterialTheme.typography.bodyMedium
        )
        preview.old?.let {
            LabeledAmount(R.string.correct_balance_old, CurrencyUtils.formatCurrency(it))
        }
        AmountField(input, preview.problem, decimals, onInputChange)
        preview.change?.let { change ->
            LabeledAmount(R.string.correct_balance_new, CurrencyUtils.formatCurrency(change.new))
            LabeledAmount(R.string.correct_balance_difference, formatDifference(change.difference))
        }
    }
}

/** The message sits in the field's supporting text, so screen readers read it with the field. */
@Composable
private fun AmountField(
    input: String,
    problem: CorrectionProblem?,
    decimals: Int,
    onInputChange: (String) -> Unit
) {
    val message = problem?.let {
        // Only the too-many-decimals message has a placeholder; the others ignore the argument.
        stringResource(it.messageRes, decimals)
    }
    val errorMessage = message?.takeIf {
        problem == CorrectionProblem.INVALID || problem == CorrectionProblem.TOO_MANY_DECIMALS
    }
    // The text is passed on unfiltered: dropping a character could change the amount.
    OutlinedTextField(
        value = input,
        onValueChange = onInputChange,
        label = { Text(stringResource(R.string.correct_balance_new)) },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = message?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            // Replaces the generic "invalid input" a screen reader would announce.
            .semantics { errorMessage?.let { error(it) } }
    )
}

@Composable
private fun LabeledAmount(@StringRes labelRes: Int, amount: String) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AmountText(amount = amount)
    }
}

/** Amounts read left to right, so a sign stays before the number in Arabic too. */
@Composable
internal fun AmountText(
    amount: String,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    Text(text = amount, style = style.copy(textDirection = TextDirection.Ltr))
}

private fun formatDifference(difference: Double): String {
    val sign = if (difference > 0) "+" else ""
    return sign + CurrencyUtils.formatCurrency(difference)
}

@get:StringRes
private val CorrectionProblem.messageRes: Int
    get() = when (this) {
        CorrectionProblem.BALANCE_UNAVAILABLE -> R.string.correct_balance_unavailable
        CorrectionProblem.INVALID -> R.string.correct_balance_invalid
        CorrectionProblem.TOO_MANY_DECIMALS -> R.string.correct_balance_too_many_decimals
        CorrectionProblem.UNCHANGED -> R.string.correct_balance_unchanged
    }
