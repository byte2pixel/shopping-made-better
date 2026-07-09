package com.fullsail.shoppingmadebetter.feature.auth.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.auth.domain.AuthCredentials
import com.fullsail.shoppingmadebetter.feature.auth.domain.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the sign-in screen. [Error] carries a messageRes fallback plus an
 * optional detail (the auth failure's message) so the screen can show the
 * specific reason when Supabase provides one.
 */
sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object Submitting : SignInUiState
    data object Success : SignInUiState
    data class Error(@param:StringRes val messageRes: Int, val detail: String? = null) : SignInUiState
}

/**
 * Drives [LoginScreen]: validates input, signs in through [SignInUseCase], and
 * exposes the result as [SignInUiState]. Depends only on the use-case interface
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = SignInUiState.Error(R.string.auth_error_fields_blank)
            return
        }
        _uiState.value = SignInUiState.Submitting
        viewModelScope.launch {
            _uiState.value = when (val output =
                signInUseCase.execute(AuthCredentials(email.trim(), password))) {
                SignInUseCase.Output.Success -> SignInUiState.Success
                is SignInUseCase.Output.Failure ->
                    SignInUiState.Error(R.string.auth_error_sign_in_failed, output.error.localizedMessage)
            }
        }
    }
}
