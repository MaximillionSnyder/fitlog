import { roundToTenth } from '@/domain/formulas';
import { DAY_MS, type ProgressRange } from '@/domain/progress';

export interface ComparisonSetInput {
  readonly exerciseId: string;
  readonly sessionId: string;
  readonly startedAtMs: number;
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly isWarmup: boolean;
}

export interface PersonalRecord {
  readonly exerciseId: string;
  readonly bestWeightKg: number;
  readonly bestWeightAtMs: number | null;
  readonly bestOneRepMaxKg: number;
  readonly bestOneRepMaxAtMs: number | null;
  readonly bestSessionVolumeKg: number;
  readonly bestSessionVolumeAtMs: number | null;
  readonly bestReps: number;
  readonly bestRepsAtMs: number | null;
}

export interface PeriodTotals {
  readonly volumeKg: number;
  readonly workingSets: number;
  readonly sessions: number;
}

export interface PeriodComparison {
  readonly current: PeriodTotals;
  readonly previous: PeriodTotals;
  readonly volumeDeltaPct: number | null;
  readonly setsDeltaPct: number | null;
  readonly sessionsDeltaPct: number | null;
}

export interface MuscleBalanceEntry {
  readonly muscleGroupSlug: string;
  readonly volumeKg: number;
  readonly sharePct: number;
}

interface SessionVolume {
  volumeKg: number;
  atMs: number;
}

interface RecordAccumulator {
  bestWeightKg: number | null;
  bestWeightAtMs: number | null;
  bestOneRepMaxKg: number | null;
  bestOneRepMaxAtMs: number | null;
  bestReps: number | null;
  bestRepsAtMs: number | null;
  sessions: Map<string, SessionVolume>;
}

function effectiveSets(sets: readonly ComparisonSetInput[]): ComparisonSetInput[] {
  return sets
    .filter((set) => !set.isWarmup)
    .slice()
    .sort((a, b) => {
      if (a.startedAtMs !== b.startedAtMs) return a.startedAtMs - b.startedAtMs;
      return a.sessionId < b.sessionId ? -1 : a.sessionId > b.sessionId ? 1 : 0;
    });
}

function isComplete(set: ComparisonSetInput): boolean {
  return set.weightKg !== null && set.reps !== null;
}

export function buildPersonalRecords(sets: readonly ComparisonSetInput[]): PersonalRecord[] {
  const byExercise = new Map<string, RecordAccumulator>();

  for (const set of effectiveSets(sets)) {
    let accumulator = byExercise.get(set.exerciseId);
    if (!accumulator) {
      accumulator = {
        bestWeightKg: null,
        bestWeightAtMs: null,
        bestOneRepMaxKg: null,
        bestOneRepMaxAtMs: null,
        bestReps: null,
        bestRepsAtMs: null,
        sessions: new Map(),
      };
      byExercise.set(set.exerciseId, accumulator);
    }

    if (set.reps !== null && (accumulator.bestReps === null || set.reps > accumulator.bestReps)) {
      accumulator.bestReps = set.reps;
      accumulator.bestRepsAtMs = set.startedAtMs;
    }

    if (!isComplete(set)) {
      continue;
    }

    const weightKg = set.weightKg as number;
    const reps = set.reps as number;

    if (accumulator.bestWeightKg === null || weightKg > accumulator.bestWeightKg) {
      accumulator.bestWeightKg = weightKg;
      accumulator.bestWeightAtMs = set.startedAtMs;
    }

    const oneRepMax = roundToTenth(weightKg * (1 + reps / 30));
    if (accumulator.bestOneRepMaxKg === null || oneRepMax > accumulator.bestOneRepMaxKg) {
      accumulator.bestOneRepMaxKg = oneRepMax;
      accumulator.bestOneRepMaxAtMs = set.startedAtMs;
    }

    const sessionVolume = accumulator.sessions.get(set.sessionId);
    if (sessionVolume) {
      sessionVolume.volumeKg += weightKg * reps;
    } else {
      accumulator.sessions.set(set.sessionId, {
        volumeKg: weightKg * reps,
        atMs: set.startedAtMs,
      });
    }
  }

  const records: PersonalRecord[] = [];
  for (const [exerciseId, accumulator] of byExercise) {
    let bestSessionVolumeKg = 0;
    let bestSessionVolumeAtMs: number | null = null;
    for (const session of accumulator.sessions.values()) {
      if (bestSessionVolumeAtMs === null || session.volumeKg > bestSessionVolumeKg) {
        bestSessionVolumeKg = session.volumeKg;
        bestSessionVolumeAtMs = session.atMs;
      }
    }

    records.push({
      exerciseId,
      bestWeightKg: accumulator.bestWeightKg ?? 0,
      bestWeightAtMs: accumulator.bestWeightAtMs,
      bestOneRepMaxKg: accumulator.bestOneRepMaxKg ?? 0,
      bestOneRepMaxAtMs: accumulator.bestOneRepMaxAtMs,
      bestSessionVolumeKg,
      bestSessionVolumeAtMs,
      bestReps: accumulator.bestReps ?? 0,
      bestRepsAtMs: accumulator.bestRepsAtMs,
    });
  }

  return records.sort((a, b) => {
    if (a.bestOneRepMaxKg !== b.bestOneRepMaxKg) {
      return b.bestOneRepMaxKg - a.bestOneRepMaxKg;
    }
    return a.exerciseId < b.exerciseId ? -1 : a.exerciseId > b.exerciseId ? 1 : 0;
  });
}

