import { useState, type FormEvent } from 'react';

import { formatDuration } from '@/domain/workout';
import type { CatalogState } from '@/state/useCatalog';
import type { WorkoutState } from '@/state/useWorkout';

const inputClass =
  'w-full rounded-lg border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none';

const buttonClass =
  'rounded-lg bg-sky-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400 disabled:opacity-50';

interface EditingSet {
  readonly id: string;
  weight: string;
  reps: string;
  rir: string;
}

function formatDateTime(timestamp: number): string {
  return new Date(timestamp).toLocaleString('es', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatVolume(volume: number): string {
  return `${Math.round(volume).toLocaleString('es')} kg`;
}

function parseNumber(raw: string): number | null {
  if (raw.trim() === '') return null;
  return Number(raw.replace(',', '.'));
}

export default function WorkoutView({
  workout,
  catalog,
}: {
  workout: WorkoutState;
  catalog: CatalogState;
}) {
  const [exerciseId, setExerciseId] = useState('');
  const [weight, setWeight] = useState('60');
  const [reps, setReps] = useState('10');
  const [rir, setRir] = useState('');
  const [notes, setNotes] = useState('');
  const [isWarmup, setIsWarmup] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [editing, setEditing] = useState<EditingSet | null>(null);

  const exercises = catalog.snapshot.exercises;
  const selectedExerciseId = exerciseId || exercises[0]?.id || '';

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
    return <p className="text-sm text-slate-400">Cargando entrenamientos…</p>;
  }

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold text-white">Entrenar</h2>
          <p className="text-xs text-slate-400">
            {workout.active
              ? `Sesión en curso desde ${formatDateTime(workout.active.startedAt)}` +
                (workout.active.routineName ? ` · Rutina: ${workout.active.routineName}` : '')
              : 'Sin sesión activa'}
          </p>
        </div>
        {workout.active ? (
          <button
            type="button"
            className="rounded-lg border border-rose-500/40 px-3 py-2 text-sm text-rose-300 hover:bg-rose-500/10"
            onClick={() => void workout.finish()}
          >
            Finalizar
          </button>
        ) : (
          <button type="button" className={buttonClass} onClick={() => void workout.start()}>
            Iniciar entrenamiento
          </button>
        )}
      </div>

      {workout.error && <p className="text-sm text-rose-400">{workout.error}</p>}

      {workout.active && (
        <>
          <div className="grid grid-cols-3 gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4 text-center">
            <div>
              <p className="text-xs text-slate-500">Series efectivas</p>
              <p className="font-mono text-lg">{workout.active.summary.workingSets}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">Volumen</p>
              <p className="font-mono text-lg">
                {formatVolume(workout.active.summary.totalVolumeKg)}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500">Series totales</p>
              <p className="font-mono text-lg">{workout.active.summary.totalSets}</p>
            </div>
          </div>

          <form
            onSubmit={submit}
            className="flex flex-col gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4"
          >
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
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={isWarmup}
                onChange={(event) => setIsWarmup(event.target.checked)}
              />
              Serie de calentamiento
            </label>
            {formError && <p className="text-sm text-rose-400">{formError}</p>}
            <button type="submit" className={`${buttonClass} self-start`}>
              Registrar serie
            </button>
          </form>

          {workout.activeSets.length === 0 ? (
            <p className="text-xs text-slate-500">Todavía no hay series en esta sesión.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {workout.activeSets.map((set) => (
                <li
                  key={set.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-slate-700/60 bg-slate-900/40 px-4 py-2 text-sm"
                >
                  {editing?.id === set.id ? (
                    <>
                      <span className="font-mono text-slate-400">#{set.setIndex}</span>
                      <input
                        className={inputClass}
                        inputMode="decimal"
                        value={editing.weight}
                        onChange={(event) =>
                          setEditing({ ...editing, weight: event.target.value })
                        }
                      />
                      <input
                        className={inputClass}
                        inputMode="numeric"
                        value={editing.reps}
                        onChange={(event) => setEditing({ ...editing, reps: event.target.value })}
                      />
                      <input
                        className={inputClass}
                        inputMode="numeric"
                        value={editing.rir}
                        onChange={(event) => setEditing({ ...editing, rir: event.target.value })}
                      />
                      <span className="flex gap-2">
                        <button type="button" className="text-xs text-emerald-300" onClick={() => void saveEdit()}>
                          Guardar
                        </button>
                        <button
                          type="button"
                          className="text-xs text-slate-400"
                          onClick={() => setEditing(null)}
                        >
                          Cancelar
                        </button>
                      </span>
                    </>
                  ) : (
                    <>
                      <span>
                        <span className="font-mono text-slate-400">#{set.setIndex}</span>{' '}
                        {set.exerciseName}
                        {set.isWarmup && (
                          <span className="ml-2 rounded-full bg-slate-600/40 px-2 py-0.5 text-[10px] uppercase text-slate-300">
                            Calentamiento
                          </span>
                        )}
                      </span>
                      <span className="flex items-center gap-3">
                        <span className="font-mono">
                          {set.weightKg ?? '—'} kg × {set.reps ?? '—'}
                          {set.rir !== null && ` · RIR ${set.rir}`}
                        </span>
                        <button
                          type="button"
                          className="text-xs text-sky-300 hover:text-sky-200"
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
                        </button>
                        <button
                          type="button"
                          className="text-xs text-slate-400 hover:text-slate-200"
                          onClick={() => void workout.remove(set.id)}
                        >
                          Eliminar
                        </button>
                      </span>
                    </>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}

      <div className="mt-2 flex flex-col gap-3">
        <h3 className="text-sm font-semibold uppercase tracking-widest text-slate-500">Historial</h3>

        {workout.history.length === 0 && (
          <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
            Todavía no registraste entrenamientos.
          </p>
        )}

        <ul className="flex flex-col gap-2">
          {workout.history.map((session) => (
            <li
              key={session.id}
              className="rounded-xl border border-slate-700/60 bg-slate-900/40 px-4 py-3 text-sm"
            >
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-medium text-slate-100">
                    {formatDateTime(session.startedAt)}
                    {session.finishedAt === null && (
                      <span className="ml-2 rounded-full bg-emerald-400/20 px-2 py-0.5 text-[10px] font-semibold uppercase text-emerald-300">
                        En curso
                      </span>
                    )}
                  </p>
                  <p className="text-xs text-slate-400">
                    {formatDuration(session.startedAt, session.finishedAt) ?? '—'} ·{' '}
                    {session.summary.workingSets} series ·{' '}
                    {formatVolume(session.summary.totalVolumeKg)}
                  </p>
                </div>
                <button
                  type="button"
                  className="text-xs text-sky-300 hover:text-sky-200"
                  onClick={() => void workout.openDetail(session.id)}
                >
                  Ver detalle
                </button>
              </div>
            </li>
          ))}
        </ul>

        {workout.detail && (
          <div className="rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-semibold text-slate-200">
                Detalle · {formatDateTime(workout.detail.session.startedAt)}
                {workout.detail.session.routineName
                  ? ` · Rutina: ${workout.detail.session.routineName}`
                  : ''}
              </h4>
              <button
                type="button"
                className="text-xs text-slate-400 hover:text-slate-200"
                onClick={workout.closeDetail}
              >
                Cerrar
              </button>
            </div>
            {workout.detail.sets.length === 0 ? (
              <p className="mt-3 text-sm text-slate-400">Esta sesión no tiene series registradas.</p>
            ) : (
              <ul className="mt-3 flex flex-col gap-1 text-sm">
                {workout.detail.sets.map((set) => (
                  <li key={set.id} className="flex items-center justify-between gap-2">
                    <span>
                      <span className="font-mono text-slate-400">#{set.setIndex}</span>{' '}
                      {set.exerciseName}
                      {set.isWarmup && (
                        <span className="ml-2 text-[10px] uppercase text-slate-400">
                          calentamiento
                        </span>
                      )}
                    </span>
                    <span className="font-mono text-slate-300">
                      {set.weightKg ?? '—'} kg × {set.reps ?? '—'}
                      {set.rir !== null && ` · RIR ${set.rir}`}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
