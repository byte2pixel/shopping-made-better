package com.fullsail.shoppingmadebetter.feature.auth.data

import android.util.Log
import com.fullsail.shoppingmadebetter.core.di.ApplicationScope
import com.fullsail.shoppingmadebetter.feature.auth.domain.AuthState
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Supabase-backed [AuthRepository]. Runs the email/password flows through the
 * [Auth] plugin on [Dispatchers.IO]; any failure propagates to the caller.
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    @ApplicationScope scope: CoroutineScope,
) : AuthRepository {

    private val _authState = MutableStateFlow(AuthState.Initializing)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // The Auth plugin restores any cached session from storage on startup and
        // emits it here; mirror its status into our state so the app can gate
        // cold-start navigation (and react to later sign-in/sign-out) off one flow.
        scope.launch {
            auth.sessionStatus.collect { status ->
                _authState.value = when (status) {
                    is SessionStatus.Authenticated -> AuthState.Authenticated
                    SessionStatus.Initializing -> AuthState.Initializing
                    is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                    // A failed token refresh means the cached session is no longer
                    // trustworthy; treat it as signed out so the user re-authenticates.
                    is SessionStatus.RefreshFailure -> AuthState.Unauthenticated
                }
                Log.d(TAG, "Session status ${status::class.simpleName} -> ${_authState.value}")
            }
        }
    }

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

    override suspend fun signOut() {
        auth.signOut()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
