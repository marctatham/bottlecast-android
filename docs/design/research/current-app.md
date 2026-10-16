# BottleCast as of 1.1.1 (before the redesign)

> Snapshot of the app at versionName 1.1.1 (2026-10-12), written to plan the redesign. It goes stale as slices land — the code wins.

This page maps the app as it stood before the Moonlit Shore redesign: the UI layer (theme, components, screens, animations and copy) and the data and auth layers. The last section lists what the redesign changes, with the slice that covers each item. The slices are listed in [README.md](../README.md); [decisions.md](../decisions.md) and [spec.md](../spec.md) describe the redesign itself.

Kotlin paths are relative to `app/src/main/kotlin/com/bottlecast/app/`. Other paths are relative to the repo root as it was then. Since the move to a monorepo (2026-10-16), the Android project is in `android/`, and the sibling repos `../bottlecast-web` and `../bottlecast-nft-contract` are `web/` and `contracts/`.

## 1. At a glance

- One activity (`MainActivity`) hosts a Compose `NavHost` (`MessageInABottleApp.kt`) with five string routes.
- **Discover** shows a random message from the Firestore `messages` collection. It needs no sign-in.
- **Send** needs a signed-in user, Google or anonymous. A message is stored as its text plus the poster's UID.
- The admin account also gets an admin-only debug list of every message.

## 2. Theme and tokens

### Dynamic colour hides the brand palette

`BottlingTheme(dynamicColor = true)` is the default, `MainActivity` never overrides it, and minSdk is 33. Every device therefore uses dynamic colour (wallpaper-based Material You), and the brand palette in `common/ui/Color.kt` is never shown.

The sage status bar and buttons and the charcoal arc in screenshots of 1.1.1 come from dynamic colour, not from the brand, so other users see different colours. A fixed palette needs `dynamicColor = false`.

### Static palette (in code, never shown)

It was built with Material Theme Builder from the seeds #00C2FF (the same cyan as the home Lottie), #FFD600, #7000FF and #EDF9FF. In the light scheme, secondary is a gold and tertiary a purple.

| Role | Dark | Light |
|---|---|---|
| primary / onPrimary | #75D1FF / #003548 | #006688 / #FFFFFF |
| primaryContainer | #004D67 | #C2E8FF |
| secondary / secondaryContainer | #E9C400 / #544600 | #705D00 / #FFE170 |
| tertiary | #D1BCFF | #7212FF |
| error | #FFB4AB | #BA1A1A |
| background = surface, and its text colour | #001F2A, text #BFE9FF | #FAFCFF, text #001F2A |
| surfaceVariant | #40484D | #DCE3E9 |
| outline | #8A9297 | #71787D |

Light and dark both follow the system setting.

### Where each colour role is used

| Role | Used for |
|---|---|
| background | Every screen |
| surfaceVariant | The curved arc on Home (charcoal in screenshots of 1.1.1) |
| primary | Filled buttons, and a strip painted behind the status bar. Status-bar icons turn dark in dark mode |
| surface | The message card and the icon buttons |
| outline | Grey body text on the sign-in screen, the legal line and the compose placeholder |
| error | The character counter while under 10, and failure text |
| secondaryContainer | Cards in the admin message list |

Outside Compose:
- The launcher icon background is #FFFFFF (#75D1FF in debug builds).
- The window theme is the dark `Theme.AppCompat.NoActionBar`.
- There is no splash screen.

### Typography

Roboto Slab only. It loads as a downloadable Google Font through Google Play services (certificates in `font_certs.xml`), with nine bundled TTF weights in `app/src/main/res/font/` as the fallback.

| Style | Size / line (sp) | Weight | Used for |
|---|---|---|---|
| displayLarge | 57/64 | Black | "BottleCast", "Message Bottled!" |
| headlineSmall | 24/32 | SemiBold | App-bar title, dialog title |
| titleLarge | 22/28 | Bold | Discovered message, sheet and sign-in titles |
| titleMedium | 16/24 | SemiBold | Button labels, tagline, admin list |
| bodyLarge / bodyMedium / bodySmall | 16 / 14 / 12 | Normal | Compose input; counter and body text; legal and error text |

The other styles are defined but unused.

### Shape and spacing

- **Shapes:** 4 / 8 / 16 / 24 / 32dp. Buttons and the message card use 16dp, so a 60dp-tall button is a rounded rectangle, not a pill. Dialogs use 32dp.
- **Spacing:** 16dp side margins, 8dp between buttons, full-width 60dp buttons, 20dp card elevation and 24dp icons. There are no spacing tokens.

