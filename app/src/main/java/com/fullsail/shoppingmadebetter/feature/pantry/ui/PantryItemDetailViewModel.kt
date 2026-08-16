package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PantryItemDetailUiState {
    data object Loading : PantryItemDetailUiState
    data class Success(val item: InventoryItem) : PantryItemDetailUiState
    data object NotFound : PantryItemDetailUiState
    data object Error : PantryItemDetailUiState
}

@HiltViewModel
class PantryItemDetailViewModel @Inject constructor(
    private val getInventoryItemUseCase: GetInventoryItemUseCase,
    private val updateInventoryLowStockThresholdUseCase: UpdateInventoryLowStockThresholdUseCase,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<PantryItemDetailUiState>(PantryItemDetailUiState.Loading)
    val uiState: StateFlow<PantryItemDetailUiState> = _uiState.asStateFlow()

    fun load(id: String) {
        _uiState.value = PantryItemDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getInventoryItemUseCase.execute(id)) {
                is GetInventoryItemUseCase.Output.Success -> PantryItemDetailUiState.Success(out.inventoryItem)
                is GetInventoryItemUseCase.Output.NotFound -> PantryItemDetailUiState.NotFound
                is GetInventoryItemUseCase.Output.Failure -> PantryItemDetailUiState.Error
            }
        }
    }

    /**
     * Optimistically sets the shown item's low-stock [newThreshold] (`null` clears it),
     * persists it, and reverts if the save fails. No-op unless an item is loaded and the
     * value actually changes.
     */
    fun onLowStockThresholdChanged(newThreshold: Int?) {
        val current = _uiState.value as? PantryItemDetailUiState.Success ?: return
        val original = current.item
        if (newThreshold == original.lowStockThreshold) return

        _uiState.update { PantryItemDetailUiState.Success(original.copy(lowStockThreshold = newThreshold)) }
        viewModelScope.launch {
            val out = updateInventoryLowStockThresholdUseCase.execute(
                UpdateInventoryLowStockThreshold(original.productId, newThreshold),
            )
            if (out is UpdateInventoryLowStockThresholdUseCase.Output.Failure) {
                // Revert only if the shown item is still the one we edited.
                _uiState.update { state ->
                    if (state is PantryItemDetailUiState.Success && state.item.id == original.id) {
                        PantryItemDetailUiState.Success(original)
                    } else {
                        state
                    }
                }
            }
        }
    }
}
