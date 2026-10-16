package com.bottlecast.app.common.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.bottlecast.app.common.legal.openInCustomTab

/** A link inside a [LinkedText]: its words, and the page they open. */
data class TextLink(val label: String, val url: String)

/**
 * Text with underlined links in it, such as the sign-in sheet's legal line (spec §3.2). [template]
 * holds `%1$s`, `%2$s`… where each of [links] goes, so translations can reorder them. A link opens in
 * a Custom Tab; TalkBack offers each one.
 */
@Composable
fun LinkedText(
    template: String,
    links: List<TextLink>,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    val context = LocalContext.current
    val text = remember(template, links, linkColor) {
        buildAnnotatedString {
            var from = 0
            PLACEHOLDER.findAll(template).forEach { match ->
                append(template.substring(from, match.range.first))
                val link = links.getOrNull(match.groupValues[1].toInt() - 1)
                if (link == null) {
                    append(match.value)
                } else {
                    withLink(
                        LinkAnnotation.Url(
                            url = link.url,
                            styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                            linkInteractionListener = { openInCustomTab(context, link.url) },
                        ),
                    ) { append(link.label) }
                }
                from = match.range.last + 1
            }
            append(template.substring(from))
        }
    }
    Text(text = text, style = style, color = color, modifier = modifier, textAlign = textAlign)
}

private val PLACEHOLDER = Regex("%(\\d+)\\\$s")
