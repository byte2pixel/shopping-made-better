package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// @HiltViewModel
class CategoryPreferenceViewModel /* @Inject constructor() */ : ViewModel() {
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    fun toggleCategory(category: String) {
        _selectedCategories.value = if (_selectedCategories.value.contains(category)) {
            _selectedCategories.value - category
        } else {
            _selectedCategories.value + category
        }
    }
}