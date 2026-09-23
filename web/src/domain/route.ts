/**
 * Ruta de un entrenamiento con GPS: los puntos del archivo, ya recortados.
 *
 * Un GPX no trae un mapa (el mapa son imágenes de un servicio de teselas): trae las coordenadas. Con
 * ellas se dibuja el trazado y los perfiles de altura y pulso, sin conexión.
 */

/** Tope de puntos guardados: alcanza para el trazado y no infla la base. */
export const MAX_ROUTE_POINTS = 300;

export interface RoutePoint {
  readonly latitude: number;
  readonly longitude: number;
  readonly elevation: number | null;
  readonly heartRate: number | null;
}

/** Recorta la ruta a [MAX_ROUTE_POINTS] puntos, repartidos parejo y conservando el primero y el último. */
export function simplifyRoute(points: readonly RoutePoint[]): RoutePoint[] {
  const valid = points.filter(
    (point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude)
  );
  if (valid.length <= MAX_ROUTE_POINTS) return valid;

  const step = (valid.length - 1) / (MAX_ROUTE_POINTS - 1);
  return Array.from({ length: MAX_ROUTE_POINTS }, (_, index) => {
    const position = Math.min(Math.trunc(index * step), valid.length - 1);
    return valid[position]!;
  });
}

/**
 * Texto compacto de la ruta: `lat,lon,ele,hr;lat,lon,ele,hr`.
 *
 * Los campos vacíos son `null`. Es el mismo formato en Android y en la web, y no necesita ninguna
 * librería para leerse.
 */
export function encodeRoute(points: readonly RoutePoint[]): string {
  return points
    .map((point) =>
      [
        format(point.latitude, 5),
        format(point.longitude, 5),
        point.elevation === null ? '' : format(point.elevation, 1),
        point.heartRate === null ? '' : format(point.heartRate, 0),
      ].join(',')
    )
    .join(';');
}

export function decodeRoute(text: string | null | undefined): RoutePoint[] {
  if (typeof text !== 'string' || text.trim() === '') return [];
  const points: RoutePoint[] = [];
  for (const chunk of text.split(';')) {
    const fields = chunk.split(',');
    const latitude = Number(fields[0]);
    const longitude = Number(fields[1]);
    if (fields[0] === undefined || fields[1] === undefined) continue;
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) continue;
    const elevation = Number(fields[2]);
    const heartRate = Number(fields[3]);
    points.push({
      latitude,
      longitude,
      elevation: fields[2] === '' || fields[2] === undefined || !Number.isFinite(elevation) ? null : elevation,
      heartRate: fields[3] === '' || fields[3] === undefined || !Number.isFinite(heartRate) ? null : heartRate,
    });
  }
  return points;
}

/** Distancia acumulada de la ruta, en metros (semiverseno). */
export function routeDistanceM(points: readonly RoutePoint[]): number {
  let total = 0;
  for (let index = 1; index < points.length; index += 1) {
    total += haversineM(
      points[index - 1]!.latitude,
      points[index - 1]!.longitude,
      points[index]!.latitude,
      points[index]!.longitude
    );
  }
  return total;
}

function haversineM(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRadians = (value: number) => (value * Math.PI) / 180;
  const deltaLat = toRadians(lat2 - lat1);
  const deltaLon = toRadians(lon2 - lon1);
  const a =
    Math.sin(deltaLat / 2) ** 2 +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(deltaLon / 2) ** 2;
  return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function format(value: number, decimals: number): string {
  return value.toFixed(decimals);
}
