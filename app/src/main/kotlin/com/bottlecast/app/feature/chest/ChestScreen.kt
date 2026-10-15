package com.bottlecast.app.feature.chest

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessStarted
import com.bottlecast.app.R
import com.bottlecast.app.common.components.Banner
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonSize
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.DomeEdge
import com.bottlecast.app.common.components.DomeLazyScaffold
import com.bottlecast.app.common.components.EmptyState
import com.bottlecast.app.common.components.MoonDisc
import com.bottlecast.app.common.components.bleed
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.feature.chest.components.ChestCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate

/**
 * The Chest tab (spec §3.10): its compact dome header with a summary, the Found and Cast tabs
 * straddling the arc, each tab's filters, and the bottles. A throw back or report from Keepsake detail
 * offers its Undo here.
 *
 * @param contentPadding the navigation bar's height.
 * @param snackbarHostState where the Undo shows.
 */
@Composable
fun ChestScreen(
    onGoToShoreHandler: () -> Unit,
    onCastHandler: () -> Unit,
    onSignInHandler: () -> Unit,
    onOpenBottleHandler: (messageId: String) -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    viewModel: ChestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // seen as soon as it's on screen, so the dot clears (spec §3.10)
    LaunchedEffect(Unit) { viewModel.onViewed() }

    // a throw back or report's Undo waits as long as its window does, as on the Shore
    val pending = uiState.pending
    val thrownBack = stringResource(R.string.shore_thrown_back)
    val reported = stringResource(R.string.reveal_reported_hidden)
    val undo = stringResource(R.string.shore_undo)
    LaunchedEffect(pending?.id) {
        val action = pending ?: return@LaunchedEffect
        val message = if (action is PendingSeaAction.ThrowBack) thrownBack else reported
        when (snackbarHostState.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Indefinite)) {
            SnackbarResult.ActionPerformed -> viewModel.onUndo(action.id)
            SnackbarResult.Dismissed -> viewModel.onCommit(action.id)
        }
    }

    // The Chest stays drawn while Keepsake detail opens over it. Taps can't reach it there, but a
    // keyboard, TalkBack or Switch Access still can, so it only acts while it's started, on top.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    ChestScreen(
        uiState = uiState,
        onTabHandler = viewModel::onTab,
        onFoundFilterHandler = viewModel::onFoundFilter,
        onCastFilterHandler = viewModel::onCastFilter,
        onLoadMoreHandler = viewModel::onLoadMore,
        onOpenBottleHandler = { messageId ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) onOpenBottleHandler(messageId)
        },
        onGoToShoreHandler = dropUnlessStarted(block = onGoToShoreHandler),
        onCastHandler = dropUnlessStarted(block = onCastHandler),
        onSignInHandler = dropUnlessStarted(block = onSignInHandler),
        onRetryHandler = viewModel::onRetry,
        contentPadding = contentPadding,
    )
}

/** The stateless Chest, for previews and screenshot tests. */
@Composable
internal fun ChestScreen(
    uiState: ChestUiState,
    onTabHandler: (ChestTab) -> Unit,
    onFoundFilterHandler: (FoundFilter) -> Unit,
    onCastFilterHandler: (CastFilter) -> Unit,
    onLoadMoreHandler: () -> Unit,
    onOpenBottleHandler: (messageId: String) -> Unit,
    onGoToShoreHandler: () -> Unit,
    onCastHandler: () -> Unit,
    onSignInHandler: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onRetryHandler: () -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    listState: LazyListState = rememberLazyListState(),
) {
    val spacing = BottlingTheme.spacing

    // Near the end of the list, the next page. Each page that comes in asks again while the end still
    // shows, so a filter that matches little in a page reads on until it fills the screen or runs out.
    val pageLimit by rememberUpdatedState((uiState.list as? ChestList.Entries)?.takeIf { it.hasMore }?.pageLimit)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val nearEnd = (layout.visibleItemsInfo.lastOrNull()?.index ?: 0) >= layout.totalItemsCount - LOAD_MORE_WITHIN
            pageLimit?.takeIf { nearEnd }
        }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { onLoadMoreHandler() }
    }

    DomeLazyScaffold(
        edge = ChestDimens.dome,
        contentPadding = contentPadding,
        modifier = modifier,
        state = listState,
        windowInsets = windowInsets,
        scenery = { insets ->
            // the Chest-Found board's moon sits top-end, just below the status bar
            MoonDisc(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = insets.calculateTopPadding() + spacing.xs,
                        end = insets.calculateEndPadding(LocalLayoutDirection.current) + spacing.xl,
                    ),
            )
        },
    ) { toArc ->
        item(key = "header") {
            // the header sits in the dome, and ends where the tabs can straddle the arc
            Column(modifier = Modifier.heightIn(min = (toArc - ChestDimens.tabRowHeight / 2).coerceAtLeast(0.dp))) {
                ChestHeader(uiState)
            }
        }
        item(key = "tabs") {
            ChestTabs(uiState, onTabHandler, modifier = Modifier.bleed(spacing.gutter))
        }
        item(key = "filters") {
            ChestFilters(
                uiState = uiState,
                onFoundFilterHandler = onFoundFilterHandler,
                onCastFilterHandler = onCastFilterHandler,
                modifier = Modifier.bleed(spacing.gutter),
            )
        }
        if (uiState.isOffline) {
            item(key = "offline") {
                Banner(
                    text = stringResource(R.string.chest_offline_banner),
                    icon = R.drawable.ic_symbol_wifi_off,
                    modifier = Modifier.padding(bottom = spacing.s),
                )
            }
        }
        chestList(uiState, onOpenBottleHandler, onGoToShoreHandler, onCastHandler, onSignInHandler, onRetryHandler)
    }
}

