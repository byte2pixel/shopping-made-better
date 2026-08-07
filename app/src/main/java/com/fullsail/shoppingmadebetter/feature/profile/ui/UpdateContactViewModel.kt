package com.fullsail.shoppingmadebetter.feature.profile.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.profile.domain.UpdateProfileInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ContactUiState {
    object Idle : ContactUiState
    object Loading : ContactUiState
    object Success : ContactUiState
    data class Error(val message: String) : ContactUiState
}

@HiltViewModel
class UpdateContactViewModel @Inject constructor(
    private val updateProfileInfoUseCase: UpdateProfileInfoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactUiState>(ContactUiState.Idle)
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    private val _emailInput = MutableStateFlow("")
    val emailInput: StateFlow<String> = _emailInput.asStateFlow()

    private val _phoneInput = MutableStateFlow("")
    val phoneInput: StateFlow<String> = _phoneInput.asStateFlow()

    fun onEmailChanged(newValue: String) { _emailInput.value = newValue }
    fun onPhoneChanged(newValue: String) { _phoneInput.value = newValue }

    fun executeContactUpdate() {
        val email = _emailInput.value.trim()
        val phone = _phoneInput.value.trim()

        if (email.isEmpty() && phone.isEmpty()) {
            _uiState.value = ContactUiState.Error("Please enter a new email or phone number.")
            return
        }

        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = ContactUiState.Error("Please enter a valid email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ContactUiState.Loading
            val result = updateProfileInfoUseCase.execute(
                UpdateProfileInfoUseCase.Input(
                    newEmail = email.ifEmpty { null },
                    newPhone = phone.ifEmpty { null }
                )
            )

            when (result) {
                is UpdateProfileInfoUseCase.Output.Success -> {
                    _uiState.value = ContactUiState.Success
                    _emailInput.value = ""
                    _phoneInput.value = ""
                }
                is UpdateProfileInfoUseCase.Output.Failure -> {
                    _uiState.value = ContactUiState.Error(
                        result.error.localizedMessage ?: "Failed to update contact info."
                    )
                }
            }
        }
    }
}