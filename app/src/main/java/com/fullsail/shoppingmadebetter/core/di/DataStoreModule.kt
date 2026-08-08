package com.fullsail.shoppingmadebetter.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Backing file for the app's local, device-level settings. A single
 * [DataStore] instance must exist per file, so the delegate lives at the top
 * level and is exposed as an app-wide singleton via [DataStoreModule].
 */
private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings",
)

/**
 * Provides the app-wide [DataStore] for local user settings. A small key-value
 * UX preferences kept on-device (as opposed to the server-side profile data in
 * Supabase). Features inject this and own their own preference keys.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userSettingsDataStore
}