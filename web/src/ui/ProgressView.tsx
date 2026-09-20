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

import { formatDecimal, formatDuration } from '@/domain/format';
import {
  ACTIVITY_METRICS,
  ACTIVITY_METRIC_LABELS,
  activityValue,
  type ActivityMetric,
} from '@/domain/activity';
import { formatDay, formatMetric, metricValue, type ProgressMetric, type RangePreset } from '@/domain/progress';
import type { CatalogState } from '@/state/useCatalog';
import type { ProgressState } from '@/state/useProgress';
import { IconCalendar, IconChart, IconScale } from '@/ui/icons';
import {
  Card,
  Chip,
  EmptyState,
  ErrorState,
  LoadingState,
  SectionHeader,
  StatTile,
} from '@/ui/primitives';

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
  'rounded-field border border-line bg-surface-low px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-accent focus:outline-none';

/**
 * Actividad importada: distancia, duración o pulso por entrenamiento.
 *
 * Es la única forma de ver los entrenamientos que vinieron de Huawei Health o de un GPX, porque no
 * tienen series con peso y reps.
 */
function ActivitySection({ progress }: { progress: ProgressState }) {
  const values = progress.activityValues;
  const totals = progress.activityTotals;
  const labelled = progress.activityPoints.filter(
    (point) => activityValue(point, progress.activityMetric) !== null
  );

  return (
    <>
      <SectionHeader
        title="Actividad importada"
        trailing={`${totals.sessions} entrenamientos`}
      />

      <div className="fl-scroll-x flex gap-2 overflow-x-auto pb-1">
        {ACTIVITY_METRICS.map((metric) => (
          <Chip
            key={metric}
            label={ACTIVITY_METRIC_LABELS[metric]}
            active={progress.activityMetric === metric}
            onClick={() => progress.selectActivityMetric(metric)}
          />
        ))}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <StatTile
          label="Distancia"
          value={formatDecimal(totals.distanceM / 1000, 2)}
          unit="km"
          tone="data"
          icon={<IconScale className="size-4" />}
        />
        <StatTile
          label="Tiempo"
          value={formatActivityValue(totals.durationMs, 'duration')}
          icon={<IconCalendar className="size-4" />}
        />
      </div>

      {values.length < 2 ? (
        <Card className="!p-4">
          <p className="text-muted text-sm">
            Hacen falta al menos dos entrenamientos con{' '}
            {ACTIVITY_METRIC_LABELS[progress.activityMetric].toLowerCase()} para dibujar la
            evolución.
          </p>
        </Card>
      ) : (
        <Card className="!p-4">
          <ResponsiveContainer width="100%" height={200}>
            <LineChart
              data={labelled.map((point) => ({
                label: formatDay(point.startedAtMs),
                value: activityValue(point, progress.activityMetric) ?? 0,
              }))}
              margin={{ top: 8, right: 8, bottom: 0, left: -12 }}
            >
              <CartesianGrid stroke="var(--fl-line)" strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fill: 'var(--fl-muted)', fontSize: 10 }} />
              <YAxis tick={{ fill: 'var(--fl-muted)', fontSize: 10 }} width={56} />
              <Tooltip
                contentStyle={{
                  background: 'var(--fl-surface)',
                  border: '1px solid var(--fl-line-strong)',
                  borderRadius: 12,
                  fontSize: 12,
                  color: 'var(--fl-ink)',
                }}
                labelStyle={{ color: 'var(--fl-muted)' }}
                formatter={(value) => [
                  formatActivityValue(Number(value ?? 0), progress.activityMetric),
                  ACTIVITY_METRIC_LABELS[progress.activityMetric],
                ]}
              />
              <Line
                type="monotone"
                dataKey="value"
                stroke="var(--fl-data)"
                strokeWidth={2}
                dot={{ r: 3, fill: 'var(--fl-data)' }}
                activeDot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Card>
      )}

      <ul className="flex flex-col gap-2">
        {[...progress.activityPoints].reverse().map((point) => {
          const value = activityValue(point, progress.activityMetric);
          return (
            <li
              key={point.startedAtMs}
              className="rounded-tile border-line bg-surface flex items-center justify-between gap-3 border px-4 py-2.5 text-xs"
            >
              <span className="text-muted">{formatDay(point.startedAtMs)}</span>
              <span className="text-ink fl-num">
                {value === null
                  ? `sin ${ACTIVITY_METRIC_LABELS[progress.activityMetric].toLowerCase()}`
                  : formatActivityValue(value, progress.activityMetric)}
              </span>
            </li>
          );
        })}
      </ul>
    </>
  );
}

