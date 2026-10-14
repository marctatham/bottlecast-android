package com.bottlecast.app.common.threading

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Identifies a coroutine scope that lives as long as the app, for work that must outlive the screen
 * that starts it, such as a throw back still waiting out its undo window on the Shore.
 */
@Qualifier
@Retention(RUNTIME)
annotation class ApplicationScope
