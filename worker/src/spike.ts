// Slice 5's spike: each route runs one invocation's worth of work, so the CPU
// time Workers Logs records for a request is that invocation's cost. Read the
// numbers with `wrangler tail --format json` (see README.md). Every route
// needs the X-Spike-Key header, so strangers can't use up the free quota.
import { createPublicClient, http, type Hex } from "viem";
import { baseSepolia } from "viem/chains";
import type { JWK } from "jose";
import {
  cronMint,
  linkInvocation,
  mintInvocation,
  PLACEHOLDER_ADDRESS,
  SAMPLE_MESSAGE_ID,
  SAMPLE_UID,
  signMint,
  signServiceAccountJwt,
  SPIKE_SA_EMAIL,
  verifiedMark,
  verifyIdToken,
  verifySiwe,
  type SpikeSecrets,
} from "./ops.ts";

export interface SpikeEnv extends SpikeSecrets {
  SPIKE_KEY: string;
}

const BASE_SEPOLIA_RPC = "https://sepolia.base.org";

export type Body = { idToken?: string; message?: string; signature?: Hex; nonce?: string };
export type Route = (env: SpikeEnv, body: Body) => Promise<unknown>;

export const routes: Record<string, Route> = {
  "/bench/noop": async () => "ok",

  "/bench/sign": async (env) => (await signMint(env.SPIKE_MINTER_KEY, await fixedMark(env), 0)).slice(0, 10),

  "/bench/siwe": async (_env, body) => verifySiwe(body.message!, body.signature!, body.nonce!),

  "/bench/idtoken": async (env, body) => verifyIdToken(body.idToken!, idJwk(env)),

  "/bench/satoken": async (env) => (await signServiceAccountJwt(env.SPIKE_SA_PKCS8, SPIKE_SA_EMAIL)).length,

  "/bench/hmac": async (env) => fixedMark(env),

  "/bench/mint": async (env, body) => (await mintInvocation(env, body.idToken!)).slice(0, 10),

  "/bench/link": async (env, body) =>
    linkInvocation(env, body.idToken!, { message: body.message!, signature: body.signature!, nonce: body.nonce! }),

  // The reads a mint makes before it can send, against Base Sepolia's public
  // RPC. No funds, no broadcast. Counts the subrequests they take. The
  // address is fixed, so no key derivation muddies the CPU figure.
  "/bench/rpc": async () => {
    let subrequests = 0;
    const client = createPublicClient({
      chain: baseSepolia,
      transport: http(BASE_SEPOLIA_RPC, { onFetchRequest: () => void subrequests++ }),
    });
    const nonce = await client.getTransactionCount({ address: PLACEHOLDER_ADDRESS, blockTag: "pending" });
    const fees = await client.estimateFeesPerGas();
    return {
      nonce,
      maxFeePerGas: fees.maxFeePerGas.toString(),
      maxPriorityFeePerGas: fees.maxPriorityFeePerGas.toString(),
      subrequests,
    };
  },

  // The same reads as one JSON-RPC batch, parsed by hand: one subrequest, and
  // no block to format. The public RPC sometimes refuses a batch, and then
  // this fails rather than read a missing answer as 0.
  "/bench/rpc-lean": async () => {
    const response = await fetch(BASE_SEPOLIA_RPC, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify([
        { jsonrpc: "2.0", id: 1, method: "eth_getTransactionCount", params: [PLACEHOLDER_ADDRESS, "pending"] },
        { jsonrpc: "2.0", id: 2, method: "eth_gasPrice", params: [] },
        { jsonrpc: "2.0", id: 3, method: "eth_maxPriorityFeePerGas", params: [] },
      ]),
    });
    const results: unknown = await response.json();
    if (!Array.isArray(results)) throw new Error(`The RPC refused the batch: ${JSON.stringify(results).slice(0, 120)}`);
    const result = (id: number): bigint => {
      const answer = (results as { id: number; result?: Hex; error?: unknown }[]).find((r) => r.id === id);
      if (!answer?.result) throw new Error(`No answer to call ${id}: ${JSON.stringify(answer?.error ?? null)}`);
      return BigInt(answer.result);
    };
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

export function fixedMark(env: SpikeEnv): Promise<Hex> {
  return verifiedMark(env.SPIKE_HMAC_KEY, SAMPLE_UID, SAMPLE_MESSAGE_ID);
}

/** The spike Worker: a route table, and the mint its cron signs. */
export function serve(
  table: Record<string, Route>,
  cron: (env: SpikeEnv) => Promise<Hex>,
): ExportedHandler<SpikeEnv> {
  return {
    async fetch(request, env): Promise<Response> {
      if (request.headers.get("X-Spike-Key") !== env.SPIKE_KEY) return new Response(null, { status: 404 });
      const route = table[new URL(request.url).pathname];
      if (!route) return new Response(null, { status: 404 });
      try {
        const body = request.method === "POST" ? ((await request.json()) as Body) : {};
        return Response.json({ result: await route(env, body) });
      } catch (error) {
        return Response.json({ error: String(error) }, { status: 400 });
      }
    },

    async scheduled(_controller, env): Promise<void> {
      const signed = await cron(env);
      console.log(`cron mint signed ${signed.length / 2 - 1} bytes`);
    },
  };
}

export default serve(routes, cronMint);
