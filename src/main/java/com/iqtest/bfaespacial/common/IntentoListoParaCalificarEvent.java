package com.iqtest.bfaespacial.common;
import com.iqtest.bfaespacial.service.CalificacionService;

/** Published when the last subtest (S1B) closes. CalificacionService listens (Phase 3). */
public record IntentoListoParaCalificarEvent(Long intentoId) {
}

