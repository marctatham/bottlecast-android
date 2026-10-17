// A first read on this machine, before anything is deployed: each operation's
// first call in a fresh process (secp256k1's tables included), then its cost
// over many calls. Cloudflare's CPUs differ, so the deployed numbers decide.
// It first checks that the WebAssembly signer gives noble's bytes, and stops
// if it doesn't.
//   node bench/local.ts [calls] [noble|wasm]   which signer's first call goes first
import { strict as assert } from "node:assert";
import { readFileSync } from "node:fs";
import { performance } from "node:perf_hooks";
import type { JWK } from "jose";
import type { Hex } from "viem";
import { privateKeyToAccount } from "viem/accounts";
import {
  cronMint,
  cronMintWasm,
  linkInvocation,
  linkInvocationWasm,
  mintInvocation,
  mintInvocationWasm,
  PLACEHOLDER_ADDRESS,
  SAMPLE_MESSAGE_ID,
  SAMPLE_TEXT,
  SAMPLE_UID,
  signMint,
  signMintWasm,
  signServiceAccountJwt,
  SPIKE_SA_EMAIL,
  verifiedMark,
  verifyIdToken,
  verifySiwe,
  verifySiweWasm,
  type SpikeSecrets,
} from "../src/ops.ts";
import { instantiateSecp256k1 } from "../src/secp256k1-wasm.ts";
import { idToken, makeKeys, siweBody } from "./fixtures.ts";

const calls = Number(process.argv[2] ?? 500);
const first = process.argv[3] === "wasm" ? "wasm" : "noble";
const keys = await makeKeys();
const env = keys.worker as unknown as SpikeSecrets;
const jwk = JSON.parse(env.SPIKE_ID_JWK) as JWK;

type Op = () => Promise<unknown>;
const mark = () => verifiedMark(env.SPIKE_HMAC_KEY, SAMPLE_UID, SAMPLE_MESSAGE_ID);
const sign: Op = async () => signMint(env.SPIKE_MINTER_KEY, await mark(), 0);

// Each signer's first call: the one that goes first in the process also pays
// for compiling viem's encoding and the first HMAC, so run it both ways.
// noble's first call builds its precomputed tables. The WebAssembly module is
// compiled from the file here; the Worker compiles it at startup.
const wasmFile = readFileSync(new URL("../node_modules/tiny-secp256k1/lib/secp256k1.wasm", import.meta.url));
let secp!: ReturnType<typeof instantiateSecp256k1>;
const instantiate = await measure(async () => {
  secp = instantiateSecp256k1(new WebAssembly.Module(wasmFile));
});
const signWasm: Op = async () => signMintWasm(secp, env.SPIKE_MINTER_KEY, PLACEHOLDER_ADDRESS, await mark(), 0);
const firsts: [string, Op][] = [
  ["sign", sign],
  ["sign-w", signWasm],
];
if (first === "wasm") firsts.reverse();
console.log(`Text on-chain: ${new TextEncoder().encode(SAMPLE_TEXT).length} bytes`);
console.log(`WebAssembly module, compiled and instantiated: ${instantiate.cpu.toFixed(2)} ms CPU`);
for (const [index, [name, op]] of firsts.entries()) {
  const cold = await measure(op);
  const when = index === 0 ? "first in the process" : `first, after ${firsts[0][0]}`;
  console.log(`${name}, ${when}: ${cold.wall.toFixed(2)} ms wall, ${cold.cpu.toFixed(2)} ms CPU`);
}
console.log();

const token = await idToken(keys.driver.idPrivateJwk);
const siwe = await siweBody(keys.driver.walletKey);

// RFC 6979 makes both signatures deterministic, so the transactions must match
// byte for byte, and both paths must recover the wallet that signed.
const markHex = await mark();
const recipient = privateKeyToAccount(env.SPIKE_MINTER_KEY).address;
for (let nonce = 0; nonce <= 20; nonce++) {
  const wasm = signMintWasm(secp, env.SPIKE_MINTER_KEY, recipient, markHex, nonce);
  assert.equal(wasm, await signMint(env.SPIKE_MINTER_KEY, markHex, nonce));
}
const wallet = privateKeyToAccount(keys.driver.walletKey).address;
const v = parseInt(siwe.signature.slice(-2), 16);
const withV = (yParityOrV: number): Hex => `${siwe.signature.slice(0, -2)}${yParityOrV.toString(16).padStart(2, "0")}` as Hex;
assert.equal(await verifySiwe(siwe.message, siwe.signature, siwe.nonce), wallet);
assert.equal(verifySiweWasm(secp, siwe.message, siwe.signature, siwe.nonce), wallet);
assert.equal(verifySiweWasm(secp, siwe.message, withV(v - 27), siwe.nonce), wallet);
// A changed r, or the other recovery id, recovers another key or none.
const otherR = `${siwe.signature.slice(0, 10)}${siwe.signature[10] === "0" ? "1" : "0"}${siwe.signature.slice(11)}` as Hex;
assert.throws(() => verifySiweWasm(secp, siwe.message, otherR, siwe.nonce));
assert.throws(() => verifySiweWasm(secp, siwe.message, withV(v === 27 ? 28 : 27), siwe.nonce));
assert.throws(() => verifySiweWasm(secp, siwe.message, withV(29), siwe.nonce));
console.log("The WebAssembly signer matches noble: 21 mints byte for byte, and the SIWE signer\n");

const ops: Record<string, Op> = {
  hmac: mark,
  satoken: () => signServiceAccountJwt(env.SPIKE_SA_PKCS8, SPIKE_SA_EMAIL),
  idtoken: () => verifyIdToken(token, jwk),
  siwe: () => verifySiwe(siwe.message, siwe.signature, siwe.nonce),
  sign,
  mint: () => mintInvocation(env, token),
  link: () => linkInvocation(env, token, siwe),
  cron: () => cronMint(env),
  "siwe-w": async () => verifySiweWasm(secp, siwe.message, siwe.signature, siwe.nonce),
  "sign-w": signWasm,
  "mint-w": () => mintInvocationWasm(secp, env, token),
  "link-w": () => linkInvocationWasm(secp, env, token, siwe),
  "cron-w": () => cronMintWasm(secp, env),
};

console.log(`op        first CPU   p50 CPU   p99 CPU   max CPU   (ms, ${calls} calls)`);
for (const [name, op] of Object.entries(ops)) {
  const first = await measure(op);
  const samples: number[] = [];
  for (let i = 0; i < calls; i++) samples.push((await measure(op)).cpu);
  samples.sort((a, b) => a - b);
  const at = (q: number) => samples[Math.min(samples.length - 1, Math.floor(q * samples.length))];
  console.log(
    [name.padEnd(8), first.cpu, at(0.5), at(0.99), samples[samples.length - 1]]
      .map((v) => (typeof v === "number" ? v.toFixed(2).padStart(9) : v))
      .join(" "),
  );
}

async function measure(op: Op): Promise<{ wall: number; cpu: number }> {
  const start = performance.now();
  const cpu = process.cpuUsage();
  await op();
  const used = process.cpuUsage(cpu);
  return { wall: performance.now() - start, cpu: (used.user + used.system) / 1000 };
}
