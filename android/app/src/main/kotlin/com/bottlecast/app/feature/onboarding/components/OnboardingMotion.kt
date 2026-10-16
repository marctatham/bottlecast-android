package com.bottlecast.app.feature.onboarding.components

import androidx.compose.runtime.Stable

/**
 * Onboarding's motion, read at draw time so screenshot tests can hold it still (spec §1.5, §3.1):
 * [bob] is the bottle's bob on page 1 (0 to 1), and [sealStamp] how far page 3's seal has dropped
 * onto the parchment (0 lifted, 1 pressed on). Under reduced motion both hold still.
 */
@Stable
class OnboardingMotion(
    val bob: () -> Float,
    val sealStamp: () -> Float,
    val haloAlpha: () -> Float,
) {
    companion object {
        /** Still, with the seal pressed on: reduced motion, previews and screenshot tests. */
        val Still = OnboardingMotion(bob = { 0f }, sealStamp = { 1f }, haloAlpha = { 1f })
    }
}
