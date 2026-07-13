package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PantryUiState {
    object Loading : PantryUiState
    data class Success(val inventoryItems: List<InventoryItem>) : PantryUiState
    data object Error: PantryUiState
}

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val getInventoryUseCase: GetInventoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PantryUiState>(PantryUiState.Loading)
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init { loadInventory() }

    fun loadInventory() {
        _uiState.value = PantryUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getInventoryUseCase.execute(Unit)) {
                is GetInventoryUseCase.Output.Success -> PantryUiState.Success(out.inventoryItems)
                is GetInventoryUseCase.Output.Failure -> PantryUiState.Error
            }
        }
    }
}
