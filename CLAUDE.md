# BottleCast (Android)

BottleCast is a message-in-a-bottle app. People cast short messages into a shared sea, and other people find them. It is being rebuilt, slice by slice, to the "Moonlit Shore" redesign:
- fixed Night and Dawn themes
- one bottle a day
- a chest of kept bottles
- gifts
- keepsakes a finder can seal as an NFT on Base

The design source is in `docs/design/`. Start with `docs/design/README.md`, then read only the spec sections and boards for your slice. Never load the whole spec (over 75 KB) or every board.

## Stack and layout
- One module, `:app`, package `com.bottlecast.app`.
  - Kotlin 2.4 via AGP 9.4's built-in Kotlin.
  - Jetpack Compose with Material 3, Navigation Compose, Hilt (KSP).
  - Firebase Auth/Firestore/Analytics/Crashlytics, and Google sign-in through Credential Manager.
  - Lottie, Timber.
  - minSdk 33, compileSdk/targetSdk 37, Java 11.
  - Gradle 9.8 with the configuration cache and build cache on. Versions live in `gradle/libs.versions.toml`.
- Code is in `app/src/main/kotlin/com/bottlecast/app/`. Build-type code is in `app/src/debug/kotlin/` and `app/src/release/kotlin/`. Tests are in `app/src/test/java/` (JVM) and `app/src/androidTest/java/` (device); both hold Kotlin.
- Shell: `MainActivity` → `BottlingTheme` → `MessageInABottleApp.kt`. That file is the NavHost; route strings are in `Navigation.kt`. `MessageInABottleApp` applies the system-bar, cutout and keyboard insets once for every screen, so screens must not add them again.
- `common/authentication/` and `common/messages/` split into:
  - `domain`: `I`-prefixed interfaces and entities
  - `data`: implementations, with Firestore models in `data/model/` and mappers in `data/mapper/`
  - `di`: Hilt modules

  `common/analytics/` and `common/threading/` are flat, plus `di`.
- `common/ui/` is the theme: `Theme.kt`, `Color.kt`, `Typography.kt`, `Shapes.kt`. `common/components/` holds shared composables.
- `feature/<name>/` holds a screen and its ViewModel (`postmessage` keeps them in `ui/`). Composables that only one feature uses go in its `components/`; use cases go in its `domain/`.

## Conventions
- **ViewModels:** `@HiltViewModel class XViewModel @Inject constructor(…)` with:
  - `private val _state: MutableStateFlow<XUiState> = MutableStateFlow(…)`
  - `val state: StateFlow<XUiState> = _state.asStateFlow()`

  The sealed state class lives in the ViewModel's file; name new ones `XUiState`. Launch work with a `CoroutineExceptionHandler` that logs with Timber and sets a failure state.
- **Screens:**
  - A public `XScreen(on…Handler: () -> Unit, viewModel: XViewModel = hiltViewModel())` collects with `collectAsStateWithLifecycle()`.
  - It passes plain state to an `internal` stateless overload, which `@Preview`s and screenshot tests call.
  - Tests can't see `private` functions. Today's screens use private ones (`HomeScreen`, `GetMessageView`), so make them `internal` before testing them.
- **UI text** lives in `res/values/strings.xml` (`stringResource`, `@StringRes` parameters). No hard-coded strings.
- **Hilt and dispatchers:** modules use `@InstallIn(SingletonComponent::class)` with `@Provides @Singleton`; leaf classes use `@Inject constructor`. Inject dispatchers with the qualifiers in `common/threading/` (`@DispatcherIO` and the rest), and switch with `withContext` in repositories.
- **Analytics:** add a case to the sealed `AnalyticsEvent` (snake_case name) and send it through `LocalTracker.current`. Tracker wiring is in two `AnalyticsModule`s, one in `src/debug` and one in `src/release`; change both.
- **Logging:** Timber only. Never log tokens, emails or message text, because release builds forward info and above to Crashlytics.
- **Redesign UI** takes colours, type, shapes and spacing from theme tokens: no literal hex or one-off sizes in composables. Use start/end, never left/right. The spec turns RTL on, though the manifest still sets `supportsRtl="false"`.

