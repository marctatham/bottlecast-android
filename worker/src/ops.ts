// The CPU-heavy steps slice 6's Worker will run, shared by the spike's routes
// (src/spike.ts, src/spike-wasm.ts) and the local bench (bench/local.ts), so
// both measure the same code. Nothing here touches the network or a funded key.
import {
  encodeFunctionData,
  hashMessage,
  recoverMessageAddress,
  toHex,
  type Address,
  type Hex,
} from "viem";
import { privateKeyToAccount, publicKeyToAddress } from "viem/accounts";
import { parseSiweMessage, validateSiweMessage } from "viem/siwe";
import { keccak_256 } from "@noble/hashes/sha3";
import { importJWK, importPKCS8, jwtVerify, SignJWT, type JWK } from "jose";
import { encodeMintCall, serializeEip1559, type Eip1559Transaction } from "./mint-tx.ts";
import type { Secp256k1Wasm } from "./secp256k1-wasm.ts";

export const BASE_SEPOLIA = 84532;

// A placeholder address: the spike's mints go to it but are never broadcast,
// and its chain reads ask about it.
export const PLACEHOLDER_ADDRESS: Address = "0x00000000000000000000000000000000b0771c11";
export const SPIKE_SA_EMAIL = "spike@demo-bottlecast.iam.gserviceaccount.com";

/** What an invocation signs and checks with: the Worker's secrets, or the local bench's keys. */
export interface SpikeSecrets {
  SPIKE_MINTER_KEY: Hex;
  SPIKE_HMAC_KEY: Hex;
  SPIKE_ID_JWK: string;
  SPIKE_SA_PKCS8: string;
}

// The mint call spec §1.7 describes, so the calldata is the real size.
const mintAbi = [
  {
    type: "function",
    name: "mint",
    stateMutability: "nonpayable",
    inputs: [
      { name: "to", type: "address" },
      { name: "messageId", type: "string" },
      { name: "text", type: "string" },
      { name: "castAt", type: "uint64" },
      { name: "penName", type: "string" },
      { name: "writerMark", type: "bytes32" },
    ],
    outputs: [{ name: "tokenId", type: "uint256" }],
  },
] as const;

// The longest message a bottle can carry on-chain: 280 bytes of UTF-8,
// mixing scripts so the encoding isn't the cheap ASCII case.
export const SAMPLE_TEXT = fitBytes(
  "Hoy el mar me devolvió algo que creía perdido: la calma. " +
    "Leave the porch light on for the people still finding their way home. " +
    "If you're reading this, drink some water and text someone you miss. " +
    "Today the sea gave me back something I thought I'd lost: calm. 🌊🌙 " +
    "Hey, that's pretty cool.",
  280,
);
export const SAMPLE_MESSAGE_ID = "Vb3kQm9TzR2xLp8sWn4E";
export const SAMPLE_UID = "kT7wq2Xr9mZp4LsVb8NcYe3HdQ1a";
const SAMPLE_CAST_AT = 1_791_763_200n; // 12 Oct 2026, as on the boards
const SAMPLE_PEN_NAME = "Driftwood";

export const SPIKE_PROJECT_ID = "demo-bottlecast";
export const SPIKE_SIWE_DOMAIN = "bottling-messages.web.app";

function fitBytes(text: string, limit: number): string {
  const encoder = new TextEncoder();
  let fitted = "";
  for (const char of text) {
    if (encoder.encode(fitted + char).length > limit) break;
    fitted += char;
  }
  return fitted;
}

/** Encodes `mint(...)` and signs it as an EIP-1559 transaction, offline. */
export async function signMint(minterKey: Hex, writerMark: Hex, nonce: number): Promise<Hex> {
  const account = privateKeyToAccount(minterKey);
  const data = encodeFunctionData({
    abi: mintAbi,
    functionName: "mint",
    args: [account.address, SAMPLE_MESSAGE_ID, SAMPLE_TEXT, SAMPLE_CAST_AT, SAMPLE_PEN_NAME, writerMark],
  });
  return account.signTransaction({
    chainId: BASE_SEPOLIA,
    type: "eip1559",
    to: PLACEHOLDER_ADDRESS,
    data,
    nonce,
    gas: 400_000n,
    maxFeePerGas: 1_200_000n,
    maxPriorityFeePerGas: 1_000_000n,
    value: 0n,
  });
}

