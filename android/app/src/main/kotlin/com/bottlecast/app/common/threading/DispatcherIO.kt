package com.bottlecast.app.common.threading

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Identifies a dispatcher corresponding to IO thread operations.
 */
@Qualifier
@Retention(RUNTIME)
annotation class DispatcherIO
