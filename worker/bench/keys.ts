// Makes the spike's throwaway keys: .dev.vars for `wrangler dev`, .secrets.json
// for `wrangler secret bulk`, and .bench-keys.json for the driver. All three
// are gitignored. Run it again with --force to replace them.
import { existsSync, writeFileSync } from "node:fs";
import { makeKeys } from "./fixtures.ts";

if (existsSync(".dev.vars") && !process.argv.includes("--force")) {
  console.error("Keys already exist. Pass --force to replace them.");
  process.exit(1);
}

const keys = await makeKeys();
const dotenv = Object.entries(keys.worker).map(([name, value]) => `${name}='${value}'`).join("\n");
writeFileSync(".dev.vars", `${dotenv}\n`, { mode: 0o600 });
writeFileSync(".secrets.json", `${JSON.stringify(keys.worker, null, 2)}\n`, { mode: 0o600 });
writeFileSync(".bench-keys.json", `${JSON.stringify(keys.driver, null, 2)}\n`, { mode: 0o600 });
console.log("Wrote .dev.vars, .secrets.json and .bench-keys.json");
