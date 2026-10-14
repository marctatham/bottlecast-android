package com.bottlecast.app.common.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert
import org.junit.Test

/** Text on parchment meets AA, 4.5:1, in both themes (spec §1.2, §4). */
class InkContrastTest {

    @Test
    fun `ink, muted ink, links and errors are readable on both parchments`() {
        // given
        val themes = mapOf("Night" to NightBottlingColors, "Dawn" to DawnBottlingColors)

        themes.forEach { (name, colors) ->
            // when
            val inks = mapOf(
                "ink" to colors.ink,
                "inkMuted" to colors.inkMuted,
                "inkLink" to colors.inkLink,
                "inkError" to colors.inkError,
            )

            // then
            inks.forEach { (token, ink) ->
                val ratio = contrast(ink, colors.parchment)
                Assert.assertTrue("$name $token on parchment is ${"%.2f".format(ratio)}:1", ratio >= AA)
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private companion object {
        const val AA = 4.5f
    }
}
