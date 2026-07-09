package com.fullsail.shoppingmadebetter.feature.auth.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.auth.domain.AuthCredentials
import com.fullsail.shoppingmadebetter.feature.auth.domain.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the sign-up screen. [Error] carries a messageRes fallback plus an
 * optional detail (the auth failure's message) so the screen can show the
 * specific reason when Supabase provides one.
 */
sealed interface SignUpUiState {
    data object Idle : SignUpUiState
    data object Submitting : SignUpUiState
    data object Success : SignUpUiState
    data class Error(@param:StringRes val messageRes: Int, val detail: String? = null) : SignUpUiState
}

/**
 * Drives [SignUpScreen]: validates input (including password confirmation),
 * registers through [SignUpUseCase], and exposes the result as [SignUpUiState].
 * Depends only on the use-case interface
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, confirmPassword: String) {
        when {
            email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                _uiState.value = SignUpUiState.Error(R.string.auth_error_registration_incomplete)
                return
            }

            password != confirmPassword -> {
                _uiState.value = SignUpUiState.Error(R.string.auth_error_passwords_mismatch)
                return
            }
        }
        _uiState.value = SignUpUiState.Submitting
        viewModelScope.launch {
            _uiState.value = when (val output =
                signUpUseCase.execute(AuthCredentials(email.trim(), password))) {
                SignUpUseCase.Output.Success -> SignUpUiState.Success
                is SignUpUseCase.Output.Failure ->
                    SignUpUiState.Error(R.string.auth_error_sign_up_failed, output.error.localizedMessage)
            }
        }
    }
}
