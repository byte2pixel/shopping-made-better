package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdMemberDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GetHouseholdUseCaseImplTest {

    @Test
    fun `returns None when the caller has no household`() = runTest {
        val out = GetHouseholdUseCaseImpl(FakeHouseholdRepository()).execute(Unit)

        assertEquals(GetHouseholdUseCase.Output.None, out)
    }

    @Test
    fun `returns Member with the head first and the rest by name`() = runTest {
        val zed = HouseholdMemberDto(id = "u3", displayName = "zed", isHead = false, isSelf = false)
        val repo = FakeHouseholdRepository(
            household = FakeHouseholdRepository.DEMO,
            members = listOf(zed, FakeHouseholdRepository.SELF, FakeHouseholdRepository.HEAD),
        )

        val out = GetHouseholdUseCaseImpl(repo).execute(Unit)

        assertEquals(
            GetHouseholdUseCase.Output.Member(
                household = Household(id = "h1", name = "Demo Household", inviteCode = "DEMO2026"),
                members = listOf(
                    HouseholdMember(id = "u1", displayName = "Demo Shopper", isHead = true, isSelf = false),
                    HouseholdMember(id = "u2", displayName = "Demo Roommate", isHead = false, isSelf = true),
                    HouseholdMember(id = "u3", displayName = "zed", isHead = false, isSelf = false),
                ),
            ),
            out,
        )
    }

    @Test
    fun `returns Failure carrying the error when the repository throws`() = runTest {
        val error = IOException("boom")

        val out = GetHouseholdUseCaseImpl(FakeHouseholdRepository(failWith = error)).execute(Unit)

        assertTrue(out is GetHouseholdUseCase.Output.Failure)
        assertSame(error, (out as GetHouseholdUseCase.Output.Failure).error)
    }
}
