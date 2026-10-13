package com.bottlecast.app.feature.postmessage.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.postmessage.domain.MINIMUM_MESSAGE_LENGTH


@Composable
fun BottlingFailureReason(
    failure: PostMessageUiState.Failure,
    modifier: Modifier = Modifier,
) {
    val failureReason:String = when (failure.reason) {
        FailureReason.InsufficientLength -> stringResource(R.string.snack_post_message_insufficient_length, MINIMUM_MESSAGE_LENGTH)
        FailureReason.NotAuthenticated -> stringResource(R.string.snack_post_message_requires_auth)
        FailureReason.Unknown -> stringResource(R.string.snack_post_message_failed)
    }

    Text(
        text = failureReason,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.error,
    )
}

@NightDawnPreviews
@Composable
private fun BottlingFailureReasonPreview() = BottlingPreview {
    BottlingFailureReason(
        PostMessageUiState.Failure(FailureReason.InsufficientLength)
    )
}
