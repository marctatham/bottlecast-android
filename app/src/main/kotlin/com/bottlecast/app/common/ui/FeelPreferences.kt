package com.bottlecast.app.common.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Settings › Appearance & feel (spec §3.17, G16), as the screens read them. MainActivity provides the
 * stored settings; previews and tests get these defaults.
 */
@Immutable
data class FeelPreferences(
    /** Reduce motion in the app even when the phone doesn't ask for it. */
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val shakeToUncork: Boolean = true,
)

val LocalFeelPreferences = staticCompositionLocalOf { FeelPreferences() }
