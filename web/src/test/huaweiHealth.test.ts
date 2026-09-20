import { describe, expect, it } from 'vitest';

import {
  huaweiNote,
  parseHuaweiExport,
  parseHuaweiFile,
} from '@/domain/huaweiHealth';
import type { ImportedWorkout } from '@/domain/importedWorkout';

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

function first(contents: readonly string[]): ImportedWorkout {
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

  it('deriva la frecuencia cardíaca del blob de sensores', () => {
    // La exportación documenta avgHeartRate, pero en la práctica viene vacío: el dato real está en
    // los segmentos tp=h-r;k=<minuto>;v=<pulsaciones>; del blob.
    const attribute =
      'tp=lbs;k=1;lat=-34.6;lon=-58.3;alt=25.0;t=1.0;' +
      'tp=h-r;k=0;v=98;k=1;v=120;k=2;v=140;k=3;v=165;' +
      'tp=rs;k=0;v=25;';

    const workout = first([
      JSON.stringify({
        recordId: 'con-track',
        startTime: start,
        totalTime: 1_800_000,
        sportType: 4,
        attribute,
      }),
    ]);

    expect(workout.averageHeartRate).toBeCloseTo(130.75, 5);
    expect(workout.maxHeartRate).toBe(165);
  });

  it('sin lecturas de frecuencia cardíaca no inventa el dato', () => {
    const workout = first([
      JSON.stringify({
        recordId: 'sin-fc',
        startTime: start,
        totalTime: 600_000,
        sportType: 5,
        attribute: 'tp=lbs;k=1;lat=1.0;lon=2.0;',
      }),
    ]);

    expect(workout.averageHeartRate).toBeNull();
    expect(workout.maxHeartRate).toBeNull();
  });

  it('la frecuencia cardíaca de resumen gana sobre la del blob', () => {
    const workout = first([
      JSON.stringify({
        recordId: 'resumen',
        startTime: start,
        totalTime: 600_000,
        sportType: 4,
        avgHeartRate: 150,
        maxHeartRate: 180,
        attribute: 'tp=h-r;k=0;v=90;',
      }),
    ]);

    expect(workout.averageHeartRate).toBe(150);
    expect(workout.maxHeartRate).toBe(180);
  });

  it('un archivo por minuto con calorías no es un entrenamiento', () => {
    // Tiene fecha y calorías, pero no duración ni fin: no es una sesión.
    const result = parseHuaweiExport([
      `{"sportPerMinute": [{"startTime": ${start}, "steps": 120, "calories": 8}]}`,
    ]);

    expect(result.workouts).toHaveLength(0);
  });

  it('la distancia redonda se muestra sin decimales de relleno', () => {
    const workout = first([activity({ totalDistance: 20_000 })]);
    expect(huaweiNote(workout)).toContain('20 km');
  });

  it('lee objetos concatenados sin saltos de línea', () => {
    // La exportación parte los archivos con marcadores de resincronización y puede quedar todo
    // pegado: el escaneo por llaves balanceadas los recupera.
    const content =
      compact('a', start).replaceAll('\n', '') +
      compact('b', start + 86_400_000).replaceAll('\n', '');

    expect(parseHuaweiExport([content]).workouts).toHaveLength(2);
  });

  it('lee un archivo con una comilla suelta en el blob de sensores', () => {
    // La telemetría a veces trae una comilla sin escapar que invalida el JSON entero.
    const content =
      `{"recordId": "q", "startTime": ${start}, "totalTime": 600000, ` +
      `"sportType": 4, "attribute": "tp=lbs;k=1;lat=1.0;"x";lon=2.0;"}`;

    const result = parseHuaweiExport([content]);

    expect(result.workouts).toHaveLength(1);
    expect(result.workouts[0]!.recordId).toBe('q');
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
