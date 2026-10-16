# BottleCast Worker

The Cloudflare Worker that slice 6 builds (`/claim`, `/wallet/nonce`, `/wallet/link` and minting) will live here. For now it holds **slice 5's spike**, which asked whether a Worker on the Free plan can sign a mint and check a wallet's signature within its 10 ms of CPU per invocation. The answers, and what slice 6 does about them, are in [`docs/design/decisions.md`](../docs/design/decisions.md) (2026-10-16, slice 5 spikes).

## What's here
- `src/ops.ts`: the CPU-heavy steps slice 6 will run.
  - The mint's calldata (280 bytes of text) and its EIP-1559 signature, offline.
  - The verified mark (HMAC-SHA256).
  - An RS256 ID-token check with `jose`.
  - A SIWE message checked and its `personal_sign` signature recovered.
  - The service-account JWT for Firestore REST.
- `src/spike.ts`: one bench route per step and per invocation (`/bench/mint`, `/bench/link` and the rest), plus `/bench/rpc` (Base Sepolia reads through viem, counting subrequests) and `/bench/rpc-lean` (the same reads as one JSON-RPC batch, one subrequest) and a `scheduled` handler that runs a cron mint. Every route needs the `X-Spike-Key` header.
- `src/spike-warm.ts`: the same Worker, plus a warm-up at module scope: one secp256k1 signature and recovery, and one offline mint signature, so they happen while the isolate starts.
- `bench/`:
  - `keys.ts` makes throwaway keys, all gitignored and never funded.
  - `local.ts` runs a first read in Node.
  - `run.ts` drives a deployed Worker.
  - `summarise.ts` turns `wrangler tail --format json` into a table.

## Running it
A Worker can't time itself: its clock stands still while it computes. The CPU numbers come from the `cpuTime` that `wrangler tail` reports for each invocation, in whole milliseconds. Tail samples busy Workers, so it doesn't see every request.

```bash
npm ci
npm run check                     # wrangler types, then both tsconfigs
node bench/keys.ts                # .dev.vars, .secrets.json, .bench-keys.json
node bench/local.ts 500           # a first read on this machine
npx wrangler dev                  # then: node bench/run.ts warm 3
```

Deploying needs the owner's OK. It uses the owner's Cloudflare account, which wrangler picks since it's logged in to only that one.

```bash
npx wrangler deploy --secrets-file .secrets.json
npx wrangler tail bottlecast-spike --format json > tail.jsonl &
SPIKE_URL=https://bottlecast-spike.<subdomain>.workers.dev SPIKE_GAP_MS=250 node bench/run.ts warm 100
node bench/summarise.ts tail.jsonl
```

- **Cold starts:** redeploy with `--var RUN:cold-<n>`, which makes a new version on fresh isolates, then `node bench/run.ts cold <n> mint` (or `link`).
- **The warm-up variant:** `npx wrangler deploy src/spike-warm.ts --name bottlecast-spike-warm --secrets-file .secrets.json`.
- **Clean-up:** delete the spike Workers afterwards with `npx wrangler delete bottlecast-spike` and `npx wrangler delete bottlecast-spike-warm`. A redeploy without `triggers` leaves an earlier cron running, but deleting the Worker removes it.

## Results (2026-10-16)
All CPU figures are Cloudflare's `cpuTime` per invocation, in whole milliseconds, on the Free plan.
- **Cold** is the first request after a redeploy (a new version on fresh isolates), ten redeploys each. Odd runs sent `mint` first and even runs sent `link` first, so each column mixes "first on the isolate" and "second".
- **Warm** is from spaced runs of 100 requests per route, of which tail kept about 30.

| Invocation | Warm p50 / max | Cold, lazy (`spike.ts`) | Cold, warmed up (`spike-warm.ts`) |
|---|---|---|---|
| `/bench/mint` | 2 / 4 | 41, 8, 35, 8, 27, 7, 40, 10, 25, 8 | 8, 5, 9, 3, 4, 4, 10, 3, 5, 4 |
| `/bench/link` | 2 / 9 | 22, 19, 21, 19, 24, 21, 36, 23, 38, 26 | 6, 5, 5, 5, 5, 7, 5, 4, 6, 5 |
| Cron mint (`scheduled`) | — | 29, 62, 59, 35, 68, 37, 24, 26 | 6, 8, 16, 13, 15, 13, 4, 12 |
| `/bench/rpc` (viem) | 5 / 15 | | |
| `/bench/rpc-lean` (one batch) | 1 / 1 | | |
| `/bench/sign` | 2 / 5 | | |
| `/bench/siwe` | 2 / 3 | | |
| `/bench/idtoken` | 0 / 3 | | |
| `/bench/satoken` | 1 / 2 | | |
| `/bench/hmac`, `/bench/noop` | 0 / 0 | | |

- **Startup:** 14–20 ms lazy, and 37–60 ms with the warm-up (the limit is 1 s). The bundle is 707 KiB, or 149 KiB gzipped.
- **The first warm-up** (one signature and recovery, without the offline mint) gave cold mints of 13, 8, 7, 9, 10, 5, 11, 6, 15 and 7, and cold links of 6, 9, 3, 10, 3, 4, 7, 5, 6 and 9. The full warm-up's offline mint compiles viem's ABI encoding and serialising too.
- **No failures that tail saw:** every request returned 200, apart from the refused RPC batches below, with no 1102. Every cron run tail caught (8 of about 13 per Worker) ended `ok`. `cpuTime` is in whole milliseconds, so a sample of 10 may be just over the limit.
- **Subrequests:** `/bench/rpc` uses 3, and `/bench/rpc-lean` uses 1. Base Sepolia's public RPC answered 3 of 15 batches with something other than an array, so batches need a fallback.
- **Locally,** on an M1 Pro with Node 24: the first secp256k1 signature in a process costs about 28 ms of CPU, and the warm operations cost 0.5–1.5 ms.

What this means for slice 6 is in `docs/design/decisions.md` (2026-10-16).
