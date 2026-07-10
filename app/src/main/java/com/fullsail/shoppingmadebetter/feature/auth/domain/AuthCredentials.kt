package com.fullsail.shoppingmadebetter.feature.auth.domain

/**
 * Email/password pair a user enters to sign in or sign up. Shared input for
 * [SignInUseCase] and [SignUpUseCase] — Supabase auth is email-based, so both
 * actions take the same two fields.
 */
data class AuthCredentials(
    val email: String,
    val password: String,
)
