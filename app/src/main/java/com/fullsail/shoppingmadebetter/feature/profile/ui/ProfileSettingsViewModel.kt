package com.fullsail.shoppingmadebetter.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.profile.domain.GetAutoAdjustEnabledUseCase
import com.fullsail.shoppingmadebetter.feature.profile.domain.SetAutoAdjustEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileSettingsEvent {
    /** Saving the auto-adjust switch failed; the switch has been reverted. */
    data object AutoAdjustUpdateFailed : ProfileSettingsEvent
}

/** State for the settings rows on [ProfileScreen]. */
@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val getAutoAdjustEnabledUseCase: GetAutoAdjustEnabledUseCase,
    private val setAutoAdjustEnabledUseCase: SetAutoAdjustEnabledUseCase,
) : ViewModel() {
    /** The auto-adjust flag; `null` until loaded, or when the load failed. */
    private val _autoAdjustEnabled = MutableStateFlow<Boolean?>(null)
    val autoAdjustEnabled: StateFlow<Boolean?> = _autoAdjustEnabled.asStateFlow()

    private val _events = Channel<ProfileSettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val out = getAutoAdjustEnabledUseCase.execute(Unit)
            if (out is GetAutoAdjustEnabledUseCase.Output.Success) _autoAdjustEnabled.value = out.enabled
        }
    }

    /** Saves the switch optimistically and reverts it with an event if the write fails. */
    fun onAutoAdjustToggled(enabled: Boolean) {
        val previous = _autoAdjustEnabled.value
        _autoAdjustEnabled.value = enabled
        viewModelScope.launch {
            val out = setAutoAdjustEnabledUseCase.execute(SetAutoAdjustEnabledUseCase.Input(enabled))
            if (out is SetAutoAdjustEnabledUseCase.Output.Failure) {
                _autoAdjustEnabled.value = previous
                _events.send(ProfileSettingsEvent.AutoAdjustUpdateFailed)
            }
        }
    }
}
