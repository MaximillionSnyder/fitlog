/**
 * Marca de FitLog: barra cargada (izquierda) y barra con progreso (derecha) sobre una linea base.
 *
 * Es el mismo lenguaje geometrico que los iconos, con el acento de marca como color de progreso.
 */
export function FitLogLogo({ className = 'size-6' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <g fill="currentColor">
        <rect x="3" y="10.2" width="2.2" height="3.6" rx="0.8" />
        <rect x="5.8" y="8.2" width="2.4" height="7.6" rx="1" />
        <rect x="8.8" y="10.9" width="5.6" height="2.2" rx="1" />
        <rect x="15.2" y="8.2" width="2.4" height="7.6" rx="1" />
        <rect x="18.4" y="10.2" width="2.2" height="3.6" rx="0.8" />
      </g>
      <rect x="8.8" y="10.6" width="6.4" height="2.8" rx="1.4" className="fill-accent" />
    </svg>
  );
}

/** Marca + nombre, para el rail de escritorio y el encabezado movil. */
export function FitLogWordmark({ className = '' }: { className?: string }) {
  return (
    <span className={`flex items-center gap-2 ${className}`}>
      <span className="bg-accent-soft text-accent-text rounded-field grid size-8 place-items-center">
        <FitLogLogo className="size-5" />
      </span>
      <span className="text-ink text-lg font-semibold tracking-tight">FitLog</span>
    </span>
  );
}
