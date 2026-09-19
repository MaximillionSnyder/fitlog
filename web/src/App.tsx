import { useState } from 'react';

import { useDatabase } from '@/db/bootstrap';
import { useDbStatusStore } from '@/state/dbStatus';
import { useCatalog } from '@/state/useCatalog';
import { useComparisons } from '@/state/useComparisons';
import { useProgress } from '@/state/useProgress';
import { useRoutines } from '@/state/useRoutines';
import { useWorkout } from '@/state/useWorkout';
import CatalogView from '@/ui/CatalogView';
import ComparisonsView from '@/ui/ComparisonsView';
import ProgressView from '@/ui/ProgressView';
import RoutinesView from '@/ui/RoutinesView';
import WorkoutView from '@/ui/WorkoutView';

const stateLabels: Record<string, string> = {
  iniciando: 'Iniciando…',
  listo: 'Base de datos lista',
  'sin-opfs': 'Este navegador no soporta OPFS',
  error: 'Error al iniciar la base de datos',
};

type View = 'inicio' | 'catalogo' | 'entrenar' | 'rutinas' | 'progreso' | 'comparativas';

export default function App() {
  const db = useDatabase();
  const status = useDbStatusStore((state) => state.status);
  const catalog = useCatalog(db);
  const workout = useWorkout(db);
  const routines = useRoutines(db);
  const progress = useProgress(db);
  const comparisons = useComparisons(db);
  const [view, setView] = useState<View>('inicio');

  return (
    <main className="mx-auto flex min-h-dvh max-w-2xl flex-col gap-6 p-6">
      <header className="flex items-end justify-between gap-4 pt-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-white">FitLog</h1>
          <p className="text-sm text-slate-400">Registro de entrenamiento local-first</p>
        </div>
        <nav className="flex gap-2 text-sm">
          {(
            [
              ['inicio', 'Inicio'],
              ['entrenar', 'Entrenar'],
              ['rutinas', 'Rutinas'],
              ['progreso', 'Progreso'],
              ['comparativas', 'Comparativas'],
              ['catalogo', 'Catálogo'],
            ] as const
          ).map(([target, label]) => (
            <button
              key={target}
              type="button"
              onClick={() => setView(target)}
              className={`rounded-lg px-3 py-1.5 ${
                view === target ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {label}
            </button>
          ))}
        </nav>
      </header>

      {view === 'inicio' && (
        <section className="rounded-2xl border border-slate-700/60 bg-slate-900/60 p-5">
          <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-500">Estado</h2>
          <p className="mt-2 text-lg font-medium">{stateLabels[status.state] ?? status.state}</p>

          {status.state === 'listo' && (
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-slate-500">Esquema</dt>
                <dd className="font-mono">v{status.schemaVersion}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Persistencia</dt>
                <dd className="font-mono">{status.persistence}</dd>
              </div>
              <div>
                <dt className="text-slate-500">SQLite</dt>
                <dd className="font-mono">{status.sqliteVersion}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Ejercicios</dt>
                <dd className="font-mono">{catalog.snapshot.exercises.length}</dd>
              </div>
            </dl>
          )}

          {status.state === 'sin-opfs' && (
            <p className="mt-3 text-sm text-amber-300">
              FitLog necesita OPFS (Origin Private File System) para guardar tus datos. Probá con una
              versión reciente de Chrome, Edge, Firefox o Safari.
            </p>
          )}

          {status.state === 'error' && <p className="mt-3 text-sm text-rose-400">{status.error}</p>}
        </section>
      )}

      {view === 'catalogo' && <CatalogView catalog={catalog} />}
      {view === 'entrenar' && <WorkoutView workout={workout} catalog={catalog} />}
      {view === 'progreso' && <ProgressView progress={progress} catalog={catalog} />}
      {view === 'comparativas' && (
        <ComparisonsView comparisons={comparisons} catalog={catalog} />
      )}
      {view === 'rutinas' && (
        <RoutinesView routines={routines} catalog={catalog} workout={workout} />
      )}
    </main>
  );
}
