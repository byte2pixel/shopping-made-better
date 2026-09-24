package com.fullsail.shoppingmadebetter.feature.household.ui

import com.fullsail.shoppingmadebetter.feature.household.domain.CreateHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.GetHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.Household
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdMember
import com.fullsail.shoppingmadebetter.feature.household.domain.JoinHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.LeaveHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.RegenerateInviteCodeUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.RemoveMemberUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.RenameHousehold
import com.fullsail.shoppingmadebetter.feature.household.domain.RenameHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.TransferHeadUseCase
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

    private class FakeRenameHouseholdUseCase(
        private val output: RenameHouseholdUseCase.Output,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : RenameHouseholdUseCase {
        val inputs = mutableListOf<RenameHousehold>()
        override suspend fun execute(input: RenameHousehold): RenameHouseholdUseCase.Output {
            inputs += input
            gate?.await()
            return output
        }
    }

    private class FakeTransferHeadUseCase(
        private val output: TransferHeadUseCase.Output,
    ) : TransferHeadUseCase {
        var lastInput: String? = null
        override suspend fun execute(input: String): TransferHeadUseCase.Output {
            lastInput = input
            return output
        }
    }

    private class FakeRemoveMemberUseCase(
        private val output: RemoveMemberUseCase.Output,
    ) : RemoveMemberUseCase {
        var lastInput: String? = null
        override suspend fun execute(input: String): RemoveMemberUseCase.Output {
            lastInput = input
            return output
        }
    }

    private class FakeRegenerateInviteCodeUseCase(
        private val output: RegenerateInviteCodeUseCase.Output,
    ) : RegenerateInviteCodeUseCase {
        var calls = 0
        override suspend fun execute(input: Unit): RegenerateInviteCodeUseCase.Output {
            calls++
            return output
        }
    }

    private fun viewModel(
        get: GetHouseholdUseCase = FakeGetHouseholdUseCase(GetHouseholdUseCase.Output.None),
        create: CreateHouseholdUseCase = FakeCreateHouseholdUseCase(CreateHouseholdUseCase.Output.Success(household)),
        join: JoinHouseholdUseCase = FakeJoinHouseholdUseCase(JoinHouseholdUseCase.Output.Success(household)),
        leave: LeaveHouseholdUseCase = FakeLeaveHouseholdUseCase(LeaveHouseholdUseCase.Output.Success),
        rename: RenameHouseholdUseCase = FakeRenameHouseholdUseCase(RenameHouseholdUseCase.Output.Success),
        transfer: TransferHeadUseCase = FakeTransferHeadUseCase(TransferHeadUseCase.Output.Success),
        remove: RemoveMemberUseCase = FakeRemoveMemberUseCase(RemoveMemberUseCase.Output.Success),
        regenerate: RegenerateInviteCodeUseCase =
            FakeRegenerateInviteCodeUseCase(RegenerateInviteCodeUseCase.Output.Success("A1B2C3D4")),
    ) = HouseholdViewModel(get, create, join, leave, rename, transfer, remove, regenerate)

    /** The caller as head with one other member, the shape every management action starts from. */
    private val selfAsHead = self.copy(isHead = true)
    private val otherMember = head.copy(isHead = false)
    private val headOutput = GetHouseholdUseCase.Output.Member(household, listOf(selfAsHead, otherMember))

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

    @Test
    fun `onRename sends the household id with the name and reloads`() = runTest {
        val renamed = household.copy(name = "Dommer House")
        val get = FakeGetHouseholdUseCase(headOutput, GetHouseholdUseCase.Output.Member(renamed, listOf(selfAsHead, otherMember)))
        val rename = FakeRenameHouseholdUseCase(RenameHouseholdUseCase.Output.Success)
        val viewModel = viewModel(get = get, rename = rename)
        viewModel.load()

        viewModel.onRename("Dommer House")

        assertEquals(listOf(RenameHousehold("h1", "Dommer House")), rename.inputs)
        assertEquals(renamed, (viewModel.uiState.value as HouseholdUiState.Member).household)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onRename emits RenameFailed and keeps the old name`() = runTest {
        val get = FakeGetHouseholdUseCase(headOutput)
        val viewModel = viewModel(
            get = get,
            rename = FakeRenameHouseholdUseCase(RenameHouseholdUseCase.Output.Failure(IOException("boom"))),
        )
        viewModel.load()

        viewModel.onRename("Dommer House")

        assertEquals(HouseholdEvent.RenameFailed, viewModel.events.first())
        assertEquals(HouseholdUiState.Member(household, listOf(selfAsHead, otherMember)), viewModel.uiState.value)
        assertEquals(1, get.calls)
    }

    @Test
    fun `onRename without a household is a no-op`() = runTest {
        val rename = FakeRenameHouseholdUseCase(RenameHouseholdUseCase.Output.Success)
        val viewModel = viewModel(rename = rename)
        viewModel.load()

        viewModel.onRename("Dommer House")

        assertTrue(rename.inputs.isEmpty())
        assertEquals(HouseholdUiState.None, viewModel.uiState.value)
    }

    @Test
    fun `onTransferHead reloads with the new head on success`() = runTest {
        val after = GetHouseholdUseCase.Output.Member(household, listOf(head, self))
        val get = FakeGetHouseholdUseCase(headOutput, after)
        val transfer = FakeTransferHeadUseCase(TransferHeadUseCase.Output.Success)
        val viewModel = viewModel(get = get, transfer = transfer)
        viewModel.load()

        viewModel.onTransferHead("u1")

        assertEquals("u1", transfer.lastInput)
        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onTransferHead when no longer head emits NotHead and reloads`() = runTest {
        val get = FakeGetHouseholdUseCase(headOutput, memberOutput)
        val viewModel = viewModel(get = get, transfer = FakeTransferHeadUseCase(TransferHeadUseCase.Output.NotHead))
        viewModel.load()

        viewModel.onTransferHead("u1")

        assertEquals(HouseholdEvent.NotHead, viewModel.events.first())
        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onTransferHead to someone who left reloads without an event`() = runTest {
        val alone = GetHouseholdUseCase.Output.Member(household, listOf(selfAsHead))
        val get = FakeGetHouseholdUseCase(headOutput, alone)
        val viewModel = viewModel(
            get = get,
            transfer = FakeTransferHeadUseCase(TransferHeadUseCase.Output.MemberNotInHousehold),
        )
        viewModel.load()

        viewModel.onTransferHead("u1")

        assertEquals(HouseholdUiState.Member(household, listOf(selfAsHead)), viewModel.uiState.value)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onTransferHead emits TransferFailed and does not reload`() = runTest {
        val get = FakeGetHouseholdUseCase(headOutput)
        val viewModel = viewModel(
            get = get,
            transfer = FakeTransferHeadUseCase(TransferHeadUseCase.Output.Failure(IOException("boom"))),
        )
        viewModel.load()

        viewModel.onTransferHead("u1")

        assertEquals(HouseholdEvent.TransferFailed, viewModel.events.first())
        assertEquals(1, get.calls)
    }

    @Test
    fun `onRemoveMember reloads without the member on success`() = runTest {
        val alone = GetHouseholdUseCase.Output.Member(household, listOf(selfAsHead))
        val get = FakeGetHouseholdUseCase(headOutput, alone)
        val remove = FakeRemoveMemberUseCase(RemoveMemberUseCase.Output.Success)
        val viewModel = viewModel(get = get, remove = remove)
        viewModel.load()

        viewModel.onRemoveMember("u1")

        assertEquals("u1", remove.lastInput)
        assertEquals(HouseholdUiState.Member(household, listOf(selfAsHead)), viewModel.uiState.value)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onRemoveMember when no longer head emits NotHead and reloads`() = runTest {
        val get = FakeGetHouseholdUseCase(headOutput, memberOutput)
        val viewModel = viewModel(get = get, remove = FakeRemoveMemberUseCase(RemoveMemberUseCase.Output.NotHead))
        viewModel.load()

        viewModel.onRemoveMember("u1")

        assertEquals(HouseholdEvent.NotHead, viewModel.events.first())
        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
    }

    @Test
    fun `onRemoveMember of someone who already left reloads without an event`() = runTest {
        val alone = GetHouseholdUseCase.Output.Member(household, listOf(selfAsHead))
        val get = FakeGetHouseholdUseCase(headOutput, alone)
        val viewModel = viewModel(
            get = get,
            remove = FakeRemoveMemberUseCase(RemoveMemberUseCase.Output.MemberNotInHousehold),
        )
        viewModel.load()

        viewModel.onRemoveMember("u1")

        assertEquals(HouseholdUiState.Member(household, listOf(selfAsHead)), viewModel.uiState.value)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onRemoveMember treats CannotRemoveSelf as RemoveFailed`() = runTest {
        val get = FakeGetHouseholdUseCase(headOutput)
        val viewModel = viewModel(
            get = get,
            remove = FakeRemoveMemberUseCase(RemoveMemberUseCase.Output.CannotRemoveSelf),
        )
        viewModel.load()

        viewModel.onRemoveMember("u2")

        assertEquals(HouseholdEvent.RemoveFailed, viewModel.events.first())
        assertEquals(1, get.calls)
    }

    @Test
    fun `onRemoveMember emits RemoveFailed on any other failure`() = runTest {
        val viewModel = viewModel(
            get = FakeGetHouseholdUseCase(headOutput),
            remove = FakeRemoveMemberUseCase(RemoveMemberUseCase.Output.Failure(IOException("boom"))),
        )
        viewModel.load()

        viewModel.onRemoveMember("u1")

        assertEquals(HouseholdEvent.RemoveFailed, viewModel.events.first())
    }

    @Test
    fun `onRegenerateCode reloads and the Member state shows the new code`() = runTest {
        val recoded = household.copy(inviteCode = "A1B2C3D4")
        val get = FakeGetHouseholdUseCase(headOutput, GetHouseholdUseCase.Output.Member(recoded, listOf(selfAsHead, otherMember)))
        val regenerate = FakeRegenerateInviteCodeUseCase(RegenerateInviteCodeUseCase.Output.Success("A1B2C3D4"))
        val viewModel = viewModel(get = get, regenerate = regenerate)
        viewModel.load()

        viewModel.onRegenerateCode()

        assertEquals(1, regenerate.calls)
        assertEquals("A1B2C3D4", (viewModel.uiState.value as HouseholdUiState.Member).household.inviteCode)
        assertEquals(2, get.calls)
    }

    @Test
    fun `onRegenerateCode when no longer head emits NotHead and reloads`() = runTest {
        val get = FakeGetHouseholdUseCase(headOutput, memberOutput)
        val viewModel = viewModel(
            get = get,
            regenerate = FakeRegenerateInviteCodeUseCase(RegenerateInviteCodeUseCase.Output.NotHead),
        )
        viewModel.load()

        viewModel.onRegenerateCode()

        assertEquals(HouseholdEvent.NotHead, viewModel.events.first())
        assertEquals(HouseholdUiState.Member(household, listOf(head, self)), viewModel.uiState.value)
    }

    @Test
    fun `onRegenerateCode emits RegenerateFailed and keeps the old code`() = runTest {
        val get = FakeGetHouseholdUseCase(headOutput)
        val viewModel = viewModel(
            get = get,
            regenerate = FakeRegenerateInviteCodeUseCase(RegenerateInviteCodeUseCase.Output.Failure(IOException("boom"))),
        )
        viewModel.load()

        viewModel.onRegenerateCode()

        assertEquals(HouseholdEvent.RegenerateFailed, viewModel.events.first())
        assertEquals("DEMO2026", (viewModel.uiState.value as HouseholdUiState.Member).household.inviteCode)
        assertEquals(1, get.calls)
    }

    @Test
    fun `a management action while busy is dropped`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val rename = FakeRenameHouseholdUseCase(RenameHouseholdUseCase.Output.Success, gate)
        val transfer = FakeTransferHeadUseCase(TransferHeadUseCase.Output.Success)
        val regenerate = FakeRegenerateInviteCodeUseCase(RegenerateInviteCodeUseCase.Output.Success("A1B2C3D4"))
        val viewModel = viewModel(
            get = FakeGetHouseholdUseCase(headOutput),
            rename = rename,
            transfer = transfer,
            regenerate = regenerate,
        )
        viewModel.load()

        viewModel.onRename("Dommer House")
        assertTrue(viewModel.busy.value)
        viewModel.onTransferHead("u1")
        viewModel.onRegenerateCode()

        assertEquals(null, transfer.lastInput)
        assertEquals(0, regenerate.calls)

        gate.complete(Unit)

        assertFalse(viewModel.busy.value)
        assertEquals(1, rename.inputs.size)
    }
}
