package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferences
import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OnboardingUiState {
    object Idle : OnboardingUiState
    object Submitting : OnboardingUiState
    object Success : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savePreferencesUseCase: SavePreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun submitFinalPreferences(dietary: List<String>, categories: List<String>, goal: String) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Submitting
            try {
                val domainModel = SavePreferences(
                    dietaryRestrictions = dietary,
                    topCategories = categories,
                    primaryGoal = goal
                )
                savePreferencesUseCase(domainModel)
                _uiState.value = OnboardingUiState.Success
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
}