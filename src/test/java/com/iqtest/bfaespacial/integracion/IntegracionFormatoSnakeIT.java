package com.iqtest.bfaespacial.integracion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P2-B: default Dashboard format is snake_case. */
@AutoConfigureMockMvc
@Transactional
class IntegracionFormatoSnakeIT extends AbstractPostgresIT {

    @Autowired MockMvc mockMvc;
    @PersistenceContext EntityManager em;

    @Test
    void formatoPorDefecto_esSnakeCase() throws Exception {
        IntegracionFixtures.seed(em, "CIF-SNK");
        String body = mockMvc.perform(get("/api/integracion/resultados/CIF-SNK/2026-I")
                        .header("Authorization", "Bearer dev-service-token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"pd_s1\":18").contains("\"perc_st\":48").contains("\"fecha_calculo\"");
        assertThat(body).doesNotContain("pdS1").doesNotContain("percSt");
    }
}
