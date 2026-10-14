package com.bottlecast.app.common.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The Moonlit Shore palette (spec §1.2; the same hex values as docs/design/boards/TEMPLATE.md §2).
// Every colour the app shows comes from here: "use these, invent no greys". Night is the hero
// theme, Dawn the light one; dynamic colour is off.

// Night
private val NightBackground = Color(0xFF0D1214)
private val NightDome = Color(0xFF20272A)
private val NightDomeGlow = Color(0xFF2E383B)
private val NightSurface = Color(0xFF161D20)
private val NightSurfaceHigh = Color(0xFF222A2D)
private val NightOutline = Color(0xFF7E8B85)
private val NightOutlineVariant = Color(0xFF343E41)
private val NightOnSurface = Color(0xFFE8ECE6)
private val NightOnSurfaceVariant = Color(0xFFA9B4AE)
private val NightPrimary = Color(0xFFC3CFC6)
private val NightOnPrimary = Color(0xFF17201B)
private val NightPrimaryContainer = Color(0xFF2E3A34)
private val NightOnPrimaryContainer = Color(0xFFDCE8DF)
private val NightGlass = Color(0xFF63D3FF)
private val NightGlassContainer = Color(0xFF0E3A4A)
private val NightMoon = Color(0xFFF0E0C4)
private val NightOnMoon = Color(0xFF2B241A)
private val NightMoonContainer = Color(0xFF3A3122)
private val NightParchment = Color(0xFFE9DFCB)
private val NightInkMuted = Color(0xFF645C4F)
private val NightError = Color(0xFFFFB4A8)
private val NightErrorContainer = Color(0xFF5C1A12)
private val NightOnErrorContainer = Color(0xFFFFDAD4)
private val NightGoogleButtonContainer = Color(0xFF131314)
private val NightGoogleButtonBorder = Color(0xFF8E918F)
private val NightGoogleButtonContent = Color(0xFFE3E3E3)

// Dawn
private val DawnBackground = Color(0xFFF6F4EE)
private val DawnDome = Color(0xFFD5E4E3)
private val DawnDomeGlow = Color(0xFFEEF5F3)
private val DawnSurface = Color(0xFFFFFFFF)
private val DawnSurfaceHigh = Color(0xFFEEF1EC)
private val DawnOutline = Color(0xFF6F7C75)
private val DawnOutlineVariant = Color(0xFFCBD3CE)
private val DawnOnSurface = Color(0xFF1A2320)
private val DawnOnSurfaceVariant = Color(0xFF4F5B55)
private val DawnPrimary = Color(0xFF3F5A4C)
private val DawnOnPrimary = Color(0xFFFFFFFF)
private val DawnPrimaryContainer = Color(0xFFD6E4DA)
private val DawnOnPrimaryContainer = Color(0xFF10261A)
private val DawnGlass = Color(0xFF006688)
private val DawnGlassContainer = Color(0xFFCBEFFF)
private val DawnMoon = Color(0xFF7A5A1E)
private val DawnOnMoon = Color(0xFFFFFFFF)
private val DawnMoonContainer = Color(0xFFF6E8CC)
private val DawnOnMoonContainer = Color(0xFF4A3510)
private val DawnParchment = Color(0xFFFFF9EC)
private val DawnParchmentBorder = Color(0xFFE6DCC6)
private val DawnInkMuted = Color(0xFF6B6459)
private val DawnError = Color(0xFFB3261E)
private val DawnErrorContainer = Color(0xFFFBDAD5)
private val DawnOnErrorContainer = Color(0xFF410E0B)
private val DawnGoogleButtonContainer = Color(0xFFFFFFFF)
private val DawnGoogleButtonBorder = Color(0xFF747775)
private val DawnGoogleButtonContent = Color(0xFF1F1F1F)

