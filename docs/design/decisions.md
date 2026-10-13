# Decisions

This file overrides `spec.md`. Whenever the owner decides something, add it here, dated, in the same PR that applies it.

## 2026-10-12 — Mockup session

Owner's raw answers (2026-10-12, mockup session): visual direction "Moonlit Shore (Recommended)"; NFT model "Mint on keep (Recommended)"; chain: "I was thinking something like BASE or POL"; writer identity on-chain "Verified-human badge".

### Decisions (confirmed with the owner)

| Topic | Decision | Changes spec |
|---|---|---|
| Visual direction | **Moonlit Shore.** It keeps Roboto Slab, sage pills, the cyan glass bottle and the arc, which becomes a "dome" with a moon. The message sits on a parchment card. A wax seal is the only visual that means "on-chain". Night (hero) and Dawn (light) themes. | §0 (the spec was written for this direction) |
| NFT model | **Mint on keep, wallet-last.** "Keep" is instant, free and stored in Firestore. The NFT is minted into the finder's *own* linked wallet after a 24h safety window, and the app pays the gas. No custody. | §0, §5 (the spec was already wallet-last) |
| Chain | **Base.** The UI labels the network "Base"; Polygon PoS is a drop-in swap, since it is the same EVM contract and only the label changes. Develop on Base Sepolia (shown as a "Test network" chip variant on one board). Mainnet costs under 1¢ per mint, so a small pre-funded minter covers it, and users never pay. | §0, §1.7, §2 (flow 4), §3.8b, §3.9a, §3.9b, §3.11, §3.16b, §5 |
| Writer identity on-chain | **Verified-human badge.** The record carries "Bottled by a verified account ✓", stored as a keyed hash (HMAC of the uid), never a name or email. An optional pen name is added only if the writer opts in, and that choice is snapshotted at cast time. Bottles are anonymous by default ("— A stranger at sea"). | §0, §1.6, §1.7, §3.4, §3.4b, §3.7, §3.8a, §3.8b, §3.11, §3.17 |
| Account vs wallets | The Google/Firebase account is the passport. Wallets are optional display cases linked to it (many to one) with a free "Prove it's yours" signature (SIWE via plain `personal_sign`, verified server-side). One wallet is marked Receiving. A wallet never signs anyone in. | §0, §1.7, §3.9a |

The setup session below refined what the verified mark hashes.

### Changes to the spec from the review

Spec v1.1 merged these. The italic line under each item names the spec sections it changed.

1. **Calendar-day tides.** A new bottle arrives at local midnight, and the reminder fires at a user-set time (default 9:00, with quiet hours). The 20h cooldown is dropped.

   *Changes spec: §0, §2 (flow 7), §3.1, §3.3, §3.16a, §3.16b, §3.17.*

2. **Reporting.**
   - A report hides the bottle for the reporter at once. It leaves the sea only after several reports or a review.
   - There is no replacement-find reward (graft G4 is dropped).
   - Add **Block this writer** to the report dialog and to gift links.
   - Terms acceptance becomes an explicit step before the first cast.

   *Changes spec: §0 (grafts G4 and G15), §2 (flow 2), §3.4b, §3.11, §3.15b, §3.17, §3.18, §3.19, §5.*

3. **Gift recipients can claim on the web.** The `/b/{token}` page gets "Connect a wallet to keep it" (WalletConnect + SIWE), so recipients without the app can claim. Notification copy becomes "Your bottle for Mum was opened".

   *Changes spec: §0, §2 (flow 6), §3.15c, §3.16b, §5.*

4. **Writers can opt a bottle out of sealing** on Preview ("Let the finder keep this as a keepsake", on by default). (Implemented with the label "Let the finder seal this as a keepsake", because keeping is always allowed.)

   *Changes spec: §0 (graft G15), §1.7, §3.4b, §3.5, §3.8a, §3.15c.*

5. **Copy for a real chain.** No "Test network" chip except on the dev-variant board. Only claim "kept forever" on mainnet. Show "BottleCast covers the tiny network fee". Never use "earn", "value" or "rare".

   *Changes spec: §0 (graft G2), §1.2, §1.7, §1.8, §3.3, §3.4b, §3.8a, §3.8b, §3.9b, §3.11, §3.12, §3.13, §3.14.*

