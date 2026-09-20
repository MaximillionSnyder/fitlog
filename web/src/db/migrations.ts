import esquemaInicial from '@shared/schema/migrations/001_esquema_inicial.sql?raw';
import actividadImportada from '@shared/schema/migrations/002_actividad_importada.sql?raw';

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
  {
    version: 2,
    name: '002_actividad_importada',
    sql: actividadImportada,
  },
];
