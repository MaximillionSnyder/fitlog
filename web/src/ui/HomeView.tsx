import { useEffect, useMemo, useState } from 'react';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

import {
  buildActivitySeries,
  type ActivityInput,
  type ActivityPoint,
} from '@/domain/activity';
import {
  buildHomeSteps,
  buildHomeSummary,
  buildVolumeTrend,
  type HomeSessionInput,
  type HomeSteps,
} from '@/domain/home';
import {
  formatDecimal,
  formatDuration,
  formatDurationLong,
  formatInteger,
  formatKg,
  formatVolumeKg,
  formatRelativeDay,
} from '@/domain/format';
import type { CatalogState } from '@/state/useCatalog';
import type { BodyMetricsState } from '@/state/useBodyMetrics';
import type { RoutinesState } from '@/state/useRoutines';
import type { WorkoutState } from '@/state/useWorkout';
import type { View } from '@/ui/destinations';
import {
  IconCalendar,
  IconCheck,
  IconCircle,
  IconChart,
  IconDumbbell,
  IconChevronRight,
  IconGrid,
  IconPlay,
  IconScale,
  IconSpark,
  IconTrophy,
} from '@/ui/icons';
import { importedDataSummary, isImportedNote } from '@/domain/importedWorkout';
import { FitLogLogo } from '@/ui/Logo';
import { Button, Card, LoadingState, NavigationRow, SectionHeader, StatTile } from '@/ui/primitives';

/**
 * Panel de Inicio: hero con la accion de entrenamiento, sesion activa, estadisticas de los ultimos
 * 7 dias comparadas con los 7 anteriores y accesos directos. No es un indice de navegacion: el menu
 * vive en la barra inferior y en "Más".
 */
