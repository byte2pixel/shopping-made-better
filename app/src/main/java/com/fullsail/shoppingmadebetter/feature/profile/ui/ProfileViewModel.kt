package com.fullsail.shoppingmadebetter.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.profile.domain.ChangePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PasswordUiState {
    object Idle : PasswordUiState
    object Loading : PasswordUiState
    object Success : PasswordUiState
    data class Error(val message: String) : PasswordUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasswordUiState>(PasswordUiState.Idle)
    val uiState: StateFlow<PasswordUiState> = _uiState.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _confirmPasswordInput = MutableStateFlow("")
    val confirmPasswordInput: StateFlow<String> = _confirmPasswordInput.asStateFlow()

    fun onPasswordChanged(newValue: String) {
        _passwordInput.value = newValue
    }

    fun onConfirmPasswordChanged(newValue: String) {
        _confirmPasswordInput.value = newValue
    }

    fun executePasswordChange() {
        val password = _passwordInput.value
        val confirmPassword = _confirmPasswordInput.value

        // 1. Validate length in ViewModel (Mel's Tier 2 request)
        if (password.length < 6) {
            _uiState.value = PasswordUiState.Error("Password must be at least 6 characters long.")
            return
        }

        // 2. Validate passwords match
        if (password != confirmPassword) {
            _uiState.value = PasswordUiState.Error("Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _uiState.value = PasswordUiState.Loading

            // 3. Call execute with Input object matching the UseCase contract
            val result = changePasswordUseCase.execute(ChangePasswordUseCase.Input(password))

            when (result) {
                is ChangePasswordUseCase.Output.Success -> {
                    _uiState.value = PasswordUiState.Success
                    _passwordInput.value = ""
                    _confirmPasswordInput.value = ""
                }
                is ChangePasswordUseCase.Output.Failure -> {
                    _uiState.value = PasswordUiState.Error(
                        result.error.localizedMessage ?: "Failed to update password."
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = PasswordUiState.Idle
    }
}