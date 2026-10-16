// A first read on this machine, before anything is deployed: each operation's
// first call in a fresh process (secp256k1's tables included), then its cost
// over many calls. Cloudflare's CPUs differ, so the deployed numbers decide.
//   node bench/local.ts [calls]
import { performance } from "node:perf_hooks";
import type { JWK } from "jose";
import {
  cronMint,
  linkInvocation,
  mintInvocation,
  SAMPLE_MESSAGE_ID,
  SAMPLE_TEXT,
  SAMPLE_UID,
  signMint,
  signServiceAccountJwt,
  SPIKE_SA_EMAIL,
  verifiedMark,
  verifyIdToken,
  verifySiwe,
  type SpikeSecrets,
} from "../src/ops.ts";
import { idToken, makeKeys, siweBody } from "./fixtures.ts";

const calls = Number(process.argv[2] ?? 500);
const keys = await makeKeys();
const env = keys.worker as unknown as SpikeSecrets;
const jwk = JSON.parse(env.SPIKE_ID_JWK) as JWK;

type Op = () => Promise<unknown>;
const mark = () => verifiedMark(env.SPIKE_HMAC_KEY, SAMPLE_UID, SAMPLE_MESSAGE_ID);
const sign: Op = async () => signMint(env.SPIKE_MINTER_KEY, await mark(), 0);

// The first secp256k1 operation in the process pays for its precomputed tables.
const cold = await measure(sign);
console.log(`Text on-chain: ${new TextEncoder().encode(SAMPLE_TEXT).length} bytes`);
console.log(`sign, first call in the process: ${cold.wall.toFixed(2)} ms wall, ${cold.cpu.toFixed(2)} ms CPU\n`);

const token = await idToken(keys.driver.idPrivateJwk);
const siwe = await siweBody(keys.driver.walletKey);
const ops: Record<string, Op> = {
  hmac: mark,
  satoken: () => signServiceAccountJwt(env.SPIKE_SA_PKCS8, SPIKE_SA_EMAIL),
  idtoken: () => verifyIdToken(token, jwk),
  siwe: () => verifySiwe(siwe.message, siwe.signature, siwe.nonce),
  sign,
  mint: () => mintInvocation(env, token),
  link: () => linkInvocation(env, token, siwe),
  cron: () => cronMint(env),
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
