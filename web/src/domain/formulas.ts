export function volumeKg(weightKg: number | null, reps: number | null): number | null {
  if (weightKg === null || reps === null) {
    return null;
  }
  return weightKg * reps;
}

export function estimatedOneRepMaxKg(weightKg: number | null, reps: number | null): number | null {
  if (weightKg === null || reps === null || reps <= 0) {
    return null;
  }
  return roundToTenth(weightKg * (1 + reps / 30));
}

export function roundToTenth(value: number): number {
  return Math.round(value * 10) / 10;
}
