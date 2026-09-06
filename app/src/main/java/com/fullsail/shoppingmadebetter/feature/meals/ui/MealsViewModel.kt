package com.fullsail.shoppingmadebetter.feature.meals.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepository
import com.fullsail.shoppingmadebetter.feature.meals.domain.Ingredient
import com.fullsail.shoppingmadebetter.feature.meals.domain.Meal
import com.fullsail.shoppingmadebetter.feature.meals.domain.SelectMealUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MealsUiState {
    object Loading : MealsUiState
    data class Success(
        val meals: List<Meal>,
        val selectedFilter: String,
        val searchQuery: String = "",
        val canMakeCount: Int,
        val almostThereCount: Int,
        val expiringCount: Int,
        val recommendedCount: Int,
        val selectedMealId: String? = null
    ) : MealsUiState
    data class Error(val message: String) : MealsUiState
}

@HiltViewModel
class MealsViewModel @Inject constructor(
    private val repository: MealsRepository,
    private val selectMealUseCase: SelectMealUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MealsUiState>(MealsUiState.Loading)
    val uiState: StateFlow<MealsUiState> = _uiState.asStateFlow()

    private var allMeals: List<Meal> = emptyList()

    init {
        loadMeals()
    }

    fun loadMeals() {
        viewModelScope.launch {
            _uiState.value = MealsUiState.Loading
            try {
                val dtos = repository.fetchMeals()
                allMeals = dtos.map { dto ->
                    Meal(
                        id = dto.id,
                        title = dto.title,
                        matchPercentage = dto.matchPercentage.filter { it.isDigit() }.toIntOrNull() ?: 0,
                        itemCount = dto.itemCount,
                        totalPrice = dto.totalPrice,
                        category = dto.category,
                        ingredients = listOf(
                            Ingredient("1", "Chicken breast", "1 Lbs", "$3.19"),
                            Ingredient("2", "Fettuccine Pasta", "16 oz", "$2.59"),
                            Ingredient("3", "Butter", "1 Lbs", "$4.32"),
                            Ingredient("4", "Parmesan cheese", "8 oz", "$4.50")
                        ),
                        instructions = listOf(
                            "Boil water in a large pot.",
                            "Add fettuccine and cook for 10 minutes.",
                            "In a separate pan, melt butter and add sliced chicken breast.",
                            "Cook chicken until golden brown, then add heavy cream and parmesan.",
                            "Toss pasta in the sauce and serve hot."
                        )
                    )
                }

                val canMake = allMeals.count { it.category.equals("Can Make", ignoreCase = true) }
                val almostThere = allMeals.count { it.category.equals("Almost There", ignoreCase = true) }
                val expiring = allMeals.count { it.category.equals("Expiring", ignoreCase = true) }
                val recommended = allMeals.count { it.category.equals("Recommended", ignoreCase = true) }

                _uiState.value = MealsUiState.Success(
                    meals = allMeals,
                    selectedFilter = "All",
                    searchQuery = "",
                    canMakeCount = canMake,
                    almostThereCount = almostThere,
                    expiringCount = expiring,
                    recommendedCount = recommended
                )
            } catch (e: Exception) {
                _uiState.value = MealsUiState.Error(e.localizedMessage ?: "Failed to load meals.")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is MealsUiState.Success) {
            applyFilters(currentState.selectedFilter, query)
        }
    }

    fun onFilterSelected(filter: String) {
        val currentState = _uiState.value
        if (currentState is MealsUiState.Success) {
            applyFilters(filter, currentState.searchQuery)
        }
    }

    private fun applyFilters(filter: String, query: String) {
        val currentState = _uiState.value
        if (currentState is MealsUiState.Success) {
            val filteredList = allMeals.filter { meal ->
                val matchesCategory = if (filter == "All") true else meal.category.equals(filter, ignoreCase = true)
                val matchesSearch = query.isEmpty() ||
                        meal.title.contains(query, ignoreCase = true) ||
                        meal.ingredients.any { it.name.contains(query, ignoreCase = true) }
                matchesCategory && matchesSearch
            }

            _uiState.value = currentState.copy(
                meals = filteredList,
                selectedFilter = filter,
                searchQuery = query
            )
        }
    }

    fun quickAddIngredient(ingredientId: String, title: String) {
        val currentState = _uiState.value
        if (currentState is MealsUiState.Success) {
            viewModelScope.launch {
                try {
                    val activeListId = "DEFAULT_LIST_ID"
                    repository.addIngredientToShoppingList(ingredientId, title, activeListId)
                } catch (e: Exception) {
                    println("Error in quickAddIngredient: ${e.message}")
                }
            }
        }
    }

    fun addMealToList(mealId: String) {
        val currentState = _uiState.value
        if (currentState is MealsUiState.Success) {
            viewModelScope.launch {
                try {
                    val activeListId = "DEFAULT_LIST_ID"
                    repository.addMealToShoppingList(mealId, activeListId)
                } catch (e: Exception) {
                    println("Error in addMealToList: ${e.message}")
                }
            }
        }
    }

    fun selectMealPlan(mealId: String) {
        val currentState = _uiState.value
        if (currentState is MealsUiState.Success) {
            viewModelScope.launch {
                val result = selectMealUseCase.execute(SelectMealUseCase.Input(mealId))
                if (result is SelectMealUseCase.Output.Success) {
                    _uiState.value = currentState.copy(selectedMealId = mealId)
                }
            }
        }
    }

    fun toggleFavorite(mealId: String, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavoriteMeal(mealId, !isCurrentlyFavorite)
            } catch (e: Exception) {
                println("Error in toggleFavorite: ${e.message}")
            }
        }
    }
}