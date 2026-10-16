package com.bottlecast.app.feature.compose

import androidx.compose.ui.unit.dp

/** Sizes only Compose uses, from spec §3.4. */
internal object ComposeDimens {
    /** The parchment editor: 20dp in, at least 240dp tall, growing to fill. */
    val editorPadding = 20.dp
    val editorMinHeight = 240.dp

    /** The helper's icon, and the gap to the counter. */
    val helperIcon = 16.dp
}
