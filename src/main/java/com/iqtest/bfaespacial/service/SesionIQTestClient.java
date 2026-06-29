package com.iqtest.bfaespacial.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reads the authenticated student's CIF from the IQTest session (§8, §17).
 * STUB — Phase 6: returns a hardcoded dev CIF. Real impl reads the IQTest
 * session attribute / token per §19 Q2.
 */
@Component
public class SesionIQTestClient {

    private final String devCif;
    private final String periodoActual;

    public SesionIQTestClient(
            @Value("${app.iqtest.dev-cif}") String devCif,
            @Value("${app.periodo-academico-actual}") String periodoActual) {
        this.devCif = devCif;
        this.periodoActual = periodoActual;
    }

    public String cifActual() {
        return devCif; // STUB — Phase 6
    }

    public String periodoActual() {
        return periodoActual;
    }
}

