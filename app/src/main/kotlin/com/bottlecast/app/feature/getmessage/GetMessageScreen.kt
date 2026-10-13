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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingAppBar
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.getmessage.components.ReportMessageDialog
import com.bottlecast.app.feature.getmessage.components.UnbottledMessageCard

@Composable
fun GetMessageScreen(
    onHomeHandler: () -> Unit,
    viewModel: GetMessageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // the bottle always plays through once, independent of uiState, as the composition is parsed
    // asynchronously & may only be ready after the message has arrived (b1d8a34)
    val composition: LottieComposition? by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_from_bottle))
    val progress: Float by animateLottieCompositionAsState(
        composition = composition,
        speed = 1f,
        iterations = 1,
    )
    GetMessageView(
        onHomeHandler = onHomeHandler,
        uiState = uiState,
        composition = composition,
        progress = { progress },
    )
}

/**
 * The stateless Discover screen: previews and screenshot tests pass a fixed Lottie [progress], so
 * they never capture a running animation.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun GetMessageView(
    onHomeHandler: () -> Unit,
    uiState: MessageUiState,
    composition: LottieComposition?,
    progress: () -> Float,
) {
    // report message functionality
    var showAlertDialog by remember { mutableStateOf(false) }
    var isReported by remember { mutableStateOf(false) }
    val handlerDismissReportDialog = { showAlertDialog = !showAlertDialog }
    val handlerReportMessage = {
        isReported = true
        handlerDismissReportDialog()
    }

    val transitionEnterFade = fadeIn(animationSpec = tween(2000, 0, FastOutSlowInEasing))
    val transitionExitFade = fadeOut(animationSpec = tween(2000, 0, FastOutSlowInEasing))

    // determined by both the animation progress and the uiState being in a final state
    // read through derivedStateOf, so the screen recomposes once the animation completes rather
    // than on every frame of it
    val isAnimationComplete by remember(progress) { derivedStateOf { progress() == 1f } }
    val isMessageVisible = isAnimationComplete
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
                progress = progress,
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

@NightDawnPreviews
@Composable
private fun GetMessageReceivedPreview() = BottlingPreview {
    GetMessageView(
        onHomeHandler = {},
        uiState = MessageUiState.MessageReceived(MessageEntity(message = "Hello World", userId = "1")),
        composition = null,
        progress = { 1f },
    )
}

@NightDawnPreviews
@Composable
private fun GetMessageFailedPreview() = BottlingPreview {
    GetMessageView(
        onHomeHandler = {},
        uiState = MessageUiState.Failure,
        composition = null,
        progress = { 1f },
    )
}
