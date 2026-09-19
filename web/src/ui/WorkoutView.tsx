import { useEffect, useMemo, useState, type FormEvent } from 'react';

import {
  formatDuration,
  formatDurationLong,
  formatKg,
  formatRelativeDay,
  formatVolumeKg,
} from '@/domain/format';
import type { CatalogState } from '@/state/useCatalog';
import type { RoutinesState } from '@/state/useRoutines';
import type { WorkoutState } from '@/state/useWorkout';
import {
  IconCalendar,
  IconChart,
  IconDumbbell,
  IconGrid,
  IconPlay,
  IconPlus,
  IconStop,
} from '@/ui/icons';
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  SectionHeader,
  StatTile,
} from '@/ui/primitives';

/**
 * Pantalla de entrenamiento: sesion activa con sus totales, alta y edicion de series e historial.
 *
 * El encabezado lo pone el shell, asi que la vista solo separa bloques con SectionHeader y arma
 * todo con las primitivas del sistema de diseno. El historial es una lista de tarjetas con el
 * resumen de cada sesion y el detalle se abre debajo.
 */

const inputClass =
  'w-full rounded-field border border-line bg-surface-low px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-accent focus:outline-none';

// Los botones de las filas (editar, eliminar, ver detalle) van compactos.
const rowButtonClass = '!px-3 !py-1.5 !text-xs';

interface EditingSet {
  readonly id: string;
  weight: string;
  reps: string;
  rir: string;
}

/**
 * Descanso entre series: tiempo pasado desde la ultima serie y, si la sesion viene de una rutina,
 * cuenta regresiva contra el descanso objetivo de ese ejercicio.
 */
function RestCard({
  exerciseName,
  startedAt,
  targetSeconds,
  now,
  onSkip,
}: {
  exerciseName: string;
  startedAt: number;
  targetSeconds: number | null;
  now: number;
  onSkip: () => void;
}) {
  const elapsedMs = Math.max(0, now - startedAt);
  const elapsedSeconds = Math.floor(elapsedMs / 1000);
  const done = targetSeconds !== null && elapsedSeconds >= targetSeconds;
  const progress =
    targetSeconds === null ? 0 : Math.min(100, (elapsedSeconds / targetSeconds) * 100);

  return (
    <div
      className={`rounded-card border border-line p-4 ${
        done ? 'bg-success-soft' : 'bg-surface'
      }`}
    >
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className={`text-sm font-semibold ${done ? 'text-success' : 'text-ink'}`}>
            {done ? 'Descanso completo' : 'Descanso'}
          </p>
          <p className="text-muted fl-num text-xs">
            {exerciseName} · {formatDuration(elapsedMs)}
            {targetSeconds === null ? '' : ` de ${formatDuration(targetSeconds * 1000)}`}
          </p>
        </div>
        <Button variant="ghost" onClick={onSkip}>
          Saltar
        </Button>
      </div>
      {targetSeconds === null ? null : (
        <div className="bg-surface-high mt-3 h-2 w-full overflow-hidden rounded-full">
          <div
            className={`h-full rounded-full ${done ? 'bg-success' : 'bg-accent'}`}
            style={{ width: `${progress}%` }}
          />
        </div>
      )}
    </div>
  );
}