## 3. Components

The shared components are in `common/components/`; the others sit in their feature's folder.

- **BottlingButton**(text, modifier, enabled, isLoading, buttonType PRIMARY|SECONDARY, onTapHandler): a filled or outlined button with a centred label and a spinner on the right while loading.
- **BottlingAppBar**(modifier, onBackHandler, title?): a centred top bar with a back arrow in a filled icon button.
- **CustomBox**(canvasContent, outsideCanvasContent): draws a surfaceVariant circle with a radius of 50% of the screen height, centred 10% down the screen. Its edge forms the arc about 60% down the screen. The top slot gets 60% of the height.
- **EducationalScreen**(image, imageDescription, title, description, primary and secondary text and handlers, onBackHandler): a 200dp icon, left-aligned grey text and buttons at the bottom.
- **UnbottledMessageCard**(isReported, onReportHandler, unbottledMessage, modifier): a raised card with centred text and a flag button (outlined, then filled once reported).
- **ReportMessageDialog**(onDismiss, onReportConfirmed): a standard Material alert dialog.
- **LoginBottomSheet**(onSignInCompleteHandler, onDismissRequest, sheetState): a Material3 `ModalBottomSheet` with Google and anonymous sign-in buttons, an error line, a legal line and a spinner overlay.
- **Smaller pieces:** BottlingFailureReason (error text), and the admin list views MessageView and MessageListView.

## 4. Screens and navigation

Routes are plain strings with no arguments, defined in `Navigation.kt`. Screen changes use Navigation Compose's default crossfade.

| Route | What it shows | Who can reach it |
|---|---|---|
| `home` (start) | The CustomBox arc, a looping Lottie, the title pulled up by -100dp, the tagline, and the Discover and Send buttons. The admin account sees "View ALL" in place of the tagline | Everyone |
| `get_messages` | The discover Lottie plays once, then the message card is revealed. On failure only a system error icon appears. Back and the button both return Home | Everyone |
| `post_message_login_required` | The sign-in explainer. "Agree" opens the sign-in sheet; on success the route is replaced by `post_message` | Signed-out users |
| `post_message` | A full-height text field with no border, a counter and "Cast Away!". On success: CustomBox plus the home Lottie | Signed-in users, including anonymous ones |
| `view_messages` | A list of every message, with no top bar | Admin-only debug list |

There is no profile, settings, sign-out, onboarding or deep linking.

### Discover (`get_messages`)

1. Every visit picks a new random message (`GetMessageViewModel.init`; the query is in section 7).
2. The discover Lottie plays through once. When it has finished and the message has loaded, a 2s crossfade reveals the card, which also slides down over 3s.
3. On failure the screen shows only the stock `android.R.drawable.stat_notify_error` icon.
4. The card shows the text alone, with no author. It can be the user's own message.
5. The flag opens the report dialog. Confirming it sets `isReported` in `GetMessageScreen.kt`, which fills the flag icon. Reports aren't saved yet (slice 2).
6. The one action, "Message Explored!", goes Home.

### Sending: sign-in, compose and success

1. Home's "Send Your Own" checks the user (`AppNavigationStateViewModel`). Signed out, it opens the explainer; signed in (Google or anonymous), it goes straight to Compose.
2. **Explainer** (`LoginRequiredToPostScreen`, an EducationalScreen): an AccountCircle icon, the title "Your Guide to Bottling Messages", long playful privacy copy that promises personal info never goes into the message, "Agree and Set Sail" (primary) and "No Thanks, I'll Stay Dry" (secondary, goes Home).
3. **Sign-in sheet** (`LoginBottomSheet`): "Log in to continue", "Sign in with Google" (primary), "Sign in Anonymously" (secondary), an inline error line, a centred spinner, and a legal line that names a User Agreement and a Privacy Policy without linking to them.
4. **Google sign-in** goes through Credential Manager (`GetGoogleIdOption`: all accounts, auto-select on). The ID token is passed to Firebase `signInWithCredential`.
5. **On success** the explainer is removed from the back stack and Compose opens.
6. **Compose:** a borderless full-height `TextField`, a counter and "Cast Away!". Errors appear as inline red text above the counter. The length rules are in section 12.
7. **Success:** CustomBox with the home Lottie, "Message Bottled!", one line of copy and "Back to adventure". The posted message isn't shown again and can't be shared.

### Admin list (`view_messages`)