// shared by both themes
private val Ink = Color(0xFF2B241A)
private val InkLink = Color(0xFF006688)
private val InkError = Color(0xFFB3261E)
private val BrandSeed = Color(0xFF00C2FF)
private val TintAmber = Color(0xFFE8A93A)
private val TintMoonglass = Color(0xFFE9E4D6)
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)

// The bottle rig's own colours (TEMPLATE.md, bottle rig): illustration only, never text
private val RigScroll = Color(0xFFF0E0D0)
private val RigScrollEnds = Color(0xFFE0C0A0)
private val RigCork = Color(0xFFC9A27A)
private val RigCorkLines = Color(0xFFA9825A)
private val RigAmberStroke = Color(0xFFF0C987)

// Material's ColorScheme with every role filled from the palette, so its baseline purple never shows.
// The spec has no secondary or tertiary colours: secondary repeats the sage primary (several
// Material components default to the secondary roles) and tertiary is the moon set. surfaceVariant
// is the dome, the surfaceContainer roles are surfaceHigh (sheets, dialogs, the navigation bar) and
// the inverse roles come from TEMPLATE.md's snackbar. The scrim is opaque here: its alpha lives in
// BottlingColors.scrim.
internal val NightColorScheme: ColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    inversePrimary = DawnPrimary,
    secondary = NightPrimary,
    onSecondary = NightOnPrimary,
    secondaryContainer = NightPrimaryContainer,
    onSecondaryContainer = NightOnPrimaryContainer,
    tertiary = NightMoon,
    onTertiary = NightOnMoon,
    tertiaryContainer = NightMoonContainer,
    onTertiaryContainer = NightMoon,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightDome,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceTint = NightSurface,
    inverseSurface = NightOnSurface,
    inverseOnSurface = DawnOnSurface,
    error = NightError,
    onError = NightErrorContainer,
    errorContainer = NightErrorContainer,
    onErrorContainer = NightOnErrorContainer,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    scrim = Black,
    surfaceBright = NightSurfaceHigh,
    surfaceContainer = NightSurfaceHigh,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHigh,
    surfaceContainerLow = NightSurfaceHigh,
    surfaceContainerLowest = NightBackground,
    surfaceDim = NightBackground,
    primaryFixed = DawnPrimaryContainer,
    primaryFixedDim = NightPrimary,
    onPrimaryFixed = DawnOnPrimaryContainer,
    onPrimaryFixedVariant = NightPrimaryContainer,
    secondaryFixed = DawnPrimaryContainer,
    secondaryFixedDim = NightPrimary,
    onSecondaryFixed = DawnOnPrimaryContainer,
    onSecondaryFixedVariant = NightPrimaryContainer,
    tertiaryFixed = DawnMoonContainer,
    tertiaryFixedDim = NightMoon,
    onTertiaryFixed = DawnOnMoonContainer,
    onTertiaryFixedVariant = NightMoonContainer,
)

