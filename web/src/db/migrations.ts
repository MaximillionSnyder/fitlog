import esquemaInicial from '@shared/schema/migrations/001_esquema_inicial.sql?raw';

export interface Migration {
  readonly version: number;
  readonly name: string;
  readonly sql: string;
}

export const migrations: readonly Migration[] = [
  {
    version: 1,
    name: '001_esquema_inicial',
    sql: esquemaInicial,
  },
];
