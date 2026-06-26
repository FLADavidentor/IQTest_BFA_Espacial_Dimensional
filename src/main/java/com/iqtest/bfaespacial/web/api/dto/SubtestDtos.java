package com.iqtest.bfaespacial.web.api.dto;

import java.util.List;

/** DTOs for the React SPA. Correct answers (es_correcta) are NEVER exposed (RN-BFA-08). */
public final class SubtestDtos {

    public record Opcion(Long id, String etiqueta) {}

    public record Item(Long id, short orden, String imagenUrl, List<Opcion> opciones) {}

    public record SubtestActual(Long ejecucionSubtestId, String subtestType, List<Item> items,
                                long tiempoRestanteSeg, String estado) {}

    public record Tiempo(long tiempoRestanteSeg, String subtestType, String estado) {}

    public record RespuestaRequest(Long ejecucionSubtestId, Long reactivoId, Long opcionReactivoId) {}

    public record SyncItem(Long reactivoId, Long opcionReactivoId) {}

    public record SyncRequest(List<SyncItem> respuestas) {}

    public record SyncResult(int sincronizadas, int rechazadas) {}

    public record CerrarResult(String next) {}

    private SubtestDtos() {}
}
