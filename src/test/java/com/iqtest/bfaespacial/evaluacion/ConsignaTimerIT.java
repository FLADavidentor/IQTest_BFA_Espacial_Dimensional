package com.iqtest.bfaespacial.evaluacion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.model.EjecucionSubtest;
import com.iqtest.bfaespacial.model.Intento;
import com.iqtest.bfaespacial.model.VersionFormulario;
import com.iqtest.bfaespacial.model.EstadoSubtest;
import com.iqtest.bfaespacial.model.TipoSubtest;
import com.iqtest.bfaespacial.service.SubtestService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** P1-A gate: timer does not start until "Comenzar" — fecha_inicio NULL before, set after. */
@Transactional
class ConsignaTimerIT extends AbstractPostgresIT {

    @Autowired SubtestService subtestService;
    @PersistenceContext EntityManager em;

    @Test
    void fechaInicio_nullAntesDeComenzar_setAlComenzar() {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026); v.setNumeroVersion((short) 1); v.setActiva(true);
        em.persist(v);
        Intento i = new Intento();
        i.setCif("CIF-CON"); i.setPeriodoAcademico("2026-I"); i.setVersionFormulario(v);
        em.persist(i);
        em.flush();

        EjecucionSubtest prep = subtestService.prepararSubtest(i.getId(), TipoSubtest.S1A);
        assertThat(prep.getEstado()).isEqualTo(EstadoSubtest.PENDIENTE);
        assertThat(prep.getFechaInicio()).isNull();                 // timer NOT running

        EjecucionSubtest started = subtestService.comenzarSubtest(i.getId());
        assertThat(started.getEstado()).isEqualTo(EstadoSubtest.EN_CURSO);
        assertThat(started.getFechaInicio()).isNotNull();           // timer started on Comenzar
    }
}