/** Valor de la métrica de actividad ya formateado. */
function formatActivityValue(value: number, metric: ActivityMetric): string {
  if (metric === 'distance') return `${formatDecimal(value / 1000, 2)} km`;
  if (metric === 'duration') return formatDuration(value);
  return `${Math.round(value)} ppm`;
}

/** Evolución por ejercicio: métrica, rango y gráfico de líneas. */
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
  const previousPoint = progress.points[progress.points.length - 2];
  const lastValue = lastPoint ? metricValue(lastPoint, metric) : null;
  const previousValue = previousPoint ? metricValue(previousPoint, metric) : null;
  const delta =
    lastValue !== null && previousValue !== null && previousValue > 0
      ? ((lastValue - previousValue) / previousValue) * 100
      : null;

  return (
    <div className="flex flex-col gap-4">
      <select
        className={inputClass}
        value={selectedExerciseId}
        onChange={(event) => progress.selectExercise(event.target.value)}
        aria-label="Ejercicio"
      >
        {exercises.map((exercise) => (
          <option key={exercise.id} value={exercise.id}>
            {exercise.name}
          </option>
        ))}
      </select>

      <div className="fl-scroll-x flex gap-2 overflow-x-auto pb-1">
        {(Object.keys(metricLabels) as ProgressMetric[]).map((option) => (
          <Chip
            key={option}
            label={metricLabels[option]}
            active={metric === option}
            onClick={() => progress.selectMetric(option)}
          />
        ))}
      </div>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as RangePreset[]).map((option) => (
          <Chip
            key={option}
            label={presetLabels[option]}
            active={progress.preset === option}
            onClick={() => progress.selectPreset(option)}
          />
        ))}
      </div>

      {progress.error ? <ErrorState message={progress.error} /> : null}
      {progress.loading ? <LoadingState message="Calculando progreso…" /> : null}

      {!progress.loading && progress.points.length === 0 ? (
        <EmptyState
          title="Sin series en el rango"
          message={
            selectedExercise
              ? `No hay series registradas de "${selectedExercise.name}" en el rango elegido.`
              : 'Elegí un ejercicio para ver su progreso.'
          }
          {...(progress.preset === 'all'
            ? {}
            : { actionLabel: 'Ver todo', onAction: () => progress.selectPreset('all') })}
        />
      ) : null}

      {progress.activityPoints.length > 0 ? (
        <ActivitySection progress={progress} />
      ) : null}

      {progress.points.length > 0 ? (
        <>
          <div className="grid grid-cols-2 gap-3">
            <StatTile
              label={metricLabels[metric]}
              value={lastValue === null ? '—' : formatMetric(lastValue, metric)}
              icon={<IconChart className="size-4" />}
              tone="data"
              delta={delta}
            />
            <StatTile
              label="Sesiones en el rango"
              value={String(progress.points.length)}
              hint={selectedExercise ? selectedExercise.name : undefined}
            />
          </div>

          <Card className="!p-4">
            <ResponsiveContainer width="100%" height={224}>
              <LineChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: -12 }}>
                <CartesianGrid stroke="var(--fl-line)" strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fill: 'var(--fl-muted)', fontSize: 11 }} />
                <YAxis tick={{ fill: 'var(--fl-muted)', fontSize: 11 }} width={56} />
                <Tooltip
                  contentStyle={{
                    background: 'var(--fl-surface)',
                    border: '1px solid var(--fl-line-strong)',
                    borderRadius: 12,
                    fontSize: 12,
                    color: 'var(--fl-ink)',
                  }}
                  labelStyle={{ color: 'var(--fl-muted)' }}
                  formatter={(value) => [
                    formatMetric(Number(value ?? 0), metric),
                    metricLabels[metric],
                  ]}
                />
                <Line
                  type="monotone"
                  dataKey="value"
                  stroke="var(--fl-data)"
                  strokeWidth={2}
                  dot={{ r: 3, fill: 'var(--fl-data)' }}
                  activeDot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </Card>

          <SectionHeader title="Sesiones" trailing={`${progress.points.length} en el rango`} />
          <ul className="flex flex-col gap-2">
            {[...progress.points].reverse().map((point) => (
              <li
                key={point.sessionId}
                className="rounded-tile border-line bg-surface flex items-center justify-between gap-3 border px-4 py-2.5 text-xs"
              >
                <span className="text-muted">{formatDay(point.startedAtMs)}</span>
                <span className="text-ink fl-num">
                  {point.workingSets} series · {formatMetric(metricValue(point, metric), metric)}
                </span>
              </li>
            ))}
          </ul>
        </>
      ) : null}
    </div>
  );
}
