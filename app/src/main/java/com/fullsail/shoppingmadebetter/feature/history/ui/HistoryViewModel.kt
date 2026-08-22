package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseHistoryUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    /** Trips newest first; an empty list is the "no purchases yet" case. */
    data class Success(val trips: List<PurchaseTrip>) : HistoryUiState
    data object Error : HistoryUiState
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getPurchaseHistoryUseCase: GetPurchaseHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the user's completed trips. Safe to call as a background refresh: the
     * History tab keeps its own back stack, so this ViewModel survives tab switches
     * and has to re-fetch after a trip is completed elsewhere. When trips are already
     * on screen it keeps them visible instead of flashing the spinner, and a failed
     * refresh leaves them in place — Loading and Error only show when there is
     * nothing to display yet.
     */
    fun load() {
        if (_uiState.value !is HistoryUiState.Success) {
            _uiState.value = HistoryUiState.Loading
        }
        viewModelScope.launch {
            when (val out = getPurchaseHistoryUseCase.execute(Unit)) {
                is GetPurchaseHistoryUseCase.Output.Success ->
                    _uiState.value = HistoryUiState.Success(out.trips)

                is GetPurchaseHistoryUseCase.Output.Failure ->
                    if (_uiState.value !is HistoryUiState.Success) {
                        _uiState.value = HistoryUiState.Error
                    }
            }
        }
    }
}
