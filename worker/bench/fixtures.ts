// Throwaway keys and request bodies for the spike. Nothing here is funded,
// and nothing leaves the scratch files that .gitignore keeps out of git.
import { exportJWK, exportPKCS8, generateKeyPair, importJWK, SignJWT, type JWK } from "jose";
import { generatePrivateKey, privateKeyToAccount } from "viem/accounts";
import { createSiweMessage, generateSiweNonce } from "viem/siwe";
import { toHex, type Hex } from "viem";
import { BASE_SEPOLIA, SAMPLE_UID, SPIKE_PROJECT_ID, SPIKE_SIWE_DOMAIN } from "../src/ops.ts";

export interface SpikeKeys {
  /** The Worker's secrets, as `wrangler secret bulk` takes them. */
  worker: Record<string, string>;
  /** What the driver needs to call the Worker. */
  driver: { spikeKey: string; idPrivateJwk: JWK; walletKey: Hex };
}

export async function makeKeys(): Promise<SpikeKeys> {
  const spikeKey = randomHex(32);
  const id = await generateKeyPair("RS256", { extractable: true });
  const idPublicJwk = { ...(await exportJWK(id.publicKey)), kid: "spike", alg: "RS256", use: "sig" };
  const idPrivateJwk = { ...(await exportJWK(id.privateKey)), kid: "spike", alg: "RS256" };
  const serviceAccount = await generateKeyPair("RS256", { extractable: true });
  const pkcs8 = (await exportPKCS8(serviceAccount.privateKey)).replace(/-----[A-Z ]+-----|\s/g, "");
  return {
    worker: {
      SPIKE_KEY: spikeKey,
      SPIKE_MINTER_KEY: generatePrivateKey(),
      SPIKE_HMAC_KEY: randomHex(32),
      SPIKE_ID_JWK: JSON.stringify(idPublicJwk),
      SPIKE_SA_PKCS8: pkcs8,
    },
    driver: { spikeKey, idPrivateJwk, walletKey: generatePrivateKey() },
  };
}

/** A Firebase-shaped ID token: RS256, Firebase's issuer and audience, an hour long. */
export async function idToken(privateJwk: JWK): Promise<string> {
  const key = await importJWK(privateJwk, "RS256");
  return new SignJWT({ auth_time: Math.floor(Date.now() / 1000), firebase: { sign_in_provider: "google.com" } })
    .setProtectedHeader({ alg: "RS256", kid: "spike", typ: "JWT" })
    .setIssuer(`https://securetoken.google.com/${SPIKE_PROJECT_ID}`)
    .setAudience(SPIKE_PROJECT_ID)
    .setSubject(SAMPLE_UID)
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(key);
}

/** The "Prove it's yours" message, signed with plain `personal_sign`. */
export async function siweBody(walletKey: Hex): Promise<{ message: string; signature: Hex; nonce: string }> {
  const account = privateKeyToAccount(walletKey);
  const nonce = generateSiweNonce();
  const now = new Date();
  const message = createSiweMessage({
    address: account.address,
    chainId: BASE_SEPOLIA,
    domain: SPIKE_SIWE_DOMAIN,
    nonce,
    uri: `https://${SPIKE_SIWE_DOMAIN}/wallet`,
    version: "1",
    statement: "Prove it's yours: link this wallet to your BottleCast account. This is free and moves no money.",
    issuedAt: now,
    expirationTime: new Date(now.getTime() + 60 * 60 * 1000),
  });
  return { message, signature: await account.signMessage({ message }), nonce };
}

function randomHex(bytes: number): Hex {
  return toHex(crypto.getRandomValues(new Uint8Array(bytes)));
}
