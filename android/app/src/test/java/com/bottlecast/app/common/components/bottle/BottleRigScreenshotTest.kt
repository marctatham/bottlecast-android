package com.bottlecast.app.common.components.bottle

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The bottle rig (spec §1.6): the three seeded silhouettes, the tints and the hue jitter, and the
 * pop's poses held still (the cork mid-flight, the scroll half out and clear of the neck, the sheen).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class BottleRigScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rigNight() = capture(darkTheme = true)

    @Test
    fun rigDawn() = capture(darkTheme = false)

    private fun capture(darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = BottlingTheme.spacing.gutter, vertical = 140.dp)
                    .width(358.dp),
                verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxl),
            ) {
                Line {
                    BottleSilhouette.entries.forEach { Bottle(BottleLook(it)) }
                    Bottle(BottleLook(tint = BottleTint.Amber))
                    Bottle(BottleLook(tint = BottleTint.Moonglass))
                }
                Line {
                    Bottle(BottleLook(hueJitter = -6f))
                    Bottle(BottleLook(hueJitter = 6f))
                    Bottle(BottleLook.Default, BottleRigPose(corkFlight = { 0.5f }))
                    Bottle(BottleLook.Default, BottleRigPose(scrollOut = { 0.5f }, shimmer = { 0.5f }), corked = false)
                    Bottle(BottleLook.Default, showScroll = false, corked = false)
                }
                Line {
                    Bottle(BottleLook.Default, BottleRigPose(scrollOut = { 1f }), corked = false)
                    Ripples(modifier = Modifier.width(200.dp))
                }
            }
        }
    }

    @Composable
    private fun Line(content: @Composable () -> Unit) {
        Row(horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s)) { content() }
    }

    @Composable
    private fun Bottle(
        look: BottleLook,
        pose: BottleRigPose = BottleRigPose.Still,
        corked: Boolean = true,
        showScroll: Boolean = true,
    ) {
        BottleRig(look = look, pose = pose, corked = corked, showScroll = showScroll, modifier = Modifier.height(96.dp))
    }
}
