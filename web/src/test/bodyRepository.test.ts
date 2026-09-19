import { describe, expect, it } from 'vitest';

import { createFitLogDb } from '@/db/client';
import { applyMigrations } from '@/db/migrate';
import { migrations } from '@/db/migrations';
import {
  BodyMetricError,
  createBodyMetric,
  deleteBodyMetric,
  listBodyMetrics,
  updateBodyMetric,
} from '@/data/body';
import { buildMetricSeries, buildMetricStats } from '@/domain/body';

import { createNodeSqliteHarness } from './helpers/nodeSqlite';

type Db = ReturnType<typeof createFitLogDb>;

function createDatabase(): Db {
  const harness = createNodeSqliteHarness();
  applyMigrations(harness.runner, migrations);
  return createFitLogDb(harness.client);
}

describe('repositorio de medidas', () => {
  it('registra una medida con unidad derivada y notas limpias', async () => {
    const db = createDatabase();
    const created = await createBodyMetric(
      db,
      { kind: 'body_weight', value: 78.5, measuredAtMs: 1000, notes: '  en ayunas ' },
      () => 2000
    );

    expect(created.unit).toBe('kg');
    expect(created.value).toBe(78.5);
    expect(created.notes).toBe('en ayunas');

    const all = await listBodyMetrics(db);
    expect(all).toHaveLength(1);
    expect(all[0]?.id).toBe(created.id);
  });

  it('rechaza valores invalidos y tipos desconocidos', async () => {
    const db = createDatabase();

    await expect(
      createBodyMetric(db, { kind: 'body_weight', value: 0, measuredAtMs: 1, notes: null })
    ).rejects.toBeInstanceOf(BodyMetricError);

    await expect(
      createBodyMetric(db, { kind: 'body_fat', value: 120, measuredAtMs: 1, notes: null })
    ).rejects.toBeInstanceOf(BodyMetricError);

    await expect(
      createBodyMetric(db, { kind: 'altura', value: 180, measuredAtMs: 1, notes: null })
    ).rejects.toBeInstanceOf(BodyMetricError);

    expect(await listBodyMetrics(db)).toHaveLength(0);
  });

  it('edita y elimina con borrado logico', async () => {
    const db = createDatabase();
    const created = await createBodyMetric(
      db,
      { kind: 'waist', value: 84, measuredAtMs: 1000, notes: null },
      () => 2000
    );

    await updateBodyMetric(db, created.id, { value: 83.5, measuredAtMs: 3000, notes: 'mejor' }, () => 4000);

    let all = await listBodyMetrics(db);
    expect(all[0]?.value).toBe(83.5);
    expect(all[0]?.notes).toBe('mejor');
    expect(all[0]?.measuredAtMs).toBe(3000);

    await deleteBodyMetric(db, created.id, () => 5000);
    all = await listBodyMetrics(db);
    expect(all).toHaveLength(0);
  });

  it('valida contra el tipo guardado al editar', async () => {
    const db = createDatabase();
    const created = await createBodyMetric(
      db,
      { kind: 'body_fat', value: 18, measuredAtMs: 1000, notes: null },
      () => 2000
    );

    await expect(
      updateBodyMetric(db, created.id, { value: 150, measuredAtMs: 1000, notes: null })
    ).rejects.toBeInstanceOf(BodyMetricError);
  });

  it('calcula series y estadisticas del periodo con datos reales', async () => {
    const db = createDatabase();
    const day = 86_400_000;
    const now = 1_700_000_000_000;

    await createBodyMetric(db, { kind: 'body_weight', value: 80, measuredAtMs: now - 40 * day, notes: null }, () => 1);
    await createBodyMetric(db, { kind: 'body_weight', value: 79, measuredAtMs: now - 20 * day, notes: null }, () => 2);
    await createBodyMetric(db, { kind: 'body_weight', value: 78, measuredAtMs: now - 2 * day, notes: null }, () => 3);
    await createBodyMetric(db, { kind: 'waist', value: 84, measuredAtMs: now - 2 * day, notes: null }, () => 4);

    const all = await listBodyMetrics(db);
    const series = buildMetricSeries(all, 'body_weight', { fromMs: now - 90 * day, toMs: now });
    const stats = buildMetricStats(series);

    expect(series.map((point) => point.value)).toEqual([80, 79, 78]);
    expect(stats.count).toBe(3);
    expect(stats.deltaAbs).toBe(-2);
    expect(stats.deltaPct).toBe(-2.5);
  });
});
