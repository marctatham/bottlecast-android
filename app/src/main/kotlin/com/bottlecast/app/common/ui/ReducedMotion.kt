package com.bottlecast.app.common.ui

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * True when the user has asked for less motion: the system's animator duration scale is 0
 * ("Remove animations"). Settings adds an in-app "Reduce motion" switch in slice 4.
 *
 * Under reduced motion there are no infinite transitions, the cast is a short crossfade, and
 * haptics stay on (spec §1.5). It's re-read each time the screen resumes, so changing the setting
 * applies without a restart.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    var reducedMotion by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    LifecycleResumeEffect(Unit) {
        reducedMotion = !ValueAnimator.areAnimatorsEnabled()
        onPauseOrDispose {}
    }
    return reducedMotion
}

/**
 * True while idle loops, such as the breathing tide and the bottle's bob, may run: the screen is
 * resumed, so it's in front and not mid-transition, and motion isn't reduced.
 *
 * @param reducedMotion pass a screen's own [rememberReducedMotion] when it reads that too.
 */
@Composable
fun rememberIdleMotionEnabled(reducedMotion: Boolean = rememberReducedMotion()): Boolean {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return !reducedMotion && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
}
