package com.iqtest.bfaespacial.evaluacion.aplicacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Server-side timer enforcement (§12, RN-BFA-04). Client timer is UX only.
 * Every 5s, closes EN_CURSO executions whose time limit has elapsed.
 */
@Service
public class TimerService {

    private static final Logger log = LoggerFactory.getLogger(TimerService.class);

    private final EjecucionSubtestRepository ejecucionRepo;
    private final SubtestService subtestService;

    public TimerService(EjecucionSubtestRepository ejecucionRepo, SubtestService subtestService) {
        this.ejecucionRepo = ejecucionRepo;
        this.subtestService = subtestService;
    }

    // P2-A: 1s poll to meet RN-BFA-04 (<=1s auto-close latency); SSE pushes closure to clients.
    // Configurable so it can be tuned without a rebuild.
    @Scheduled(fixedDelayString = "${app.timer.fixed-delay-ms:1000}")
    public void cerrarExpirados() {
        List<Long> expirados = ejecucionRepo.findIdsExpirados();
        for (Long id : expirados) {
            subtestService.cerrar(id, true);
            log.info("Subtest ejecución {} cerrada por tiempo", id);
        }
    }
}
