# Open issues

Questions for the owner and review findings that are still open, each tagged with the slice it affects (the slices are listed in [README.md](README.md)). They come from the 2026-10-12 review of the spec and from reconciling the spec with the owner's decisions. Review findings that spec v1.1 already fixed aren't listed.

When an item is settled, record the answer in [decisions.md](decisions.md), fix `spec.md`, and move the item to "Resolved" with its answer, all in the same PR.

## Questions for the owner

- [ ] **What the Chest and Me tabs show before they're built** (Slice 1)
  - Slice 1 adds the Shore/Chest/Me navigation bar, but the Chest arrives in slice 4, and the streak pill and stats need data from slices 2 and 4.
  - Options: hide them until their slice lands, or show placeholder states.

- [ ] **Material3 1.5 beta or the spec's fallbacks** (Slice 1)
  - Expressive motion, the wavy progress indicators and MaterialShapes need Material3 1.5. Compose BOM 2026.09.00 resolves to 1.4.0, and 1.5 is only in beta (1.5.0-beta01).
  - Options: pin material3 1.5.0-beta01, or stay on 1.4.0 with the fallbacks (hand-written springs, the standard CircularProgressIndicator, CircleShape).
  - Proposed: the fallbacks, until 1.5 is stable.

- [ ] **How much of the Worker comes forward to slice 2** (Slice 2)
  - Blocking, the report threshold and a server-pinned daily bottle all need discovery to go through a Worker (`/discover`, `/report`, `/block`), which is planned for slice 6.
  - Options: a minimal Worker in slice 2, or client-only reporting until slice 6.
  - Also open: how many reports take a bottle out of the sea. Proposed: 3.

- [ ] **Keeping it to one bottle a day** (Slice 2)
  - Calendar-day tides fix the cooldown maths, but the daily rule still has to hold when someone peeks at a prefetched bottle and backs out, reinstalls as a guest, or changes the phone's time zone.
  - Options: a server-pinned bottle per user per day; a time zone pinned to the account; accepting the guest-reinstall bypass, or requiring sign-in to keep a streak.

- [ ] **Bottles that drift again** (Slice 2)
  - Thrown-back and expired bottles drift back into the sea, and a reported one stays there for everyone but the reporter, so a bottle can be found more than once. The data model has a single `foundBy`/`foundAt`, and "Your bottle was found" and Cast success's journey row assume one finder.
  - Options (from the review): a find count, with the writer told once.

- [ ] **Board or spec where they differ** (Slices 2, 3, 6 and 7)
  - Spec §6 lets the spec win for Reveal's button order (slice 2), the Compose and Preview wording and the three onboarding pages (slice 3), the three-step wallet sheet (slice 6) and Gift-Compose's own pen-name switch (slice 7). The owner may prefer a board in some of these.
  - Proposed: the spec, unless the owner picks the board.

- [ ] **Merging a guest into an existing Google account** (Slice 3)
  - The sign-in chip promises "Your 3-day streak and today's find come with you". If that Google account already exists, `linkWithCredential` fails, and moving the streak and the find between two uids needs a server-side merge.
  - Options: a server-side merge with a rule (for example, keep the longer streak), or softer copy on the chip.

- [ ] **Onboarding page 3 mentions sealing** (Slice 3)
  - Onboarding ships in slice 3, but sealing only arrives in slice 6.
  - Options: keep the line, or hold it back until slice 6.

- [ ] **Quiet hours and "found" notifications** (Slice 4)
  - Spec §3.16b holds every notification, the daily reminder included, until quiet hours end. The Notify-Rationale and Settings boards promise "found" notifications, which the spec defers until push exists.
  - Proposed: confirm both as the spec has them; until push exists, Settings marks the "found" row "Coming soon" (spec §3.17 and §6).

- [ ] **Shields for the first cast each day** (Slice 4)
  - A calm-sea shield for each day's first cast (10-character minimum) may invite filler casts that moderation then has to absorb.
  - Options: keep the rule, or change what gives a shield.

- [ ] **What deleting an account does to other people's chests** (Slice 4)
  - The delete dialog (spec §3.17) covers the user's own profile, chest, streak and drifting bottles. Undecided: a deleted writer's unsealed bottles kept in other people's chests, and seals still pending.
  - Options: remove the text from those chests or keep it there anonymously; cancel pending seals or let them finish.

- [ ] **Which name the Me tab shows** (Slice 4)
  - Spec §3.12 heads the Me tab with "Marc T.", which §3.17 shows as the Google account name, while the sample pen name is "Driftwood". The Profile board draws "Marc T." with a "Pen name: Driftwood" chip and "MT" initials. But §3.12 also says the avatar falls back to the pen-name initial and that a user with no pen name is an "Anonymous sailor", which only fits a pen-name heading.
  - Either way, public pages and the share card show the pen name (spec §3.13 and §6), so the "MT" initials on the Share-Profile board's card become the pen-name initial.
  - Options: the pen name as the heading and initial, or the Google name with a pen-name chip, as the board draws it.

- [ ] **When the 24-hour safety check starts** (Slice 6)
  - The spec and the Claim-Waiting board count from the cast. The Keepsake-Detail board assumed the find, though its dates fit either reading.
  - Options: from the cast, or from the find. Proposed: from the cast (spec §1.7).

