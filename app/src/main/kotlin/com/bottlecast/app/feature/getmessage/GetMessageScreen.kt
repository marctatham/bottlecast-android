package com.bottlecast.app.feature.getmessage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCancellationBehavior
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingAppBar
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.feature.getmessage.components.ReportMessageDialog
import com.bottlecast.app.feature.getmessage.components.UnbottledMessageCard

@Composable
fun GetMessageScreen(
    onHomeHandler: () -> Unit,
    viewModel: GetMessageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    GetMessageView(onHomeHandler = onHomeHandler, uiState = uiState)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun GetMessageView(
    onHomeHandler: () -> Unit,
    uiState: MessageUiState,
) {
    // report message functionality
    var showAlertDialog by remember { mutableStateOf(false) }
    var isReported by remember { mutableStateOf(false) }
    val handlerDismissReportDialog = { showAlertDialog = !showAlertDialog }
    val handlerReportMessage = {
        isReported = true
        handlerDismissReportDialog()
    }

    // animation control
    val isPlaying = uiState is MessageUiState.PlayingAnimation
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_from_bottle))
    val progress: Float by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        speed = 1f,
        restartOnPlay = true,
        iterations = 1,
        cancellationBehavior = LottieCancellationBehavior.OnIterationFinish
    )

    val transitionEnterFade = fadeIn(animationSpec = tween(2000, 0, FastOutSlowInEasing))
    val transitionExitFade = fadeOut(animationSpec = tween(2000, 0, FastOutSlowInEasing))

    // determined by both the animation progress and the uiState being in a final state
    val isMessageVisible = progress == 1f // is animation complete?
            && (uiState is MessageUiState.PlayingAnimation).not() // is in either success or failure state?
    val lottieVisible = !isMessageVisible

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        BottlingAppBar(
            onBackHandler = onHomeHandler,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        AnimatedVisibility(
            visible = lottieVisible,
            enter = transitionEnterFade,
            exit = transitionExitFade
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = isMessageVisible,
            enter = transitionEnterFade,
            exit = transitionExitFade
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState is MessageUiState.MessageReceived) {
                    UnbottledMessageCard(
                        isReported = isReported,
                        onReportHandler = { showAlertDialog = !showAlertDialog },
                        unbottledMessage = uiState.messageEntity.message,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.Center)
                            .animateEnterExit(
                                enter = slideInVertically(
                                    animationSpec = tween(
                                        3000,
                                        0,
                                        FastOutSlowInEasing
                                    )
                                ),
                                exit = transitionExitFade,
                                label = "card slide in animation"
                            )
                    )
                } else if (uiState is MessageUiState.Failure) {
                    Image(
                        painter = painterResource(id = android.R.drawable.stat_notify_error),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                BottlingButton(
                    text = R.string.get_message_button_acknowledge,
                    buttonType = BottlingButtonType.PRIMARY,
                    onTapHandler = onHomeHandler,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }

    if (showAlertDialog) {
        ReportMessageDialog(
            onDismiss = handlerDismissReportDialog,
            onReportConfirmed = handlerReportMessage
        )
    }
}

@Preview
@Composable
private fun GetMessageReceivedPreview(
) {
    GetMessageView(
        onHomeHandler = {},
        uiState = MessageUiState.MessageReceived(MessageEntity("1", "Hello World"))
    )
}

@Preview
@Composable
private fun GetMessageAnimatingPreview(
) {
    GetMessageView(
        onHomeHandler = {},
        uiState = MessageUiState.PlayingAnimation,
    )
}

@Preview
@Composable
private fun GetMessageFailedPreview(
) {
    GetMessageView(
        onHomeHandler = {},
        uiState = MessageUiState.Failure
    )
}
