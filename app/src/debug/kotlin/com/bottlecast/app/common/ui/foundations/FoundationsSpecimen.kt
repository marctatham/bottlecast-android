package com.bottlecast.app.common.ui.foundations

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.components.ParchmentCard
import com.bottlecast.app.common.components.StatusChip
import com.bottlecast.app.common.components.StatusChipStyle
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.DawnBottlingColors
import com.bottlecast.app.common.ui.DawnColorScheme
import com.bottlecast.app.common.ui.NightBottlingColors
import com.bottlecast.app.common.ui.NightColorScheme
import com.bottlecast.app.common.ui.RobotoMono
import com.bottlecast.app.common.ui.RobotoSlab
import com.bottlecast.app.common.ui.robotoFlex

/**
 * A debug-only specimen of the theme, laid out like the Foundations-DesignSystem board (1440×1000),
 * so the owner can sign off on the look from one screenshot per theme. Never shipped.
 *
 * @param focusInteractionSource lets a test show the focus ring on the last pill.
 */
@Composable
internal fun FoundationsSpecimen(
    modifier: Modifier = Modifier,
    focusInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(SpecimenDimens.framePadding),
        verticalArrangement = Arrangement.spacedBy(SpecimenDimens.rowGap),
    ) {
        Header()
        Row(
            modifier = Modifier.height(SpecimenDimens.mainHeight),
            horizontalArrangement = Arrangement.spacedBy(SpecimenDimens.columnGap),
        ) {
            ColourColumn(Modifier.width(SpecimenDimens.colourWidth))
            TypeColumn(Modifier.width(SpecimenDimens.typeWidth))
            ComponentsColumn(Modifier.width(SpecimenDimens.componentsWidth), focusInteractionSource)
        }
        IconStrip()
    }
}

@Composable
private fun Header() {
    val colors = BottlingTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SpecimenDimens.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(SpecimenDimens.headerGap)) {
            Box(modifier = Modifier.padding(top = BottlingTheme.spacing.xs)) { MoonDisc() }
            Column {
                Text(
                    text = stringResource(R.string.foundations_title),
                    style = SpecimenDimens.titleStyle(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.foundations_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val title = stringResource(R.string.foundations_banner_title)
        val body = stringResource(R.string.foundations_banner_body)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title) }
                append(body)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onMoonContainer,
            modifier = Modifier
                .width(SpecimenDimens.bannerWidth)
                .background(colors.moonContainer, BottlingTheme.shapes.field)
                .padding(horizontal = BottlingTheme.spacing.m, vertical = BottlingTheme.spacing.s),
        )
    }
}

@Composable
private fun SectionHeading(@StringRes text: Int) {
    val ruleColor = MaterialTheme.colorScheme.outlineVariant
    val hairline = BottlingTheme.sizes.hairline
    OverlineText(
        text = stringResource(text),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = ruleColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = hairline.toPx(),
                )
            }
            .padding(bottom = BottlingTheme.spacing.xs),
    )
    Spacer(modifier = Modifier.height(BottlingTheme.spacing.m))
}

private data class Swatch(val name: String, val color: Color)

private fun swatches(night: Boolean): List<Swatch> {
    val scheme = if (night) NightColorScheme else DawnColorScheme
    val colors = if (night) NightBottlingColors else DawnBottlingColors
    // the board's 13 tokens; names are code identifiers, not copy
    return listOf(
        Swatch("background", scheme.background),
        Swatch("dome", colors.dome),
        Swatch("surface", scheme.surface),
        Swatch("surfaceHigh", colors.surfaceHigh),
        Swatch("primary", scheme.primary),
        Swatch("primaryContainer", scheme.primaryContainer),
        Swatch("glass", colors.glass),
        Swatch("glassContainer", colors.glassContainer),
        Swatch("moon", colors.moon),
        Swatch("moonContainer", colors.moonContainer),
        Swatch("parchment", colors.parchment),
        Swatch("ink", colors.ink),
        Swatch("error", scheme.error),
    )
}

