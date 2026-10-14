package com.bottlecast.app.feature.reveal.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bottlecast.app.R
import com.bottlecast.app.feature.reveal.Byline
import com.bottlecast.app.feature.reveal.RevealedBottle
import com.bottlecast.app.feature.reveal.domain.CastAgo

/** "— A stranger at sea · cast 3 days ago", or the pen name the writer signed it with (spec §3.7). */
@Composable
internal fun revealByline(bottle: RevealedBottle): String {
    val who = when (val byline = bottle.byline) {
        Byline.Stranger -> stringResource(R.string.message_byline_stranger)
        is Byline.PenName -> stringResource(R.string.message_byline_pen_name, byline.name)
    }
    val cast = when (val ago = bottle.castAgo) {
        null -> return who
        CastAgo.Today -> stringResource(R.string.reveal_cast_today)
        CastAgo.Yesterday -> stringResource(R.string.reveal_cast_yesterday)
        is CastAgo.Days -> pluralStringResource(R.plurals.reveal_cast_days, ago.days, ago.days)
        is CastAgo.Months -> pluralStringResource(R.plurals.reveal_cast_months, ago.months, ago.months)
        is CastAgo.Years -> pluralStringResource(R.plurals.reveal_cast_years, ago.years, ago.years)
    }
    return stringResource(R.string.message_byline_when, who, cast)
}
