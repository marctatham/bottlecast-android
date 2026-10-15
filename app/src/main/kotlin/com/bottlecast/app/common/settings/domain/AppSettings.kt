package com.bottlecast.app.common.settings.domain

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/** Settings › Appearance & feel's theme (spec §3.17, decisions: System is the default). */
enum class ThemeChoice { System, Night, Dawn }

/** The settings kept on this phone (spec §3.17). The reminders keep their own. */
data class AppSettings(
    val theme: ThemeChoice = ThemeChoice.System,
    /** Reduce motion even when the phone doesn't ask for it; the phone's setting always applies. */
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    /** Shaking uncorks a bottle (G16); tapping and holding always do. */
    val shakeToUncork: Boolean = true,
    /** Preview's "Sign with my pen name" starts on; each bottle keeps what was chosen when it was cast. */
    val signWithPenName: Boolean = false,
)

interface ISettingsStore {
    val settings: Flow<AppSettings>

    fun setTheme(theme: ThemeChoice): Job

    fun setReduceMotion(reduce: Boolean): Job

    fun setHaptics(on: Boolean): Job

    fun setShakeToUncork(on: Boolean): Job

    fun setSignWithPenName(sign: Boolean): Job
}

/** Applies the theme to the app, so its windows, splash and Compose theme all follow it. */
fun interface IThemeApplier {
    fun apply(theme: ThemeChoice)
}
