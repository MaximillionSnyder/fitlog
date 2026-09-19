import { describe, expect, it } from 'vitest';

import { generateUlid, isValidUlid, randomHex, ulidFromParts } from '@/domain/ulid';

import vectors from '@shared/test-vectors/ulid.json';

interface FromPartsCase {
  timestamp_ms: number;
  random_hex: string;
  expected: string;
}

interface InvalidPartsCase {
  timestamp_ms: number;
  random_hex: string;
}

const validCases: string[] = vectors.valid;
const invalidCases: string[] = vectors.invalid;
const fromPartsCases: FromPartsCase[] = vectors.from_parts;
const invalidPartsCases: InvalidPartsCase[] = vectors.from_parts_invalid;

describe('isValidUlid', () => {
  it.each(validCases)('acepta %s', (value) => {
    expect(isValidUlid(value)).toBe(true);
  });

  it.each(invalidCases)('rechaza %s', (value) => {
    expect(isValidUlid(value)).toBe(false);
  });
});

describe('ulidFromParts', () => {
  it.each(fromPartsCases)(
    'timestamp $timestamp_ms + $random_hex -> $expected',
    ({ timestamp_ms, random_hex, expected }) => {
      expect(ulidFromParts(timestamp_ms, random_hex)).toBe(expected);
    }
  );

  it.each(invalidPartsCases)(
    'rechaza timestamp $timestamp_ms con random $random_hex',
    ({ timestamp_ms, random_hex }) => {
      expect(() => ulidFromParts(timestamp_ms, random_hex)).toThrow(RangeError);
    }
  );
});

describe('generateUlid', () => {
  it('genera ULIDs validos y unicos', () => {
    const generated = new Set(Array.from({ length: 500 }, () => generateUlid()));
    expect(generated.size).toBe(500);
    for (const value of generated) {
      expect(isValidUlid(value)).toBe(true);
    }
  });

  it('codifica el timestamp indicado', () => {
    const timestamp = 1700000000000;
    const value = generateUlid(timestamp);
    expect(value.slice(0, 10)).toBe(ulidFromParts(timestamp, randomHex()).slice(0, 10));
  });
});
