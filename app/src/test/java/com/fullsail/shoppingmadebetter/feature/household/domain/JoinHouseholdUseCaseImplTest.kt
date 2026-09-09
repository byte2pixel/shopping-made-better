package com.fullsail.shoppingmadebetter.feature.household.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class JoinHouseholdUseCaseImplTest {

    @Test
    fun `returns Success with the joined household and trims the code`() = runTest {
        val repo = FakeHouseholdRepository()

        val out = JoinHouseholdUseCaseImpl(repo).execute(" demo2026 ")

        assertEquals(listOf("demo2026"), repo.joinedCodes)
        assertEquals(
            JoinHouseholdUseCase.Output.Success(Household(id = "h1", name = "Demo Household", inviteCode = "DEMO2026")),
            out,
        )
    }

    @Test
    fun `maps the invalid-code message even when wrapped`() = runTest {
        val repo = FakeHouseholdRepository(failWith = RuntimeException("P0001: invalid invite code (rpc join_household)"))

        val out = JoinHouseholdUseCaseImpl(repo).execute("NOPE")

        assertEquals(JoinHouseholdUseCase.Output.InvalidCode, out)
    }

    @Test
    fun `maps the already-in-a-household message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.ALREADY_IN_HOUSEHOLD))

        val out = JoinHouseholdUseCaseImpl(repo).execute("DEMO2026")

        assertEquals(JoinHouseholdUseCase.Output.AlreadyInHousehold, out)
    }

    @Test
    fun `returns Failure carrying any other error`() = runTest {
        val error = IOException("boom")

        val out = JoinHouseholdUseCaseImpl(FakeHouseholdRepository(failWith = error)).execute("DEMO2026")

        assertTrue(out is JoinHouseholdUseCase.Output.Failure)
        assertSame(error, (out as JoinHouseholdUseCase.Output.Failure).error)
    }
}
