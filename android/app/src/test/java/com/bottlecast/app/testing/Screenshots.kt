package com.bottlecast.app.testing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.bottlecast.app.common.ui.BottlingTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.roundToInt

/**
 * Captures [content] in the Night or Dawn theme as a golden named after the calling test.
 *
 * It goes through the compose rule rather than `captureRoboImage { }`, and draws the window itself
 * (see [captureByDrawing]), because both Roborazzi paths come out blank for some tests depending
 * on what ran before them in the same JVM.
 */
internal fun ComposeContentTestRule.captureThemed(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    setContent {
        BottlingTheme(darkTheme = darkTheme) {
            Box(modifier = Modifier.testTag(CAPTURE_TAG)) { content() }
        }
    }
    waitForIdle()
    onNodeWithTag(CAPTURE_TAG).captureByDrawing()
}

/**
 * Captures this node by drawing its window into a bitmap and cropping to the node. Roborazzi's own
 * node capture copies the window with PixelCopy, which under Robolectric intermittently returns
 * the bare window background instead of the Compose content; drawing the window is deterministic.
 */
internal fun SemanticsNodeInteraction.captureByDrawing() {
    val node = fetchSemanticsNode()
    val window = (node.root as ViewRootForTest).view.rootView
    val windowBitmap = Bitmap.createBitmap(window.width, window.height, Bitmap.Config.ARGB_8888)
    window.draw(Canvas(windowBitmap))

    val bounds = node.boundsInWindow
    val left = bounds.left.roundToInt().coerceIn(0, window.width - 1)
    val top = bounds.top.roundToInt().coerceIn(0, window.height - 1)
    val width = bounds.width.roundToInt().coerceIn(1, window.width - left)
    val height = bounds.height.roundToInt().coerceIn(1, window.height - top)
    Bitmap.createBitmap(windowBitmap, left, top, width, height).captureRoboImage()
}

private const val CAPTURE_TAG = "screenshot"

/**
 * Captures the whole screen by drawing each of the composition's windows where it sits: the
 * activity's first, then any dialog, sheet or popup over it, with the dim a dialog's window asks
 * the system to lay behind it. Roborazzi's screen capture copies the screen with PixelCopy, which
 * leaves popups out and sometimes misses the content behind a dialog; drawing is deterministic.
 */
internal fun ComposeContentTestRule.captureScreenByDrawing() {
    val windows = onAllNodes(isRoot()).fetchSemanticsNodes()
        .map { (it.root as ViewRootForTest).view.rootView }
        .distinct()
    val screen = windows.first()
    val screenBitmap = Bitmap.createBitmap(screen.width, screen.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(screenBitmap)
    windows.forEach { window ->
        val params = window.layoutParams as? WindowManager.LayoutParams
        val frame = Rect(0, 0, window.width, window.height)
        if (window != screen && params != null) {
            if (params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0) {
                canvas.drawColor(Color.argb((params.dimAmount * OPAQUE).roundToInt(), 0, 0, 0))
            }
            // placed by its gravity, as the window manager would; Robolectric reports a centred
            // dialog at the top-left
            Gravity.apply(params.gravity, window.width, window.height, Rect(0, 0, screen.width, screen.height), params.x, params.y, frame)
        }
        canvas.save()
        canvas.translate(frame.left.toFloat(), frame.top.toFloat())
        window.draw(canvas)
        canvas.restore()
    }
    screenBitmap.captureRoboImage()
}

private const val OPAQUE = 255
