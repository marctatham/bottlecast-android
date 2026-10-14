package com.bottlecast.app.feature.postmessage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.feature.postmessage.domain.MINIMUM_MESSAGE_LENGTH
import com.bottlecast.app.feature.postmessage.domain.fitEditToMessageLimit
import com.bottlecast.app.common.components.BottlingAppBar
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.NightDawnPreviews

@Composable
fun PostMessageView(
    onBackHandler: () -> Unit,
    uiState: PostMessageUiState,
    onPostHandler: (messageToPost: String) -> Unit,
) {
    var textState: String by rememberSaveable { mutableStateOf("") }
    val isCurrentlyInError = uiState is PostMessageUiState.Failure
    val isMessageLongEnough = textState.length >= MINIMUM_MESSAGE_LENGTH
    val isCurrentlyEditable = isCurrentlyEditable(uiState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BottlingAppBar(
            onBackHandler = onBackHandler,
            modifier = Modifier.fillMaxWidth(),
            title = R.string.title_post_message
        )

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            value = textState,
            onValueChange = { textState = it.fitEditToMessageLimit(previous = textState) },
            enabled = isCurrentlyEditable,
            singleLine = false,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.post_message_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedIndicatorColor = Color.Transparent,

                unfocusedContainerColor = Color.Transparent,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedIndicatorColor = Color.Transparent,

                disabledContainerColor = Color.Transparent,
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledIndicatorColor = Color.Transparent,

            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrect = true
            ),
        )

        if (isCurrentlyInError) {
            BottlingFailureReason(
                failure = uiState as PostMessageUiState.Failure,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = if (isMessageLongEnough) textState.length.toString() else stringResource(
                id = R.string.post_message_char_count,
                textState.length,
                MINIMUM_MESSAGE_LENGTH
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            color = if (isMessageLongEnough) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )

        BottlingButton(
            text = R.string.btnPostMessage,
            enabled = isCurrentlyEditable && isMessageLongEnough,
            onTapHandler = { onPostHandler(textState) },
            isLoading = uiState is PostMessageUiState.Loading,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun isCurrentlyEditable(uiState: PostMessageUiState): Boolean = when (uiState) {
    is PostMessageUiState.Failure,
    PostMessageUiState.Idle -> true

    PostMessageUiState.Loading,
    is PostMessageUiState.Success -> false
}

@NightDawnPreviews
@Composable
private fun PostViewPreviewIdle() = BottlingPreview {
    PostMessageView({}, PostMessageUiState.Idle, onPostHandler = {})
}

@NightDawnPreviews
@Composable
private fun PostViewPreviewFailure() = BottlingPreview {
    PostMessageView(
        {},
        PostMessageUiState.Failure(FailureReason.NotAuthenticated),
        onPostHandler = {})
}

@NightDawnPreviews
@Composable
private fun PostViewPreviewSuccess() = BottlingPreview {
    PostMessageView({}, PostMessageUiState.Success(MessageEntity(id = "", message = "", userId = "")), onPostHandler = {})
}
