import type { ReactNode } from 'react';

import {
  IconCalendar,
  IconChart,
  IconDumbbell,
  IconGear,
  IconGrid,
  IconHome,
  IconMore,
  IconScale,
  IconShield,
  IconSpark,
  IconTrophy,
} from '@/ui/icons';

/**
 * Arquitectura de informacion de la web: los mismos destinos que la app Android.
 *
 * Cinco destinos de primer nivel en la barra inferior (o el rail lateral en escritorio) y el resto
 * agrupado en "Más" con una descripcion.
 */

export type View =
  | 'inicio'
  | 'entrenar'
  | 'progreso'
  | 'rutinas'
  | 'mas'
  | 'catalogo'
  | 'comparativas'
  | 'tips'
  | 'medidas'
  | 'respaldo'
  | 'ajustes';

export interface TopLevelDestination {
  readonly id: View;
  readonly label: string;
  readonly icon: ReactNode;
}

const iconClass = 'size-[22px]';

export const topLevelDestinations: readonly TopLevelDestination[] = [
  { id: 'inicio', label: 'Inicio', icon: <IconHome className={iconClass} /> },
  { id: 'entrenar', label: 'Entrenar', icon: <IconDumbbell className={iconClass} /> },
  { id: 'progreso', label: 'Progreso', icon: <IconChart className={iconClass} /> },
  { id: 'rutinas', label: 'Rutinas', icon: <IconCalendar className={iconClass} /> },
  { id: 'mas', label: 'Más', icon: <IconMore className={iconClass} /> },
];

export interface SecondaryDestination {
  readonly id: View;
  readonly title: string;
  readonly description: string;
  readonly icon: ReactNode;
}

export const secondaryDestinations: readonly SecondaryDestination[] = [
  {
    id: 'catalogo',
    title: 'Catálogo',
    description: 'Explorá ejercicios por grupo muscular y equipamiento',
    icon: <IconGrid className={iconClass} />,
  },
  {
    id: 'medidas',
    title: 'Medidas',
    description: 'Peso corporal y perímetros a lo largo del tiempo',
    icon: <IconScale className={iconClass} />,
  },
  {
    id: 'comparativas',
    title: 'Comparativas',
    description: 'Récords, mes contra mes y balance muscular',
    icon: <IconTrophy className={iconClass} />,
  },
  {
    id: 'tips',
    title: 'Tips',
    description: 'Observaciones sobre tus últimos entrenamientos',
    icon: <IconSpark className={iconClass} />,
  },
  {
    id: 'respaldo',
    title: 'Respaldo',
    description: 'Exportá o fusioná tus datos entre dispositivos',
    icon: <IconShield className={iconClass} />,
  },
  {
    id: 'ajustes',
    title: 'Ajustes',
    description: 'Tema, colores y estado de la base de datos',
    icon: <IconGear className={iconClass} />,
  },
];

export function titleForView(view: View): string | null {
  if (view === 'inicio') return null;
  const top = topLevelDestinations.find((destination) => destination.id === view);
  if (top) return top.label;
  return secondaryDestinations.find((destination) => destination.id === view)?.title ?? null;
}

export function isTopLevel(view: View): boolean {
  return topLevelDestinations.some((destination) => destination.id === view);
}
