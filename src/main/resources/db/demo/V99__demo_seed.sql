-- DEMO SEED — applied only under the 'demo' profile (Flyway location classpath:db/demo).
-- Makes the module click-through ready: active form version + all 110 items + options (correct = A).
-- Image URLs are placeholders -> the SPA shows the "[ Imagen no disponible ]" fallback.

INSERT INTO version_formulario (anio, numero_version, activa) VALUES (2026, 1, true);

-- Items: 27 S1A + 34 S2 + 49 S1B, real BFA counts.
INSERT INTO reactivo (version_formulario_id, tipo_subtest, orden, enunciado_imagen_url)
  SELECT (SELECT id FROM version_formulario WHERE anio = 2026 AND numero_version = 1),
         'S1A', g, 's1a/' || g || '.png' FROM generate_series(1, 27) g;
INSERT INTO reactivo (version_formulario_id, tipo_subtest, orden, enunciado_imagen_url)
  SELECT (SELECT id FROM version_formulario WHERE anio = 2026 AND numero_version = 1),
         'S2', g, 's2/' || g || '.png' FROM generate_series(1, 34) g;
INSERT INTO reactivo (version_formulario_id, tipo_subtest, orden, enunciado_imagen_url)
  SELECT (SELECT id FROM version_formulario WHERE anio = 2026 AND numero_version = 1),
         'S1B', g, 's1b/' || g || '.png' FROM generate_series(1, 49) g;

-- Options: A/B for the binary subtests; A–E for S1B. Correct answer = A on every item.
INSERT INTO opcion_reactivo (reactivo_id, etiqueta, es_correcta) SELECT id, 'A', true  FROM reactivo;
INSERT INTO opcion_reactivo (reactivo_id, etiqueta, es_correcta) SELECT id, 'B', false FROM reactivo;
INSERT INTO opcion_reactivo (reactivo_id, etiqueta, es_correcta) SELECT id, 'C', false FROM reactivo WHERE tipo_subtest = 'S1B';
INSERT INTO opcion_reactivo (reactivo_id, etiqueta, es_correcta) SELECT id, 'D', false FROM reactivo WHERE tipo_subtest = 'S1B';
INSERT INTO opcion_reactivo (reactivo_id, etiqueta, es_correcta) SELECT id, 'E', false FROM reactivo WHERE tipo_subtest = 'S1B';
