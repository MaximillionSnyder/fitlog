import { describe, expect, it } from 'vitest';

import { looksLikeGpx, parseGpx } from '@/domain/gpx';
import { importedWorkoutNote, isImportedNote } from '@/domain/importedWorkout';

const start = Date.UTC(2023, 10, 14, 22, 13, 20);
const end = Date.UTC(2023, 10, 14, 22, 43, 20);

/** GPX como el que exporta un reloj: dos puntos con tiempo, altura y frecuencia cardíaca. */
function gpx(
  options: { type?: string; withTime?: boolean; withHeartRate?: boolean; creator?: string | null } = {}
): string {
  const { type = 'running', withTime = true, withHeartRate = true, creator = 'Huawei Health' } = options;
  const creatorAttribute = creator === null ? '' : ` creator="${creator}"`;
  const hr = (value: number) =>
    withHeartRate
      ? `<extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>${value}</gpxtpx:hr></gpxtpx:TrackPointExtension></extensions>`
      : '';
  const time = (value: string) => (withTime ? `<time>${value}</time>` : '');

  return `<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1"${creatorAttribute} xmlns="http://www.topografix.com/GPX/1/1">
  <metadata><time>2023-11-14T22:13:20Z</time></metadata>
  <trk>
    <name>Entrenamiento de la mañana</name>
    <type>${type}</type>
    <trkseg>
      <trkpt lat="-34.6037" lon="-58.3816">
        <ele>25.0</ele>
        ${time('2023-11-14T22:13:20Z')}
        ${hr(120)}
      </trkpt>
      <trkpt lat="-34.6047" lon="-58.3826">
        <ele>32.0</ele>
        ${time('2023-11-14T22:43:20Z')}
        ${hr(170)}
      </trkpt>
    </trkseg>
  </trk>
</gpx>`;
}

function first(content: string, fileName?: string) {
  const workout = parseGpx(content, fileName)[0];
  if (!workout) throw new Error('no se reconoció ningún entrenamiento');
  return workout;
}

describe('parseGpx', () => {
  it('reconoce un gpx y no un json', () => {
    expect(looksLikeGpx(gpx())).toBe(true);
    expect(looksLikeGpx('{"recordId": "x", "startTime": 1}')).toBe(false);
  });

  it('lee el entrenamiento con su duración, distancia y frecuencia cardíaca', () => {
    const workout = first(gpx());

    expect(workout.startedAtMs).toBe(start);
    expect(workout.finishedAtMs).toBe(end);
    expect(workout.durationMs).toBe(1_800_000);
    expect(workout.sportName).toBe('Running');
    expect(workout.averageHeartRate).toBeCloseTo(145, 5);
    expect(workout.maxHeartRate).toBe(170);
    // Dos puntos a ~140 m: la distancia se calcula sumando los tramos.
    expect(workout.distanceM ?? 0).toBeGreaterThan(100);
    expect(workout.distanceM ?? 0).toBeLessThan(200);
    // El desnivel positivo entre 25 y 32 m.
    expect(workout.elevationGainM).toBeCloseTo(7, 5);
  });

  it('la nota dice que vino de un gpx y de qué reloj', () => {
    const note = importedWorkoutNote(first(gpx()));

    expect(note.startsWith('Huawei Health')).toBe(true);
    expect(note).toContain('Running');
    expect(note).toContain('144 m');
    expect(note).toContain('FC 145/170');
    expect(note).toContain('desnivel 7 m');
    expect(isImportedNote(note)).toBe(true);
  });

  it('sin creador conocido la nota dice GPX', () => {
    const note = importedWorkoutNote(first(gpx({ creator: null })));
    expect(note.startsWith('GPX · Running')).toBe(true);
  });

  it('un gpx sin tiempos no se importa', () => {
    expect(parseGpx(gpx({ withTime: false }))).toHaveLength(0);
  });

  it('un gpx con una sola marca de tiempo no se importa', () => {
    const unPunto = `<gpx version="1.1"><trk><trkseg>
      <trkpt lat="-34.6" lon="-58.3"><time>2023-11-14T22:13:20Z</time></trkpt>
    </trkseg></trk></gpx>`;

    expect(parseGpx(unPunto)).toHaveLength(0);
  });

  it('un gpx sin frecuencia cardíaca no inventa el dato', () => {
    const workout = first(gpx({ withHeartRate: false }));

    expect(workout.averageHeartRate).toBeNull();
    expect(workout.maxHeartRate).toBeNull();
  });

  it('traduce el tipo de deporte del gpx', () => {
    expect(first(gpx({ type: 'cycling' })).sportName).toBe('Bicicleta');
    expect(first(gpx({ type: 'hiking' })).sportName).toBe('Caminata');
  });

  it('sin tipo usa el nombre del archivo', () => {
    const sinTipo = gpx().replace('<type>running</type>', '').replace(
      '<name>Entrenamiento de la mañana</name>',
      ''
    );

    expect(first(sinTipo, 'salida_sabado.gpx').sportName).toBe('Salida sabado');
  });
});
