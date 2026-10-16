package com.bottlecast.app.common.threading

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Identifies a dispatcher corresponding to CPU intensive tasks
 */
@Qualifier
@Retention(RUNTIME)
annotation class DispatcherDefault
