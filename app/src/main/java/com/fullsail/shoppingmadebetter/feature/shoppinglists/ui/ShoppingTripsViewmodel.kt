package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RemoveListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RenameList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RenameShoppingListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.SetSortOrderUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.SortListCreatedUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.SortListUpdatedUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.SortOrder
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
    private val getRemoveListUseCase: RemoveListUseCase,
    private val getRenameListUseCase: RenameShoppingListUseCase,
    private val getSortListCreatedUseCase: SortListCreatedUseCase,
    private val getSortListUpdatedUseCase: SortListUpdatedUseCase,
    private val setSortOrderUseCase : SetSortOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingTripsUiState>(ShoppingTripsUiState.Loading)
    val uiState: StateFlow<ShoppingTripsUiState> = _uiState.asStateFlow()

    init { load() }
    fun sortListsByCreated()
    {
        viewModelScope.launch {
            when(getSortListCreatedUseCase.execute(Unit))
            {
               is SortListCreatedUseCase.Output.Success -> {
                   load()
                }

              is SortListCreatedUseCase.Output.Failure ->
                {

                }
            }
        }
    }
    fun setSortOrder(s : SortOrder)
    {
        viewModelScope.launch {
            when( setSortOrderUseCase.execute(s))
            {

                is SetSortOrderUseCase.Output.Success -> {
                    load()
                }
                is SetSortOrderUseCase.Output.Failure ->
                {

                }
            }
        }
    }
    fun sortListsByUpdated()
    {
        viewModelScope.launch {
            when(getSortListUpdatedUseCase.execute(Unit))
            {
                is SortListUpdatedUseCase.Output.Success -> {
                    load()
                }

                is SortListUpdatedUseCase.Output.Failure ->
                {

                }
            }
        }
    }
    fun removeList (listName : String)
    {
        _uiState.value = ShoppingTripsUiState.Loading
        viewModelScope.launch {
             when (getRemoveListUseCase.execute(listName)) {
                is RemoveListUseCase.Output.Success ->{
                    load()
                }
                is RemoveListUseCase.Output.Failure ->
                {
                    ShoppingTripsUiState.Error
                }

            }

        }

    }

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

    fun renameList(rename : RenameList)
    {
        _uiState.value = ShoppingTripsUiState.Loading
        viewModelScope.launch {
            when (getRenameListUseCase.execute(rename) ){
                is RenameShoppingListUseCase.Output.Success ->
                {
                    load()

                }

                is RenameShoppingListUseCase.Output.Failure ->
                {
                    ShoppingTripsUiState.Error
                }
            }
        }
    }

}
