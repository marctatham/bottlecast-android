package com.bottlecast.app.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The parchment card that carries a message (spec §1.2, §1.4). On Night it has a 1dp moonlit rim
 * outside its edge and a soft glass glow; on Dawn a 1dp border and a 2dp shadow. Content is ink.
 *
 * @param border replaces the theme's edge, e.g. with an `outline` border so an interactive card
 *   reaches 3:1 (spec §4).
 */
@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    shape: Shape = BottlingTheme.shapes.parchment,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(BottlingTheme.spacing.l),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BottlingTheme.colors
    val hairline = BottlingTheme.sizes.hairline
    val edge = border ?: if (colors.isNight) null else BorderStroke(hairline, colors.parchmentEdge)

    Column(
        modifier = modifier
            .dropShadow(shape, BottlingTheme.effects.parchmentGlow)
            .then(
                // the boards' `0 0 0 1px` rim sits outside the card; drawn inside it would be moon on
                // parchment, which has no contrast
                if (colors.isNight && border == null) {
                    Modifier.dropShadow(shape, Shadow(radius = 0.dp, color = colors.parchmentEdge, spread = hairline))
                } else {
                    Modifier
                }
            )
            .background(colors.parchment, shape)
            .then(if (edge != null) Modifier.border(edge, shape) else Modifier)
            .padding(contentPadding),
    ) {
        val scope = this
        CompositionLocalProvider(LocalContentColor provides colors.ink) { scope.content() }
    }
}

@NightDawnPreviews
@Composable
private fun ParchmentCardPreview() = BottlingPreview {
    ParchmentCard(modifier = Modifier.padding(BottlingTheme.spacing.l)) {
        Text(text = "Hoy el mar me devolvió algo que creía perdido: la calma.", style = MaterialTheme.typography.titleLarge)
    }
}
