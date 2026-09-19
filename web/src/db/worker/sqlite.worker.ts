import sqlite3InitModule, { type Database, type Sqlite3Static } from '@sqlite.org/sqlite-wasm';

import { applyMigrations, type SqlRunner, type SqlValue } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import { seedCatalog } from '@/data/catalogSeed';

import catalogSeed from '@shared/seed/catalog.json';

import type { WorkerRequest, WorkerResponse } from './protocol';

const DB_FILENAME = '/fitlog.sqlite3';

interface WorkerScope {
  onmessage: ((event: MessageEvent<WorkerRequest>) => void) | null;
  postMessage(message: WorkerResponse): void;
}

const ctx = globalThis as unknown as WorkerScope;

let sqlite3: Sqlite3Static | undefined;
let db: Database | undefined;

function requireDb(): Database {
  if (!db) {
    throw new Error('La base de datos SQLite no esta inicializada');
  }
  return db;
}

function createRunner(): SqlRunner {
  return {
    exec(sql, params) {
      const database = requireDb();
      if (params && params.length > 0) {
        const stmt = database.prepare(sql);
        try {
          stmt.bind([...params]);
          stmt.step();
        } finally {
          stmt.finalize();
        }
        return;
      }
      database.exec(sql);
    },
    all(sql, params) {
      const database = requireDb();
      const stmt = database.prepare(sql);
      const rows: SqlValue[][] = [];
      try {
        if (params && params.length > 0) {
          stmt.bind([...params]);
        }
        while (stmt.step()) {
          rows.push(stmt.get([]) as unknown as SqlValue[]);
        }
      } finally {
        stmt.finalize();
      }
      return rows;
    },
  };
}

async function init(): Promise<void> {
  sqlite3 = await sqlite3InitModule();
  if (!sqlite3.oo1.OpfsDb) {
    throw new Error('OPFS no esta disponible en este navegador');
  }
  db = new sqlite3.oo1.OpfsDb(DB_FILENAME, 'c');
  db.exec('PRAGMA foreign_keys = ON');

  const schemaVersion = applyMigrations(createRunner(), migrations);
  const seedResult = seedCatalog(createRunner(), catalogSeed);

  const ready: WorkerResponse = {
    id: 0,
    ok: true,
    kind: 'ready',
    schemaVersion,
    persistence: 'opfs',
    sqliteVersion: sqlite3.version.libVersion,
    catalogSeeded: seedResult.seeded,
    catalogExercises: seedResult.exercises,
  };
  ctx.postMessage(ready);
}

function handle(request: WorkerRequest): void {
  try {
    const runner = createRunner();
    if (request.kind === 'exec') {
      runner.exec(request.sql, request.params);
      ctx.postMessage({ id: request.id, ok: true, kind: 'result', rows: [] } satisfies WorkerResponse);
      return;
    }
    const rows = runner.all(request.sql, request.params);
    ctx.postMessage({ id: request.id, ok: true, kind: 'result', rows } satisfies WorkerResponse);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    ctx.postMessage({ id: request.id, ok: false, kind: 'error', error: message } satisfies WorkerResponse);
  }
}

ctx.onmessage = (event: MessageEvent<WorkerRequest>) => {
  handle(event.data);
};

init().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : String(error);
  ctx.postMessage({ id: 0, ok: false, kind: 'error', error: message } satisfies WorkerResponse);
});