An admin-only debug list, reached from Home's "View ALL" button, which only the admin account sees. It loads the whole collection with no paging and shows plain cards in a LazyColumn. `purgeMessages()` is an empty TODO.

## 5. Lottie animations

Both files are in `app/src/main/res/raw/`. They use different art styles (licence to confirm; replaced by the bottle rig).

### `message_in_bottle.json` (Home and post success)

- 616 KB, 25fps, frames 0–100 = 4.0s. The app plays it at 0.6× speed, so one loop takes about 6.7s.
- Canvas 1200×800; the composition is named "合成 1" ("Composition 1").
- Mostly raster, with four embedded PNGs:
  - 瓶身 (bottle): cyan, about #00C0F0, with white highlights.
  - 卷轴 (scroll): cream and tan, #F0E0D0, #E0C0A0 and #E0B090.
  - 底座 (base): a cyan splash.
  - Union.png: a white wave used as a mask, scrolling across a pale #EEFBFF ellipse that forms the water.
- Nine #00C2FF ellipses act as ripples, starting 17 frames apart.
- The bottle rocks from 0 to 37° and bobs 20px. The scroll swings between -65° and -9°.

### `message_from_bottle.json` (Discover)

- 644 KB, 30fps, frames 0–76 = 2.53s, played once.
- Canvas 375×667; the composition is named "成为密友 5". All vector, with thick outlines.
- Sequence:
  - 瓶塞子 (cork) pops off to the left (frames 0–10).
  - 瓶子 (bottle) tilts to -95° (frames 11–30), then flies off the top right (frames 45–55).
  - 漂流纸 (paper) slides out and grows from 60% to 100% (frames 0–40).
  - A cream note fades in (frames 55–65) showing the Chinese text "成为密友" ("become close friends") in #4C435F.
- Colours: #4C435F aubergine outlines (the main colour), #EDF6D9 paper, #47B7D7 glass, #CDE7FF and #F1F3FF highlights, #FCC867 marigold, #BF5345 cork, and #FF6F5C and #FFA79C coral. #FF0000 appears only in hidden masks.

`app/src/main/res/drawable/ic_floating_bottle.xml` is unused. It is black line art, the same as the launcher icon.

## 6. Copy

From `app/src/main/res/values/strings.xml`, verbatim except that each emoji is written as its [name].

- **Home:** "BottleCast" · "Ready for a virtual adventure filled with serendipitous discoveries? Find a hidden message in a virtual bottle or cast your own into the digital sea for others to uncover." · "Discover a Message [desert island]" · "Send Your Own [water wave]" · "View ALL [ghost]"
- **Discover:** "Message Explored! [glowing star]" · "Report Message" · "Report Offensive Message" · "Are you sure you want to report this message as offensive? Our team will review it, and if it is found to be offensive, it will be removed from circulation to maintain a safe and enjoyable experience for all users." · "Cancel"
- **Sign-in explainer:** "Your Guide to Bottling Messages" · "Safety First, sign in is required" (screen-reader label) · "Ready to cast your message adrift in a virtual bottle? [water wave] You’ll need to be signed in to set sail on this adventure. It’s not just about keeping the high seas clean; it’s also about making sure our virtual beach stays family-friendly!\n\nDon’t worry though, your secrets are safe with us! [zipper-mouth face] We’ve got your back on privacy, and we’ll never spill your personally identifiable info into the message. Even if you choose to go incognito, your identity will remain as elusive as a message in a real bottle lost at sea! [pirate flag][anchor]" · "Agree and Set Sail" · "No Thanks, I'll Stay Dry"
- **Sign-in sheet:** "Log in to continue" · "Sign in with Google" · "Sign in Anonymously" · "By continuing, you agree to our User Agreement and acknowledge that you understand the Privacy Policy." · "There was a problem signing you in, please try an alternative sign-in method." · and a toast hard-coded in Kotlin: "There was a problem signing in with Google"
- **Compose:** "Your BottleCast" · "[glowing star] Need a nudge to start? How about this: \nIf you could send a message to anyone, anywhere, what would it say?" · `%1$s/%2$s` (the counter) · "Cast Away!" · "Your message must be at least `%1$s` characters long" · "You must be logged in to post!" · "Failed to post your message!"
- **Post success:** "Message Bottled!" · "Your message has been bottled up and is ready to set sail!" · "Back to adventure"
- **Other:** "There was a problem fetching messages" · "Back"

Tone: nautical puns, an emoji on most buttons, and inconsistent capitalisation.

Issues in the strings file:
- The `button_back` string ends in `</string>>`, with an extra `>`.
- Five strings contain stray `"` characters: the admin list's fetch error, the three compose snackbars and the counter.

