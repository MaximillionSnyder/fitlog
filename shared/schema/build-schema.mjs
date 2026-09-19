#!/usr/bin/env node
// Genera shared/schema/schema.sql concatenando las migraciones en orden.
// schema.sql es un artefacto derivado: nunca se edita a mano.

import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const migrationsDir = join(here, 'migrations');
const outputPath = join(here, 'schema.sql');

const migrationPattern = /^(\d{3})_[a-z0-9_]+\.sql$/;

const migrations = readdirSync(migrationsDir)
  .filter((name) => migrationPattern.test(name))
  .sort();

if (migrations.length === 0) {
  console.error('No se encontraron migraciones en ' + migrationsDir);
  process.exit(1);
}

const expectedVersions = migrations.map((_, index) => String(index + 1).padStart(3, '0'));
const actualVersions = migrations.map((name) => migrationPattern.exec(name)[1]);
for (let i = 0; i < expectedVersions.length; i += 1) {
  if (expectedVersions[i] !== actualVersions[i]) {
    console.error(
      `Secuencia de migraciones invalida: se esperaba ${expectedVersions[i]}_*.sql y se encontro ${migrations[i]}`
    );
    process.exit(1);
  }
}

const header = [
  '-- FitLog - esquema canonico v1 (GENERADO, no editar a mano).',
  '-- Fuente: shared/schema/migrations/*.sql',
  '-- Regenerar con: npm run schema:build',
  '',
].join('\n');

const body = migrations
  .map((name) => {
    const sql = readFileSync(join(migrationsDir, name), 'utf8').trimEnd();
    return `-- >>> ${name}\n\n${sql}\n`;
  })
  .join('\n');

writeFileSync(outputPath, `${header}\n${body}`, 'utf8');
console.log(`schema.sql generado con ${migrations.length} migracion(es): ${migrations.join(', ')}`);
