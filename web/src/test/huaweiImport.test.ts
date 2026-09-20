import { describe, expect, it } from 'vitest';

import { createFitLogDb } from '@/db/client';
import { applyMigrations } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import { seedCatalog } from '@/data/catalogSeed';
import { importSessions, listSessions } from '@/data/workout';
import { huaweiNote, parseHuaweiExport } from '@/domain/huaweiHealth';
import catalogSeed from '@shared/seed/catalog.json';

import { createNodeSqliteHarness } from './helpers/nodeSqlite';

type Db = ReturnType<typeof createFitLogDb>;

function createDatabase(): Db {
  const harness = createNodeSqliteHarness();
  applyMigrations(harness.runner, migrations);
  seedCatalog(harness.runner, catalogSeed, () => 1);
  return createFitLogDb(harness.client);
}

const start = 1_690_000_000_000;
const day = 86_400_000;

function activity(
  recordId: string,
  startedAt: number,
  sportType: number,
  extra: Record<string, unknown> = {}
): string {
  return JSON.stringify({
    recordId,
    startTime: startedAt,
    endTime: startedAt + 2_700_000,
    sportType,
    totalTime: 2_700_000,
    totalSteps: 4_200,
    ...extra,
  });
}

/**
 * Exportación de prueba: tres entrenamientos repartidos en dos archivos con registros repetidos,
 * más archivos de sueño y de pasos por minuto que no son entrenamientos.
 */
function exportFiles(): string[] {
  const running = activity('run-1', start, 4, {
    totalDistance: 5_240,
    totalCalories: 320_000,
    attribute: 'tp=lbs;k=1;lat=-34.6;lon=-58.3;tp=h-r;k=0;v=120;k=1;v=150;k=2;v=170;',
  });
  const cycling = activity('bike-1', start + day, 3, {
    totalDistance: 20_000,
    totalCalories: 600_000,
  });
  const strength = activity('gym-1', start + 2 * day, 147);

  return [
    `[${running}, ${cycling}]`,
    `[${running}, ${strength}]`,
    `{"sleepRecords": [{"startTime": ${start + 3 * day}, "endTime": ${start + 3 * day + 28_800_000}, "deepSleep": 90}]}`,
    `{"sportPerMinute": [{"startTime": ${start + 4 * day}, "steps": 120, "calories": 8}]}`,
  ];
}

async function importExport(db: Db): Promise<{ imported: number; skipped: number }> {
  const parsed = parseHuaweiExport(exportFiles());
  return importSessions(
    db,
    parsed.workouts.map((workout) => ({
      startedAtMs: workout.startedAtMs,
      finishedAtMs: workout.finishedAtMs,
      notes: huaweiNote(workout),
    }))
  );
}

describe('importación completa de una exportación', () => {
  it('deja el historial con las tres sesiones y sus notas', async () => {
    const db = createDatabase();
    const result = await importExport(db);

    expect(result).toEqual({ imported: 3, skipped: 0 });

    const sessions = await listSessions(db);
    expect(sessions).toHaveLength(3);
    // El historial viene del más nuevo al más viejo.
    expect(sessions[0]!.notes).toBe('Huawei Health · Entrenamiento de fuerza · 4200 pasos');
    expect(sessions[1]!.notes).toContain('Bicicleta');
    expect(sessions[2]!.notes).toContain('Running');
    // La frecuencia cardíaca no venía en el resumen: se deriva del blob de sensores.
    expect(sessions[2]!.notes).toContain('FC 147/170');
    // Una sesión importada no tiene series: Huawei no exporta peso ni reps.
    expect(sessions[2]!.summary.workingSets).toBe(0);
    expect(sessions[2]!.summary.totalVolumeKg).toBe(0);
  });

  it('reimportar la misma exportación no agrega nada', async () => {
    const db = createDatabase();
    await importExport(db);
    const second = await importExport(db);

    expect(second).toEqual({ imported: 0, skipped: 3 });
    expect(await listSessions(db)).toHaveLength(3);
  });

  it('una exportación posterior solo agrega lo nuevo', async () => {
    const db = createDatabase();
    await importExport(db);

    const nuevo = parseHuaweiExport([activity('run-2', 1_695_000_000_000, 4)]);
    const result = await importSessions(
      db,
      nuevo.workouts.map((workout) => ({
        startedAtMs: workout.startedAtMs,
        finishedAtMs: workout.finishedAtMs,
        notes: huaweiNote(workout),
      }))
    );

    expect(result.imported).toBe(1);
    expect(await listSessions(db)).toHaveLength(4);
  });
});
