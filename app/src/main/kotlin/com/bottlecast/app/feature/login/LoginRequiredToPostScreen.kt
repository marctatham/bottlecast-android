package com.bottlecast.app.feature.login

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.bottlecast.app.R
import com.bottlecast.app.common.components.EducationalScreen
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.NightDawnPreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRequiredToPostScreen(
    onProceedHandler: () -> Unit,
    onCancelHandler: () -> Unit,
    onBackHandler: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottlingSheetState()
    var showBottomSheet: Boolean by remember { mutableStateOf(false) }

    val bottomSheetExpand: () -> Unit = { showBottomSheet = true }
    val bottomSheetCollapse: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
            }
        }
    }

    EducationalScreen(
        image = ImageVector.vectorResource(R.drawable.ic_symbol_account_circle),
        imageDescription = R.string.login_required_image_description,
        title = R.string.login_required_title,
        description = R.string.login_required_description,
        onBackHandler = onBackHandler,
        onPrimaryTapHandler = bottomSheetExpand,
        primaryButtonText = R.string.login_required_button_primary,
        secondaryButtonText = R.string.login_required_button_secondary,
        onSecondaryTapHandler = onCancelHandler,
    )

    if (showBottomSheet) {
        LoginBottomSheet(
            onDismissRequest = bottomSheetCollapse,
            sheetState = sheetState,
            onSignInCompleteHandler = onProceedHandler
        )
    }
}

@NightDawnPreviews
@Composable
private fun LoginRequiredToPostScreenPreview() = BottlingPreview {
    LoginRequiredToPostScreen(
        onBackHandler = { },
        onProceedHandler = { },
        onCancelHandler = { },
    )
}