## Build and verify
- Gradle runs on Android Studio's bundled JDK (JBR 25) through `JAVA_HOME`; the build sets no toolchain. There is no CI, so these commands are the only gate.
- **Verify every change:** `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
- **Release bundle:** `./gradlew :app:bundleRelease`, in its own invocation. Never run lint and a release task in the same invocation (lint/Hilt race).
- **Instrumented tests:** `./gradlew :app:connectedDebugAndroidTest`. Read "Environments and safety" first.
- **Screenshot tests (Roborazzi on Robolectric):**
  - Record one class: `./gradlew :app:testDebugUnitTest --tests '*<Name>ScreenshotTest' -Proborazzi.test.record=true`
  - Record all: `./gradlew :app:recordRoborazziDebug`
  - Report diffs without failing: `./gradlew :app:compareRoborazziDebug`
  - Gate before committing UI work: `./gradlew :app:verifyRoborazziDebug`
  - Goldens are committed at `app/src/test/screenshots/<package>.<Class>.<method>.png`.
  - After a failure, Read `app/build/outputs/roborazzi/*_compare.png`, which shows the reference, the diff and the new image side by side. The report is at `app/build/reports/roborazzi/index.html`.
- Gradle skips up-to-date test tasks. When you need a fresh run as proof, add `--rerun-tasks`, or `--rerun` after a single task.
- **Fresh clones and worktrees don't build as they are.** These files are gitignored:
  - `app/google-services.json` (debug)
  - `app/src/release/google-services.json` (production)
  - `local.properties` (SDK path, signing values)

  Copy the ones you need from the main checkout (debug work needs `app/google-services.json`), and never commit them.

## Tests
- **Unit tests** (`src/test`):
  - JUnit4 with `org.junit.Assert`, backtick names, and `// given // when // then`.
  - mockk (`mockk<IFoo>()`, `every`/`coEvery`/`coVerify`) inside `runTest`.
  - New ViewModels and use cases get unit tests. ViewModel tests call `Dispatchers.setMain(…)`, because `viewModelScope` runs on Main.
- **Screenshot tests** (`src/test`, named `*ScreenshotTest`; see `UnbottledMessageCardScreenshotTest`):
  - Use `RobolectricTestRunner` and `@GraphicsMode(NATIVE)`.
  - Use `@Config(sdk = [37], application = Application::class, qualifiers = …)`. The plain `Application` keeps the Hilt `BottleCastApplication` out.
  - Full screens use `qualifiers = "w390dp-h844dp-xhdpi"`, the board size: PNG pixels ÷ 2 = board pixels.
  - Inside `captureRoboImage { }`, wrap the stateless composable in `BottlingTheme(darkTheme = …, dynamicColor = false)`. Capture every state in Night and in Dawn.
- **Never capture a running animation.** Hoist Lottie `progress` (and any other animation state) into a parameter and pass a fixed value.
  - Pass the composition in too: `rememberLottieComposition` parses on a background thread, so a capture can come out blank.
  - In tests, load it with `LottieCompositionFactory.fromRawResSync(context, R.raw.…).value`.
- **Re-recording goldens:** only for an intended change, and only after reading its `_compare.png`. Never re-record just to make the gate pass.
- **Instrumented tests** (`src/androidTest`):
  - `@HiltAndroidTest`, `HiltAndroidRule` at order 0, and `createAndroidComposeRule<MainActivity>()` from `androidx.compose.ui.test.junit4.v2`.
  - Fakes in `fakes/` replace real modules through `testdi/*Module` with `@TestInstallIn(replaces = […])`.
  - A screen test without Hilt uses `ComponentActivity` and builds its ViewModel from fakes, as `HomeScreenTest` does.
  - Look text up with `activity.getString(R.string.…)`. Wait with `waitUntil`; never sleep or add delays to fakes.

## Environments and safety
- **Firebase projects:** debug builds (`com.bottlecast.app.debug`) use the debug Firebase project; release builds (`com.bottlecast.app`) use production. Never commit, print or paste either `google-services.json`.
- **Production is off limits without asking.** Never sign in to, read or write production Firebase without asking first.
  - A production write needs an explicit yes, a backup and a dry run.
  - Before a Firebase smoke test, check that `project_id` in `app/build/generated/res/processDebugGoogleServices/values/values.xml` is the debug project.
- **Devices:** connected tests install and uninstall the debug app on every attached device, which wipes its data.
  1. Run `adb devices` first.
  2. Boot a throwaway emulator with `<sdk>/emulator/emulator -avd <AVD> -read-only -no-snapshot -no-window -no-audio -no-boot-anim -port <p>`. `<sdk>` is `$ANDROID_HOME`, or `sdk.dir` in `local.properties`.
  3. Run the tests with `ANDROID_SERIAL=emulator-<p>`.

  If a writable instance of that AVD blocks the boot, ask before stopping it.
- **Flaky instrumented tests:** install once with `./gradlew :app:installDebug :app:installDebugAndroidTest`, then repeat `adb shell am instrument -w -r com.bottlecast.app.debug.test/com.bottlecast.app.HiltTestRunner`.
- **Secrets** stay out of the repo, the logs and the chat: keystores, the signing values in `local.properties`, Worker secrets, minter keys and API tokens. The owner signs releases; never ask for or handle keystore passwords.
- **Releases are the owner's call.** Only when asked:
  - fast-forward `master` with `git push origin develop:master` (never force)
  - upload with `scripts/play_upload.py`. It needs `gcloud`, `bundletool`, and `PLAY_UPLOAD_ACCOUNT` when the active gcloud account isn't the right one.

  Show the owner the script's dry run (track, versionCode, notes) and get an explicit yes before running `release --yes`.
- **Chain work** runs on Base Sepolia. Deploying to Base mainnet, or funding a mainnet minter, needs the owner's approval. Ask before changing or deploying the sibling repos `../bottlecast-nft-contract` and `../bottlecast-web`; the web repo deploys to the production Firebase project.
- Notes specific to one machine belong in `CLAUDE.local.md` (gitignored), not here.

## Git and PRs
- **Branches:** branch from `develop` with a kebab-case name and open the PR into `develop` (`gh pr create --base develop`). Commit straight to `develop` only when the owner says so.
- **Commit messages:**
  - Subject: imperative, sentence case, no full stop, about 72 characters at most.
  - Body wrapped at 72 characters: the why or root cause first, then `- ` bullets listing the changes.
- **PR description:** what changed and why, and how you verified it (commands run, screenshot tests added). The golden PNGs in the diff show the UI.
- **Version bumps:** changing `versionCode`/`versionName` in `app/build.gradle.kts` is a commit of its own.
- **The repo is public.** Never add any of these to code, docs, commits or PRs:
  - personal emails or account names
  - keys, tokens or signing values
  - new console identifiers (Firebase project IDs, Play account or app IDs)

## Implementing a redesign slice
1. **Plan first, in plan mode.** Read `docs/design/README.md` and `docs/design/decisions.md`, then the slice's sections of `docs/design/spec.md` and its boards in `docs/design/boards/`. Hand several boards to a subagent rather than reading them all yourself. Raise the slice's open items from `docs/design/open-issues.md`.
2. **Plan the files, states and tests,** and say what is out of scope. Get the plan approved.
3. **Branch from `develop` and implement.** When sources disagree: `decisions.md` beats `spec.md` (its §6 lists where a board wins), which beats the boards, which beat `wallets.md` and `research/`.
4. **Add or extend a `*ScreenshotTest`** for every new or changed screen state, in Night and in Dawn. Record it, then Read each PNG.
5. **Compare each PNG with its board's markup** (you can't render the boards): layout, spacing, colour, type, copy and icons. Fix and re-record until they match, or until the only difference is one that spec §6 accepts.
6. **Run the verify command, then `verifyRoborazziDebug`.** For new navigation or flows, add instrumented tests and run them on a read-only emulator.
7. **Finish up:**
   - run `/code-review` and fix what it finds
   - tick the slice in `docs/design/README.md`
   - update this file if a command, convention or gotcha changed
   - open the PR

## Copy rules
- Sentence case. No emoji in labels, buttons or titles; user messages may contain them.
- Never use "earn", "value", "worth", "rare", "rarity", "profit" or "invest". Say "keep", "collect" and "seal". Badges are "collected".
- Warm, never shaming: "Streak resting", not "You lost your streak!". Every error says what is safe and what to do next.
- No prices. For fees, say "Free for you — BottleCast covers the tiny network fee." Where a no-money line is needed, say "Keepsakes are mementos, not money."
- Claim permanence ("for good", "can't be erased") only for Base mainnet. Only debug builds show the "Test network" chip, and they say test-network keepsakes may be reset.
- Keep crypto jargon out of primary copy. Detail rows name each technical term once (spec §1.7).

## Gotchas
- **Dynamic colour:** `BottlingTheme` defaults to `dynamicColor = true`, so colours come from the wallpaper and `Color.kt` is never shown. Slice 1 turns it off.
- **Material3:** Compose BOM 2026.09.00 resolves to Material3 1.4.0. There, `MotionScheme.expressive()` is internal, and the wavy progress indicators and `MaterialShapes` don't exist. Material3 1.5 is only in beta, so pinning it is the owner's call; otherwise use the spec's fallbacks.
- **Built-in Kotlin:** with AGP 9's built-in Kotlin, don't apply `org.jetbrains.kotlin.android`. Give Kotlin compiler plugins (compose, serialization) the `pluginKotlin` version.
- **Analytics route names:** screen analytics send `destination.route` (`MessageInABottleApp`). Type-safe routes turn that into a qualified class name, so keep stable snake_case screen names when the routes change.
- **Lottie race (b1d8a34):** never gate the bottle Lottie on `uiState`. The ~630 KB composition parses asynchronously, taking over 1 s on a cold start. Let it play its single iteration, reveal only when `progress == 1f` and the state is final, and make sure rotation can't freeze it.
- **Firestore models and R8:** models must be `@Keep`, because release 1.0 wrote R8-obfuscated `a`/`b` fields. Mappers read named fields and skip malformed documents. R8 is on in release and `proguard-rules.pro` has no rules, so smoke-test a release build after adding libraries that rely on reflection.
- **Robolectric setup:** it needs the `--add-opens` JVM flags set on the `Test` tasks in `app/build.gradle.kts` (JDK 17+). Keep unit-test Espresso at 3.7.0 or later: Roborazzi brings 3.5.1, which fails on SDK 37 with `InputManager.getInstance()`.
- **Fonts under Robolectric:** downloadable Google Fonts don't load, so each weight falls back to the next `Font` in its family. Only Roboto Slab is bundled in `res/font/`. Bundle the Roboto Flex (400, 500, 600) and Roboto Mono (400, 500) weights the design uses, or screenshots won't match devices.
- **Debug Google sign-in failing with `[28444]` or `NoCredentialException`:** usually Firebase config drift (the debug Android OAuth client is missing), not app code. Check that `app/google-services.json` has an `oauth_client` with `client_type: 1`. Fixing it changes the Firebase project, so ask first.
- **AppCompat:** `Theme.Bottling` extends `Theme.AppCompat.NoActionBar`, but AppCompat only arrives as a transitive dependency.
