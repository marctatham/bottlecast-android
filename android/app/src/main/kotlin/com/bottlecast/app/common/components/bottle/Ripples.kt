package com.bottlecast.app.common.components.bottle

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * The glass rings on the water under a floating bottle (TEMPLATE.md's ripples): two rings and a
 * faint pool, in a 240 × 40 viewBox. Decorative.
 */
@Composable
fun Ripples(modifier: Modifier = Modifier) {
    val glass = BottlingTheme.colors.glass
    Spacer(
        modifier = modifier
            .aspectRatio(RIPPLES_WIDTH / RIPPLES_HEIGHT)
            .clearAndSetSemantics {}
            .drawWithCache {
                val scale = size.width / RIPPLES_WIDTH
                val ring = Stroke(width = RING_WIDTH)
                onDrawBehind {
                    scale(scale, scale, pivot = Offset.Zero) {
                        drawOval(glass.copy(alpha = OUTER_ALPHA), topLeft = Offset(10f, 8f), size = Size(220f, 24f), style = ring)
                        drawOval(glass.copy(alpha = INNER_ALPHA), topLeft = Offset(50f, 13f), size = Size(140f, 14f), style = ring)
                        drawOval(glass.copy(alpha = POOL_ALPHA), topLeft = Offset(86f, 16f), size = Size(68f, 8f))
                    }
                }
            }
    )
}

private const val RIPPLES_WIDTH = 240f
private const val RIPPLES_HEIGHT = 40f
private const val RING_WIDTH = 2f
private const val OUTER_ALPHA = 0.25f
private const val INNER_ALPHA = 0.45f
private const val POOL_ALPHA = 0.18f
