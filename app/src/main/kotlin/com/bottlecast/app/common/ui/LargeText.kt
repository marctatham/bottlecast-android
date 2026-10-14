package com.bottlecast.app.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity

/** The font scale from which large text takes over the layout. */
const val LARGE_TEXT_SCALE = 1.75f

/**
 * Whether the text is large enough, from 175%, for decoration to step aside for the words: the
 * Shore's bottle and glints (spec §3.3 hides the bottle at 200%), and Reveal's card cap (spec §4).
 */
@Composable
@ReadOnlyComposable
fun isLargeText(): Boolean = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
