// The spike with a warm-up at module scope. One signature and one recovery,
// done while the isolate starts, build noble's secp256k1 tables and compile
// its signing and recovery code before any request arrives. It's the same
// noble instance viem uses (one deduplicated @noble/curves). Then one offline
// mint signature compiles viem's ABI encoding and transaction serialising.
// Deployed as its own Worker, bottlecast-spike-warm (README.md).
import { env } from "cloudflare:workers";
import { secp256k1 } from "@noble/curves/secp256k1";
import { keccak_256 } from "@noble/hashes/sha3";
import { hexToBytes, type Hex } from "viem";
import { signMint } from "./ops.ts";
import spike from "./spike.ts";

const minterKey = (env as unknown as { SPIKE_MINTER_KEY: Hex }).SPIKE_MINTER_KEY;
const warmUpHash = keccak_256(new TextEncoder().encode("bottlecast warm-up"));
secp256k1.sign(warmUpHash, hexToBytes(minterKey), { lowS: true }).recoverPublicKey(warmUpHash);
// Not awaited. It does no I/O, and the cold numbers fell with it (README.md),
// so it settled while the isolate started up; that was measured, not promised
// by workerd. A failure is logged, not hidden.
signMint(minterKey, `0x${"00".repeat(32)}`, 0).catch((error) => console.error(`Warm-up failed: ${error}`));

export default spike;
