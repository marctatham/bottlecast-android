# BottleCast platform research: the non-blockchain pieces on a zero budget (as of 2026-10-12)

> Research notes from 2026-10-12, kept for their sources. Superseded by [docs/design/spec.md](../spec.md) and [decisions.md](../decisions.md) where they disagree — notably:
> - Calendar-day tides replace the 20-hour cooldown (sections B and C): a new bottle arrives at local midnight, each local day allows one find, and the reminder fires at a time the user sets (default 9:00, with quiet hours) rather than when a cooldown ends.
> - Firebase Auth on Spark with Identity Platform caps at 3,000 daily active users, not 50K MAU (section E).
> - Material3 1.4.0 lacks the expressive APIs (section G): `MotionScheme.expressive()`, the wavy progress indicators and `MaterialShapes` need 1.5.
> - Claims, wallet linking, minting and the "your bottle was found" push run on a free Cloudflare Worker, not Cloud Functions on Blaze (sections C and E).
> - Firestore's daily quotas reset around midnight US Pacific, which is when the spec's "harbour closed" state reopens (section E).

## A. On-device AI

**Gemini Nano through the ML Kit GenAI APIs** ([overview](https://developers.google.com/ml-kit/genai))
- **Runs on AICore.** Every feature is still pre-GA:
  - Beta: Summarization, Proofreading, Rewriting, Image Description, Prompt.
  - Alpha: Speech recognition, and Prompt's structured output.
- **Devices are flagships only:**
  - Pixel 9, 10 and 11 series; Galaxy S25 and S26; Z Fold7 and Z Fold8; OnePlus 13 and 15; Xiaomi 15 and 17; some Honor, OPPO and vivo models.
  - The Prompt API model generation differs by device: nano-v2, v3 or v4.
  - **Every AI feature must be an optional extra.**
- **Download states:** `checkFeatureStatus()` returns `UNAVAILABLE`, `DOWNLOADABLE`, `DOWNLOADING` or `AVAILABLE`. `downloadFeature()` reports `onDownloadStarted`, `onDownloadProgress`, `onDownloadCompleted` and `onDownloadFailed`. If you skip that call, the first inference starts the download ([proofreading](https://developers.google.com/ml-kit/genai/proofreading/android)).
- **Quotas.** No numbers are published.
  - A per-app inference limit returns `BUSY`; retry with backoff.
  - A long-window battery limit returns `PER_APP_BATTERY_USE_QUOTA_EXCEEDED`.
  - Inference works only in the foreground. Background use returns `BACKGROUND_USE_BLOCKED`, so a worker cannot run inference.
- **Cost:** no per-call server cost.
- **Proofreading and Rewriting:** English, Japanese, French, German, Italian, Spanish and Korean; input must be under 256 tokens. Rewrite styles are Elaborate, Emojify, Shorten, Friendly, Professional and Rephrase ([rewriting](https://developers.google.com/ml-kit/genai/rewriting/android)).
- **Summarization:** English, Japanese and Korean only. Article input needs more than 400 characters, so it is a **poor fit for short bottle messages** ([summarization](https://developers.google.com/ml-kit/genai/summarization/android)).

**ML Kit Translation and Language Identification**
- **Translation** ([guide](https://developers.google.com/ml-kit/language/translation/android), [overview](https://developers.google.com/ml-kit/language/translation)):
  - Runs on the device and works offline.
  - Covers 50+ languages. Models are about 30 MB per language, managed with `RemoteModelManager`; download with `requireWifi()` as the docs advise.
  - Google positions it for "casual and simple translations". Pairs where neither language is English translate through English.
  - Apps must follow the Cloud Translation attribution rules ([terms](https://developers.google.com/ml-kit/language/translation/translation-terms)).
  - Being free is UNVERIFIED: the pages do not state a price.
- **Language Identification:** 100+ languages; returns `und` when it cannot tell ([docs](https://developers.google.com/ml-kit/language/identification)).

**Recommended "Translate" flow**
1. **On cast:** the writer's device runs Language ID and stores `lang` on the message, so readers do not have to.
2. **On reveal:** if `lang` differs from the reader's locale and is not `und`, show a "Translate from Spanish" chip.
3. **If the model is missing:** show a sheet saying "Download Spanish (~30 MB) · Wi-Fi recommended", then a progress state. Translation's download API probably gives no percentage (UNVERIFIED), so use an indeterminate bobbing-bottle loader.
4. **Translated:** show the text with a "Translated · Show original" toggle and the attribution.
5. **Errors:** if offline or the download fails, show "Try again on Wi-Fi" and keep the original text visible.

Translation runs on any API 23+ device, so this flow needs no fallback for unsupported devices.

**Other AI ideas** (free and on-device, but only on Gemini Nano devices; hide them elsewhere)
- **Kindness check before "Cast Away!":** use Rewriting in Friendly mode plus Proofreading as a "Polish" suggestion the writer can accept or ignore.
- **"Emojify" button** using the Rewriting API.
- **"Nudge me" button:** the Prompt API generates three openers to replace the static placeholder.
- **Bottle title or mood:** the Prompt API picks an emoji, a two-word title or a colour tint. Structured output is Alpha.
- **Moderation pre-check:** the Prompt API classifies the draft and warns the writer. This is advisory only, because client-side checks cannot be trusted. Keep the Report flow.

## B. One discovery per day

**Rules-only enforcement (works on Spark)**
- Make discovery one batched write with two parts:
  - Create `users/{uid}/discoveries/{msgId}`, or set `messages/{id}.discoveredBy`.
  - Update `users/{uid}.lastDiscoveryAt = serverTimestamp()`.
- **Rules on the user doc:** `request.resource.data.lastDiscoveryAt == request.time`, plus a cooldown check against `resource.data.lastDiscoveryAt`.
- **Rules on the message or discovery doc:** use `getAfter(/users/$(uid)).data.lastDiscoveryAt == request.time` so neither write can happen alone ([conditions](https://firebase.google.com/docs/firestore/security/rules-conditions)).
- **Limits:** 10 `get` calls per evaluation, 20 per batch. Rule `get()` calls are billed as reads, even when the request is rejected.

**Timezones.** Rules only see UTC; `timestamp.date()` is midnight UTC ([ref](https://firebase.google.com/docs/reference/rules/rules.timestamp)). There are three options:
1. **UTC day:** simple, but the reset lands at odd local hours.
2. **Rolling cooldown, e.g. 20 h (recommended):** no timezone problem, and the UI can show "Next bottle washes ashore in 3h 12m". A cooldown under 24 h stops the daily time drifting later.
3. **Local midnight:** needs a timezone offset sent by the client, which can be faked.

**Honest limit of this approach.** Rules can enforce the *discovery record*. They cannot stop a modified client from *reading* other message documents. You can reduce this with `allow list: if request.query.limit <= 3`, but truly hiding content needs a backend, which means Blaze.

**Random message without a backend**
- Store `rand` (a double from 0 to 1) at creation.
- Query `status == "floating" && rand >= r`, ordered by `rand`, with `limit(3)`. If that returns nothing, wrap around with `rand < r`.
- Filter out your own and already-seen messages on the client. That is cheaper than adding an `authorId != uid` inequality, which needs a composite index (verify).
- If one bottle has one finder, as in the NFT idea, flipping `status` to `"discovered"` removes it from the pool automatically.

## C. Notifications

- **FCM is free, but scheduled sends need a backend.** That means scheduled Cloud Functions on Blaze. The console Notifications composer offers recurring campaigns, but daily delivery in each user's local time is UNVERIFIED ([2018 post](https://firebase.blog/posts/2018/11/introducing-redesigned-firebase)).
- **Recommended zero-cost approach: a local notification.**
  - After each discovery, enqueue a one-time WorkManager job at `nextBottleReadyAt`. Replace it after every discovery.
  - Timing is not exact, which is fine for "your bottle is ready". The periodic minimum is 15 minutes ([WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)).
  - **Avoid exact alarms.** `SCHEDULE_EXACT_ALARM` is denied by default on Android 14+ for new installs. `USE_EXACT_ALARM` is for alarm and calendar apps only ([Android 14](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)).
  - Android 17 changes are UNVERIFIED. There are press reports of new user controls for sorting notifications.
- **Permission UX** ([docs](https://developer.android.com/develop/ui/views/notifications/notification-permission)):
  - Ask in context, after the first discovery: a card saying "Want a nudge when tomorrow's bottle washes ashore?", then the system dialog.
  - Show an extra rationale only when `shouldShowRequestPermissionRationale()` returns true. Check `areNotificationsEnabled()` before posting.
  - Users can see how many notifications per day an app sends.
- **Channels:**
  - "Daily bottle" (default importance).
  - "Achievements" (low).
  - Later, "Your bottle was claimed". That one needs a server push, so Blaze.

## D. Sharing and deep links

- **Firebase Dynamic Links shut down on 25 August 2025.** Links now return 404. Google recommends App Links, with `assetlinks.json` optionally hosted on Firebase Hosting ([FAQ](https://firebase.google.com/support/dynamic-links-faq)).
- **Hosting on Spark is free:** 10 GB storage and 360 MB/day transfer ([pricing](https://firebase.google.com/pricing)). Use the free `<project>.web.app` domain, for example:
  - `/.well-known/assetlinks.json`
  - `/b/{shareId}`: a bottle page
  - `/u/{handle}`: a profile page
- **App Links behaviour:** set `autoVerify`. Android 15+ re-verifies in the background, and changes can take up to 7 days to reach devices. Test with `pm verify-app-links` ([verify](https://developer.android.com/training/app-links/verify-android-applinks)).
- **Landing page** for people without the app: static HTML with the Firebase JS SDK reading public fields, plus a Play Store button.
  - Per-message link previews (Open Graph tags) need server rendering, which means Functions on Blaze. On Spark, use one generic preview image.
  - A Play Install Referrer for deferred deep links is UNVERIFIED for this setup.
- **"Share to someone in particular"** uses a capability link: `shares/{randomId}`, where the 128-bit document ID is the secret.
  - Rules: `allow get`, deny `list`.
  - Whoever holds the link can read it and claim it.
- **Share images:**
  - Capture with `rememberGraphicsLayer()`, then `record {}` in `drawWithContent`, then `toImageBitmap()` (Compose 1.7+) ([docs](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)).
  - Write the image to the cache directory and share it through `FileProvider` with `ACTION_SEND` and the Sharesheet.

## E. Firebase free tier (Spark)

| Product | No-cost quota |
|---|---|
| Firestore | 1 GiB stored, 50K reads/day, 20K writes/day, 20K deletes/day, 10 GiB/month egress |
| Hosting | 10 GB storage, 360 MB/day transfer |
| Auth | 50K MAU; phone auth is billed |
| FCM, Crashlytics, App Check | No-cost |
| Remote Config | **New:** 100K fetches/day from 1 Sep 2026 ([pricing](https://firebase.google.com/docs/remote-config/pricing)). Not used by BottleCast yet. |
| Play Integrity (behind App Check) | 10K requests/day default ([overview](https://developer.android.com/google/play/integrity/overview)) |
| Cloud Functions, new Cloud Storage buckets | **Blaze only.** Functions include 2M invocations/month free. |

- **Going over a Spark quota** shuts that product off for the rest of the month, according to the [plans page](https://firebase.google.com/docs/projects/billing/firebase-pricing-plans). Some third-party guides say quotas reset daily; trust the official page.
- **Is Blaze effectively free?** Mostly yes:
  - Blaze keeps the same free quotas.
  - Budget alerts do not cap spending.
  - **Spend caps** pause usage at 100% of budget, but only for Functions, AI Logic, App Hosting and Extensions ([spend caps](https://firebase.google.com/docs/projects/billing/spend-caps)). **Firestore is not covered**, so a runaway read loop could still be billed.
  - Recommendation: stay on Spark until a backend is truly needed. NFT minting will probably force one, because the minting key cannot live in the app. Then move to Blaze with a Functions spend cap, a budget alert and App Check enforced.
- **Firestore TTL:**
  - One TTL field per collection group.
  - Documents are deleted within about 24 h of expiry, and TTL deletes are billed as normal deletes ([TTL](https://firebase.google.com/docs/firestore/ttl)).
  - Good for expiring share links. Availability on Spark is UNVERIFIED.

## F. Achievements

- **Play Games Services:**
  - Free, with built-in UI.
  - Rules: at least 10 achievements, at most 400. Points come in multiples of 5, at most 200 each, 2,000 in total ([concepts](https://developer.android.com/games/pgs/achievements)).
  - v2 signs players in automatically to a separate Gamer Profile. Google says it should not be your main identity ([sign-in](https://developer.android.com/games/pgs/signin)).
  - **The Play Console reportedly shows PGS only for apps declared as games** (UNVERIFIED, from a [forum report](https://b4x.com/android/forum/threads/play-games-services-available-for-a-non-game-app.135930/)).
- **Recommendation: build achievements yourself in Firestore.**
  - Store them in `users/{uid}/achievements/{id}`, with counters on the user doc (`bottledCount`, `discoveredCount`).
  - Increment counters with `increment(1)` in the same batch as the action that earns them, checked by rules.
  - Achievements then appear in the profile and on share cards, and could later become NFT badges.
  - PGS's UI cannot be drawn into your own share image.

## G. Shake, haptics and motion

**Shake detection** (community practice, not an official spec)
- Use `TYPE_ACCELEROMETER` at `SENSOR_DELAY_GAME`.
- Compute the force as magnitude divided by gravity. Count a peak above about 2.2–2.7 g, and require 2–3 peaks within about 1 s, with debounce.
- Register only while the screen is resumed (`DisposableEffect` plus a lifecycle observer).
- Handle devices with no accelerometer.
- Show a "cork loosening" progress ring that fills with each shake.

**Haptics** ([custom effects](https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects))
- **Per shake:** `PRIMITIVE_LOW_TICK` or `TICK`, scaled up as the cork loosens.
- **On the pop:** `QUICK_RISE` then `CLICK`, or a `THUD`.
- **Fallbacks:**
  - A Composition has no automatic fallback; check `arePrimitivesSupported()` first.
  - Android 16+ `VibrationEffect.Builder` adds automatic fallback.
  - `BasicEnvelopeBuilder` can produce a "wave swell" effect.
- Use `performHapticFeedback` for simple button taps.

**Accessibility**
- Always offer **"Tap to uncork"** and press-and-hold alternatives to shaking.
- Compose's built-in animations follow the system animator scale.
- **Lottie's `animateLottieCompositionAsState` jumps to the last frame when animations are off** ([commit](https://android.googlesource.com/platform/external/lottie/+/a226747d)). That matters for the existing GetMessage reveal. When the scale is 0, skip the shake and show the message directly.
- Pause looping animations, such as a bobbing bottle, in that mode.

**Compose motion**
- **`spring()`**: wave physics.
- **`Animatable`**: cork pop.
- **`AnimatedContent`**: bottle-to-message transition.
- **`SharedTransitionLayout`**: bottle-to-card transition. Whether it is stable is UNVERIFIED.

**Material 3 Expressive**
- **Versions:** `material3` 1.4.0 is stable; 1.5.0-beta01 came out on 2026-10-07 ([release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)).
- **Stabilised in the 1.5 pre-releases:** ButtonGroup, FloatingToolbar and SplitButton.
- **Still experimental:** MaterialShapes and LoadingIndicator; their stable promotion was reverted in May 2026.
- **Motion:** read it through `MaterialTheme.motionScheme`.
- **Ideas to adopt:**
  - Expressive spring motion.
  - A shape-morphing LoadingIndicator for "drifting in…".
  - MaterialShapes frames for achievement badges and avatars.
  - A ButtonGroup for Translate / Share / Report on the message card.
