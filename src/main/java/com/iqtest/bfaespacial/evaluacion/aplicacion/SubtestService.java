package com.iqtest.bfaespacial.evaluacion.aplicacion;

import com.iqtest.bfaespacial.administracion.catalogo.ConfiguracionSubtestRepository;
import com.iqtest.bfaespacial.common.IntentoListoParaCalificarEvent;
import com.iqtest.bfaespacial.common.SubtestCerradoException;
import com.iqtest.bfaespacial.domain.*;
import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubtestService {

    // Subtest order S1A -> S2 -> S1B (RN-BFA-03)
    private static final List<TipoSubtest> SECUENCIA = List.of(TipoSubtest.S1A, TipoSubtest.S2, TipoSubtest.S1B);
    private static final TipoSubtest ULTIMO_SUBTEST = TipoSubtest.S1B;

    public record VistaActual(EjecucionSubtest ejecucion, long tiempoRestanteSeg) {}

    private final EjecucionSubtestRepository ejecucionRepo;
    private final RespuestaRepository respuestaRepo;
    private final ConfiguracionSubtestRepository configRepo;
    private final ApplicationEventPublisher events;
    private final com.iqtest.bfaespacial.administracion.auditoria.AuditoriaService auditoria;
    private final EntityManager em;

    public SubtestService(EjecucionSubtestRepository ejecucionRepo, RespuestaRepository respuestaRepo,
                          ConfiguracionSubtestRepository configRepo, ApplicationEventPublisher events,
                          com.iqtest.bfaespacial.administracion.auditoria.AuditoriaService auditoria,
                          EntityManager em) {
        this.ejecucionRepo = ejecucionRepo;
        this.respuestaRepo = respuestaRepo;
        this.configRepo = configRepo;
        this.events = events;
        this.auditoria = auditoria;
        this.em = em;
    }

    private static final List<EstadoSubtest> ACTIVOS = List.of(EstadoSubtest.PENDIENTE, EstadoSubtest.EN_CURSO);

    /** Current active subtest (PENDIENTE or EN_CURSO) plus server-computed remaining seconds (§12). */
    @Transactional(readOnly = true)
    public Optional<VistaActual> vistaActual(Long intentoId) {
        return ejecucionRepo.findFirstByIntentoIdAndEstadoInOrderByIdAsc(intentoId, ACTIVOS)
                .map(e -> new VistaActual(e, tiempoRestanteSeg(e)));
    }

    public long tiempoRestanteSeg(EjecucionSubtest e) {
        int limite = configRepo.findById(e.getTipoSubtest()).orElseThrow().getTiempoLimiteSeg();
        if (e.getFechaInicio() == null) {
            return limite; // PENDIENTE — not started yet, full time shown on the consigna
        }
        long transcurrido = Duration.between(e.getFechaInicio(), OffsetDateTime.now()).getSeconds();
        return Math.max(0, limite - transcurrido);
    }

    /** Next subtest in the sequence, or empty after the last (S1B). */
    public Optional<TipoSubtest> siguiente(TipoSubtest actual) {
        int i = SECUENCIA.indexOf(actual);
        return (i >= 0 && i < SECUENCIA.size() - 1) ? Optional.of(SECUENCIA.get(i + 1)) : Optional.empty();
    }

    /** Create (or return) a PENDIENTE execution. Timer does NOT start here (P1-A). */
    @Transactional
    public EjecucionSubtest prepararSubtest(Long intentoId, TipoSubtest tipo) {
        return ejecucionRepo.findByIntentoIdAndTipoSubtest(intentoId, tipo)
                .orElseGet(() -> {
                    EjecucionSubtest e = new EjecucionSubtest();
                    e.setIntento(em.getReference(Intento.class, intentoId));
                    e.setTipoSubtest(tipo);
                    e.setEstado(EstadoSubtest.PENDIENTE); // fecha_inicio stays NULL
                    return ejecucionRepo.save(e);
                });
    }

    /**
     * Student clicked "Comenzar": start the current PENDIENTE subtest and set fecha_inicio = NOW.
     * Idempotent on resume — an already EN_CURSO subtest keeps its original fecha_inicio (RN-BFA-09).
     */
    @Transactional
    public EjecucionSubtest comenzarSubtest(Long intentoId) {
        EjecucionSubtest e = ejecucionRepo.findFirstByIntentoIdAndEstadoInOrderByIdAsc(intentoId, ACTIVOS)
                .orElseThrow(() -> new IllegalStateException("No hay subtest por comenzar"));
        if (e.getEstado() == EstadoSubtest.PENDIENTE) {
            e.setEstado(EstadoSubtest.EN_CURSO);
            e.setFechaInicio(OffsetDateTime.now());
            auditoria.registrar(e.getIntento().getId(), e.getIntento().getCif(),
                    "SUBTEST_INICIADO", e.getTipoSubtest().name());
        }
        return e;
    }

    /** Upsert one answer. Rejected if the subtest is not EN_CURSO (RN-BFA-05). */
    @Transactional
    public Respuesta registrarRespuesta(Long ejecucionId, Long reactivoId, Long opcionReactivoId) {
        EjecucionSubtest ejec = ejecucionRepo.findById(ejecucionId)
                .orElseThrow(() -> new IllegalArgumentException("Ejecución no existe: " + ejecucionId));
        if (ejec.getEstado() != EstadoSubtest.EN_CURSO) {
            throw new SubtestCerradoException(ejecucionId);
        }
        Respuesta r = respuestaRepo.findByEjecucionSubtestIdAndReactivoId(ejecucionId, reactivoId)
                .orElseGet(() -> {
                    Respuesta nueva = new Respuesta();
                    nueva.setEjecucionSubtest(ejec);
                    nueva.setReactivo(em.getReference(Reactivo.class, reactivoId));
                    return nueva;
                });
        r.setOpcionReactivo(opcionReactivoId == null ? null : em.getReference(OpcionReactivo.class, opcionReactivoId));
        r.setFechaRegistro(OffsetDateTime.now());
        r.setSincronizada(true);
        return respuestaRepo.save(r);
    }

    /** Close a subtest. porTiempo=true when the server timer expired it (RN-BFA-04). */
    @Transactional
    public void cerrar(Long ejecucionId, boolean porTiempo) {
        EjecucionSubtest ejec = ejecucionRepo.findById(ejecucionId)
                .orElseThrow(() -> new IllegalArgumentException("Ejecución no existe: " + ejecucionId));
        if (ejec.getEstado() == EstadoSubtest.COMPLETADO || ejec.getEstado() == EstadoSubtest.CERRADO_POR_TIEMPO) {
            return; // idempotent
        }
        ejec.setEstado(porTiempo ? EstadoSubtest.CERRADO_POR_TIEMPO : EstadoSubtest.COMPLETADO);
        ejec.setCerradaPorTiempo(porTiempo);
        ejec.setFechaCierre(OffsetDateTime.now());

        Long intentoId = ejec.getIntento().getId();
        auditoria.registrar(intentoId, ejec.getIntento().getCif(),
                porTiempo ? "SUBTEST_CERRADO_POR_TIEMPO" : "SUBTEST_CERRADO_MANUAL",
                ejec.getTipoSubtest().name());
        if (ejec.getTipoSubtest() == ULTIMO_SUBTEST) {
            events.publishEvent(new IntentoListoParaCalificarEvent(intentoId));
        } else {
            // Advance: prepare the next subtest as PENDIENTE (works for manual and timer closure).
            siguiente(ejec.getTipoSubtest()).ifPresent(next -> prepararSubtest(intentoId, next));
        }
    }
}
