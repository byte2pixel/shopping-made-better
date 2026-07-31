package com.fullsail.shoppingmadebetter.feature.auth.data

/**
 * Authentication contract backed by Supabase Auth. Implementations talk to the
 * Supabase Auth plugin; callers (use cases) depend on this interface so they can
 * be tested with a fake.
 */
interface AuthRepository {
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