- [ ] **Before Base mainnet** (Slice 6)
  - [ ] Who funds the minter. While it's empty, seals wait with "Sealing · delayed" (spec §3.8b).
  - [ ] File Play's Financial features declaration before a release with mainnet keepsakes.
  - [ ] Check which Auth tier `bottlecast-prod` is on: Identity Platform means a cap of 3,000 daily active users. This is a console check for the owner.

- [ ] **Moderating gifts before they're sealed** (Slice 7)
  - A gift is read only by whoever holds its link, so reports can't catch abuse: a sender could open their own link with a second account or a web wallet and seal any text.
  - Options: a server-side text check, or an admin queue, before any gift is sealed.

- [ ] **Which achievement list** (Slice 8)
  - The spec and the Profile board use one 15-badge list; the Achievements and Achievement-Unlock boards use another ("Sealed with care" against "Sealed with wax").
  - "Night owl" (a find between midnight and 4 AM) now rewards staying up for the midnight tide, against quiet hours. The board list already drops it.
  - Proposed: spec §3.14's list, which wins until the owner picks (spec §6). Night owl: keep or drop.

## Open review findings

- [ ] **Accessibility timeouts left at fixed times** (Slices 2 and 8)
  - Spec §4 says timed controls stay up for at least `AccessibilityManager.getRecommendedTimeoutMillis()`, and that snackbars with an action don't time out while TalkBack is on. But §3.7 still gives the "Reported" [Undo] bar 5 s, and §3.14 the achievement toast 4 s.
  - Proposed: read 5 s and 4 s as minimums and follow §4.

- [ ] **Old app versions after the data model changes** (Slice 2)
  - Installs still on 1.1.1 keep reading and writing messages in the old shape after slice 2 adds fields and `firestore.rules`. No forced-update or migration state is designed.
  - Options: a forced-update screen, or rules and mappers that accept both shapes for a while.

- [ ] **Guest accounts removed by automatic clean-up** (Slice 2)
  - If Identity Platform's automatic clean-up of anonymous accounts (after 30 days) is turned on, a guest loses their streak and finds without warning.
  - Options: leave clean-up off, or design a state for a guest whose account was removed.

- [ ] **Push for "found", "kept" and gift events** (Slices 4 and 7)
  - These need a push sender (the Worker calling FCM HTTP v1 with a service-account token). No slice builds it, and the spec doesn't describe it.
  - Proposed: the spec's v1 fallback, a Chest badge dot and the Shore's "While you were away" banner (§3.16b), until a slice adds push.

- [ ] **Worker within the free limits** (Slices 5 and 6)
  - The spec mints one claim per invocation and shows "Sealing · delayed" when it can't. Still open: whether signing fits in 10 ms of CPU (a slice 5 spike), staying under 50 subrequests per invocation (cron runs included), and serialising the single minter's nonce.
  - Options for the nonce: cron-only minting, or a Durable Object that owns it. Someone also has to watch the minter's balance (the Base Sepolia faucet gives 0.1 ETH per rolling 24 hours).

- [ ] **"Create one with Google"** (Slices 5 and 6)
  - The embedded-wallet option on Add a wallet stays "Coming soon" behind a remote flag until a free embedded-wallet tier is confirmed, so linking an existing wallet is the only path for now.
  - It hangs on the slice 5 spike: does Privy's free plan accept a Firebase custom JWT?

- [ ] **Testnet keepsakes in a wallet (debug builds)** (Slice 6)
  - Wallets don't auto-detect testnet NFTs. Spec §3.8b gives debug builds copy buttons for the contract address and keepsake ID, but there's no "Add to my wallet" sheet with MetaMask's manual "Import NFT" steps, and no board for one.
  - Options: the copy buttons alone (what §3.8b has now), or a sheet with the steps.

- [ ] **Public profile data for `/u/{handle}`** (Slice 7)
  - The page needs a publicly readable copy of the profile (for example `publicProfiles/{handle}`) with its own security rules, holding only what the "Show my…" switches allow. The read path is settled: one Firestore REST `get` (spec §3.15c).

- [ ] **Gift link to a removed or reported bottle** (Slice 7)
  - Spec §3.15b and §3.15c cover kept, invalid, expired and called-back links, but not a bottle that was reported or taken out of the sea.

- [ ] **A way back to an unopened gift** (Slice 7)
  - A gift opens from its App Link (spec §3.15b). If the recipient closes it or taps "Not now", nothing on the Shore leads back to it.

- [ ] **Quota states still missing** (Slices 7 and 8)
  - "Harbour closed" now covers the Firestore, Auth and Worker daily limits, and a low minter shows "Sealing · delayed". Not yet designed: Firebase Hosting's 360 MB a day running out (the gift and profile pages and `assetlinks.json` go down) and a Shore variant of Harbour closed.

## Resolved

- [x] **What the verified mark hashes** (Slice 6)
  - The plan's "HMAC of the uid" would give every keepsake from one writer the same value, so anyone could group them on-chain, and one bottle signed with a pen name would name the rest.
  - Answer (2026-10-12, setup session): HMAC-SHA256, keyed with a Worker secret, over the writer's uid and the messageId. See [decisions.md](decisions.md).
