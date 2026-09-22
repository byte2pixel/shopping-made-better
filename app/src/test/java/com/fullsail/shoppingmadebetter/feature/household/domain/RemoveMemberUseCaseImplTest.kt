package com.fullsail.shoppingmadebetter.feature.household.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RemoveMemberUseCaseImplTest {

    @Test
    fun `returns Success and the member is gone`() = runTest {
        val repo = FakeHouseholdRepository(
            household = FakeHouseholdRepository.DEMO,
            members = listOf(FakeHouseholdRepository.HEAD, FakeHouseholdRepository.SELF),
        )

        val out = RemoveMemberUseCaseImpl(repo).execute("u2")

        assertEquals(RemoveMemberUseCase.Output.Success, out)
        assertEquals("u2", repo.removed)
        assertEquals(listOf("u1"), repo.members.map { it.id })
    }

    @Test
    fun `maps the not-the-head message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.NOT_THE_HEAD))

        val out = RemoveMemberUseCaseImpl(repo).execute("u2")

        assertEquals(RemoveMemberUseCase.Output.NotHead, out)
    }

    @Test
    fun `maps the member-not-in-household message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.MEMBER_NOT_IN_HOUSEHOLD))

        val out = RemoveMemberUseCaseImpl(repo).execute("u9")

        assertEquals(RemoveMemberUseCase.Output.MemberNotInHousehold, out)
    }

    @Test
    fun `maps the cannot-remove-yourself message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.CANNOT_REMOVE_SELF))

        val out = RemoveMemberUseCaseImpl(repo).execute("u1")

        assertEquals(RemoveMemberUseCase.Output.CannotRemoveSelf, out)
    }

    @Test
    fun `returns Failure carrying any other error`() = runTest {
        val error = IOException("boom")

        val out = RemoveMemberUseCaseImpl(FakeHouseholdRepository(failWith = error)).execute("u2")

        assertTrue(out is RemoveMemberUseCase.Output.Failure)
        assertSame(error, (out as RemoveMemberUseCase.Output.Failure).error)
    }
}
