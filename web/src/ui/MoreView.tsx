import { secondaryDestinations, type View } from '@/ui/destinations';
import { IconSpark } from '@/ui/icons';
import { Card, NavigationRow, SectionHeader } from '@/ui/primitives';

/**
 * Pantalla "Más": agrupa los destinos que no son de primer nivel.
 *
 * La barra inferior se queda con lo cotidiano (Inicio, Entrenar, Progreso, Rutinas) y el resto vive
 * acá con una descripción, para que la navegación principal no tenga nueve destinos.
 */
export function MoreView({ onNavigate }: { onNavigate: (view: View) => void }) {
  const explore = secondaryDestinations.filter((destination) => destination.id !== 'ajustes');
  const app = secondaryDestinations.filter((destination) => destination.id === 'ajustes');

  return (
    <div className="flex flex-col gap-4">
      <Card tone="data">
        <div className="flex items-center gap-3">
          <span className="rounded-field bg-surface text-data grid size-10 shrink-0 place-items-center">
            <IconSpark className="size-5" />
          </span>
          <div>
            <p className="text-ink text-sm font-semibold">Todo en un solo lugar</p>
            <p className="text-muted text-xs">
              Catálogo, medidas, comparativas, tips y respaldo.
            </p>
          </div>
        </div>
      </Card>

      <SectionHeader title="Explorar" />
      <div className="flex flex-col gap-3">
        {explore.map((destination) => (
          <NavigationRow
            key={destination.id}
            title={destination.title}
            description={destination.description}
            icon={destination.icon}
            onClick={() => onNavigate(destination.id)}
          />
        ))}
      </div>

      <SectionHeader title="App" />
      <div className="flex flex-col gap-3">
        {app.map((destination) => (
          <NavigationRow
            key={destination.id}
            title={destination.title}
            description={destination.description}
            icon={destination.icon}
            onClick={() => onNavigate(destination.id)}
          />
        ))}
      </div>
    </div>
  );
}
