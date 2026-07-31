package com.fullsail.shoppingmadebetter.feature.auth.data

import com.fullsail.shoppingmadebetter.feature.auth.domain.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication contract backed by Supabase Auth. Implementations talk to the
 * Supabase Auth plugin; callers (use cases) depend on this interface so they can
 * be tested with a fake.
 */
interface AuthRepository {
    /**
     * The current authentication session as a tri-state stream. Starts at
     * [AuthState.Initializing] while Supabase restores any cached session on
     * startup, then settles on [AuthState.Authenticated] or
     * [AuthState.Unauthenticated] and updates on every later sign-in/sign-out.
     */
    val authState: StateFlow<AuthState>

    /**
     * Registers a new account with [email] and [password]. Throws if the request
     * fails (email taken, weak password, network, …) — error translation is the
     * caller's responsibility.
     */
    suspend fun signUp(email: String, password: String)

    /**
     * Signs an existing user in with [email] and [password]. Throws if the request
     * fails (bad credentials, network, …).
     */
    suspend fun signIn(email: String, password: String)

    /**
     * Signs out the current user.
     */
    suspend fun signOut()
}
