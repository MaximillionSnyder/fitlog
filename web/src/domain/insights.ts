import { buildMuscleBalance, type ComparisonSetInput } from '@/domain/comparisons';
import { roundToTenth } from '@/domain/formulas';
import type { ProgressRange } from '@/domain/progress';

export type TipKind =
  | 'consistency'
  | 'incomplete_data'
  | 'imbalance'
  | 'progress'
  | 'stagnation';

export type TipSeverity = 'warning' | 'info' | 'success';

export interface Tip {
  readonly kind: TipKind;
  readonly severity: TipSeverity;
  readonly subject: string | null;
  readonly value: number;
  readonly message: string;
}

export interface InsightsInput {
  readonly sets: readonly ComparisonSetInput[];
  readonly groupByExercise: Readonly<Record<string, string>>;
  readonly range: ProgressRange;
  readonly periodDays: number;
}

export const TIP_LIMIT = 6;

export const THRESHOLDS = {
  progressMinSessions: 3,
  stagnationMinSessions: 4,
  stagnationMaxVariationPct: 0.5,
  imbalanceMinSharePct: 50,
  consistencyHighPerWeek: 3,
  consistencyLowPerWeek: 1.5,
  incompleteMinSets: 5,
  incompleteMinSharePct: 30,
} as const;

const severityOrder: Record<TipSeverity, number> = {
  warning: 0,
  info: 1,
  success: 2,
};

export function formatNumber(value: number): string {
  return String(roundToTenth(value));
}

function inRange(set: ComparisonSetInput, range: ProgressRange): boolean {
  if (range.fromMs !== null && set.startedAtMs < range.fromMs) {
    return false;
  }
  return set.startedAtMs <= range.toMs;
}

function isComplete(set: ComparisonSetInput): boolean {
  return set.weightKg !== null && set.reps !== null;
}

export function buildTips(input: InsightsInput): Tip[] {
  const { range, periodDays, groupByExercise } = input;
  const effective = input.sets.filter((set) => !set.isWarmup && inRange(set, range));
  const tips: Tip[] = [];

  if (effective.length === 0) {
    return tips;
  }

  const sessions = new Set(effective.map((set) => set.sessionId));
  const sessionStartById = new Map<string, number>();
  for (const set of effective) {
    const current = sessionStartById.get(set.sessionId);
    if (current === undefined || set.startedAtMs < current) {
      sessionStartById.set(set.sessionId, set.startedAtMs);
    }
  }

  const weeks = Math.max(1, periodDays / 7);
  const perWeek = sessions.size / weeks;
  if (perWeek >= THRESHOLDS.consistencyHighPerWeek) {
    tips.push({
      kind: 'consistency',
      severity: 'success',
      subject: null,
      value: roundToTenth(perWeek),
      message: `Buena frecuencia: ${formatNumber(perWeek)} sesiones por semana`,
    });
  } else if (perWeek < THRESHOLDS.consistencyLowPerWeek) {
    tips.push({
      kind: 'consistency',
      severity: 'warning',
      subject: null,
      value: sessions.size,
      message:
        sessions.size === 1
          ? 'Poca frecuencia: 1 sesión en el periodo'
          : `Poca frecuencia: ${sessions.size} sesiones en el periodo`,
    });
  }

  const incomplete = effective.filter((set) => !isComplete(set)).length;
  const incompleteShare = (incomplete / effective.length) * 100;
  if (
    effective.length >= THRESHOLDS.incompleteMinSets &&
    incompleteShare >= THRESHOLDS.incompleteMinSharePct
  ) {
    tips.push({
      kind: 'incomplete_data',
      severity: 'info',
      subject: null,
      value: roundToTenth(incompleteShare),
      message: `${formatNumber(incompleteShare)}% de las series no tienen peso y repeticiones`,
    });
  }

  const balance = buildMuscleBalance(input.sets, groupByExercise, range);
  if (balance.length >= 2) {
    const top = balance[0];
    if (top && top.sharePct >= THRESHOLDS.imbalanceMinSharePct) {
      tips.push({
        kind: 'imbalance',
        severity: 'warning',
        subject: top.muscleGroupSlug,
        value: top.sharePct,
        message: `Un grupo concentra ${formatNumber(top.sharePct)}% del volumen del periodo`,
      });
    }
  }

  const oneRepMaxBySession = new Map<string, Map<string, number>>();
  for (const set of effective) {
    if (!isComplete(set)) continue;
    const bySession = oneRepMaxBySession.get(set.exerciseId) ?? new Map<string, number>();
    const estimate = roundToTenth(
      (set.weightKg as number) * (1 + (set.reps as number) / 30)
    );
    const current = bySession.get(set.sessionId);
    if (current === undefined || estimate > current) {
      bySession.set(set.sessionId, estimate);
    }
    oneRepMaxBySession.set(set.exerciseId, bySession);
  }

  for (const [exerciseId, bySession] of oneRepMaxBySession) {
    const ordered = [...bySession.entries()].sort((a, b) => {
      const atA = sessionStartById.get(a[0]) ?? 0;
      const atB = sessionStartById.get(b[0]) ?? 0;
      if (atA !== atB) return atA - atB;
      return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0;
    });

    if (ordered.length < THRESHOLDS.progressMinSessions) {
      continue;
    }

    const first = ordered[0]?.[1] ?? 0;
    const last = ordered[ordered.length - 1]?.[1] ?? 0;
    if (first === 0) {
      continue;
    }

    const variationPct = roundToTenth(((last - first) / first) * 100);

    if (ordered.length >= THRESHOLDS.stagnationMinSessions &&
        Math.abs(variationPct) <= THRESHOLDS.stagnationMaxVariationPct) {
      tips.push({
        kind: 'stagnation',
        severity: 'warning',
        subject: exerciseId,
        value: ordered.length,
        message: `Sin progreso de 1RM en las últimas ${ordered.length} sesiones`,
      });
    } else if (variationPct > 0) {
      tips.push({
        kind: 'progress',
        severity: 'success',
        subject: exerciseId,
        value: variationPct,
        message: `Tu 1RM estimado subió ${formatNumber(variationPct)}% en ${ordered.length} sesiones`,
      });
    }
  }

  return tips.sort((a, b) => {
    const bySeverity = severityOrder[a.severity] - severityOrder[b.severity];
    if (bySeverity !== 0) return bySeverity;
    if (a.kind !== b.kind) return a.kind < b.kind ? -1 : 1;
    const subjectA = a.subject ?? '';
    const subjectB = b.subject ?? '';
    if (subjectA !== subjectB) return subjectA < subjectB ? -1 : 1;
    return 0;
  });
}
