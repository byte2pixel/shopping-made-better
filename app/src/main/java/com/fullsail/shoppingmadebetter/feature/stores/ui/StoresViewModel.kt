package com.fullsail.shoppingmadebetter.feature.stores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the stores example screen. */
sealed interface StoresUiState {
    data object Loading : StoresUiState
    data class Success(val stores: List<Store>) : StoresUiState
    data object Error : StoresUiState
}

/**
 * Drives [StoresScreen]: loads stores through [GetStoresUseCase] and exposes the
 * result as [StoresUiState]. Depends only on the use-case interface — the Supabase
 * details live behind it.
 */
@HiltViewModel
class StoresViewModel @Inject constructor(
    private val getStoresUseCase: GetStoresUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoresUiState>(StoresUiState.Loading)
    val uiState: StateFlow<StoresUiState> = _uiState.asStateFlow()

    init {
        loadStores()
    }

    fun loadStores() {
        _uiState.value = StoresUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val output = getStoresUseCase.execute(Unit)) {
                is GetStoresUseCase.Output.Success -> StoresUiState.Success(output.stores)
                is GetStoresUseCase.Output.Failure -> StoresUiState.Error
            }
        }
    }
}
