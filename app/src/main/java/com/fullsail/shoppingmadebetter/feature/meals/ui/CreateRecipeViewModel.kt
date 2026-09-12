package com.fullsail.shoppingmadebetter.feature.meals.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.meals.domain.SaveCustomRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRecipeViewModel @Inject constructor(
    private val saveCustomRecipe: SaveCustomRecipeUseCase
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _category = MutableStateFlow("Recommended")
    val category = _category.asStateFlow()

    private val _ingredients = MutableStateFlow("")
    val ingredients = _ingredients.asStateFlow()

    fun updateTitle(newTitle: String) { _title.value = newTitle }
    fun updateCategory(newCategory: String) { _category.value = newCategory }
    fun updateIngredients(newIngredients: String) { _ingredients.value = newIngredients }

    fun saveRecipe() {
        viewModelScope.launch {
            saveCustomRecipe(
                title = _title.value,
                category = _category.value,
                ingredients = _ingredients.value
            )

            _title.value = ""
            _ingredients.value = ""
        }
    }
}