export function HomeView({
  workout,
  body,
  catalog,
  routines,
  onNavigate,
}: {
  workout: WorkoutState;
  body: BodyMetricsState;
  catalog: CatalogState;
  routines: RoutinesState;
  onNavigate: (view: View) => void;
}) {
  const [now, setNow] = useState(() => Date.now());
  const [busy, setBusy] = useState(false);

  // La duracion de la sesion activa se refresca sola mientras hay una en curso.
  useEffect(() => {
    if (!workout.active) return;
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, [workout.active]);

  const summary = useMemo(() => {
    const sessions: HomeSessionInput[] = workout.history.map((session) => ({
      id: session.id,
      startedAt: session.startedAt,
      finishedAt: session.finishedAt,
      workingSets: session.summary.workingSets,
      volumeKg: session.summary.totalVolumeKg,
    }));
    const weights = body.metrics
      .filter((metric) => metric.kind === 'body_weight')
      .map((metric) => ({ measuredAt: metric.measuredAtMs, value: metric.value }));
    return buildHomeSummary(sessions, weights, now);
  }, [body.metrics, now, workout.history]);

  // Tendencia de volumen: el numero de la semana dice cuanto, esto dice hacia donde va.
  const trend = useMemo(() => {
    const sessions: HomeSessionInput[] = workout.history.map((session) => ({
      id: session.id,
      startedAt: session.startedAt,
      finishedAt: session.finishedAt,
      workingSets: session.summary.workingSets,
      volumeKg: session.summary.totalVolumeKg,
    }));
    return buildVolumeTrend(sessions);
  }, [workout.history]);

  // Actividad importada: lo que se muestra cuando la historia no tiene series con peso.
  const activityTrend = useMemo(() => {
    const sessions: ActivityInput[] = workout.history.map((session) => ({
      startedAtMs: session.startedAt,
      finishedAtMs: session.finishedAt,
      distanceM: session.activity?.distanceM ?? null,
      averageHeartRate: session.activity?.averageHeartRate ?? null,
    }));
    return buildActivitySeries(sessions, null).slice(-8);
  }, [workout.history]);

  const recent = useMemo(
    () => workout.history.filter((session) => session.finishedAt !== null).slice(0, 3),
    [workout.history]
  );

  const active = workout.active;
  const activeVolume = active ? active.summary.totalVolumeKg : 0;
  const activeSets = active ? active.summary.workingSets : 0;
  const steps = buildHomeSteps(
    routines.routines.length,
    summary.totalSessions,
    body.metrics.length
  );
  const hasVolume = trend.some((point) => point.volumeKg > 0);
  const greeting = greetingFor(now);
  const exerciseCount = catalog.snapshot.exercises.length;

  const startOrContinue = async () => {
    if (active) {
      onNavigate('entrenar');
      return;
    }
    setBusy(true);
    try {
      await workout.start(null);
      onNavigate('entrenar');
    } finally {
      setBusy(false);
    }
  };

  if (workout.loading) {
    return <LoadingState message="Preparando tu panel…" />;
  }

  return (
    <div className="flex flex-col gap-5">
      <Card
        className="relative overflow-hidden !p-6"
        tone="surface"
      >
        <div
          aria-hidden="true"
          className="bg-accent/20 pointer-events-none absolute -top-20 -right-16 size-52 rounded-full blur-3xl"
        />
        <div className="relative flex flex-col gap-3">
          <span className="text-accent-text flex items-center gap-2 text-[0.6875rem] font-semibold tracking-[0.14em]">
            <FitLogLogo className="size-4" />
            FITLOG
          </span>
          <div>
            <p className="text-muted text-sm">{greeting},</p>
            <p className="text-ink text-2xl font-semibold tracking-tight">
              {active ? 'Sesión en curso' : '¿Entrenamos?'}
            </p>
          </div>
          <p className="text-muted max-w-md text-sm">
            {active
              ? 'Retomá donde la dejaste: tu sesión sigue abierta.'
              : 'Registrá tus series y mirá cómo progresa tu volumen semana a semana.'}
          </p>
          <Button
            onClick={() => void startOrContinue()}
            disabled={busy}
            icon={active ? <IconPlay className="size-4" /> : <IconDumbbell className="size-4" />}
            className="mt-1 self-start"
          >
            {active ? 'Continuar sesión' : 'Iniciar entrenamiento'}
          </Button>
          {workout.error ? <p className="text-danger text-xs">{workout.error}</p> : null}
        </div>
      </Card>

      {active ? (
        <Card tone="accent">
          <div className="flex items-center gap-3">
            <IconDumbbell className="text-accent-text size-5 shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-ink text-sm font-semibold">
                {active.routineName ?? 'Entrenamiento libre'}
              </p>
              <p className="text-muted fl-num text-xs">
                {formatDuration(now - active.startedAt)} · {activeSets} series ·{' '}
                {formatVolumeKg(activeVolume)} kg
              </p>
            </div>
            <Button variant="secondary" onClick={() => onNavigate('entrenar')}>
              Abrir
            </Button>
          </div>
        </Card>
      ) : null}

      {summary.today.sessions > 0 ? (
        <div className="rounded-card border border-line bg-surface flex items-center justify-between gap-3 p-4">
          <div className="flex items-center gap-3">
            <span className="rounded-field bg-accent-soft text-accent-text grid size-9 place-items-center">
              <IconSpark className="size-4" />
            </span>
            <div>
              <p className="text-ink text-sm font-semibold">Hoy</p>
              <p className="text-muted fl-num text-xs">
                {summary.today.workingSets} series · {formatVolumeKg(summary.today.volumeKg)} kg
              </p>
            </div>
          </div>
          <Button variant="ghost" onClick={() => onNavigate('entrenar')}>
            {active ? 'Seguir' : 'Ver'}
          </Button>
        </div>
      ) : null}

      <SectionHeader title="Últimos 7 días" trailing="vs. 7 anteriores" />

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile
          label="Sesiones"
          value={String(summary.current.sessions)}
          icon={<IconCalendar className="size-4" />}
          delta={summary.sessionsDeltaPercent}
        />
        <StatTile
          label="Volumen"
          value={formatVolumeKg(summary.current.volumeKg)}
          unit="kg"
          tone="data"
          icon={<IconChart className="size-4" />}
          delta={summary.volumeDeltaPercent}
        />
        <StatTile
          label="Racha"
          value={String(summary.streakWeeks)}
          unit={summary.streakWeeks === 1 ? 'semana' : 'semanas'}
          tone="accent"
          icon={<IconSpark className="size-4" />}
          hint={`${summary.current.workingSets} series en la semana`}
        />
        <StatTile
          label="Peso corporal"
          value={formatKg(summary.latestBodyWeightKg)}
          {...(summary.latestBodyWeightKg === null ? {} : { unit: 'kg' })}
          icon={<IconScale className="size-4" />}
          hint={
            summary.latestBodyWeightAt === null
              ? 'Sin medidas todavía'
              : formatRelativeDay(summary.latestBodyWeightAt, now)
          }
        />
      </div>

      {hasVolume && trend.length > 1 ? (
        <Card className="!p-4">
          <SectionHeader title="Volumen por sesión" trailing={`${trend.length} últimas`} />
          <div className="mt-3">
            <ResponsiveContainer width="100%" height={160}>
              <LineChart
                data={trend.map((point) => ({
                  label: formatTrendDay(point.startedAtMs),
                  value: point.volumeKg,
                }))}
                margin={{ top: 8, right: 8, bottom: 0, left: -18 }}
              >
                <CartesianGrid stroke="var(--fl-line)" strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" tick={{ fill: 'var(--fl-muted)', fontSize: 10 }} />
                <YAxis tick={{ fill: 'var(--fl-muted)', fontSize: 10 }} width={48} />
                <Tooltip
                  contentStyle={{
                    background: 'var(--fl-surface)',
                    border: '1px solid var(--fl-line-strong)',
                    borderRadius: 12,
                    fontSize: 12,
                    color: 'var(--fl-ink)',
                  }}
                  labelStyle={{ color: 'var(--fl-muted)' }}
                  formatter={(value) => [`${formatVolumeKg(Number(value ?? 0))} kg`, 'Volumen']}
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
          </div>
        </Card>
      ) : null}

      {!hasVolume && activityTrend.length > 1 ? (
        <ActivityTrendCard points={activityTrend} />
      ) : null}

      {steps.isComplete ? null : (
        <Card tone="data">
          <SectionHeader
            title="Primeros pasos"
            trailing={`${steps.doneCount} de ${steps.total}`}
          />
          <ul className="mt-3 flex flex-col gap-3">
            {steps.items.map((step) => (
              <li key={step.id}>
                <button
                  type="button"
                  onClick={() => onNavigate(stepView(step.id))}
                  className="flex w-full items-center gap-3 text-left"
                >
                  {step.done ? (
                    <IconCheck className="text-success size-4 shrink-0" />
                  ) : (
                    <IconCircle className="text-muted size-4 shrink-0" />
                  )}
                  <span className="min-w-0 flex-1">
                    <span
                      className={`block text-sm font-semibold ${
                        step.done ? 'text-muted' : 'text-ink'
                      }`}
                    >
                      {step.title}
                    </span>
                    <span className="text-muted block text-xs">{step.description}</span>
                  </span>
                  <IconChevronRight className="text-muted size-4 shrink-0" />
                </button>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {recent.length > 0 ? (
        <>
          <SectionHeader title="Recientes" trailing={`${summary.totalSessions} en total`} />
          <div className="flex flex-col gap-3">
            {recent.map((session) => {
              const imported = isImportedNote(session.notes);
              const sport = imported
                ? (importedDataSummary(session.notes)?.split(' · ')[0] ?? 'Entrenamiento importado')
                : (session.routineName ?? 'Entrenamiento libre');
              return (
                <Card key={session.id} className="!p-4">
                  <div className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-ink flex items-center gap-2 truncate text-sm font-semibold">
                        {sport}
                        {imported ? (
                          <span className="bg-data-soft text-data rounded-lg px-2 py-0.5 text-[0.625rem] font-semibold">
                            IMPORTADO
                          </span>
                        ) : null}
                      </p>
                      <p className="text-muted text-xs">
                        {formatRelativeDay(session.startedAt, now)} ·{' '}
                        {formatDurationLong(session.startedAt, session.finishedAt)} ·{' '}
                        {/* Una sesión importada no tiene series: se muestran sus datos. */}
                        {imported
                          ? [
                              session.activity?.distanceM == null
                                ? null
                                : session.activity.distanceM >= 1000
                                  ? `${formatDecimal(session.activity.distanceM / 1000, 2)} km`
                                  : `${formatInteger(session.activity.distanceM)} m`,
                              session.activity?.averageHeartRate == null
                                ? null
                                : `FC ${formatInteger(session.activity.averageHeartRate)}`,
                              importedDataSummary(session.notes),
                            ]
                              .filter(Boolean)
                              .join(' · ')
                          : `${session.summary.workingSets} series`}
                      </p>
                    </div>
                    {imported ? null : (
                      <span className="text-ink fl-num text-sm font-semibold">
                        {formatVolumeKg(session.summary.totalVolumeKg)} kg
                      </span>
                    )}
                  </div>
                </Card>
              );
            })}
          </div>
        </>
      ) : null}

      <SectionHeader title="Accesos" />
      <div className="flex flex-col gap-3">
        <NavigationRow
          title="Rutinas"
          description="Tus días de entrenamiento con series y reps objetivo"
          icon={<IconCalendar className="size-5" />}
          onClick={() => onNavigate('rutinas')}
        />
        <NavigationRow
          title="Catálogo"
          description={`${exerciseCount} ejercicios con grupos musculares y equipamiento`}
          icon={<IconGrid className="size-5" />}
          onClick={() => onNavigate('catalogo')}
        />
        <NavigationRow
          title="Comparativas"
          description="Récords, mes contra mes y balance muscular"
          icon={<IconTrophy className="size-5" />}
          onClick={() => onNavigate('comparativas')}
        />
      </div>
    </div>
  );
}

/**
 * Tendencia de la actividad importada: distancia por entrenamiento y, si no hay distancia,
 * duración. Es lo que se muestra cuando la historia no tiene series con peso.
 */
function ActivityTrendCard({ points }: { points: readonly ActivityPoint[] }) {
  const withDistance = points.filter((point) => point.distanceM > 0).length > 1;
  const shown = withDistance
    ? points.filter((point) => point.distanceM > 0)
    : points.filter((point) => point.durationMs > 0);
  const values = shown.map((point) =>
    withDistance ? point.distanceM / 1000 : point.durationMs / 60_000
  );

  return (
    <Card className="!p-4">
      <SectionHeader
        title={withDistance ? 'Distancia por sesión' : 'Duración por sesión'}
        trailing={`${values.length} últimas`}
      />
      <div className="mt-3">
        <ResponsiveContainer width="100%" height={160}>
          <LineChart
            data={shown.map((point, index) => ({
              label: formatTrendDay(point.startedAtMs),
              value: values[index] ?? 0,
            }))}
            margin={{ top: 8, right: 8, bottom: 0, left: -18 }}
          >
            <CartesianGrid stroke="var(--fl-line)" strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="label" tick={{ fill: 'var(--fl-muted)', fontSize: 10 }} />
            <YAxis tick={{ fill: 'var(--fl-muted)', fontSize: 10 }} width={48} />
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
                withDistance
                  ? `${formatDecimal(Number(value ?? 0), 2)} km`
                  : formatDuration(Number(value ?? 0) * 60_000),
                withDistance ? 'Distancia' : 'Duración',
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
      </div>
    </Card>
  );
}

/** Dia corto de la tendencia: `12/3`. */
function formatTrendDay(timestamp: number): string {
  const date = new Date(timestamp);
  return `${date.getDate()}/${date.getMonth() + 1}`;
}

function stepView(id: HomeSteps['items'][number]['id']): View {
  if (id === 'routine') return 'rutinas';
  if (id === 'workout') return 'entrenar';
  return 'medidas';
}

function greetingFor(now: number): string {
  const hour = new Date(now).getHours();
  if (hour < 6) return 'Buenas noches';
  if (hour < 13) return 'Buenos días';
  if (hour < 20) return 'Buenas tardes';
  return 'Buenas noches';
}
