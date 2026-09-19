import { useMemo, useState, type FormEvent } from 'react';

import {
  EXERCISE_KINDS,
  filterExercises,
  type CatalogExercise,
  type CatalogFilters,
  type ExerciseKind,
} from '@/domain/catalog';
import { formatInteger } from '@/domain/format';
import type { CatalogState } from '@/state/useCatalog';
import { IconChevronRight, IconDumbbell, IconPlus, IconSearch } from '@/ui/icons';
import {
  Button,
  Card,
  Chip,
  EmptyState,
  ErrorState,
  LabeledValue,
  LoadingState,
  SectionHeader,
} from '@/ui/primitives';

const kindLabels: Record<ExerciseKind, string> = {
  strength: 'Fuerza',
  cardio: 'Cardio',
  mobility: 'Movilidad',
};

const inputClass =
  'w-full rounded-field border border-line bg-surface-low px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-accent focus:outline-none';

/**
 * Catalogo de ejercicios: busqueda por texto, filtros por chips (grupo muscular, equipamiento y
 * tipo), alta de ejercicios propios y detalle expandible de cada fila.
 */
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

  const { snapshot, loading, error, create, remove, reload } = catalog;

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

  const total = snapshot.exercises.length;

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
      <div className="flex flex-wrap items-center justify-between gap-3">
        <SectionHeader title="Ejercicios" className="flex-1" />
        <Button
          variant={showForm ? 'secondary' : 'primary'}
          icon={showForm ? undefined : <IconPlus className="size-4" />}
          onClick={() => {
            setShowForm((value) => !value);
            setFormError(null);
          }}
        >
          {showForm ? 'Cerrar' : 'Nuevo propio'}
        </Button>
      </div>

      {showForm ? (
        <Card>
          <form onSubmit={submit} className="flex flex-col gap-3">
            <input
              className={inputClass}
              placeholder="Nombre del ejercicio"
              aria-label="Nombre del ejercicio"
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <select
                className={inputClass}
                aria-label="Grupo muscular"
                value={groupId}
                onChange={(event) => setGroupId(event.target.value)}
              >
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
                aria-label="Equipamiento"
                value={equipment}
                onChange={(event) => setEquipment(event.target.value)}
              />
              <select
                className={inputClass}
                aria-label="Tipo de ejercicio"
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
            {formError ? <p className="text-danger text-sm">{formError}</p> : null}
            <Button
              type="submit"
              disabled={busy}
              className="self-start"
              icon={<IconPlus className="size-4" />}
            >
              {busy ? 'Guardando…' : 'Guardar'}
            </Button>
          </form>
        </Card>
      ) : null}

      <Card className="!p-4">
        <div className="flex flex-col gap-3">
          <div className="relative">
            <IconSearch className="text-faint pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
            <input
              className={`${inputClass} pl-9`}
              placeholder="Buscar ejercicio…"
              aria-label="Buscar ejercicio"
              value={filters.query}
              onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))}
            />
          </div>

          <div className="fl-scroll-x flex gap-2 overflow-x-auto pb-1">
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

          <p className="text-faint text-xs">
            <span className="fl-num">{formatInteger(visible.length)}</span> de{' '}
            <span className="fl-num">{formatInteger(total)}</span> ejercicios
          </p>
        </div>
      </Card>

      {error ? <ErrorState message={error} onRetry={() => void reload()} /> : null}
      {loading ? <LoadingState message="Cargando catálogo…" /> : null}

      {!loading && visible.length === 0 ? (
        <EmptyState title="Sin resultados" message="No hay ejercicios que coincidan con la búsqueda." />
      ) : null}

      <ul className="flex flex-col gap-3">
        {visible.map((exercise) => {
          const expanded = detailExercise?.id === exercise.id;
          return (
            <li key={exercise.id}>
              <Card className="!p-0">
                <div className="flex items-center gap-2 px-4 py-3">
                  <button
                    type="button"
                    onClick={() =>
                      setDetailId((current) => (current === exercise.id ? null : exercise.id))
                    }
                    aria-expanded={expanded}
                    className="rounded-field focus-visible:ring-accent flex min-w-0 flex-1 items-center gap-3 text-left transition focus-visible:ring-2 focus-visible:outline-none"
                  >
                    <span className="rounded-field bg-surface-high text-accent grid size-10 shrink-0 place-items-center">
                      <IconDumbbell className="size-5" />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="text-ink flex flex-wrap items-center gap-2 text-sm font-semibold">
                        {exercise.name}
                        {exercise.isCustom ? (
                          <span className="bg-warning-soft text-warning rounded-full px-2 py-0.5 text-[0.625rem] font-semibold tracking-wide uppercase">
                            Propio
                          </span>
                        ) : null}
                      </span>
                      <span className="text-muted block text-xs">
                        {groupNameById.get(exercise.muscleGroupId) ?? 'Sin grupo'} ·{' '}
                        {exercise.equipment} · {kindLabels[exercise.kind]}
                      </span>
                    </span>
                    <IconChevronRight
                      className={`text-faint size-4 shrink-0 transition ${expanded ? 'rotate-90' : ''}`}
                    />
                  </button>
                  {exercise.isCustom ? (
                    <Button
                      variant="danger"
                      className="!px-3 !py-1.5 text-xs"
                      onClick={() => {
                        if (window.confirm(`¿Eliminar "${exercise.name}"?`)) {
                          void remove(exercise.id);
                        }
                      }}
                    >
                      Eliminar
                    </Button>
                  ) : null}
                </div>
                {expanded ? (
                  <ExerciseDetail
                    exercise={exercise}
                    groupNameById={groupNameById}
                    onClose={() => setDetailId(null)}
                  />
                ) : null}
              </Card>
            </li>
          );
        })}
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
    <div className="border-line flex flex-col gap-3 border-t px-4 py-3">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-ink text-sm font-semibold">Detalle</h3>
        <Button variant="ghost" className="!px-2 !py-1 text-xs" onClick={onClose}>
          Cerrar
        </Button>
      </div>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        <LabeledValue label="Tipo" value={kindLabels[exercise.kind]} />
        <LabeledValue label="Equipamiento" value={exercise.equipment} />
        <LabeledValue
          label="Grupo muscular"
          value={groupNameById.get(exercise.muscleGroupId) ?? 'Sin grupo'}
        />
        {secondaryGroupName !== null ? (
          <LabeledValue label="Grupo secundario" value={secondaryGroupName} />
        ) : null}
      </div>
    </div>
  );
}
