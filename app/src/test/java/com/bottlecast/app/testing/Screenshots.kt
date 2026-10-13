package com.bottlecast.app.testing

import android.graphics.Bitmap
import android.graphics.Canvas
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
 * Captures the whole screen, popups included, by drawing each of the composition's windows where
 * it sits on screen: the activity's first, then popups such as an open DropdownMenu, which
 * Roborazzi's screen capture leaves out.
 */
internal fun ComposeContentTestRule.captureScreenByDrawing() {
    val windows = onAllNodes(isRoot()).fetchSemanticsNodes()
        .map { (it.root as ViewRootForTest).view.rootView }
        .distinct()
    val screen = windows.first()
    val screenBitmap = Bitmap.createBitmap(screen.width, screen.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(screenBitmap)
    val screenLocation = IntArray(2).also { screen.getLocationOnScreen(it) }
    windows.forEach { window ->
        val location = IntArray(2).also { window.getLocationOnScreen(it) }
        canvas.save()
        canvas.translate((location[0] - screenLocation[0]).toFloat(), (location[1] - screenLocation[1]).toFloat())
        window.draw(canvas)
        canvas.restore()
    }
    screenBitmap.captureRoboImage()
}
