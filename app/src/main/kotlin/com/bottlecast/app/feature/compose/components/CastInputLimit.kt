package com.bottlecast.app.feature.compose.components

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange
import com.bottlecast.app.common.messages.domain.fitEditToCast
import com.bottlecast.app.common.messages.domain.fitsCast

/**
 * Stops typing at the bottle's limit, 240 characters or 280 UTF-8 bytes (spec §3.4). An edit that
 * goes over keeps what fits of what it added, never the message's own words, and [onLimit] plays the
 * limit's tick.
 */
internal class CastInputLimit(private val onLimit: () -> Unit) : InputTransformation {

    override fun TextFieldBuffer.transformInput() {
        val proposed = asCharSequence().toString()
        if (proposed.fitsCast()) return
        val previous = originalText.toString()
        val fitted = proposed.fitEditToCast(previous)
        // the cursor stays where the added words end
        val prefix = proposed.commonPrefixWith(previous).length
        val suffix = minOf(proposed.commonSuffixWith(previous).length, proposed.length - prefix, previous.length - prefix)
        replace(0, length, fitted)
        selection = TextRange((fitted.length - suffix).coerceIn(0, fitted.length))
        onLimit()
    }
}
