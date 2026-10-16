package com.bottlecast.app.feature.shore.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.bottlecast.app.R
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.shore.domain.TideCountdown
import com.bottlecast.app.feature.shore.domain.countdownFor
import java.time.Duration

/**
 * "NEXT TIDE IN" over the countdown (spec §3.3): "3h 12m", "42 min" under an hour, "Any moment…"
 * under a minute. TalkBack reads it in words when focused; it isn't announced as it ticks (§4).
 */
@Composable
internal fun NextTide(
    untilNextTide: Duration,
    modifier: Modifier = Modifier,
    rollDigits: Boolean = true,
) {
    val countdown = countdownFor(untilNextTide)
    val text = countdownText(untilNextTide)
    val description = when (countdown) {
        is TideCountdown.HoursMinutes -> stringResource(
            R.string.shore_countdown_description,
            pluralStringResource(R.plurals.shore_countdown_hours_description, countdown.hours.toInt(), countdown.hours) + " " +
                pluralStringResource(R.plurals.shore_countdown_minutes_description, countdown.minutes.toInt(), countdown.minutes),
        )
        is TideCountdown.Minutes -> stringResource(
            R.string.shore_countdown_description,
            pluralStringResource(R.plurals.shore_countdown_minutes_description, countdown.minutes.toInt(), countdown.minutes),
        )
        TideCountdown.AnyMoment -> stringResource(R.string.shore_countdown_any_moment_description)
    }

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OverlineText(text = stringResource(R.string.shore_next_tide_in), color = MaterialTheme.colorScheme.onSurfaceVariant)
        // each minute the digits roll up (spec §1.5), or swap in place under reduced motion
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                if (rollDigits) {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "countdown",
        ) { countdownText ->
            Text(text = countdownText, style = BottlingTheme.typography.countdown, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** The countdown as it reads: "3h 12m", "42 min" under an hour, "Any moment…" under a minute. */
@Composable
internal fun countdownText(untilNextTide: Duration): String = when (val countdown = countdownFor(untilNextTide)) {
    is TideCountdown.HoursMinutes -> stringResource(R.string.shore_countdown_hours_minutes, countdown.hours, countdown.minutes)
    is TideCountdown.Minutes -> stringResource(R.string.shore_countdown_minutes, countdown.minutes)
    TideCountdown.AnyMoment -> stringResource(R.string.shore_countdown_any_moment)
}

@NightDawnPreviews
@Composable
private fun NextTidePreview() = BottlingPreview {
    NextTide(untilNextTide = Duration.ofHours(7).plusMinutes(12))
}
