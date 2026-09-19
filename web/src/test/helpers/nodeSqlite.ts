import { DatabaseSync } from 'node:sqlite';

import type { SqlClient } from '@/db/client';
import type { SqlRunner, SqlValue } from '@/db/migrate';

export interface NodeSqliteHarness {
  readonly db: DatabaseSync;
  readonly runner: SqlRunner;
  readonly client: SqlClient;
}

export function createNodeSqliteHarness(): NodeSqliteHarness {
  const db = new DatabaseSync(':memory:');

  const runner: SqlRunner = {
    exec(sql, params) {
      if (params && params.length > 0) {
        db.prepare(sql).run(...params);
        return;
      }
      db.exec(sql);
    },
    all(sql, params) {
      const statement = db.prepare(sql);
      const rows =
        params && params.length > 0
          ? statement.all(...params)
          : statement.all();
      return rows.map((row) => Object.values(row) as SqlValue[]);
    },
  };

  const client: SqlClient = {
    async exec(sql, params) {
      if (params && params.length > 0) {
        db.prepare(sql).run(...params);
        return [];
      }
      db.exec(sql);
      return [];
    },
    async query(sql, params) {
      const statement = db.prepare(sql);
      const rows =
        params && params.length > 0
          ? statement.all(...params)
          : statement.all();
      return rows.map((row) => Object.values(row) as SqlValue[]);
    },
  };

  return { db, runner, client };
}
