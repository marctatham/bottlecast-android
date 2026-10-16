package com.bottlecast.app.common.components.bottle

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.bottlecast.app.common.ui.BottleRigColors
import com.bottlecast.app.common.ui.BottlingColors
import java.security.MessageDigest

/** The three bottle shapes a message's seed picks between (spec §1.6, G9). None is rarer than another. */
enum class BottleSilhouette { Classic, Flask, Gourd }

/** The writer's glass colour. Slice 2's bottles are all sea glass; the choice comes later. */
enum class BottleTint {
    SeaGlass, Amber, Moonglass;

    internal fun fill(colors: BottlingColors): Color = when (this) {
        SeaGlass -> colors.tintSeaGlass
        Amber -> colors.tintAmber
        Moonglass -> colors.tintMoonglass
    }

    internal fun stroke(rig: BottleRigColors): Color = when (this) {
        SeaGlass -> rig.seaGlassStroke
        Amber -> rig.amberStroke
        Moonglass -> rig.moonglassStroke
    }
}

/** How a bottle looks: its seeded silhouette, and its tint turned by the seed's hue jitter. */
@Immutable
data class BottleLook(
    val silhouette: BottleSilhouette = BottleSilhouette.Classic,
    val tint: BottleTint = BottleTint.SeaGlass,
    /** Degrees, from -6 to +6. */
    val hueJitter: Float = 0f,
) {
    companion object {
        /** A bottle before its message is known, such as the Shore's. */
        val Default = BottleLook()
    }
}

/**
 * A message's one-of-a-kind bottle (spec §1.6, G9), seeded by SHA-256 of its id: the first byte,
 * mod 3, picks the silhouette, and the second turns the tint's hue by (byte / 255) × 12 − 6 degrees.
 * The keepsake's on-chain SVG and the web pages port this function, so it must never change.
 */
fun bottleLookFor(messageId: String, tint: BottleTint = BottleTint.SeaGlass): BottleLook {
    val seed = MessageDigest.getInstance("SHA-256").digest(messageId.toByteArray(Charsets.UTF_8))
    val silhouette = BottleSilhouette.entries[(seed[0].toInt() and 0xFF) % BottleSilhouette.entries.size]
    val hueJitter = (seed[1].toInt() and 0xFF) / 255f * HUE_SPREAD - HUE_SPREAD / 2
    return BottleLook(silhouette, tint, hueJitter)
}

private const val HUE_SPREAD = 12f

/** Turns the colour's hue by [degrees], keeping its saturation, brightness and alpha. */
fun Color.rotateHue(degrees: Float): Color {
    if (degrees == 0f) return this
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta == 0f) return this
    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return Color.hsv(((hue + degrees) % 360f + 360f) % 360f, delta / max, max, alpha)
}
