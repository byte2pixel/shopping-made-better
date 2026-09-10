package com.fullsail.shoppingmadebetter.feature.household.ui

import com.fullsail.shoppingmadebetter.feature.household.domain.CreateHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.GetHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.Household
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdMember
import com.fullsail.shoppingmadebetter.feature.household.domain.JoinHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.LeaveHouseholdUseCase
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class HouseholdViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val household = Household(id = "h1", name = "Demo Household", inviteCode = "DEMO2026")
    private val head = HouseholdMember(id = "u1", displayName = "Demo Shopper", isHead = true, isSelf = false)
    private val self = HouseholdMember(id = "u2", displayName = "Demo Roommate", isHead = false, isSelf = true)
    private val memberOutput = GetHouseholdUseCase.Output.Member(household, listOf(head, self))

    /** Returns [outputs] in order and repeats the last one. */
    private class FakeGetHouseholdUseCase(vararg outputs: GetHouseholdUseCase.Output) : GetHouseholdUseCase {
        private val queue = outputs.toMutableList()
        var calls = 0
        override suspend fun execute(input: Unit): GetHouseholdUseCase.Output {
            calls++
            return if (queue.size > 1) queue.removeAt(0) else queue.first()
        }
    }

    private class FakeCreateHouseholdUseCase(
        private val output: CreateHouseholdUseCase.Output,
    ) : CreateHouseholdUseCase {
        var lastInput: String? = null
        override suspend fun execute(input: String): CreateHouseholdUseCase.Output {
            lastInput = input
            return output
        }
    }

    private class FakeJoinHouseholdUseCase(
        private val output: JoinHouseholdUseCase.Output,
    ) : JoinHouseholdUseCase {
        var lastInput: String? = null
        override suspend fun execute(input: String): JoinHouseholdUseCase.Output {
            lastInput = input
            return output
        }
    }

    private class FakeLeaveHouseholdUseCase(
        private val output: LeaveHouseholdUseCase.Output,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : LeaveHouseholdUseCase {
        var calls = 0
        override suspend fun execute(input: Unit): LeaveHouseholdUseCase.Output {
            calls++
            gate?.await()
            return output
        }
    }

    private fun viewModel(
        get: GetHouseholdUseCase = FakeGetHouseholdUseCase(GetHouseholdUseCase.Output.None),
        create: CreateHouseholdUseCase = FakeCreateHouseholdUseCase(CreateHouseholdUseCase.Output.Success(household)),
        join: JoinHouseholdUseCase = FakeJoinHouseholdUseCase(JoinHouseholdUseCase.Output.Success(household)),
        leave: LeaveHouseholdUseCase = FakeLeaveHouseholdUseCase(LeaveHouseholdUseCase.Output.Success),
    ) = HouseholdViewModel(get, create, join, leave)

    @Test
    fun `starts Loading until load is called`() = runTest {
        val viewModel = viewModel()

        assertEquals(HouseholdUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `load shows None when the caller has no household`() = runTest {
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(HouseholdUiState.None, viewModel.uiState.value)
    }

    @Test
    fun `load shows Member with the household and its members`() = runTest {
        val viewModel = viewModel(get = FakeGetHouseholdUseCase(memberOutput))

        viewModel.load()

        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
    }

    @Test
    fun `load shows Error when the lookup fails`() = runTest {
        val viewModel = viewModel(
            get = FakeGetHouseholdUseCase(GetHouseholdUseCase.Output.Failure(IOException("boom"))),
        )

        viewModel.load()

        assertEquals(HouseholdUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `onCreate reloads into Member on success`() = runTest {
        val get = FakeGetHouseholdUseCase(GetHouseholdUseCase.Output.None, memberOutput)
        val create = FakeCreateHouseholdUseCase(CreateHouseholdUseCase.Output.Success(household))
        val viewModel = viewModel(get = get, create = create)
        viewModel.load()

        viewModel.onCreate("Demo Household")

        assertEquals("Demo Household", create.lastInput)
        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onCreate emits CreateFailed and stays on None`() = runTest {
        val viewModel = viewModel(
            create = FakeCreateHouseholdUseCase(CreateHouseholdUseCase.Output.Failure(IOException("boom"))),
        )
        viewModel.load()

        viewModel.onCreate("Demo Household")

        assertEquals(HouseholdEvent.CreateFailed, viewModel.events.first())
        assertEquals(HouseholdUiState.None, viewModel.uiState.value)
    }

    @Test
    fun `onJoin with a bad code emits JoinInvalidCode and stays on None`() = runTest {
        val get = FakeGetHouseholdUseCase(GetHouseholdUseCase.Output.None)
        val join = FakeJoinHouseholdUseCase(JoinHouseholdUseCase.Output.InvalidCode)
        val viewModel = viewModel(get = get, join = join)
        viewModel.load()

        viewModel.onJoin("NOPE")

        assertEquals("NOPE", join.lastInput)
        assertEquals(HouseholdEvent.JoinInvalidCode, viewModel.events.first())
        assertEquals(HouseholdUiState.None, viewModel.uiState.value)
        assertEquals(1, get.calls)
    }

    @Test
    fun `onJoin reloads into Member on success`() = runTest {
        val get = FakeGetHouseholdUseCase(GetHouseholdUseCase.Output.None, memberOutput)
        val viewModel = viewModel(get = get)
        viewModel.load()

        viewModel.onJoin("demo2026")

        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
    }

    @Test
    fun `onJoin when already a member emits AlreadyInHousehold and reloads`() = runTest {
        val get = FakeGetHouseholdUseCase(GetHouseholdUseCase.Output.None, memberOutput)
        val viewModel = viewModel(
            get = get,
            join = FakeJoinHouseholdUseCase(JoinHouseholdUseCase.Output.AlreadyInHousehold),
        )
        viewModel.load()

        viewModel.onJoin("DEMO2026")

        assertEquals(HouseholdEvent.AlreadyInHousehold, viewModel.events.first())
        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
    }

    @Test
    fun `onLeave as head with members emits LeaveRefusedHead and stays on Member`() = runTest {
        val viewModel = viewModel(
            get = FakeGetHouseholdUseCase(memberOutput),
            leave = FakeLeaveHouseholdUseCase(LeaveHouseholdUseCase.Output.HeadWithMembers),
        )
        viewModel.load()

        viewModel.onLeave()

        assertEquals(HouseholdEvent.LeaveRefusedHead, viewModel.events.first())
        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
    }

    @Test
    fun `onLeave reloads into None on success`() = runTest {
        val get = FakeGetHouseholdUseCase(memberOutput, GetHouseholdUseCase.Output.None)
        val viewModel = viewModel(get = get)
        viewModel.load()

        viewModel.onLeave()

        assertEquals(HouseholdUiState.None, viewModel.uiState.value)
    }

    @Test
    fun `busy is set for the length of a call and a second tap is dropped`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val leave = FakeLeaveHouseholdUseCase(LeaveHouseholdUseCase.Output.Success, gate)
        val viewModel = viewModel(get = FakeGetHouseholdUseCase(memberOutput, GetHouseholdUseCase.Output.None), leave = leave)
        viewModel.load()

        viewModel.onLeave()
        assertTrue(viewModel.busy.value)
        viewModel.onLeave()
        assertEquals(1, leave.calls)

        gate.complete(Unit)

        assertFalse(viewModel.busy.value)
        assertEquals(HouseholdUiState.None, viewModel.uiState.value)
    }
}
