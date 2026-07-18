package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class GoalPreferenceViewModel @Inject constructor() : ViewModel() {
    private val _selectedGoal = MutableStateFlow("")
    val selectedGoal: StateFlow<String> = _selectedGoal.asStateFlow()

    fun selectGoal(goal: String) {
        _selectedGoal.value = goal
    }
}