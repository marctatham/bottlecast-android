package com.bottlecast.app.feature.reveal.components

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.bottlecast.app.R
import com.bottlecast.app.common.components.ParchmentCard
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.reveal.Byline
import com.bottlecast.app.feature.reveal.RevealDimens
import com.bottlecast.app.feature.reveal.RevealedBottle
import com.bottlecast.app.feature.reveal.domain.CastAgo

/**
 * The message on its parchment card (spec §3.7): the text, a divider, and the byline. The card hugs
 * the message from 220dp up to [maxHeight], past which the message scrolls inside it, fading at its
 * edges. With no [maxHeight] (large text), the whole page scrolls instead.
 *
 * @param sheen how far the moonlight's sweep across the card has gone, from 0 to 1, or null.
 */
@Composable
internal fun MessageCard(
    bottle: RevealedBottle,
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
            .heightIn(min = RevealDimens.cardMinHeight, max = maxHeight ?: Dp.Unspecified)
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
            val style = if (bottle.text.length > RevealDimens.LONG_MESSAGE) {
                BottlingTheme.typography.titleLargeLong
            } else {
                MaterialTheme.typography.titleLarge
            }
            Text(
                text = bottle.text,
                // a message runs in its own script's direction, whichever way the app is laid out
                style = style.copy(textDirection = TextDirection.Content),
                color = colors.ink,
            )
        }
        Column(modifier = Modifier.padding(top = BottlingTheme.spacing.m)) {
            HorizontalDivider(thickness = BottlingTheme.sizes.hairline, color = colors.ink.copy(alpha = DIVIDER_ALPHA))
            Spacer(modifier = Modifier.height(BottlingTheme.spacing.s))
            Text(
                text = bylineText(bottle),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = colors.inkMuted,
            )
        }
    }
}

/** "— A stranger at sea · cast 3 days ago", or the pen name the writer signed it with (spec §3.7). */
@Composable
private fun bylineText(bottle: RevealedBottle): String {
    val who = when (val byline = bottle.byline) {
        Byline.Stranger -> stringResource(R.string.reveal_byline_stranger)
        is Byline.PenName -> stringResource(R.string.reveal_byline_pen_name, byline.name)
    }
    val cast = when (val ago = bottle.castAgo) {
        null -> return who
        CastAgo.Today -> stringResource(R.string.reveal_cast_today)
        CastAgo.Yesterday -> stringResource(R.string.reveal_cast_yesterday)
        is CastAgo.Days -> pluralStringResource(R.plurals.reveal_cast_days, ago.days, ago.days)
        is CastAgo.Months -> pluralStringResource(R.plurals.reveal_cast_months, ago.months, ago.months)
        is CastAgo.Years -> pluralStringResource(R.plurals.reveal_cast_years, ago.years, ago.years)
    }
    return stringResource(R.string.reveal_byline_cast, who, cast)
}

// Parchment-coloured fades where there's more message to scroll to.
private fun Modifier.fadingEdges(scroll: ScrollState, parchment: Color): Modifier = drawWithContent {
    drawContent()
    val fade = RevealDimens.cardFade.toPx()
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

private const val DIVIDER_ALPHA = 0.12f
private const val SHEEN_WIDTH = 0.35f
private const val SHEEN_ALPHA = 0.35f
