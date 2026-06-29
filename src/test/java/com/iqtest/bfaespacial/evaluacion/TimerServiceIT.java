package com.iqtest.bfaespacial.evaluacion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.model.*;
import com.iqtest.bfaespacial.model.EstadoSubtest;
import com.iqtest.bfaespacial.model.TipoSubtest;
import com.iqtest.bfaespacial.service.TimerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TimerServiceIT extends AbstractPostgresIT {

    @Autowired TimerService timerService;
    @PersistenceContext EntityManager em;

    @Test
    void ejecucionExpirada_seCierraPorTiempo() {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026);
        v.setNumeroVersion((short) 1);
        v.setActiva(true);
        em.persist(v);

        Intento intento = new Intento();
        intento.setCif("CIF-C");
        intento.setPeriodoAcademico("2026-I");
        intento.setVersionFormulario(v);
        em.persist(intento);

        // S1A limit = 180s; started 1000s ago => expired
        EjecucionSubtest ejec = new EjecucionSubtest();
        ejec.setIntento(intento);
        ejec.setTipoSubtest(TipoSubtest.S1A);
        ejec.setEstado(EstadoSubtest.EN_CURSO);
        ejec.setFechaInicio(OffsetDateTime.now().minusSeconds(1000));
        em.persist(ejec);
        em.flush();

        timerService.cerrarExpirados();
        em.flush();
        em.clear();

        EjecucionSubtest reloaded = em.find(EjecucionSubtest.class, ejec.getId());
        assertThat(reloaded.getEstado()).isEqualTo(EstadoSubtest.CERRADO_POR_TIEMPO);
        assertThat(reloaded.isCerradaPorTiempo()).isTrue();
        assertThat(reloaded.getFechaCierre()).isNotNull();
    }
}

