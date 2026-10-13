package com.bottlecast.app.feature.getmessage.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

@Composable
fun UnbottledMessageCard(
    isReported: Boolean,
    onReportHandler: () -> Unit,
    unbottledMessage: String,
    modifier: Modifier = Modifier,
) {
    val flagIcon: Int = if (isReported) R.drawable.ic_symbol_flag_filled else R.drawable.ic_symbol_flag
    val spacing = BottlingTheme.spacing

    // flat on Night, which is tonal only; a 1dp shadow on Dawn (spec §1.4)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = BottlingTheme.shapes.card,
        elevation = CardDefaults.cardElevation(defaultElevation = BottlingTheme.effects.cardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp, horizontal = 0.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            FilledIconButton(
                onClick = onReportHandler,
                modifier = Modifier.padding(spacing.xs),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(flagIcon),
                    contentDescription = stringResource(id = R.string.get_message_report),
                    modifier = Modifier.size(BottlingTheme.sizes.icon)
                )
            }
        }

        Text(
            modifier = Modifier.padding(start = spacing.m, end = spacing.m, bottom = spacing.xl),
            text = unbottledMessage,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@NightDawnPreviews
@Composable
private fun UnbottledMessageCardPreview() = BottlingPreview {
    UnbottledMessageCard(
        isReported = false,
        {},
        unbottledMessage = "This is the message I have received"
    )
}
