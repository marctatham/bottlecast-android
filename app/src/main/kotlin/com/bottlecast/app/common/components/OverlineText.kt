package com.bottlecast.app.common.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * An overline such as "FOUND TODAY · DAY 6" (labelSmall, uppercase, +0.8sp). Copy stays in sentence
 * case and is uppercased here, since Compose has no text-transform; TalkBack reads the sentence case.
 */
@Composable
fun OverlineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BottlingTheme.colors.moon,
) {
    Text(
        text = text.toUpperCase(LocaleList.current),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.semantics { contentDescription = text },
    )
}

@NightDawnPreviews
@Composable
private fun OverlineTextPreview() = BottlingPreview {
    OverlineText(text = "Found today · Day 6")
}
