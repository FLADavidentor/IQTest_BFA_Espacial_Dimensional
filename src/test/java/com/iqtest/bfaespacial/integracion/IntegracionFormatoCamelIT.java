package com.iqtest.bfaespacial.integracion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P2-B: CAMEL override produces camelCase keys. */
@SpringBootTest(properties = "app.integracion.json-format=CAMEL")
@AutoConfigureMockMvc
@Transactional
class IntegracionFormatoCamelIT extends AbstractPostgresIT {

    @Autowired MockMvc mockMvc;
    @PersistenceContext EntityManager em;

    @Test
    void formatoCamel_produceCamelCase() throws Exception {
        IntegracionFixtures.seed(em, "CIF-CAM");
        String body = mockMvc.perform(get("/api/integracion/resultados/CIF-CAM/2026-I")
                        .header("Authorization", "Bearer dev-service-token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"pdS1\":18").contains("\"percSt\":48").contains("\"fechaCalculo\"");
        assertThat(body).doesNotContain("pd_s1").doesNotContain("perc_st");
    }
}
