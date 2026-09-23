import { SectionHeader } from '@/ui/primitives';
import type { RoutePoint } from '@/domain/route';

const WIDTH = 320;
const HEIGHT = 180;
const PADDING = 12;

/**
 * Trazado del recorrido.
 *
 * No es un mapa: un GPX trae las coordenadas, no las imágenes (esas vienen de un servicio de teselas
 * y necesitan conexión). El dibujo se escala solo para entrar en la tarjeta, con el inicio y el fin
 * marcados.
 */
export function RouteSketch({ route }: { route: readonly RoutePoint[] }) {
  if (route.length < 2) return null;

  const latitudes = route.map((point) => point.latitude);
  const longitudes = route.map((point) => point.longitude);
  const minLatitude = Math.min(...latitudes);
  const maxLatitude = Math.max(...latitudes);
  const minLongitude = Math.min(...longitudes);
  const maxLongitude = Math.max(...longitudes);

  const spanLatitude = maxLatitude - minLatitude || 1e-6;
  const spanLongitude = maxLongitude - minLongitude || 1e-6;
  const scale = Math.min(
    (WIDTH - PADDING * 2) / spanLongitude,
    (HEIGHT - PADDING * 2) / spanLatitude
  );
  const offsetX = (WIDTH - spanLongitude * scale) / 2;
  const offsetY = (HEIGHT - spanLatitude * scale) / 2;

  const project = (point: RoutePoint) => ({
    x: offsetX + (point.longitude - minLongitude) * scale,
    // La latitud crece hacia el norte y el SVG hacia abajo.
    y: offsetY + (maxLatitude - point.latitude) * scale,
  });

  const projected = route.map(project);
  const path = projected
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)} ${point.y.toFixed(1)}`)
    .join(' ');
  const start = projected[0]!;
  const end = projected[projected.length - 1]!;

  return (
    <div>
      <SectionHeader title="Recorrido" trailing={`${route.length} puntos`} />
      <div className="rounded-tile border-line bg-surface mt-3 border p-3">
        <svg
          viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
          className="h-44 w-full"
          role="img"
          aria-label="Trazado del recorrido"
        >
          <path
            d={path}
            fill="none"
            stroke="var(--fl-data)"
            strokeWidth={3}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx={start.x} cy={start.y} r={6} fill="var(--fl-success)" />
          <circle cx={end.x} cy={end.y} r={6} fill="var(--fl-danger)" />
        </svg>
        <p className="text-muted mt-2 text-xs">
          Inicio y fin marcados · el mapa con imágenes necesita conexión
        </p>
      </div>
    </div>
  );
}
