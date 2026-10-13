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

