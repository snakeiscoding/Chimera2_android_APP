package com.jht.annotations

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Qualifier

/**
 * Annotations files to inject various types of [CoroutineDispatcher]
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IODispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher