export class RoutineOrderError extends RangeError {
  constructor(message: string) {
    super(message);
    this.name = 'RoutineOrderError';
  }
}

export function moveItem<T>(items: readonly T[], fromIndex: number, toIndex: number): T[] {
  if (
    !Number.isInteger(fromIndex) ||
    !Number.isInteger(toIndex) ||
    fromIndex < 0 ||
    fromIndex >= items.length ||
    toIndex < 0 ||
    toIndex >= items.length
  ) {
    throw new RoutineOrderError(
      `Índices fuera de rango: no se puede mover ${fromIndex} a ${toIndex} en una lista de ${items.length}`
    );
  }

  if (fromIndex === toIndex) {
    return [...items];
  }

  const copy = [...items];
  const [moved] = copy.splice(fromIndex, 1);
  copy.splice(toIndex, 0, moved as T);
  return copy;
}

export function assignPositions(ids: readonly string[]): { id: string; position: number }[] {
  return ids.map((id, index) => ({ id, position: index + 1 }));
}
