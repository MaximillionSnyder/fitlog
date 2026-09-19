import { useState, type FormEvent } from 'react';

import { formatInteger, formatKg } from '@/domain/format';
import type { CatalogState } from '@/state/useCatalog';
import type { RoutinesState } from '@/state/useRoutines';
import type { WorkoutState } from '@/state/useWorkout';
import {
  IconArrowDown,
  IconArrowUp,
  IconChevronRight,
  IconDumbbell,
  IconPlay,
  IconPlus,
} from '@/ui/icons';
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  SectionHeader,
} from '@/ui/primitives';

/**
 * Rutinas: plantillas reutilizables con sus ejercicios objetivo.
 *
 * La pantalla se arma con las primitivas del sistema de diseno (Card, SectionHeader, Button,
 * EmptyState): el titulo de la pantalla lo pone el shell, aca solo van los encabezados de seccion.
 */

const inputClass =
  'rounded-field border border-line bg-surface-low px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-accent focus:outline-none w-full';

function parseNumber(raw: string): number | null {
  if (raw.trim() === '') return null;
  return Number(raw.replace(',', '.'));
}

function parseIntOrNull(raw: string): number | null {
  if (raw.trim() === '') return null;
  return Number.parseInt(raw, 10);
}

export default function RoutinesView({
  routines,
  catalog,
  workout,
}: {
  routines: RoutinesState;
  catalog: CatalogState;
  workout: WorkoutState;
}) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const [exerciseId, setExerciseId] = useState('');
  const [targetSets, setTargetSets] = useState('4');
  const [targetReps, setTargetReps] = useState('8');
  const [targetWeight, setTargetWeight] = useState('');
  const [restSeconds, setRestSeconds] = useState('');

  const exercises = catalog.snapshot.exercises;

  async function submitRoutine(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    if (name.trim() === '') {
      setFormError('El nombre de la rutina no puede estar vacío');
      return;
    }
    await routines.create(name, description.trim() === '' ? null : description.trim());
    setName('');
    setDescription('');
  }

  async function submitEdit(routineId: string) {
    setFormError(null);
    if (editName.trim() === '') {
      setFormError('El nombre de la rutina no puede estar vacío');
      return;
    }
    await routines.update(
      routineId,
      editName,
      editDescription.trim() === '' ? null : editDescription.trim()
    );
    setEditingId(null);
  }

  async function submitExercise(event: FormEvent, routineId: string) {
    event.preventDefault();
    setFormError(null);
    const selected = exerciseId || exercises[0]?.id || '';
    if (selected === '') {
      setFormError('Elegí un ejercicio');
      return;
    }
    await routines.addExercise(routineId, {
      exerciseId: selected,
      targetSets: parseIntOrNull(targetSets),
      targetReps: parseIntOrNull(targetReps),
      targetWeightKg: parseNumber(targetWeight),
      restSeconds: parseIntOrNull(restSeconds),
      notes: null,
    });
    setTargetWeight('');
    setRestSeconds('');
  }

  if (routines.loading) {
    return <LoadingState message="Cargando rutinas…" />;
  }

  return (
    <div className="flex flex-col gap-5">
      {routines.error ? <ErrorState message={routines.error} /> : null}
      {formError ? (
        <p className="rounded-field bg-danger-soft text-danger px-3 py-2 text-sm">{formError}</p>
      ) : null}

      <SectionHeader title="Nueva rutina" />
      <Card>
        <form onSubmit={submitRoutine} className="flex flex-col gap-3">
          <input
            className={inputClass}
            placeholder="Nombre de la rutina (ej. Día de empuje)"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <input
            className={inputClass}
            placeholder="Descripción (opcional)"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          <Button type="submit" icon={<IconPlus className="size-4" />} className="self-start">
            Crear rutina
          </Button>
        </form>
      </Card>

      <SectionHeader
        title="Tus rutinas"
        trailing={`${formatInteger(routines.routines.length)} en total`}
      />

      {routines.routines.length === 0 ? (
        <Card>
          <EmptyState
            title="Todavía no tenés rutinas"
            message="Armá tu primera plantilla con el formulario de arriba."
          />
        </Card>
      ) : (
        <ul className="flex flex-col gap-3">
          {routines.routines.map((routine) => (
            <li key={routine.id}>
              <Card>
                {editingId === routine.id ? (
                  <div className="flex flex-col gap-3">
                    <input
                      className={inputClass}
                      value={editName}
                      aria-label="Nombre de la rutina"
                      onChange={(event) => setEditName(event.target.value)}
                    />
                    <input
                      className={inputClass}
                      placeholder="Descripción (opcional)"
                      value={editDescription}
                      onChange={(event) => setEditDescription(event.target.value)}
                    />
                    <div className="flex flex-wrap gap-2">
                      <Button onClick={() => void submitEdit(routine.id)}>Guardar</Button>
                      <Button variant="secondary" onClick={() => setEditingId(null)}>
                        Cancelar
                      </Button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="text-ink flex items-center gap-2 text-sm font-semibold">
                          <IconDumbbell className="text-accent-text size-4 shrink-0" />
                          <span className="truncate">{routine.name}</span>
                        </p>
                        <p className="text-muted mt-1 text-xs">
                          {routine.description ?? 'Sin descripción'} ·{' '}
                          {formatInteger(routine.exercises.length)} ejercicios
                        </p>
                      </div>
                      <Button
                        icon={<IconPlay className="size-4" />}
                        disabled={workout.active !== null}
                        onClick={() => void workout.start(routine.id)}
                      >
                        Entrenar
                      </Button>
                    </div>

                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      <Button
                        variant="secondary"
                        onClick={() => {
                          setEditingId(routine.id);
                          setEditName(routine.name);
                          setEditDescription(routine.description ?? '');
                        }}
                      >
                        Editar
                      </Button>
                      <Button
                        variant="ghost"
                        icon={
                          <IconChevronRight
                            className={`size-4 transition-transform ${
                              expandedId === routine.id ? 'rotate-90' : ''
                            }`}
                          />
                        }
                        aria-expanded={expandedId === routine.id}
                        onClick={() =>
                          setExpandedId(expandedId === routine.id ? null : routine.id)
                        }
                      >
                        {expandedId === routine.id ? 'Ocultar' : 'Ejercicios'}
                      </Button>
                      <Button
                        variant="danger"
                        onClick={() => {
                          if (window.confirm(`¿Eliminar la rutina "${routine.name}"?`)) {
                            void routines.remove(routine.id);
                          }
                        }}
                      >
                        Eliminar
                      </Button>
                    </div>

                    {expandedId === routine.id && (
                      <div className="border-line mt-4 flex flex-col gap-3 border-t pt-4">
                        <SectionHeader title="Ejercicios de la rutina" />

                        {routine.exercises.length === 0 ? (
                          <p className="text-faint text-xs">
                            La rutina todavía no tiene ejercicios.
                          </p>
                        ) : (
                          <ul className="flex flex-col gap-2">
                            {routine.exercises.map((item) => (
                              <li
                                key={item.id}
                                className="rounded-tile bg-surface-low flex flex-wrap items-center justify-between gap-3 px-3 py-2"
                              >
                                <span className="text-ink min-w-0 text-sm">
                                  <span className="text-faint fl-num">{item.position}.</span>{' '}
                                  {item.exerciseName}
                                </span>
                                <span className="flex flex-wrap items-center gap-2">
                                  <span className="text-muted fl-num text-xs">
                                    {item.targetSets ?? '—'} × {item.targetReps ?? '—'}
                                    {item.targetWeightKg !== null &&
                                      ` · ${formatKg(item.targetWeightKg)} kg`}
                                    {item.restSeconds !== null && ` · ${item.restSeconds}s`}
                                  </span>
                                  <Button
                                    variant="secondary"
                                    icon={<IconArrowUp className="size-4" />}
                                    aria-label={`Mover ${item.exerciseName} arriba`}
                                    title="Mover arriba"
                                    onClick={() => void routines.moveExercise(item.id, 'up')}
                                  />
                                  <Button
                                    variant="secondary"
                                    icon={<IconArrowDown className="size-4" />}
                                    aria-label={`Mover ${item.exerciseName} abajo`}
                                    title="Mover abajo"
                                    onClick={() => void routines.moveExercise(item.id, 'down')}
                                  />
                                  <Button
                                    variant="danger"
                                    aria-label={`Quitar ${item.exerciseName}`}
                                    title="Quitar ejercicio"
                                    onClick={() => void routines.removeExercise(item.id)}
                                  >
                                    ✕
                                  </Button>
                                </span>
                              </li>
                            ))}
                          </ul>
                        )}

                        <form
                          onSubmit={(event) => void submitExercise(event, routine.id)}
                          className="flex flex-col gap-2"
                        >
                          <select
                            className={inputClass}
                            aria-label="Ejercicio"
                            value={exerciseId || exercises[0]?.id || ''}
                            onChange={(event) => setExerciseId(event.target.value)}
                          >
                            {exercises.map((exercise) => (
                              <option key={exercise.id} value={exercise.id}>
                                {exercise.name}
                              </option>
                            ))}
                          </select>
                          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                            <input
                              className={`${inputClass} fl-num`}
                              inputMode="numeric"
                              placeholder="Series"
                              aria-label="Series"
                              value={targetSets}
                              onChange={(event) => setTargetSets(event.target.value)}
                            />
                            <input
                              className={`${inputClass} fl-num`}
                              inputMode="numeric"
                              placeholder="Reps"
                              aria-label="Reps"
                              value={targetReps}
                              onChange={(event) => setTargetReps(event.target.value)}
                            />
                            <input
                              className={`${inputClass} fl-num`}
                              inputMode="decimal"
                              placeholder="Peso"
                              aria-label="Peso"
                              value={targetWeight}
                              onChange={(event) => setTargetWeight(event.target.value)}
                            />
                            <input
                              className={`${inputClass} fl-num`}
                              inputMode="numeric"
                              placeholder="Descanso"
                              aria-label="Descanso"
                              value={restSeconds}
                              onChange={(event) => setRestSeconds(event.target.value)}
                            />
                          </div>
                          <Button
                            type="submit"
                            variant="secondary"
                            icon={<IconPlus className="size-4" />}
                            className="self-start"
                          >
                            Agregar ejercicio
                          </Button>
                        </form>
                      </div>
                    )}
                  </>
                )}
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
