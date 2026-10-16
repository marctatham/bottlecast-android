// Summarises `wrangler tail --format json` output: CPU and wall time per route
// and run (the ?run= label), with cron invocations as their own row.
// Workers report both in whole milliseconds.
//   node bench/summarise.ts <tail.jsonl> [more.jsonl …]
import { readFileSync } from "node:fs";

interface TailEvent {
  cpuTime?: number;
  wallTime?: number;
  outcome: string;
  event: { request?: { url: string }; response?: { status: number }; cron?: string } | null;
}

const events: TailEvent[] = [];
for (const file of process.argv.slice(2)) {
  const raw = readFileSync(file, "utf8");
  // Tail prints one pretty-printed object per event, back to back.
  let depth = 0;
  let start = -1;
  let inString = false;
  for (let i = 0; i < raw.length; i++) {
    const char = raw[i];
    if (inString) {
      if (char === "\\") i++;
      else if (char === '"') inString = false;
    } else if (char === '"') inString = true;
    else if (char === "{" && depth++ === 0) start = i;
    else if (char === "}" && --depth === 0) events.push(JSON.parse(raw.slice(start, i + 1)) as TailEvent);
  }
}

const groups = new Map<string, TailEvent[]>();
for (const e of events) {
  let key = "other";
  if (e.event?.cron) key = "cron";
  else if (e.event?.request) {
    const url = new URL(e.event.request.url);
    const run = url.searchParams.get("run") ?? "";
    key = `${url.pathname.replace("/bench/", "")} ${run.startsWith("cold") ? "cold" : run}`;
  }
  groups.set(key, [...(groups.get(key) ?? []), e]);
}

console.log("route / run          n   p50 CPU  p99 CPU  max CPU  max wall  over 10 ms  not ok  CPU samples (cold and cron)");
for (const [key, list] of [...groups].sort()) {
  const cpu = list.map((e) => e.cpuTime ?? NaN).sort((a, b) => a - b);
  const at = (q: number) => cpu[Math.min(cpu.length - 1, Math.floor(q * cpu.length))];
  const over = cpu.filter((c) => c > 10).length;
  const notOk = list.filter((e) => e.outcome !== "ok" || (e.event?.response && e.event.response.status !== 200)).length;
  const maxWall = Math.max(...list.map((e) => e.wallTime ?? NaN));
  const samples = key.endsWith("cold") || key === "cron" ? list.map((e) => e.cpuTime).join(",") : "";
  console.log(
    `${key.padEnd(18)} ${String(list.length).padStart(4)} ${[at(0.5), at(0.99), cpu[cpu.length - 1], maxWall]
      .map((v) => String(v).padStart(8))
      .join(" ")} ${String(over).padStart(11)} ${String(notOk).padStart(7)}  ${samples}`,
  );
}
