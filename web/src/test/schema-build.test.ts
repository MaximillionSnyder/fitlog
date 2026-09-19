import { describe, expect, it } from 'vitest';

import { migrations } from '@/db/migrations';

import schemaSql from '@shared/schema/schema.sql?raw';

describe('schema.sql generado', () => {
  it('incluye todas las migraciones en orden ascendente', () => {
    let cursor = -1;
    for (const migration of migrations) {
      const body = migration.sql.trim();
      const position = schemaSql.indexOf(body);
      expect(position, `migracion ${migration.name} no encontrada en schema.sql`).toBeGreaterThan(-1);
      expect(position, `migracion ${migration.name} fuera de orden`).toBeGreaterThan(cursor);
      cursor = position;
    }
  });

  it('no contiene SQL de tablas fuera de las migraciones', () => {
    expect(schemaSql).toContain('CREATE TABLE muscle_group');
    expect(schemaSql).toContain('CREATE TABLE set_entry');
    expect(schemaSql).toContain('CREATE TABLE body_metric');
  });
});
