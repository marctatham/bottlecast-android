package com.bottlecast.app.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EducationalScreen(
    image: ImageVector,
    @StringRes imageDescription: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    @StringRes primaryButtonText: Int,
    onPrimaryTapHandler: () -> Unit,
    onBackHandler: () -> Unit,
    @StringRes secondaryButtonText: Int,
    onSecondaryTapHandler: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        BottlingAppBar(onBackHandler = onBackHandler)

        Icon(
            imageVector = image,
            contentDescription = stringResource(id = imageDescription),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(200.dp)
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = stringResource(id = title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            text = stringResource(id = description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.padding(16.dp)) {
            BottlingButton(
                text = primaryButtonText,
                buttonType = BottlingButtonType.PRIMARY,
                onTapHandler = onPrimaryTapHandler,
            )

            BottlingButton(
                text = secondaryButtonText,
                buttonType = BottlingButtonType.SECONDARY,
                onTapHandler = onSecondaryTapHandler,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
