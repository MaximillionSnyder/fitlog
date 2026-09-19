import { deltaPercent } from '@/domain/format';
import { DAY_MS } from '@/domain/progress';

/**
 * Agregacion del panel de Inicio.
 *
 * Es una funcion pura sobre las sesiones y las medidas que ya existen: no agrega consultas nuevas al
 * esquema ni columnas derivadas persistidas (el volumen sigue siendo un valor calculado).
 */

export const HOME_WINDOW_DAYS = 7;
export const HOME_WEEK_MS = HOME_WINDOW_DAYS * DAY_MS;

/** Punto de la tendencia de volumen: una sesion terminada. */
export interface HomeTrendPoint {
  readonly startedAtMs: number;
  readonly volumeKg: number;
}

export const TREND_LIMIT = 8;

/**
 * Tendencia de volumen de las ultimas sesiones terminadas, en orden cronologico.
 *
 * Solo entran sesiones finalizadas: una en curso todavia no tiene volumen definitivo y
 * distorsionaria la lectura de la tendencia.
 */
export function buildVolumeTrend(
  sessions: readonly HomeSessionInput[],
  limit: number = TREND_LIMIT
): HomeTrendPoint[] {
  return sessions
    .filter((session) => session.finishedAt !== null)
    .slice()
    .sort((a, b) => a.startedAt - b.startedAt)
    .slice(-limit)
    .map((session) => ({ startedAtMs: session.startedAt, volumeKg: session.volumeKg }));
}

/** Paso del recorrido de inicio, con su estado. */
export type HomeStepId = 'routine' | 'workout' | 'body_metric';

export interface HomeStep {
  readonly id: HomeStepId;
  readonly title: string;
  readonly description: string;
  readonly done: boolean;
}

export interface HomeSteps {
  readonly items: readonly HomeStep[];
  readonly doneCount: number;
  readonly total: number;
  readonly isComplete: boolean;
}

/**
 * Recorrido de los primeros pasos: armar una rutina, registrar un entrenamiento y cargar una
 * medida. La tarjeta desaparece cuando los tres estan hechos.
 */
export function buildHomeSteps(
  routineCount: number,
  sessionCount: number,
  bodyMetricCount: number
): HomeSteps {
  const items: HomeStep[] = [
    {
      id: 'routine',
      title: 'Armá una rutina',
      description: 'Definí los ejercicios, las series y las reps objetivo',
      done: routineCount > 0,
    },
    {
      id: 'workout',
      title: 'Registrá un entrenamiento',
      description: 'Series con peso, reps y RIR, con el descanso medido',
      done: sessionCount > 0,
    },
    {
      id: 'body_metric',
      title: 'Cargá una medida',
      description: 'Peso corporal o perímetros para seguir su evolución',
      done: bodyMetricCount > 0,
    },
  ];
  const doneCount = items.filter((item) => item.done).length;
  return { items, doneCount, total: items.length, isComplete: doneCount === items.length };
}

export interface HomeSessionInput {
  readonly id: string;
  readonly startedAt: number;
  readonly finishedAt: number | null;
  readonly workingSets: number;
  readonly volumeKg: number;
}

export interface HomeBodyInput {
  readonly measuredAt: number;
  readonly value: number;
}

export interface HomeWindow {
  readonly sessions: number;
  readonly volumeKg: number;
  readonly workingSets: number;
}

/**
 * Inicio del dia local: la app agrupa "Hoy" por el calendario del dispositivo, no por UTC. Como el
 * resultado depende de la zona horaria, se pasa como parametro con el mismo valor por defecto que
 * usa el panel.
 */
export type StartOfDay = (now: number) => number;

const localStartOfDay: StartOfDay = (now) => new Date(now).setHours(0, 0, 0, 0);

export interface HomeSummary {
  readonly current: HomeWindow;
  readonly previous: HomeWindow;
  /** Actividad desde la medianoche de hoy (incluye la sesion en curso). */
  readonly today: HomeWindow;
  readonly totalSessions: number;
  readonly streakWeeks: number;
  readonly latestBodyWeightKg: number | null;
  readonly latestBodyWeightAt: number | null;
  readonly volumeDeltaPercent: number | null;
  readonly sessionsDeltaPercent: number | null;
}

function windowOf(sessions: readonly HomeSessionInput[]): HomeWindow {
  return {
    sessions: sessions.length,
    volumeKg: sessions.reduce((total, session) => total + session.volumeKg, 0),
    workingSets: sessions.reduce((total, session) => total + session.workingSets, 0),
  };
}

/**
 * Semanas consecutivas con al menos una sesion, contando hacia atras desde la semana en curso. Si la
 * semana en curso todavia no tiene sesiones, la racha se mide desde la anterior.
 */
function streakWeeks(sessions: readonly HomeSessionInput[], now: number): number {
  if (sessions.length === 0) return 0;
  const weeks = new Set(
    sessions
      .map((session) => Math.floor((now - session.startedAt) / HOME_WEEK_MS))
      .filter((index) => index >= 0)
  );

  let streak = 0;
  let index = weeks.has(0) ? 0 : 1;
  while (weeks.has(index)) {
    streak += 1;
    index += 1;
  }
  return streak;
}

export function buildHomeSummary(
  sessions: readonly HomeSessionInput[],
  bodyPoints: readonly HomeBodyInput[],
  now: number,
  startOfDay: StartOfDay = localStartOfDay
): HomeSummary {
  const effective = sessions.filter((session) => session.startedAt <= now);
  const currentFrom = now - HOME_WEEK_MS;
  const previousFrom = now - 2 * HOME_WEEK_MS;

  const startOfToday = startOfDay(now);
  const today = windowOf(effective.filter((session) => session.startedAt >= startOfToday));

  const current = windowOf(effective.filter((session) => session.startedAt > currentFrom));
  const previous = windowOf(
    effective.filter(
      (session) => session.startedAt > previousFrom && session.startedAt <= currentFrom
    )
  );

  const latestBody = bodyPoints
    .filter((point) => point.measuredAt <= now)
    .reduce<HomeBodyInput | null>(
      (latest, point) => (latest === null || point.measuredAt > latest.measuredAt ? point : latest),
      null
    );

  return {
    current,
    previous,
    today,
    totalSessions: effective.length,
    streakWeeks: streakWeeks(effective, now),
    latestBodyWeightKg: latestBody?.value ?? null,
    latestBodyWeightAt: latestBody?.measuredAt ?? null,
    volumeDeltaPercent: deltaPercent(current.volumeKg, previous.volumeKg),
    sessionsDeltaPercent: deltaPercent(current.sessions, previous.sessions),
  };
}
