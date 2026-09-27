package com.fullsail.shoppingmadebetter.feature.meals.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.meals.domain.SaveCustomRecipeUseCase
import com.fullsail.shoppingmadebetter.feature.meals.domain.UpdateCustomRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateRecipeUiState(
    val title: String = "",
    val category: String = "Breakfast",
    val ingredients: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CreateRecipeViewModel @Inject constructor(
    private val saveCustomRecipeUseCase: SaveCustomRecipeUseCase,
    private val updateCustomRecipeUseCase: UpdateCustomRecipeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mealId: String? = savedStateHandle.get<String>("mealId")
    val isEditMode: Boolean = mealId != null

    private val _uiState = MutableStateFlow(CreateRecipeUiState())
    val uiState: StateFlow<CreateRecipeUiState> = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, error = null) }
    }

    fun onCategoryChange(newCategory: String) {
        _uiState.update { it.copy(category = newCategory) }
    }

    fun onIngredientsChange(newIngredients: String) {
        _uiState.update { it.copy(ingredients = newIngredients, error = null) }
    }

    fun saveOrUpdateRecipe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (isEditMode && mealId != null) {
                updateCustomRecipeUseCase(
                    mealId = mealId,
                    title = _uiState.value.title,
                    category = _uiState.value.category,
                    ingredients = _uiState.value.ingredients
                )
            } else {
                saveCustomRecipeUseCase(
                    title = _uiState.value.title,
                    category = _uiState.value.category,
                    ingredients = _uiState.value.ingredients
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
            )
        }
    }
}