package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShoppingTripsUiState {
    data object Loading : ShoppingTripsUiState
    data class Success(val trips: List<ShoppingTrip>) : ShoppingTripsUiState
    data object Error : ShoppingTripsUiState
}

@HiltViewModel
class ShoppingTripsViewModel @Inject constructor(
    private val getShoppingTripsUseCase: GetShoppingTripsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingTripsUiState>(ShoppingTripsUiState.Loading)
    val uiState: StateFlow<ShoppingTripsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = ShoppingTripsUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getShoppingTripsUseCase.execute(Unit)) {
                is GetShoppingTripsUseCase.Output.Success ->
                    ShoppingTripsUiState.Success(out.trips)
                is GetShoppingTripsUseCase.Output.Failure ->
                    ShoppingTripsUiState.Error
            }
        }
    }
}
