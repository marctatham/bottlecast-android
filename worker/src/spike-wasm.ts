// The spike with secp256k1 as WebAssembly (src/secp256k1-wasm.ts) in place of
// noble: the mint, link, sign and SIWE routes and the cron mint use it, and
// every other route is the plain spike's. Deployed as its own Worker,
// bottlecast-spike-wasm (README.md).
//
// The module is compiled and instantiated while the isolate starts, so that
// cost lands in startup (limit 1 s), not in a request. Then, as in
// src/spike-warm.ts, one signature and one recovery, and one offline mint, run
// at module scope. Deployed, a first call cost far more than in Node, probably
// because V8 compiles each WebAssembly function on its first call; the warm-up
// moves that compiling into startup too.
//
// The warm-up has to seed libsecp256k1's blinding first, and Workers allows no
// random numbers at module scope, so the seed comes from a secret: a hash of
// the spike's HMAC key, kept apart from the mark by a label. Slice 6 would give
// it its own secret. `--var SPIKE_SEED:lazy` reproduces the first try instead:
// no seed, so the signer's first call (and its seeding and compiling) happens
// in the first request, and the warm-up runs only the mint's JavaScript, with a
// stand-in signer.
import { env } from "cloudflare:workers";
import secp256k1Module from "../node_modules/tiny-secp256k1/lib/secp256k1.wasm";
import { keccak_256 } from "@noble/hashes/sha3";
import { hexToBytes, type Hex } from "viem";
import {
  cronMintWasm,
  linkInvocationWasm,
  mintInvocationWasm,
  PLACEHOLDER_ADDRESS,
  signMintWasm,
  verifySiweWasm,
  type SpikeSecrets,
} from "./ops.ts";
import { instantiateSecp256k1, type Secp256k1Wasm } from "./secp256k1-wasm.ts";
import { fixedMark, routes, serve } from "./spike.ts";

const spikeEnv = env as unknown as SpikeSecrets & { SPIKE_SEED?: string };
const lazy = spikeEnv.SPIKE_SEED === "lazy";
const zeroMark: Hex = `0x${"00".repeat(32)}`;
let secp: Secp256k1Wasm;

if (lazy) {
  secp = instantiateSecp256k1(secp256k1Module);
  const standIn: Secp256k1Wasm = {
    signRecoverable: () => ({ signature: new Uint8Array(64).fill(1), recoveryId: 0 }),
    recover: () => null,
  };
  signMintWasm(standIn, spikeEnv.SPIKE_MINTER_KEY, PLACEHOLDER_ADDRESS, zeroMark, 0);
} else {
  const label = new TextEncoder().encode("bottlecast secp256k1 blinding");
  const hmacKey = hexToBytes(spikeEnv.SPIKE_HMAC_KEY);
  const seedInput = new Uint8Array(label.length + hmacKey.length);
  seedInput.set(label);
  seedInput.set(hmacKey, label.length);
  secp = instantiateSecp256k1(secp256k1Module, keccak_256(seedInput));
  const warmUpHash = keccak_256(new TextEncoder().encode("bottlecast warm-up"));
  const warmUp = secp.signRecoverable(warmUpHash, hexToBytes(spikeEnv.SPIKE_MINTER_KEY));
  secp.recover(warmUpHash, warmUp.signature, warmUp.recoveryId);
  signMintWasm(secp, spikeEnv.SPIKE_MINTER_KEY, PLACEHOLDER_ADDRESS, zeroMark, 0);
}

export default serve(
  {
    ...routes,
    "/bench/sign": async (env) =>
      signMintWasm(secp, env.SPIKE_MINTER_KEY, PLACEHOLDER_ADDRESS, await fixedMark(env), 0).slice(0, 10),
    "/bench/siwe": async (_env, body) => verifySiweWasm(secp, body.message!, body.signature!, body.nonce!),
    "/bench/mint": async (env, body) => (await mintInvocationWasm(secp, env, body.idToken!)).slice(0, 10),
    "/bench/link": async (env, body) =>
      linkInvocationWasm(secp, env, body.idToken!, {
        message: body.message!,
        signature: body.signature!,
        nonce: body.nonce!,
      }),
  },
  (env) => cronMintWasm(secp, env),
);
