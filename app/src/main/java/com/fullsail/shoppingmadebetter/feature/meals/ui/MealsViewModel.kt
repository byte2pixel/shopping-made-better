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
                        matchPercentage = dto.matchPercentage,
                        itemCount = dto.itemCount,
                        totalPrice = dto.totalPrice,
                        category = dto.category,
                        ingredients = listOf(
                            Ingredient("1", "Chicken breast", "1 Lbs", "$3.19"),
                            Ingredient("2", "Fettuccine Pasta", "16 oz", "$2.59"),
                            Ingredient("3", "Butter", "1 Lbs", "$4.32"),
                            Ingredient("4", "Parmesan cheese", "8 oz", "$4.50")
                        )
                    )
                }

                _uiState.value = MealsUiState.Success(
                    meals = allMeals,
                    selectedFilter = "All",
                    canMakeCount = 12,
                    almostThereCount = 3,
                    expiringCount = 5,
                    recommendedCount = 5
                )
            } catch (e: Exception) {
                _uiState.value = MealsUiState.Error(e.localizedMessage ?: "Failed to load meals.")
            }
        }
    }

    fun onFilterSelected(filter: String) {
        val currentState = _uiState.value
        if (currentState is MealsUiState.Success) {
            val filteredList = if (filter == "All") {
                allMeals
            } else {
                allMeals.filter { it.category.equals(filter, ignoreCase = true) }
            }
            _uiState.value = currentState.copy(
                meals = filteredList,
                selectedFilter = filter
            )
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
}