@Composable
private fun ColourColumn(modifier: Modifier) {
    Column(modifier = modifier) {
        SectionHeading(R.string.foundations_section_colour)
        Row(horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.l)) {
            SwatchList(R.string.foundations_night, R.string.foundations_night_hint, swatches(night = true), Modifier.weight(1f))
            SwatchList(R.string.foundations_dawn, R.string.foundations_dawn_hint, swatches(night = false), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SwatchList(@StringRes title: Int, @StringRes hint: Int, swatches: List<Swatch>, modifier: Modifier) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxs)) {
        val titleText = stringResource(title)
        val hintText = stringResource(hint)
        Text(
            text = buildAnnotatedString {
                append(titleText)
                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = onSurfaceVariant)) { append(hintText) }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = BottlingTheme.spacing.xxs),
        )
        swatches.forEach { swatch ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s)) {
                Box(
                    modifier = Modifier
                        .size(SpecimenDimens.swatch)
                        .background(swatch.color, SpecimenDimens.swatchShape)
                        .border(BottlingTheme.sizes.hairline, MaterialTheme.colorScheme.outlineVariant, SpecimenDimens.swatchShape),
                )
                Column {
                    Text(text = swatch.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = swatch.color.hex(), style = BottlingTheme.typography.mono, color = onSurfaceVariant)
                }
            }
        }
    }
}

private fun Color.hex(): String = "#%06X".format(value.shr(32).toLong() and 0xFFFFFF)

