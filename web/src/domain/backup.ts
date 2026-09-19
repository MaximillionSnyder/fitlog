export const BACKUP_FORMAT = 'fitlog-backup';
export const BACKUP_FORMAT_VERSION = 1;

export type BackupValue = string | number | null;
export type BackupRow = Record<string, BackupValue>;

export type TableName =
  | 'exercise'
  | 'routine'
  | 'routine_exercise'
  | 'session'
  | 'set_entry'
  | 'body_metric'
  | 'app_setting';

export const TABLES: readonly TableName[] = [
  'exercise',
  'routine',
  'routine_exercise',
  'session',
  'set_entry',
  'body_metric',
  'app_setting',
];

export interface BackupFile {
  readonly format: string;
  readonly format_version: number;
  readonly exported_at_ms: number;
  readonly app_version: string;
  readonly sections: Partial<Record<TableName, BackupRow[]>>;
}

export interface BackupSnapshot {
  exercise: BackupRow[];
  routine: BackupRow[];
  routine_exercise: BackupRow[];
  session: BackupRow[];
  set_entry: BackupRow[];
  body_metric: BackupRow[];
  app_setting: BackupRow[];
}

export interface SectionSummary {
  readonly inserted: number;
  readonly updated: number;
  readonly ignored: number;
  readonly remapped: number;
}

export type MergeSummary = Record<TableName, SectionSummary>;

export interface MergeResult {
  readonly snapshot: BackupSnapshot;
  readonly summary: MergeSummary;
}

export type BackupErrorCode =
  | 'invalid_json'
  | 'unknown_format'
  | 'unsupported_version'
  | 'invalid_row'
  | 'missing_reference';

export class BackupError extends Error {
  readonly code: BackupErrorCode;
  readonly detail: string;

  constructor(code: BackupErrorCode, detail: string) {
    super(`${code}: ${detail}`);
    this.name = 'BackupError';
    this.code = code;
    this.detail = detail;
  }
}

interface TableSpec {
  readonly name: TableName;
  readonly pk: string;
  readonly required: readonly string[];
}

const TABLE_SPECS: Record<TableName, TableSpec> = {
  exercise: {
    name: 'exercise',
    pk: 'id',
    required: ['id', 'slug', 'name', 'muscle_group_id', 'is_custom', 'updated_at'],
  },
  routine: { name: 'routine', pk: 'id', required: ['id', 'name', 'updated_at'] },
  routine_exercise: {
    name: 'routine_exercise',
    pk: 'id',
    required: ['id', 'routine_id', 'exercise_id', 'position', 'updated_at'],
  },
  session: { name: 'session', pk: 'id', required: ['id', 'started_at', 'updated_at'] },
  set_entry: {
    name: 'set_entry',
    pk: 'id',
    required: ['id', 'session_id', 'exercise_id', 'set_index', 'updated_at'],
  },
  body_metric: {
    name: 'body_metric',
    pk: 'id',
    required: ['id', 'measured_at', 'kind', 'value', 'unit', 'updated_at'],
  },
  app_setting: { name: 'app_setting', pk: 'key', required: ['key', 'value', 'updated_at'] },
};

interface ReferenceSpec {
  readonly table: TableName;
  readonly column: string;
  readonly target: TableName;
}

const REFERENCES: readonly ReferenceSpec[] = [
  { table: 'exercise', column: 'muscle_group_id', target: 'exercise' },
  { table: 'routine_exercise', column: 'routine_id', target: 'routine' },
  { table: 'routine_exercise', column: 'exercise_id', target: 'exercise' },
  { table: 'set_entry', column: 'session_id', target: 'session' },
  { table: 'set_entry', column: 'exercise_id', target: 'exercise' },
];

export function emptySnapshot(): BackupSnapshot {
  return {
    exercise: [],
    routine: [],
    routine_exercise: [],
    session: [],
    set_entry: [],
    body_metric: [],
    app_setting: [],
  };
}

