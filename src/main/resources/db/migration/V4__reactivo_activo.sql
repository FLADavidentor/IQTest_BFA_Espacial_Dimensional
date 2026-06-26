-- P0-B: soft-delete flag for reactivo (hard deletes unsafe — answers may reference items).
ALTER TABLE reactivo ADD COLUMN activo BOOLEAN NOT NULL DEFAULT true;
