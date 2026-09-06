package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetItemDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed interface ItemInformationState {
    data object Loading : ItemInformationState
    data class Success(val item: GetItemDetailsUseCase.Output.Success) : ItemInformationState
    data object Error : ItemInformationState
}

@HiltViewModel
class InformationViewModel @Inject constructor(
    private val getItemUseCase : GetItemDetailsUseCase
)  : ViewModel() {
    private val _uiState = MutableStateFlow<ItemInformationState>(ItemInformationState.Loading)
    val uiState: StateFlow<ItemInformationState> = _uiState.asStateFlow()
    fun getItem(input: String) {
        _uiState.value = ItemInformationState.Loading
        viewModelScope.launch {
            when (val out = getItemUseCase.execute(input)) {
                is GetItemDetailsUseCase.Output.Success -> {
                    _uiState.value = ItemInformationState.Success(out)}
                    is GetItemDetailsUseCase.Output.Failure ->{
                        _uiState.value = ItemInformationState.Error
                    }
                }
            }
        }
    }