## 7. Data: messages in Firestore

**Collection `messages`.** Each document has exactly two fields (`common/messages/data/model/MessageFirestoreDataModel.kt`):
- `messageContent` (string)
- `userId` (string): the poster's Firebase UID, Google or anonymous

There is no timestamp, language, status, report count, discovery count or display name. `MessageEntity(message, userId)` doesn't carry the document ID, so the app can't refer back to a specific message later (to report, keep or share it).

Release 1.0 wrote R8-obfuscated `a`/`b` fields, so the mapper skips documents whose `messageContent` is blank or missing.

**Random pick** (`MessageFirestoreSource.getMessage()`):
1. Generate a Firestore `autoId()`.
2. Query documents with `documentId >= randomId`, ordered by document ID, limit 10, and take the first readable one.
3. If there is none, wrap around and take the first readable document from the start.

## 8. Auth

**What the app knows about the user.** `UserEntity.AuthenticatedUser` holds:
- `providerType` (Google or Anonymous)
- `jwtToken`
- `userId` (the UID)
- `isAdmin`

The app doesn't expose the display name, photo or email, although `FirebaseUser` has them for Google sign-ins. The UID is also set as the Crashlytics user ID.

**Anonymous counts as signed in.** Firebase anonymous sign-in is offered, and an anonymous user can post, so "only signed-in users can send" means "Google or anonymous". The app never links an anonymous account to Google later. `signOut()` exists, but no screen calls it; a TODO says it will move to a "profile screen that's coming".

**When sign-in is needed.** Only for posting, through the flow in section 4. Discovery needs no sign-in.

## 9. Libraries, SDK levels and Firebase projects

From `gradle/libs.versions.toml` and `app/build.gradle.kts`:

| Item | Version or setting |
|---|---|
| Compose BOM | 2026.09.00 |
| Material3 (via the BOM) | 1.4.0. The redesign needs 1.5 for its expressive APIs (see spec.md) |
| Compose UI | 1.12.1 |
| Material icons extended | 1.7.8 |
| Navigation Compose | 2.10.2 (string routes, not type-safe) |
| Lottie and lottie-compose | 6.7.1 |
| Credentials | 1.6.0 |
| googleid | 1.2.1 |
| Firebase BoM | 35.0.0 (Auth, Firestore, Analytics and Crashlytics only) |
| Hilt | 2.60.1 |
| Kotlin | 2.4.21 |
| AGP | 9.4.1 |
| Lifecycle | 2.11.0 |
| Fonts | Roboto Slab via `ui-text-google-fonts`, with bundled fallbacks |
| SDK levels | minSdk 33, targetSdk 37, compileSdk 37 |
| App version | versionCode 4, versionName 1.1.1 |

**Firebase projects:**
- Debug builds (`com.bottlecast.app.debug`) use the debug Firebase project.
- Release builds (`com.bottlecast.app`) use `bottlecast-prod`.

## 10. Analytics

Events are defined in `common/analytics/AnalyticsEvent.kt`:
- `get_message_tapped`
- `post_message_tapped`

Both fire from Home. A `screen_view` is also logged on every navigation change, with the route names `home`, `get_messages`, `post_message`, `post_message_login_required` and `view_messages`.

Nothing is tracked for sign-in, post success, reports or the reveal. Debug builds log to Timber and to the debug Firebase project; release builds log to Firebase only.

## 11. Tests

- **Unit:** mapper edge cases, and the sign-in check in `PostMessageUseCase`.
- **Instrumented** (with fake repositories): Home shows its title and actions; Discover, then the reveal, then "Message Explored!" goes Home; Discover, then Back, goes Home.
- Nothing covers sign-in, the compose UI, reporting or the admin list.

Roborazzi screenshot tests arrived after this snapshot, in the setup slice (slice 0).

## 12. Gaps relative to the redesign

Slice numbers refer to the slice list in [README.md](../README.md).

