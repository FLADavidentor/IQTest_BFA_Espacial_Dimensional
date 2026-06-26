-- Audit scenario: 3 versions (1 active), full item set, known answers, baremo with a ST gap.
INSERT INTO version_formulario(anio,numero_version,activa) VALUES (2026,1,true),(2026,2,false),(2026,3,false);
SELECT id AS vid FROM version_formulario WHERE anio=2026 AND numero_version=1 \gset
INSERT INTO intento(cif,periodo_academico,version_formulario_id,estado) VALUES ('DEV0000001','2026-I',:vid,'ACTIVO');
SELECT id AS iid FROM intento WHERE cif='DEV0000001' AND periodo_academico='2026-I' \gset

INSERT INTO reactivo(version_formulario_id,tipo_subtest,orden,enunciado_imagen_url) SELECT :vid,'S1A',g,'/img/s1a/'||g||'.png' FROM generate_series(1,27) g;
INSERT INTO reactivo(version_formulario_id,tipo_subtest,orden,enunciado_imagen_url) SELECT :vid,'S2',g,'/img/s2/'||g||'.png' FROM generate_series(1,34) g;
INSERT INTO reactivo(version_formulario_id,tipo_subtest,orden,enunciado_imagen_url) SELECT :vid,'S1B',g,'/img/s1b/'||g||'.png' FROM generate_series(1,49) g;

INSERT INTO opcion_reactivo(reactivo_id,etiqueta,es_correcta) SELECT id,'A',true  FROM reactivo WHERE version_formulario_id=:vid;
INSERT INTO opcion_reactivo(reactivo_id,etiqueta,es_correcta) SELECT id,'B',false FROM reactivo WHERE version_formulario_id=:vid;
INSERT INTO opcion_reactivo(reactivo_id,etiqueta,es_correcta) SELECT id,'C',false FROM reactivo WHERE version_formulario_id=:vid AND tipo_subtest='S1B';
INSERT INTO opcion_reactivo(reactivo_id,etiqueta,es_correcta) SELECT id,'D',false FROM reactivo WHERE version_formulario_id=:vid AND tipo_subtest='S1B';
INSERT INTO opcion_reactivo(reactivo_id,etiqueta,es_correcta) SELECT id,'E',false FROM reactivo WHERE version_formulario_id=:vid AND tipo_subtest='S1B';

INSERT INTO ejecucion_subtest(intento_id,tipo_subtest,estado) VALUES (:iid,'S1A','COMPLETADO'),(:iid,'S2','COMPLETADO'),(:iid,'S1B','EN_CURSO');

-- S1A: 10 correct (orden<=10 -> A), rest B
INSERT INTO respuesta(ejecucion_subtest_id,reactivo_id,opcion_reactivo_id)
SELECT e.id, r.id, (SELECT o.id FROM opcion_reactivo o WHERE o.reactivo_id=r.id AND o.etiqueta=CASE WHEN r.orden<=10 THEN 'A' ELSE 'B' END)
FROM reactivo r JOIN ejecucion_subtest e ON e.tipo_subtest=r.tipo_subtest AND e.intento_id=:iid
WHERE r.tipo_subtest='S1A' AND r.version_formulario_id=:vid;

-- S2: 15 correct
INSERT INTO respuesta(ejecucion_subtest_id,reactivo_id,opcion_reactivo_id)
SELECT e.id, r.id, (SELECT o.id FROM opcion_reactivo o WHERE o.reactivo_id=r.id AND o.etiqueta=CASE WHEN r.orden<=15 THEN 'A' ELSE 'B' END)
FROM reactivo r JOIN ejecucion_subtest e ON e.tipo_subtest=r.tipo_subtest AND e.intento_id=:iid
WHERE r.tipo_subtest='S2' AND r.version_formulario_id=:vid;

-- S1B: 8 correct
INSERT INTO respuesta(ejecucion_subtest_id,reactivo_id,opcion_reactivo_id)
SELECT e.id, r.id, (SELECT o.id FROM opcion_reactivo o WHERE o.reactivo_id=r.id AND o.etiqueta=CASE WHEN r.orden<=8 THEN 'A' ELSE 'B' END)
FROM reactivo r JOIN ejecucion_subtest e ON e.tipo_subtest=r.tipo_subtest AND e.intento_id=:iid
WHERE r.tipo_subtest='S1B' AND r.version_formulario_id=:vid;

-- baremo: S1@18, S2@15 exact; ST@30 only (ST@33 missing -> gap, fallback to 30)
INSERT INTO baremo(factor,puntuacion_directa,percentil) VALUES ('S1',18,55),('S2',15,40),('ST',30,48);

SELECT 'vid='||:vid||' iid='||:iid AS seeded;