@Composable
private fun ChestHeader(uiState: ChestUiState) {
    val spacing = BottlingTheme.spacing
    Box(
        modifier = Modifier
            .padding(top = spacing.xxs)
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.touchTarget),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.chest_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
    }
    summaryText(uiState)?.let { summary ->
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "9 kept · 1 not kept yet" (spec §3.10), with whichever parts there are. Sealed waits for slice 6. */
@Composable
private fun summaryText(uiState: ChestUiState): String? {
    val kept = uiState.keptCount?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.chest_summary_kept, it, it) }
    val notKept = uiState.notKeptCount?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.chest_summary_not_kept, it, it) }
    return when {
        kept != null && notKept != null -> stringResource(R.string.chest_summary_pair, kept, notKept)
        else -> kept ?: notKept
    }
}

/** "Found (14)" | "Cast (12)", straddling the arc (spec §3.10). */
@Composable
private fun ChestTabs(uiState: ChestUiState, onTabHandler: (ChestTab) -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    PrimaryTabRow(
        selectedTabIndex = uiState.tab.ordinal,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = colorScheme.primary,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(uiState.tab.ordinal, matchContentSize = true),
                color = colorScheme.primary,
            )
        },
        divider = { HorizontalDivider(color = colorScheme.outlineVariant) },
    ) {
        ChestTab.entries.forEach { tab ->
            val name = stringResource(if (tab == ChestTab.Found) R.string.chest_tab_found else R.string.chest_tab_cast)
            val count = if (tab == ChestTab.Found) uiState.foundCount else uiState.castCount.takeUnless { uiState.isGuest }
            Tab(
                selected = uiState.tab == tab,
                onClick = { onTabHandler(tab) },
                selectedContentColor = colorScheme.primary,
                unselectedContentColor = colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        text = count?.let { stringResource(R.string.chest_tab_count, name, it) } ?: name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

/** The tab's filters: a row of chips that scrolls sideways, edge to edge (spec §3.10). */
@Composable
private fun ChestFilters(
    uiState: ChestUiState,
    onFoundFilterHandler: (FoundFilter) -> Unit,
    onCastFilterHandler: (CastFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.list == ChestList.SignInToTrack) {
        Spacer(modifier = modifier.height(BottlingTheme.spacing.s))
        return
    }
    val spacing = BottlingTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter, vertical = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        when (uiState.tab) {
            ChestTab.Found -> FoundFilter.entries.forEach { filter ->
                ChestFilterChip(label = filter.label(), selected = uiState.foundFilter == filter, onClick = { onFoundFilterHandler(filter) })
            }
            ChestTab.Cast -> CastFilter.entries.forEach { filter ->
                ChestFilterChip(label = filter.label(), selected = uiState.castFilter == filter, onClick = { onCastFilterHandler(filter) })
            }
        }
    }
}

@Composable
private fun ChestFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_check),
                    contentDescription = null,
                    modifier = Modifier.size(BottlingTheme.sizes.iconSmall),
                )
            }
        } else {
            null
        },
        shape = BottlingTheme.shapes.pill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = colorScheme.onSurface,
            selectedContainerColor = colorScheme.primaryContainer,
            selectedLabelColor = colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colorScheme.outline,
            selectedBorderColor = Color.Transparent,
            borderWidth = BottlingTheme.sizes.hairline,
        ),
    )
}

