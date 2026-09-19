import { useMemo } from 'react';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { formatDay, formatMetric, metricValue, type ProgressMetric, type RangePreset } from '@/domain/progress';
import type { CatalogState } from '@/state/useCatalog';
import type { ProgressState } from '@/state/useProgress';

const metricLabels: Record<ProgressMetric, string> = {
  maxWeightKg: 'Peso máximo',
  volumeKg: 'Volumen',
  bestOneRepMaxKg: '1RM estimado',
};

const presetLabels: Record<RangePreset, string> = {
  '30d': '30 días',
  '90d': '90 días',
  all: 'Todo',
};

const inputClass =
  'w-full rounded-lg border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-sky-500 focus:outline-none';

export default function ProgressView({
  progress,
  catalog,
}: {
  progress: ProgressState;
  catalog: CatalogState;
}) {
  const exercises = catalog.snapshot.exercises;
  const selectedExerciseId = progress.exerciseId ?? exercises[0]?.id ?? '';
  const selectedExercise = exercises.find((exercise) => exercise.id === selectedExerciseId);
  const metric = progress.metric;

  const chartData = useMemo(
    () =>
      progress.points.map((point) => ({
        label: formatDay(point.startedAtMs),
        value: metricValue(point, metric),
        workingSets: point.workingSets,
      })),
    [progress.points, metric]
  );

  const lastPoint = progress.points[progress.points.length - 1];

  return (
    <section className="flex flex-col gap-4">
      <div>
        <h2 className="text-xl font-semibold text-white">Progreso</h2>
        <p className="text-xs text-slate-400">Evolución por ejercicio a lo largo del tiempo.</p>
      </div>

      <select
        className={inputClass}
        value={selectedExerciseId}
        onChange={(event) => progress.selectExercise(event.target.value)}
      >
        {exercises.map((exercise) => (
          <option key={exercise.id} value={exercise.id}>
            {exercise.name}
          </option>
        ))}
      </select>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(metricLabels) as ProgressMetric[]).map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => progress.selectMetric(option)}
            className={`rounded-full border px-3 py-1 text-xs ${
              metric === option
                ? 'border-sky-400 bg-sky-400/20 text-sky-200'
                : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-slate-500'
            }`}
          >
            {metricLabels[option]}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as RangePreset[]).map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => progress.selectPreset(option)}
            className={`rounded-full border px-3 py-1 text-xs ${
              progress.preset === option
                ? 'border-slate-400 bg-slate-700/60 text-white'
                : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-slate-500'
            }`}
          >
            {presetLabels[option]}
          </button>
        ))}
      </div>

      {progress.error && <p className="text-sm text-rose-400">{progress.error}</p>}
      {progress.loading && <p className="text-sm text-slate-400">Calculando progreso…</p>}

      {!progress.loading && progress.points.length === 0 && (
        <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
          {selectedExercise
            ? `No hay series registradas de "${selectedExercise.name}" en el rango elegido.`
            : 'Elegí un ejercicio para ver su progreso.'}
        </p>
      )}

      {progress.points.length > 0 && (
        <>
          <div className="rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4">
            <div className="flex items-baseline justify-between">
              <p className="text-xs text-slate-400">
                {metricLabels[metric]} · {progress.points.length} sesiones
              </p>
              <p className="font-mono text-lg text-sky-300">
                {lastPoint ? formatMetric(metricValue(lastPoint, metric), metric) : '—'}
              </p>
            </div>
            <div className="mt-3 h-56 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: -12 }}>
                  <CartesianGrid stroke="#1e293b" strokeDasharray="3 3" />
                  <XAxis dataKey="label" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                  <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} width={56} />
                  <Tooltip
                    contentStyle={{
                      background: '#0f172a',
                      border: '1px solid #334155',
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                    labelStyle={{ color: '#e2e8f0' }}
                    formatter={(value) => [
                      formatMetric(Number(value ?? 0), metric),
                      metricLabels[metric],
                    ]}
                  />
                  <Line
                    type="monotone"
                    dataKey="value"
                    stroke="#38bdf8"
                    strokeWidth={2}
                    dot={{ r: 3, fill: '#38bdf8' }}
                    activeDot={{ r: 5 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          <ul className="flex flex-col gap-1 text-xs text-slate-400">
            {[...progress.points].reverse().map((point) => (
              <li key={point.sessionId} className="flex items-center justify-between gap-2">
                <span>{formatDay(point.startedAtMs)}</span>
                <span className="font-mono">
                  {point.workingSets} series · {formatMetric(metricValue(point, metric), metric)}
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

