package com.bottlecast.app.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.fakeChildComposable

@Composable
fun CustomBox(
    canvasContent: @Composable () -> Unit = fakeChildComposable,
    outsideCanvasContent: @Composable () -> Unit = fakeChildComposable
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp: Dp = configuration.screenHeightDp.dp
    val radius: Dp = screenHeightDp * 0.5f  // Calculate the radius as 60% of screen height
    val yOffset: Dp =
        screenHeightDp * 0.1f  // push it up a bit so that top box = 60%, bottom box = 40%
    val domeColor = BottlingTheme.colors.dome

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    center = Offset(x = size.width / 2f, y = yOffset.toPx()),
                    color = domeColor,
                    radius = radius.toPx()
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        // Displays the canvasContent within the canvas
        Box(
            modifier = Modifier.fillMaxHeight(0.6f)
        ) { canvasContent() }

        // Display the remaining content outside the canvas space
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            outsideCanvasContent()
        }
    }
}

@NightDawnPreviews
@Composable
private fun CustomBoxPreview() = BottlingPreview {
    Box(modifier = Modifier.fillMaxSize()) {
        CustomBox(
            canvasContent = {
                Box(
                    modifier = Modifier.fillMaxSize()
                )
            },
            outsideCanvasContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                )
            }
        )
    }
}