@Composable
private fun FoundFilter.label(): String = stringResource(
    when (this) {
        FoundFilter.All -> R.string.chest_filter_all
        FoundFilter.NotKept -> R.string.chest_filter_not_kept
        FoundFilter.Kept -> R.string.chest_filter_kept
    }
)

@Composable
private fun CastFilter.label(): String = stringResource(
    when (this) {
        CastFilter.All -> R.string.chest_filter_all
        CastFilter.Drifting -> R.string.chest_filter_drifting
        CastFilter.Found -> R.string.chest_filter_found
        CastFilter.Kept -> R.string.chest_filter_kept
        CastFilter.UnderReview -> R.string.chest_filter_under_review
    }
)

// The bottles, or what stands in for them.
private fun LazyListScope.chestList(
    uiState: ChestUiState,
    onOpenBottleHandler: (messageId: String) -> Unit,
    onGoToShoreHandler: () -> Unit,
    onCastHandler: () -> Unit,
    onSignInHandler: () -> Unit,
    onRetryHandler: () -> Unit,
) {
    when (val list = uiState.list) {
        ChestList.Loading -> item(key = "loading") { ChestSkeleton() }
        is ChestList.Entries -> items(list.entries, key = { it.messageId }) { entry ->
            ChestCard(
                entry = entry,
                today = uiState.today,
                onClick = { onOpenBottleHandler(entry.messageId) },
                modifier = Modifier
                    .animateItem()
                    .padding(bottom = BottlingTheme.spacing.s),
            )
        }
        ChestList.Empty -> item(key = "empty") {
            val found = uiState.tab == ChestTab.Found
            EmptyState(
                title = stringResource(if (found) R.string.chest_empty_title else R.string.chest_cast_empty_title),
                body = stringResource(if (found) R.string.chest_empty_body else R.string.chest_cast_empty_body),
                actionLabel = stringResource(if (found) R.string.go_to_shore else R.string.shore_cast),
                actionIcon = if (found) null else R.drawable.ic_glyph_bottle,
                onAction = if (found) onGoToShoreHandler else onCastHandler,
                modifier = Modifier.padding(vertical = BottlingTheme.spacing.xxl),
            )
        }
        ChestList.NothingFiltered -> item(key = "nothing") {
            Text(
                text = stringResource(R.string.chest_filter_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BottlingTheme.spacing.xxl),
            )
        }
        ChestList.Failed -> item(key = "failed") {
            EmptyState(
                title = stringResource(R.string.adrift_title),
                body = stringResource(R.string.adrift_body),
                actionLabel = stringResource(R.string.try_again),
                actionIcon = R.drawable.ic_symbol_refresh,
                onAction = onRetryHandler,
                announce = true,
                modifier = Modifier.padding(vertical = BottlingTheme.spacing.xxl),
            )
        }
        ChestList.SignInToTrack -> item(key = "sign in") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BottlingTheme.spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.chest_guest_cast),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(BottlingTheme.spacing.l))
                BottlingButton(text = R.string.chest_sign_in, leadingIcon = R.drawable.ic_symbol_person, onTapHandler = onSignInHandler)
            }
        }
    }
}

/** Three still cards while the chest opens (spec §3.10): static, so reduced motion has nothing to stop. */
@Composable
private fun ChestSkeleton() {
    val description = stringResource(R.string.chest_loading_description)
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.s),
    ) {
        repeat(SKELETON_CARDS) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChestDimens.skeletonHeight)
                    .background(MaterialTheme.colorScheme.surface, BottlingTheme.shapes.card),
            )
        }
    }
}

private const val SKELETON_CARDS = 3

// the next page loads once the last few items show
private const val LOAD_MORE_WITHIN = 3

@NightDawnPreviews
@Composable
private fun ChestScreenPreview() = BottlingPreview {
    ChestScreen(
        uiState = ChestUiState(today = LocalDate.of(2026, 10, 12), list = ChestList.Empty),
        onTabHandler = {},
        onFoundFilterHandler = {},
        onCastFilterHandler = {},
        onLoadMoreHandler = {},
        onOpenBottleHandler = {},
        onGoToShoreHandler = {},
        onCastHandler = {},
        onSignInHandler = {},
        contentPadding = PaddingValues(),
    )
}
