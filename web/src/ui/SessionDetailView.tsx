import type { WorkoutState } from '@/state/useWorkout';
import {
  formatDecimal,
  formatDuration,
  formatDurationLong,
  formatInteger,
  formatKg,
  formatVolumeKg,
} from '@/domain/format';
import { buildSessionPace } from '@/domain/workout';
import { IconCalendar, IconDumbbell, IconStop } from '@/ui/icons';
import {
  Card,
  EmptyState,
  ErrorState,
  LabeledValue,
  LoadingState,
  SectionHeader,
  StatTile,
} from '@/ui/primitives';

/**
 * Detalle de un entrenamiento: resumen de la sesion y sus series agrupadas por ejercicio.
 *
 * Antes vivia como una tarjeta desplegable dentro de Entrenar; ahora es una vista propia, igual que
 * en Android.
 */
export function SessionDetailView({ workout }: { workout: WorkoutState }) {
  const detail = workout.detail;

  if (workout.error) {
    return <ErrorState message={workout.error} />;
  }

  if (!detail) {
    return <LoadingState message="Cargando entrenamiento…" />;
  }

  const session = detail.session;
  const pace = buildSessionPace(
    detail.sets.map((set) => ({ createdAtMs: set.createdAtMs, isWarmup: set.isWarmup }))
  );
  const byExercise = new Map<string, typeof detail.sets>();
  for (const set of detail.sets) {
    const current = byExercise.get(set.exerciseName) ?? [];
    byExercise.set(set.exerciseName, [...current, set]);
  }

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="text-ink text-sm font-semibold">{formatDateTime(session.startedAt)}</p>
        <p className="text-muted text-xs">
          {session.routineName ?? 'Entrenamiento libre'}
          {session.finishedAt === null ? ' · en curso' : ''}
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
        <StatTile
          label="Duración"
          value={formatDurationLong(session.startedAt, session.finishedAt)}
          icon={<IconCalendar className="size-4" />}
        />
        <StatTile
          label="Series"
          value={formatInteger(session.summary.workingSets)}
          hint={`${formatInteger(session.summary.totalSets)} en total`}
          icon={<IconStop className="size-4" />}
        />
        <StatTile
          label="Volumen"
          value={formatVolumeKg(session.summary.totalVolumeKg)}
          unit="kg"
          tone="data"
          icon={<IconDumbbell className="size-4" />}
        />
      </div>

      {pace.workingSets > 0 ? (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
          <StatTile
            label="Duración real"
            value={formatDuration(pace.spanMs)}
            hint="de la primera a la última serie"
          />
          {pace.setsPerHour === null ? null : (
            <StatTile
              label="Ritmo"
              value={formatDecimal(pace.setsPerHour, 1)}
              unit="series/h"
            />
          )}
          {pace.averageRestMs === null ? null : (
            <StatTile
              label="Descanso promedio"
              value={formatDuration(pace.averageRestMs)}
              hint="entre series efectivas"
            />
          )}
        </div>
      ) : null}

      {session.notes ? (
        <Card className="!p-4">
          <LabeledValue label="Notas" value={session.notes} />
        </Card>
      ) : null}

      <SectionHeader title="Series" trailing={`${formatInteger(detail.sets.length)} registradas`} />

      {detail.sets.length === 0 ? (
        <Card className="!p-0">
          <EmptyState title="Sin series" message="Esta sesión no tiene series registradas." />
        </Card>
      ) : (
        [...byExercise.entries()].map(([exerciseName, sets]) => (
          <div key={exerciseName} className="flex flex-col gap-3">
            <Card className="!p-4">
              <p className="text-ink text-sm font-semibold">{exerciseName}</p>
              <p className="text-muted fl-num text-xs">
                {formatInteger(sets.filter((set) => !set.isWarmup).length)} series efectivas
                {sets.some((set) => set.isWarmup)
                  ? ` · ${formatInteger(sets.filter((set) => set.isWarmup).length)} de calentamiento`
                  : ''}
              </p>
            </Card>
            <ul className="flex flex-col gap-2">
              {sets.map((set) => (
                <li
                  key={set.id}
                  className="rounded-tile border-line bg-surface flex items-center justify-between gap-3 border px-4 py-2.5"
                >
                  <span className="flex items-center gap-2">
                    <span className="text-faint fl-num text-xs">#{set.setIndex}</span>
                    {set.isWarmup ? (
                      <span className="bg-warning-soft text-warning rounded-lg px-2 py-0.5 text-[0.625rem] font-semibold">
                        CALENTAMIENTO
                      </span>
                    ) : null}
                  </span>
                  <span className="text-ink fl-num text-sm font-semibold">
                    {formatKg(set.weightKg)} kg × {set.reps ?? '—'}
                    {set.rir !== null ? ` · RIR ${set.rir}` : ''}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        ))
      )}
    </div>
  );
}

function formatDateTime(timestamp: number): string {
  return new Intl.DateTimeFormat('es', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp);
}
