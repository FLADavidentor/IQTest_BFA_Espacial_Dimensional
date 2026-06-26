package com.iqtest.bfaespacial.evaluacion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.common.SubtestCerradoException;
import com.iqtest.bfaespacial.domain.*;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import com.iqtest.bfaespacial.evaluacion.aplicacion.SubtestService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class SubtestServiceIT extends AbstractPostgresIT {

    @Autowired SubtestService subtestService;
    @PersistenceContext EntityManager em;

    @Test
    void respuestaTrasCierre_esRechazada() {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026);
        v.setNumeroVersion((short) 1);
        v.setActiva(true);
        em.persist(v);

        Reactivo reactivo = new Reactivo();
        reactivo.setVersionFormulario(v);
        reactivo.setTipoSubtest(TipoSubtest.S1A);
        reactivo.setOrden((short) 1);
        reactivo.setEnunciadoImagenUrl("u");
        em.persist(reactivo);

        Intento intento = new Intento();
        intento.setCif("CIF-B");
        intento.setPeriodoAcademico("2026-I");
        intento.setVersionFormulario(v);
        em.persist(intento);
        em.flush();

        subtestService.prepararSubtest(intento.getId(), TipoSubtest.S1A);
        EjecucionSubtest ejec = subtestService.comenzarSubtest(intento.getId());

        // answer accepted while EN_CURSO
        subtestService.registrarRespuesta(ejec.getId(), reactivo.getId(), null);

        // close, then a further answer must be rejected (RN-BFA-05)
        subtestService.cerrar(ejec.getId(), false);

        assertThatThrownBy(() -> subtestService.registrarRespuesta(ejec.getId(), reactivo.getId(), null))
                .isInstanceOf(SubtestCerradoException.class);
    }
}
