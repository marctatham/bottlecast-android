// Slice 5's spike: each route runs one invocation's worth of work, so the CPU
// time Workers Logs records for a request is that invocation's cost. Read the
// numbers with `wrangler tail --format json` (see README.md). Every route
// needs the X-Spike-Key header, so strangers can't use up the free quota.
import { createPublicClient, http, type Hex } from "viem";
import { baseSepolia } from "viem/chains";
import type { JWK } from "jose";
import {
  SAMPLE_MESSAGE_ID,
  SAMPLE_UID,
  signMint,
  signServiceAccountJwt,
  verifiedMark,
  verifyIdToken,
  verifySiwe,
} from "./ops.ts";

interface SpikeEnv {
  SPIKE_KEY: string;
  SPIKE_MINTER_KEY: Hex;
  SPIKE_HMAC_KEY: Hex;
  SPIKE_ID_JWK: string;
  SPIKE_SA_PKCS8: string;
}

const SA_EMAIL = "spike@demo-bottlecast.iam.gserviceaccount.com";
const BASE_SEPOLIA_RPC = "https://sepolia.base.org";
const READ_ADDRESS = "0x00000000000000000000000000000000b0771c11";

type Body = { idToken?: string; message?: string; signature?: Hex; nonce?: string };

const routes: Record<string, (env: SpikeEnv, body: Body) => Promise<unknown>> = {
  "/bench/noop": async () => "ok",

  "/bench/sign": async (env) => (await signMint(env.SPIKE_MINTER_KEY, await fixedMark(env), 0)).slice(0, 10),

  "/bench/siwe": async (_env, body) => verifySiwe(body.message!, body.signature!, body.nonce!),

  "/bench/idtoken": async (env, body) => verifyIdToken(body.idToken!, idJwk(env)),

  "/bench/satoken": async (env) => (await signServiceAccountJwt(env.SPIKE_SA_PKCS8, SA_EMAIL)).length,

  "/bench/hmac": async (env) => fixedMark(env),

  // The heaviest `/claim` → mint invocation: who's asking, the mark, the signature.
  "/bench/mint": async (env, body) => {
    const uid = await verifyIdToken(body.idToken!, idJwk(env));
    const mark = await verifiedMark(env.SPIKE_HMAC_KEY, uid, SAMPLE_MESSAGE_ID);
    return (await signMint(env.SPIKE_MINTER_KEY, mark, 0)).slice(0, 10);
  },

  // `/wallet/link`: who's asking, then the wallet's SIWE signature.
  "/bench/link": async (env, body) => {
    await verifyIdToken(body.idToken!, idJwk(env));
    return verifySiwe(body.message!, body.signature!, body.nonce!);
  },

  // The reads a mint makes before it can send, against Base Sepolia's public
  // RPC. No funds, no broadcast. Counts the subrequests they take. The
  // address is fixed, so no key derivation muddies the CPU figure.
  "/bench/rpc": async () => {
    let subrequests = 0;
    const client = createPublicClient({
      chain: baseSepolia,
      transport: http(BASE_SEPOLIA_RPC, { onFetchRequest: () => void subrequests++ }),
    });
    const nonce = await client.getTransactionCount({ address: READ_ADDRESS, blockTag: "pending" });
    const fees = await client.estimateFeesPerGas();
    return {
      nonce,
      maxFeePerGas: fees.maxFeePerGas.toString(),
      maxPriorityFeePerGas: fees.maxPriorityFeePerGas.toString(),
      subrequests,
    };
  },

  // The same reads as one JSON-RPC batch, parsed by hand: one subrequest, and
  // no block to format.
  "/bench/rpc-lean": async () => {
    const response = await fetch(BASE_SEPOLIA_RPC, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify([
        { jsonrpc: "2.0", id: 1, method: "eth_getTransactionCount", params: [READ_ADDRESS, "pending"] },
        { jsonrpc: "2.0", id: 2, method: "eth_gasPrice", params: [] },
        { jsonrpc: "2.0", id: 3, method: "eth_maxPriorityFeePerGas", params: [] },
      ]),
    });
    const results = (await response.json()) as { id: number; result?: Hex }[];
    const result = (id: number) => BigInt(results.find((r) => r.id === id)?.result ?? "0x0");
    return {
      nonce: Number(result(1)),
      gasPrice: result(2).toString(),
      maxPriorityFeePerGas: result(3).toString(),
      subrequests: 1,
    };
  },
};

function idJwk(env: SpikeEnv): JWK {
  return JSON.parse(env.SPIKE_ID_JWK) as JWK;
}

function fixedMark(env: SpikeEnv): Promise<Hex> {
  return verifiedMark(env.SPIKE_HMAC_KEY, SAMPLE_UID, SAMPLE_MESSAGE_ID);
}

export default {
  async fetch(request, env): Promise<Response> {
    if (request.headers.get("X-Spike-Key") !== env.SPIKE_KEY) return new Response(null, { status: 404 });
    const route = routes[new URL(request.url).pathname];
    if (!route) return new Response(null, { status: 404 });
    const body = request.method === "POST" ? ((await request.json()) as Body) : {};
    try {
      return Response.json({ result: await route(env, body) });
    } catch (error) {
      return Response.json({ error: String(error) }, { status: 400 });
    }
  },

  // A cron mint: no ID token, but a Firestore token to sign, then the mark and the mint.
  async scheduled(_controller, env): Promise<void> {
    await signServiceAccountJwt(env.SPIKE_SA_PKCS8, SA_EMAIL);
    const mark = await verifiedMark(env.SPIKE_HMAC_KEY, SAMPLE_UID, SAMPLE_MESSAGE_ID);
    const signed = await signMint(env.SPIKE_MINTER_KEY, mark, 0);
    console.log(`cron mint signed ${signed.length / 2 - 1} bytes`);
  },
} satisfies ExportedHandler<SpikeEnv>;
