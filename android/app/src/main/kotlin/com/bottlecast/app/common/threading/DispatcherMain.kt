package com.bottlecast.app.common.threading

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Identifies a dispatcher corresponding to main/UI thread operations.
 */
@Qualifier
@Retention(RUNTIME)
annotation class DispatcherMain