| Area | In 1.1.1 | In the redesign |
|---|---|---|
| Colour and theme | Dynamic colour decides every colour; `Color.kt` is never shown | Fixed Night and Dawn themes with dynamic colour off (slice 1) |
| Navigation and shell | String routes, crossfades, no navigation bar and no splash screen | Type-safe routes, a Shore/Chest/Me navigation bar, edge-to-edge and a splash screen (slice 1) |
| Home layout | The -100dp title offset plus 57sp text can overlap the Lottie at large font sizes | Shore (slice 1). Every layout must hold at 200% text size (spec §4) |
| Discover animation | Ends on the Chinese note, which stays up for as long as loading takes. Its dark outlines barely show on a near-black background | The bottle rig, with shake, tap or hold to uncork (slice 2) |
| Message card | A long message overflows the card, which doesn't scroll | A parchment card that scrolls inside (slice 2) |
| Data model | `{messageContent, userId}` only; the document ID isn't kept | Adds `createdAt`, `lang`, `status`, `rand`, `id`, `penName`, `sealable` and `reportCount` (slice 2) |
| Daily limit | Discovery has no limit, can return the user's own or already-seen messages, and records nothing | Calendar-day tides: one find per local day, recorded in Firestore (slice 2) |
| Reporting and moderation | Reports aren't saved yet (slice 2). Admin tooling is the read-only debug list | A report hides the bottle for the reporter at once, the bottle leaves the sea after several reports or a review, and the dialog adds "Block this writer" (slice 2) |
| Security rules | Rules are managed in the Firebase console | Slice 2 adds `firestore.rules` to the repo |
| Message length | Minimum 10 characters, checked on the trimmed text; no maximum yet. The button uses the untrimmed length, so a whitespace-padded message can enable "Cast Away!" and then fail with "at least 10 characters". The counter shows "n/10" in the error colour until the minimum, then just "n", so colour is the only cue | Compose (slice 3): input stops at 240 characters or 280 UTF-8 bytes, and near the limit the counter shows text as well as colour |
| Sign-in | Google or anonymous, offered only on the way to posting. The legal line names a User Agreement and a Privacy Policy without links. The sign-in sheet's previews are TODO stubs | A lazy Google sign-in sheet, the guest upgrade and an explicit terms step before the first cast (slice 3) |
| Account and profile | No `users` collection, profile or stats. `signOut()` has no UI | The Me tab, stats and Settings, including sign-out (slice 4) |
| Keeping and collecting | Nothing records a find | Keep and the Chest (slice 4); sealing a keepsake as an NFT (slice 6) |
| Notifications | No FCM dependency, no `POST_NOTIFICATIONS` permission and no WorkManager | A daily reminder with quiet hours (slice 4) |
| Sharing and deep links | Only the LAUNCHER intent filter: no share intents or link handling, and a posted message can't be shared | Gifts, App Links and the `/b/{token}` and `/u/{handle}` web pages (slice 7) |
| Wallets and NFTs | Nothing in the app. `../bottlecast-nft-contract` is an untouched Hardhat template (sample `Lock.sol`, OpenZeppelin 5.3 installed, one commit) | Spikes (slice 5), then the contract, wallet linking and sealing on Base Sepolia (slice 6) |
| Server | No Cloud Functions and no backend. `../bottlecast-web` is a static Firebase Hosting site (privacy and data-deletion pages) on `bottlecast-prod`, at `bottling-messages.web.app` | A Cloudflare Worker (slice 6), and the web side of gifts and App Links in `../bottlecast-web` (slice 7) |
| Achievements, translation and on-device AI | None | Slice 8 |
| Failure states | A system icon with no text and no screen-reader label. The admin list's error uses a hard-coded 24sp | Edge states with copy (slices 2 and 8) |
| Accessibility | Grey (`outline`) body text has low contrast, the Lotties have no descriptions, screen readers read the emoji aloud, and right-to-left layouts are off (`supportsRtl=false`) | Spec §4 applies to every slice and turns RTL on |
| Privacy promise | The README and the sign-in copy promise anonymity ("we’ll never spill your personally identifiable info into the message") | Bottles stay anonymous by default. The on-chain mark is a keyed hash, and a pen name appears only if the writer opts in ([decisions.md](../decisions.md)) |
| Analytics | Only the two Home taps and `screen_view` | Not in the spec yet |
| Strings | The `button_back` typo and the stray quotes (section 6) | Rewritten as each screen is rebuilt |
| Tests | Section 11 | Screenshot tests for every screen state in Night and Dawn, from slice 0 |

## Sources

- `app/src/main/kotlin/com/bottlecast/app/common/ui/Theme.kt` and `Color.kt`
- `app/src/main/kotlin/com/bottlecast/app/MessageInABottleApp.kt` and `Navigation.kt`
- `app/src/main/kotlin/com/bottlecast/app/common/messages/` and `common/authentication/`
- `app/src/main/kotlin/com/bottlecast/app/feature/`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/raw/`
- `gradle/libs.versions.toml` and `app/build.gradle.kts`
