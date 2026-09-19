import { drizzle } from 'drizzle-orm/sqlite-proxy';

import { schema } from '@/db/schema';
import type { SqlValue, WorkerRequest, WorkerResponse } from '@/db/worker/protocol';

export interface ReadyInfo {
  readonly schemaVersion: number;
  readonly sqliteVersion: string;
  readonly persistence: 'opfs';
}

interface Pending {
  resolve: (rows: SqlValue[][]) => void;
  reject: (error: Error) => void;
}

export class SqliteWorkerClient {
  private readonly worker: Worker;
  private readonly pending = new Map<number, Pending>();
  private nextId = 1;
  private readySettled = false;
  private readonly readyPromise: Promise<ReadyInfo>;
  private resolveReady: (info: ReadyInfo) => void = () => undefined;
  private rejectReady: (error: Error) => void = () => undefined;

  constructor() {
    this.readyPromise = new Promise<ReadyInfo>((resolve, reject) => {
      this.resolveReady = resolve;
      this.rejectReady = reject;
    });

    this.worker = new Worker(new URL('./worker/sqlite.worker.ts', import.meta.url), {
      type: 'module',
    });
    this.worker.onmessage = (event: MessageEvent<WorkerResponse>) => {
      this.handleMessage(event.data);
    };
    this.worker.onerror = (event: ErrorEvent) => {
      this.failAll(new Error(event.message || 'Error desconocido del worker SQLite'));
    };
  }

  get ready(): Promise<ReadyInfo> {
    return this.readyPromise;
  }

  exec(sql: string, params?: readonly SqlValue[]): Promise<SqlValue[][]> {
    return this.send('exec', sql, params);
  }

  query(sql: string, params?: readonly SqlValue[]): Promise<SqlValue[][]> {
    return this.send('query', sql, params);
  }

  terminate(): void {
    this.worker.terminate();
  }

  private send(
    kind: 'exec' | 'query',
    sql: string,
    params?: readonly SqlValue[]
  ): Promise<SqlValue[][]> {
    const id = this.nextId;
    this.nextId += 1;
    return new Promise<SqlValue[][]>((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      const message: WorkerRequest = params
        ? { id, kind, sql, params }
        : { id, kind, sql };
      this.worker.postMessage(message);
    });
  }

  private handleMessage(message: WorkerResponse): void {
    if (message.kind === 'ready') {
      if (!this.readySettled) {
        this.readySettled = true;
        this.resolveReady({
          schemaVersion: message.schemaVersion,
          sqliteVersion: message.sqliteVersion,
          persistence: message.persistence,
        });
      }
      return;
    }
    const pending = this.pending.get(message.id);
    if (!pending) {
      return;
    }
    this.pending.delete(message.id);
    if (message.ok) {
      pending.resolve(message.rows);
    } else {
      pending.reject(new Error(message.error));
    }
  }

  private failAll(error: Error): void {
    if (!this.readySettled) {
      this.readySettled = true;
      this.rejectReady(error);
    }
    for (const pending of this.pending.values()) {
      pending.reject(error);
    }
    this.pending.clear();
  }
}

export function createFitLogDb(client: SqliteWorkerClient) {
  return drizzle(
    async (sql, params, method) => {
      const values = params as SqlValue[];
      if (method === 'run') {
        await client.exec(sql, values);
        return { rows: [] };
      }
      const rows = await client.query(sql, values);
      if (method === 'get') {
        return { rows: rows[0] ?? [] };
      }
      return { rows };
    },
    { schema }
  );
}

export type FitLogDb = ReturnType<typeof createFitLogDb>;

export function supportsOpfs(): boolean {
  return typeof navigator !== 'undefined' && 'storage' in navigator && 'getDirectory' in navigator.storage;
}
