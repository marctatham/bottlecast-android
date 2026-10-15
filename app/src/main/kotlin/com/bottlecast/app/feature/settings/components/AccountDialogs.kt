package com.bottlecast.app.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonSize
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.BottlingDialog
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/** "Sign out?" / "You'll go back to guest mode on this phone." [Cancel] [Sign out] (spec §3.17). */
@Composable
internal fun SignOutDialog(onDismissRequest: () -> Unit, onSignOut: () -> Unit) {
    AccountDialog(
        title = stringResource(R.string.settings_sign_out_title),
        body = stringResource(R.string.settings_sign_out_body),
        onDismissRequest = onDismissRequest,
    ) {
        BottlingButton(
            text = R.string.settings_sign_out_confirm,
            size = BottlingButtonSize.MEDIUM,
            onTapHandler = onSignOut,
        )
    }
}

/**
 * "Delete your account?" with what it removes and what stays, and "I understand this can't be undone",
 * which [Delete account] waits for (spec §3.17, G20).
 */
@Composable
internal fun DeleteAccountDialog(onDismissRequest: () -> Unit, onDelete: () -> Unit, startTicked: Boolean = false) {
    var understood by rememberSaveable { mutableStateOf(startTicked) }
    AccountDialog(
        title = stringResource(R.string.settings_delete_title),
        body = stringResource(R.string.settings_delete_body),
        onDismissRequest = onDismissRequest,
        above = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = BottlingTheme.sizes.touchTarget)
                    .toggleable(value = understood, role = Role.Checkbox, onValueChange = { understood = it }),
                horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = understood,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.error,
                        checkmarkColor = MaterialTheme.colorScheme.onError,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                    ),
                )
                Text(
                    text = stringResource(R.string.settings_delete_understand),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        BottlingButton(
            text = R.string.settings_delete_confirm,
            enabled = understood,
            buttonType = BottlingButtonType.DESTRUCTIVE,
            size = BottlingButtonSize.MEDIUM,
            onTapHandler = onDelete,
        )
    }
}

@Composable
private fun AccountDialog(
    title: String,
    body: String,
    onDismissRequest: () -> Unit,
    above: @Composable () -> Unit = {},
    confirm: @Composable () -> Unit,
) {
    val spacing = BottlingTheme.spacing
    BottlingDialog(onDismissRequest = onDismissRequest) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(spacing.m))
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(spacing.s))
        above()
        Spacer(modifier = Modifier.height(spacing.s))
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottlingTextButton(text = R.string.settings_cancel, onTapHandler = onDismissRequest)
            confirm()
        }
    }
}

/**
 * The blocking progress while the account is signed out of or deleted (spec §3.17): nothing else can be
 * tapped, and back waits too.
 */
@Composable
internal fun AccountBusyOverlay(deleting: Boolean) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BottlingTheme.colors.scrim),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.m)) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(if (deleting) R.string.settings_deleting else R.string.settings_signing_out),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@NightDawnPreviews
@Composable
private fun DeleteAccountDialogPreview() = BottlingPreview {
    DeleteAccountDialog(onDismissRequest = {}, onDelete = {}, startTicked = true)
}
