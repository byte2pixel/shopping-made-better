package com.fullsail.shoppingmadebetter.feature.household.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TransferHeadUseCaseImplTest {

    @Test
    fun `returns Success and the target is the only head`() = runTest {
        val self = FakeHouseholdRepository.SELF.copy(isHead = true)
        val other = FakeHouseholdRepository.HEAD.copy(isHead = false)
        val repo = FakeHouseholdRepository(household = FakeHouseholdRepository.DEMO, members = listOf(self, other))

        val out = TransferHeadUseCaseImpl(repo).execute("u1")

        assertEquals(TransferHeadUseCase.Output.Success, out)
        assertEquals("u1", repo.transferredTo)
        assertEquals(listOf("u1"), repo.members.filter { it.isHead }.map { it.id })
    }

    @Test
    fun `maps the not-the-head message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.NOT_THE_HEAD))

        val out = TransferHeadUseCaseImpl(repo).execute("u1")

        assertEquals(TransferHeadUseCase.Output.NotHead, out)
    }

    @Test
    fun `maps the member-not-in-household message`() = runTest {
        val repo = FakeHouseholdRepository(failWith = IllegalStateException(HouseholdErrors.MEMBER_NOT_IN_HOUSEHOLD))

        val out = TransferHeadUseCaseImpl(repo).execute("u9")

        assertEquals(TransferHeadUseCase.Output.MemberNotInHousehold, out)
    }

    @Test
    fun `returns Failure carrying any other error`() = runTest {
        val error = IOException("boom")

        val out = TransferHeadUseCaseImpl(FakeHouseholdRepository(failWith = error)).execute("u1")

        assertTrue(out is TransferHeadUseCase.Output.Failure)
        assertSame(error, (out as TransferHeadUseCase.Output.Failure).error)
    }
}
