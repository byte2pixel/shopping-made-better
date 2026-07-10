package com.fullsail.shoppingmadebetter.feature.auth.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Supabase-backed [AuthRepository]. Runs the email/password flows through the
 * [Auth] plugin on [Dispatchers.IO]; any failure propagates to the caller.
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
) : AuthRepository {

    override suspend fun signUp(email: String, password: String) = withContext(Dispatchers.IO) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }
}