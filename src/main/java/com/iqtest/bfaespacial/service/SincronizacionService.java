package com.iqtest.bfaespacial.service;
import com.iqtest.bfaespacial.model.Resultado;

import com.iqtest.bfaespacial.service.AuditoriaService;
import com.iqtest.bfaespacial.model.EjecucionSubtest;
import com.iqtest.bfaespacial.model.EstadoSubtest;
import com.iqtest.bfaespacial.repository.EjecucionSubtestRepository;
import com.iqtest.bfaespacial.service.SubtestService;
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
    private final AuditoriaService auditoria;

    public SincronizacionService(SubtestService subtestService, EjecucionSubtestRepository ejecucionRepo,
                                 AuditoriaService auditoria) {
        this.subtestService = subtestService;
        this.ejecucionRepo = ejecucionRepo;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado sincronizar(Long ejecucionId, List<RespuestaPendiente> pendientes) {
        EjecucionSubtest ejec = ejecucionRepo.findById(ejecucionId).orElse(null);
        // Single state check: the subtest already closed (e.g. timer expired) => reject the whole batch.
        if (ejec == null || ejec.getEstado() != EstadoSubtest.EN_CURSO) {
            return new Resultado(0, pendientes.size());
        }
        for (RespuestaPendiente p : pendientes) {
            subtestService.registrarRespuesta(ejecucionId, p.reactivoId(), p.opcionReactivoId());
        }
        auditoria.registrar(ejec.getIntento().getId(), ejec.getIntento().getCif(),
                "SYNC_RECIBIDA", pendientes.size() + " respuestas");
        return new Resultado(pendientes.size(), 0);
    }
}


