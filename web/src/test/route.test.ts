import { describe, expect, it } from 'vitest';

import { decodeRoute, encodeRoute, MAX_ROUTE_POINTS, routeDistanceM, simplifyRoute } from '@/domain/route';

const point = (latitude: number, longitude: number, elevation: number | null = null, heartRate: number | null = null) => ({
  latitude,
  longitude,
  elevation,
  heartRate,
});

describe('route', () => {
  it('el texto de la ruta va y vuelve igual', () => {
    const route = [point(-34.6037, -58.3816, 25, 120), point(-34.6047, -58.3826, 32.5, 170)];

    const decoded = decodeRoute(encodeRoute(route));

    expect(decoded).toHaveLength(2);
    expect(decoded[0]!.latitude).toBeCloseTo(-34.6037, 5);
    expect(decoded[0]!.longitude).toBeCloseTo(-58.3816, 5);
    expect(decoded[0]!.elevation).toBe(25);
    expect(decoded[0]!.heartRate).toBe(120);
    expect(decoded[1]!.elevation).toBe(32.5);
  });

  it('los campos que faltan quedan vacíos', () => {
    const decoded = decodeRoute(encodeRoute([point(-34.6, -58.3)]));

    expect(decoded).toHaveLength(1);
    expect(decoded[0]!.elevation).toBeNull();
    expect(decoded[0]!.heartRate).toBeNull();
  });

  it('una ruta vacía o rota no rompe', () => {
    expect(decodeRoute(null)).toHaveLength(0);
    expect(decodeRoute('')).toHaveLength(0);
    expect(decodeRoute('basura;otra cosa')).toHaveLength(0);
  });

  it('recorta la ruta sin perder el principio ni el final', () => {
    const many = Array.from({ length: 5_000 }, (_, index) => point(-34.6 + index * 0.0001, -58.3));

    const simplified = simplifyRoute(many);

    expect(simplified).toHaveLength(MAX_ROUTE_POINTS);
    expect(simplified[0]!.latitude).toBeCloseTo(many[0]!.latitude, 5);
    expect(simplified[simplified.length - 1]!.latitude).toBeCloseTo(
      many[many.length - 1]!.latitude,
      5
    );
  });

  it('una ruta corta no se toca y descarta los puntos sin coordenadas', () => {
    const few = Array.from({ length: 20 }, (_, index) => point(-34.6 + index * 0.0001, -58.3));

    expect(simplifyRoute(few)).toHaveLength(20);
    expect(simplifyRoute([point(-34.6, -58.3), point(Number.NaN, Number.NaN)])).toHaveLength(1);
  });

  it('la distancia de la ruta suma los tramos', () => {
    const distance = routeDistanceM([point(-34.6037, -58.3816), point(-34.6047, -58.3826)]);

    expect(distance).toBeGreaterThan(130);
    expect(distance).toBeLessThan(160);
  });
});
