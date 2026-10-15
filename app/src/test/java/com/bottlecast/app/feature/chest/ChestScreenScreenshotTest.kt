package com.bottlecast.app.feature.chest

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bottlecast.app.TopLevelDestination
import com.bottlecast.app.feature.chest.domain.BottleStanding
import com.bottlecast.app.testing.BoardStatusBar
import com.bottlecast.app.testing.BoardTabFrame
import com.bottlecast.app.testing.captureThemed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * The Chest tab (spec §3.10) in its tab frame, at the boards' 390×844 with a 44dp status bar: PNG
 * pixels ÷ 2 = board pixels. The bottles are the Chest boards' sample messages, with sealing's states
 * standing as Kept until slice 6.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
class ChestScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun foundNight() = capture(found(), darkTheme = true)

    @Test
    fun foundDawn() = capture(found(), darkTheme = false)

    @Test
    fun keptFilterNight() = capture(found(filter = FoundFilter.Kept), darkTheme = true)

    @Test
    fun keptFilterDawn() = capture(found(filter = FoundFilter.Kept), darkTheme = false)

    @Test
    fun castNight() = capture(cast(), darkTheme = true)

    @Test
    fun castDawn() = capture(cast(), darkTheme = false)

    @Test
    fun emptyNight() = capture(state(list = ChestList.Empty, kept = 0, notKept = 0), darkTheme = true)

    @Test
    fun emptyDawn() = capture(state(list = ChestList.Empty, kept = 0, notKept = 0), darkTheme = false)

    @Test
    fun castEmptyNight() = capture(state(tab = ChestTab.Cast, list = ChestList.Empty, cast = 0), darkTheme = true)

    @Test
    fun castEmptyDawn() = capture(state(tab = ChestTab.Cast, list = ChestList.Empty, cast = 0), darkTheme = false)

    @Test
    fun nothingFilteredNight() = capture(state(foundFilter = FoundFilter.NotKept, list = ChestList.NothingFiltered, notKept = 0), darkTheme = true)

    @Test
    fun nothingFilteredDawn() = capture(state(foundFilter = FoundFilter.NotKept, list = ChestList.NothingFiltered, notKept = 0), darkTheme = false)

    // a guest finds and holds bottles, but only a Google account casts
    @Test
    fun guestCastNight() = capture(state(tab = ChestTab.Cast, list = ChestList.SignInToTrack, isGuest = true, kept = 0), darkTheme = true)

    @Test
    fun guestCastDawn() = capture(state(tab = ChestTab.Cast, list = ChestList.SignInToTrack, isGuest = true, kept = 0), darkTheme = false)

    @Test
    fun loadingNight() = capture(state(list = ChestList.Loading, kept = null, notKept = null, cast = null), darkTheme = true)

    @Test
    fun loadingDawn() = capture(state(list = ChestList.Loading, kept = null, notKept = null, cast = null), darkTheme = false)

    // offline, the counts aren't known, and the chest says it's still here
    @Test
    fun offlineNight() = capture(found().copy(isOffline = true, keptCount = null, castCount = null), darkTheme = true)

    @Test
    fun offlineDawn() = capture(found().copy(isOffline = true, keptCount = null, castCount = null), darkTheme = false)

    // at 200% the chips move under the words
    @Test
    @Config(fontScale = 2f)
    fun largeTextNight() = capture(found(), darkTheme = true)

    @Test
    @Config(fontScale = 2f)
    fun largeTextDawn() = capture(found(), darkTheme = false)

    private fun capture(uiState: ChestUiState, darkTheme: Boolean) {
        composeTestRule.captureThemed(darkTheme = darkTheme) {
            BoardTabFrame(selected = TopLevelDestination.CHEST) { contentPadding ->
                ChestScreen(
                    uiState = uiState,
                    onTabHandler = {},
                    onFoundFilterHandler = {},
                    onCastFilterHandler = {},
                    onLoadMoreHandler = {},
                    onOpenBottleHandler = {},
                    onGoToShoreHandler = {},
                    onCastHandler = {},
                    onSignInHandler = {},
                    contentPadding = contentPadding,
                    windowInsets = BoardStatusBar,
                )
            }
        }
    }

    internal companion object {
        // the boards' day
        val TODAY: LocalDate = LocalDate.of(2026, 10, 12)

        fun state(
            tab: ChestTab = ChestTab.Found,
            foundFilter: FoundFilter = FoundFilter.All,
            castFilter: CastFilter = CastFilter.All,
            list: ChestList,
            kept: Int? = 9,
            notKept: Int? = 1,
            cast: Int? = 12,
            isGuest: Boolean = false,
        ) = ChestUiState(
            today = TODAY,
            tab = tab,
            foundFilter = foundFilter,
            castFilter = castFilter,
            isGuest = isGuest,
            keptCount = kept,
            notKeptCount = notKept,
            castCount = cast,
            list = list,
        )

        fun found(filter: FoundFilter = FoundFilter.All): ChestUiState {
            val notKept = listOf(
                ChestEntry(
                    "lighthouse0000000000",
                    "Somewhere a lighthouse is blinking just for you.",
                    BottleStanding.NotKept(5),
                    ChestMeta.Found(TODAY.minusDays(2), "en"),
                ),
            )
            val kept = listOf(
                ChestEntry(
                    "hoyelmar000000000000",
                    "Hoy el mar me devolvió algo que creía perdido: la calma.",
                    BottleStanding.Kept,
                    ChestMeta.Found(TODAY, "es"),
                ),
                ChestEntry("prettycool0000000000", "Hey, that's pretty cool", BottleStanding.Kept, ChestMeta.Found(TODAY.minusDays(1), "en")),
                ChestEntry(
                    "porchlight0000000000",
                    "Leave the porch light on for the people still finding their way home.",
                    BottleStanding.Kept,
                    ChestMeta.Found(TODAY.minusDays(4), "und"),
                ),
            )
            val entries = if (filter == FoundFilter.Kept) kept else notKept + kept
            return state(foundFilter = filter, list = ChestList.Entries(entries, hasMore = false))
        }

        fun cast(): ChestUiState = state(
            tab = ChestTab.Cast,
            list = ChestList.Entries(
                listOf(
                    ChestEntry("water000000000000000", "If you're reading this, drink some water today.", BottleStanding.Drifting, ChestMeta.Cast(TODAY)),
                    ChestEntry(
                        "smallwins00000000000",
                        "Small wins count. Today mine was a good cup of tea.",
                        BottleStanding.Found,
                        ChestMeta.Cast(TODAY.minusDays(3)),
                    ),
                    ChestEntry("tide0000000000000000", "The tide always comes back.", BottleStanding.Kept, ChestMeta.Cast(TODAY.minusDays(9))),
                    ChestEntry("removed0000000000000", null, BottleStanding.Removed, ChestMeta.Cast(TODAY.minusDays(20))),
                ),
                hasMore = false,
            ),
        )
    }
}
