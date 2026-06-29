package com.iqtest.bfaespacial.web;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.model.*;
import com.iqtest.bfaespacial.model.EstadoSubtest;
import com.iqtest.bfaespacial.model.TipoSubtest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regression: UC6 review page must render (lazy associations) with open-in-view off. */
@AutoConfigureMockMvc
@Transactional
class ResultadosRevisionIT extends AbstractPostgresIT {

    @Autowired MockMvc mockMvc;
    @PersistenceContext EntityManager em;

    @Test
    @WithMockUser(roles = "EVALUADOR")
    void revisionRespuestas_renderiza() throws Exception {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026); v.setNumeroVersion((short) 1); v.setActiva(true);
        em.persist(v);
        Intento i = new Intento();
        i.setCif("CIF-REV"); i.setPeriodoAcademico("2026-I"); i.setVersionFormulario(v);
        em.persist(i);
        EjecucionSubtest e = new EjecucionSubtest();
        e.setIntento(i); e.setTipoSubtest(TipoSubtest.S1A); e.setEstado(EstadoSubtest.COMPLETADO);
        em.persist(e);
        Reactivo r = new Reactivo();
        r.setVersionFormulario(v); r.setTipoSubtest(TipoSubtest.S1A); r.setOrden((short) 1);
        r.setEnunciadoImagenUrl("/img/1.png");
        em.persist(r);
        OpcionReactivo a = new OpcionReactivo();
        a.setReactivo(r); a.setEtiqueta("A"); a.setEsCorrecta(true);
        em.persist(a);
        Respuesta resp = new Respuesta();
        resp.setEjecucionSubtest(e); resp.setReactivo(r); resp.setOpcionReactivo(a);
        em.persist(resp);
        em.flush();

        mockMvc.perform(get("/resultados/CIF-REV/2026-I/respuestas"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Correcta")));
    }
}

