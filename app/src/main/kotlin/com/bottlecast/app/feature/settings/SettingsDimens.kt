package com.bottlecast.app.feature.settings

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only Settings uses, from spec §3.17, and the Settings board (390×1400) where the spec gives none. */
internal object SettingsDimens {
    /** The dome behind the top bar: its edge at y=108 of the board's 844-tall first screen. */
    val dome = DomeEdge(108f / 844f)

    /** A row is at least 56dp tall (spec §3.17). */
    val rowMinHeight = 56.dp

    /** The gap under a section's header, and between sections (Settings board). */
    val headerGap = 6.dp
    val sectionGap = 12.dp

    /** The leading icon's slot, and the account's avatar (Settings board). */
    val leadingSlot = 32.dp
    val accountAvatar = 32.dp
    val chevron = 20.dp

    /** The top bar reaches past the gutter, to 4dp from the screen's edges (the boards). */
    val topBarBleed = 12.dp
}
