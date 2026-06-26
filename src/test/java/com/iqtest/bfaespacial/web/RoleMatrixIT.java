package com.iqtest.bfaespacial.web;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 7-B: every route rejects the wrong role; actuator is not public. */
@AutoConfigureMockMvc
class RoleMatrixIT extends AbstractPostgresIT {

    @Autowired MockMvc mockMvc;

    @Test @WithMockUser(roles = "ESTUDIANTE")
    void estudiante_noAdminNoResultadosNoActuator() throws Exception {
        mockMvc.perform(get("/admin/reactivos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/resultados/C/2026-I")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "EVALUADOR")
    void evaluador_noAdminNoEvaluacionNoApiNoActuator() throws Exception {
        mockMvc.perform(get("/admin/reactivos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/evaluacion/inicio")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/subtest/current")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void admin_noEvaluacionNoResultadosNoApi_butActuatorOk() throws Exception {
        mockMvc.perform(get("/evaluacion/inicio")).andExpect(status().isForbidden());
        mockMvc.perform(get("/resultados/C/2026-I")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/subtest/current")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()); // ADMIN may read
    }

    @Test
    void anonimo_actuatorNoPublico() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().is3xxRedirection());
    }
}
