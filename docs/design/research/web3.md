# BottleCast: NFTs, wallets and claiming on a zero budget (researched 2026-10-12)

> Research notes from 2026-10-12, kept for their sources. Superseded by [docs/design/spec.md](../spec.md) and [decisions.md](../decisions.md) where they disagree — notably:
> - Base mainnet is the release target (Base Sepolia for development). "Start on Base Sepolia", the "Testnet" label and "testnet collectibles" apply to debug builds only ("In short", sections C and G).
> - Claiming becomes Keep and Seal (mint on keep, wallet-last): keeping is instant, free and stored only in Firestore, and sealing mints into the finder's own linked wallet after a 24-hour safety window, with BottleCast paying the fee (sections B and G).
> - Reown AppKit only connects wallets and requests a plain `personal_sign`; the Worker verifies the SIWE message. Reown Auth, one-click auth and SIWX aren't used, because Reown's free Starter plan caps those users at 500 MAU (sections E and G).
> - The on-chain "verified account" mark is an HMAC of the uid and the messageId, not of the uid alone, so a writer's keepsakes can't be linked to each other (section D).
> - Gift recipients can keep a bottle from the `/b/{token}` web page with a wallet, without the app or a Google sign-in (section G).
>
> Prices and limits change. The $0.002 Base mainnet figure is for a plain mint; storing the text on-chain costs more, though still under a cent.

**In short:** Keep the Firebase `uid` as the account. Wallets are optional and get linked to the account by having the wallet sign a message. A claim is written to Firestore first, and the NFT is minted only once the claimer has a linked wallet. Start on **Base Sepolia**, which is free. A free **Cloudflare Worker** acts as the signer. Moving to Base mainnet later costs fractions of a cent per mint.

---

## A. Identity model

**Recommended pattern**
1. The Firebase `uid` is the primary key for messages, the once-a-day limit, achievements, FCM tokens and moderation.
2. Store wallets in `users/{uid}/wallets/{chain:address}` with these fields: address, chain, source (external or embedded), `isDefault`, `linkedAt`.
   - Add a global index `walletIndex/{chain:address} → uid` so each address belongs to only one uid.
   - If an address is already linked to another uid, refuse. Never merge accounts silently.
