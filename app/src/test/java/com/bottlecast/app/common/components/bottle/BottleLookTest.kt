package com.bottlecast.app.common.components.bottle

import androidx.compose.ui.graphics.Color
import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [bottleLookFor]. The keepsake's on-chain SVG and the web pages port the same
 * function, so these vectors pin it: byte 0 of SHA-256(id) mod 3 picks the silhouette, and byte 1
 * gives (byte / 255) × 12 − 6 degrees of hue.
 */
class BottleLookTest {

    @Test
    fun `the seed picks each silhouette and its hue from the id's SHA-256`() {
        // given: SHA-256("abc") starts ba 78, ("bottle-2") a0 44, ("bottle-1") c8 99
        val vectors = mapOf(
            "abc" to (BottleSilhouette.Classic to -0.3529f),
            "bottle-2" to (BottleSilhouette.Flask to -2.8f),
            "bottle-1" to (BottleSilhouette.Gourd to 1.2f),
        )

        vectors.forEach { (id, expected) ->
            // when
            val look = bottleLookFor(id)

            // then
            Assert.assertEquals(id, expected.first, look.silhouette)
            Assert.assertEquals(id, expected.second, look.hueJitter, 0.0001f)
            Assert.assertEquals(id, BottleTint.SeaGlass, look.tint)
        }
    }

    @Test
    fun `the same id always gives the same bottle`() {
        Assert.assertEquals(bottleLookFor("seededBottle00000001"), bottleLookFor("seededBottle00000001"))
    }

    @Test
    fun `the hue jitter stays within six degrees either way`() {
        val jitters = (0 until 500).map { bottleLookFor("bottle-$it").hueJitter }

        Assert.assertTrue(jitters.all { it in -6f..6f })
        Assert.assertTrue(jitters.any { it < -5f } && jitters.any { it > 5f })
    }

    @Test
    fun `turning the hue keeps saturation, brightness and alpha`() {
        // given: brandSeed, #00C2FF, at hue 194.35
        val seaGlass = Color(0xFF00C2FF)

        // when
        val turned = seaGlass.rotateHue(6f)
        val back = turned.rotateHue(-6f)

        // then
        Assert.assertEquals(seaGlass.red, back.red, 0.005f)
        Assert.assertEquals(seaGlass.green, back.green, 0.005f)
        Assert.assertEquals(seaGlass.blue, back.blue, 0.005f)
        Assert.assertEquals(1f, turned.blue, 0.001f)
        Assert.assertTrue(turned.green < seaGlass.green)
        Assert.assertEquals(seaGlass, seaGlass.rotateHue(0f))
        Assert.assertEquals(0.42f, seaGlass.copy(alpha = 0.42f).rotateHue(3f).alpha, 0.001f)
    }

    @Test
    fun `a grey has no hue to turn`() {
        val grey = Color(0xFF808080)

        Assert.assertEquals(grey, grey.rotateHue(6f))
    }
}
