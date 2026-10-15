package com.bottlecast.app.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingBottomSheet
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.sea.domain.BlockedWriter
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.shortDate
import com.bottlecast.app.feature.settings.SettingsDimens
import java.time.LocalDate
import java.time.ZoneId

/**
 * The writers this account blocked (spec §3.17): never named, only when, each with [Unblock]. Their
 * bottles reach the account again from the next look at the sea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockedWritersSheet(
    writers: List<BlockedWriter>,
    today: LocalDate,
    zone: ZoneId,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onUnblock: (BlockedWriter) -> Unit,
) {
    val spacing = BottlingTheme.spacing
    BottlingBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.l, end = spacing.l, bottom = spacing.l),
        ) {
            Text(
                text = stringResource(R.string.settings_blocked),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            if (writers.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_blocked_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            writers.forEach { writer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SettingsDimens.rowMinHeight),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = writer.blockedAt?.let { stringResource(R.string.settings_blocked_on, shortDate(it.atZone(zone).toLocalDate(), today)) }
                            ?: stringResource(R.string.settings_blocked_writer),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    BottlingTextButton(text = R.string.settings_unblock, onTapHandler = { onUnblock(writer) })
                }
            }
        }
    }
}
