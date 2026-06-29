package com.iqtest.bfaespacial.common;

/** Published when the server timer closes a subtest. Drives the SSE "tiempo agotado" push (P2-A). */
public record SubtestCerradoPorTiempoEvent(Long intentoId, String subtestType) {
}

