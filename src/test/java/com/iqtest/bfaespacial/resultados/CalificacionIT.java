package com.iqtest.bfaespacial.resultados;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.administracion.auditoria.RegistroAuditoriaRepository;
import com.iqtest.bfaespacial.domain.*;
import com.iqtest.bfaespacial.domain.enums.EstadoIntento;
import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.domain.enums.FactorEspacial;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import com.iqtest.bfaespacial.evaluacion.gestion.IntentoRepository;
import com.iqtest.bfaespacial.resultados.calificacion.CalificacionService;
import com.iqtest.bfaespacial.resultados.percentiles.BaremoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CalificacionIT extends AbstractPostgresIT {

    @Autowired CalificacionService calificacionService;
    @Autowired BaremoRepository baremoRepo;
    @Autowired IntentoRepository intentoRepo;
    @Autowired RegistroAuditoriaRepository auditoriaRepo;
    @PersistenceContext EntityManager em;

    private VersionFormulario version;
    private short orden = 1;

    @Test
    void calificaConteosGeneradasYPercentiles_conGapEnST() {
        version = new VersionFormulario();
        version.setAnio((short) 2026);
        version.setNumeroVersion((short) 1);
        version.setActiva(true);
        em.persist(version);

        Intento intento = new Intento();
        intento.setCif("CIF-CAL");
        intento.setPeriodoAcademico("2026-I");
        intento.setVersionFormulario(version);
        em.persist(intento);

        // pd_s1a=10, pd_s1b=8, pd_s2=15  -> pd_s1=18, pd_st=33
        EjecucionSubtest e1a = ejec(intento, TipoSubtest.S1A);
        for (int i = 0; i < 10; i++) answer(e1a, TipoSubtest.S1A, true);

        EjecucionSubtest e1b = ejec(intento, TipoSubtest.S1B);
        for (int i = 0; i < 8; i++) answer(e1b, TipoSubtest.S1B, true);

        EjecucionSubtest e2 = ejec(intento, TipoSubtest.S2);
        for (int i = 0; i < 15; i++) answer(e2, TipoSubtest.S2, true);
        em.flush();

        // No baremo seeding: assert against the REAL Normas data loaded by V6.
        long t0 = System.nanoTime();
        Resultado r = calificacionService.calificar(intento.getId());
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertThat(ms).as("RN-BFA-06: resultado < 3s").isLessThan(3000);

        // direct-score counts
        assertThat(r.getPdS1a()).isEqualTo((short) 10);
        assertThat(r.getPdS1b()).isEqualTo((short) 8);
        assertThat(r.getPdS2()).isEqualTo((short) 15);
        // GENERATED columns
        assertThat(r.getPdS1()).isEqualTo((short) 18);
        assertThat(r.getPdSt()).isEqualTo((short) 33);
        // REAL Normas percentiles: S1@18=70, S2@15=65, ST@33=65
        assertThat(r.getPercS1()).isEqualTo((short) 70);
        assertThat(r.getPercS2()).isEqualTo((short) 65);
        assertThat(r.getPercSt()).isEqualTo((short) 65);

        Intento done = intentoRepo.findById(intento.getId()).orElseThrow();
        assertThat(done.getEstado()).isEqualTo(EstadoIntento.COMPLETADO);
        assertThat(done.getFechaFin()).isNotNull();
    }

    private EjecucionSubtest ejec(Intento intento, TipoSubtest tipo) {
        EjecucionSubtest e = new EjecucionSubtest();
        e.setIntento(intento);
        e.setTipoSubtest(tipo);
        e.setEstado(EstadoSubtest.COMPLETADO);
        em.persist(e);
        return e;
    }

    private void answer(EjecucionSubtest ejec, TipoSubtest tipo, boolean correct) {
        Reactivo rx = new Reactivo();
        rx.setVersionFormulario(version);
        rx.setTipoSubtest(tipo);
        rx.setOrden(orden++);
        rx.setEnunciadoImagenUrl("u");
        em.persist(rx);

        OpcionReactivo op = new OpcionReactivo();
        op.setReactivo(rx);
        op.setEtiqueta(correct ? "A" : "B");
        op.setEsCorrecta(correct);
        em.persist(op);

        Respuesta rsp = new Respuesta();
        rsp.setEjecucionSubtest(ejec);
        rsp.setReactivo(rx);
        rsp.setOpcionReactivo(op);
        em.persist(rsp);
    }
}