export function parseBackup(json: string): BackupFile {
  let parsed: unknown;
  try {
    parsed = JSON.parse(json);
  } catch {
    throw new BackupError('invalid_json', 'El archivo no es JSON válido');
  }

  if (typeof parsed !== 'object' || parsed === null) {
    throw new BackupError('invalid_json', 'El archivo no contiene un objeto JSON');
  }

  const candidate = parsed as Partial<BackupFile>;
  if (candidate.format !== BACKUP_FORMAT) {
    throw new BackupError(
      'unknown_format',
      `Se esperaba el formato ${BACKUP_FORMAT} y llegó ${String(candidate.format)}`
    );
  }
  if (typeof candidate.format_version !== 'number') {
    throw new BackupError('invalid_json', 'Falta format_version');
  }
  if (candidate.format_version > BACKUP_FORMAT_VERSION) {
    throw new BackupError(
      'unsupported_version',
      `El archivo es de la versión ${candidate.format_version} y esta app soporta hasta la ${BACKUP_FORMAT_VERSION}`
    );
  }

  const sections: Partial<Record<TableName, BackupRow[]>> = {};
  for (const table of TABLES) {
    const rows = candidate.sections?.[table];
    if (rows === undefined) continue;
    if (!Array.isArray(rows)) {
      throw new BackupError('invalid_json', `La sección ${table} no es una lista`);
    }
    sections[table] = rows as BackupRow[];
  }

  return {
    format: candidate.format,
    format_version: candidate.format_version,
    exported_at_ms: typeof candidate.exported_at_ms === 'number' ? candidate.exported_at_ms : 0,
    app_version: typeof candidate.app_version === 'string' ? candidate.app_version : '',
    sections,
  };
}

function requiredFields(table: TableName): readonly string[] {
  return TABLE_SPECS[table].required;
}

function primaryKey(table: TableName): string {
  return TABLE_SPECS[table].pk;
}

function isPresent(row: BackupRow, field: string): boolean {
  return Object.prototype.hasOwnProperty.call(row, field) && row[field] !== undefined;
}

export function validateBackup(file: BackupFile, local: BackupSnapshot): void {
  for (const table of TABLES) {
    const rows = file.sections[table];
    if (!rows) continue;
    for (const row of rows) {
      for (const field of requiredFields(table)) {
        if (!isPresent(row, field)) {
          const identifier = String(row[primaryKey(table)] ?? '?');
          throw new BackupError(
            'invalid_row',
            `La fila ${identifier} de ${table} no tiene el campo ${field}`
          );
        }
      }
    }
  }

  const known = new Map<string, Set<string>>();
  for (const table of TABLES) {
    const ids = new Set<string>();
    for (const row of local[table]) {
      ids.add(String(row[primaryKey(table)]));
    }
    for (const row of file.sections[table] ?? []) {
      ids.add(String(row[primaryKey(table)]));
    }
    known.set(table, ids);
  }

  const localGroups = new Set(local.exercise.map((row) => String(row.muscle_group_id)));
  const fileGroups = new Set(
    (file.sections.exercise ?? []).map((row) => String(row.muscle_group_id))
  );

  for (const reference of REFERENCES) {
    const rows = file.sections[reference.table] ?? [];
    for (const row of rows) {
      const value = row[reference.column];
      if (value === null || value === undefined) continue;
      const key = String(value);

      if (reference.table === 'exercise' && reference.column === 'muscle_group_id') {
        if (!localGroups.has(key) && !fileGroups.has(key)) {
          throw new BackupError(
            'missing_reference',
            `El ejercicio ${String(row.id)} referencia el grupo muscular ${key}, que no existe en la base`
          );
        }
        continue;
      }

      const candidates = known.get(reference.target);
      if (!candidates?.has(key)) {
        throw new BackupError(
          'missing_reference',
          `La fila ${String(row[primaryKey(reference.table)])} de ${reference.table} referencia ${reference.target} ${key}, que no está en el archivo ni en la base`
        );
      }
    }
  }
}

function updatedAt(row: BackupRow): number {
  const value = row.updated_at;
  return typeof value === 'number' ? value : 0;
}

function mergeByPrimaryKey(
  localRows: readonly BackupRow[],
  importedRows: readonly BackupRow[],
  pk: string
): { rows: BackupRow[]; inserted: number; updated: number; ignored: number } {
  const byId = new Map<string, BackupRow>();
  for (const row of localRows) {
    byId.set(String(row[pk]), row);
  }

  let inserted = 0;
  let updated = 0;
  let ignored = 0;

  for (const imported of importedRows) {
    const key = String(imported[pk]);
    const existing = byId.get(key);
    if (!existing) {
      byId.set(key, imported);
      inserted += 1;
      continue;
    }
    if (updatedAt(imported) > updatedAt(existing)) {
      byId.set(key, imported);
      updated += 1;
    } else {
      ignored += 1;
    }
  }

  const rows = [...byId.values()].sort((a, b) => {
    const keyA = String(a[pk]);
    const keyB = String(b[pk]);
    return keyA < keyB ? -1 : keyA > keyB ? 1 : 0;
  });
  return { rows, inserted, updated, ignored };
}

function remapRow(row: BackupRow, column: string, idMap: Map<string, string>): BackupRow {
  const value = row[column];
  if (value === null || value === undefined) return row;
  const mapped = idMap.get(String(value));
  if (mapped === undefined) return row;
  return { ...row, [column]: mapped };
}

