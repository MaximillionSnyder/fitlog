export type SqlValue = string | number | null;

export interface ExecRequest {
  readonly id: number;
  readonly kind: 'exec';
  readonly sql: string;
  readonly params?: readonly SqlValue[];
}

export interface QueryRequest {
  readonly id: number;
  readonly kind: 'query';
  readonly sql: string;
  readonly params?: readonly SqlValue[];
}

export type WorkerRequest = ExecRequest | QueryRequest;

export interface ReadyMessage {
  readonly id: 0;
  readonly ok: true;
  readonly kind: 'ready';
  readonly schemaVersion: number;
  readonly persistence: 'opfs';
  readonly sqliteVersion: string;
  readonly catalogSeeded: boolean;
  readonly catalogExercises: number;
}

export interface SuccessMessage {
  readonly id: number;
  readonly ok: true;
  readonly kind: 'result';
  readonly rows: SqlValue[][];
}

export interface ErrorMessage {
  readonly id: number;
  readonly ok: false;
  readonly kind: 'error';
  readonly error: string;
}

export type WorkerResponse = ReadyMessage | SuccessMessage | ErrorMessage;
