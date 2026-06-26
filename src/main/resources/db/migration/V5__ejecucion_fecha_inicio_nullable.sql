-- P1-A: timer starts only when the student clicks "Comenzar" (after reading the consigna).
-- fecha_inicio is NULL while the subtest is PENDIENTE; set on comenzar, not on create.
ALTER TABLE ejecucion_subtest ALTER COLUMN fecha_inicio DROP NOT NULL;
ALTER TABLE ejecucion_subtest ALTER COLUMN fecha_inicio DROP DEFAULT;
