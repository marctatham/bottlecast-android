# BottleCast Android

The Android app, a Gradle project rooted in this folder. Paths here are relative to `android/`. The repo-wide rules are in the root `CLAUDE.md`: environments and safety, git and PRs, copy rules, how to implement a redesign slice, and the gotchas the app shares with the Firestore rules and the Worker.

Run Gradle from this folder. From the repo root, use `android/gradlew -p android <tasks>`.

## Stack and layout
- One module, `:app`, package `com.bottlecast.app`.
  - Kotlin 2.4 via AGP 9.4's built-in Kotlin.
  - Jetpack Compose with Material 3 (material3 pinned to 1.5.0-beta01 ahead of the BOM), Navigation Compose with type-safe routes (kotlinx-serialization), Hilt (KSP).
  - Firebase Auth/Firestore/Analytics/Crashlytics, and Google sign-in through Credential Manager.
  - DataStore Preferences, WorkManager (through `androidx.hilt:hilt-work`), Custom Tabs (`androidx.browser`), ML Kit's language identification (the unbundled model), AboutLibraries (its Gradle plugin and core, for the licences screen's data) and Timber.
  - minSdk 33, compileSdk/targetSdk 37, Java 11.
  - Gradle 9.8 with the configuration cache and build cache on. Versions live in `gradle/libs.versions.toml`.
- Code is in `app/src/main/kotlin/com/bottlecast/app/`. Build-type code is in `app/src/debug/kotlin/` and `app/src/release/kotlin/`. Tests are in `app/src/test/java/` (JVM) and `app/src/androidTest/java/` (device); both hold Kotlin.
- Shell: `MainActivity` → `BottlingTheme` → `MessageInABottleApp.kt`, the NavHost. The app draws edge to edge with transparent bars.
  - Routes are `@Serializable` objects in `Navigation.kt`, each with a stable `screenName` for analytics. `TopLevelDestination` lists the tabs.
  - The start destination is Onboarding until it's been seen, then the Shore.
    - `AppNavigationStateViewModel` reads it once from DataStore.
    - `MainActivity` holds the first frame with a pre-draw listener until it's known, and the NavHost isn't composed before then.
    - Tabs pop to `Shore`, never to `findStartDestination()`, which can be Onboarding.
  - Tab roots (Shore, Chest, Me) draw themselves in a `TabFrame`, which adds the navigation bar and hands its height to the screen as `contentPadding`. Every other route is immersive.
  - Screens under the dome (Shore, Chest, Me) are built on `DomeScaffold`. It draws the dome under the status bar and keeps the content inside `windowInsets = WindowInsets.safeDrawing.only(Top + Horizontal)`, scrolling below the see-through bar. The bottom comes only from `contentPadding`, so the navigation bar isn't counted twice. The Chest lists more than fits, so it uses `DomeLazyScaffold`, the same frame around a `LazyColumn`.
  - Every other route is registered with `immersive<T>`. Its frame rounds the corners on a back swipe and takes every tap, so none falls through to the screen still drawn underneath. Screens from before the redesign also sit in `LegacyScreen`, which keeps them inside the system bars over an opaque background.
  - A tab root stays drawn while a screen opens over it. A keyboard, TalkBack or Switch Access can still reach it there, so its actions go through `dropUnlessStarted`, and `TabFrame` guards the bar the same way.