6. **Contrast fixes.**
   - Near the character limit, the counter shows text ("20 left") as well as a colour change, in ink tokens.
   - Chips on parchment use ink tokens.
   - The editor border uses `outline`.
   - The Translate chip stroke is solid.

   *Changes spec: §1.2 (contrast table, chips on parchment), §3.1 (page dots), §3.3 (today's-find chip), §3.4 (editor border, counter), §3.7 (Translate chip).*

## 2026-10-12 — Setup session

- **Verified mark input:** the on-chain "Bottled by a verified account" mark is HMAC-SHA256 (keyed with a Worker secret) over the writer's uid **and the messageId**, so a writer's keepsakes can't be linked to each other on-chain. (Refines "HMAC of the uid" above; resolves review item "what the verified mark hashes".)

  *Changes spec: §0 (the owner decisions) and §1.7 (the "Bottled by a verified account ✓" row).*

- **Docs are public:** the design docs, board snapshot and research are published in this public repo, after redactions (no local paths, no debug-project ID, no live-gap details).

- **Sample user** in the boards and spec stays "Marc T." / "@marct" (the owner's public identity).

- **Workflow:** the redesign is built in slices (see README), one branch and PR into `develop` per slice, verified with Roborazzi screenshot tests against the boards.

  *Replaces spec §5 and `wallets.md` §9 with the slices in `README.md`.*

## 2026-10-13 — Slice 1 planning

The owner's answers while planning slice 1 (theme, navigation and Shore). The theme PR applies these; the navigation and Shore PR adds the rest of the slice's decisions below.

- **Material3:** pin `material3` 1.5.0-beta01 ahead of the Compose BOM (which resolves 1.4.0), rather than the hand-written fallbacks. It needs Compose 1.12.0, compileSdk 37 and AGP 9.1 or later; the BOM already provides Compose 1.12.1. Drop the pin when the BOM ships 1.5. Resolves the open issue "Material3 1.5 beta or the spec's fallbacks".

  *Changes spec: the header, §1.4 (badges), §1.5 (motion, the cork ring).*

- **Icons:** Material Symbols Rounded (weight 400, fill 0, 24dp), bundled as `ic_symbol_*` vector drawables from google/material-design-icons, including the filled flag for a reported message. The Shore's house-with-waves and the bottle have no Symbol, so they're custom `ic_glyph_*` vectors drawn from the TEMPLATE.md paths.

  *Changes spec: §1.6 (icons).*

- **Fonts:** all three families are bundled; nothing downloads. Roboto Flex is the official variable font, with weights selected through its `wght` axis and `opsz` set per style; Roboto Mono is Regular only; Roboto Slab keeps its existing files, minus the weights the scale never uses. Sources and licences are in `licenses/`.

  *Changes spec: §1.3 (fonts).*

- **Look details:**
  - The Dawn moon is the boards' pale sun-moon: a #F6E8CC disc (Dawn moonContainer) with a soft moon glow at 12%, not "moon at 30%".
  - The outlined pill's stroke is 1dp `primary`, as the spec says; the boards' 1.5px is superseded.
  - Avatars are plain circles, as the boards draw them; badges keep the MaterialShapes shapes.

  *Changes spec: §1.4 (avatars), §1.6 (the Dawn moon).*

- **Delivery:** slice 1 ships as two PRs. The first applies the theme to today's screens and is signed off from its screenshots; the second brings edge-to-edge, the splash, RTL, type-safe routes, the navigation bar, Chest, Me and the Shore.

## 2026-10-13 — Slice 1 part 2

The owner's answers while planning the navigation and Shore PR, which applies them.

- **Order:** finish slice 1 before slice 2.
- **Chest and Me before slice 4:** honest empty states.
  - The Chest shows its compact dome header and the spec's own empty state: "Your chest is empty" / "Today's bottle is waiting on the shore." [Go to the shore] (spec §3.10).
  - Me shows the avatar in its compact dome, then "Your profile is on its way" / "Your streak, badges and pen name will live here soon." [Go to the shore].
  - Slice 4 replaces both. Resolves the open issue "What the Chest and Me tabs show before they're built".
- **Streak pill and stats before slices 2 and 4:** the live Shore hides them until real data exists. The screen can draw both, and the screenshot tests use the boards' sample data so they compare with the boards.
- **Splash:** the dome colour with the bottle glyph in `glass`. No board draws a splash.

  *Changes spec: §1.1 (the splash glyph).*

Choices made while building it, which the owner can revisit at review:

- **The Me avatar** is a person glyph on moonContainer until slice 4 settles which name its initial comes from (open issue "Which name the Me tab shows").
- **Analytics screen names** keep their old values, so the Shore still reports `home`; the new tabs report `chest` and `me`.
- **Where the Shore's boards and the spec differ,** the spec wins:

  | Spec value, taken | Board value, not taken |
  |---|---|
  | Stats 24dp apart in one 48dp target | Stats 40 apart, 56 tall |
  | Glints drifting | Glints static |
  | Only the moon's halo pulses | The whole disc fades |
  | Moonpath at 25%, fading at both ends | Moonpath at 28%, no fade |
  | Nav labels in `labelLarge` 14/20 | Nav labels 12/16 |
  | "Not kept · 7d left" on Shore B | "5d left" |
  | The bottle Lottie on Shore A | The SVG rig, which slice 2 brings |

- **From the boards, where the spec is silent:**
  - the stars
  - the dome glow's centres
  - "NEXT TIDE IN" in onSurfaceVariant
  - the tide ring's end marker
  - the today's-find card's padding and "TODAY'S FIND" overline
  - the 8dp moon badge dot

  The admin's "Moderation" menu, which no board draws, sits at the end of the top row.
- **Large text:** from 175% text the bottle and the glints step aside, and the streak pill wraps under the wordmark (spec §3.3 hides the bottle at 200%).
- **Landscape:** the dome's radius widens to three quarters of the screen's width, so the arc still spans it.
- **No failure screen on the Shore:** if its inputs fail, it logs and still offers today's bottle.
- **Predictive back:** full-screen routes shrink to 90% with 32dp corners over the screen underneath (spec §1.5), and tabs fade back to the Shore.

## 2026-10-13 — Slice 2 planning

The owner's answers while planning slice 2 (Discover, Reveal and the data model), which its PR applies.

- **Rules only, no Worker.** Firestore rules enforce the create-only daily record, one report per finder and a threshold of 3 reports. Blocking happens on the phone, by the writer id each bottle carries. Slice 6's Worker takes over discovery and blocking. Accepted until then: a modified app can read bottles without using up its day, or group one writer's bottles. Resolves the open issue "How much of the Worker comes forward to slice 2".
- **One finder at a time.** A find holds the bottle for 7 days. Throwing it back, or 7 days without a Keep, returns it to the sea. `findCount` counts the finds, nobody finds the same bottle twice, and the writer is told once (later slices). Resolves "Bottles that drift again".
- **Small daily-limit holes accepted.** The candidate is pinned in memory for the account and the day. The rules accept a date that's real somewhere on Earth at that moment (UTC−12 to +14). A reinstall makes a fresh guest. Resolves "Keeping it to one bottle a day".
- **Keep waits for slice 4.** Reveal ships with Share and Throw back only. Not in slice 2: the Keep pill and its helper, the "It'll wait in your chest" snackbar, and Shore B's "Not kept" chip.
- **Old versions: strict.** The new rules and the production migration ship together at release. 1.1.1 can't find or cast until it updates. Resolves "Old app versions after the data model changes".
- **The spec wins over the boards** where they differ, and the boards fill in where it's silent, as in slice 1. Resolves "Board or spec where they differ" for slice 2.
- **Dialog scrim:** `BottlingDialog` turns the platform's dim off and draws the theme's `scrim`, so Dawn gets its #1A2320 at 40%. Resolves "The Dawn dialog scrim".
- **One PR** for the whole slice.
- **Icons:** the share, help and refresh Symbols come from the same pinned commit as the rest.

  *Changes spec: §1.7 (the data model), §3.6 and §3.7 (no Keep yet), §3.19 (blocking and the threshold).*

Choices made while building it, which the owner can revisit at review:

- **Casting stays as it is until slice 3.** The rules let any signed-in user cast, guests included, and only a Google account skips today's sign-in sheet. Slice 3 adds a Google-only rule.
- **Signing in with Google links a guest** (`linkWithCredential`), which keeps the uid and today's find. If that Google account already exists, it signs in instead and the guest's day is lost (slice 3's merge rule).
- **Admin is a custom claim** (`admin: true`), set per project with `scripts/set_admin_claim.py`. The hard-coded admin uid is gone.
- **Bottles cast before slice 3** get `lang` "und", `penName` null and `sealable` false, since their writers never saw the choice.
- **The threshold lives in the queries and the rules** (`reportCount` in [0, 1, 2], and below 3), so no client writes a moderation status. `under_review` means an admin pulled the bottle.
- **Reports:** only the live holder can report, and the report keeps the hold, so the bottle stays out of the sea for the rest of its 7 days. Undo works inside the window, before anything is written. Blocking is a write of its own. The finder's hidden list is their find marked `reported`.
- **Timed actions:** undo windows last 5 s, or the system's recommended timeout if longer, and wait for the user while TalkBack is on (spec §4). Throw backs and reports commit from an app-scoped coordinator, and leaving Reveal commits a report still waiting. If the app dies first, the action is lost and the bottle drifts back when its hold ends.
- **The Shore's bottle is the rig,** as spec §3.3 plans for phase 2. The bottle Lottie stays only on cast success until slice 3, and Discover's Lottie is gone.
- **The rig** is drawn on a Canvas from TEMPLATE.md's paths. Its seed is SHA-256 of the bottle id: byte 0 mod 3 picks the silhouette, and (byte 1 / 255) · 12 − 6 turns the tint's hue that many degrees. The contract and the web pages port the same formula. It leans 12° with its neck towards the start edge, mirrored in RTL.
- **Copy with no spec source:**
  - the share text, `“{message}” — found in a bottle on BottleCast` and the Play link, under "Share this bottle"
  - bylines that say "cast today", "cast yesterday" or "cast n days, months or years ago"
  - Discover's calm-sea note, "This didn't use up today's bottle." (slice 4 adds ", and your streak is safe")
  - "How to uncork" for the help button's label
  - the reported bar and the thanks after it
- **Anonymous auto clean-up stays off,** checked at release. Resolves "Guest accounts removed by automatic clean-up".
- **Where the boards and the spec differ,** the spec wins:

  | Spec value, taken | Board value, not taken |
  |---|---|
  | "Shake to uncork" / "Give your phone a few gentle shakes." | "Give it a shake" / "Shake your phone to pop the cork — or press and hold the bottle." |
  | An outlined "Tap to uncork" and "or press and hold the bottle", with help behind the "?" button | A filled "Tap to uncork", a "No shaking needed?" button and an optional-shaking hint |
  | Discover's text ends 32dp above the arc and its pill sits 40dp below, as the spec's y≈470 and 600 put them | Text from y 436, the pill at 622 |
  | Tonal Share and "Throw back" pills 20dp under the card | A pinned Keep pill, an outlined Share and a "Throw it back" text button |
  | The card at least 220dp tall, 8dp under its overline | 236dp, 12dp under it |
  | The spec's five report reasons, in its order | "Hateful or harassing", "Spam or a scam" and the board's order |

- **From the boards, where the spec is silent:**
  - Discover's stars, glints and "n of 3" counter
  - Reveal's moon, stars and drifting empty bottle
  - a short message's byline sitting at the foot of its card
  - the report dialog's "Reason (optional)" heading
  - the calm Shore's second pill, "Open your chest"
- **Layout:**
  - Discover's moon hangs just below the top bar, since the help button sits where the board puts it.
  - The bottle stays put while the counter comes and goes and "Unrolling…" takes over.
  - A message runs in its own script's direction, whichever way the app is laid out: Reveal's card and the Shore's excerpt.
  - At large text, Reveal's card grows with the page, its pills stack, and the drifting bottle steps aside.
  - Long edge-state titles break into even lines, and the countdown keeps "7h 12m" on one line.
  - The calm Shore hides the sky's moon, since its illustration brings one.

