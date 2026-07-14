package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}
