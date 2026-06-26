package com.iqtest.bfaespacial.common;

/** RN-BFA-01: an intento already exists for this cif+periodo. Maps to HTTP 409. */
public class IntentoConflictException extends RuntimeException {
    public IntentoConflictException(String cif, String periodo) {
        super("Ya existe un intento para CIF %s en el periodo %s".formatted(cif, periodo));
    }
}
