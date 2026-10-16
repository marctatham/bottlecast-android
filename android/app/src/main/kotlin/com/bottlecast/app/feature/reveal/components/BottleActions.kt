package com.bottlecast.app.feature.reveal.components

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.bottlecast.app.R
import com.bottlecast.app.common.chest.domain.KeepResult

/** Shares a found bottle's words, with where to find BottleCast, through the system's sharesheet. */
internal fun shareBottle(context: Context, message: String) {
    val text = context.getString(R.string.reveal_share_text, message, context.getString(R.string.play_store_url))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.reveal_share_chooser)))
}

/** What Keep's snackbar says about how it went, on Reveal and Keepsake detail alike. */
@StringRes
internal fun keepMessage(result: KeepResult): Int = when (result) {
    KeepResult.Kept -> R.string.keep_kept
    KeepResult.Offline -> R.string.keep_offline
    KeepResult.NotHeld -> R.string.keep_not_held
    KeepResult.Failed -> R.string.keep_failed
}
