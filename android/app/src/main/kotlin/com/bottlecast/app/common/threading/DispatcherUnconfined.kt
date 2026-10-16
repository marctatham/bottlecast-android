package com.bottlecast.app.common.threading

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Identifies a dispatcher that is not confined to any specific thread
 */
@Qualifier
@Retention(RUNTIME)
annotation class DispatcherUnconfined
