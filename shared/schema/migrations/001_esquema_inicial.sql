-- FitLog - migracion 001: esquema inicial v1
-- Fuente de verdad estructural compartida por Android (Room) y Web (SQLite WASM).
-- Reglas: id TEXT ULID (26), timestamps epoch ms UTC, unidades kg/cm.

CREATE TABLE IF NOT EXISTS schema_migration (
    version    INTEGER NOT NULL PRIMARY KEY,
    name       TEXT    NOT NULL,
    applied_at INTEGER NOT NULL
);

CREATE TABLE muscle_group (
    id         TEXT    NOT NULL PRIMARY KEY,
    slug       TEXT    NOT NULL UNIQUE,
    name       TEXT    NOT NULL,
    body_region TEXT   NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    deleted_at INTEGER
);

CREATE TABLE exercise (
    id                        TEXT    NOT NULL PRIMARY KEY,
    slug                      TEXT    NOT NULL UNIQUE,
    name                      TEXT    NOT NULL,
    muscle_group_id           TEXT    NOT NULL REFERENCES muscle_group (id),
    secondary_muscle_group_id TEXT    REFERENCES muscle_group (id),
    equipment                 TEXT    NOT NULL,
    kind                      TEXT    NOT NULL CHECK (kind IN ('strength', 'cardio', 'mobility')),
    is_custom                 INTEGER NOT NULL DEFAULT 0 CHECK (is_custom IN (0, 1)),
    created_at                INTEGER NOT NULL,
    updated_at                INTEGER NOT NULL,
    deleted_at                INTEGER
);

CREATE INDEX idx_exercise_muscle_group ON exercise (muscle_group_id);

CREATE TABLE routine (
    id          TEXT    NOT NULL PRIMARY KEY,
    name        TEXT    NOT NULL,
    description TEXT,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    deleted_at  INTEGER
);

CREATE TABLE routine_exercise (
    id             TEXT    NOT NULL PRIMARY KEY,
    routine_id     TEXT    NOT NULL REFERENCES routine (id),
    exercise_id    TEXT    NOT NULL REFERENCES exercise (id),
    position       INTEGER NOT NULL,
    target_sets    INTEGER,
    target_reps    INTEGER,
    target_weight_kg REAL,
    rest_seconds   INTEGER,
    notes          TEXT,
    created_at     INTEGER NOT NULL,
    updated_at     INTEGER NOT NULL,
    deleted_at     INTEGER,
    UNIQUE (routine_id, position)
);

CREATE INDEX idx_routine_exercise_routine ON routine_exercise (routine_id);

CREATE TABLE session (
    id          TEXT    NOT NULL PRIMARY KEY,
    routine_id  TEXT    REFERENCES routine (id),
    started_at  INTEGER NOT NULL,
    finished_at INTEGER,
    notes       TEXT,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    deleted_at  INTEGER
);

CREATE INDEX idx_session_started_at ON session (started_at);

CREATE TABLE set_entry (
    id          TEXT    NOT NULL PRIMARY KEY,
    session_id  TEXT    NOT NULL REFERENCES session (id),
    exercise_id TEXT    NOT NULL REFERENCES exercise (id),
    set_index   INTEGER NOT NULL,
    weight_kg   REAL,
    reps        INTEGER,
    rir         INTEGER,
    rpe         REAL,
    is_warmup   INTEGER NOT NULL DEFAULT 0 CHECK (is_warmup IN (0, 1)),
    notes       TEXT,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    deleted_at  INTEGER
);

CREATE INDEX idx_set_entry_session ON set_entry (session_id);
CREATE INDEX idx_set_entry_exercise ON set_entry (exercise_id);

CREATE TABLE body_metric (
    id          TEXT    NOT NULL PRIMARY KEY,
    measured_at INTEGER NOT NULL,
    kind        TEXT    NOT NULL CHECK (kind IN ('body_weight', 'body_fat', 'waist', 'chest', 'arm', 'thigh', 'hip', 'neck', 'other')),
    value       REAL    NOT NULL,
    unit        TEXT    NOT NULL CHECK (unit IN ('kg', 'cm', '%')),
    notes       TEXT,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    deleted_at  INTEGER
);

CREATE INDEX idx_body_metric_kind_time ON body_metric (kind, measured_at);

CREATE TABLE app_setting (
    key        TEXT    NOT NULL PRIMARY KEY,
    value      TEXT    NOT NULL,
    updated_at INTEGER NOT NULL
);
