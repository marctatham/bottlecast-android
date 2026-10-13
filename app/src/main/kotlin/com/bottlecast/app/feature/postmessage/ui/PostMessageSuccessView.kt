package com.bottlecast.app.feature.postmessage.ui

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
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.CustomBox
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.NightDawnPreviews


@Composable
fun PostMessageSuccessView(
    onHomeHandler: () -> Unit,
) {
    val composition: LottieComposition? by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_in_bottle))
    val progress: Float by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.6f,
    )
    PostMessageSuccessView(onHomeHandler = onHomeHandler, composition = composition, progress = { progress })
}

/**
 * The stateless cast success: previews and screenshot tests pass a fixed Lottie [progress], so they
 * never capture a running animation.
 */
@Composable
internal fun PostMessageSuccessView(
    onHomeHandler: () -> Unit,
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
                    text = stringResource(id = R.string.post_completion_title),
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
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                    ),
                    text = stringResource(id = R.string.post_completion_description),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))

                BottlingButton(
                    text = R.string.btn_post_complete,
                    buttonType = BottlingButtonType.PRIMARY,
                    onTapHandler = onHomeHandler,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    )
}

@NightDawnPreviews
@Composable
private fun PostMessageSuccessViewPreview() = BottlingPreview {
    val composition: LottieComposition? by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.message_in_bottle))
    PostMessageSuccessView(onHomeHandler = {}, composition = composition, progress = { 0f })
}