- These packages each split into `domain`, `data` and `di`:
  - `common/authentication/`
  - `common/messages/`: the writer's side (the draft, and casting through the app-scoped `CastCoordinator`, with the daily tally and the language) and moderation
  - `common/profile/`: `users/{uid}`, holding the pen name and the accepted Terms version
  - `common/sea/`: the finder's side (today's candidate, the find, throw back, report, and handing a guest's find over to an existing account)
  - `common/chest/`: Keep (`claims/{messageId}`), the chest's lists, one bottle for Keepsake detail, pulling a cast, the server counts, and the Chest's news (its dot and "While you were away")
  - `common/streak/`: the streak, folded from the daily records and `streakDays` marks by the pure `foldStreak()`/`liveStreak()`, with the fold so far kept on the phone; calm seas and a guest's carried streak
  - `common/settings/`: the phone's settings in DataStore (theme, reduce motion, haptics, shake to uncork, signing with the pen name) and the theme's `UiModeManager` applier. Reduce motion, haptics and shaking reach composables through `LocalFeelPreferences`, which `MainActivity` provides.
  - `common/reminders/`: the daily reminder. Its settings are in DataStore; the pure `nextReminderAt()`/`decideReminder()` hold the rules; `DailyReminderWorker` (a `@HiltWorker`) posts it; `ReminderCoordinator` keeps it scheduled.
  - `common/account/`: signing out, and deleting the account: `DeleteAccountUseCase`, and `AccountFirestoreEraser`, which leaves the `deletions/{uid}` tombstone, then anonymises or deletes the account's bottles and deletes its records

  `domain` holds `I`-prefixed interfaces and entities; `data` the implementations, with Firestore field names in `data/model/` and mappers in `data/mapper/`; and `di` the Hilt modules.

  `common/analytics/`, `common/connectivity/`, `common/preferences/` (the app's one DataStore, and whether onboarding has been seen), `common/threading/` (dispatchers and `@ApplicationScope`) and `common/time/` (a `Clock` that follows the phone's time zone, and a monotonic `TimeSource`) are flat, plus `di`.
  - `common/firestore/di/` provides `FirebaseFirestore`, and `common/random/di/` provides `Random` and `SecureRandom`.
  - `common/legal/` holds the Terms, Community guidelines and privacy links, which open in a Custom Tab.
