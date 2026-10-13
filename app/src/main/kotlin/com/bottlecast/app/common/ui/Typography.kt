package com.bottlecast.app.common.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.bottlecast.app.R

// All three families are bundled in res/font, so devices render exactly what the screenshot tests
// capture (Robolectric can't load downloadable fonts). Licences: licenses/README.md.

/** Roboto Slab, the voice: headlines, titles, button labels. */
internal val RobotoSlab = FontFamily(
    Font(R.font.robotoslab_regular, FontWeight.Normal),
    Font(R.font.robotoslab_medium, FontWeight.Medium),
    Font(R.font.robotoslab_semibold, FontWeight.SemiBold),
    Font(R.font.robotoslab_bold, FontWeight.Bold),
    Font(R.font.robotoslab_black, FontWeight.Black),
)

/**
 * Roboto Flex, for reading. It ships only as a variable font, so each weight is a variation of
 * the same file. The boards render it with CSS `font-optical-sizing: auto`, so each family pins
 * the optical size (`opsz`) to the text size it's used at.
 *
 * The optical size is the style's size in sp as a plain number. `FontVariation.opticalSizing`
 * would scale it by the font scale when the typeface first loads, and Compose caches typefaces
 * process-wide without the font scale in the key: whichever scale loaded first would then stick,
 * on a device after the user changes the text size, and between screenshot tests.
 */
internal fun robotoFlex(opticalSize: TextUnit): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold).map { weight ->
        Font(
            resId = R.font.roboto_flex,
            weight = weight,
            variationSettings = FontVariation.Settings(
                weight,
                FontStyle.Normal,
                FontVariation.Setting(OPTICAL_SIZE_AXIS, opticalSize.value),
            ),
        )
    }
)

private const val OPTICAL_SIZE_AXIS = "opsz"

private val RobotoFlex11 = robotoFlex(11.sp)
private val RobotoFlex12 = robotoFlex(12.sp)
private val RobotoFlex14 = robotoFlex(14.sp)
private val RobotoFlex16 = robotoFlex(16.sp)

/** Roboto Mono, for on-chain facts. */
internal val RobotoMono = FontFamily(Font(R.font.roboto_mono_regular, FontWeight.Normal))

// matches Material's own defaults (centred line height, no trimming), with the boards' zero tracking
private val BaseStyle = TextStyle(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
    letterSpacing = 0.sp,
)

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
) = BaseStyle.copy(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

// spec §1.3; roles the spec doesn't name keep Material's sizes in the brand families
private val displayLarge = style(RobotoSlab, FontWeight.Black, 57.sp, 64.sp)
private val displayMedium = style(RobotoSlab, FontWeight.Bold, 45.sp, 52.sp)
private val displaySmall = style(RobotoSlab, FontWeight.Black, 36.sp, 44.sp)
private val headlineLarge = style(RobotoSlab, FontWeight.SemiBold, 32.sp, 40.sp)
private val headlineMedium = style(RobotoSlab, FontWeight.SemiBold, 28.sp, 36.sp)
private val headlineSmall = style(RobotoSlab, FontWeight.SemiBold, 24.sp, 32.sp)
private val titleLarge = style(RobotoSlab, FontWeight.Bold, 22.sp, 30.sp)
private val titleMedium = style(RobotoSlab, FontWeight.SemiBold, 16.sp, 24.sp)
private val bodyLarge = style(RobotoFlex16, FontWeight.Normal, 16.sp, 24.sp)
private val bodyMedium = style(RobotoFlex14, FontWeight.Normal, 14.sp, 20.sp)
private val bodySmall = style(RobotoFlex12, FontWeight.Normal, 12.sp, 16.sp)
private val labelLarge = style(RobotoFlex14, FontWeight.Medium, 14.sp, 20.sp)
private val labelMedium = style(RobotoFlex12, FontWeight.Medium, 12.sp, 16.sp)
private val labelSmall = style(RobotoFlex11, FontWeight.SemiBold, 11.sp, 16.sp, letterSpacing = 0.8.sp)

// Material's tabs read titleSmall; the spec sets tabs in labelLarge
private val titleSmall = labelLarge

internal val BottlingM3Typography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
    // the design has no emphasized roles, so they match the base roles and Material's font never leaks
    displayLargeEmphasized = displayLarge,
    displayMediumEmphasized = displayMedium,
    displaySmallEmphasized = displaySmall,
    headlineLargeEmphasized = headlineLarge,
    headlineMediumEmphasized = headlineMedium,
    headlineSmallEmphasized = headlineSmall,
    titleLargeEmphasized = titleLarge,
    titleMediumEmphasized = titleMedium,
    titleSmallEmphasized = titleSmall,
    bodyLargeEmphasized = bodyLarge,
    bodyMediumEmphasized = bodyMedium,
    bodySmallEmphasized = bodySmall,
    labelLargeEmphasized = labelLarge,
    labelMediumEmphasized = labelMedium,
    labelSmallEmphasized = labelSmall,
)

/**
 * The spec's roles that Material's [Typography] has no slot for (spec §1.3). Overlines (labelSmall)
 * are uppercased by [com.bottlecast.app.common.components.OverlineText], since Compose has no
 * text-transform.
 */
object BottlingTypography {
    /** "BottleCast", top-start on the Shore. */
    val wordmark: TextStyle = style(RobotoSlab, FontWeight.Black, 20.sp, 28.sp)

    /** Shore B's "3h 12m". */
    val countdown: TextStyle = style(RobotoSlab, FontWeight.SemiBold, 36.sp, 44.sp)
        .copy(fontFeatureSettings = "tnum")

    /** Profile and share-card stats. */
    val statNumeral: TextStyle = style(RobotoSlab, FontWeight.Black, 28.sp, 32.sp)
        .copy(fontFeatureSettings = "tnum")

    /** The Shore's smaller stats row. */
    val shoreStatNumeral: TextStyle = style(RobotoSlab, FontWeight.SemiBold, 18.sp, 24.sp)

    /** Messages of 121–240 characters; shorter ones use titleLarge. */
    val titleLargeLong: TextStyle = style(RobotoSlab, FontWeight.Medium, 20.sp, 28.sp)

    /** Addresses and ids, middle-elided. */
    val mono: TextStyle = style(RobotoMono, FontWeight.Normal, 13.sp, 18.sp)

    /** Inline links such as the banner's "See them". */
    val link: TextStyle = style(RobotoFlex14, FontWeight.SemiBold, 14.sp, 20.sp)
        .copy(textDecoration = TextDecoration.Underline)
}
