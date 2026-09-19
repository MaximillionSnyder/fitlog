/**
 * Formato de numeros de la interfaz.
 *
 * Punto como separador decimal y espacio fino para los miles, para que los valores se lean igual
 * en cualquier configuracion regional del navegador.
 */

const THIN_SPACE = '\u2009';

function groupThousands(value: string): string {
  const negative = value.startsWith('-');
  const digits = negative ? value.slice(1) : value;
  const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, THIN_SPACE);
  return negative ? `-${grouped}` : grouped;
}

export function formatInteger(value: number): string {
  return groupThousands(Math.round(value).toString());
}

/** Numero con la cantidad de decimales pedida, sin ceros de relleno. */
export function formatDecimal(value: number, decimals: number): string {
  const factor = 10 ** Math.min(Math.max(decimals, 0), 6);
  const rounded = Math.round(value * factor) / factor;
  const [whole = '0', fraction] = rounded.toString().split('.');
  const grouped = groupThousands(whole);
  return fraction ? `${grouped}.${fraction}` : grouped;
}

/** Peso en kg: un decimal cuando el valor no es entero. */
export function formatKg(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  return formatDecimal(value, 1);
}

/** Volumen en kg: sin decimales por encima de 100, para no ensuciar el numero. */
export function formatVolumeKg(value: number): string {
  return value >= 100 ? formatInteger(value) : formatDecimal(value, 1);
}

/** Duracion compacta: `45s`, `12m`, `1h 05m`. */
export function formatDuration(millis: number): string {
  const totalSeconds = Math.max(0, Math.floor(millis / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) return `${hours} h ${minutes.toString().padStart(2, '0')} m`;
  if (minutes > 0) return `${minutes} m`;
  return `${seconds} s`;
}

/** Duracion larga para las listas: `1 h 5 min`. */
export function formatDurationLong(startedAt: number, finishedAt: number | null): string {
  if (finishedAt === null) return '—';
  const minutes = Math.max(0, Math.floor((finishedAt - startedAt) / 60_000));
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return hours === 0 ? `${rest} min` : `${hours} h ${rest} min`;
}

/** Variacion porcentual, o `null` cuando no hay base de comparacion. */
export function deltaPercent(current: number, previous: number): number | null {
  if (previous <= 0) return null;
  return ((current - previous) / previous) * 100;
}

/** Texto de la variacion: `+12 %`, `-4 %`, `= 0 %`. */
export function deltaText(percent: number): string {
  const rounded = Math.round(percent);
  if (rounded > 0) return `+${rounded} %`;
  if (rounded < 0) return `${rounded} %`;
  return '= 0 %';
}

/** Fecha relativa breve para las listas: `Hoy`, `Ayer`, `12 mar`. */
export function formatRelativeDay(timestamp: number, now: number = Date.now()): string {
  const day = 86_400_000;
  const startOfToday = new Date(now).setHours(0, 0, 0, 0);
  const diff = Math.round((startOfToday - new Date(timestamp).setHours(0, 0, 0, 0)) / day);
  if (diff <= 0) return 'Hoy';
  if (diff === 1) return 'Ayer';
  if (diff < 7) return `Hace ${diff} días`;
  return new Intl.DateTimeFormat('es', { day: 'numeric', month: 'short' }).format(timestamp);
}
