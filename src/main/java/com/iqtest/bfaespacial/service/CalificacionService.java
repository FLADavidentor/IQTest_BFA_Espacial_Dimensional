package com.iqtest.bfaespacial.service;

import com.iqtest.bfaespacial.service.AuditoriaService;
import com.iqtest.bfaespacial.common.IntentoListoParaCalificarEvent;
import com.iqtest.bfaespacial.model.Intento;
import com.iqtest.bfaespacial.model.Resultado;
import com.iqtest.bfaespacial.model.EstadoIntento;
import com.iqtest.bfaespacial.model.FactorEspacial;
import com.iqtest.bfaespacial.model.TipoSubtest;
import com.iqtest.bfaespacial.repository.RespuestaRepository;
import com.iqtest.bfaespacial.repository.IntentoRepository;
import com.iqtest.bfaespacial.repository.ResultadoRepository;
import com.iqtest.bfaespacial.service.PercentilService;
import com.iqtest.bfaespacial.service.PercentilService.PercentilResultado;
import jakarta.persistence.EntityManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** UC4: score an intento and convert to percentiles when the last subtest closes. */
@Service
public class CalificacionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CalificacionService.class);

    private final RespuestaRepository respuestaRepo;
    private final ResultadoRepository resultadoRepo;
    private final IntentoRepository intentoRepo;
    private final PercentilService percentilService;
    private final AuditoriaService auditoria;
    private final EntityManager em;

    public CalificacionService(RespuestaRepository respuestaRepo, ResultadoRepository resultadoRepo,
                               IntentoRepository intentoRepo, PercentilService percentilService,
                               AuditoriaService auditoria, EntityManager em) {
        this.respuestaRepo = respuestaRepo;
        this.resultadoRepo = resultadoRepo;
        this.intentoRepo = intentoRepo;
        this.percentilService = percentilService;
        this.auditoria = auditoria;
        this.em = em;
    }

    @EventListener
    public void onIntentoListo(IntentoListoParaCalificarEvent event) {
        calificar(event.intentoId());
    }

    @Transactional
    public Resultado calificar(Long intentoId) {
        long t0 = System.nanoTime();
        Intento intento = intentoRepo.findById(intentoId)
                .orElseThrow(() -> new IllegalArgumentException("Intento no existe: " + intentoId));

        short pdS1a = (short) respuestaRepo.countCorrectas(intentoId, TipoSubtest.S1A);
        short pdS1b = (short) respuestaRepo.countCorrectas(intentoId, TipoSubtest.S1B);
        short pdS2  = (short) respuestaRepo.countCorrectas(intentoId, TipoSubtest.S2);

        short s1 = (short) (pdS1a + pdS1b);
        short st = (short) (s1 + pdS2);

        PercentilResultado pS1 = percentilService.calcular(FactorEspacial.S1, s1);
        PercentilResultado pS2 = percentilService.calcular(FactorEspacial.S2, pdS2);
        PercentilResultado pSt = percentilService.calcular(FactorEspacial.ST, st);

        logGap(intentoId, intento.getCif(), FactorEspacial.S1, s1, pS1);
        logGap(intentoId, intento.getCif(), FactorEspacial.S2, pdS2, pS2);
        logGap(intentoId, intento.getCif(), FactorEspacial.ST, st, pSt);

        Resultado r = new Resultado();
        r.setIntentoId(intentoId);
        r.setPdS1a(pdS1a);
        r.setPdS1b(pdS1b);
        r.setPdS2(pdS2);
        r.setPercS1(pS1.percentil());
        r.setPercS2(pS2.percentil());
        r.setPercSt(pSt.percentil());
        // Resultado has a manually-assigned @Id, so save() merges; use the returned managed instance.
        Resultado saved = resultadoRepo.save(r);
        em.flush();
        em.refresh(saved); // load GENERATED pd_s1 / pd_st

        intento.setEstado(EstadoIntento.COMPLETADO);
        intento.setFechaFin(OffsetDateTime.now());

        auditoria.registrar(intentoId, intento.getCif(), "RESULTADO_CALCULADO",
                "pd_s1a=%d pd_s1b=%d pd_s2=%d perc_st=%d".formatted(pdS1a, pdS1b, pdS2, pSt.percentil()));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        log.info("RESULTADO_CALCULADO intento={} en {}ms (NFR RN-BFA-06 < 3000)", intentoId, ms);
        return saved;
    }

    private void logGap(Long intentoId, String cif, FactorEspacial factor, short score, PercentilResultado pr) {
        if (pr.fallback()) {
            auditoria.registrar(intentoId, cif, "BAREMO_GAP",
                    "factor=%s score=%d -> percentil=%d (NEXT_LOWER)".formatted(factor, score, pr.percentil()));
        }
    }
}


