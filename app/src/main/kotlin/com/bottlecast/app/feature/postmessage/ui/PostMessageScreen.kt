package com.bottlecast.app.feature.postmessage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PostMessageScreen(
    onBackHandler: () -> Unit,
    onHomeHandler: () -> Unit,
    viewModel: PostMessageViewModel = hiltViewModel(),
) {
    val state: PostMessageUiState by viewModel.state.collectAsStateWithLifecycle()
    val onPostHandler: (messageToPost: String) -> Unit = { viewModel.postMessage(it) }
    when (state) {
        is PostMessageUiState.Failure,
        PostMessageUiState.Idle,
        PostMessageUiState.Loading -> PostMessageView(onBackHandler, state, onPostHandler)

        is PostMessageUiState.Success -> PostMessageSuccessView(onHomeHandler = onHomeHandler)
    }
}