- `common/ui/` is the theme: `Theme.kt`, `Color.kt`, `Effects.kt`, `Typography.kt`, `Shapes.kt`, `Dimens.kt`, `Motion.kt`. Material's roles come from `MaterialTheme`; the brand tokens Material has no slot for come from `BottlingTheme.colors`, `.effects`, `.typography`, `.shapes`, `.spacing`, `.sizes` and `.motion`.
- `common/components/` holds shared composables, including the design-system pieces: `BottlingButton` (pills), `BottlingBottomSheet`, `BottlingDialog` (which draws the theme's own scrim), `BottlingSnackbarHost`, `ImmersiveTopBar`, `StatusChip`, `ParchmentCard`, `MoonDisc`, `OverlineText`, `Dome`, `DomeScaffold`, `BottlingNavigationBar`, `TabFrame`, `Banner`, `EmptyState`, `Avatar`, `MessageCard`, `TideRing`, `WaxSeal` and `LinkedText` (text with links, through `LinkAnnotation`). `bottle/` is the bottle rig, drawn on a Canvas in each bottle's seeded look (`bottleLookFor(id)`), plus `FloatingBottle`, the rig bobbing on its ripples. `illustrations/` holds the spot illustrations. The debug-only `FoundationsSpecimen` (in `src/debug`) shows the whole design system.
  - `common/ui/` also holds the motion policy. `rememberReducedMotion()` reads the system setting. `rememberIdleMotionEnabled()` is true while the screen is resumed and motion isn't reduced, and gates idle loops such as `rememberBreathingTide()`, which holds still where it is and carries on from there.
  - Haptics go through `rememberHaptics()` (spec §1.5), which respects the system's touch feedback setting. Timed controls, such as an Undo, last `rememberTimedControlMillis()`: at least the system's recommended timeout, and no timeout while TalkBack is on (spec §4). `isLargeText()` is true from 175% text.
- `feature/<name>/` holds a screen and its ViewModel. Composables that only one feature uses go in its `components/`; use cases go in its `domain/`. Sizes only one feature uses go in its own tokens object, such as `ShoreDimens`.
  - `shore` is the home (states A and B, spec §3.3).
  - `discover` uncorks today's bottle and `reveal` shows it (spec §3.6–3.7).
  - `onboarding` is the first run (spec §3.1).
  - `signin` is the lazy Google sign-in sheet, with the guest upgrade, and `penname` is the pen-name sheet (spec §3.2).
  - `compose`, `composepreview` and `castsuccess` are casting (spec §3.4–3.5).
  - `chest` is the Chest's tabs (spec §3.10) and `keepsake` its Keepsake detail (§3.11).
  - `me` is the Me tab (spec §3.12). `settings` is Settings (§3.17) and its Open-source licences screen, and `goodbye` follows deleting the account.
  - `reminders` is the reminder sheet (spec §3.16a), opened from the Shore and Settings.
  - `viewmessages` is the moderation screen from before the redesign.

## Conventions
- **ViewModels:** `@HiltViewModel class XViewModel @Inject constructor(…)` with:
  - `private val _state: MutableStateFlow<XUiState> = MutableStateFlow(…)`
  - `val state: StateFlow<XUiState> = _state.asStateFlow()`

  The sealed state class lives in the ViewModel's file; name new ones `XUiState`. Launch work with a `CoroutineExceptionHandler` that logs with Timber and sets a failure state.
- **Screens:**
  - A public `XScreen(on…Handler: () -> Unit, viewModel: XViewModel = hiltViewModel())` collects with `collectAsStateWithLifecycle()`.
  - It passes plain state to an `internal` stateless overload, which `@Preview`s and screenshot tests call.
  - Tests can't see `private` functions, so make a stateless overload `internal` before testing it. `ShoreScreen`, `DiscoverScreen`, `RevealScreen`, `OnboardingScreen` and `CastSuccessScreen` already are, with their motion hoisted.
- **UI text** lives in `res/values/strings.xml` (`stringResource`, `@StringRes` parameters). No hard-coded strings.
- **Hilt and dispatchers:** modules use `@InstallIn(SingletonComponent::class)` with `@Provides @Singleton`; leaf classes use `@Inject constructor`. Inject dispatchers with the qualifiers in `common/threading/` (`@DispatcherIO` and the rest), and switch with `withContext` in repositories.
- **Analytics:** add a case to the sealed `AnalyticsEvent` (snake_case name) and send it through `LocalTracker.current`. An event that happens after its screen has gone, such as a throw back or report written once its undo window ends, is tracked where it happens, with the injected `ITracker` (`SeaActions`). Tracker wiring is in two `AnalyticsModule`s, one in `src/debug` and one in `src/release`; change both.
- **Logging:** Timber only. Never log tokens, emails or message text, because release builds forward info and above to Crashlytics.
- **Redesign UI** takes colours, type, shapes and spacing from theme tokens: no literal hex or one-off sizes in composables. Use start/end, never left/right: RTL is on (`supportsRtl="true"`).
  - Icons are the bundled `ic_symbol_*` (Material Symbols Rounded) and `ic_glyph_*` drawables, drawn with `painterResource`; there's no `Icons.*` dependency.
  - Overlines go through `OverlineText`, which uppercases at render time, since Compose has no text-transform.
  - Previews use `@NightDawnPreviews` and wrap their content in `BottlingPreview`; anything reading `BottlingTheme.colors` throws outside the theme.

## Build and verify
- Gradle runs on Android Studio's bundled JDK (JBR 25) through `JAVA_HOME`; the build sets no toolchain. There is no CI, so these commands are the only gate.
- **Verify every change:** `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, then `./gradlew :app:assembleDebugAndroidTest` in its own invocation, so instrumented tests keep compiling. In the same invocation as lint it trips the lint/Hilt race.
- **Release bundle:** `./gradlew :app:bundleRelease`, in its own invocation. Never run lint and a release task in the same invocation (lint/Hilt race).
- **Instrumented tests:** `./gradlew :app:connectedDebugAndroidTest`. Read "Devices" below, and "Environments and safety" in the root `CLAUDE.md`, first.
- **Screenshot tests (Roborazzi on Robolectric):**
  - Record one class: `./gradlew :app:testDebugUnitTest --tests '*<Name>ScreenshotTest' -Proborazzi.test.record=true`
  - Record all: `./gradlew :app:recordRoborazziDebug`
  - Report diffs without failing: `./gradlew :app:compareRoborazziDebug`
  - Gate before committing UI work: `./gradlew :app:verifyRoborazziDebug`
  - Goldens are committed at `app/src/test/screenshots/<package>.<Class>.<method>.png`.
  - Roborazzi keeps a copy of every image in `app/build/intermediates/roborazzi/` and can copy it back into the goldens folder. After deleting a screenshot test, also delete its images there (or run `./gradlew clean`), and never commit goldens of tests that no longer exist.
  - After a failure, Read `app/build/outputs/roborazzi/*_compare.png`, which shows the reference, the diff and the new image side by side. The report is at `app/build/reports/roborazzi/index.html`.
- Gradle skips up-to-date test tasks. When you need a fresh run as proof, add `--rerun-tasks`, or `--rerun` after a single task.
- **Fresh clones and worktrees don't build as they are.** These files are gitignored:
  - `app/google-services.json` (debug)
  - `app/src/release/google-services.json` (production)
  - `local.properties` (SDK path, signing values, `reownProjectId`)

  Copy the ones you need from the main checkout (debug work needs `app/google-services.json`), and never commit them.

## Devices
- **Devices:** connected tests install and uninstall the debug app on every attached device, which wipes its data.
  1. Run `adb devices` first.
  2. Boot a throwaway emulator with `<sdk>/emulator/emulator -avd <AVD> -read-only -no-snapshot -no-window -no-audio -no-boot-anim -port <p>`. `<sdk>` is `$ANDROID_HOME`, or `sdk.dir` in `local.properties`.
  3. Run the tests with `ANDROID_SERIAL=emulator-<p>`.

  If a writable instance of that AVD blocks the boot, ask before stopping it.
- **Flaky instrumented tests:** install once with `./gradlew :app:installDebug :app:installDebugAndroidTest`, then repeat `adb shell am instrument -w -r com.bottlecast.app.debug.test/com.bottlecast.app.HiltTestRunner`.

## Tests
- **Unit tests** (`src/test`):
  - JUnit4 with `org.junit.Assert`, backtick names, and `// given // when // then`.
  - mockk (`mockk<IFoo>()`, `every`/`coEvery`/`coVerify`) inside `runTest`.
  - New ViewModels and use cases get unit tests. ViewModel tests call `Dispatchers.setMain(…)`, because `viewModelScope` runs on Main.
  - **DataStore** does its IO on its own thread, off the test scheduler.
    - Writes that tests wait for return their `Job` (`markSeen()`, `clear()`), so a test can `join()` it.
    - Test timing, such as the draft's 1 s save delay, on `MemoryPreferences`.
    - Test persistence on `filePreferences(scope)`, a real file in a temp folder.
    - Both are in `testing/TestDataStores.kt`.
- **Screenshot tests** (`src/test`, named `*ScreenshotTest`; see `RevealScreenScreenshotTest`):
  - Use `RobolectricTestRunner` and `@GraphicsMode(NATIVE)`.
  - Use `@Config(sdk = [37], application = Application::class, qualifiers = …)`. The plain `Application` keeps the Hilt `BottleCastApplication` out.
  - Full screens use `qualifiers = "w390dp-h844dp-xhdpi"`, the board size: PNG pixels ÷ 2 = board pixels.
  - Capture with a compose rule and `composeTestRule.captureThemed(darkTheme) { … }` from `testing/Screenshots.kt`, which wraps the content in `BottlingTheme` and draws the window itself. Roborazzi's `captureRoboImage { }` and its node capture can come out blank depending on which tests ran before. The software draw leaves out elevation shadows (Dawn's 1dp card shadow); `dropShadow` glows do render. Capture every state in Night and in Dawn.
  - Sheets, dialogs and popups (an open `DropdownMenu`) are windows of their own: call `waitForIdle()`, then `composeTestRule.captureScreenByDrawing()`, which draws every window where it sits, with a dialog's dim. Roborazzi's `captureScreenRoboImage()` leaves popups out and sometimes misses the content behind a dialog. If something animates in, set `mainClock.autoAdvance = false` and advance past it first.
  - Screens drawn under the status bar take a `windowInsets` parameter: pass `BoardStatusBar`, the boards' 44dp status bar, so the PNG lines up with the board. Render tab roots in `BoardTabFrame`, a `TabFrame` with no system navigation bar. Both are in `testing/BoardFrames.kt`.
  - For 200% text use `@Config(fontScale = 2f)`, which scales non-linearly as a device does; a `LocalDensity` override scales linearly.
  - Infinite animations only run with `mainClock.autoAdvance = false`. A state write from the test thread then reaches the composition only after `waitForIdle()` (see `BreathingTideTest`).
  - Test classes share a JVM, and a class's tests share one Robolectric sandbox, so process-wide caches carry over from test to test. Compose's typeface cache, for one, ignores the font scale. Keep anything a test varies, such as `fontScale`, out of cached state, so a golden never depends on test order. That's why `Typography.kt` pins `opsz` as a plain number.
- **Never capture a running animation.** Hoist animation state into a parameter and pass a fixed value, such as a lambda read at draw time, as `DiscoverMotion`, `OnboardingMotion`, `CastMotion` and `BottleRigPose` do. The cast reads one timeline through the pure `castPose(ms)`, so a test can capture any frame.
- **Re-recording goldens:** only for an intended change, and only after reading its `_compare.png`. Never re-record just to make the gate pass.
- **Instrumented tests** (`src/androidTest`):
  - `@HiltAndroidTest`, `HiltAndroidRule` at order 0, and `createAndroidComposeRule<MainActivity>()` from `androidx.compose.ui.test.junit4.v2`.
  - Fakes in `fakes/` replace real modules through `testdi/*Module` with `@TestInstallIn(replaces = […])`. `FakeTracker` records screen views and events. Inject it with `@Inject` and `hiltRule.inject()`.
  - `NoFirestoreModule` replaces `FirestoreModule` with a provider that throws, so nothing unfaked can reach the debug project. A new module that binds anything backed by Firestore needs a fake module too.
  - Onboarding counts as seen. A test of the first run adds `FirstLaunchRule` at order 1, before the compose rule at order 2.
  - `FakeAuthenticationRepository` signs in with `signInAsGoogle()` or `signInAsGuest()`. Its `googleAccountExists` sends a sign-in down the handover path, and `FakeSeaRepository.foundElsewhere(id)` gives the guest today's find to carry.
  - `FakeCastCoordinator` answers at once, or refuses once after `refuseNext`. Offline, it queues casts until `confirmQueued()`.
  - `FakeSeaRepository` holds one seeded bottle. Switch it with `seaCalm`, `alreadyFoundToday` or `failNextFind`, or hold a find back with `findGate`. `FakeSeaActions` holds throw backs and reports until a test calls `expireAll()`, so no test waits out an undo window.
  - `FakeChestRepository` builds the chest on `FakeSeaRepository`'s find: today's held find is Not kept until it's kept, and `casts` holds the account's casts. `FakeStreakRepository` holds whatever `streak` a test sets.
  - DataStore stays out of instrumented tests, so `FakeSettingsModule`, `FakeRemindersModule` and `FakeAccountModule` (all in `testdi/FakeSettingsModule.kt`) replace the settings, the reminder (with notifications allowed, the sheet already offered and a scheduler that does nothing) and the account's eraser. `FakeAccountEraser.erased` and `FakeAuthenticationRepository.deleted` show what a deletion did.
  - A screen test without Hilt uses `ComponentActivity` and builds its ViewModel from fakes, as `ShoreScreenTest` does.
  - Look text up with `activity.getString(R.string.…)`. Wait with `waitUntil`; never sleep or add delays to fakes.
  - `performClick()` taps a node's centre. On a merged row that holds links, such as the Terms checkbox, the centre can land on a link and open a Custom Tab, and the next lookup finds no Compose hierarchy. Tap the control itself with `performTouchInput { click(…) }`.

## Gotchas
- **No dynamic colour:** `BottlingTheme(darkTheme)` is Night or Dawn and follows the system's dark mode until Settings offers a choice (slice 4). Every `ColorScheme` role is filled from the palette; `BottlingColorSchemeTest` fails if one isn't.
- **Material3:** the version catalog pins material3 1.5.0-beta01 ahead of the Compose BOM (which resolves 1.4.0), for `MotionScheme.expressive()`, `MaterialShapes` and the wavy indicators. It needs compileSdk 37 and AGP 9.1 or later. `ModalBottomSheet` and `SheetState` still need `ExperimentalMaterial3Api`; use `rememberBottlingSheetState()`. Drop the pin when the BOM ships 1.5.
- **Built-in Kotlin:** with AGP 9's built-in Kotlin, don't apply `org.jetbrains.kotlin.android`. Give Kotlin compiler plugins (compose, serialization) the `pluginKotlin` version. Navigation only brings `kotlinx-serialization-core` transitively, so the catalog declares it at the version Firebase resolves.
- **Analytics screen names:** screen analytics send each route's `screenName` (`Navigation.kt`), not `destination.route`, which type-safe routes turn into a qualified class name. The names predate the routes (the Shore is still `home`); `BottlingRouteTest` pins them, so never rename one.
- **Predictive back:** the manifest's `enableOnBackInvokedCallback` turns it on for Android 13–15. Navigation 2.10 animates back swipes with NavHost's `predictivePopEnterTransition`/`predictivePopExitTransition`, not the pop transitions, so set both (`MessageInABottleApp`). Its defaults shrink the screen to 70% and fade the one underneath in.
- **System bars:** `MainActivity` turns off the navigation bar's contrast scrim, which 3-button navigation would lay over the app's own bar. `MessageInABottleApp` sets the bar icons from the theme.
- **Firestore documents and R8:** write documents as explicit maps, with their field names in the `*Fields` objects, never as POJOs: release 1.0 wrote R8-obfuscated `a`/`b` fields from one. Mappers read named fields and skip malformed documents. R8 is on in release and `proguard-rules.pro` has no rules, so smoke-test a release build after adding libraries that rely on reflection.
- **Firestore errors on the JVM:** `FirebaseFirestoreException.Code` can't load in plain unit tests, so the sea's data source turns Firestore errors into `SeaRefusedException` and `SeaUnavailableException` before they reach the repository.
- **One owner per animation:** an `Animatable` cancels whatever animation it's already running, so a second coroutine animating it can cut the first one short, along with anything that coroutine would have done next. Discover's reveal waits for the scroll's slide half out for this reason. A `LaunchedEffect` that changes its own key, such as clearing a notice, cancels itself too: clear the key after the work, so an interrupted effect runs again instead of losing it.
- **The streak's checkpoint is a cache:** the fold up to three days ago lives in DataStore, and moves on only once the server has confirmed those days, so a fresh phone's empty cache can't save a wrong one. Bump `FOLD_VERSION` when the fold's rules change, so every phone folds its history again.
- **Firestore offline reads:** a query's default `get()` answers from the cache once Firestore is offline, possibly with nothing. Reads that decide something, such as whether the sea is calm, use `Source.SERVER`, so offline they fail instead.
- **Robolectric setup:** it needs the `--add-opens` JVM flags set on the `Test` tasks in `app/build.gradle.kts` (JDK 17+). Keep unit-test Espresso at 3.7.0 or later: Roborazzi brings 3.5.1, which fails on SDK 37 with `InputManager.getInstance()`.
- **Fonts:** all three families are bundled in `res/font/` and nothing downloads, because Robolectric can't load downloadable fonts. Roboto Flex is the variable font, with each weight selected through `FontVariation` and `opsz` pinned per size; `BottlingFontsTest` checks Robolectric still applies the axes. Sources and licences are in `licenses/`.
- **Material Symbols:** upstream XML carries `android:tint="?attr/colorControlNormal"`, an AppCompat attribute. Remove it when adding a Symbol (Compose tints icons) and keep `android:autoMirrored`.
- **mockk under Robolectric:** once a Robolectric NATIVE test has run in a test JVM, mockk can't attach its agent at runtime. The `Test` tasks preload ByteBuddy's agent with `-javaagent` (the `mockkAgent` configuration in `app/build.gradle.kts`); keep its version in step with mockk's.
- **Debug Google sign-in failing with `[28444]` or `NoCredentialException`:** usually Firebase config drift (the debug Android OAuth client is missing), not app code. Check that `app/google-services.json` has an `oauth_client` with `client_type: 1`. Fixing it changes the Firebase project, so ask first.
- **AppCompat:** `Theme.Bottling` extends `Theme.AppCompat.NoActionBar`, but AppCompat only arrives as a transitive dependency.
- **WorkManager and Hilt:** `BottleCastApplication` is WorkManager's `Configuration.Provider`, with Hilt's worker factory, and the manifest removes WorkManager's own initializer. So `WorkManager.getInstance()` works only in the real app: instrumented tests run on Hilt's test application, and fake the scheduler.
- **The theme setting** is applied with `UiModeManager.setApplicationNightMode`, which Android keeps across restarts, so `isSystemInDarkTheme()`, the splash and the window colours all follow it. Nothing reads the stored theme at start.
- **Licences:** the AboutLibraries plugin writes `res/raw/aboutlibraries.json` at build time, offline, and the `licenceTexts` task copies `licenses/*.txt` into the app's assets. A new bundled font or icon set needs its row in `BundledAsset` too.
