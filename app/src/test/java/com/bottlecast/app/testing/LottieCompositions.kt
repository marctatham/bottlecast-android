package com.bottlecast.app.testing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.RawRes
import androidx.core.graphics.scale
import androidx.test.core.app.ApplicationProvider
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory

/**
 * Loads a Lottie composition synchronously for a screenshot test, so the capture can't come out
 * blank. rememberLottieComposition both parses on a background thread and decodes the images
 * embedded as base64; the synchronous factory only parses, so the images are decoded here the
 * same way, or image layers (the bottle, its scroll, the water) wouldn't draw.
 */
internal fun loadLottieComposition(@RawRes res: Int): LottieComposition {
    val context: Context = ApplicationProvider.getApplicationContext()
    val composition = requireNotNull(LottieCompositionFactory.fromRawResSync(context, res).value)
    composition.images.values
        .filter { it.bitmap == null && it.fileName.startsWith("data:") && it.fileName.contains("base64,") }
        .forEach { asset ->
            val bytes = Base64.decode(asset.fileName.substringAfter("base64,"), Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inScaled = true
                inDensity = DEFAULT_DENSITY
            }
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            asset.bitmap = if (bitmap.width == asset.width && bitmap.height == asset.height) {
                bitmap
            } else {
                bitmap.scale(asset.width, asset.height)
            }
        }
    return composition
}

// lottie-compose decodes embedded images at mdpi
private const val DEFAULT_DENSITY = 160
