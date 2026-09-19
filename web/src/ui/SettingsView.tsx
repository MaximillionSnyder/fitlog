import { useDbStatusStore } from '@/state/dbStatus';
import type { ThemeChoice } from '@/ui/theme';
import { IconMoon, IconSun } from '@/ui/icons';
import { Card, LabeledValue, SectionHeader } from '@/ui/primitives';

const stateLabels: Record<string, string> = {
  iniciando: 'Iniciando…',
  listo: 'Lista',
  'sin-opfs': 'Sin OPFS',
  error: 'Error',
};

/**
 * Ajustes: tema de la app y estado de la base de datos.
 *
 * El estado de la base de datos vivía en Inicio ocupando el lugar del contenido útil; acá es
 * información de diagnóstico y no compite con el panel de entrenamiento.
 */
export function SettingsView({
  choice,
  onChoice,
}: {
  choice: ThemeChoice;
  onChoice: (choice: ThemeChoice) => void;
}) {
  const status = useDbStatusStore((state) => state.status);
  const options: readonly { id: ThemeChoice; label: string }[] = [
    { id: 'system', label: 'Auto' },
    { id: 'light', label: 'Claro' },
    { id: 'dark', label: 'Oscuro' },
  ];

  return (
    <div className="flex flex-col gap-4">
      <SectionHeader title="Apariencia" />

      <Card>
        <p className="text-ink text-sm font-semibold">Tema</p>
        <p className="text-muted mt-1 text-xs">
          Elegí cómo se ve FitLog en este dispositivo. En modo automático sigue al sistema.
        </p>
        <div
          role="radiogroup"
          aria-label="Tema"
          className="border-line mt-3 inline-flex rounded-field border p-1"
        >
          {options.map((option) => {
            const active = option.id === choice;
            return (
              <button
                key={option.id}
                type="button"
                role="radio"
                aria-checked={active}
                onClick={() => onChoice(option.id)}
                className={`rounded-field px-3 py-1.5 text-xs font-semibold transition ${
                  active ? 'bg-accent-soft text-accent-text' : 'text-muted hover:text-ink'
                }`}
              >
                {option.label}
              </button>
            );
          })}
        </div>
      </Card>

      <SectionHeader title="Datos" />

      <Card>
        <p className="text-ink flex items-center gap-2 text-sm font-semibold">
          {status.state === 'listo' ? (
            <IconSun className="text-warning size-4" />
          ) : (
            <IconMoon className="text-muted size-4" />
          )}
          Base de datos
        </p>
        <div className="mt-3 flex flex-col gap-2">
          <LabeledValue label="Estado" value={stateLabels[status.state] ?? status.state} />
          <LabeledValue
            label="Esquema"
            value={status.schemaVersion === undefined ? '—' : `v${status.schemaVersion}`}
          />
          <LabeledValue label="SQLite" value={status.sqliteVersion ?? '—'} />
          <LabeledValue label="Persistencia" value={status.persistence ?? '—'} />
        </div>
        {status.state === 'sin-opfs' ? (
          <p className="text-warning mt-3 text-xs">
            FitLog necesita OPFS (Origin Private File System) para guardar tus datos. Probá con una
            versión reciente de Chrome, Edge, Firefox o Safari.
          </p>
        ) : null}
        {status.state === 'error' && status.error ? (
          <p className="text-danger mt-3 text-xs">{status.error}</p>
        ) : null}
      </Card>
    </div>
  );
}
