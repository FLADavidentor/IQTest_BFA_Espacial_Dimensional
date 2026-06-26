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
    private final EntityManager em;

    public SubtestService(EjecucionSubtestRepository ejecucionRepo, RespuestaRepository respuestaRepo,
                          ConfiguracionSubtestRepository configRepo,
                          ApplicationEventPublisher events, EntityManager em) {
        this.ejecucionRepo = ejecucionRepo;
        this.respuestaRepo = respuestaRepo;
        this.configRepo = configRepo;
        this.events = events;
        this.em = em;
    }

    /** Current EN_CURSO execution for an intento plus server-computed remaining seconds (§12). */
    @Transactional(readOnly = true)
    public Optional<VistaActual> vistaActual(Long intentoId) {
        return ejecucionRepo.findFirstByIntentoIdAndEstado(intentoId, EstadoSubtest.EN_CURSO)
                .map(e -> new VistaActual(e, tiempoRestanteSeg(e)));
    }

    public long tiempoRestanteSeg(EjecucionSubtest e) {
        int limite = configRepo.findById(e.getTipoSubtest()).orElseThrow().getTiempoLimiteSeg();
        long transcurrido = Duration.between(e.getFechaInicio(), OffsetDateTime.now()).getSeconds();
        return Math.max(0, limite - transcurrido);
    }

    /** Next subtest in the sequence, or empty after the last (S1B). */
    public Optional<TipoSubtest> siguiente(TipoSubtest actual) {
        int i = SECUENCIA.indexOf(actual);
        return (i >= 0 && i < SECUENCIA.size() - 1) ? Optional.of(SECUENCIA.get(i + 1)) : Optional.empty();
    }

    /** Start (or return existing) timed execution. Sets fecha_inicio = NOW (server clock). */
    @Transactional
    public EjecucionSubtest iniciar(Long intentoId, TipoSubtest tipo) {
        return ejecucionRepo.findByIntentoIdAndTipoSubtest(intentoId, tipo)
                .orElseGet(() -> {
                    EjecucionSubtest e = new EjecucionSubtest();
                    e.setIntento(em.getReference(Intento.class, intentoId));
                    e.setTipoSubtest(tipo);
                    e.setEstado(EstadoSubtest.EN_CURSO);
                    e.setFechaInicio(OffsetDateTime.now());
                    return ejecucionRepo.save(e);
                });
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

        if (ejec.getTipoSubtest() == ULTIMO_SUBTEST) {
            events.publishEvent(new IntentoListoParaCalificarEvent(ejec.getIntento().getId()));
        }
    }
}
