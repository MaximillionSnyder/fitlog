import type { SVGProps } from 'react';

/**
 * Iconos propios de FitLog, en un lienzo de 24x24 y con el mismo estilo geometrico que la app
 * Android (trazo de 1.8, uniones redondeadas). Evita sumar una libreria de iconos por un punado
 * de glifos.
 */
type IconProps = SVGProps<SVGSVGElement>;

function Stroke({ children, ...props }: IconProps & { children: React.ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {children}
    </svg>
  );
}

function Solid({ children, ...props }: IconProps & { children: React.ReactNode }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" {...props}>
      {children}
    </svg>
  );
}

export const IconHome = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M4 11.2 12 4.2l8 7" />
    <path d="M6.4 10.6v8.8h11.2v-8.8" />
    <path d="M10.2 19.4v-5.2h3.6v5.2" />
  </Stroke>
);

export const IconDumbbell = (props: IconProps) => (
  <Solid {...props}>
    <rect x="3.2" y="10.4" width="2.8" height="3.2" rx="0.8" />
    <rect x="17.2" y="10.4" width="2.8" height="3.2" rx="0.8" />
    <rect x="6.6" y="8.4" width="2.8" height="7.2" rx="0.9" />
    <rect x="13.8" y="8.4" width="2.8" height="7.2" rx="0.9" />
    <rect x="9.6" y="11.3" width="4" height="1.4" rx="0.6" />
  </Solid>
);

export const IconChart = (props: IconProps) => (
  <Solid {...props}>
    <rect x="3.4" y="13" width="2.2" height="7" rx="0.8" />
    <rect x="8.6" y="9" width="2.4" height="11" rx="0.8" />
    <rect x="14" y="4.6" width="2.4" height="15.4" rx="0.8" />
    <rect x="18.8" y="11" width="2.4" height="9" rx="0.8" />
  </Solid>
);

export const IconCalendar = (props: IconProps) => (
  <Stroke {...props}>
    <rect x="4" y="6.8" width="16" height="14.8" rx="2.4" opacity="0.25" fill="currentColor" />
    <path d="M5.6 9.4h12.8" />
    <path d="M8.4 3.6v3M15.6 3.6v3" />
    <rect x="4" y="6.8" width="16" height="14.8" rx="2.4" fill="none" />
  </Stroke>
);

export const IconGrid = (props: IconProps) => (
  <Solid {...props}>
    <rect x="4" y="4" width="6.6" height="6.6" rx="2.2" />
    <rect x="13.4" y="4" width="6.6" height="6.6" rx="2.2" opacity="0.7" />
    <rect x="4" y="13.4" width="6.6" height="6.6" rx="2.2" opacity="0.7" />
    <rect x="13.4" y="13.4" width="6.6" height="6.6" rx="2.2" />
  </Solid>
);

export const IconPlus = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M12 5.4v13.2M5.4 12h13.2" />
  </Stroke>
);

export const IconSearch = (props: IconProps) => (
  <Stroke {...props}>
    <circle cx="10.8" cy="10.8" r="5.4" />
    <path d="m15.2 15.2 4.8 4.8" />
  </Stroke>
);

export const IconScale = (props: IconProps) => (
  <Stroke {...props}>
    <rect x="4" y="4" width="16" height="16" rx="5" opacity="0.2" fill="currentColor" />
    <path d="M5.6 16.4a7.4 7.4 0 0 1 12.8 0" />
    <path d="m12 16.2 3.4-5.8" />
    <circle cx="12" cy="16.2" r="1.2" fill="currentColor" stroke="none" />
  </Stroke>
);

export const IconTrophy = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M7.6 4.6h8.8V11a4.4 4.4 0 0 1-8.8 0Z" />
    <path d="M7.8 6.6H5.4v2a2.6 2.6 0 0 0 2.6 2.6" />
    <path d="M16.2 6.6h2.4v2a2.6 2.6 0 0 1-2.6 2.6" />
    <path d="M12 15.4v3" />
    <path d="M8.6 20.4h6.8v-2H8.6Z" />
  </Stroke>
);

export const IconSpark = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M12 3.6c.9 4.6 2.4 6 7 6.9-4.6.9-6.1 2.4-7 7-.9-4.6-2.4-6.1-7-7 4.6-.9 6.1-2.3 7-6.9Z" />
    <circle cx="18.4" cy="18.4" r="1.5" fill="currentColor" stroke="none" />
  </Stroke>
);

export const IconShield = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M12 3.6 19 6.2v5.4c0 4.6-2.8 7.8-7 9.2-4.2-1.4-7-4.6-7-9.2V6.2Z" />
    <path d="m8.8 12.2 2.4 2.4 4.2-4.8" />
  </Stroke>
);

export const IconGear = (props: IconProps) => (
  <Stroke {...props}>
    <circle cx="12" cy="12" r="2.4" />
    <path d="M12 3.6V6M12 18v2.4M3.6 12H6M18 12h2.4M6.1 6.1l1.7 1.7M16.2 16.2l1.7 1.7M17.9 6.1l-1.7 1.7M7.8 16.2l-1.7 1.7" />
  </Stroke>
);

export const IconSun = (props: IconProps) => (
  <Stroke {...props}>
    <circle cx="12" cy="12" r="3.4" />
    <path d="M12 2.8V5M12 19v2.2M2.8 12H5M19 12h2.2M5.5 5.5 7.1 7.1M16.9 16.9l1.6 1.6M18.5 5.5l-1.6 1.6M7.1 16.9l-1.6 1.6" />
  </Stroke>
);

export const IconMoon = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M20 14.4A8.6 8.6 0 0 1 9.6 4a8.6 8.6 0 1 0 10.4 10.4Z" />
  </Stroke>
);

export const IconPlay = (props: IconProps) => (
  <Solid {...props}>
    <path d="M8.4 5.6 18.4 12l-10 6.4Z" />
  </Solid>
);

export const IconStop = (props: IconProps) => (
  <Solid {...props}>
    <rect x="7" y="7" width="10" height="10" rx="2.4" />
  </Solid>
);

export const IconArrowUp = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M12 19V5.6M6.4 11.2 12 5.6l5.6 5.6" />
  </Stroke>
);

export const IconArrowDown = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M12 5v13.4M6.4 12.8 12 18.4l5.6-5.6" />
  </Stroke>
);

export const IconChevronRight = (props: IconProps) => (
  <Stroke {...props}>
    <path d="m9.6 5.6 6.4 6.4-6.4 6.4" />
  </Stroke>
);

export const IconBack = (props: IconProps) => (
  <Stroke {...props}>
    <path d="M19 12H5.4M11.4 5.8 5.2 12l6.2 6.2" />
  </Stroke>
);

export const IconMore = (props: IconProps) => (
  <Solid {...props}>
    <circle cx="5.6" cy="12" r="1.9" />
    <circle cx="12" cy="12" r="1.9" />
    <circle cx="18.4" cy="12" r="1.9" />
  </Solid>
);
