package com.fullsail.shoppingmadebetter.feature.household.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.household.domain.CreateHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.GetHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.Household
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdMember
import com.fullsail.shoppingmadebetter.feature.household.domain.JoinHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.LeaveHouseholdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HouseholdUiState {
    data object Loading : HouseholdUiState
    data object None : HouseholdUiState
    data class Member(val household: Household, val members: List<HouseholdMember>) : HouseholdUiState
    data object Error : HouseholdUiState
}

sealed interface HouseholdEvent {
    data object CreateFailed : HouseholdEvent
    data object JoinInvalidCode : HouseholdEvent
    data object AlreadyInHousehold : HouseholdEvent
    data object JoinFailed : HouseholdEvent
    data object LeaveRefusedHead : HouseholdEvent
    data object LeaveFailed : HouseholdEvent
}

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val getHouseholdUseCase: GetHouseholdUseCase,
    private val createHouseholdUseCase: CreateHouseholdUseCase,
    private val joinHouseholdUseCase: JoinHouseholdUseCase,
    private val leaveHouseholdUseCase: LeaveHouseholdUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HouseholdUiState>(HouseholdUiState.Loading)
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    private val _events = Channel<HouseholdEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** True while an RPC is in flight; the screen disables its buttons. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun load() {
        viewModelScope.launch { refresh() }
    }

    fun onCreate(name: String) = whileBusy {
        when (createHouseholdUseCase.execute(name)) {
            is CreateHouseholdUseCase.Output.Success -> refresh()
            CreateHouseholdUseCase.Output.AlreadyInHousehold -> {
                _events.send(HouseholdEvent.AlreadyInHousehold)
                refresh()
            }
            is CreateHouseholdUseCase.Output.Failure -> _events.send(HouseholdEvent.CreateFailed)
        }
    }

    fun onJoin(code: String) = whileBusy {
        when (joinHouseholdUseCase.execute(code)) {
            is JoinHouseholdUseCase.Output.Success -> refresh()
            JoinHouseholdUseCase.Output.InvalidCode -> _events.send(HouseholdEvent.JoinInvalidCode)
            JoinHouseholdUseCase.Output.AlreadyInHousehold -> {
                _events.send(HouseholdEvent.AlreadyInHousehold)
                refresh()
            }
            is JoinHouseholdUseCase.Output.Failure -> _events.send(HouseholdEvent.JoinFailed)
        }
    }

    fun onLeave() = whileBusy {
        when (leaveHouseholdUseCase.execute(Unit)) {
            LeaveHouseholdUseCase.Output.Success -> refresh()
            LeaveHouseholdUseCase.Output.HeadWithMembers -> _events.send(HouseholdEvent.LeaveRefusedHead)
            is LeaveHouseholdUseCase.Output.Failure -> _events.send(HouseholdEvent.LeaveFailed)
        }
    }

    private suspend fun refresh() {
        _uiState.value = HouseholdUiState.Loading
        _uiState.value = when (val out = getHouseholdUseCase.execute(Unit)) {
            GetHouseholdUseCase.Output.None -> HouseholdUiState.None
            is GetHouseholdUseCase.Output.Member -> HouseholdUiState.Member(out.household, out.members)
            is GetHouseholdUseCase.Output.Failure -> HouseholdUiState.Error
        }
    }

    /** Runs one RPC at a time; a tap while busy is dropped. */
    private fun whileBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}
