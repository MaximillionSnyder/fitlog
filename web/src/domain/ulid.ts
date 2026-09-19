const ALPHABET = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
const ULID_LENGTH = 26;
const TIME_LENGTH = 10;
const RANDOM_LENGTH = 16;
const MAX_TIMESTAMP_MS = 281474976710655;
const RANDOM_HEX_PATTERN = /^[0-9a-f]{20}$/;
const ULID_PATTERN = /^[0-9A-HJKMNP-TV-Z]{26}$/;

function encodeBase32(value: bigint, length: number): string {
  let remaining = value;
  let out = '';
  for (let i = 0; i < length; i += 1) {
    out = ALPHABET.charAt(Number(remaining % 32n)) + out;
    remaining /= 32n;
  }
  return out;
}

export function randomHex(byteCount = 10): string {
  const bytes = new Uint8Array(byteCount);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

export function ulidFromParts(timestampMs: number, hex: string): string {
  if (!Number.isSafeInteger(timestampMs) || timestampMs < 0 || timestampMs > MAX_TIMESTAMP_MS) {
    throw new RangeError(`timestamp_ms fuera de rango: ${timestampMs}`);
  }
  if (!RANDOM_HEX_PATTERN.test(hex)) {
    throw new RangeError(`random_hex invalido: ${hex}`);
  }
  return (
    encodeBase32(BigInt(timestampMs), TIME_LENGTH) +
    encodeBase32(BigInt(`0x${hex}`), RANDOM_LENGTH)
  );
}

export function generateUlid(timestampMs: number = Date.now()): string {
  return ulidFromParts(timestampMs, randomHex());
}

export function isValidUlid(value: string): boolean {
  if (value.length !== ULID_LENGTH || !ULID_PATTERN.test(value)) {
    return false;
  }
  return value.charAt(0) <= '7';
}
