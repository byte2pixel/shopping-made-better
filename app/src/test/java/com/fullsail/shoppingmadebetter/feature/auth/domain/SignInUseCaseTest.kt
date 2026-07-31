package com.fullsail.shoppingmadebetter.feature.auth.domain

import com.fullsail.shoppingmadebetter.feature.auth.data.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [SignInUseCaseImpl] using a hand-written fake [AuthRepository] —
 * no mocking framework needed.
 */
class SignInUseCaseTest {

    /** Fake repository: records the last call, or throws [error] if set. */
    private class FakeAuthRepository(private val error: Throwable? = null) : AuthRepository {
        var lastSignInEmail: String? = null
        override suspend fun signIn(email: String, password: String) {
            lastSignInEmail = email
            error?.let { throw it }
        }
        override suspend fun signUp(email: String, password: String) = Unit
        override suspend fun signOut() = Unit
    }

    @Test
    fun `execute returns Success and forwards credentials when the repository succeeds`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = SignInUseCaseImpl(repository)

        val output = useCase.execute(AuthCredentials("user@example.com", "hunter2"))

        assertTrue(output is SignInUseCase.Output.Success)
        assertEquals("user@example.com", repository.lastSignInEmail)
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = SignInUseCaseImpl(FakeAuthRepository(error = boom))

        val output = useCase.execute(AuthCredentials("user@example.com", "hunter2"))

        assertTrue(output is SignInUseCase.Output.Failure)
        assertSame(boom, (output as SignInUseCase.Output.Failure).error)
    }
}