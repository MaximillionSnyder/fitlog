-- FitLog - migracion 003: ruta del recorrido
-- Un GPX trae las coordenadas del entrenamiento. Se guardan recortadas (300 puntos) en texto
-- compacto para dibujar el trazado y los perfiles de altura y pulso sin conexion. Una sesion
-- registrada a mano, o importada de Huawei Health, la deja en NULL.

ALTER TABLE session ADD COLUMN route TEXT;
