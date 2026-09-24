package com.fullsail.shoppingmadebetter.feature.household.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RegenerateInviteCodeUseCaseImplTest {

    @Test
    fun `returns Success with the new code and the household carries it`() = runTest {
        val repo = FakeHouseholdRepository(household = FakeHouseholdRepository.DEMO)

        val out = RegenerateInviteCodeUseCaseImpl(repo).execute(Unit)

        assertEquals(RegenerateInviteCodeUseCase.Output.Success(FakeHouseholdRepository.NEW_CODE), out)
        assertEquals(1, repo.regenerated)
        assertEquals(FakeHouseholdRepository.NEW_CODE, repo.household?.inviteCode)
    }

    @Test
    fun `maps the not-the-head message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.NOT_THE_HEAD))

        val out = RegenerateInviteCodeUseCaseImpl(repo).execute(Unit)

        assertEquals(RegenerateInviteCodeUseCase.Output.NotHead, out)
    }

    @Test
    fun `returns Failure carrying any other error`() = runTest {
        val error = IOException("boom")

        val out = RegenerateInviteCodeUseCaseImpl(FakeHouseholdRepository(failWith = error)).execute(Unit)

        assertTrue(out is RegenerateInviteCodeUseCase.Output.Failure)
        assertSame(error, (out as RegenerateInviteCodeUseCase.Output.Failure).error)
    }
}
