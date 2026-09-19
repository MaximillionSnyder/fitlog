import { formatDelta } from '@/domain/comparisons';
import type { CatalogState } from '@/state/useCatalog';
import type { ComparisonPreset, ComparisonsState } from '@/state/useComparisons';

const presetLabels: Record<ComparisonPreset, string> = {
  '30d': 'Últimos 30 días',
  '90d': 'Últimos 90 días',
};

function formatDate(timestampMs: number | null): string {
  if (timestampMs === null) return '—';
  return new Date(timestampMs).toLocaleDateString('es', {
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
  });
}

function formatKg(value: number): string {
  return `${Math.round(value * 10) / 10} kg`;
}

function deltaClass(delta: number | null): string {
  if (delta === null) return 'text-slate-400';
  if (delta > 0) return 'text-emerald-300';
  if (delta < 0) return 'text-rose-300';
  return 'text-slate-300';
}

export default function ComparisonsView({
  comparisons,
  catalog,
}: {
  comparisons: ComparisonsState;
  catalog: CatalogState;
}) {
  const exerciseNameById = new Map(
    catalog.snapshot.exercises.map((exercise) => [exercise.id, exercise.name])
  );
  const groupNameBySlug = new Map(
    catalog.snapshot.groups.map((group) => [group.slug, group.name])
  );

  return (
    <section className="flex flex-col gap-5">
      <div>
        <h2 className="text-xl font-semibold text-white">Comparativas</h2>
        <p className="text-xs text-slate-400">
          Marcas personales, comparación de periodos y balance muscular.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as ComparisonPreset[]).map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => comparisons.selectPreset(option)}
            className={`rounded-full border px-3 py-1 text-xs ${
              comparisons.preset === option
                ? 'border-slate-400 bg-slate-700/60 text-white'
                : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-slate-500'
            }`}
          >
            {presetLabels[option]}
          </button>
        ))}
      </div>

      {comparisons.error && <p className="text-sm text-rose-400">{comparisons.error}</p>}
      {comparisons.loading && <p className="text-sm text-slate-400">Calculando comparativas…</p>}

      {comparisons.comparison && (
        <div className="grid grid-cols-3 gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4 text-center">
          <div>
            <p className="text-xs text-slate-500">Volumen</p>
            <p className="font-mono text-lg">{formatKg(comparisons.comparison.current.volumeKg)}</p>
            <p className={`text-xs ${deltaClass(comparisons.comparison.volumeDeltaPct)}`}>
              {formatDelta(comparisons.comparison.volumeDeltaPct)}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Series efectivas</p>
            <p className="font-mono text-lg">{comparisons.comparison.current.workingSets}</p>
            <p className={`text-xs ${deltaClass(comparisons.comparison.setsDeltaPct)}`}>
              {formatDelta(comparisons.comparison.setsDeltaPct)}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Sesiones</p>
            <p className="font-mono text-lg">{comparisons.comparison.current.sessions}</p>
            <p className={`text-xs ${deltaClass(comparisons.comparison.sessionsDeltaPct)}`}>
              {formatDelta(comparisons.comparison.sessionsDeltaPct)}
            </p>
          </div>
        </div>
      )}

      <div className="flex flex-col gap-2">
        <h3 className="text-sm font-semibold uppercase tracking-widest text-slate-500">
          Marcas personales
        </h3>

        {!comparisons.loading && comparisons.records.length === 0 && (
          <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
            Todavía no hay marcas registradas.
          </p>
        )}

        <ul className="flex flex-col gap-2">
          {comparisons.records.map((record) => (
            <li
              key={record.exerciseId}
              className="rounded-xl border border-slate-700/60 bg-slate-900/40 px-4 py-3 text-sm"
            >
              <p className="font-medium text-slate-100">
                {exerciseNameById.get(record.exerciseId) ?? record.exerciseId}
              </p>
              <div className="mt-1 grid grid-cols-2 gap-1 text-xs text-slate-400">
                <span>
                  Peso: <span className="font-mono text-slate-200">{formatKg(record.bestWeightKg)}</span>{' '}
                  · {formatDate(record.bestWeightAtMs)}
                </span>
                <span>
                  1RM: <span className="font-mono text-slate-200">{formatKg(record.bestOneRepMaxKg)}</span>{' '}
                  · {formatDate(record.bestOneRepMaxAtMs)}
                </span>
                <span>
                  Volumen:{' '}
                  <span className="font-mono text-slate-200">
                    {formatKg(record.bestSessionVolumeKg)}
                  </span>{' '}
                  · {formatDate(record.bestSessionVolumeAtMs)}
                </span>
                <span>
                  Reps: <span className="font-mono text-slate-200">{record.bestReps}</span> ·{' '}
                  {formatDate(record.bestRepsAtMs)}
                </span>
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="flex flex-col gap-2">
        <h3 className="text-sm font-semibold uppercase tracking-widest text-slate-500">
          Balance muscular del periodo
        </h3>

        {!comparisons.loading && comparisons.balance.length === 0 && (
          <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
            No hay volumen registrado en el periodo elegido.
          </p>
        )}

        <ul className="flex flex-col gap-2">
          {comparisons.balance.map((entry) => (
            <li key={entry.muscleGroupSlug} className="flex flex-col gap-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-300">
                  {groupNameBySlug.get(entry.muscleGroupSlug) ?? entry.muscleGroupSlug}
                </span>
                <span className="font-mono text-slate-400">
                  {formatKg(entry.volumeKg)} · {entry.sharePct}%
                </span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-800">
                <div
                  className="h-full rounded-full bg-sky-500"
                  style={{ width: `${Math.min(100, entry.sharePct)}%` }}
                />
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
