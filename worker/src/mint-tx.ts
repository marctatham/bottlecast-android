// The one transaction the minter sends, encoded by hand: the `mint(...)` call's
// ABI encoding inside an EIP-1559 envelope. viem's encodeFunctionData and
// serializeTransaction do the same for any ABI and any transaction type, and
// on a fresh isolate that generality is a few milliseconds of unoptimised
// JavaScript; this is a few loops. bench/local.ts checks it gives viem's bytes.

/** `mint(to, messageId, text, castAt, penName, writerMark)`, as spec §1.7 describes it. */
export interface MintCall {
  to: Uint8Array; // 20 bytes
  messageId: string;
  text: string;
  castAt: bigint; // uint64
  penName: string;
  writerMark: Uint8Array; // 32 bytes
}

/** An EIP-1559 transaction with an empty access list. */
export interface Eip1559Transaction {
  chainId: number;
  nonce: number;
  maxPriorityFeePerGas: bigint;
  maxFeePerGas: bigint;
  gas: bigint;
  to: Uint8Array; // 20 bytes
  value: bigint;
  data: Uint8Array;
}

// The first 4 bytes of keccak256("mint(address,string,string,uint64,string,bytes32)").
const MINT_SELECTOR = Uint8Array.of(0x63, 0x16, 0xc4, 0x2e);
const WORD = 32;
const encoder = new TextEncoder();

/** The calldata: the selector, six head words, then each string's length and padded bytes. */
export function encodeMintCall(call: MintCall): Uint8Array {
  expectLength(call.to, 20, "recipient");
  expectLength(call.writerMark, WORD, "writer mark");
  expectRange(call.castAt, 64, "castAt");
  const strings = [call.messageId, call.text, call.penName].map((s) => encoder.encode(s));
  const headSize = 6 * WORD;
  const tailSizes = strings.map((bytes) => WORD + Math.ceil(bytes.length / WORD) * WORD);
  const offsets: number[] = [];
  let next = headSize;
  for (const size of tailSizes) {
    offsets.push(next);
    next += size;
  }
  const out = new Uint8Array(MINT_SELECTOR.length + next);
  out.set(MINT_SELECTOR);
  const word = (index: number) => MINT_SELECTOR.length + index * WORD;
  out.set(call.to, word(0) + WORD - call.to.length);
  writeUint(out, word(1), BigInt(offsets[0]));
  writeUint(out, word(2), BigInt(offsets[1]));
  writeUint(out, word(3), call.castAt);
  writeUint(out, word(4), BigInt(offsets[2]));
  out.set(call.writerMark, word(5));
  strings.forEach((bytes, i) => {
    const at = MINT_SELECTOR.length + offsets[i];
    writeUint(out, at, BigInt(bytes.length));
    out.set(bytes, at + WORD);
  });
  return out;
}

/** The type-2 envelope, unsigned (what gets hashed and signed) or signed (what gets sent). */
export function serializeEip1559(
  tx: Eip1559Transaction,
  signature?: { yParity: number; r: Uint8Array; s: Uint8Array },
): Uint8Array {
  expectLength(tx.to, 20, "contract address");
  for (const [name, value] of Object.entries({
    chainId: BigInt(tx.chainId),
    nonce: BigInt(tx.nonce),
    maxPriorityFeePerGas: tx.maxPriorityFeePerGas,
    maxFeePerGas: tx.maxFeePerGas,
    gas: tx.gas,
    value: tx.value,
  })) {
    expectRange(value, 256, name);
  }
  if (signature) {
    if (signature.yParity !== 0 && signature.yParity !== 1) throw new RangeError(`Bad yParity ${signature.yParity}`);
    expectLength(signature.r, WORD, "r");
    expectLength(signature.s, WORD, "s");
  }
  const fields: Rlp[] = [
    uint(BigInt(tx.chainId)),
    uint(BigInt(tx.nonce)),
    uint(tx.maxPriorityFeePerGas),
    uint(tx.maxFeePerGas),
    uint(tx.gas),
    tx.to,
    uint(tx.value),
    tx.data,
    [], // the access list
  ];
  if (signature) fields.push(uint(BigInt(signature.yParity)), stripZeros(signature.r), stripZeros(signature.s));
  return concat(Uint8Array.of(0x02), rlp(fields));
}

type Rlp = Uint8Array | Rlp[];

function rlp(item: Rlp): Uint8Array {
  if (item instanceof Uint8Array) {
    if (item.length === 1 && item[0] < 0x80) return item;
    return concat(lengthPrefix(0x80, item.length), item);
  }
  const payload = concat(...item.map(rlp));
  return concat(lengthPrefix(0xc0, payload.length), payload);
}

function lengthPrefix(base: number, length: number): Uint8Array {
  if (length <= 55) return Uint8Array.of(base + length);
  const lengthBytes = uint(BigInt(length));
  return concat(Uint8Array.of(base + 55 + lengthBytes.length), lengthBytes);
}

/** A non-negative integer as RLP wants it: big-endian, no leading zeros, zero as no bytes. */
function uint(value: bigint): Uint8Array {
  const bytes: number[] = [];
  for (let v = value; v > 0n; v >>= 8n) bytes.unshift(Number(v & 0xffn));
  return Uint8Array.from(bytes);
}

function stripZeros(bytes: Uint8Array): Uint8Array {
  let start = 0;
  while (start < bytes.length && bytes[start] === 0) start++;
  return bytes.subarray(start);
}

function writeUint(out: Uint8Array, at: number, value: bigint): void {
  for (let i = WORD - 1, v = value; v > 0n; i--, v >>= 8n) out[at + i] = Number(v & 0xffn);
}

// viem refused these; a hand encoding that took them would sign wrong calldata.
function expectLength(bytes: Uint8Array, size: number, name: string): void {
  if (bytes.length !== size) throw new RangeError(`Expected a ${size}-byte ${name}, got ${bytes.length}`);
}

function expectRange(value: bigint, bits: number, name: string): void {
  if (value < 0n || value >= 1n << BigInt(bits)) throw new RangeError(`${name} doesn't fit uint${bits}: ${value}`);
}

function concat(...parts: Uint8Array[]): Uint8Array {
  const out = new Uint8Array(parts.reduce((sum, part) => sum + part.length, 0));
  let at = 0;
  for (const part of parts) {
    out.set(part, at);
    at += part.length;
  }
  return out;
}
