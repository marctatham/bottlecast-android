package com.bottlecast.app.common.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * The message on its parchment card (spec §3.7), as Reveal shows it and Preview previews it (spec
 * §3.4b): the text, a divider, and the [byline]. The card hugs the message from 220dp up to
 * [maxHeight], past which the message scrolls inside it, fading at its edges. With no [maxHeight]
 * (large text), the whole page scrolls instead.
 *
 * @param sheen how far the moonlight's sweep across the card has gone, from 0 to 1, or null.
 */
@Composable
fun MessageCard(
    text: String,
    byline: String,
    maxHeight: Dp?,
    modifier: Modifier = Modifier,
    sheen: () -> Float? = { null },
) {
    val colors = BottlingTheme.colors
    val scroll = rememberScrollState()
    val shape = BottlingTheme.shapes.parchment
    ParchmentCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CardMinHeight, max = maxHeight ?: Dp.Unspecified)
            .drawWithContent {
                drawContent()
                sheen()?.let { drawSheen(it, Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }, colors.moon) }
            },
        // as on the board, a short message leaves the byline at the foot of the card
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = if (maxHeight != null) {
                Modifier
                    .weight(1f, fill = false)
                    .fadingEdges(scroll, colors.parchment)
                    .verticalScroll(scroll)
            } else {
                Modifier
            },
        ) {
            val style = if (text.length > LONG_MESSAGE) {
                BottlingTheme.typography.titleLargeLong
            } else {
                MaterialTheme.typography.titleLarge
            }
            Text(
                text = text,
                // a message runs in its own script's direction, whichever way the app is laid out
                style = style.copy(textDirection = TextDirection.Content),
                color = colors.ink,
            )
        }
        Column(modifier = Modifier.padding(top = BottlingTheme.spacing.m)) {
            HorizontalDivider(thickness = BottlingTheme.sizes.hairline, color = colors.ink.copy(alpha = DIVIDER_ALPHA))
            Spacer(modifier = Modifier.height(BottlingTheme.spacing.s))
            Text(
                text = byline,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = colors.inkMuted,
            )
        }
    }
}

// Parchment-coloured fades where there's more message to scroll to.
private fun Modifier.fadingEdges(scroll: ScrollState, parchment: Color): Modifier = drawWithContent {
    drawContent()
    val fade = CardFade.toPx()
    if (scroll.canScrollBackward) {
        drawRect(Brush.verticalGradient(0f to parchment, 1f to Color.Transparent, startY = 0f, endY = fade))
    }
    if (scroll.canScrollForward) {
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 1f to parchment, startY = size.height - fade, endY = size.height),
            topLeft = Offset(0f, size.height - fade),
        )
    }
}

// One soft band of moonlight sweeping across the card, from the start edge to the end (spec §1.5).
private fun ContentDrawScope.drawSheen(progress: Float, card: Path, moon: Color) {
    val band = size.width * SHEEN_WIDTH
    val fromStart = -band + progress * (size.width + band * 2)
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - fromStart else fromStart
    clipPath(card) {
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                0.5f to moon.copy(alpha = SHEEN_ALPHA),
                1f to Color.Transparent,
                start = Offset(x - band, 0f),
                end = Offset(x + band, size.height),
            )
        )
    }
}

/** At least 220dp tall (spec §3.7). */
private val CardMinHeight = 220.dp

/** Messages longer than this use the smaller titleLargeLong (spec §3.7). */
private const val LONG_MESSAGE = 120

/** The fade at the top and bottom of a long message that scrolls inside the card. */
private val CardFade = 16.dp

private const val DIVIDER_ALPHA = 0.12f
private const val SHEEN_WIDTH = 0.35f
private const val SHEEN_ALPHA = 0.35f