function formatDateTime(timestamp: number): string {
  return new Date(timestamp).toLocaleString('es', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function parseNumber(raw: string): number | null {
  if (raw.trim() === '') return null;
  return Number(raw.replace(',', '.'));
}

export default function WorkoutView({
  workout,
  catalog,
  routines,
  onOpenSessionDetail,
}: {
  workout: WorkoutState;
  catalog: CatalogState;
  routines: RoutinesState;
  onOpenSessionDetail: (sessionId: string) => void;
}) {
  const [exerciseId, setExerciseId] = useState('');
  const [weight, setWeight] = useState('60');
  const [reps, setReps] = useState('10');
  const [rir, setRir] = useState('');
  const [notes, setNotes] = useState('');
  const [isWarmup, setIsWarmup] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [editing, setEditing] = useState<EditingSet | null>(null);
  const [now, setNow] = useState(() => Date.now());
  // Descanso en curso: se arma al registrar una serie y se corta al registrar la siguiente.
  const [rest, setRest] = useState<{ startedAt: number; exerciseName: string } | null>(null);

  const active = workout.active;
  const exercises = catalog.snapshot.exercises;
  const selectedExerciseId = exerciseId || exercises[0]?.id || '';
  const selectedExerciseName =
    exercises.find((exercise) => exercise.id === selectedExerciseId)?.name ?? 'Ejercicio';

  // Descanso objetivo por ejercicio cuando la sesion se arranco desde una rutina.
  const restTargets = useMemo(() => {
    const routine = routines.routines.find((item) => item.id === active?.routineId);
    const targets = new Map<string, number>();
    for (const exercise of routine?.exercises ?? []) {
      if (exercise.restSeconds !== null) targets.set(exercise.exerciseId, exercise.restSeconds);
    }
    return targets;
  }, [active?.routineId, routines.routines]);

  // Ultima serie del ejercicio elegido: sirve para precargar el formulario y para repetir.
  const lastSet = useMemo(() => {
    const ofExercise = workout.activeSets.filter((set) => set.exerciseId === selectedExerciseId);
    return ofExercise.length === 0
      ? null
      : ofExercise.reduce((latest, set) => (set.setIndex > latest.setIndex ? set : latest));
  }, [selectedExerciseId, workout.activeSets]);

  // Al cambiar de ejercicio, el formulario arranca con lo ultimo que se hizo con ese ejercicio.
  // Es el ajuste de estado durante el render que recomienda React, en lugar de un efecto.
  const [prefilledFor, setPrefilledFor] = useState(selectedExerciseId);
  if (prefilledFor !== selectedExerciseId) {
    setPrefilledFor(selectedExerciseId);
    if (lastSet) {
      setWeight(lastSet.weightKg === null ? '' : String(lastSet.weightKg));
      setReps(lastSet.reps === null ? '' : String(lastSet.reps));
      setRir(lastSet.rir === null ? '' : String(lastSet.rir));
    }
  }

  // Reloj de la sesion: alimenta la duracion y el tiempo desde la ultima serie (descanso).
  useEffect(() => {
    if (!active) return;
    const first = window.setTimeout(() => setNow(Date.now()), 0);
    const timer = window.setInterval(() => setNow(Date.now()), 5_000);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(timer);
    };
  }, [active]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    if (selectedExerciseId === '') {
      setFormError('Elegí un ejercicio');
      return;
    }

    const weightValue = parseNumber(weight);
    if (weightValue !== null && (Number.isNaN(weightValue) || weightValue < 0)) {
      setFormError('El peso debe ser un número mayor o igual a 0');
      return;
    }
    const repsValue = parseNumber(reps);
    if (repsValue !== null && (!Number.isInteger(repsValue) || repsValue < 0)) {
      setFormError('Las repeticiones deben ser un entero mayor o igual a 0');
      return;
    }
    const rirValue = parseNumber(rir);
    if (rirValue !== null && (!Number.isInteger(rirValue) || rirValue < 0 || rirValue > 10)) {
      setFormError('El RIR debe ser un entero entre 0 y 10');
      return;
    }

    await workout.add({
      exerciseId: selectedExerciseId,
      weightKg: weightValue,
      reps: repsValue,
      rir: rirValue,
      notes: notes.trim() === '' ? null : notes.trim(),
      isWarmup,
    });
    setNotes('');
    setIsWarmup(false);
  }

  async function saveEdit() {
    if (!editing) return;
    const weightValue = parseNumber(editing.weight);
    const repsValue = parseNumber(editing.reps);
    const rirValue = parseNumber(editing.rir);

    if (weightValue !== null && (Number.isNaN(weightValue) || weightValue < 0)) {
      setFormError('El peso debe ser un número mayor o igual a 0');
      return;
    }
    if (repsValue !== null && (!Number.isInteger(repsValue) || repsValue < 0)) {
      setFormError('Las repeticiones deben ser un entero mayor o igual a 0');
      return;
    }
    if (rirValue !== null && (!Number.isInteger(rirValue) || rirValue < 0 || rirValue > 10)) {
      setFormError('El RIR debe ser un entero entre 0 y 10');
      return;
    }

    setFormError(null);
    await workout.update(editing.id, {
      weightKg: weightValue,
      reps: repsValue,
      rir: rirValue,
      notes: null,
    });
    setEditing(null);
  }

  if (workout.loading) {
    return <LoadingState message="Cargando entrenamientos…" />;
  }

  const historyTrailing =
    workout.history.length === 0
      ? undefined
      : workout.history.length === 1
        ? '1 sesión'
        : `${workout.history.length} sesiones`;

  return (
    <div className="flex flex-col gap-5">
      <SectionHeader title="Sesión activa" />

      {active ? (
        <Card tone="accent" className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex min-w-0 items-center gap-3">
              <span className="bg-surface-high text-accent-text rounded-field grid size-10 shrink-0 place-items-center">
                <IconDumbbell className="size-5" />
              </span>
              <div className="min-w-0">
                <p className="text-ink truncate text-sm font-semibold">
                  {active.routineName ?? 'Entrenamiento libre'}
                </p>
                <p className="text-muted text-xs">
                  Sesión en curso desde{' '}
                  <span className="fl-num">{formatDateTime(active.startedAt)}</span>
                </p>
              </div>
            </div>
            <Button
              variant="primary"
              icon={<IconStop className="size-4" />}
              onClick={() => void workout.finish()}
            >
              Finalizar
            </Button>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <StatTile
              label="Duración"
              value={formatDuration(now - active.startedAt)}
              icon={<IconCalendar className="size-4" />}
            />
            <StatTile
              label="Series efectivas"
              value={String(active.summary.workingSets)}
              icon={<IconDumbbell className="size-4" />}
            />
            <StatTile
              label="Series totales"
              value={String(active.summary.totalSets)}
              icon={<IconGrid className="size-4" />}
            />
            <StatTile
              label="Volumen"
              value={formatVolumeKg(active.summary.totalVolumeKg)}
              unit="kg"
              tone="data"
              icon={<IconChart className="size-4" />}
            />
          </div>
        </Card>
      ) : (
        <Card className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex min-w-0 items-center gap-3">
            <span className="bg-surface-high text-accent-text rounded-field grid size-10 shrink-0 place-items-center">
              <IconDumbbell className="size-5" />
            </span>
            <p className="text-ink text-sm font-semibold">Sin sesión activa</p>
          </div>
          <Button
            variant="primary"
            icon={<IconPlay className="size-4" />}
            onClick={() => void workout.start()}
          >
            Iniciar entrenamiento
          </Button>
        </Card>
      )}

      {workout.error ? <ErrorState message={workout.error} /> : null}

      {active ? (
        <>
          <SectionHeader title="Agregar serie" />
          <Card>
            <form onSubmit={submit} className="flex flex-col gap-3">
              <select
                className={inputClass}
                value={selectedExerciseId}
                onChange={(event) => setExerciseId(event.target.value)}
              >
                {exercises.map((exercise) => (
                  <option key={exercise.id} value={exercise.id}>
                    {exercise.name}
                  </option>
                ))}
              </select>
              {lastSet ? (
                <div className="flex items-center justify-between gap-3">
                  <p className="text-muted fl-num text-xs">
                    Última: {formatKg(lastSet.weightKg)} kg × {lastSet.reps ?? '—'}
                    {lastSet.rir === null ? '' : ` · RIR ${lastSet.rir}`} · hace{' '}
                    {formatDuration(Math.max(0, now - lastSet.createdAtMs))}
                  </p>
                  <Button
                    variant="ghost"
                    onClick={() => {
                      setRest({ startedAt: Date.now(), exerciseName: selectedExerciseName });
                      void workout.add({
                        exerciseId: selectedExerciseId,
                        weightKg: lastSet.weightKg,
                        reps: lastSet.reps,
                        rir: lastSet.rir,
                        notes: null,
                        isWarmup: false,
                      });
                    }}
                  >
                    Repetir
                  </Button>
                </div>
              ) : null}

              <div className="grid grid-cols-3 gap-3">
                <input
                  className={inputClass}
                  inputMode="decimal"
                  placeholder="Peso (kg)"
                  value={weight}
                  onChange={(event) => setWeight(event.target.value)}
                />
                <input
                  className={inputClass}
                  inputMode="numeric"
                  placeholder="Reps"
                  value={reps}
                  onChange={(event) => setReps(event.target.value)}
                />
                <input
                  className={inputClass}
                  inputMode="numeric"
                  placeholder="RIR"
                  value={rir}
                  onChange={(event) => setRir(event.target.value)}
                />
              </div>
              <input
                className={inputClass}
                placeholder="Notas (opcional)"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
              />
              <label className="text-muted flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="accent-accent size-4"
                  checked={isWarmup}
                  onChange={(event) => setIsWarmup(event.target.checked)}
                />
                Serie de calentamiento
              </label>
              {formError ? <p className="text-danger text-sm">{formError}</p> : null}
              <Button
                type="submit"
                variant="primary"
                icon={<IconPlus className="size-4" />}
                className="self-start"
              >
                Registrar serie
              </Button>
            </form>
          </Card>

          {rest ? (
            <RestCard
              exerciseName={rest.exerciseName}
              startedAt={rest.startedAt}
              targetSeconds={restTargets.get(selectedExerciseId) ?? null}
              now={now}
              onSkip={() => setRest(null)}
            />
          ) : null}

          {workout.activeSets.length === 0 ? (
            <Card className="!p-0">
              <EmptyState message="Todavía no hay series en esta sesión." />
            </Card>
          ) : (
            <ul className="flex flex-col gap-3">
              {workout.activeSets.map((set) => (
                <li key={set.id}>
                  <Card className="!p-4">
                    {editing?.id === set.id ? (
                      <div className="flex flex-col gap-3">
                        <span className="text-faint fl-num text-sm">#{set.setIndex}</span>
                        <div className="grid grid-cols-3 gap-3">
                          <input
                            className={inputClass}
                            inputMode="decimal"
                            aria-label="Peso (kg)"
                            value={editing.weight}
                            onChange={(event) =>
                              setEditing({ ...editing, weight: event.target.value })
                            }
                          />
                          <input
                            className={inputClass}
                            inputMode="numeric"
                            aria-label="Reps"
                            value={editing.reps}
                            onChange={(event) =>
                              setEditing({ ...editing, reps: event.target.value })
                            }
                          />
                          <input
                            className={inputClass}
                            inputMode="numeric"
                            aria-label="RIR"
                            value={editing.rir}
                            onChange={(event) => setEditing({ ...editing, rir: event.target.value })}
                          />
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Button
                            variant="primary"
                            className={rowButtonClass}
                            onClick={() => void saveEdit()}
                          >
                            Guardar
                          </Button>
                          <Button
                            variant="ghost"
                            className={rowButtonClass}
                            onClick={() => setEditing(null)}
                          >
                            Cancelar
                          </Button>
                        </div>
                      </div>
                    ) : (
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="min-w-0">
                          <p className="text-ink flex flex-wrap items-center gap-2 text-sm font-semibold">
                            <span className="text-faint fl-num">#{set.setIndex}</span>
                            <span className="truncate">{set.exerciseName}</span>
                            {set.isWarmup ? (
                              <span className="bg-warning-soft text-warning rounded-full px-2 py-0.5 text-[0.625rem] font-semibold tracking-wide uppercase">
                                Calentamiento
                              </span>
                            ) : null}
                          </p>
                          <p className="text-muted fl-num mt-1 text-xs">
                            {formatKg(set.weightKg)} kg × {set.reps ?? '—'}
                            {set.rir !== null ? ` · RIR ${set.rir}` : ''}
                          </p>
                        </div>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="ghost"
                            className={rowButtonClass}
                            onClick={() =>
                              setEditing({
                                id: set.id,
                                weight: set.weightKg?.toString() ?? '',
                                reps: set.reps?.toString() ?? '',
                                rir: set.rir?.toString() ?? '',
                              })
                            }
                          >
                            Editar
                          </Button>
                          <Button
                            variant="danger"
                            className={rowButtonClass}
                            onClick={() => void workout.remove(set.id)}
                          >
                            Eliminar
                          </Button>
                        </div>
                      </div>
                    )}
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </>
      ) : null}

      <SectionHeader title="Historial" trailing={historyTrailing} />

      {workout.history.length === 0 ? (
        <Card className="!p-0">
          <EmptyState message="Todavía no registraste entrenamientos." />
        </Card>
      ) : (
        <ul className="flex flex-col gap-3">
          {workout.history.map((session) => (
            <li key={session.id}>
              <Card className="!p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-ink flex flex-wrap items-center gap-2 text-sm font-semibold">
                      <span className="truncate">{session.routineName ?? 'Entrenamiento libre'}</span>
                      {session.finishedAt === null ? (
                        <span className="bg-accent-soft text-accent-text rounded-full px-2 py-0.5 text-[0.625rem] font-semibold tracking-wide uppercase">
                          En curso
                        </span>
                      ) : null}
                    </p>
                    <p className="text-muted mt-1 text-xs">
                      <span className="fl-num">{formatRelativeDay(session.startedAt, now)}</span>
                      {' · '}
                      <span className="fl-num">
                        {formatDurationLong(session.startedAt, session.finishedAt)}
                      </span>
                      {' · '}
                      <span className="fl-num">{session.summary.workingSets}</span> series
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-data fl-num text-sm font-semibold">
                      {formatVolumeKg(session.summary.totalVolumeKg)} kg
                    </span>
                    <Button
                      variant="secondary"
                      className={rowButtonClass}
                      onClick={() => onOpenSessionDetail(session.id)}
                    >
                      Ver detalle
                    </Button>
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

    </div>
  );
}