internal val DawnColorScheme: ColorScheme = lightColorScheme(
    primary = DawnPrimary,
    onPrimary = DawnOnPrimary,
    primaryContainer = DawnPrimaryContainer,
    onPrimaryContainer = DawnOnPrimaryContainer,
    inversePrimary = NightPrimary,
    secondary = DawnPrimary,
    onSecondary = DawnOnPrimary,
    secondaryContainer = DawnPrimaryContainer,
    onSecondaryContainer = DawnOnPrimaryContainer,
    tertiary = DawnMoon,
    onTertiary = DawnOnMoon,
    tertiaryContainer = DawnMoonContainer,
    onTertiaryContainer = DawnOnMoonContainer,
    background = DawnBackground,
    onBackground = DawnOnSurface,
    surface = DawnSurface,
    onSurface = DawnOnSurface,
    surfaceVariant = DawnDome,
    onSurfaceVariant = DawnOnSurfaceVariant,
    surfaceTint = DawnSurface,
    inverseSurface = DawnOnSurface,
    inverseOnSurface = NightOnSurface,
    error = DawnError,
    onError = DawnOnPrimary,
    errorContainer = DawnErrorContainer,
    onErrorContainer = DawnOnErrorContainer,
    outline = DawnOutline,
    outlineVariant = DawnOutlineVariant,
    scrim = DawnOnSurface,
    surfaceBright = DawnSurface,
    surfaceContainer = DawnSurfaceHigh,
    surfaceContainerHigh = DawnSurfaceHigh,
    surfaceContainerHighest = DawnSurfaceHigh,
    surfaceContainerLow = DawnSurfaceHigh,
    surfaceContainerLowest = DawnSurface,
    surfaceDim = DawnBackground,
    primaryFixed = DawnPrimaryContainer,
    primaryFixedDim = NightPrimary,
    onPrimaryFixed = DawnOnPrimaryContainer,
    onPrimaryFixedVariant = NightPrimaryContainer,
    secondaryFixed = DawnPrimaryContainer,
    secondaryFixedDim = NightPrimary,
    onSecondaryFixed = DawnOnPrimaryContainer,
    onSecondaryFixedVariant = NightPrimaryContainer,
    tertiaryFixed = DawnMoonContainer,
    tertiaryFixedDim = NightMoon,
    onTertiaryFixed = DawnOnMoonContainer,
    onTertiaryFixedVariant = NightMoonContainer,
)

/**
 * The spec's tokens that Material's [ColorScheme] has no role for (spec §1.2), read through
 * [BottlingTheme.colors]. Use [androidx.compose.material3.MaterialTheme.colorScheme] for the roles
 * it does have.
 */
@Immutable
data class BottlingColors(
    /** True for Night, false for Dawn; for scenery whose per-element alphas differ by theme. */
    val isNight: Boolean,
    /** The arc and dome fill. */
    val dome: Color,
    /** Radial moonlight at the top of the dome; decorative only. */
    val domeGlow: Color,
    /** Sheets, dialogs, the navigation bar, the stats card, toasts and snackbars. */
    val surfaceHigh: Color,
    /** Links, progress rings, ripples, "Drifting" and the focus ring. */
    val glass: Color,
    val glassContainer: Color,
    val onGlassContainer: Color,
    /** The treasure accent: overlines, the wax seal, collected badges. */
    val moon: Color,
    val onMoon: Color,
    val moonContainer: Color,
    val onMoonContainer: Color,
    /** The moon disc: moon at 85% on Night, a pale sun-moon on Dawn (decisions.md, 2026-10-13). */
    val moonDisc: Color,
    /** The 1dp line tracing the dome's arc. */
    val moonpath: Color,
    /** Stars and glints, before each element's own alpha. */
    val sceneryAccent: Color,
    /** The message card only. */
    val parchment: Color,
    /** The parchment's 1dp edge: a moonlit rim outside the card on Night, a border on Dawn. */
    val parchmentEdge: Color,
    /** Text on parchment. */
    val ink: Color,
    /** Bylines, placeholders, counters and chip borders on parchment. */
    val inkMuted: Color,
    /** Links and the cursor on parchment. */
    val inkLink: Color,
    /** An error on parchment, such as Compose's minimum (spec §3.4): 4.5:1 or more on both parchments. */
    val inkError: Color,
    /** The bottle's glass fill and the notification accent. Never text. */
    val brandSeed: Color,
    /** Bottle tints: illustration only. */
    val tintSeaGlass: Color,
    val tintAmber: Color,
    val tintMoonglass: Color,
    /** Behind sheets and dialogs, alpha included. */
    val scrim: Color,
    /** "Continue with Google", following Google's brand guidelines. */
    val googleButtonContainer: Color,
    val googleButtonBorder: Color,
    val googleButtonContent: Color,
    /** The bottle rig's parts. */
    val rig: BottleRigColors,
)

/**
 * The bottle rig's colours (spec §1.6, TEMPLATE.md's rig): a cream scroll with an amber ribbon, a
 * tan cork and white highlights. The glass takes its fill from the bottle's tint.
 */
