import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';

import { AppShell } from '@/ui/AppShell';
import { HomeView } from '@/ui/HomeView';
import { MoreView } from '@/ui/MoreView';
import { SessionDetailView } from '@/ui/SessionDetailView';
import { SettingsView } from '@/ui/SettingsView';
import CatalogView from '@/ui/CatalogView';
import { ImportView } from '@/ui/ImportView';
import type { ImportState } from '@/state/useImport';
import WorkoutView from '@/ui/WorkoutView';
import type { RoutinesState } from '@/state/useRoutines';
import type { BodyMetricsState } from '@/state/useBodyMetrics';
import type { CatalogState } from '@/state/useCatalog';
import type { WorkoutState } from '@/state/useWorkout';

/**
 * Prueba de humo de la capa de interfaz.
 *
 * No hay navegador en este entorno, asi que las vistas se renderizan a HTML estatico: alcanza para
 * detectar errores de ejecucion (accesos indefinidos, props mal armadas) y para verificar que los
 * textos y las clases del sistema de diseno lleguen al marcado.
 */
const catalog = {
  snapshot: {
    groups: [{ id: 'g1', slug: 'pecho', name: 'Pecho', bodyRegion: 'torso' }],
    exercises: [
      {
        id: 'e1',
        slug: 'press-banca',
        name: 'Press banca',
        muscleGroupId: 'g1',
        secondaryMuscleGroupId: null,
        equipment: 'barra',
        kind: 'strength',
        isCustom: false,
      },
    ],
  },
  loading: false,
  error: null,
  create: async () => {},
  remove: async () => {},
  reload: async () => {},
} as unknown as CatalogState;

const emptyCatalog = {
  ...catalog,
  snapshot: { groups: [], exercises: [] },
} as unknown as CatalogState;

const body = {
  metrics: [],
  series: [],
  stats: { count: 0, first: null, latest: null, min: null, max: null, deltaAbs: null, deltaPct: null },
  kind: 'body_weight',
  preset: '90d',
  loading: false,
  error: null,
  selectKind: () => {},
  selectPreset: () => {},
  add: async () => {},
  edit: async () => {},
  remove: async () => {},
} as unknown as BodyMetricsState;

const workout = {
  active: null,
  activeSets: [],
  history: [],
  detail: null,
  loading: false,
  error: null,
  start: async () => {},
  finish: async () => {},
  add: async () => {},
  update: async () => {},
  remove: async () => {},
  openDetail: async () => {},
  closeDetail: () => {},
} as unknown as WorkoutState;

const routines = {
  routines: [],
  loading: false,
  error: null,
  create: async () => {},
  update: async () => {},
  remove: async () => {},
  addExercise: async () => {},
  removeExercise: async () => {},
  moveExercise: async () => {},
} as unknown as RoutinesState;

const activeWorkout = {
  ...workout,
  active: {
    id: 's1',
    startedAt: Date.now() - 600_000,
    finishedAt: null,
    notes: null,
    routineId: null,
    routineName: null,
    summary: { totalSets: 1, workingSets: 1, totalVolumeKg: 600, volumeByExercise: { e1: 600 } },
  },
  activeSets: [
    {
      id: 'set1',
      sessionId: 's1',
      exerciseId: 'e1',
      exerciseName: 'Press banca',
      setIndex: 1,
      weightKg: 60,
      reps: 10,
      rir: 2,
      isWarmup: false,
      notes: null,
      createdAtMs: Date.now() - 120_000,
    },
  ],
} as unknown as WorkoutState;

const importedWorkout = {
  ...workout,
  history: [
    {
      id: 's1',
      startedAt: Date.now() - 86_400_000,
      finishedAt: Date.now() - 82_800_000,
      notes: 'Huawei Health · Running · 5.24 km',
      routineId: null,
      routineName: null,
      summary: { totalSets: 0, workingSets: 0, totalVolumeKg: 0, volumeByExercise: {} },
      activity: {
        distanceM: 5_240,
        calories: 320,
        averageHeartRate: 147,
        maxHeartRate: 170,
        steps: 6_800,
        elevationGainM: 12,
        source: 'Huawei Health',
      },
    },
    {
      id: 's2',
      startedAt: Date.now() - 2 * 86_400_000,
      finishedAt: Date.now() - 2 * 86_400_000 + 2_700_000,
      notes: 'GPX · Bicicleta · 20 km',
      routineId: null,
      routineName: null,
      summary: { totalSets: 0, workingSets: 0, totalVolumeKg: 0, volumeByExercise: {} },
      activity: {
        distanceM: 20_000,
        calories: null,
        averageHeartRate: null,
        maxHeartRate: null,
        steps: null,
        elevationGainM: null,
        source: 'GPX',
      },
    },
  ],
} as unknown as WorkoutState;

const detailWorkout = {
  ...workout,
  detail: {
    session: {
      id: 's1',
      startedAt: Date.now() - 3_600_000,
      finishedAt: Date.now() - 1_800_000,
      notes: null,
      routineId: null,
      routineName: 'Día de empuje',
      summary: { totalSets: 2, workingSets: 1, totalVolumeKg: 600, volumeByExercise: { e1: 600 } },
    },
    sets: [
      {
        id: 'set1',
        sessionId: 's1',
        exerciseId: 'e1',
        exerciseName: 'Press banca',
        setIndex: 1,
        weightKg: 40,
        reps: 10,
        rir: null,
        isWarmup: true,
        notes: null,
        createdAtMs: Date.now() - 3_000_000,
      },
      {
        id: 'set2',
        sessionId: 's1',
        exerciseId: 'e1',
        exerciseName: 'Press banca',
        setIndex: 2,
        weightKg: 60,
        reps: 10,
        rir: 2,
        isWarmup: false,
        notes: null,
        createdAtMs: Date.now() - 2_900_000,
      },
    ],
  },
} as unknown as WorkoutState;

