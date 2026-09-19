import { useMemo, useState, type FormEvent } from 'react';

import { EXERCISE_KINDS, filterExercises, type CatalogExercise, type CatalogFilters, type ExerciseKind } from '@/domain/catalog';
import type { CatalogState } from '@/state/useCatalog';

const kindLabels: Record<ExerciseKind, string> = {
  strength: 'Fuerza',
  cardio: 'Cardio',
  mobility: 'Movilidad',
};

const inputClass =
  'w-full rounded-lg border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none';

function Chip({
  active,
  label,
  onClick,
}: {
  active: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full border px-3 py-1 text-xs transition ${
        active
          ? 'border-sky-400 bg-sky-400/20 text-sky-200'
          : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-slate-500'
      }`}
    >
      {label}
    </button>
  );
}

export default function CatalogView({ catalog }: { catalog: CatalogState }) {
  const [filters, setFilters] = useState<CatalogFilters>({
    query: '',
    muscleGroupSlug: null,
    equipment: null,
    kind: null,
  });
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [groupId, setGroupId] = useState('');
  const [equipment, setEquipment] = useState('mancuernas');
  const [kind, setKind] = useState<ExerciseKind>('strength');
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [detailId, setDetailId] = useState<string | null>(null);

  const { snapshot, loading, error, create, remove } = catalog;

  const equipments = useMemo(
    () => [...new Set(snapshot.exercises.map((exercise) => exercise.equipment))].sort(),
    [snapshot.exercises]
  );

  const visible = useMemo(
    () => filterExercises(snapshot.exercises, snapshot.groups, filters),
    [snapshot.exercises, snapshot.groups, filters]
  );

  const groupNameById = useMemo(
    () => new Map(snapshot.groups.map((group) => [group.id, group.name])),
    [snapshot.groups]
  );

  const detailExercise =
    detailId === null
      ? null
      : (snapshot.exercises.find((exercise) => exercise.id === detailId) ?? null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    setBusy(true);
    try {
      await create({ name, muscleGroupId: groupId, equipment, kind });
      setName('');
      setShowForm(false);
    } catch (cause) {
      setFormError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold text-white">Catálogo</h2>
          <p className="text-xs text-slate-400">
            {visible.length} de {snapshot.exercises.length} ejercicios
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            setShowForm((value) => !value);
            setFormError(null);
          }}
          className="rounded-lg bg-sky-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400"
        >
          {showForm ? 'Cerrar' : 'Nuevo propio'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={submit} className="flex flex-col gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4">
          <input
            className={inputClass}
            placeholder="Nombre del ejercicio"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <select className={inputClass} value={groupId} onChange={(event) => setGroupId(event.target.value)}>
              <option value="">Grupo muscular…</option>
              {snapshot.groups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.name}
                </option>
              ))}
            </select>
            <input
              className={inputClass}
              placeholder="Equipamiento"
              value={equipment}
              onChange={(event) => setEquipment(event.target.value)}
            />
            <select
              className={inputClass}
              value={kind}
              onChange={(event) => setKind(event.target.value as ExerciseKind)}
            >
              {EXERCISE_KINDS.map((value) => (
                <option key={value} value={value}>
                  {kindLabels[value]}
                </option>
              ))}
            </select>
          </div>
          {formError && <p className="text-sm text-rose-400">{formError}</p>}
          <button
            type="submit"
            disabled={busy}
            className="self-start rounded-lg bg-sky-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400 disabled:opacity-50"
          >
            {busy ? 'Guardando…' : 'Guardar'}
          </button>
        </form>
      )}

      <input
        className={inputClass}
        placeholder="Buscar ejercicio…"
        value={filters.query}
        onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))}
      />

      <div className="flex flex-wrap gap-2">
        <Chip
          active={filters.muscleGroupSlug === null}
          label="Todos"
          onClick={() => setFilters((current) => ({ ...current, muscleGroupSlug: null }))}
        />
        {snapshot.groups.map((group) => (
          <Chip
            key={group.id}
            active={filters.muscleGroupSlug === group.slug}
            label={group.name}
            onClick={() =>
              setFilters((current) => ({
                ...current,
                muscleGroupSlug: current.muscleGroupSlug === group.slug ? null : group.slug,
              }))
            }
          />
        ))}
      </div>

      <div className="flex flex-wrap gap-2">
        {equipments.map((value) => (
          <Chip
            key={value}
            active={filters.equipment === value}
            label={value}
            onClick={() =>
              setFilters((current) => ({
                ...current,
                equipment: current.equipment === value ? null : value,
              }))
            }
          />
        ))}
        {EXERCISE_KINDS.map((value) => (
          <Chip
            key={value}
            active={filters.kind === value}
            label={kindLabels[value]}
            onClick={() =>
              setFilters((current) => ({
                ...current,
                kind: current.kind === value ? null : value,
              }))
            }
          />
        ))}
      </div>

      {error && <p className="text-sm text-rose-400">{error}</p>}
      {loading && <p className="text-sm text-slate-400">Cargando catálogo…</p>}

      {!loading && visible.length === 0 && (
        <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
          No hay ejercicios que coincidan con la búsqueda.
        </p>
      )}

      <ul className="flex flex-col gap-2">
        {visible.map((exercise) => (
          <li
            key={exercise.id}
            className="rounded-xl border border-slate-700/60 bg-slate-900/40"
          >
            <div className="flex items-center justify-between gap-3 px-4 py-3">
              <button
                type="button"
                onClick={() =>
                  setDetailId((current) => (current === exercise.id ? null : exercise.id))
                }
                className="flex-1 text-left"
              >
                <p className="text-sm font-medium text-slate-100">
                  {exercise.name}
                  {exercise.isCustom && (
                    <span className="ml-2 rounded-full bg-amber-400/20 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-amber-300">
                      Propio
                    </span>
                  )}
                </p>
                <p className="text-xs text-slate-400">
                  {groupNameById.get(exercise.muscleGroupId) ?? 'Sin grupo'} · {exercise.equipment} ·{' '}
                  {kindLabels[exercise.kind]}
                </p>
              </button>
              {exercise.isCustom && (
                <button
                  type="button"
                  onClick={() => {
                    if (window.confirm(`¿Eliminar "${exercise.name}"?`)) {
                      void remove(exercise.id);
                    }
                  }}
                  className="rounded-lg border border-rose-500/40 px-2 py-1 text-xs text-rose-300 hover:bg-rose-500/10"
                >
                  Eliminar
                </button>
              )}
            </div>
            {detailExercise?.id === exercise.id && (
              <ExerciseDetail
                exercise={exercise}
                groupNameById={groupNameById}
                onClose={() => setDetailId(null)}
              />
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

function ExerciseDetail({
  exercise,
  groupNameById,
  onClose,
}: {
  exercise: CatalogExercise;
  groupNameById: Map<string, string>;
  onClose: () => void;
}) {
  const secondaryGroupName =
    exercise.secondaryMuscleGroupId === null
      ? null
      : (groupNameById.get(exercise.secondaryMuscleGroupId) ?? null);

  return (
    <div className="border-t border-slate-700/60 px-4 py-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-200">Detalle</h3>
        <button
          type="button"
          onClick={onClose}
          className="text-xs text-slate-400 hover:text-slate-200"
        >
          Cerrar
        </button>
      </div>
      <dl className="mt-2 grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
        <div>
          <dt className="text-xs text-slate-500">Tipo</dt>
          <dd className="text-slate-100">{kindLabels[exercise.kind]}</dd>
        </div>
        <div>
          <dt className="text-xs text-slate-500">Equipamiento</dt>
          <dd className="text-slate-100">{exercise.equipment}</dd>
        </div>
        <div>
          <dt className="text-xs text-slate-500">Grupo muscular</dt>
          <dd className="text-slate-100">
            {groupNameById.get(exercise.muscleGroupId) ?? 'Sin grupo'}
          </dd>
        </div>
        {secondaryGroupName !== null && (
          <div>
            <dt className="text-xs text-slate-500">Grupo secundario</dt>
            <dd className="text-slate-100">{secondaryGroupName}</dd>
          </div>
        )}
      </dl>
    </div>
  );
}