/** The heaviest `/claim` → mint invocation: who's asking, the mark, the signature. */
export async function mintInvocation(secrets: SpikeSecrets, idToken: string): Promise<Hex> {
  const uid = await verifyIdToken(idToken, JSON.parse(secrets.SPIKE_ID_JWK) as JWK);
  const mark = await verifiedMark(secrets.SPIKE_HMAC_KEY, uid, SAMPLE_MESSAGE_ID);
  return signMint(secrets.SPIKE_MINTER_KEY, mark, 0);
}

/** `/wallet/link`: who's asking, then the wallet's SIWE signature. */
export async function linkInvocation(
  secrets: SpikeSecrets,
  idToken: string,
  siwe: { message: string; signature: Hex; nonce: string },
): Promise<Address> {
  await verifyIdToken(idToken, JSON.parse(secrets.SPIKE_ID_JWK) as JWK);
  return verifySiwe(siwe.message, siwe.signature, siwe.nonce);
}

/** A cron mint: no ID token, but a Firestore token to sign, then the mark and the mint. */
export async function cronMint(secrets: SpikeSecrets): Promise<Hex> {
  await signServiceAccountJwt(secrets.SPIKE_SA_PKCS8, SPIKE_SA_EMAIL);
  const mark = await verifiedMark(secrets.SPIKE_HMAC_KEY, SAMPLE_UID, SAMPLE_MESSAGE_ID);
  return signMint(secrets.SPIKE_MINTER_KEY, mark, 0);
}

/** The verified mark: HMAC-SHA256 over the writer's uid and the messageId. */
export async function verifiedMark(hmacKey: Hex, uid: string, messageId: string): Promise<Hex> {
  const key = await crypto.subtle.importKey(
    "raw",
    hexBytes(hmacKey),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const mac = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`${uid}:${messageId}`));
  return toHex(new Uint8Array(mac));
}

/** Verifies a Firebase-shaped ID token (RS256), as `/claim` and `/wallet/link` will. */
export async function verifyIdToken(token: string, publicJwk: JWK): Promise<string> {
  const key = await importJWK(publicJwk, "RS256");
  const { payload } = await jwtVerify(token, key, {
    algorithms: ["RS256"],
    issuer: `https://securetoken.google.com/${SPIKE_PROJECT_ID}`,
    audience: SPIKE_PROJECT_ID,
  });
  if (!payload.sub) throw new Error("The token has no subject");
  return payload.sub;
}

/** Checks a SIWE message signed with plain `personal_sign`, as `/wallet/link` will. */
export async function verifySiwe(message: string, signature: Hex, nonce: string): Promise<Address> {
  const fields = parseSiweMessage(message);
  const valid = validateSiweMessage({ message: fields, domain: SPIKE_SIWE_DOMAIN, nonce });
  if (!valid || !fields.address) throw new Error("The SIWE message isn't valid");
  const signer = await recoverMessageAddress({ message, signature });
  if (signer.toLowerCase() !== fields.address.toLowerCase()) throw new Error("The signer doesn't match");
  return signer;
}

/** Signs the service-account JWT that buys a Firestore REST access token. */
export async function signServiceAccountJwt(pkcs8Base64: string, clientEmail: string): Promise<string> {
  const pem = `-----BEGIN PRIVATE KEY-----\n${pkcs8Base64}\n-----END PRIVATE KEY-----`;
  const key = await importPKCS8(pem, "RS256");
  return new SignJWT({ scope: "https://www.googleapis.com/auth/datastore" })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(clientEmail)
    .setSubject(clientEmail)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(key);
}

// The same steps with secp256k1 as WebAssembly (src/secp256k1-wasm.ts) in
// place of noble, and the mint encoded by hand (src/mint-tx.ts) in place of
// viem's encoder. The caller passes the signer in, because the Worker and the
// local bench compile its module differently.

/**
 * `signMint` with the WebAssembly signer and the hand encoding. The recipient
 * is given, as slice 6's claim will give the finder's wallet, so no public key
 * is derived from the minter key. Given the minter's own address it returns
 * `signMint`'s bytes.
 */
