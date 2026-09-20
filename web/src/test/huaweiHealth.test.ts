import { describe, expect, it } from 'vitest';

import {
  huaweiNote,
  parseHuaweiExport,
  parseHuaweiFile,
  type HuaweiWorkout,
} from '@/domain/huaweiHealth';

const start = 1_700_000_000_000;

function activity(overrides: Record<string, unknown> = {}): string {
  return JSON.stringify({
    recordId: 'rec-1',
    startTime: start,
    endTime: start + 1_800_000,
    sportType: 4,
    totalTime: 1_800_000,
    totalCalories: 320_000,
    totalDistance: 5_240,
    totalSteps: 6_800,
    avgHeartRate: 145,
    maxHeartRate: 172,
    attribute: 'tp=lbs;k=1;lat=1.0;lon=2.0;alt=3.0;t=1.0;',
    ...overrides,
  });
}

function compact(recordId: string, startTime: number): string {
  return JSON.stringify({
    recordId,
    startTime,
    endTime: startTime + 1_800_000,
    sportType: 4,
    totalTime: 1_800_000,
    totalDistance: 5_240,
  });
}

function first(contents: readonly string[]): HuaweiWorkout {
  const workout = parseHuaweiExport(contents).workouts[0];
  if (!workout) throw new Error('no se reconoció ningún entrenamiento');
  return workout;
}

describe('parseHuaweiExport', () => {
  it('lee un entrenamiento con sus datos', () => {
    const result = parseHuaweiExport([activity()]);

    expect(result.workouts).toHaveLength(1);
    const workout = first([activity()]);
    expect(workout.recordId).toBe('rec-1');
    expect(workout.startedAtMs).toBe(start);
    expect(workout.finishedAtMs).toBe(start + 1_800_000);
    expect(workout.sportName).toBe('Running');
    expect(workout.durationMs).toBe(1_800_000);
    expect(workout.distanceM).toBe(5_240);
    expect(workout.calories).toBe(320);
    expect(workout.steps).toBe(6_800);
    expect(workout.averageHeartRate).toBe(145);
  });

  it('la exportación triplica los registros y se cuenta una sola vez', () => {
    expect(parseHuaweiExport([activity(), activity(), activity()]).workouts).toHaveLength(1);
  });

  it('lee la raíz como lista y como objeto con la lista adentro', () => {
    expect(parseHuaweiExport([`[${activity()}]`]).workouts).toHaveLength(1);
    expect(parseHuaweiExport([`{"sportRecords": [${activity()}]}`]).workouts).toHaveLength(1);
  });

  it('lee un archivo con un objeto por línea', () => {
    const content = `${compact('a', start)}\n${compact('b', start + 86_400_000)}`;
    expect(parseHuaweiExport([content]).workouts).toHaveLength(2);
  });

  it('un registro sin fecha se descarta', () => {
    expect(parseHuaweiExport(['{"recordId": "x", "sportType": 4}']).workouts).toHaveLength(0);
  });

  it('acepta epoch en segundos y fechas en texto', () => {
    const seconds = first([activity({ startTime: start / 1000, endTime: start / 1000 + 1800 })]);
    expect(seconds.startedAtMs).toBe(start);

    const text = first([
      '{"startTime": "2023-11-14T22:13:20", "totalTime": 600000, "sportType": 5}',
    ]);
    expect(text.startedAtMs).toBe(start);
    expect(text.durationMs).toBe(600_000);
  });

  it('los archivos que no son entrenamientos se ignoran sin error', () => {
    const result = parseHuaweiExport([
      activity(),
      // Con inicio y fin, como el sueño real: lo que lo descarta es no tener señal de deporte.
      `{"sleepRecords": [{"startTime": ${start}, "endTime": ${start + 28_800_000}, "deepSleep": 10}]}`,
      'no es json',
    ]);

    expect(result.workouts).toHaveLength(1);
    expect(result.filesRead + result.filesSkipped).toBe(3);
  });

  it('la nota resume el origen y los datos disponibles', () => {
    const note = huaweiNote(first([activity()]));

    expect(note.startsWith('Huawei Health · Running')).toBe(true);
    expect(note).toContain('5.24 km');
    expect(note).toContain('320 kcal');
    expect(note).toContain('FC 145/172');
    expect(note).toContain('6800 pasos');
  });

  it('la nota se limita a lo que existe', () => {
    const workout = first([
      `{"recordId": "solo", "startTime": ${start}, "totalTime": 600000, "sportType": 147}`,
    ]);

    expect(huaweiNote(workout)).toBe('Huawei Health · Entrenamiento de fuerza');
    expect(workout.distanceM).toBeNull();
  });

  it('parseHuaweiFile devuelve la lista de un archivo suelto', () => {
    expect(parseHuaweiFile(activity())).toHaveLength(1);
    expect(parseHuaweiFile('')).toHaveLength(0);
  });
});
