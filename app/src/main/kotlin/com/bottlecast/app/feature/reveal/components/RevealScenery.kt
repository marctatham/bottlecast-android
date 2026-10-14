package com.bottlecast.app.feature.reveal.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.BOARD_WIDTH
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.components.SceneryStar
import com.bottlecast.app.common.components.drawStars
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.reveal.RevealDimens

/** Reveal's scenery on the compact dome (Reveal board): three stars and the moon. Decorative. */
@Composable
internal fun RevealScenery(insets: PaddingValues, modifier: Modifier = Modifier) {
    val colors = BottlingTheme.colors
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawStars(Stars, colors) }
        MoonDisc(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = insets.calculateTopPadding() + RevealDimens.moonTop,
                    end = insets.calculateEndPadding(layoutDirection) + RevealDimens.moonEnd,
                ),
        )
    }
}

// the board's 2×2dp stars, by their centres
private val Stars = listOf(
    SceneryStar(45f / BOARD_WIDTH, 125.dp, 0.40f),
    SceneryStar(125f / BOARD_WIDTH, 87.dp, 0.30f),
    SceneryStar(353f / BOARD_WIDTH, 123.dp, 0.35f),
)

// the board's moon sits beside the flag, 18dp under the status bar
