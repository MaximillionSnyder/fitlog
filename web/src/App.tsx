import { useDatabase } from '@/db/bootstrap';
import { useDbStatusStore } from '@/state/dbStatus';

const stateLabels: Record<string, string> = {
  iniciando: 'Iniciando…',
  listo: 'Base de datos lista',
  'sin-opfs': 'Este navegador no soporta OPFS',
  error: 'Error al iniciar la base de datos',
};

export default function App() {
  const db = useDatabase();
  const status = useDbStatusStore((state) => state.status);

  return (
    <main className="mx-auto flex min-h-dvh max-w-2xl flex-col gap-6 p-6">
      <header className="pt-6">
        <h1 className="text-3xl font-bold tracking-tight text-white">FitLog</h1>
        <p className="text-sm text-slate-400">Registro de entrenamiento local-first</p>
      </header>

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
            <div className="col-span-2">
              <dt className="text-slate-500">SQLite</dt>
              <dd className="font-mono">{status.sqliteVersion}</dd>
            </div>
            <div className="col-span-2">
              <dt className="text-slate-500">Cliente Drizzle</dt>
              <dd className="font-mono">{db ? 'conectado' : 'pendiente'}</dd>
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
    </main>
  );
}
