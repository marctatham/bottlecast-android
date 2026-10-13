package com.bottlecast.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.CustomBox
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.NightDawnPreviews


@Composable
fun HomeScreen(
    onGetMessageHandler: () -> Unit,
    onPostMessageHandler: () -> Unit,
    onViewMessagesHandler: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val tracker: ITracker = LocalTracker.current

    val homeUiState: HomeUiState by viewModel.state.collectAsStateWithLifecycle()
    val composition: LottieComposition? by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_in_bottle))
    val progress: Float by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.6f,
    )
    HomeScreen(
        onGetMessageHandler = {
            tracker.trackEvent(AnalyticsEvent.GetMessageTapped)
            onGetMessageHandler()
        },
        onPostMessageHandler = {
            tracker.trackEvent(AnalyticsEvent.PostMessageTapped)
            onPostMessageHandler()
        },
        onViewMessagesHandler = onViewMessagesHandler,
        homeUiState = homeUiState,
        composition = composition,
        progress = { progress },
    )
}

/**
 * The stateless Home: previews and screenshot tests pass a fixed Lottie [progress] (and a
 * composition loaded up front), so they never capture a running animation.
 */
@Composable
internal fun HomeScreen(
    onGetMessageHandler: () -> Unit,
    onPostMessageHandler: () -> Unit,
    onViewMessagesHandler: () -> Unit,
    homeUiState: HomeUiState,
    composition: LottieComposition?,
    progress: () -> Float,
) {

    CustomBox(
        canvasContent = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                LottieAnimation(
                    composition = composition,
                    progress = progress,
                    modifier = Modifier.fillMaxSize(0.6f)
                )

                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .offset(x = 0.dp, y = (-100).dp),
                    text = stringResource(id = R.string.home_title),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        outsideCanvasContent = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp)
            ) {
                if (homeUiState is HomeUiState.Admin) {
                    Spacer(modifier = Modifier.weight(1f))
                    BottlingButton(
                        text = R.string.home_button_view_messages,
                        buttonType = BottlingButtonType.SECONDARY,
                        onTapHandler = onViewMessagesHandler,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 8.dp
                        )
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                        ),
                        text = stringResource(id = R.string.home_description),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                BottlingButton(
                    text = R.string.home_button_get_message,
                    buttonType = BottlingButtonType.PRIMARY,
                    onTapHandler = onGetMessageHandler,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                BottlingButton(
                    text = R.string.home_button_put_message,
                    buttonType = BottlingButtonType.SECONDARY,
                    onTapHandler = onPostMessageHandler,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    )
                )
            }
        }
    )
}

@NightDawnPreviews
@Composable
private fun HomeScreenPreview() = BottlingPreview {
    val composition: LottieComposition? by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_in_bottle))
    HomeScreen(
        onGetMessageHandler = {},
        onPostMessageHandler = {},
        onViewMessagesHandler = {},
        homeUiState = HomeUiState.Normal,
        composition = composition,
        progress = { 0f },
    )
}