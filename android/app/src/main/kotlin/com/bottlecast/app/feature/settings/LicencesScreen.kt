package com.bottlecast.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingBottomSheet
import com.bottlecast.app.common.components.DomeLazyScaffold
import com.bottlecast.app.common.components.ImmersiveTopBar
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.components.bleed
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.legal.openInCustomTab
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.settings.components.SettingsDivider
import com.bottlecast.app.feature.settings.components.SettingsRow

/** Open-source licences (spec §3.17), from Settings › About. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicencesScreen(
    onBackHandler: () -> Unit,
    viewModel: LicencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LicencesScreen(
        uiState = uiState,
        onBackHandler = onBackHandler,
        onBundledHandler = viewModel::onOpenBundled,
        onLibraryHandler = { library -> library.url?.let { openInCustomTab(context, it) } },
    )
    (uiState as? LicencesUiState.Showing)?.openText?.let { open ->
        BottlingBottomSheet(onDismissRequest = viewModel::onCloseBundled, sheetState = rememberBottlingSheetState()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = BottlingTheme.spacing.l, end = BottlingTheme.spacing.l, bottom = BottlingTheme.spacing.l),
            ) {
                Text(
                    text = stringResource(open.asset.title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(BottlingTheme.spacing.m))
                Text(text = open.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The stateless list, for previews and screenshot tests. */
@Composable
internal fun LicencesScreen(
    uiState: LicencesUiState,
    onBackHandler: () -> Unit,
    onBundledHandler: (BundledAsset) -> Unit,
    onLibraryHandler: (LicensedLibrary) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    contentPadding: PaddingValues = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
) {
    val showing = uiState as? LicencesUiState.Showing
    DomeLazyScaffold(
        edge = SettingsDimens.dome,
        contentPadding = contentPadding,
        modifier = modifier,
        windowInsets = windowInsets,
    ) { _ ->
        item(key = "top") {
            ImmersiveTopBar(onBackHandler = onBackHandler, title = R.string.licences_title, modifier = Modifier.bleed(SettingsDimens.topBarBleed))
        }
        if (showing == null) return@DomeLazyScaffold
        item(key = "bundled") { Header(R.string.licences_bundled) }
        items(showing.bundled, key = { it.name }) { asset ->
            GroupRow(index = showing.bundled.indexOf(asset), count = showing.bundled.size) {
                SettingsRow(
                    headline = stringResource(asset.title),
                    supporting = stringResource(asset.licence),
                    trailingIcon = R.drawable.ic_symbol_chevron_right,
                    onClick = { onBundledHandler(asset) },
                )
            }
        }
        item(key = "libraries") { Header(R.string.licences_libraries) }
        items(showing.libraries.size, key = { showing.libraries[it].id }) { index ->
            val library = showing.libraries[index]
            GroupRow(index = index, count = showing.libraries.size) {
                SettingsRow(
                    headline = library.name,
                    supporting = listOfNotNull(library.version, library.licences.joinToString(" · ").ifEmpty { null })
                        .takeIf { it.isNotEmpty() }
                        ?.let { parts -> if (parts.size == 2) stringResource(R.string.licences_library_detail, parts[0], parts[1]) else parts[0] },
                    trailingIcon = library.url?.let { R.drawable.ic_symbol_open_in_new },
                    onClick = library.url?.let { { onLibraryHandler(library) } },
                )
            }
        }
        item(key = "end") { Spacer(modifier = Modifier.height(BottlingTheme.spacing.l)) }
    }
}

@Composable
private fun Header(title: Int) {
    OverlineText(
        text = stringResource(title),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = BottlingTheme.spacing.m, top = SettingsDimens.sectionGap, bottom = SettingsDimens.headerGap)
            .semantics { heading() },
    )
}

// one row of a group drawn a row at a time, so a long list stays lazy: the first and last round its corners
@Composable
private fun GroupRow(index: Int, count: Int, content: @Composable () -> Unit) {
    val card = BottlingTheme.shapes.card as CornerBasedShape
    val shape: Shape = when {
        count == 1 -> card
        index == 0 -> card.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)
        index == count - 1 -> card.copy(topStart = ZeroCornerSize, topEnd = ZeroCornerSize)
        else -> RectangleShape
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (index > 0) SettingsDivider()
        content()
    }
}

@NightDawnPreviews
@Composable
private fun LicencesScreenPreview() = BottlingPreview {
    LicencesScreen(
        uiState = LicencesUiState.Showing(
            libraries = listOf(
                LicensedLibrary("androidx.activity:activity", "Activity", "1.13.0", listOf("Apache License 2.0"), "https://www.apache.org/licenses/LICENSE-2.0.txt"),
            ),
        ),
        onBackHandler = {},
        onBundledHandler = {},
        onLibraryHandler = {},
    )
}
