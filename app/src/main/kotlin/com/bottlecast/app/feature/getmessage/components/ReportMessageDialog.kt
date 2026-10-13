package com.bottlecast.app.feature.getmessage.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.NightDawnPreviews

@Composable
fun ReportMessageDialog(
    onDismiss: () -> Unit,
    onReportConfirmed: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) { Text(text = stringResource(id = R.string.get_message_report_dialog_cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = onReportConfirmed,
            ) { Text(text = stringResource(id = R.string.get_message_report_dialog_confirm)) }
        },
        title = {
            Text(
                text = stringResource(id = R.string.get_message_report_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Start,
            )
        },
        text = { Text(text = stringResource(id = R.string.get_message_report_dialog_description)) })
}

@NightDawnPreviews
@Composable
private fun ReportMessageDialogPreview() = BottlingPreview {
    ReportMessageDialog(onReportConfirmed = {}, onDismiss = {})
}
