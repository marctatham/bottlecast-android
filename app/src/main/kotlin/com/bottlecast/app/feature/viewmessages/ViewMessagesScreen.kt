package com.bottlecast.app.feature.viewmessages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

@Composable
fun ViewMessagesScreen(
    viewModel: ViewMessagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        MessagesState.Failure -> ViewMessagesError()
        MessagesState.Loading -> ViewMessagesLoading()
        is MessagesState.MessagesReceived -> MessageListView(messageEntities = (state as MessagesState.MessagesReceived).messageEntities)
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
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.view_messages_error_get),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Center),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MessageListView(
    messageEntities: List<MessageEntity>
) {
    val scrollState = rememberLazyListState()

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(vertical = 8.dp),
        content = {
            items(messageEntities) { MessageView(it) }
        })
}

@Composable
fun MessageView(messageEntity: MessageEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = messageEntity.message,
                textAlign = TextAlign.Start,
                maxLines = 20,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}

@NightDawnPreviews
@Composable
private fun MessageViewPreview() = BottlingPreview {
    val fakeMessage = MessageEntity(message = "Hello World", userId = "my fake user ID")
    MessageView(fakeMessage)
}

@NightDawnPreviews
@Composable
private fun MessageListViewPreview() = BottlingPreview {
    val fakeUserId = "myFakeUserId"
    MessageListView(
        listOf(
            MessageEntity(message = "Hello World", userId = fakeUserId),
            MessageEntity(
                message = "It's nice to see you 👋 this is an example of a really long message that spans multiple lines",
                userId = fakeUserId
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