export function signMintWasm(
  secp: Secp256k1Wasm,
  minterKey: Hex,
  recipient: Address,
  writerMark: Hex,
  nonce: number,
): Hex {
  const transaction: Eip1559Transaction = {
    chainId: BASE_SEPOLIA,
    nonce,
    maxPriorityFeePerGas: 1_000_000n,
    maxFeePerGas: 1_200_000n,
    gas: 400_000n,
    to: hexBytes(PLACEHOLDER_ADDRESS),
    value: 0n,
    data: encodeMintCall({
      to: hexBytes(recipient),
      messageId: SAMPLE_MESSAGE_ID,
      text: SAMPLE_TEXT,
      castAt: SAMPLE_CAST_AT,
      penName: SAMPLE_PEN_NAME,
      writerMark: hexBytes(writerMark),
    }),
  };
  const hash = keccak_256(serializeEip1559(transaction));
  const { signature, recoveryId } = secp.signRecoverable(hash, hexBytes(minterKey));
  return toHex(
    serializeEip1559(transaction, { yParity: recoveryId, r: signature.subarray(0, 32), s: signature.subarray(32) }),
  );
}

/** `verifySiwe` with the WebAssembly signer recovering the key. */
export function verifySiweWasm(secp: Secp256k1Wasm, message: string, signature: Hex, nonce: string): Address {
  const fields = parseSiweMessage(message);
  const valid = validateSiweMessage({ message: fields, domain: SPIKE_SIWE_DOMAIN, nonce });
  if (!valid || !fields.address) throw new Error("The SIWE message isn't valid");
  const bytes = hexBytes(signature);
  if (bytes.length !== 65) throw new Error("A personal_sign signature is 65 bytes");
  // v is 27 or 28 from most wallets, 0 or 1 from some.
  const v = bytes[64];
  const publicKey = secp.recover(hexBytes(hashMessage(message)), bytes.subarray(0, 64), v >= 27 ? v - 27 : v);
  if (!publicKey) throw new Error("The signature doesn't recover a key");
  const signer = publicKeyToAddress(toHex(publicKey));
  if (signer.toLowerCase() !== fields.address.toLowerCase()) throw new Error("The signer doesn't match");
  return signer;
}

/** `mintInvocation` with the WebAssembly signer. */
export async function mintInvocationWasm(secp: Secp256k1Wasm, secrets: SpikeSecrets, idToken: string): Promise<Hex> {
  const uid = await verifyIdToken(idToken, JSON.parse(secrets.SPIKE_ID_JWK) as JWK);
  const mark = await verifiedMark(secrets.SPIKE_HMAC_KEY, uid, SAMPLE_MESSAGE_ID);
  return signMintWasm(secp, secrets.SPIKE_MINTER_KEY, PLACEHOLDER_ADDRESS, mark, 0);
}

/** `linkInvocation` with the WebAssembly signer. */
export async function linkInvocationWasm(
  secp: Secp256k1Wasm,
  secrets: SpikeSecrets,
  idToken: string,
  siwe: { message: string; signature: Hex; nonce: string },
): Promise<Address> {
  await verifyIdToken(idToken, JSON.parse(secrets.SPIKE_ID_JWK) as JWK);
  return verifySiweWasm(secp, siwe.message, siwe.signature, siwe.nonce);
}

/** `cronMint` with the WebAssembly signer. */
export async function cronMintWasm(secp: Secp256k1Wasm, secrets: SpikeSecrets): Promise<Hex> {
  await signServiceAccountJwt(secrets.SPIKE_SA_PKCS8, SPIKE_SA_EMAIL);
  const mark = await verifiedMark(secrets.SPIKE_HMAC_KEY, SAMPLE_UID, SAMPLE_MESSAGE_ID);
  return signMintWasm(secp, secrets.SPIKE_MINTER_KEY, PLACEHOLDER_ADDRESS, mark, 0);
}

function hexBytes(hex: Hex): Uint8Array<ArrayBuffer> {
  const bytes = new Uint8Array((hex.length - 2) / 2);
  for (let i = 0; i < bytes.length; i++) bytes[i] = parseInt(hex.slice(2 + i * 2, 4 + i * 2), 16);
  return bytes;
}
