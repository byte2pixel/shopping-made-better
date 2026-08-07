package com.fullsail.shoppingmadebetter.core.di

import javax.inject.Qualifier

/**
 * Marks the application-wide [kotlinx.coroutines.CoroutineScope] — a singleton scope
 * that lives for the whole process, for work that must outlast any one screen or
 * ViewModel (e.g. a repository mirroring Supabase's session status).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope