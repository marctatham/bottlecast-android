package com.bottlecast.app.common.ui

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Spacing used across features, read through [BottlingTheme.spacing]. The layouts use a 16dp side
 * gutter (spec, 360×800 reference frame).
 */
object BottlingSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** The side gutter of every screen. */
    val gutter = 16.dp
}

/**
 * Component sizes used across features (spec §1.4, TEMPLATE.md §4), read through
 * [BottlingTheme.sizes]. Sizes only one feature uses belong in that feature's own tokens.
 * Heights are minimums: components grow with the text (spec §4).
 */
object BottlingSizes {
    val icon = 24.dp
    val iconSmall = 18.dp

    /** The pills' leading icon and the gap after it. */
    val pillIcon = 20.dp
    val pillIconGap = 8.dp
    val pillMinHeight = 56.dp

    val chipMinHeight = 32.dp
    val chipIconGap = 6.dp
    val chipPaddingStart = 8.dp
    val chipPaddingEnd = 12.dp

    val touchTarget = 48.dp
    val hairline = 1.dp
    val focusRing = 2.dp

    /** The moon on the Shore and compact domes, and Shore B's larger moon. */
    val moonDisc = 40.dp
    val moonDiscLarge = 56.dp

    /** Avatars: the Me tab's in the navigation bar, and the profile's. Plain circles. */
    val navAvatar = 24.dp
    val navAvatarGlyph = 16.dp
    val profileAvatar = 96.dp
    val profileAvatarGlyph = 48.dp
    val profileAvatarRim = 3.dp

    /** The "something's new" dot on a navigation bar icon, on the icon's top-end corner. */
    val badgeDot = 8.dp
    val badgeDotOffset = DpOffset(4.dp, (-1).dp)

    /** The leading icon of a banner (spec §3.18). */
    val bannerIcon = 20.dp

    /** An edge or empty state's spot illustration (spec §1.6, §3.18). */
    val spotIllustration = 160.dp

    /** The corners a full-screen route gains as a back swipe shrinks it away (spec §1.5). */
    val predictiveBackCorner = 32.dp
}
