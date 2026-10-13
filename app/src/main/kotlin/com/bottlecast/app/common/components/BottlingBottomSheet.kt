package com.bottlecast.app.common.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * A modal bottom sheet in the Moonlit Shore style: surfaceHigh, 32dp top corners, the theme's scrim
 * and an outline-coloured drag handle (spec §1.2, §1.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottlingBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = BottlingTheme.shapes.sheet,
        containerColor = BottlingTheme.colors.surfaceHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = BottlingTheme.colors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
        content = content,
    )
}

/** The state for a [BottlingBottomSheet]: hidden or fully expanded, never half open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberBottlingSheetState(): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
)
