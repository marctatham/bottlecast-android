package com.bottlecast.app.common.ui

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * True when the user has asked for less motion: the system's animator duration scale is 0
 * ("Remove animations"). Settings adds an in-app "Reduce motion" switch in slice 4.
 *
 * Under reduced motion there are no infinite transitions, Lottie is pinned to a still frame and
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
