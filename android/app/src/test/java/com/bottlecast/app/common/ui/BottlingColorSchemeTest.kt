package com.bottlecast.app.common.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert
import org.junit.Test

/**
 * Checks the Night and Dawn colour schemes against the spec's palette (spec §1.2), so no role
 * falls back to Material's baseline purple.
 */
class BottlingColorSchemeTest {

    @Test
    fun `every colour role comes from the brand palette`() {
        // given
        val schemes = mapOf("Night" to NightColorScheme, "Dawn" to DawnColorScheme)

        schemes.forEach { (name, scheme) ->
            // when
            val roles = colorRolesOf(scheme)

            // then
            Assert.assertEquals("$name should fill every ColorScheme role", COLOR_ROLE_COUNT, roles.size)
            roles.forEach { (role, color) ->
                Assert.assertTrue(
                    "$name $role is ${color.hex()}, which isn't in the spec's palette",
                    color.hex() in SPEC_PALETTE,
                )
            }
        }
    }

    @Test
    fun `night roles match spec 1_2`() {
        // given
        val scheme = NightColorScheme

        // then
        assertColor("#0D1214", scheme.background)
        assertColor("#161D20", scheme.surface)
        assertColor("#20272A", scheme.surfaceVariant) // the dome
        assertColor("#222A2D", scheme.surfaceContainer) // surfaceHigh
        assertColor("#7E8B85", scheme.outline)
        assertColor("#343E41", scheme.outlineVariant)
        assertColor("#E8ECE6", scheme.onSurface)
        assertColor("#A9B4AE", scheme.onSurfaceVariant)
        assertColor("#C3CFC6", scheme.primary)
        assertColor("#17201B", scheme.onPrimary)
        assertColor("#2E3A34", scheme.primaryContainer)
        assertColor("#DCE8DF", scheme.onPrimaryContainer)
        assertColor("#FFB4A8", scheme.error)
        assertColor("#5C1A12", scheme.errorContainer)
        assertColor("#FFDAD4", scheme.onErrorContainer)
    }

    @Test
    fun `dawn roles match spec 1_2`() {
        // given
        val scheme = DawnColorScheme

        // then
        assertColor("#F6F4EE", scheme.background)
        assertColor("#FFFFFF", scheme.surface)
        assertColor("#D5E4E3", scheme.surfaceVariant) // the dome
        assertColor("#EEF1EC", scheme.surfaceContainer) // surfaceHigh
        assertColor("#6F7C75", scheme.outline)
        assertColor("#CBD3CE", scheme.outlineVariant)
        assertColor("#1A2320", scheme.onSurface)
        assertColor("#4F5B55", scheme.onSurfaceVariant)
        assertColor("#3F5A4C", scheme.primary)
        assertColor("#FFFFFF", scheme.onPrimary)
        assertColor("#D6E4DA", scheme.primaryContainer)
        assertColor("#10261A", scheme.onPrimaryContainer)
        assertColor("#B3261E", scheme.error)
        assertColor("#FBDAD5", scheme.errorContainer)
        assertColor("#410E0B", scheme.onErrorContainer)
    }

    @Test
    fun `brand colours match spec 1_2`() {
        // given
        val night = NightBottlingColors
        val dawn = DawnBottlingColors

        // then
        assertColor("#20272A", night.dome); assertColor("#D5E4E3", dawn.dome)
        assertColor("#2E383B", night.domeGlow); assertColor("#EEF5F3", dawn.domeGlow)
        assertColor("#222A2D", night.surfaceHigh); assertColor("#EEF1EC", dawn.surfaceHigh)
        assertColor("#63D3FF", night.glass); assertColor("#006688", dawn.glass)
        assertColor("#0E3A4A", night.glassContainer); assertColor("#CBEFFF", dawn.glassContainer)
        assertColor("#F0E0C4", night.moon); assertColor("#7A5A1E", dawn.moon)
        assertColor("#3A3122", night.moonContainer); assertColor("#F6E8CC", dawn.moonContainer)
        assertColor("#4A3510", dawn.onMoonContainer)
        assertColor("#E9DFCB", night.parchment); assertColor("#FFF9EC", dawn.parchment)
        assertColor("#2B241A", night.ink); assertColor("#2B241A", dawn.ink)
        assertColor("#645C4F", night.inkMuted); assertColor("#6B6459", dawn.inkMuted)
        assertColor("#00C2FF", night.brandSeed)
        // the Dawn moon is a pale sun-moon disc (decisions.md, 2026-10-13)
        assertColor("#F6E8CC", dawn.moonDisc)
        Assert.assertEquals(0.85f, night.moonDisc.alpha, 0.01f)
    }

    private fun assertColor(expected: String, actual: Color) =
        Assert.assertEquals(expected, actual.hex())

    // ColorScheme's roles are Color value classes, which compile to getters returning a long
    private fun colorRolesOf(scheme: ColorScheme): Map<String, Color> =
        ColorScheme::class.java.methods
            .filter { it.parameterCount == 0 && it.returnType == Long::class.javaPrimitiveType && it.name.startsWith("get") }
            .associate { it.name.substringAfter("get").substringBefore('-') to Color((it.invoke(scheme) as Long).toULong()) }

    private fun Color.hex(): String = "#%06X".format(value.shr(32).toLong() and 0xFFFFFF)

    private companion object {
        const val COLOR_ROLE_COUNT = 48

        // every hex value in spec §1.2's token table, plus black for the opaque Night scrim
        val SPEC_PALETTE = setOf(
            "#0D1214", "#F6F4EE", "#20272A", "#D5E4E3", "#2E383B", "#EEF5F3", "#161D20", "#FFFFFF",
            "#222A2D", "#EEF1EC", "#7E8B85", "#6F7C75", "#343E41", "#CBD3CE", "#E8ECE6", "#1A2320",
            "#A9B4AE", "#4F5B55", "#C3CFC6", "#3F5A4C", "#17201B", "#2E3A34", "#D6E4DA", "#DCE8DF",
            "#10261A", "#63D3FF", "#006688", "#0E3A4A", "#CBEFFF", "#F0E0C4", "#7A5A1E", "#2B241A",
            "#3A3122", "#F6E8CC", "#4A3510", "#E9DFCB", "#FFF9EC", "#645C4F", "#6B6459", "#00C2FF",
            "#FFB4A8", "#B3261E", "#5C1A12", "#FBDAD5", "#FFDAD4", "#410E0B", "#000000",
        )
    }
}
