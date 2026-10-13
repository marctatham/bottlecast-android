package com.bottlecast.app.common.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleThemeConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * The Moonlit Shore theme: Night when [darkTheme], Dawn otherwise, with dynamic colour off
 * (spec §1.1). It follows the system until Settings offers System / Night / Dawn (slice 4).
 */
@Composable
fun BottlingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) NightColorScheme else DawnColorScheme
    val colors = if (darkTheme) NightBottlingColors else DawnBottlingColors
    val effects = if (darkTheme) NightBottlingEffects else DawnBottlingEffects

    // the status bar itself is painted in MessageInABottleApp now that the app draws edge-to-edge,
    // so only its icon colour (contrasting with colorScheme.primary) is set here
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    CompositionLocalProvider(
        LocalBottlingColors provides colors,
        LocalBottlingEffects provides effects,
        // Night is tonal only: surfaces keep their exact token colours at any elevation
        LocalTonalElevationEnabled provides false,
        // the focus ring is 2dp of glass following the shape, over a 2dp ring in the background colour
        // so it reads on sage pills too (spec §4); touch ripples are glass (spec §1.2)
        LocalRippleThemeConfiguration provides InsetFocusRing,
        LocalRippleConfiguration provides RippleConfiguration(
            focus = RippleConfiguration.Focus.InsetRing(
                outerStrokeColor = colors.glass,
                innerStrokeColor = colorScheme.background,
            ),
            color = colors.glass,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = BottlingM3Shapes,
            typography = BottlingM3Typography,
            content = content,
        )
    }
}

private val InsetFocusRing = RippleThemeConfiguration(
    focus = RippleThemeConfiguration.Focus.InsetRing(
        outerStrokeInset = 0.dp,
        outerStrokeWidth = BottlingSizes.focusRing,
        innerStrokeInset = BottlingSizes.focusRing,
        innerStrokeWidth = BottlingSizes.focusRing,
    )
)

/**
 * The Moonlit Shore tokens that Material's theme has no slot for. Use
 * [MaterialTheme.colorScheme], [MaterialTheme.typography] and [MaterialTheme.shapes] for the
 * roles Material does have.
 */
object BottlingTheme {
    val colors: BottlingColors
        @Composable @ReadOnlyComposable get() = LocalBottlingColors.current

    val effects: BottlingEffects
        @Composable @ReadOnlyComposable get() = LocalBottlingEffects.current

    val typography: BottlingTypography = BottlingTypography
    val shapes: BottlingShapes = BottlingShapes
    val spacing: BottlingSpacing = BottlingSpacing
    val sizes: BottlingSizes = BottlingSizes
    val motion: BottlingMotion = BottlingMotion
}
