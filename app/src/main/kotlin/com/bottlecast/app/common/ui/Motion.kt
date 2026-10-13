package com.bottlecast.app.common.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * The spec's non-spring motion (spec §1.5), read through [BottlingTheme.motion]. Springs come from
 * `MaterialTheme.motionScheme`, which BottlingTheme sets to `MotionScheme.expressive()`: spatial
 * springs for physical objects, effects springs for colour and opacity.
 */
object BottlingMotion {
    const val MICRO_MILLIS = 200
    const val COMPONENT_MILLIS = 300
    const val SCREEN_MILLIS = 450

    // Material's emphasized easings, which Material3 keeps internal
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> micro(): FiniteAnimationSpec<T> = tween(MICRO_MILLIS, easing = Emphasized)
    fun <T> component(): FiniteAnimationSpec<T> = tween(COMPONENT_MILLIS, easing = Emphasized)
    fun <T> screen(): FiniteAnimationSpec<T> = tween(SCREEN_MILLIS, easing = Emphasized)
}
