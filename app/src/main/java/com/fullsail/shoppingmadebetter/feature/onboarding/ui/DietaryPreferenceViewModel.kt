package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DietaryPreferenceViewModel @Inject constructor() : ViewModel() {
    private val _selectedDiets = MutableStateFlow<Set<String>>(emptySet())
    val selectedDiets: StateFlow<Set<String>> = _selectedDiets.asStateFlow()

    fun toggleDiet(diet: String) {
        _selectedDiets.value = if (_selectedDiets.value.contains(diet)) {
            _selectedDiets.value - diet
        } else {
            _selectedDiets.value + diet
        }
    }
}