@Immutable
data class BottleRigColors(
    val scroll: Color,
    val scrollEnds: Color,
    val ribbon: Color,
    val cork: Color,
    val corkLines: Color,
    val highlight: Color,
    /** The lip of the neck, where the cork sits. */
    val neckLip: Color,
    /** The glass's edge for each tint. */
    val seaGlassStroke: Color,
    val amberStroke: Color,
    val moonglassStroke: Color,
    /**
     * Dawn's 1dp outline in place of the tint's stroke: the glass is only 1.9:1 on the light sand,
     * and a tappable bottle needs 3:1 (spec §4). Null on Night.
     */
    val outline: Color?,
)

private fun bottleRigColors(outline: Color?) = BottleRigColors(
    scroll = RigScroll,
    scrollEnds = RigScrollEnds,
    ribbon = TintAmber,
    cork = RigCork,
    corkLines = RigCorkLines,
    highlight = White,
    neckLip = NightGlass,
    seaGlassStroke = NightGlass,
    amberStroke = RigAmberStroke,
    moonglassStroke = White,
    outline = outline,
)

internal val NightBottlingColors = BottlingColors(
    isNight = true,
    dome = NightDome,
    domeGlow = NightDomeGlow,
    surfaceHigh = NightSurfaceHigh,
    glass = NightGlass,
    glassContainer = NightGlassContainer,
    onGlassContainer = NightGlass,
    moon = NightMoon,
    onMoon = NightOnMoon,
    moonContainer = NightMoonContainer,
    onMoonContainer = NightMoon,
    moonDisc = NightMoon.copy(alpha = 0.85f),
    moonpath = NightMoon.copy(alpha = 0.25f),
    sceneryAccent = NightMoon,
    parchment = NightParchment,
    parchmentEdge = NightMoon.copy(alpha = 0.2f),
    ink = Ink,
    inkMuted = NightInkMuted,
    inkLink = InkLink,
    inkError = InkError,
    brandSeed = BrandSeed,
    tintSeaGlass = BrandSeed,
    tintAmber = TintAmber,
    tintMoonglass = TintMoonglass,
    scrim = Black.copy(alpha = 0.6f),
    googleButtonContainer = NightGoogleButtonContainer,
    googleButtonBorder = NightGoogleButtonBorder,
    googleButtonContent = NightGoogleButtonContent,
    rig = bottleRigColors(outline = null),
)

internal val DawnBottlingColors = BottlingColors(
    isNight = false,
    dome = DawnDome,
    domeGlow = DawnDomeGlow,
    surfaceHigh = DawnSurfaceHigh,
    glass = DawnGlass,
    glassContainer = DawnGlassContainer,
    onGlassContainer = DawnGlass,
    moon = DawnMoon,
    onMoon = DawnOnMoon,
    moonContainer = DawnMoonContainer,
    onMoonContainer = DawnOnMoonContainer,
    moonDisc = DawnMoonContainer,
    moonpath = DawnGlass.copy(alpha = 0.3f),
    sceneryAccent = DawnGlass,
    parchment = DawnParchment,
    parchmentEdge = DawnParchmentBorder,
    ink = Ink,
    inkMuted = DawnInkMuted,
    inkLink = InkLink,
    inkError = InkError,
    brandSeed = BrandSeed,
    tintSeaGlass = BrandSeed,
    tintAmber = TintAmber,
    tintMoonglass = TintMoonglass,
    scrim = DawnOnSurface.copy(alpha = 0.4f),
    googleButtonContainer = DawnGoogleButtonContainer,
    googleButtonBorder = DawnGoogleButtonBorder,
    googleButtonContent = DawnGoogleButtonContent,
    rig = bottleRigColors(outline = DawnGlass),
)

internal val LocalBottlingColors = staticCompositionLocalOf<BottlingColors> {
    error("No BottlingColors provided: wrap the content in BottlingTheme")
}