export function comparisonRanges(
  preset: '30d' | '90d',
  nowMs: number
): { current: ProgressRange; previous: ProgressRange } {
  const days = preset === '30d' ? 30 : 90;
  const spanMs = days * DAY_MS;
  return {
    current: { fromMs: nowMs - spanMs, toMs: nowMs },
    previous: { fromMs: nowMs - 2 * spanMs, toMs: nowMs - spanMs - 1 },
  };
}

function totalsFor(sets: readonly ComparisonSetInput[], range: ProgressRange): PeriodTotals {
  const sessions = new Set<string>();
  let volumeKg = 0;
  let workingSets = 0;

  for (const set of sets) {
    if (set.isWarmup) continue;
    if (set.startedAtMs < (range.fromMs ?? Number.MIN_SAFE_INTEGER)) continue;
    if (set.startedAtMs > range.toMs) continue;

    workingSets += 1;
    sessions.add(set.sessionId);
    if (isComplete(set)) {
      volumeKg += (set.weightKg as number) * (set.reps as number);
    }
  }

  return { volumeKg, workingSets, sessions: sessions.size };
}

function deltaPct(current: number, previous: number): number | null {
  if (previous === 0) {
    return null;
  }
  return roundToTenth(((current - previous) / previous) * 100);
}

export function comparePeriods(
  sets: readonly ComparisonSetInput[],
  current: ProgressRange,
  previous: ProgressRange
): PeriodComparison {
  const currentTotals = totalsFor(sets, current);
  const previousTotals = totalsFor(sets, previous);

  return {
    current: currentTotals,
    previous: previousTotals,
    volumeDeltaPct: deltaPct(currentTotals.volumeKg, previousTotals.volumeKg),
    setsDeltaPct: deltaPct(currentTotals.workingSets, previousTotals.workingSets),
    sessionsDeltaPct: deltaPct(currentTotals.sessions, previousTotals.sessions),
  };
}

export function buildMuscleBalance(
  sets: readonly ComparisonSetInput[],
  groupByExercise: Readonly<Record<string, string>>,
  range: ProgressRange | null = null
): MuscleBalanceEntry[] {
  const volumeByGroup = new Map<string, number>();
  let totalVolume = 0;

  for (const set of sets) {
    if (set.isWarmup || !isComplete(set)) continue;
    if (range !== null) {
      if (set.startedAtMs < (range.fromMs ?? Number.MIN_SAFE_INTEGER)) continue;
      if (set.startedAtMs > range.toMs) continue;
    }

    const slug = groupByExercise[set.exerciseId] ?? 'sin-grupo';
    const volume = (set.weightKg as number) * (set.reps as number);
    volumeByGroup.set(slug, (volumeByGroup.get(slug) ?? 0) + volume);
    totalVolume += volume;
  }

  if (totalVolume === 0) {
    return [];
  }

  return [...volumeByGroup.entries()]
    .map(([muscleGroupSlug, volumeKg]) => ({
      muscleGroupSlug,
      volumeKg,
      sharePct: roundToTenth((volumeKg / totalVolume) * 100),
    }))
    .sort((a, b) => {
      if (a.volumeKg !== b.volumeKg) return b.volumeKg - a.volumeKg;
      return a.muscleGroupSlug < b.muscleGroupSlug ? -1 : a.muscleGroupSlug > b.muscleGroupSlug ? 1 : 0;
    });
}

export function formatDelta(deltaPct: number | null): string {
  if (deltaPct === null) {
    return 'sin datos';
  }
  const sign = deltaPct > 0 ? '+' : '';
  return `${sign}${deltaPct}%`;
}