const importer = {
  step: 'preview',
  reading: false,
  importing: false,
  error: null,
  filesRead: 2,
  workouts: [
    {
      recordId: 'a',
      startedAtMs: Date.UTC(2023, 10, 14, 22, 13, 20),
      finishedAtMs: Date.UTC(2023, 10, 14, 22, 43, 20),
      sportType: 4,
      sportName: 'Running',
      durationMs: 1_800_000,
      distanceM: 5_240,
      calories: 320,
      steps: 6_800,
      averageHeartRate: 145,
      maxHeartRate: 172,
    },
  ],
  alreadyImported: 1,
  result: null,
  pending: 0,
  canImport: false,
  sports: [{ name: 'Running', count: 1 }],
  firstAtMs: Date.UTC(2023, 10, 14),
  lastAtMs: Date.UTC(2023, 10, 14),
  readFiles: async () => {},
  runImport: async () => {},
  reset: () => {},
} as unknown as ImportState;

describe('interfaz web', () => {
  it('el shell muestra los cinco destinos y la marca', () => {
    const html = renderToStaticMarkup(
      <AppShell
        view="inicio"
        onNavigate={() => {}}
        onBack={() => {}}
        canGoBack={false}
        isDark
        onToggleTheme={() => {}}
        children={<p>contenido</p>}
      />
    );

    for (const label of ['Inicio', 'Entrenar', 'Progreso', 'Rutinas', 'Más']) {
      expect(html).toContain(label);
    }
    expect(html).toContain('FitLog');
    expect(html).toContain('fl-app-bg');
  });

  it('el panel de Inicio invita a entrenar y muestra las estadísticas en cero', () => {
    const html = renderToStaticMarkup(
      <HomeView
        workout={workout}
        body={body}
        catalog={catalog}
        routines={routines}
        onNavigate={() => {}}
      />
    );

    expect(html).toContain('¿Entrenamos?');
    expect(html).toContain('Iniciar entrenamiento');
    expect(html).toContain('Últimos 7 días');
    expect(html).toContain('Racha');
    expect(html).toContain('Primeros pasos');
    expect(html).toContain('Armá una rutina');
    expect(html).toContain('0 de 3');
    expect(html).toContain('bg-surface');
  });

  it('el panel con historia importada muestra la tendencia de actividad', () => {
    const html = renderToStaticMarkup(
      <HomeView
        workout={importedWorkout}
        body={body}
        catalog={catalog}
        routines={routines}
        onNavigate={() => {}}
      />
    );

    expect(html).toContain('Distancia por sesión');
    // Sin series no hay volumen que graficar.
    expect(html).not.toContain('Volumen por sesión');
  });

  it('Más agrupa los destinos secundarios con su descripción', () => {
    const html = renderToStaticMarkup(<MoreView onNavigate={() => {}} />);

    for (const title of ['Catálogo', 'Medidas', 'Comparativas', 'Tips', 'Respaldo', 'Ajustes']) {
      expect(html).toContain(title);
    }
  });

  it('Entrenar precarga la última serie y ofrece repetirla', () => {
    const html = renderToStaticMarkup(
      <WorkoutView
        workout={activeWorkout}
        catalog={catalog}
        routines={routines}
        onOpenSessionDetail={() => {}}
      />
    );

    expect(html).toContain('Última:');
    expect(html).toContain('Repetir');
    expect(html).toContain('Registrar serie');
    expect(html).toContain('Press banca');
  });

  it('el detalle del entrenamiento agrupa las series por ejercicio', () => {
    const html = renderToStaticMarkup(<SessionDetailView workout={detailWorkout} />);

    expect(html).toContain('Día de empuje');
    expect(html).toContain('Volumen');
    expect(html).toContain('Press banca');
    expect(html).toContain('series efectivas');
    expect(html).toContain('CALENTAMIENTO');
  });

  it('el catálogo sin resultados ofrece limpiar los filtros', () => {
    const html = renderToStaticMarkup(<CatalogView catalog={emptyCatalog} />);

    expect(html).toContain('Sin resultados');
    expect(html).toContain('Limpiar filtros');
  });

  it('la importación muestra la vista previa antes de escribir', () => {
    const html = renderToStaticMarkup(
      <ImportView importer={importer} onOpenHistory={() => {}} />
    );

    expect(html).toContain('Qué se encontró');
    expect(html).toContain('Entrenamientos');
    expect(html).toContain('Running');
    expect(html).toContain('Archivos leídos');
    expect(html).toContain('No hay nada nuevo para importar');
  });

  it('Ajustes ofrece los tres modos de tema y el estado de la base', () => {
    const html = renderToStaticMarkup(<SettingsView choice="system" onChoice={() => {}} />);

    for (const label of ['Auto', 'Claro', 'Oscuro']) {
      expect(html).toContain(label);
    }
    expect(html).toContain('Base de datos');
  });
});