3. **Proving ownership:**
   - The server issues a nonce bound to the uid.
   - The wallet signs an EIP-4361 SIWE message containing the domain, chainId, nonce and expiry (https://eips.ethereum.org/EIPS/eip-4361).
   - The server verifies it, using EIP-1271 for smart-contract wallets, then writes the link.
   - Solana uses SIWS instead.
   - Vendors use the same "link after login" step: Privy `useLinkWithSiwe` (https://docs-legacy.privy.io/guide/expo/authentication/siwe) and CDP SIWE with nonce and expiry (https://docs.cdp.coinbase.com/embedded-wallets/siwe).
4. The **default/receive wallet** is where claims are minted. Changing it requires signing in again.

**Why the wallet should not be the login**
- One person has many wallets and loses or rotates them, so their identity would be split.
- New wallets cost nothing, so "one discovery per day" would be easy to cheat by wallet. It is harder to cheat by Google account.
- Push notifications, Firestore rules and reports all key off the uid.
- Most users have no wallet.

**Embedded wallets created from the Google login.** For Firebase as the token source, set the JWKS to `https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`, `iss=https://securetoken.google.com/<projectId>` and `aud=<projectId>` (https://docs.metamask.io/embedded-wallets/authentication/custom-connections/firebase.md). Firebase ID tokens expire after one hour.

| Provider | Native Kotlin SDK | Accepts Firebase JWT | Free tier | Catch |
|---|---|---|---|---|
| Privy | Yes, `io.privy:privy-core`, API 28+ (https://docs.privy.io/guide/android) | Yes, JWKS plus `sub` claim; you must "request access" (https://docs.privy.io/authentication/user-authentication/jwt-based-auth/setup) | 0–499 MAU, 50K signatures/month (https://www.privy.io/pricing) | Whether custom auth is included on the free plan is **UNVERIFIED** |
| Dynamic | Yes, `signInWithExternalJwt` (https://www.dynamic.xyz/docs/kotlin/authentication.md) | Yes | Up to 1,000 MAU (https://dynamic.xyz/pricing) | "Bring your own Auth" is listed under Enterprise, so probably not free (**UNVERIFIED**) |
| MetaMask Embedded Wallets (formerly Web3Auth) | Yes, v10.0.1, API 26+ (https://docs.metamask.io/embedded-wallets/sdk/android) | Yes, with a Firebase guide | 1,000 monthly active wallets (https://metamask.io/developer/embedded-wallets) | Custom JWT in production needs the **Growth plan at $69/month**; it is free only on devnet (https://docs.metamask.io/embedded-wallets/sdk/android/advanced/custom-authentication.md) |
| Coinbase CDP Embedded | None found (React, React Native, JS, Swift) | Yes, any RS256/ES256 JWKS; Firebase is named (https://docs.cdp.coinbase.com/embedded-wallets/custom-authentication) | 5,000 operations/month, then $0.005 each (https://docs.cdp.coinbase.com/embedded-wallets/pricing) | Android only through React Native |
| thirdweb | None found | Yes (https://portal.thirdweb.com/connect/in-app-wallet/custom-auth/firebase-auth) | 1,000 wallets (https://thirdweb.com/pricing) | Custom auth is listed under Growth at $99/month |
| Turnkey | Yes, `com.turnkey:sdk-kotlin` (https://docs.turnkey.com/sdks/kotlin/overview) | Firebase/OIDC **UNVERIFIED** | About 25 signatures/month (third-party source, **UNVERIFIED**) | Free tier is very small |
| Openfort | None found | Firebase and custom JWT (https://www.openfort.io/authentication) | 2,000 operations/month (vendor blog) | No native Android SDK |
| Crossmint | Kotlin SDK says it is "not yet updated for V1" (https://docs.crossmint.com/wallets/quickstarts/kotlin) | — | About 1,000 monthly active wallets (**UNVERIFIED**) | Custodial wallet tied to the user's email |

---

## B. Claiming without a wallet (cheapest first)

1. **Mint on link (recommended).**
   - A claim writes `claims/{messageId}` with status `pending_wallet`. Nothing goes on-chain.
   - When the user links a wallet, the server mints every pending claim to their default wallet.
   - Gas is only spent for users who actually want the NFT.
2. **EIP-712 signed mint vouchers ("lazy minting").**
   - The server signs a `MintRequest` (recipient, uri, uid, validity window), and any wallet can redeem it.
   - thirdweb's `mintWithSignature` implements this (https://portal.thirdweb.com/references/typescript/v5/erc721/generateMintSignature).
   - The person redeeming pays gas unless you relay it or use a paymaster.
3. **Mint straight into an embedded wallet** created at sign-in. This is the best UX, but you depend on a vendor's pricing (see the table above).
4. **Mint to an email address.** Crossmint's recipient format `email:<addr>:<chain>` creates a custodial wallet automatically (https://docs.crossmint.com/minting/advanced/specify-recipient). The downside is that it shares users' emails with a third party and holds the NFT in custody.
5. **Escrow (mint when the message is bottled, transfer on claim).** You pay gas twice, including for messages nobody ever finds. Avoid it.

"Claim links" from Coinbase/Base only cover funds for Base app users, and unclaimed funds return after 5 days (https://help.coinbase.com/en/wallet/trading-funding/send-receive). They don't apply to NFTs. For BottleCast, build your own link: a random single-use token, stored hashed in Firestore, feeding the same claim pipeline.

---

## C. Chain and cost

| Chain | Cost per mint |
|---|---|
| Base mainnet | About **$0.002** for an ERC-721 mint (early-2026 estimate, https://skills.sh/austintgriffith/ethskills/gas). The minimum base fee was raised in steps up to 2,000,000 wei by February 2026 (https://docs.base.org/base-chain/network-information/configuration-changelog). Storing about 280 bytes of text still stays under a cent (my estimate). |
| Polygon PoS | Under a cent by my own arithmetic (**UNVERIFIED**). Fee pressure rose in 2026. |
| Solana compressed NFTs (Bubblegum v2) | About 0.00002 SOL per NFT, after a one-time 0.34 SOL fee for a 16,384-NFT tree (https://www.metaplex.com/docs/smart-contracts/bubblegum-v2/faq). Support for v2 in wallets and marketplaces is incomplete, and it needs a DAS-capable RPC. |
| Arbitrum, Optimism, Zora, Abstract, Flow | Probably under a cent too (**UNVERIFIED**) |
| Immutable zkEVM | Free gas for players in 2026 is **UNVERIFIED** |
| Base Sepolia, Polygon Amoy (testnets) | $0 |

**Gas sponsorship (paymasters)**
- **CDP Paymaster:** Base and Base Sepolia only, smart accounts only, gas plus a 7% fee. You can apply for up to $15K in credits (https://docs.cdp.coinbase.com/paymaster/introduction/welcome).
- **Alchemy Gas Manager:** the free tier sponsors testnets without limit; mainnet costs gas plus 8% (https://alchemy.com/docs/reference/gas-manager-faqs).
- **Pimlico:** the free tier covers testnets only; mainnet costs gas plus 10% (https://docs.pimlico.io/guides/pricing).
- **thirdweb:** 1,000 sponsored transactions included, plus a 2.5% mainnet surcharge.

**Verdict:** No service sponsors mainnet gas for free, apart from credits you have to apply for. If the server does the minting, though, its own wallet pays about $0.002 per mint, so \$5 covers thousands.

**Is a testnet a sensible "free forever" option?** Yes, for a hobby app.
- The CDP faucet gives 0.1 ETH per 24 hours on Base Sepolia (https://docs.cdp.coinbase.com/faucets/docs/welcome).
- Trade-offs:
  - OpenSea no longer supports testnets (https://support.opensea.io/en/articles/11833955-farewell-testnets).
  - Wallets hide testnets by default.
  - The tokens have no value, so be honest in the UI and call them "testnet collectibles".
  - Testnets do get retired (Base Goerli was). Keep Firestore as the source of truth so you can re-mint on mainnet later.

---

## D. Metadata and storage

- **Recommended: fully on-chain.**
  - The contract stores the text (capped at about 280 bytes) and a timestamp.
  - `tokenURI()` returns a `data:application/json;base64` document with an SVG parchment image generated when it's read.
  - It never breaks and needs no pinning service. OpenSea says on-chain metadata is fine when it is compact (https://docs.opensea.io/docs/metadata-storage).
- **IPFS free tiers are small:**
  - Pinata Free allows 1 GB and **500 files**, which means 500 messages (https://pinata.cloud/blog/pinatas-new-free-plan/).
  - Filebase gives 5 GB pooled; whether it caps the number of files is **UNVERIFIED** (https://filebase.com/free/).
  - NFT.Storage Classic stopped accepting uploads on 2024-06-30 (https://docs.pinata.cloud/frameworks/migrating-from-nft-storage).
- **Firebase Storage** buckets now need a linked billing account (https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024).
- **Privacy:**
  - Never put email, Google name, photo or a raw uid on-chain.
  - Optional fields: a pen name the user chooses (opt-in), `bottledAt`, and "BottleCast #N".
  - If you need provenance, use an HMAC of the uid with a server secret. A plain hash can be guessed and linked.
  - On-chain text is permanent, and GDPR erasure can't remove it. Mint only after the report window has passed, and only with the sender's consent in the ToS.

---

## E. Android wallet SDKs

- **Reown AppKit Kotlin (formerly WalletConnect):** actively maintained. BOM 1.6.17 shipped 14 September (https://github.com/reown-com/reown-kotlin/releases), minSdk 23 (https://docs.reown.com/appkit/android/core/installation), and it supports SIWE. This is the default choice.
- **MetaMask Android SDK 0.2.1** (https://docs.metamask.io/wallet/how-to/use-sdk/mobile/android): the JS SDK was reportedly deprecated in favour of MetaMask Connect. The native SDK's status is **UNVERIFIED**, so prefer AppKit.
- **Solana Mobile Wallet Adapter `clientlib-ktx` 2.0.3** (docs updated February 2026, https://docs.solanamobile.com/get-started/kotlin/installation): maintained. Use it only if you pick Solana.
- **Coinbase Wallet Mobile SDK:** legacy, and it "does not work with Smart Wallet" (https://docs.cdp.coinbase.com/wallet-sdk/docs/mobile-sdk-overview). Its successor, the Base Account SDK, is web-first (https://docs.base.org/base-account/guides/migration-guide). I found no native Android SDK for passkey smart wallets (**UNVERIFIED**).

---

## F. Server

- **Cloud Functions for Firebase:** deploying needs the **Blaze plan with a card** (https://firebase.google.com/docs/functions/get-started).
  - The free quota is 2M invocations, 400K GB-seconds, 200K CPU-seconds and 5 GB egress per month (https://firebase.google.com/pricing).
  - Container storage adds a few cents (https://firebase.google.com/docs/functions/quotas). Set a budget alert.
- **Cloudflare Workers Free:** 100k requests per day and 10 ms of CPU per request; waiting on I/O doesn't count (https://developers.cloudflare.com/workers/platform/limits).
  - The Worker would verify the Firebase token with `jose`, sign with `viem`, and write to Firestore over REST.
  - Whether signing fits in 10 ms of CPU is **UNVERIFIED**, though likely.
- **Supabase Edge Functions:** about 500K invocations per month (third-party source).
- **Vercel Hobby:** 1M invocations, but **non-commercial use only** (https://vercel.com/docs/plans/hobby).
- **Managed signers (so you never hold a raw key):**
  - CDP Server Wallets: 5,000 operations per month free; a mint uses 2 operations, so about **2,500 free mints per month** (https://docs.cdp.coinbase.com/wallets/pricing).
  - thirdweb Engine: 1,000 requests included.
  - Crossmint: about $0.01 per action (**UNVERIFIED**).
- Keep only a few dollars in the minter key, and keep the contract owner key separate and offline.

---

## G. Recommended stack

**Primary: "wallet-optional, mint on link"**
- Login: Firebase Google sign-in, unchanged.
- Chain: Base Sepolia now, then Base mainnet with the same contract.
- Contract: OpenZeppelin ERC-721 with `MINTER_ROLE`, on-chain text and SVG, and `mint(to, messageId, text)` that is safe to retry for the same message.
- Server: a Cloudflare Worker with `/wallet/nonce`, `/wallet/link`, `/claim` and `/share`.
- Wallet connection: Reown AppKit.

**Fallback: "everyone gets a wallet"**
- Privy's Android SDK with Firebase JWT auth creates a wallet silently at sign-in, and claims mint straight into it.
- First confirm with Privy whether custom auth works on the free tier (499 MAU).
- If it doesn't, use MetaMask Embedded Wallets: free on devnet, $69/month in production.
- If you'd rather use Solana: Bubblegum v2 with the Mobile Wallet Adapter.

**Claim flow**
1. A sent message goes to `messages/{id}` with status `afloat`. Nothing goes on-chain.
2. Discovery is limited to one per uid per day, enforced on the server.
3. "Claim" calls `/claim` with the ID token. A Firestore transaction marks the message claimed and creates `claims/{id}`.
4. If the user has a default wallet, mint and save the `txHash`. If not, set the status to "Awaiting wallet".
5. Linking a wallet means signing in with SIWE, after which every pending claim is minted.
6. Sharing works through a single-use token in an App Link. The recipient signs in and goes through steps 3–5.

**Screens needed**
- Claim button on the revealed message.
- Claim result: minted, or pending a wallet.
- Wallets list, with a default star and unlink.
- Connect-wallet sheet with a "what's a wallet?" explainer.
- Explainer before the SIWE signature.
- My Collection, with status chips.
- On-chain badge with an explorer link.
- Share-a-bottle sheet (link and QR code) and the screen that opens from a shared link.
- Profile card with stats, and Achievements.
- "Come back tomorrow" state.
- "What goes on-chain" privacy note, and a "Testnet" label.

**What's free, what's not**

| Item | Free | Could cost money |
|---|---|---|
| Firebase Auth, Firestore, FCM | Within the Spark quotas | Usage above the quotas |
| Cloudflare Worker | 100k requests/day | Paid plan if you outgrow it |
| Cloud Functions | Free quota, but needs a card (Blaze) | Container storage; overages |
| Base Sepolia gas | Yes, from the faucet | — |
| Base mainnet gas | No | About $0.002 per mint |
| Paymasters | On testnets | Gas plus 7–10% on mainnet |
| CDP Server Wallet | 5,000 operations/month | $0.005 per operation |
| On-chain metadata | Only gas | — |
| Embedded wallets | Privy ≤499 MAU (custom JWT **UNVERIFIED**); MetaMask devnet only | $69–\$299+/month |

---

## Google Play and regulatory issues

- **Declaration:** apps that let users *earn* tokenized digital assets must declare it in the Financial features declaration (https://support.google.com/googleplay/android-developer/answer/13607354). My reading is that free claimable NFTs count as earning, so declare them. The policy doesn't mention free giveaways explicitly.
- **No earnings talk:** never promote or glamorize earnings.
- **No paid chances:** never take money for a chance at an NFT. Paid extra "shakes" would look like loot boxes (https://android-developers.googleblog.com/2023/07/new-blockchain-based-content-opportunities-google-play.html). Whether rewarded ads count is **UNVERIFIED**. Label any in-app product that represents an NFT.
- **Custody:** custodial wallets fall under the software-wallet licensing rules (https://support.google.com/googleplay/android-developer/answer/16329703). Holding users' NFTs in custody pushes you toward that category, which is another reason to mint on link.
- **Free giveaways:** giving NFTs away for free looks fine under the policy, and testnet NFTs with no value carry the least risk. This is not legal advice.
