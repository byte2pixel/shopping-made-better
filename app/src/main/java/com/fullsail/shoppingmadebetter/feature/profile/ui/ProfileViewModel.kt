package com.fullsail.shoppingmadebetter.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.profile.domain.ChangePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

        if (password != confirmPassword) {
            _uiState.value = PasswordUiState.Error("Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _uiState.value = PasswordUiState.Loading
            changePasswordUseCase(newPassword = password)
                .onSuccess {
                    _uiState.value = PasswordUiState.Success
                    _passwordInput.value = ""
                    _confirmPasswordInput.value = ""
                }
                .onFailure { error ->
                    _uiState.value = PasswordUiState.Error(error.localizedMessage ?: "Failed to update password.")
                }
        }
    }

    fun resetState() {
        _uiState.value = PasswordUiState.Idle
    }
}