package com.bottlecast.app.feature.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.bottlecast.app.R
import com.bottlecast.app.common.components.ParchmentCard
import com.bottlecast.app.common.components.TideRing
import com.bottlecast.app.common.components.WaxSeal
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.BottleRig
import com.bottlecast.app.common.components.bottle.FloatingBottle
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.onboarding.OnboardingDimens

/**
 * One onboarding page's illustration (spec §3.1), drawn at [size] across. It's a picture with one
 * label, so it keeps its own proportions at any text size, words on the parchment included.
 */
@Composable
internal fun OnboardingArt(
    page: Int,
    size: Dp,
    motion: OnboardingMotion,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        when (page) {
            0 -> R.string.onboarding_1_art
            1 -> R.string.onboarding_2_art
            else -> R.string.onboarding_3_art
        }
    )
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Image
            },
        contentAlignment = Alignment.Center,
    ) {
        // drawn at the full 240dp, then scaled to fit, at the phone's density but no font scale
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            Box(
                modifier = Modifier
                    .requiredSize(OnboardingDimens.art)
                    .scale(size / OnboardingDimens.art),
                contentAlignment = Alignment.Center,
            ) {
                when (page) {
                    0 -> FloatingBottle(height = OnboardingDimens.bottleHeight, bob = motion.bob)
                    1 -> BottleOnSand(motion)
                    else -> SealedParchment(motion)
                }
            }
        }
    }
}

/** Page 2: the bottle on the sand under the moon, wrapped by a glass tide ring two-thirds full. */
@Composable
private fun BottleOnSand(motion: OnboardingMotion) {
    val sand = BottlingTheme.colors.moonContainer
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier = Modifier.requiredSize(OnboardingDimens.art)) {
        TideRing(
            dayElapsed = OnboardingDimens.TIDE_SHARE,
            haloAlpha = motion.haloAlpha,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(OnboardingDimens.sandWidth, OnboardingDimens.sandHeight)
                .clipToBounds(),
        ) {
            // a low dune: the top of a wide oval
            drawOval(color = sand, topLeft = Offset.Zero, size = Size(size.width, size.height * 2f))
        }
        BottleRig(
            look = BottleLook.Default,
            // resting on the dune, clear of the ring above
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(OnboardingDimens.lyingBottleHeight)
                .graphicsLayer {
                    // lying on its side with the neck towards the start, mirrored in right-to-left layouts
                    rotationZ = if (rtl) -OnboardingDimens.LYING_BOTTLE_TILT else OnboardingDimens.LYING_BOTTLE_TILT
                    scaleX = if (rtl) -1f else 1f
                },
        )
    }
}

/** Page 3: a kept message on parchment, and the wax seal stamping down on its corner. */
@Composable
private fun SealedParchment(motion: OnboardingMotion) {
    val colors = BottlingTheme.colors
    Box(modifier = Modifier.rotate(OnboardingDimens.PARCHMENT_TILT)) {
        ParchmentCard(modifier = Modifier.requiredWidth(OnboardingDimens.art)) {
            Text(
                text = stringResource(R.string.onboarding_3_message),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.ink,
            )
            Text(
                text = stringResource(R.string.message_byline_stranger),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                modifier = Modifier.padding(top = BottlingTheme.spacing.s),
            )
        }
        WaxSeal(
            size = OnboardingDimens.seal,
            stamp = motion.sealStamp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = OnboardingDimens.sealOverhangX, y = OnboardingDimens.sealOverhangY)
                .rotate(OnboardingDimens.SEAL_TILT),
        )
    }
}
