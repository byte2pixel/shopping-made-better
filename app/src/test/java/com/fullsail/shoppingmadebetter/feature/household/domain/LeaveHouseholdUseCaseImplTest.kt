package com.fullsail.shoppingmadebetter.feature.household.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LeaveHouseholdUseCaseImplTest {

    @Test
    fun `returns Success and the repository no longer has a household`() = runTest {
        val repo = FakeHouseholdRepository(household = FakeHouseholdRepository.DEMO)

        val out = LeaveHouseholdUseCaseImpl(repo).execute(Unit)

        assertEquals(LeaveHouseholdUseCase.Output.Success, out)
        assertEquals(1, repo.leaveCalls)
        assertNull(repo.household)
    }

    @Test
    fun `maps the head-cannot-leave message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.HEAD_CANNOT_LEAVE))

        val out = LeaveHouseholdUseCaseImpl(repo).execute(Unit)

        assertEquals(LeaveHouseholdUseCase.Output.HeadWithMembers, out)
    }

    @Test
    fun `treats not-in-a-household as Success`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.NOT_IN_HOUSEHOLD))

        val out = LeaveHouseholdUseCaseImpl(repo).execute(Unit)

        assertEquals(LeaveHouseholdUseCase.Output.Success, out)
    }

    @Test
    fun `returns Failure carrying any other error`() = runTest {
        val error = IOException("boom")

        val out = LeaveHouseholdUseCaseImpl(FakeHouseholdRepository(failWith = error)).execute(Unit)

        assertTrue(out is LeaveHouseholdUseCase.Output.Failure)
        assertSame(error, (out as LeaveHouseholdUseCase.Output.Failure).error)
    }
}
