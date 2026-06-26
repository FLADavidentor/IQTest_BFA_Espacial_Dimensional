package com.iqtest.bfaespacial.evaluacion.sincronizacion;

import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.evaluacion.aplicacion.EjecucionSubtestRepository;
import com.iqtest.bfaespacial.evaluacion.aplicacion.SubtestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UC3: sync locally-buffered answers after a reconnection (RN-BFA-09).
 * The server timer is NOT reset; answers are accepted only while EN_CURSO.
 */
@Service
public class SincronizacionService {

    public record RespuestaPendiente(Long reactivoId, Long opcionReactivoId) {}

    public record Resultado(int sincronizadas, int rechazadas) {}

    private final SubtestService subtestService;
    private final EjecucionSubtestRepository ejecucionRepo;

    public SincronizacionService(SubtestService subtestService, EjecucionSubtestRepository ejecucionRepo) {
        this.subtestService = subtestService;
        this.ejecucionRepo = ejecucionRepo;
    }

    @Transactional
    public Resultado sincronizar(Long ejecucionId, List<RespuestaPendiente> pendientes) {
        // Single state check: the subtest already closed (e.g. timer expired) => reject the whole batch.
        boolean enCurso = ejecucionRepo.findById(ejecucionId)
                .map(e -> e.getEstado() == EstadoSubtest.EN_CURSO)
                .orElse(false);
        if (!enCurso) {
            return new Resultado(0, pendientes.size());
        }
        for (RespuestaPendiente p : pendientes) {
            subtestService.registrarRespuesta(ejecucionId, p.reactivoId(), p.opcionReactivoId());
        }
        return new Resultado(pendientes.size(), 0);
    }
}
