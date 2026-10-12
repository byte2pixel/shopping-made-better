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

//State outcomes of the Shopping Trip ViewModel
sealed interface ShoppingTripsUiState {
    data object Loading : ShoppingTripsUiState
    data class Success(val trips: List<ShoppingTrip>) : ShoppingTripsUiState
    data object Error : ShoppingTripsUiState
}

//
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
    //Sorts the list by the date they were created
    fun sortListsByCreated()
    {
        viewModelScope.launch {
            when(getSortListCreatedUseCase.execute(Unit))
            {
               is SortListCreatedUseCase.Output.Success -> {
                   //Refreshes the lists
                   load()
                }

              is SortListCreatedUseCase.Output.Failure ->
                {

                }
            }
        }
    }
    //Swaps the sort order of two different lists
    //Sort order is listId, newSortOrderValue, listId2, newSortOrderValue2
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
    //sorts the list by the date they were last updated
    fun sortListsByUpdated()
    {
        viewModelScope.launch {
            when(getSortListUpdatedUseCase.execute(Unit))
            {
                is SortListUpdatedUseCase.Output.Success -> {
                    //Refreshes the lists
                    load()
                }

                is SortListUpdatedUseCase.Output.Failure ->
                {

                }
            }
        }
    }
    //Used to delete a list by the lists Id
    fun removeList (listId : String)
    {
        _uiState.value = ShoppingTripsUiState.Loading
        viewModelScope.launch {
             when (getRemoveListUseCase.execute(listId)) {
                is RemoveListUseCase.Output.Success ->{
                    //Refreshes the lists
                    load()
                }
                is RemoveListUseCase.Output.Failure ->
                {
                    ShoppingTripsUiState.Error
                }

            }

        }

    }
//Refreshes the users lists
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
    //Renames a given list
    // Takes Rename list : (listId, newName)

    fun renameList(rename : RenameList)
    {
        _uiState.value = ShoppingTripsUiState.Loading
        viewModelScope.launch {
            when (getRenameListUseCase.execute(rename) ){
                is RenameShoppingListUseCase.Output.Success ->
                {
                    //refreshes the lists
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
