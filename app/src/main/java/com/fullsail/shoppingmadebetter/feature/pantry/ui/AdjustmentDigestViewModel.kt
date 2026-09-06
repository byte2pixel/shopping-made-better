package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentDigestEntry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetAdjustmentDigestUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustment
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustmentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdjustmentDigestUiState {
    data object Loading : AdjustmentDigestUiState

    /** [undoingAll] disables the row and bulk actions while the batch runs. */
    data class Success(
        val entries: List<AdjustmentDigestEntry>,
        val undoingAll: Boolean = false,
    ) : AdjustmentDigestUiState

    data object Error : AdjustmentDigestUiState
}

/** One-shot outcomes surfaced to the user as a snackbar. */
sealed interface AdjustmentDigestEvent {
    data class UndoFailed(val productName: String) : AdjustmentDigestEvent

    /** How many of [attempted] rows "Undo all" reversed. */
    data class UndoneAll(val succeeded: Int, val attempted: Int) : AdjustmentDigestEvent
}

@HiltViewModel
class AdjustmentDigestViewModel @Inject constructor(
    private val getAdjustmentDigestUseCase: GetAdjustmentDigestUseCase,
    private val undoInventoryAdjustmentUseCase: UndoInventoryAdjustmentUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AdjustmentDigestUiState>(AdjustmentDigestUiState.Loading)
    val uiState: StateFlow<AdjustmentDigestUiState> = _uiState.asStateFlow()

    private val _events = Channel<AdjustmentDigestEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = AdjustmentDigestUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getAdjustmentDigestUseCase.execute(Unit)) {
                is GetAdjustmentDigestUseCase.Output.Success ->
                    AdjustmentDigestUiState.Success(out.entries)

                is GetAdjustmentDigestUseCase.Output.Failure -> AdjustmentDigestUiState.Error
            }
        }
    }

    /** Reverses [entry], dropping its row straight away and putting it back if the write fails. */
    fun onUndo(entry: AdjustmentDigestEntry) {
        val state = _uiState.value
        if (state !is AdjustmentDigestUiState.Success || state.undoingAll) return
        val index = state.entries.indexOfFirst { it.adjustmentId == entry.adjustmentId }
        if (index < 0) return
        _uiState.value = state.copy(entries = state.entries - entry)
        viewModelScope.launch {
            if (!undo(entry)) {
                restore(entry, index)
                _events.send(AdjustmentDigestEvent.UndoFailed(entry.productName))
            }
        }
    }

    /**
     * Reverses every listed row, one call at a time so each undo is its own transaction and a
     * failure costs only its own row. Rows that fail stay listed.
     */
    fun onUndoAll() {
        val state = _uiState.value
        if (state !is AdjustmentDigestUiState.Success || state.undoingAll) return
        val batch = state.entries
        if (batch.isEmpty()) return
        _uiState.value = state.copy(undoingAll = true)
        viewModelScope.launch {
            var succeeded = 0
            batch.forEach { entry ->
                if (undo(entry)) {
                    succeeded++
                    val current = _uiState.value
                    if (current is AdjustmentDigestUiState.Success) {
                        _uiState.value = current.copy(entries = current.entries - entry)
                    }
                }
            }
            val current = _uiState.value
            if (current is AdjustmentDigestUiState.Success) {
                _uiState.value = current.copy(undoingAll = false)
            }
            _events.send(AdjustmentDigestEvent.UndoneAll(succeeded, batch.size))
        }
    }

    private suspend fun undo(entry: AdjustmentDigestEntry): Boolean =
        undoInventoryAdjustmentUseCase
            .execute(UndoInventoryAdjustment(entry.adjustmentId)) is
            UndoInventoryAdjustmentUseCase.Output.Success

    /** Puts [entry] back where it was, so a failed undo does not reorder the list. */
    private fun restore(entry: AdjustmentDigestEntry, index: Int) {
        val state = _uiState.value
        if (state !is AdjustmentDigestUiState.Success) return
        val entries = state.entries.toMutableList()
        entries.add(index.coerceAtMost(entries.size), entry)
        _uiState.value = state.copy(entries = entries)
    }
}
