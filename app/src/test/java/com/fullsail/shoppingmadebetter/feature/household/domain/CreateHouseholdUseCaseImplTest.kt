package com.fullsail.shoppingmadebetter.feature.household.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CreateHouseholdUseCaseImplTest {

    @Test
    fun `returns Success with the new household and trims the name`() = runTest {
        val repo = FakeHouseholdRepository()

        val out = CreateHouseholdUseCaseImpl(repo).execute("  Our Place ")

        assertEquals(listOf("Our Place"), repo.createdNames)
        assertEquals(
            CreateHouseholdUseCase.Output.Success(Household(id = "h-new", name = "Our Place", inviteCode = "NEW12345")),
            out,
        )
    }

    @Test
    fun `maps the already-in-a-household message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.ALREADY_IN_HOUSEHOLD))

        val out = CreateHouseholdUseCaseImpl(repo).execute("Our Place")

        assertEquals(CreateHouseholdUseCase.Output.AlreadyInHousehold, out)
    }

    @Test
    fun `returns Failure carrying any other error`() = runTest {
        val error = IOException("boom")

        val out = CreateHouseholdUseCaseImpl(FakeHouseholdRepository(failWith = error)).execute("Our Place")

        assertTrue(out is CreateHouseholdUseCase.Output.Failure)
        assertSame(error, (out as CreateHouseholdUseCase.Output.Failure).error)
    }
}
