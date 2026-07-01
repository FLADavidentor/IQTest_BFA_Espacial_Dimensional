package com.iqtest.bfaespacial.service;

import com.iqtest.bfaespacial.model.*;
import com.iqtest.bfaespacial.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MonitoreoService {

    public record MonitoreoFila(
            Long intentoId,
            String cif,
            String periodo,
            String version,
            String subtestActivo,
            String progreso,
            long tiempoRestanteSeg
    ) {}

    private final IntentoRepository intentoRepo;
    private final EjecucionSubtestRepository ejecucionRepo;
    private final RespuestaRepository respuestaRepo;
    private final ConfiguracionSubtestRepository configRepo;
    private final ResultadoRepository resultadoRepo;
    private final CalificacionService calificacionService;
    private final SubtestService subtestService;
    private final EntityManager em;

    public MonitoreoService(IntentoRepository intentoRepo, EjecucionSubtestRepository ejecucionRepo,
                            RespuestaRepository respuestaRepo, ConfiguracionSubtestRepository configRepo,
                            ResultadoRepository resultadoRepo, CalificacionService calificacionService,
                            SubtestService subtestService, EntityManager em) {
        this.intentoRepo = intentoRepo;
        this.ejecucionRepo = ejecucionRepo;
        this.respuestaRepo = respuestaRepo;
        this.configRepo = configRepo;
        this.resultadoRepo = resultadoRepo;
        this.calificacionService = calificacionService;
        this.subtestService = subtestService;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<MonitoreoFila> obtenerIntentosActivos() {
        List<Intento> activos = intentoRepo.findAll().stream()
                .filter(i -> i.getEstado() == EstadoIntento.ACTIVO)
                .toList();

        List<MonitoreoFila> filas = new ArrayList<>();
        List<EstadoSubtest> subtestActivos = List.of(EstadoSubtest.PENDIENTE, EstadoSubtest.EN_CURSO);

        for (Intento i : activos) {
            Optional<EjecucionSubtest> ejecOpt = ejecucionRepo.findFirstByIntentoIdAndEstadoInOrderByIdAsc(i.getId(), subtestActivos);
            if (ejecOpt.isPresent()) {
                EjecucionSubtest e = ejecOpt.get();
                long rest = subtestService.tiempoRestanteSeg(e);
                long resp = respuestaRepo.countByEjecucionSubtestId(e.getId());
                long total = configRepo.findById(e.getTipoSubtest()).map(ConfiguracionSubtest::getCantidadItems).orElse((short) 0);
                
                filas.add(new MonitoreoFila(
                        i.getId(),
                        i.getCif(),
                        i.getPeriodoAcademico(),
                        "V" + i.getVersionFormulario().getNumeroVersion(),
                        e.getTipoSubtest().name(),
                        resp + " / " + total,
                        rest
                ));
            } else {
                filas.add(new MonitoreoFila(
                        i.getId(),
                        i.getCif(),
                        i.getPeriodoAcademico(),
                        "V" + i.getVersionFormulario().getNumeroVersion(),
                        "Consigna / Pausado",
                        "—",
                        0
                ));
            }
        }
        return filas;
    }

    @Transactional
    public void forzarFinalizacion(Long intentoId) {
        // 1. Close any running execution subtest
        List<EstadoSubtest> subtestActivos = List.of(EstadoSubtest.PENDIENTE, EstadoSubtest.EN_CURSO);
        ejecucionRepo.findFirstByIntentoIdAndEstadoInOrderByIdAsc(intentoId, subtestActivos).ifPresent(e -> {
            e.setEstado(EstadoSubtest.COMPLETADO);
            e.setFechaCierre(java.time.OffsetDateTime.now());
            ejecucionRepo.save(e);
        });

        // 2. Score and save result (this changes attempt status to COMPLETADO)
        calificacionService.calificar(intentoId);
    }

    @Transactional
    public void anularIntento(Long intentoId) {
        // 1. Delete answers
        em.createQuery("DELETE FROM Respuesta r WHERE r.ejecucionSubtest.id IN " +
                "(SELECT e.id FROM EjecucionSubtest e WHERE e.intento.id = :intentoId)")
                .setParameter("intentoId", intentoId)
                .executeUpdate();

        // 2. Delete executions
        em.createQuery("DELETE FROM EjecucionSubtest e WHERE e.intento.id = :intentoId")
                .setParameter("intentoId", intentoId)
                .executeUpdate();

        // 3. Delete results
        em.createQuery("DELETE FROM Resultado r WHERE r.intentoId = :intentoId")
                .setParameter("intentoId", intentoId)
                .executeUpdate();

        // 4. Delete attempt
        em.createQuery("DELETE FROM Intento i WHERE i.id = :intentoId")
                .setParameter("intentoId", intentoId)
                .executeUpdate();
    }
}
