package com.bottlecast.app.feature.castsuccess

import android.app.Application
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.feature.castsuccess.components.CastMotion
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Cast success (spec §3.5, the Cast-Success board), settled in Night and Dawn, and frames of the cast
 * on its way (spec §1.5).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class CastSuccessScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun castNight() = capture(darkTheme = true)

    @Test
    fun castDawn() = capture(darkTheme = false)

    @Test
    fun queuedNight() = capture(darkTheme = true, queued = true)

    @Test
    fun queuedDawn() = capture(darkTheme = false, queued = true)

    @Test
    fun sealingOffNight() = capture(darkTheme = true, sealable = false)

    @Test
    fun sealingOffDawn() = capture(darkTheme = false, sealable = false)

    @Test
    fun rollingUpNight() = capture(darkTheme = true, motion = CastMotion.at(140f))

    @Test
    fun slippingInNight() = capture(darkTheme = true, motion = CastMotion.at(390f))

    @Test
    fun corkedNight() = capture(darkTheme = true, motion = CastMotion.at(700f))

    @Test
    fun tossedNight() = capture(darkTheme = true, motion = CastMotion.at(1_250f))

    @Test
    fun splashedNight() = capture(darkTheme = true, motion = CastMotion.at(1_900f))

    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(darkTheme = true)

    @Test
    fun rightToLeftNight() = capture(darkTheme = true, layoutDirection = LayoutDirection.Rtl)

    private fun capture(
        darkTheme: Boolean,
        queued: Boolean = false,
        sealable: Boolean = true,
        motion: CastMotion = CastMotion.Settled,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CastSuccessScreen(
                    uiState = CastSuccessUiState(look = bottleLookFor(BOTTLE), queued = queued, sealable = sealable, played = true),
                    motion = motion,
                    onSkip = {},
                    onBackToShore = {},
                    windowInsets = BoardBars,
                )
            }
        }
    }

    private companion object {
        const val BOTTLE = "bottle00000000000013"
        val BoardBars = WindowInsets(top = 44.dp, bottom = 16.dp)
    }
}
