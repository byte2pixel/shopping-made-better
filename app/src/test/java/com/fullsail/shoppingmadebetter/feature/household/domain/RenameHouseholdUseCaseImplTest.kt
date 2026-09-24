package com.fullsail.shoppingmadebetter.feature.household.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RenameHouseholdUseCaseImplTest {

    @Test
    fun `returns Success and writes the trimmed name against the household id`() = runTest {
        val repo = FakeHouseholdRepository(household = FakeHouseholdRepository.DEMO)

        val out = RenameHouseholdUseCaseImpl(repo).execute(RenameHousehold("h1", "  Dommer House "))

        assertEquals(RenameHouseholdUseCase.Output.Success, out)
        assertEquals("h1" to "Dommer House", repo.renamedTo)
        assertEquals("Dommer House", repo.household?.name)
    }

    @Test
    fun `refuses a blank name without calling the repository`() = runTest {
        val repo = FakeHouseholdRepository(household = FakeHouseholdRepository.DEMO)

        val out = RenameHouseholdUseCaseImpl(repo).execute(RenameHousehold("h1", "   "))

        assertTrue(out is RenameHouseholdUseCase.Output.Failure)
        assertNull(repo.renamedTo)
    }

    @Test
    fun `returns Failure carrying any other error`() = runTest {
        val error = IOException("boom")

        val out = RenameHouseholdUseCaseImpl(FakeHouseholdRepository(failWith = error))
            .execute(RenameHousehold("h1", "Dommer House"))

        assertTrue(out is RenameHouseholdUseCase.Output.Failure)
        assertSame(error, (out as RenameHouseholdUseCase.Output.Failure).error)
    }
}
