// Drives the spike Worker. The CPU numbers come from `wrangler tail`, not from
// here: a Worker's clock stands still while it computes, so it can't time itself.
//   SPIKE_URL=https://bottlecast-spike.<subdomain>.workers.dev node bench/run.ts warm [requests]
//   SPIKE_URL=… node bench/run.ts cold <label> [first]  one /bench/mint and one /bench/link, `first` going first
//   SPIKE_URL=… node bench/run.ts rpc                   the reads a mint makes, through viem and as one batch
// Tail samples busy Workers, so SPIKE_GAP_MS spaces warm requests out to keep more events.
import { readFileSync } from "node:fs";
import { idToken, siweBody, type SpikeKeys } from "./fixtures.ts";

const base = process.env.SPIKE_URL ?? "http://localhost:8787";
const gapMs = Number(process.env.SPIKE_GAP_MS ?? 0);
const [mode = "warm", arg] = process.argv.slice(2);
const driver = JSON.parse(readFileSync(".bench-keys.json", "utf8")) as SpikeKeys["driver"];

const token = await idToken(driver.idPrivateJwk);
const siwe = await siweBody(driver.walletKey);
const bodies: Record<string, object | undefined> = {
  noop: undefined,
  hmac: undefined,
  satoken: undefined,
  sign: undefined,
  idtoken: { idToken: token },
  siwe,
  mint: { idToken: token },
  link: { idToken: token, ...siwe },
};

async function call(route: string, run: string): Promise<{ status: number; text: string }> {
  const body = bodies[route];
  const response = await fetch(`${base}/bench/${route}?run=${run}`, {
    method: body ? "POST" : "GET",
    headers: { "X-Spike-Key": driver.spikeKey, "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: response.status, text: await response.text() };
}

if (mode === "warm") {
  const requests = Number(arg ?? 200);
  for (const route of Object.keys(bodies)) {
    const statuses = new Map<number, number>();
    let sample = "";
    for (let i = 0; i < requests; i++) {
      if (gapMs) await new Promise((resolve) => setTimeout(resolve, gapMs));
      const { status, text } = await call(route, "warm");
      statuses.set(status, (statuses.get(status) ?? 0) + 1);
      if (status !== 200 || !sample) sample = text.slice(0, 120);
    }
    console.log(`${route.padEnd(8)} ${[...statuses].map(([s, n]) => `${s}×${n}`).join(" ")}  ${sample}`);
  }
} else if (mode === "cold") {
  const first = process.argv[4] ?? "mint";
  if (first !== "mint" && first !== "link") {
    console.error(`Cold runs start with mint or link, not ${first}`);
    process.exit(1);
  }
  for (const route of [first, first === "mint" ? "link" : "mint"]) {
    const { status, text } = await call(route, `cold-${arg ?? "0"}`);
    console.log(`${route.padEnd(8)} ${status}  ${text.slice(0, 120)}`);
  }
} else if (mode === "rpc") {
  for (const route of ["rpc", "rpc-lean"]) {
    const { status, text } = await call(route, "rpc");
    console.log(`${route.padEnd(8)} ${status}  ${text}`);
  }
} else {
  console.error(`Unknown mode ${mode}: use warm, cold or rpc`);
  process.exit(1);
}
