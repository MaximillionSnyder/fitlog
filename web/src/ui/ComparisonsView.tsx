import { formatDelta } from '@/domain/comparisons';
import { formatKg as formatWeight } from '@/domain/format';
import type { CatalogState } from '@/state/useCatalog';
import type { ComparisonPreset, ComparisonsState } from '@/state/useComparisons';
import { IconTrophy } from '@/ui/icons';
import {
  Card,
  Chip,
  EmptyState,
  ErrorState,
  LoadingState,
  SectionHeader,
  StatTile,
} from '@/ui/primitives';

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

/** Un delta nulo o cero no se muestra como variación: va como texto de apoyo. */
function deltaOrNull(delta: number | null): number | null {
  return delta === null || delta === 0 ? null : delta;
}

/** Récords personales, comparación de periodos y balance muscular. */
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

  const comparison = comparisons.comparison;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as ComparisonPreset[]).map((option) => (
          <Chip
            key={option}
            label={presetLabels[option]}
            active={comparisons.preset === option}
            onClick={() => comparisons.selectPreset(option)}
          />
        ))}
      </div>

      {comparisons.error ? <ErrorState message={comparisons.error} /> : null}
      {comparisons.loading ? <LoadingState message="Calculando comparativas…" /> : null}

      {comparison ? (
        <div className="grid grid-cols-3 gap-3">
          <StatTile
            label="Volumen"
            value={formatWeight(comparison.current.volumeKg)}
            unit="kg"
            tone="data"
            delta={deltaOrNull(comparison.volumeDeltaPct)}
            hint={formatDelta(comparison.volumeDeltaPct)}
          />
          <StatTile
            label="Series efectivas"
            value={String(comparison.current.workingSets)}
            delta={deltaOrNull(comparison.setsDeltaPct)}
            hint={formatDelta(comparison.setsDeltaPct)}
          />
          <StatTile
            label="Sesiones"
            value={String(comparison.current.sessions)}
            delta={deltaOrNull(comparison.sessionsDeltaPct)}
            hint={formatDelta(comparison.sessionsDeltaPct)}
          />
        </div>
      ) : null}

      <SectionHeader title="Marcas personales" trailing={`${comparisons.records.length}`} />

      {!comparisons.loading && comparisons.records.length === 0 ? (
        <EmptyState
          title="Sin marcas todavía"
          message="Todavía no hay marcas registradas: cargá series con peso y reps para verlas acá."
        />
      ) : null}

      <ul className="flex flex-col gap-3">
        {comparisons.records.map((record) => (
          <li key={record.exerciseId}>
            <Card className="!p-4">
              <p className="text-ink flex items-center gap-2 text-sm font-semibold">
                <IconTrophy className="text-warning size-4 shrink-0" />
                {exerciseNameById.get(record.exerciseId) ?? record.exerciseId}
              </p>
              <dl className="mt-3 grid grid-cols-2 gap-2 text-xs">
                <RecordValue
                  label="Peso máximo"
                  value={formatWeight(record.bestWeightKg)}
                  date={formatDate(record.bestWeightAtMs)}
                />
                <RecordValue
                  label="1RM estimado"
                  value={formatWeight(record.bestOneRepMaxKg)}
                  date={formatDate(record.bestOneRepMaxAtMs)}
                />
                <RecordValue
                  label="Volumen de sesión"
                  value={formatWeight(record.bestSessionVolumeKg)}
                  date={formatDate(record.bestSessionVolumeAtMs)}
                />
                <RecordValue
                  label="Reps"
                  value={String(record.bestReps)}
                  date={formatDate(record.bestRepsAtMs)}
                />
              </dl>
            </Card>
          </li>
        ))}
      </ul>

      <SectionHeader title="Balance muscular del periodo" />

      {!comparisons.loading && comparisons.balance.length === 0 ? (
        <EmptyState
          title="Sin volumen en el periodo"
          message="No hay volumen registrado en el periodo elegido. Probá con un rango más amplio."
        />
      ) : null}

      <ul className="flex flex-col gap-3">
        {comparisons.balance.map((entry) => (
          <li key={entry.muscleGroupSlug} className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-ink">
                {groupNameBySlug.get(entry.muscleGroupSlug) ?? entry.muscleGroupSlug}
              </span>
              <span className="text-muted fl-num">
                {formatWeight(entry.volumeKg)} kg · {entry.sharePct} %
              </span>
            </div>
            <div className="bg-surface-high h-2 w-full overflow-hidden rounded-full">
              <div
                className="bg-data h-full rounded-full"
                style={{ width: `${Math.min(100, entry.sharePct)}%` }}
              />
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

function RecordValue({ label, value, date }: { label: string; value: string; date: string }) {
  return (
    <div>
      <dt className="text-faint">{label}</dt>
      <dd className="text-ink fl-num">
        {value} <span className="text-faint">· {date}</span>
      </dd>
    </div>
  );
}
