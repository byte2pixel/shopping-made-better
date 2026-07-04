package com.fullsail.shoppingmadebetter.di

import com.fullsail.shoppingmadebetter.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Singleton

/**
 * Provides the app-wide [SupabaseClient] and its plugin handles.
 *
 * The client points at the local Supabase stack (see README "Setting Up Supabase
 * Locally"); its URL and publishable key come from [BuildConfig], which is fed
 * from `local.properties`. Repositories inject the specific plugin they need
 * (e.g. [Postgrest]) rather than the whole client.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
            // Installed now so the client is ready for the auth/login ticket;
            // unused by the current store use case.
            install(Auth)
        }

    @Provides
    @Singleton
    fun providePostgrest(client: SupabaseClient): Postgrest = client.postgrest
}
