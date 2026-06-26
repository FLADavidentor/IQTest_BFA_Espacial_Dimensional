package com.iqtest.bfaespacial.administracion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.administracion.catalogo.OpcionReactivoRepository;
import com.iqtest.bfaespacial.administracion.catalogo.OpcionReactivoService;
import com.iqtest.bfaespacial.domain.OpcionReactivo;
import com.iqtest.bfaespacial.domain.Reactivo;
import com.iqtest.bfaespacial.domain.VersionFormulario;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class OpcionAdminIT extends AbstractPostgresIT {

    @Autowired OpcionReactivoService opcionService;
    @Autowired OpcionReactivoRepository opcionRepo;
    @Autowired MockMvc mockMvc;
    @PersistenceContext EntityManager em;

    private Long seedReactivo() {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026); v.setNumeroVersion((short) 1); v.setActiva(true);
        em.persist(v);
        Reactivo r = new Reactivo();
        r.setVersionFormulario(v); r.setTipoSubtest(TipoSubtest.S1A); r.setOrden((short) 1);
        r.setEnunciadoImagenUrl("s1a/1.png");
        em.persist(r);
        em.flush();
        return r.getId();
    }

    @Test
    void exactamenteUnaCorrecta() {
        Long rid = seedReactivo();
        opcionService.agregar(rid, "A", true);
        opcionService.agregar(rid, "B", true);  // must flip A to false

        List<OpcionReactivo> ops = opcionRepo.findByReactivoIdOrderByEtiqueta(rid);
        assertThat(ops).hasSize(2);
        assertThat(ops.stream().filter(OpcionReactivo::isEsCorrecta).count()).isEqualTo(1);
        Long aId = ops.stream().filter(o -> o.getEtiqueta().equals("A")).findFirst().orElseThrow().getId();
        assertThat(ops.stream().filter(o -> o.getEtiqueta().equals("B")).findFirst().orElseThrow().isEsCorrecta()).isTrue();

        opcionService.marcarCorrecta(aId);   // switch correct back to A
        List<OpcionReactivo> after = opcionRepo.findByReactivoIdOrderByEtiqueta(rid);
        assertThat(after.stream().filter(OpcionReactivo::isEsCorrecta).count()).isEqualTo(1);
        assertThat(after.stream().filter(o -> o.getEtiqueta().equals("A")).findFirst().orElseThrow().isEsCorrecta()).isTrue();
    }

    @Test @WithMockUser(roles = "ADMIN")
    void paginasAdminRenderizan() throws Exception {
        Long rid = seedReactivo();
        mockMvc.perform(get("/admin/reactivos/" + rid + "/opciones"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Agregar opción")));
        mockMvc.perform(get("/admin/imagenes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Subir imagen")));
    }
}
