import { useCallback, useState } from 'react';

import { useDatabase } from '@/db/bootstrap';
import { useImport } from '@/state/useImport';
import { useBackup } from '@/state/useBackup';
import { useBodyMetrics } from '@/state/useBodyMetrics';
import { useCatalog } from '@/state/useCatalog';
import { useComparisons } from '@/state/useComparisons';
import { useProgress } from '@/state/useProgress';
import { useRoutines } from '@/state/useRoutines';
import { useTips } from '@/state/useTips';
import { useWorkout } from '@/state/useWorkout';
import { AppShell } from '@/ui/AppShell';
import { HomeView } from '@/ui/HomeView';
import { ImportView } from '@/ui/ImportView';
import { MoreView } from '@/ui/MoreView';
import { SessionDetailView } from '@/ui/SessionDetailView';
import { SettingsView } from '@/ui/SettingsView';
import BodyMetricsView from '@/ui/BodyMetricsView';
import BackupView from '@/ui/BackupView';
import CatalogView from '@/ui/CatalogView';
import ComparisonsView from '@/ui/ComparisonsView';
import ProgressView from '@/ui/ProgressView';
import RoutinesView from '@/ui/RoutinesView';
import TipsView from '@/ui/TipsView';
import WorkoutView from '@/ui/WorkoutView';
import type { View } from '@/ui/destinations';
import { isTopLevel } from '@/ui/destinations';
import { useTheme } from '@/ui/theme';

/**
 * Raiz de la app web: un solo estado de navegacion, los hooks de datos y el shell.
 *
 * El shell provee el encabezado y la navegacion (rail en escritorio, barra inferior en movil); las
 * vistas ya no dibujan su propio encabezado.
 */
export default function App() {
  const db = useDatabase();
  const catalog = useCatalog(db);
  const workout = useWorkout(db);
  const routines = useRoutines(db);
  const progress = useProgress(db);
  const comparisons = useComparisons(db);
  const tips = useTips(db);
  const body = useBodyMetrics(db);
  const backup = useBackup(db);
  const importer = useImport(db);
  const { choice, isDark, setChoice, toggle } = useTheme();

  const [view, setView] = useState<View>('inicio');
  const [history, setHistory] = useState<readonly View[]>([]);

  const navigate = useCallback(
    (next: View) => {
      if (next === view) return;
      if (isTopLevel(next)) {
        // Cambiar de pestana reinicia el recorrido: la barra inferior no es una pila.
        setHistory([]);
      } else {
        setHistory((current) => [...current, view]);
      }
      setView(next);
      window.scrollTo({ top: 0 });
    },
    [view]
  );

  // El detalle del entrenamiento se carga antes de entrar a la vista, asi no hay efecto de carga.
  const openSessionDetail = useCallback(
    (id: string) => {
      void workout.openDetail(id);
      setHistory((current) => [...current, view]);
      setView('sesion');
      window.scrollTo({ top: 0 });
    },
    [view, workout]
  );

  const goBack = useCallback(() => {
    setHistory((current) => {
      if (current.length === 0) {
        setView('inicio');
        return current;
      }
      setView(current[current.length - 1] ?? 'inicio');
      return current.slice(0, -1);
    });
  }, []);

  return (
    <AppShell
      view={view}
      onNavigate={navigate}
      onBack={goBack}
      canGoBack={history.length > 0}
      isDark={isDark}
      onToggleTheme={toggle}
    >
      {view === 'inicio' && (
        <HomeView
          workout={workout}
          body={body}
          catalog={catalog}
          routines={routines}
          onNavigate={navigate}
        />
      )}
      {view === 'entrenar' && (
        <WorkoutView
          workout={workout}
          catalog={catalog}
          routines={routines}
          onOpenSessionDetail={openSessionDetail}
        />
      )}
      {view === 'sesion' && <SessionDetailView workout={workout} />}
      {view === 'progreso' && <ProgressView progress={progress} catalog={catalog} />}
      {view === 'rutinas' && (
        <RoutinesView routines={routines} catalog={catalog} workout={workout} />
      )}
      {view === 'mas' && <MoreView onNavigate={navigate} />}
      {view === 'catalogo' && <CatalogView catalog={catalog} />}
      {view === 'comparativas' && (
        <ComparisonsView
          comparisons={comparisons}
          catalog={catalog}
          onOpenWorkout={() => navigate('entrenar')}
        />
      )}
      {view === 'tips' && <TipsView tips={tips} catalog={catalog} />}
      {view === 'medidas' && <BodyMetricsView body={body} />}
      {view === 'respaldo' && <BackupView backup={backup} />}
      {view === 'importar' && (
        <ImportView importer={importer} onOpenHistory={() => navigate('entrenar')} />
      )}
      {view === 'ajustes' && (
        <SettingsView choice={choice} onChoice={setChoice} workout={workout} />
      )}
    </AppShell>
  );
}
