package com.bottlecast.app.feature.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonSize
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.BottlingDialog
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.ui.BottlingTheme

/** "Toss this draft?" [Keep writing] [Toss] (spec §3.4), when Compose closes with words written. */
@Composable
internal fun DiscardDraftDialog(onKeepWriting: () -> Unit, onToss: () -> Unit) {
    BottlingDialog(onDismissRequest = onKeepWriting) {
        Text(
            text = stringResource(R.string.compose_discard_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(BottlingTheme.spacing.l))
        // as the Report dialog's: the safe choice as text, the one that loses words as a pill
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottlingTextButton(text = R.string.compose_discard_keep, onTapHandler = onKeepWriting)
            BottlingButton(
                text = R.string.compose_discard_toss,
                buttonType = BottlingButtonType.DESTRUCTIVE,
                size = BottlingButtonSize.MEDIUM,
                onTapHandler = onToss,
            )
        }
    }
}
