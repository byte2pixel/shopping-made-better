package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Unit tests for [GetAutoAdjustEnabledUseCaseImpl]. */
class GetAutoAdjustEnabledUseCaseImplTest {

    /** Fake repository: returns [enabled], or throws [error]. */
    private class FakeProfileRepository(
        private val enabled: Boolean = true,
        private val error: Throwable? = null,
    ) : ProfileRepository {
        override suspend fun changePassword(newPassword: String) = TODO("Not yet implemented")
        override suspend fun updateContactInfo(email: String?, phone: String?) = TODO("Not yet implemented")
        override suspend fun getAutoAdjustEnabled(): Boolean = error?.let { throw it } ?: enabled
        override suspend fun setAutoAdjustEnabled(enabled: Boolean) = TODO("Not yet implemented")
    }

    @Test
    fun `returns Success carrying the flag`() = runTest {
        assertEquals(
            GetAutoAdjustEnabledUseCase.Output.Success(enabled = true),
            GetAutoAdjustEnabledUseCaseImpl(FakeProfileRepository(enabled = true)).execute(Unit),
        )
        assertEquals(
            GetAutoAdjustEnabledUseCase.Output.Success(enabled = false),
            GetAutoAdjustEnabledUseCaseImpl(FakeProfileRepository(enabled = false)).execute(Unit),
        )
    }

    @Test
    fun `returns Failure carrying the error when the repository throws`() = runTest {
        val error = IOException("boom")
        val useCase = GetAutoAdjustEnabledUseCaseImpl(FakeProfileRepository(error = error))

        val out = useCase.execute(Unit)

        assertTrue(out is GetAutoAdjustEnabledUseCase.Output.Failure)
        assertSame(error, (out as GetAutoAdjustEnabledUseCase.Output.Failure).error)
    }
}
