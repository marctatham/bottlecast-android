package com.bottlecast.app.common.ui

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext

/**
 * How long a timed control, such as an Undo, stays up (spec §4): at least [minimumMillis], or the
 * timeout the user asked accessibility settings for when that's longer. With TalkBack on, a control
 * with an action doesn't time out (null), so it waits for the user.
 */
fun timedControlMillis(
    minimumMillis: Long,
    hasAction: Boolean,
    touchExplorationEnabled: Boolean,
    recommendedMillis: (Long) -> Long,
): Long? = if (touchExplorationEnabled && hasAction) {
    null
} else {
    maxOf(minimumMillis, recommendedMillis(minimumMillis))
}

/** [timedControlMillis] for this screen, following TalkBack as it's turned on and off. */
@Composable
fun rememberTimedControlMillis(minimumMillis: Long, hasAction: Boolean): Long? {
    val accessibilityManager = LocalAccessibilityManager.current
    val touchExploration = rememberTouchExplorationEnabled()
    return remember(accessibilityManager, touchExploration, minimumMillis, hasAction) {
        timedControlMillis(minimumMillis, hasAction, touchExploration) { millis ->
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                millis,
                containsIcons = false,
                containsText = true,
                containsControls = hasAction,
            ) ?: millis
        }
    }
}

/** Whether TalkBack (or another touch-exploration service) is on. */
@Composable
fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

/** The spec's shortest undo window (spec §3.7: the Reported bar's [Undo] stays 5s). */
const val UNDO_WINDOW_MILLIS = 5_000L
