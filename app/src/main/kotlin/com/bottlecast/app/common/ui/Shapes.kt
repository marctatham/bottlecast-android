package com.bottlecast.app.common.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// spec §1.4: pills are fully rounded; parchment 28dp; cards 24dp; fields, toasts, snackbars,
// banners and sheet rows 16dp; sheet tops and dialogs 32dp; focus outlines 4dp
internal val BottlingM3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(32.dp),
)

/**
 * The spec's shapes by purpose, read through [BottlingTheme.shapes]. Material's chips default to
 * [Shapes.small] and its snackbars and text fields to [Shapes.extraSmall], so pass these explicitly.
 */
object BottlingShapes {
    /** Buttons, chips, the streak pill and segmented buttons. */
    val pill: Shape = CircleShape

    /** Text fields, toasts, snackbars, banners and sheet rows. */
    val field: Shape = BottlingM3Shapes.small

    /** Cards, chest items, the stats card and list groups. */
    val card: Shape = BottlingM3Shapes.medium

    /** The message card. */
    val parchment: Shape = BottlingM3Shapes.large

    /** Dialogs. */
    val dialog: Shape = BottlingM3Shapes.extraLarge

    /** Bottom sheets: rounded top corners only. */
    val sheet: Shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}
