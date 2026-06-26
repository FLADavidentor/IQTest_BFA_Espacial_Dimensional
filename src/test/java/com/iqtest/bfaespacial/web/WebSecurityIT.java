package com.iqtest.bfaespacial.web;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.domain.*;
import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class WebSecurityIT extends AbstractPostgresIT {

    @Autowired MockMvc mockMvc;
    @PersistenceContext EntityManager em;

    @Test
    void anonimo_inicioEvaluacion_redirigeALogin() throws Exception {
        mockMvc.perform(get("/evaluacion/inicio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void estudiante_resultados_403() throws Exception {
        mockMvc.perform(get("/resultados/CIF-X/2026-I"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiError_sinStackTrace() throws Exception {
        // integracion endpoint without token -> 401 ProblemDetail, no leaked trace
        String body = mockMvc.perform(get("/api/integracion/resultados/X/2026-I"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("at com.iqtest");
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @Transactional
    void apiSubtestCurrent_sinEsCorrecta() throws Exception {
        // seed an intento for the dev CIF (SesionIQTestClient stub) with one EN_CURSO subtest + item
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026);
        v.setNumeroVersion((short) 1);
        v.setActiva(true);
        em.persist(v);

        Intento intento = new Intento();
        intento.setCif("DEV0000001");       // = app.iqtest.dev-cif
        intento.setPeriodoAcademico("2026-I"); // = app.periodo-academico-actual
        intento.setVersionFormulario(v);
        em.persist(intento);

        EjecucionSubtest ejec = new EjecucionSubtest();
        ejec.setIntento(intento);
        ejec.setTipoSubtest(TipoSubtest.S1A);
        ejec.setEstado(EstadoSubtest.EN_CURSO);
        em.persist(ejec);

        Reactivo rx = new Reactivo();
        rx.setVersionFormulario(v);
        rx.setTipoSubtest(TipoSubtest.S1A);
        rx.setOrden((short) 1);
        rx.setEnunciadoImagenUrl("https://img/s1a-1.png");
        em.persist(rx);

        OpcionReactivo a = new OpcionReactivo();
        a.setReactivo(rx); a.setEtiqueta("A"); a.setEsCorrecta(true);
        em.persist(a);
        OpcionReactivo b = new OpcionReactivo();
        b.setReactivo(rx); b.setEtiqueta("B"); b.setEsCorrecta(false);
        em.persist(b);
        em.flush();

        String body = mockMvc.perform(get("/api/subtest/current"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("etiqueta");          // options are present
        assertThat(body).doesNotContain("esCorrecta");  // RN-BFA-08
        assertThat(body).doesNotContain("es_correcta");
    }
}
