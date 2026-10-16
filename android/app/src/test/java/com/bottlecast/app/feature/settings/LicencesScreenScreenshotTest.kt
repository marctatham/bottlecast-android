package com.bottlecast.app.feature.settings

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Open-source licences (spec §3.17), at the boards' 390×844 with a 44dp status bar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class LicencesScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun licencesNight() = capture(darkTheme = true)

    @Test
    fun licencesDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            LicencesScreen(
                uiState = LicencesUiState.Showing(libraries = LIBRARIES),
                onBackHandler = {},
                onBundledHandler = {},
                onLibraryHandler = {},
                windowInsets = BoardStatusBar,
                contentPadding = PaddingValues(),
            )
        }
    }

    private companion object {
        val APACHE = listOf("Apache License, Version 2.0")
        const val APACHE_URL = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        val LIBRARIES = listOf(
            LicensedLibrary("androidx.activity:activity", "Activity", "1.13.0", APACHE, APACHE_URL),
            LicensedLibrary("com.google.dagger:hilt-android", "Hilt Android", "2.60.1", APACHE, APACHE_URL),
            LicensedLibrary("com.jakewharton.timber:timber", "Timber", "5.0.1", APACHE, APACHE_URL),
            LicensedLibrary("org.jetbrains.kotlinx:kotlinx-coroutines-core", "kotlinx-coroutines-core", "1.11.0", APACHE, APACHE_URL),
        )
    }
}
