-- Enums (PostgreSQL custom types) — §5
CREATE TYPE tipo_subtest    AS ENUM ('S1A', 'S2', 'S1B');
CREATE TYPE estado_intento  AS ENUM ('ACTIVO', 'COMPLETADO', 'EXPIRADO', 'INTERRUMPIDO');
CREATE TYPE estado_subtest  AS ENUM ('PENDIENTE', 'EN_CURSO', 'COMPLETADO', 'CERRADO_POR_TIEMPO');
CREATE TYPE factor_espacial AS ENUM ('S1', 'S2', 'ST');

-- Rotating form versions (3 per year)
CREATE TABLE version_formulario (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  anio           SMALLINT NOT NULL,
  numero_version SMALLINT NOT NULL,
  activa         BOOLEAN  NOT NULL DEFAULT false,
  UNIQUE (anio, numero_version)
);

-- Per-subtest configuration (seed data)
CREATE TABLE configuracion_subtest (
  tipo_subtest      tipo_subtest PRIMARY KEY,
  tiempo_limite_seg INTEGER     NOT NULL,
  cantidad_items    SMALLINT    NOT NULL,
  tipo_seleccion    VARCHAR(20) NOT NULL  -- 'BINARIO' | 'MULTIPLE'
);

-- Items
CREATE TABLE reactivo (
  id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version_formulario_id BIGINT       NOT NULL REFERENCES version_formulario (id),
  tipo_subtest          tipo_subtest NOT NULL REFERENCES configuracion_subtest (tipo_subtest),
  orden                 SMALLINT     NOT NULL,
  enunciado_imagen_url  TEXT         NOT NULL
);

-- Answer options (normalized)
CREATE TABLE opcion_reactivo (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reactivo_id BIGINT     NOT NULL REFERENCES reactivo (id),
  etiqueta    VARCHAR(4) NOT NULL,
  es_correcta BOOLEAN    NOT NULL DEFAULT false,
  UNIQUE (reactivo_id, etiqueta)
);
-- Exactly one correct option per reactivo
CREATE UNIQUE INDEX ux_opcion_correcta_por_reactivo
  ON opcion_reactivo (reactivo_id) WHERE es_correcta;

-- One attempt per student per period (RN-BFA-01)
CREATE TABLE intento (
  id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cif                   VARCHAR(20)    NOT NULL,
  periodo_academico     VARCHAR(20)    NOT NULL,
  estado                estado_intento NOT NULL DEFAULT 'ACTIVO',
  fecha_inicio          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  fecha_fin             TIMESTAMPTZ,
  version_formulario_id BIGINT         NOT NULL REFERENCES version_formulario (id),
  UNIQUE (cif, periodo_academico)
);

-- Timed subtest execution within an attempt
CREATE TABLE ejecucion_subtest (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  intento_id         BIGINT         NOT NULL REFERENCES intento (id),
  tipo_subtest       tipo_subtest   NOT NULL REFERENCES configuracion_subtest (tipo_subtest),
  fecha_inicio       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  fecha_cierre       TIMESTAMPTZ,
  estado             estado_subtest NOT NULL DEFAULT 'PENDIENTE',
  cerrada_por_tiempo BOOLEAN        NOT NULL DEFAULT false,
  UNIQUE (intento_id, tipo_subtest)
);

-- Student's answer per item
CREATE TABLE respuesta (
  id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ejecucion_subtest_id BIGINT      NOT NULL REFERENCES ejecucion_subtest (id),
  reactivo_id          BIGINT      NOT NULL REFERENCES reactivo (id),
  opcion_reactivo_id   BIGINT      REFERENCES opcion_reactivo (id),  -- NULL if unanswered
  fecha_registro       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  sincronizada         BOOLEAN     NOT NULL DEFAULT true,
  UNIQUE (ejecucion_subtest_id, reactivo_id)
);

-- 1:1 with intento. Composite scores are GENERATED (RN-BFA-10).
CREATE TABLE resultado (
  intento_id    BIGINT   PRIMARY KEY REFERENCES intento (id),
  pd_s1a        SMALLINT NOT NULL,
  pd_s1b        SMALLINT NOT NULL,
  pd_s1         SMALLINT GENERATED ALWAYS AS (pd_s1a + pd_s1b) STORED,
  pd_s2         SMALLINT NOT NULL,
  pd_st         SMALLINT GENERATED ALWAYS AS (pd_s1a + pd_s1b + pd_s2) STORED,
  perc_s1       SMALLINT NOT NULL,
  perc_s2       SMALLINT NOT NULL,
  perc_st       SMALLINT NOT NULL,
  fecha_calculo TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Baremo (Normas Nacionales BFA Nicaragua 1992)
CREATE TABLE baremo (
  factor             factor_espacial NOT NULL,
  puntuacion_directa SMALLINT        NOT NULL,
  percentil          SMALLINT        NOT NULL,
  PRIMARY KEY (factor, puntuacion_directa)
);

-- Audit log
CREATE TABLE registro_auditoria (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  intento_id BIGINT       NOT NULL REFERENCES intento (id),
  cif_actor  VARCHAR(20)  NOT NULL,
  accion     VARCHAR(100) NOT NULL,
  fecha_hora TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  detalle    TEXT
);
