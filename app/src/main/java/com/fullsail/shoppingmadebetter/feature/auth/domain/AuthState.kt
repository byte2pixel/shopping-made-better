package com.fullsail.shoppingmadebetter.feature.auth.domain

/**
 * The app's view of the current authentication session, derived from Supabase's
 * `SessionStatus`. Used to gate cold-start navigation: show a splash while
 * [Initializing], then route into the app ([Authenticated]) or to the login
 * screen ([Unauthenticated]).
 */
enum class AuthState {
    /** The session is still being restored from storage; nothing decided yet. */
    Initializing,

    /** A valid session exists — the user is signed in. */
    Authenticated,

    /** No valid session — the user must sign in. */
    Unauthenticated,
}