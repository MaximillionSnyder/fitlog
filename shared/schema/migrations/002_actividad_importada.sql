-- FitLog - migracion 002: metricas de actividad importada
-- Las sesiones importadas (Huawei Health o GPX) traen distancia, calorias, frecuencia cardiaca,
-- pasos y desnivel. Hasta ahora vivian solo en la nota; pasan a columnas para poder mostrarlas y
-- graficarlas. Son opcionales: una sesion registrada a mano las deja en NULL.

ALTER TABLE session ADD COLUMN distance_m REAL;
ALTER TABLE session ADD COLUMN calories REAL;
ALTER TABLE session ADD COLUMN avg_heart_rate REAL;
ALTER TABLE session ADD COLUMN max_heart_rate REAL;
ALTER TABLE session ADD COLUMN steps INTEGER;
ALTER TABLE session ADD COLUMN elevation_gain_m REAL;
ALTER TABLE session ADD COLUMN source TEXT;
