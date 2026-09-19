import { useEffect, useMemo, useState } from 'react';

import { buildHomeSummary, type HomeSessionInput } from '@/domain/home';
import {
  formatDuration,
  formatDurationLong,
  formatKg,
  formatVolumeKg,
  formatRelativeDay,
} from '@/domain/format';
import type { CatalogState } from '@/state/useCatalog';
import type { BodyMetricsState } from '@/state/useBodyMetrics';
import type { WorkoutState } from '@/state/useWorkout';
import type { View } from '@/ui/destinations';
import {
  IconCalendar,
  IconChart,
  IconDumbbell,
  IconGrid,
  IconPlay,
  IconScale,
  IconSpark,
  IconTrophy,
} from '@/ui/icons';
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
  onNavigate,
}: {
  workout: WorkoutState;
  body: BodyMetricsState;
  catalog: CatalogState;
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

  const recent = useMemo(
    () => workout.history.filter((session) => session.finishedAt !== null).slice(0, 3),
    [workout.history]
  );

  const active = workout.active;
  const activeVolume = active ? active.summary.totalVolumeKg : 0;
  const activeSets = active ? active.summary.workingSets : 0;
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

      {summary.totalSessions === 0 ? (
        <Card tone="data">
          <p className="text-ink text-sm font-semibold">Tu primer entrenamiento</p>
          <p className="text-muted mt-1 text-sm">
            Armá una rutina o iniciá una sesión libre. Después vas a ver acá tu volumen, tu racha y tu
            evolución.
          </p>
          <Button
            variant="secondary"
            className="mt-3"
            onClick={() => onNavigate('rutinas')}
            icon={<IconCalendar className="size-4" />}
          >
            Crear una rutina
          </Button>
        </Card>
      ) : null}

      {recent.length > 0 ? (
        <>
          <SectionHeader title="Recientes" trailing={`${summary.totalSessions} en total`} />
          <div className="flex flex-col gap-3">
            {recent.map((session) => (
              <Card key={session.id} className="!p-4">
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-ink truncate text-sm font-semibold">
                      {session.routineName ?? 'Entrenamiento libre'}
                    </p>
                    <p className="text-muted text-xs">
                      {formatRelativeDay(session.startedAt, now)} ·{' '}
                      {formatDurationLong(session.startedAt, session.finishedAt)} ·{' '}
                      {session.summary.workingSets} series
                    </p>
                  </div>
                  <span className="text-ink fl-num text-sm font-semibold">
                    {formatVolumeKg(session.summary.totalVolumeKg)} kg
                  </span>
                </div>
              </Card>
            ))}
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

function greetingFor(now: number): string {
  const hour = new Date(now).getHours();
  if (hour < 6) return 'Buenas noches';
  if (hour < 13) return 'Buenos días';
  if (hour < 20) return 'Buenas tardes';
  return 'Buenas noches';
}
