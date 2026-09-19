import { useState, type FormEvent } from 'react';

import type { CatalogState } from '@/state/useCatalog';
import type { RoutinesState } from '@/state/useRoutines';
import type { WorkoutState } from '@/state/useWorkout';

const inputClass =
  'w-full rounded-lg border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none';

const buttonClass =
  'rounded-lg bg-sky-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400 disabled:opacity-50';

const smallButton = 'text-xs text-slate-400 hover:text-slate-200';

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
    return <p className="text-sm text-slate-400">Cargando rutinas…</p>;
  }

  return (
    <section className="flex flex-col gap-4">
      <div>
        <h2 className="text-xl font-semibold text-white">Rutinas</h2>
        <p className="text-xs text-slate-400">
          Plantillas reutilizables: definí los ejercicios y arrancá a entrenar desde acá.
        </p>
      </div>

      {routines.error && <p className="text-sm text-rose-400">{routines.error}</p>}
      {formError && <p className="text-sm text-rose-400">{formError}</p>}

      <form
        onSubmit={submitRoutine}
        className="flex flex-col gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4"
      >
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
        <button type="submit" className={`${buttonClass} self-start`}>
          Crear rutina
        </button>
      </form>

      {routines.routines.length === 0 && (
        <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
          Todavía no tenés rutinas.
        </p>
      )}

      <ul className="flex flex-col gap-3">
        {routines.routines.map((routine) => (
          <li
            key={routine.id}
            className="rounded-2xl border border-slate-700/60 bg-slate-900/40 px-4 py-3 text-sm"
          >
            {editingId === routine.id ? (
              <div className="flex flex-col gap-2">
                <input
                  className={inputClass}
                  value={editName}
                  onChange={(event) => setEditName(event.target.value)}
                />
                <input
                  className={inputClass}
                  placeholder="Descripción (opcional)"
                  value={editDescription}
                  onChange={(event) => setEditDescription(event.target.value)}
                />
                <div className="flex gap-2">
                  <button type="button" className={buttonClass} onClick={() => void submitEdit(routine.id)}>
                    Guardar
                  </button>
                  <button type="button" className={smallButton} onClick={() => setEditingId(null)}>
                    Cancelar
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <p className="font-medium text-slate-100">{routine.name}</p>
                    <p className="text-xs text-slate-400">
                      {routine.description ?? 'Sin descripción'} · {routine.exercises.length} ejercicios
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      className={buttonClass}
                      disabled={workout.active !== null}
                      onClick={() => void workout.start(routine.id)}
                    >
                      Entrenar
                    </button>
                    <button
                      type="button"
                      className={smallButton}
                      onClick={() => {
                        setEditingId(routine.id);
                        setEditName(routine.name);
                        setEditDescription(routine.description ?? '');
                      }}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className={smallButton}
                      onClick={() =>
                        setExpandedId(expandedId === routine.id ? null : routine.id)
                      }
                    >
                      {expandedId === routine.id ? 'Ocultar' : 'Ejercicios'}
                    </button>
                    <button
                      type="button"
                      className="text-xs text-rose-300 hover:text-rose-200"
                      onClick={() => {
                        if (window.confirm(`¿Eliminar la rutina "${routine.name}"?`)) {
                          void routines.remove(routine.id);
                        }
                      }}
                    >
                      Eliminar
                    </button>
                  </div>
                </div>

                {expandedId === routine.id && (
                  <div className="mt-3 flex flex-col gap-3 border-t border-slate-700/60 pt-3">
                    {routine.exercises.length === 0 ? (
                      <p className="text-xs text-slate-500">La rutina todavía no tiene ejercicios.</p>
                    ) : (
                      <ul className="flex flex-col gap-2">
                        {routine.exercises.map((item) => (
                          <li
                            key={item.id}
                            className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-slate-950/40 px-3 py-2"
                          >
                            <span>
                              <span className="font-mono text-slate-400">{item.position}.</span>{' '}
                              {item.exerciseName}
                            </span>
                            <span className="flex items-center gap-3 text-xs text-slate-400">
                              <span className="font-mono">
                                {item.targetSets ?? '—'} × {item.targetReps ?? '—'}
                                {item.targetWeightKg !== null && ` · ${item.targetWeightKg} kg`}
                                {item.restSeconds !== null && ` · ${item.restSeconds}s`}
                              </span>
                              <button
                                type="button"
                                className={smallButton}
                                onClick={() => void routines.moveExercise(item.id, 'up')}
                              >
                                ↑
                              </button>
                              <button
                                type="button"
                                className={smallButton}
                                onClick={() => void routines.moveExercise(item.id, 'down')}
                              >
                                ↓
                              </button>
                              <button
                                type="button"
                                className="text-rose-300 hover:text-rose-200"
                                onClick={() => void routines.removeExercise(item.id)}
                              >
                                ✕
                              </button>
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
                        value={exerciseId || exercises[0]?.id || ''}
                        onChange={(event) => setExerciseId(event.target.value)}
                      >
                        {exercises.map((exercise) => (
                          <option key={exercise.id} value={exercise.id}>
                            {exercise.name}
                          </option>
                        ))}
                      </select>
                      <div className="grid grid-cols-4 gap-2">
                        <input
                          className={inputClass}
                          inputMode="numeric"
                          placeholder="Series"
                          value={targetSets}
                          onChange={(event) => setTargetSets(event.target.value)}
                        />
                        <input
                          className={inputClass}
                          inputMode="numeric"
                          placeholder="Reps"
                          value={targetReps}
                          onChange={(event) => setTargetReps(event.target.value)}
                        />
                        <input
                          className={inputClass}
                          inputMode="decimal"
                          placeholder="Peso"
                          value={targetWeight}
                          onChange={(event) => setTargetWeight(event.target.value)}
                        />
                        <input
                          className={inputClass}
                          inputMode="numeric"
                          placeholder="Descanso"
                          value={restSeconds}
                          onChange={(event) => setRestSeconds(event.target.value)}
                        />
                      </div>
                      <button type="submit" className={`${buttonClass} self-start`}>
                        Agregar ejercicio
                      </button>
                    </form>
                  </div>
                )}
              </>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