export function mergeBackup(local: BackupSnapshot, file: BackupFile): MergeResult {
  validateBackup(file, local);

  const snapshot: BackupSnapshot = {
    exercise: [...local.exercise],
    routine: [...local.routine],
    routine_exercise: [...local.routine_exercise],
    session: [...local.session],
    set_entry: [...local.set_entry],
    body_metric: [...local.body_metric],
    app_setting: [...local.app_setting],
  };
  const summary = {} as MergeSummary;
  for (const table of TABLES) {
    summary[table] = { inserted: 0, updated: 0, ignored: 0, remapped: 0 };
  }

  const idMap = new Map<string, string>();
  const localExerciseById = new Map(snapshot.exercise.map((row) => [String(row.id), row]));
  const localExerciseBySlug = new Map(snapshot.exercise.map((row) => [String(row.slug), row]));

  const importedExercises: BackupRow[] = [];
  for (const imported of file.sections.exercise ?? []) {
    const id = String(imported.id);
    const existingById = localExerciseById.get(id);
    if (existingById) {
      importedExercises.push(imported);
      continue;
    }
    const existingBySlug = localExerciseBySlug.get(String(imported.slug));
    if (existingBySlug) {
      idMap.set(id, String(existingBySlug.id));
      summary.exercise = { ...summary.exercise, remapped: summary.exercise.remapped + 1 };
      continue;
    }
    importedExercises.push(imported);
  }

  const exerciseMerge = mergeByPrimaryKey(
    snapshot.exercise,
    importedExercises,
    primaryKey('exercise')
  );
  snapshot.exercise = exerciseMerge.rows;
  summary.exercise = {
    ...summary.exercise,
    inserted: exerciseMerge.inserted,
    updated: exerciseMerge.updated,
    ignored: exerciseMerge.ignored,
  };

  for (const table of TABLES) {
    if (table === 'exercise') continue;
    const importedRows = (file.sections[table] ?? []).map((row) => {
      if (table === 'routine_exercise' || table === 'set_entry') {
        return remapRow(row, 'exercise_id', idMap);
      }
      return row;
    });

    const merged = mergeByPrimaryKey(snapshot[table], importedRows, primaryKey(table));
    snapshot[table] = merged.rows;
    summary[table] = {
      inserted: merged.inserted,
      updated: merged.updated,
      ignored: merged.ignored,
      remapped: 0,
    };
  }

  return { snapshot, summary };
}

export interface ExportOptions {
  readonly sections: readonly TableName[];
  readonly appVersion: string;
  readonly nowMs: number;
}

export function serializeBackup(
  snapshot: BackupSnapshot,
  options: ExportOptions
): string {
  const requested = new Set<TableName>(options.sections);

  const sets = requested.has('set_entry') ? snapshot.set_entry : [];
  const routineItems = requested.has('routine_exercise') ? snapshot.routine_exercise : [];

  const referencedExerciseIds = new Set<string>();
  for (const set of sets) {
    referencedExerciseIds.add(String(set.exercise_id));
  }
  for (const item of routineItems) {
    referencedExerciseIds.add(String(item.exercise_id));
  }

  const exercises = snapshot.exercise.filter(
    (row) =>
      Number(row.is_custom) === 1 &&
      (requested.has('exercise') || referencedExerciseIds.has(String(row.id)))
  );

  const sections: Partial<Record<TableName, BackupRow[]>> = {};
  if (exercises.length > 0) sections.exercise = exercises;
  if (requested.has('routine')) sections.routine = snapshot.routine;
  if (routineItems.length > 0) sections.routine_exercise = routineItems;
  if (requested.has('session')) sections.session = snapshot.session;
  if (sets.length > 0) sections.set_entry = sets;
  if (requested.has('body_metric')) sections.body_metric = snapshot.body_metric;
  if (requested.has('app_setting')) sections.app_setting = snapshot.app_setting;

  const file: BackupFile = {
    format: BACKUP_FORMAT,
    format_version: BACKUP_FORMAT_VERSION,
    exported_at_ms: options.nowMs,
    app_version: options.appVersion,
    sections,
  };

  return JSON.stringify(file, null, 2);
}

export const UI_GROUPS: readonly { readonly id: string; readonly label: string; readonly tables: readonly TableName[] }[] = [
  { id: 'custom_exercises', label: 'Ejercicios propios', tables: ['exercise'] },
  { id: 'routines', label: 'Rutinas', tables: ['routine', 'routine_exercise'] },
  { id: 'workouts', label: 'Entrenamientos', tables: ['session', 'set_entry'] },
  { id: 'body_metrics', label: 'Medidas', tables: ['body_metric'] },
  { id: 'settings', label: 'Ajustes', tables: ['app_setting'] },
];
