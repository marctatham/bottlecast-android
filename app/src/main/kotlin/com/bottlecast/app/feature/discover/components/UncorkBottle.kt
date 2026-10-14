package com.bottlecast.app.feature.discover.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import com.bottlecast.app.R
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.BottleRig
import com.bottlecast.app.common.components.bottle.BottleRigPose
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.discover.DiscoverDimens

/**
 * Today's bottle on its sand mound, with the cork ring round its neck (spec §3.6). It's a button:
 * tap it to loosen the cork a third, or press and hold to fill the ring. TalkBack, Switch Access
 * and keyboards open it in one go.
 *
 * @param steps how many shakes and taps have loosened the cork, for TalkBack.
 * @param showRing false once the cork is out.
 */
@Composable
internal fun UncorkBottle(
    look: BottleLook,
    steps: Int,
    ringProgress: () -> Float,
    motion: DiscoverMotion,
    showRing: Boolean,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    onCancelled: () -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    waveSpeed: Dp = WavyProgressIndicatorDefaults.CircularWavelength,
) {
    val currentOnPressed by rememberUpdatedState(onPressed)
    val currentOnReleased by rememberUpdatedState(onReleased)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnActivate by rememberUpdatedState(onActivate)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val label = stringResource(R.string.discover_bottle)
    val action = stringResource(R.string.discover_bottle_action)
    val loosening = stringResource(R.string.discover_cork_loosening, steps)
    // the neck leans towards the start edge, so the cork flies off up and to the start
    val tilt = if (LocalLayoutDirection.current == LayoutDirection.Ltr) DiscoverDimens.BOTTLE_TILT else -DiscoverDimens.BOTTLE_TILT
    val mirrored = LocalLayoutDirection.current == LayoutDirection.Rtl
    val bottleWidth = DiscoverDimens.bottleHeight * RIG_ASPECT

    Box(
        modifier = modifier.size(width = DiscoverDimens.moundWidth, height = DiscoverDimens.bottleHeight + DiscoverDimens.moundHeight / 2),
        contentAlignment = Alignment.TopCenter,
    ) {
        SandMound(modifier = Modifier.align(Alignment.BottomCenter))
        Box(
            modifier = Modifier
                .width(bottleWidth)
                .height(DiscoverDimens.bottleHeight)
                .graphicsLayer {
                    rotationZ = tilt + motion.wobble()
                    translationY = -DiscoverDimens.bobHeight.toPx() * motion.bob()
                    scaleX = if (mirrored) -1f else 1f
                    transformOrigin = TransformOrigin(0.5f, PIVOT_Y)
                }
                .then(
                    if (focused) {
                        Modifier.border(BottlingTheme.sizes.focusRing, BottlingTheme.colors.glass, BottlingTheme.shapes.card)
                    } else {
                        Modifier
                    }
                )
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = label
                    if (steps > 0) stateDescription = loosening
                    onClick(label = action) {
                        currentOnActivate()
                        true
                    }
                }
                .focusable(interactionSource = interactionSource)
                .onKeyEvent { event ->
                    val activates = event.type == KeyEventType.KeyUp && event.key in ActivationKeys
                    if (activates) currentOnActivate()
                    activates
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        currentOnPressed()
                        // a press that ends anywhere but a lift on the bottle (a scroll took it, it was
                        // dragged off, the bottle left the screen) is never a tap
                        if (waitForUpOrCancellation() != null) currentOnReleased() else currentOnCancelled()
                    }
                },
        ) {
            BottleRig(
                look = look,
                pose = BottleRigPose(corkFlight = motion.corkFlight, scrollOut = motion.scrollOut, shimmer = motion.shimmer),
            )
            if (showRing) {
                CorkRing(
                    progress = ringProgress,
                    waveSpeed = waveSpeed,
                    // centred on the neck, which is at the rig's middle, 47 of its 220 units down
                    modifier = Modifier.offset(
                        x = bottleWidth / 2 - DiscoverDimens.corkRing / 2,
                        y = DiscoverDimens.bottleHeight * NECK_Y - DiscoverDimens.corkRing / 2,
                    ),
                )
            }
        }
    }
}

/** A faint mound of sand: the sea's colour pooled under the bottle, fading out to the dome. */
@Composable
private fun SandMound(modifier: Modifier = Modifier) {
    val sand = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier.size(width = DiscoverDimens.moundWidth, height = DiscoverDimens.moundHeight)) {
        drawOval(
            brush = Brush.radialGradient(
                0f to sand.copy(alpha = MOUND_ALPHA),
                1f to sand.copy(alpha = 0f),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.width / 2f,
            ),
        )
    }
}

private val ActivationKeys = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter)

// the rig's 140 × 220 viewBox
private const val RIG_ASPECT = 140f / 220f
private const val NECK_Y = 47f / 220f

// the bottle leans and wobbles about a point just above its base
private const val PIVOT_Y = 0.85f

private const val MOUND_ALPHA = 0.7f
