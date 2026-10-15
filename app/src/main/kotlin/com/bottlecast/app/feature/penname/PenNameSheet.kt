package com.bottlecast.app.feature.penname

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.BottlingBottomSheet
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonSize
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * The pen-name sheet (spec §3.2): "Your pen name", a field, [Surprise me] and [Save]. It opens from the
 * signed-in snackbar's "Pick one" and from Preview's "Set a pen name…", and hands the saved name to
 * [onSaved].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenNameSheet(
    onDismissRequest: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: PenNameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val tracker = LocalTracker.current
    val sheetState = rememberBottlingSheetState()
    val currentOnSaved by rememberUpdatedState(onSaved)
    val suggestions = stringArrayResource(R.array.pen_name_suggestions).toList()

    // The next opening starts afresh, whatever this one ended on. A rotation isn't a new opening.
    val activity = LocalActivity.current
    DisposableEffect(viewModel) { onDispose { if (activity?.isChangingConfigurations != true) viewModel.reset() } }

    LaunchedEffect(uiState) {
        val saved = uiState as? PenNameUiState.Saved ?: return@LaunchedEffect
        tracker.trackEvent(
            AnalyticsEvent.PenNameSaved,
            mapOf(AnalyticsEvent.PARAM_SOURCE to if (saved.suggested) "surprise" else "typed"),
        )
        sheetState.hide()
        currentOnSaved(saved.name)
    }

    PenNameSheet(
        uiState = uiState as? PenNameUiState.Editing ?: PenNameUiState.Editing(),
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        onNameChange = viewModel::onNameChange,
        onSurpriseMe = { viewModel.onSurpriseMe(suggestions) },
        onSave = viewModel::onSave,
    )
}

/** The sheet itself, for previews and screenshot tests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PenNameSheet(
    uiState: PenNameUiState.Editing,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onNameChange: (String) -> Unit,
    onSurpriseMe: () -> Unit,
    onSave: () -> Unit,
) {
    val spacing = BottlingTheme.spacing
    BottlingBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.l, end = spacing.l, bottom = spacing.l),
        ) {
            Text(
                text = stringResource(R.string.pen_name_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(spacing.m))
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.pen_name_label)) },
                supportingText = {
                    Text(stringResource(if (uiState.showError) R.string.pen_name_error else R.string.pen_name_helper))
                },
                isError = uiState.showError,
                singleLine = true,
                shape = BottlingTheme.shapes.field,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = BottlingTheme.colors.glass,
                    cursorColor = BottlingTheme.colors.glass,
                    focusedLabelColor = BottlingTheme.colors.glass,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
            )
            Spacer(modifier = Modifier.height(spacing.m))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                BottlingButton(
                    text = R.string.pen_name_surprise,
                    buttonType = BottlingButtonType.SECONDARY,
                    size = BottlingButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                    onTapHandler = onSurpriseMe,
                )
                BottlingButton(
                    text = R.string.pen_name_save,
                    size = BottlingButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                    onTapHandler = onSave,
                )
            }
        }
    }
}
