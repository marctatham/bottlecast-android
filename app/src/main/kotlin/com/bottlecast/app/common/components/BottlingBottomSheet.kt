package com.bottlecast.app.common.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * A modal bottom sheet in the Moonlit Shore style: surfaceHigh, 32dp top corners, the theme's scrim
 * and an outline-coloured drag handle (spec §1.2, §1.4).
 *
 * @param dismissible false while the sheet is busy, such as signing in (spec §3.2): the handle goes,
 * and neither a swipe, back nor a tap on the scrim closes it. Its [sheetState] should also refuse
 * [SheetValue.Hidden] then.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottlingBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = dismissible,
        shape = BottlingTheme.shapes.sheet,
        containerColor = BottlingTheme.colors.surfaceHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = BottlingTheme.colors.scrim,
        dragHandle = if (dismissible) {
            { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        } else {
            null
        },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = dismissible,
            shouldDismissOnClickOutside = dismissible,
        ),
        content = content,
    )
}

/**
 * The state for a [BottlingBottomSheet]: hidden or fully expanded, never half open.
 *
 * @param canHide read whenever the sheet would close; false keeps it open while it's busy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberBottlingSheetState(canHide: () -> Boolean = { true }): SheetState {
    // the state keeps the first lambda it's given, so it reads the latest one through this
    val latestCanHide = rememberUpdatedState(canHide)
    return rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { it != SheetValue.Hidden || latestCanHide.value() },
    )
}
