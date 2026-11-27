package com.mousa.mizani.presentation.feature.recurring.due

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.core.startup.OpeningPromptSession
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.usecase.DueRecurringAttentionUseCase
import com.mousa.mizani.domain.usecase.DueRecurringItem
import com.mousa.mizani.domain.usecase.SkipRecurringOccurrenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DueRecurring"

enum class DueRecurringMessage { LOAD_FAILED, ADD_FAILED, SKIP_FAILED, ITEM_CHANGED }

/** The prefilled form to open: one item, for the period that was shown. */
data class RecurringFormRequest(val recurringId: Long, val dueMillis: Long)

data class DueRecurringUiState(
    val items: List<DueRecurringItem> = emptyList(),
    /** An Add or Skip is running; the buttons are disabled. */
    val isBusy: Boolean = false,
    /** The form the screen has to open now. */
    val openForm: RecurringFormRequest? = null,
    val message: DueRecurringMessage? = null
)

/** The question about due recurring items. All calls come from the main thread. */
@HiltViewModel
class DueRecurringViewModel @Inject constructor(
    private val dueAttention: DueRecurringAttentionUseCase,
    private val skipOccurrence: SkipRecurringOccurrenceUseCase,
    private val recurringRepository: IRecurringRepository,
    private val session: OpeningPromptSession
) : ViewModel() {

    val isDismissed: StateFlow<Boolean> = session.recurringDismissed

    private val _uiState = MutableStateFlow(DueRecurringUiState())
    val uiState: StateFlow<DueRecurringUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** True between [load] and [clear]: only then may a list be shown. */
    private var isListWanted = false

    fun load() {
        isListWanted = true
        loadJob?.cancel()
        loadJob = viewModelScope.launch { refresh() }
    }

    /** Drops the list when the question may not show, so an outdated one never reappears. */
    fun clear() {
        isListWanted = false
        loadJob?.cancel()
        _uiState.update { it.copy(items = emptyList(), openForm = null) }
    }

    fun later() = session.dismissRecurring()

    /** Asks for the item's form, unless the shown period was handled since the list was read. */
    fun add(item: DueRecurringItem) = runAction(DueRecurringMessage.ADD_FAILED) {
        val current = recurringRepository.getRecurringById(item.recurring.id)
        if (current == null || !current.isActive || current.nextDueDate != item.recurring.nextDueDate) {
            showMessage(DueRecurringMessage.ITEM_CHANGED)
            refreshIfWanted()
        } else if (isListWanted) {
            loadJob?.cancel()
            // The list is emptied at once, so the same entry cannot be tapped again.
            val request = RecurringFormRequest(current.id, item.recurring.nextDueDate.time)
            _uiState.update { it.copy(items = emptyList(), openForm = request) }
        }
    }

    fun skip(item: DueRecurringItem) = runAction(DueRecurringMessage.SKIP_FAILED) {
        val skipped = skipOccurrence(item.recurring.id, item.recurring.nextDueDate.time)
        if (!skipped) showMessage(DueRecurringMessage.ITEM_CHANGED)
        refreshIfWanted()
    }

    fun onFormOpened() = _uiState.update { it.copy(openForm = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** Runs one Add or Skip at a time; taps that arrive meanwhile are ignored. */
    private fun runAction(failure: DueRecurringMessage, action: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Action on a due recurring item failed", e)
                showMessage(failure)
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private suspend fun refreshIfWanted() {
        if (!isListWanted) return
        loadJob?.cancel()
        refresh()
    }

    private suspend fun refresh() {
        try {
            val items = dueAttention()
            _uiState.update { it.copy(items = items) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Loading due recurring items failed", e)
            _uiState.update { it.copy(items = emptyList(), message = DueRecurringMessage.LOAD_FAILED) }
            // The items stay due. Dismissing avoids repeating the error on every tab change,
            // and lets the Overview banner try again.
            session.dismissRecurring()
        }
    }

    private fun showMessage(message: DueRecurringMessage) = _uiState.update { it.copy(message = message) }
}
