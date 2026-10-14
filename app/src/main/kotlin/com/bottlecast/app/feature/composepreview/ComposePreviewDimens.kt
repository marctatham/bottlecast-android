package com.bottlecast.app.feature.composepreview

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only Preview uses, from spec §3.4b. */
internal object ComposePreviewDimens {
    /** The compact dome: its edge at y≈260 of 844. */
    val dome = DomeEdge(260f / 844f)

    /** The bottle, 120dp, in the bottle's own tint. */
    val bottle = 120.dp

    /** The checklist's rows: at least 40dp, an icon and bodyMedium. */
    val checklistRow = 40.dp

    /** The settings' switch rows: at least 56dp (spec §3.17's rows), 72dp with a pen name on the board. */
    val settingRow = 56.dp
}
