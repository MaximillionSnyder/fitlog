import { describe, expect, it } from 'vitest';

import {
  BackupError,
  emptySnapshot,
  mergeBackup,
  parseBackup,
  serializeBackup,
  type BackupFile,
  type BackupSnapshot,
  type TableName,
} from '@/domain/backup';

import vectors from '@shared/test-vectors/backup-merge.json';

interface VectorCase {
  name: string;
  local: BackupSnapshot;
  backup: BackupFile;
  expected: {
    summary: Record<string, { inserted: number; updated: number; ignored: number; remapped: number }>;
    snapshot: BackupSnapshot;
  };
}

interface InvalidCase {
  name: string;
  local: BackupSnapshot;
  json: string;
  expected_error: string;
}

const cases = vectors.cases as VectorCase[];
const invalidCases = vectors.invalid_cases as InvalidCase[];

describe('mergeBackup', () => {
  it.each(cases)('$name', (testCase) => {
    const result = mergeBackup(testCase.local, testCase.backup);

    for (const table of Object.keys(testCase.expected.summary) as TableName[]) {
      const expectedSummary = testCase.expected.summary[table];
      expect(result.summary[table]).toEqual(expectedSummary);
    }

    for (const table of Object.keys(testCase.expected.snapshot) as TableName[]) {
      expect(result.snapshot[table]).toEqual(testCase.expected.snapshot[table]);
    }
  });
});

describe('parseBackup y validación', () => {
  it.each(invalidCases)('$name', (testCase) => {
    let code = 'sin-error';
    try {
      mergeBackup(testCase.local, parseBackup(testCase.json));
    } catch (cause) {
      code = cause instanceof BackupError ? cause.code : 'desconocido';
    }
    expect(code).toBe(testCase.expected_error);
  });

  it('rechaza un archivo sin formato de FitLog', () => {
    expect(() => parseBackup('{"format":"otro"}')).toThrow(BackupError);
  });
});

describe('serializeBackup', () => {
  const snapshot: BackupSnapshot = {
    ...emptySnapshot(),
    exercise: [
      {
        id: 'ex-custom',
        slug: 'remo-maquina',
        name: 'Remo en máquina',
        muscle_group_id: 'mg-espalda',
        is_custom: 1,
        updated_at: 10,
        deleted_at: null,
      },
      {
        id: 'ex-base',
        slug: 'press-banca-barra',
        name: 'Press banca con barra',
        muscle_group_id: 'mg-pecho',
        is_custom: 0,
        updated_at: 10,
        deleted_at: null,
      },
    ],
    routine: [{ id: 'r1', name: 'Empuje', updated_at: 10, deleted_at: null }],
    routine_exercise: [
      { id: 're1', routine_id: 'r1', exercise_id: 'ex-custom', position: 1, updated_at: 10, deleted_at: null },
    ],
    session: [{ id: 's1', started_at: 1000, updated_at: 10, deleted_at: null }],
    set_entry: [
      { id: 'set1', session_id: 's1', exercise_id: 'ex-base', set_index: 1, weight_kg: 100, reps: 8, updated_at: 10, deleted_at: null },
    ],
    body_metric: [
      { id: 'm1', measured_at: 1000, kind: 'body_weight', value: 80, unit: 'kg', updated_at: 10, deleted_at: null },
    ],
    app_setting: [{ key: 'tema', value: 'oscuro', updated_at: 10 }],
  };

  it('exporta solo las secciones pedidas y los ejercicios propios referenciados', () => {
    const json = serializeBackup(snapshot, {
      sections: ['session', 'set_entry'],
      appVersion: '0.1.7',
      nowMs: 5000,
    });
    const file = parseBackup(json);

    expect(file.sections.session).toHaveLength(1);
    expect(file.sections.set_entry).toHaveLength(1);
    expect(file.sections.routine).toBeUndefined();
    expect(file.sections.body_metric).toBeUndefined();
    // El ejercicio base no viaja aunque esté referenciado
    expect(file.sections.exercise ?? []).toHaveLength(0);
  });

  it('auto-incluye ejercicios propios referenciados por las rutinas', () => {
    const json = serializeBackup(snapshot, {
      sections: ['routine', 'routine_exercise'],
      appVersion: '0.1.7',
      nowMs: 5000,
    });
    const file = parseBackup(json);

    expect(file.sections.exercise?.map((row) => row.id)).toEqual(['ex-custom']);
    expect(file.sections.routine).toHaveLength(1);
    expect(file.sections.routine_exercise).toHaveLength(1);
  });

  it('la sección de ajustes viaja cuando se pide', () => {
    const json = serializeBackup(snapshot, {
      sections: ['app_setting'],
      appVersion: '0.1.7',
      nowMs: 5000,
    });
    const file = parseBackup(json);

    expect(file.sections.app_setting).toHaveLength(1);
    expect(file.format).toBe('fitlog-backup');
    expect(file.format_version).toBe(1);
  });
});
