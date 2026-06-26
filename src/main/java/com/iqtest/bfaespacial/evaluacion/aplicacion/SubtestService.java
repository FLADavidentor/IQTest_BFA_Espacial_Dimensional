package com.iqtest.bfaespacial.evaluacion.aplicacion;

import com.iqtest.bfaespacial.common.IntentoListoParaCalificarEvent;
import com.iqtest.bfaespacial.common.SubtestCerradoException;
import com.iqtest.bfaespacial.domain.*;
import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SubtestService {

    // Last subtest in the sequence S1A -> S2 -> S1B (RN-BFA-03)
    private static final TipoSubtest ULTIMO_SUBTEST = TipoSubtest.S1B;

    private final EjecucionSubtestRepository ejecucionRepo;
    private final RespuestaRepository respuestaRepo;
    private final ApplicationEventPublisher events;
    private final EntityManager em;

    public SubtestService(EjecucionSubtestRepository ejecucionRepo, RespuestaRepository respuestaRepo,
                          ApplicationEventPublisher events, EntityManager em) {
        this.ejecucionRepo = ejecucionRepo;
        this.respuestaRepo = respuestaRepo;
        this.events = events;
        this.em = em;
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
