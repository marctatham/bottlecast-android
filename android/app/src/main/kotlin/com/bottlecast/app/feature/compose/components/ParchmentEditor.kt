package com.bottlecast.app.feature.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import com.bottlecast.app.R
import com.bottlecast.app.common.components.ParchmentCard
import com.bottlecast.app.common.messages.domain.MessageLimits
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.compose.ComposeDimens
import com.bottlecast.app.feature.compose.domain.ComposeHelper
import com.bottlecast.app.feature.compose.domain.DraftMeter
import java.util.Locale

/**
 * The parchment editor (spec §3.4): Slab Medium 20/28 in ink, a daily placeholder in italic, and the
 * helper and counter along its foot. Its 1dp outline marks it as a field, since on Dawn the parchment
 * barely stands off the page.
 */
@Composable
internal fun ParchmentEditor(
    state: TextFieldState,
    @StringRes placeholder: Int,
    meter: DraftMeter,
    helper: ComposeHelper,
    inputTransformation: InputTransformation,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
) {
    val colors = BottlingTheme.colors
    val label = stringResource(R.string.compose_editor_label)
    ParchmentCard(
        modifier = modifier,
        border = BorderStroke(BottlingTheme.sizes.hairline, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(ComposeDimens.editorPadding),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = label },
                inputTransformation = inputTransformation,
                // a message runs in its own script's direction, whichever way the app is laid out
                textStyle = BottlingTheme.typography.titleLargeLong.copy(color = colors.ink, textDirection = TextDirection.Content),
                cursorBrush = SolidColor(colors.inkLink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorator = { field ->
                    if (state.text.isEmpty()) {
                        Text(
                            text = stringResource(placeholder),
                            style = BottlingTheme.typography.titleLargeLong.copy(fontStyle = FontStyle.Italic),
                            color = colors.inkMuted,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                    }
                    field()
                },
            )
        }
        Spacer(modifier = Modifier.height(BottlingTheme.spacing.s))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Helper(helper = helper, modifier = Modifier.weight(1f))
            Counter(meter = meter)
        }
    }
}

/** The helper, with an icon so an error isn't colour alone, read out as it changes. */
@Composable
private fun Helper(helper: ComposeHelper, modifier: Modifier = Modifier) {
    val colors = BottlingTheme.colors
    val (text, icon) = when (helper) {
        is ComposeHelper.Minimum -> pluralStringResource(
            R.plurals.compose_helper_minimum, MessageLimits.MIN_TRIMMED, MessageLimits.MIN_TRIMMED,
        ) to R.drawable.ic_symbol_info
        is ComposeHelper.Language -> stringResource(R.string.compose_helper_language, languageName(helper.tag)) to R.drawable.ic_symbol_language
        ComposeHelper.AtLimit -> stringResource(R.string.compose_helper_limit) to R.drawable.ic_symbol_info
        ComposeHelper.EmojiRoom -> stringResource(R.string.compose_helper_emoji) to R.drawable.ic_symbol_info
        ComposeHelper.None -> return
    }
    val color = if (helper is ComposeHelper.Minimum && helper.asError) colors.inkError else colors.inkMuted
    Row(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(ComposeDimens.helperIcon))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** "42 / 240", then "20 left" in ink from 220, counting down (spec §3.4). */
@Composable
private fun Counter(meter: DraftMeter) {
    val colors = BottlingTheme.colors
    val text = if (meter.countingDown) {
        pluralStringResource(R.plurals.compose_counter_left, meter.left, meter.left)
    } else {
        stringResource(R.string.compose_counter, meter.characters, MessageLimits.MAX_CHARACTERS)
    }
    val description = pluralStringResource(
        R.plurals.compose_counter_description, meter.characters, meter.characters, MessageLimits.MAX_CHARACTERS,
    )
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        color = if (meter.countingDown) colors.ink else colors.inkMuted,
        modifier = Modifier.semantics { contentDescription = if (meter.countingDown) text else description },
    )
}

// "English", in the phone's own language
private fun languageName(tag: String): String =
    Locale.forLanguageTag(tag).getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
