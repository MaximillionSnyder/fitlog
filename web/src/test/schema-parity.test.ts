import { DatabaseSync } from 'node:sqlite';
import { getTableConfig } from 'drizzle-orm/sqlite-core';
import { describe, expect, it } from 'vitest';

import { schema } from '@/db/schema';

import schemaSql from '@shared/schema/schema.sql?raw';

interface CanonicalColumn {
  name: string;
  type: string;
  notNull: boolean;
  primaryKey: boolean;
}

function canonicalTables(db: DatabaseSync): Map<string, Map<string, CanonicalColumn>> {
  const tables = db
    .prepare("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")
    .all();

  const result = new Map<string, Map<string, CanonicalColumn>>();
  for (const table of tables) {
    const tableName = String(table.name);
    const columns = db.prepare(`PRAGMA table_info(${tableName})`).all();
    const columnMap = new Map<string, CanonicalColumn>();
    for (const column of columns) {
      columnMap.set(String(column.name), {
        name: String(column.name),
        type: String(column.type).toUpperCase(),
        notNull: Number(column.notnull) === 1,
        primaryKey: Number(column.pk) > 0,
      });
    }
    result.set(tableName, columnMap);
  }
  return result;
}

function drizzleTables(): Map<string, Map<string, CanonicalColumn>> {
  const result = new Map<string, Map<string, CanonicalColumn>>();
  for (const table of Object.values(schema)) {
    const config = getTableConfig(table);
    const columnMap = new Map<string, CanonicalColumn>();
    for (const column of config.columns) {
      columnMap.set(column.name, {
        name: column.name,
        type: column.getSQLType().toUpperCase(),
        notNull: column.notNull,
        primaryKey: column.primary,
      });
    }
    result.set(config.name, columnMap);
  }
  return result;
}

describe('paridad entre schema.sql y Drizzle', () => {
  const db = new DatabaseSync(':memory:');
  db.exec(schemaSql);
  const canonical = canonicalTables(db);
  const drizzle = drizzleTables();

  it('define exactamente las mismas tablas', () => {
    expect([...drizzle.keys()].sort()).toEqual([...canonical.keys()].sort());
  });

  for (const [tableName, canonicalColumns] of canonical) {
    it(`declara las mismas columnas en ${tableName}`, () => {
      const drizzleColumns = drizzle.get(tableName);
      expect(drizzleColumns).toBeDefined();
      expect([...(drizzleColumns?.keys() ?? [])].sort()).toEqual(
        [...canonicalColumns.keys()].sort()
      );
    });

    it(`coincide tipo, nulabilidad y PK en ${tableName}`, () => {
      const drizzleColumns = drizzle.get(tableName);
      for (const [columnName, canonicalColumn] of canonicalColumns) {
        const drizzleColumn = drizzleColumns?.get(columnName);
        expect(drizzleColumn, `${tableName}.${columnName}`).toEqual(canonicalColumn);
      }
    });
  }
});
