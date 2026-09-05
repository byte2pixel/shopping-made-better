package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Unit tests for [SetAutoAdjustEnabledUseCaseImpl]. */
class SetAutoAdjustEnabledUseCaseImplTest {

    /** Fake repository: records the value written, or throws [error]. */
    private class FakeProfileRepository(
        private val error: Throwable? = null,
    ) : ProfileRepository {
        var lastEnabled: Boolean? = null

        override suspend fun changePassword(newPassword: String) = TODO("Not yet implemented")
        override suspend fun updateContactInfo(email: String?, phone: String?) = TODO("Not yet implemented")
        override suspend fun getAutoAdjustEnabled(): Boolean = TODO("Not yet implemented")
        override suspend fun setAutoAdjustEnabled(enabled: Boolean) {
            error?.let { throw it }
            lastEnabled = enabled
        }
    }

    @Test
    fun `writes the flag and returns Success`() = runTest {
        val repo = FakeProfileRepository()
        val useCase = SetAutoAdjustEnabledUseCaseImpl(repo)

        val out = useCase.execute(SetAutoAdjustEnabledUseCase.Input(enabled = false))

        assertEquals(SetAutoAdjustEnabledUseCase.Output.Success, out)
        assertEquals(false, repo.lastEnabled)
    }

    @Test
    fun `returns Failure carrying the error when the repository throws`() = runTest {
        val error = IOException("boom")
        val repo = FakeProfileRepository(error = error)
        val useCase = SetAutoAdjustEnabledUseCaseImpl(repo)

        val out = useCase.execute(SetAutoAdjustEnabledUseCase.Input(enabled = true))

        assertTrue(out is SetAutoAdjustEnabledUseCase.Output.Failure)
        assertSame(error, (out as SetAutoAdjustEnabledUseCase.Output.Failure).error)
        assertNull(repo.lastEnabled)
    }
}
