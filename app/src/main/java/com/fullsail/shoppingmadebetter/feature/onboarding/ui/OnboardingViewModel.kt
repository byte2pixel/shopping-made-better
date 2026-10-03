package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.onboarding.domain.getPreferences.GetPreferencesUseCase
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
    private val savePreferencesUseCase: SavePreferencesUseCase,
    private val getPreferencesUseCase: GetPreferencesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _prefill = MutableStateFlow<SavePreferences?>(null)

    /** The saved answers an edit starts from; null until [loadForEdit] has read them. */
    val prefill: StateFlow<SavePreferences?> = _prefill.asStateFlow()

    /** Reads the saved answers once; a failed read leaves the steps blank, as on sign-up. */
    fun loadForEdit() {
        if (_prefill.value != null) return
        viewModelScope.launch {
            try {
                _prefill.value = getPreferencesUseCase()
            } catch (e: Exception) {
                Log.w(TAG, "Could not load saved preferences", e)
            }
        }
    }

    fun submitFinalPreferences(
        dietary: List<String>,
        categories: List<String>,
        goal: String,
        autoAdjust: Boolean,
    ) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Submitting
            try {
                val domainModel = SavePreferences(
                    dietaryRestrictions = dietary,
                    topCategories = categories,
                    primaryGoal = goal,
                    autoAdjustEnabled = autoAdjust
                )
                savePreferencesUseCase(domainModel)
                _uiState.value = OnboardingUiState.Success
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    private companion object {
        const val TAG = "OnboardingViewModel"
    }
}
