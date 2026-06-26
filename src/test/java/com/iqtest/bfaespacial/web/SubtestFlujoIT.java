package com.iqtest.bfaespacial.web;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.domain.*;
import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import com.iqtest.bfaespacial.evaluacion.aplicacion.TimerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Automated walkthrough of UC2 server-side: load -> answer -> expire -> closed. */
@AutoConfigureMockMvc
@Transactional
class SubtestFlujoIT extends AbstractPostgresIT {

    @Autowired MockMvc mockMvc;
    @Autowired TimerService timerService;
    @PersistenceContext EntityManager em;

    @Test
    void flujoCompleto_respondeYCierraPorTiempo() throws Exception {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026); v.setNumeroVersion((short) 1); v.setActiva(true);
        em.persist(v);

        Intento intento = new Intento();
        intento.setCif("DEV0000001");
        intento.setPeriodoAcademico("2026-I");
        intento.setVersionFormulario(v);
        em.persist(intento);

        EjecucionSubtest ejec = new EjecucionSubtest();
        ejec.setIntento(intento);
        ejec.setTipoSubtest(TipoSubtest.S1A);
        ejec.setEstado(EstadoSubtest.EN_CURSO);
        em.persist(ejec);

        Reactivo rx = new Reactivo();
        rx.setVersionFormulario(v); rx.setTipoSubtest(TipoSubtest.S1A);
        rx.setOrden((short) 1); rx.setEnunciadoImagenUrl("u");
        em.persist(rx);

        OpcionReactivo a = new OpcionReactivo();
        a.setReactivo(rx); a.setEtiqueta("A"); a.setEsCorrecta(true);
        em.persist(a);
        em.flush();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(
                        formLogin().user("estudiante").password("x"))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();

        // load current subtest (carries ejecucionSubtestId, no es_correcta)
        mockMvc.perform(get("/api/subtest/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejecucionSubtestId").value(ejec.getId()))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("esCorrecta"))));

        // answer an item
        String body = "{\"ejecucionSubtestId\":%d,\"reactivoId\":%d,\"opcionReactivoId\":%d}"
                .formatted(ejec.getId(), rx.getId(), a.getId());
        mockMvc.perform(post("/api/respuesta").session(session)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());

        // force expiry, run the server timer
        ejec.setFechaInicio(OffsetDateTime.now().minusSeconds(1000));
        em.flush();
        timerService.cerrarExpirados();
        em.clear();

        // S1A is closed by time (server-enforced)
        EjecucionSubtest reloaded = em.find(EjecucionSubtest.class, ejec.getId());
        org.assertj.core.api.Assertions.assertThat(reloaded.getEstado())
                .isEqualTo(EstadoSubtest.CERRADO_POR_TIEMPO);

        // flow advanced to the next subtest as PENDIENTE (consigna not yet started -> full time)
        mockMvc.perform(get("/api/subtest/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtestType").value("S2"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }
}