@Composable
private fun TypeColumn(modifier: Modifier) {
    val typography = MaterialTheme.typography
    val colors = BottlingTheme.colors
    Column(modifier = modifier) {
        SectionHeading(R.string.foundations_section_type)
        Row(horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s)) {
            FontTile(R.string.foundations_font_slab, R.string.foundations_font_slab_caption, SpecimenDimens.fontSample(RobotoSlab, FontWeight.SemiBold), Modifier.weight(1f))
            FontTile(R.string.foundations_font_flex, R.string.foundations_font_flex_caption, SpecimenDimens.fontSample(SpecimenDimens.flexSampleFamily, FontWeight.Normal), Modifier.weight(1f))
            FontTile(R.string.foundations_font_mono, R.string.foundations_font_mono_caption, SpecimenDimens.fontSample(RobotoMono, FontWeight.Normal), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(BottlingTheme.spacing.l))
        Column(verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.l)) {
            Role(R.string.foundations_role_headline_medium) {
                Text(stringResource(R.string.foundations_sample_hero), style = typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Role(R.string.foundations_role_title_large) {
                ParchmentCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(localeList = LocaleList("es"))) {
                                append(stringResource(R.string.foundations_sample_message))
                            }
                        },
                        style = typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.foundations_sample_byline),
                        style = typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = BottlingTheme.spacing.s),
                    )
                }
            }
            Role(R.string.foundations_role_body_large) {
                Text(stringResource(R.string.foundations_sample_body), style = typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Role(R.string.foundations_role_label_small) {
                OverlineText(stringResource(R.string.foundations_sample_overline))
            }
            Role(R.string.foundations_role_mono) {
                Text(stringResource(R.string.foundations_sample_address), style = BottlingTheme.typography.mono, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun FontTile(@StringRes name: Int, @StringRes caption: Int, sampleStyle: TextStyle, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, BottlingTheme.shapes.field)
            .padding(BottlingTheme.spacing.s),
    ) {
        Text(text = stringResource(R.string.foundations_font_sample), style = sampleStyle, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = stringResource(name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = BottlingTheme.spacing.xxs),
        )
        Text(text = stringResource(caption), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Role(@StringRes label: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SpecimenDimens.roleLabelGap)) {
        Text(text = stringResource(label), style = BottlingTheme.typography.mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun ComponentsColumn(modifier: Modifier, focusInteractionSource: MutableInteractionSource) {
    val spacing = BottlingTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Column {
            SectionHeading(R.string.foundations_section_components)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
                Column(modifier = Modifier.width(SpecimenDimens.pillWidth), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Caption(R.string.foundations_pills_caption)
                    BottlingButton(
                        text = stringResource(R.string.foundations_pill_find),
                        buttonType = BottlingButtonType.PRIMARY,
                        leadingIcon = R.drawable.ic_glyph_bottle,
                    )
                    BottlingButton(text = stringResource(R.string.foundations_pill_cast), buttonType = BottlingButtonType.SECONDARY)
                    BottlingButton(text = stringResource(R.string.foundations_pill_keep), buttonType = BottlingButtonType.TONAL)
                    Caption(R.string.foundations_pill_focused)
                    BottlingButton(
                        text = stringResource(R.string.foundations_pill_cast),
                        buttonType = BottlingButtonType.PRIMARY,
                        interactionSource = focusInteractionSource,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Caption(R.string.foundations_moon_caption)
                    BottlingTheme(darkTheme = true) { MoonTile() }
                    BottlingTheme(darkTheme = false) { MoonTile() }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Caption(R.string.foundations_chips_caption)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                StatusChip(stringResource(R.string.foundations_chip_drifting), R.drawable.ic_symbol_waves, StatusChipStyle.Glass)
                StatusChip(stringResource(R.string.foundations_chip_not_kept), R.drawable.ic_symbol_waves, StatusChipStyle.Outline)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                StatusChip(stringResource(R.string.foundations_chip_kept), R.drawable.ic_symbol_inventory_2, StatusChipStyle.PrimaryContainer)
                StatusChip(stringResource(R.string.foundations_chip_under_review), R.drawable.ic_symbol_flag, StatusChipStyle.ErrorContainer)
            }
            Caption(R.string.foundations_chips_parchment_caption)
            ParchmentCard(
                shape = BottlingTheme.shapes.card,
                contentPadding = PaddingValues(spacing.s),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    StatusChip(stringResource(R.string.foundations_chip_kept), R.drawable.ic_symbol_inventory_2, StatusChipStyle.Parchment)
                    StatusChip(stringResource(R.string.foundations_chip_not_kept_parchment), R.drawable.ic_symbol_waves, StatusChipStyle.Parchment)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Caption(R.string.foundations_role_extended)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
                Text(stringResource(R.string.foundations_sample_wordmark), style = BottlingTheme.typography.wordmark, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.foundations_sample_countdown), style = BottlingTheme.typography.countdown, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.foundations_sample_stat), style = BottlingTheme.typography.statNumeral, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(stringResource(R.string.foundations_sample_long_message), style = BottlingTheme.typography.titleLargeLong, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MoonTile() {
    Box(
        modifier = Modifier
            .size(SpecimenDimens.moonTile)
            .background(BottlingTheme.colors.dome, BottlingTheme.shapes.card),
        contentAlignment = Alignment.Center,
    ) { MoonDisc() }
}

@Composable
private fun Caption(@StringRes text: Int) {
    Text(text = stringResource(text), style = BottlingTheme.typography.mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private val SpecimenIcons = listOf(
    R.drawable.ic_glyph_shore to "ic_glyph_shore",
    R.drawable.ic_glyph_bottle to "ic_glyph_bottle",
    R.drawable.ic_symbol_inventory_2 to "inventory_2",
    R.drawable.ic_symbol_person to "person",
    R.drawable.ic_symbol_waves to "waves",
    R.drawable.ic_symbol_shield to "shield",
    R.drawable.ic_symbol_notifications to "notifications",
    R.drawable.ic_symbol_chevron_right to "chevron_right",
    R.drawable.ic_symbol_more_vert to "more_vert",
    R.drawable.ic_symbol_arrow_back to "arrow_back",
    R.drawable.ic_symbol_flag to "flag",
    R.drawable.ic_symbol_flag_filled to "flag (filled)",
    R.drawable.ic_symbol_account_circle to "account_circle",
)

@Composable
private fun IconStrip() {
    Column {
        SectionHeading(R.string.foundations_section_icons)
        Row(horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.l), verticalAlignment = Alignment.Top) {
            SpecimenIcons.forEach { (icon, name) -> IconSpecimen(icon, name) }
        }
    }
}

@Composable
private fun IconSpecimen(@DrawableRes icon: Int, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxs)) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(text = name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The board's own frame and one-off specimen sizes; debug tooling only, not app tokens. */
private object SpecimenDimens {
    val framePadding = 64.dp
    val rowGap = 28.dp
    val headerHeight = 84.dp
    val headerGap = 20.dp
    val bannerWidth = 560.dp
    val mainHeight = 643.dp
    val columnGap = 44.dp
    val colourWidth = 392.dp
    val typeWidth = 372.dp
    val componentsWidth = 460.dp
    val swatch = 40.dp
    val swatchShape = RoundedCornerShape(10.dp)
    val roleLabelGap = 6.dp
    val pillWidth = 280.dp
    val moonTile = 96.dp

    val fontSampleSize = 28.sp
    val flexSampleFamily = robotoFlex(fontSampleSize)

    @Composable
    fun titleStyle() = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, lineHeight = 56.sp)

    fun fontSample(family: FontFamily, weight: FontWeight) =
        TextStyle(fontFamily = family, fontWeight = weight, fontSize = fontSampleSize, lineHeight = 32.sp)
}

@Preview(name = "Night", widthDp = 1440, heightDp = 1000)
@Composable
private fun FoundationsSpecimenNightPreview() = BottlingTheme(darkTheme = true) { FoundationsSpecimen() }

@Preview(name = "Dawn", widthDp = 1440, heightDp = 1000)
@Composable
private fun FoundationsSpecimenDawnPreview() = BottlingTheme(darkTheme = false) { FoundationsSpecimen() }
