package com.mousa.mizani.presentation.feature.recurring.due

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mousa.mizani.R
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.core.time.DateUtils
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.domain.usecase.DueRecurringItem

private val MIN_TOUCH_TARGET = 48.dp

/**
 * Asks about due recurring items while [isAllowed] and the user has not chosen Later.
 * The list is read again each time it may show: on a new [screenKey] and on every resume.
 */
@Composable
fun DueRecurringPrompt(
    isAllowed: Boolean,
    screenKey: String?,
    onOpenForm: (RecurringFormRequest) -> Unit,
    viewModel: DueRecurringViewModel = hiltViewModel()
) {
    val isDismissed by viewModel.isDismissed.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val canShow = isAllowed && !isDismissed

    LifecycleResumeEffect(canShow, screenKey) {
        if (canShow) viewModel.load()
        onPauseOrDispose { viewModel.clear() }
    }

    LaunchedEffect(state.openForm) {
        state.openForm?.let { request ->
            viewModel.onFormOpened()
            onOpenForm(request)
        }
    }

    // A toast, because a message must stay visible whether or not the sheet is open.
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            Toast.makeText(context, message.textRes(), Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    if (canShow && state.items.isNotEmpty()) {
        DueRecurringSheet(
            items = state.items,
            isBusy = state.isBusy,
            onAdd = viewModel::add,
            onSkip = viewModel::skip,
            onLater = viewModel::later
        )
    }
}

@StringRes
private fun DueRecurringMessage.textRes(): Int = when (this) {
    DueRecurringMessage.LOAD_FAILED -> R.string.recurring_due_load_failed
    DueRecurringMessage.ADD_FAILED -> R.string.recurring_due_add_failed
    DueRecurringMessage.SKIP_FAILED -> R.string.recurring_due_skip_failed
    DueRecurringMessage.ITEM_CHANGED -> R.string.recurring_due_item_changed
}

/** Swiping the sheet away and Back both mean Later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueRecurringSheet(
    items: List<DueRecurringItem>,
    isBusy: Boolean,
    onAdd: (DueRecurringItem) -> Unit,
    onSkip: (DueRecurringItem) -> Unit,
    onLater: () -> Unit
) {
    var skipTarget by remember { mutableStateOf<DueRecurringItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") { DueRecurringHeader() }
            items(items, key = { it.recurring.id }) { item ->
                DueRecurringCard(
                    item = item,
                    isEnabled = !isBusy,
                    onAdd = { onAdd(item) },
                    onSkip = { skipTarget = item }
                )
            }
            item(key = "later") {
                TextButton(
                    onClick = onLater,
                    modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET)
                ) {
                    Text(stringResource(R.string.recurring_due_later))
                }
            }
        }
    }

    skipTarget?.let { target ->
        SkipConfirmationDialog(
            recurring = target.recurring,
            onConfirm = {
                skipTarget = null
                onSkip(target)
            },
            onCancel = { skipTarget = null }
        )
    }
}

@Composable
private fun DueRecurringHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.recurring_due_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.recurring_due_sheet_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DueRecurringCard(
    item: DueRecurringItem,
    isEnabled: Boolean,
    onAdd: () -> Unit,
    onSkip: () -> Unit
) {
    val recurring = item.recurring
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = recurringName(recurring),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = CurrencyUtils.formatCurrency(recurring.amount),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.recurring_due_date, dueDateText(recurring)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.isBlocked) {
                Text(
                    text = stringResource(R.string.recurring_due_blocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            // Wraps onto two lines when large text leaves no room side by side.
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAdd,
                    enabled = isEnabled,
                    modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)
                ) {
                    Text(stringResource(R.string.recurring_due_add))
                }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = isEnabled,
                    modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)
                ) {
                    Text(stringResource(R.string.recurring_due_skip))
                }
            }
        }
    }
}

@Composable
private fun SkipConfirmationDialog(
    recurring: RecurringTransaction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.recurring_due_skip_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.recurring_due_skip_confirm_message,
                    recurringName(recurring),
                    CurrencyUtils.formatCurrency(recurring.amount),
                    dueDateText(recurring)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
                Text(stringResource(R.string.recurring_due_skip))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun recurringName(recurring: RecurringTransaction): String = recurring.description.ifBlank {
    stringResource(
        if (recurring.type == TransactionType.INCOME) {
            R.string.recurring_banner_title_income
        } else {
            R.string.recurring_banner_title_expense
        }
    )
}

private fun dueDateText(recurring: RecurringTransaction): String =
    DateUtils.format(recurring.nextDueDate, DateUtils.PATTERN_DEFAULT)
