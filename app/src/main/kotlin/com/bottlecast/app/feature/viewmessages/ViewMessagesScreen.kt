package com.bottlecast.app.feature.viewmessages

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.ModerationAction
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

@Composable
fun ViewMessagesScreen(
    viewModel: ViewMessagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ViewMessagesScreen(state = state, onModerate = viewModel::moderate)
}

@Composable
internal fun ViewMessagesScreen(
    state: MessagesState,
    onModerate: (messageId: String, action: ModerationAction) -> Unit,
) {
    when (state) {
        MessagesState.Failure -> ViewMessagesError()
        MessagesState.Loading -> ViewMessagesLoading()
        is MessagesState.MessagesReceived -> MessageListView(state.messageEntities, state.actionFailed, onModerate)
    }
}

@Composable
internal fun ViewMessagesLoading() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = BottlingTheme.colors.glass,
            modifier = Modifier
                .size(BottlingTheme.sizes.touchTarget)
                .align(Alignment.Center),
        )
    }
}

@Composable
internal fun ViewMessagesError() {
    CentredMessage(R.string.view_messages_error_get, isError = true)
}

@Composable
private fun CentredMessage(@StringRes text: Int, isError: Boolean = false) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.headlineSmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(BottlingTheme.spacing.m)
                .align(Alignment.Center),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun MessageListView(
    messageEntities: List<MessageEntity>,
    actionFailed: Boolean = false,
    onModerate: (messageId: String, action: ModerationAction) -> Unit = { _, _ -> },
) {
    if (messageEntities.isEmpty()) {
        CentredMessage(R.string.moderation_empty)
        return
    }

    val scrollState = rememberLazyListState()
    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(vertical = BottlingTheme.spacing.xs),
    ) {
        if (actionFailed) {
            item {
                Text(
                    text = stringResource(R.string.moderation_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = BottlingTheme.spacing.m, vertical = BottlingTheme.spacing.xs),
                )
            }
        }
        items(messageEntities, key = { it.id }) { MessageView(it, onModerate) }
    }
}

@Composable
internal fun MessageView(
    messageEntity: MessageEntity,
    onModerate: (messageId: String, action: ModerationAction) -> Unit = { _, _ -> },
) {
    val spacing = BottlingTheme.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs)
    ) {
        Column(Modifier.fillMaxWidth().padding(start = spacing.m, end = spacing.m, top = spacing.m)) {
            Text(
                text = messageEntity.message,
                maxLines = 20,
                // a message runs in its own script's direction, whichever way the app is laid out
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
            )
            Text(
                text = stringResource(
                    R.string.moderation_summary,
                    pluralStringResource(R.plurals.moderation_reports, messageEntity.reportCount, messageEntity.reportCount),
                    stringResource(messageEntity.status.label()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.xs),
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (messageEntity.status != MessageStatus.UnderReview) {
                    ModerationButton(R.string.moderation_pull) { onModerate(messageEntity.id, ModerationAction.Pull) }
                }
                if (messageEntity.status != MessageStatus.Removed) {
                    ModerationButton(R.string.moderation_remove) { onModerate(messageEntity.id, ModerationAction.Remove) }
                }
                ModerationButton(R.string.moderation_restore) { onModerate(messageEntity.id, ModerationAction.Restore) }
            }
        }
    }
}

@Composable
private fun ModerationButton(@StringRes text: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(text)) }
}

@StringRes
private fun MessageStatus.label(): Int = when (this) {
    MessageStatus.Afloat -> R.string.moderation_status_afloat
    MessageStatus.Found -> R.string.moderation_status_found
    MessageStatus.UnderReview -> R.string.moderation_status_under_review
    MessageStatus.Removed -> R.string.moderation_status_removed
}

@NightDawnPreviews
@Composable
private fun MessageListViewPreview() = BottlingPreview {
    MessageListView(
        listOf(
            MessageEntity(id = "1", message = "Hello World", userId = "writer", reportCount = 3),
            MessageEntity(
                id = "2",
                message = "It's nice to see you 👋 this is an example of a really long message that spans multiple lines",
                userId = "writer",
                status = MessageStatus.UnderReview,
                reportCount = 1,
            )
        )
    )
}

@NightDawnPreviews
@Composable
private fun ViewMessagesLoadingPreview() = BottlingPreview { ViewMessagesLoading() }

@NightDawnPreviews
@Composable
private fun ViewMessagesErrorPreview() = BottlingPreview { ViewMessagesError() }
