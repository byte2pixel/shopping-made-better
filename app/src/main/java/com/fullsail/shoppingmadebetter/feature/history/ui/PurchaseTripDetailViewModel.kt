package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseTripUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PurchaseTripDetailUiState {
    data object Loading : PurchaseTripDetailUiState
    data class Success(val trip: PurchaseTrip) : PurchaseTripDetailUiState

    /** The trip was deleted, or never belonged to this user. */
    data object NotFound : PurchaseTripDetailUiState
    data object Error : PurchaseTripDetailUiState
}

@HiltViewModel
class PurchaseTripDetailViewModel @Inject constructor(
    private val getPurchaseTripUseCase: GetPurchaseTripUseCase,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<PurchaseTripDetailUiState>(PurchaseTripDetailUiState.Loading)
    val uiState: StateFlow<PurchaseTripDetailUiState> = _uiState.asStateFlow()

    /** Loads the trip [purchaseId]; called from the screen, which owns the id. */
    fun load(purchaseId: String) {
        _uiState.value = PurchaseTripDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getPurchaseTripUseCase.execute(purchaseId)) {
                is GetPurchaseTripUseCase.Output.Success ->
                    PurchaseTripDetailUiState.Success(out.trip)

                GetPurchaseTripUseCase.Output.NotFound -> PurchaseTripDetailUiState.NotFound
                is GetPurchaseTripUseCase.Output.Failure -> PurchaseTripDetailUiState.Error
            }
        }
    }
}
