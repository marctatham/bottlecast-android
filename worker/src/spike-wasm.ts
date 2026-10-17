// The spike with secp256k1 as WebAssembly (src/secp256k1-wasm.ts) in place of
// noble: the mint, link, sign and SIWE routes and the cron mint use it, and
// every other route is the plain spike's. Deployed as its own Worker,
// bottlecast-spike-wasm (README.md).
//
// The module is compiled and instantiated while the isolate starts, so that
// cost lands in startup (limit 1 s), not in a request. Then everything a mint,
// a link or a cron mint runs is run at module scope, so V8 has compiled it
// before the first request: a signature and a recovery, an offline mint, the
// SIWE check, the ID-token check, the service-account JWT and the mark.
//
// A first try (stage 1, README.md) warmed up only the mint's JavaScript, and
// left libsecp256k1's first call to the first request: deployed, that call
// cost far more than in Node, probably because V8 compiles each WebAssembly
// function on its first call. Warming the signer needs its blinding seeded
// first, and Workers allows no random numbers at module scope, so the seed
// comes from a secret: a hash of the spike's HMAC key, kept apart from the
// mark by a label. Slice 6 would give it a secret of its own.
import { env } from "cloudflare:workers";
import secp256k1Module from "../node_modules/tiny-secp256k1/lib/secp256k1.wasm";
import { keccak_256 } from "@noble/hashes/sha3";
import { errors, type JWK } from "jose";
import { hashMessage, hexToBytes, toHex, type Hex } from "viem";
import { publicKeyToAddress } from "viem/accounts";
import { createSiweMessage } from "viem/siwe";
import {
  BASE_SEPOLIA,
  cronMintWasm,
  linkInvocationWasm,
  mintInvocationWasm,
  PLACEHOLDER_ADDRESS,
  signMintWasm,
  signServiceAccountJwt,
  SPIKE_SA_EMAIL,
  SPIKE_SIWE_DOMAIN,
  verifiedMark,
  verifyIdToken,
  verifySiweWasm,
  type SpikeSecrets,
} from "./ops.ts";
import { instantiateSecp256k1 } from "./secp256k1-wasm.ts";
import { fixedMark, routes, serve } from "./spike.ts";

const secrets = env as unknown as SpikeSecrets;
const encoder = new TextEncoder();
const minterKey = hexToBytes(secrets.SPIKE_MINTER_KEY);

const seedLabel = encoder.encode("bottlecast secp256k1 blinding");
const hmacKey = hexToBytes(secrets.SPIKE_HMAC_KEY);
const seedInput = new Uint8Array(seedLabel.length + hmacKey.length);
seedInput.set(seedLabel);
seedInput.set(hmacKey, seedLabel.length);
const secp = instantiateSecp256k1(secp256k1Module, keccak_256(seedInput));

// The curve, both ways, an offline mint, and the link's SIWE check on a message
// the minter signs for itself. These throw if anything's wrong, which fails the
// deploy rather than leaving a Worker that isn't warmed up. One run compiles
// the JavaScript, but V8 still interprets it, and keccak alone then costs about
// 2 ms a mint. So they run 30 times, and V8 has optimised them by the first
// request; that adds about 40 ms of startup. `--var SPIKE_WARM_ROUNDS:<n>`
// changes the count (1 reproduces stage 2, README.md).
const warmUpHash = keccak_256(encoder.encode("bottlecast warm-up"));
const warmUp = secp.signRecoverable(warmUpHash, minterKey);
const minterAddress = publicKeyToAddress(toHex(secp.recover(warmUpHash, warmUp.signature, warmUp.recoveryId)!));
const warmUpNonce = "warmupnonce1";
const siweMessage = createSiweMessage({
  address: minterAddress,
  chainId: BASE_SEPOLIA,
  domain: SPIKE_SIWE_DOMAIN,
  nonce: warmUpNonce,
  uri: `https://${SPIKE_SIWE_DOMAIN}/wallet`,
  version: "1",
  issuedAt: new Date(0),
});
const rounds = Math.max(1, Number((env as unknown as { SPIKE_WARM_ROUNDS?: string }).SPIKE_WARM_ROUNDS ?? 30));
for (let round = 0; round < rounds; round++) {
  signMintWasm(secp, secrets.SPIKE_MINTER_KEY, PLACEHOLDER_ADDRESS, `0x${"00".repeat(32)}`, round);
  const siweSigned = secp.signRecoverable(hexToBytes(hashMessage(siweMessage)), minterKey);
  const siweSignature: Hex = `${toHex(siweSigned.signature)}${(27 + siweSigned.recoveryId).toString(16)}`;
  verifySiweWasm(secp, siweMessage, siweSignature, warmUpNonce);
}

// The WebCrypto steps are async. They're not awaited (module scope can't), but
// workerd settles them before the first request, as `bench/run.ts` against
// `wrangler dev` shows from the log order.
const base64url = (json: object) =>
  btoa(JSON.stringify(json)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
// A well-formed ID token with a wrong signature: jose imports the key and
// runs WebCrypto's verify, then refuses it, as it should.
const unsignedToken = `${base64url({ alg: "RS256", kid: "spike", typ: "JWT" })}.${base64url({ sub: "warm-up" })}`;
const warmUps = [
  verifyIdToken(`${unsignedToken}.${"A".repeat(342)}`, JSON.parse(secrets.SPIKE_ID_JWK) as JWK).then(
    () => Promise.reject(new Error("a token with a wrong signature passed")),
    (error: unknown) => {
      if (!(error instanceof errors.JWSSignatureVerificationFailed)) throw error;
    },
  ),
  signServiceAccountJwt(secrets.SPIKE_SA_PKCS8, SPIKE_SA_EMAIL),
  verifiedMark(secrets.SPIKE_HMAC_KEY, "warm-up", "warm-up"),
];
Promise.all(warmUps).then(
  () => console.log("Warm-up settled"),
  (error) => console.error(`Warm-up failed: ${error}`),
);

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
