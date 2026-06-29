package com.iqtest.bfaespacial.administracion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.repository.VersionFormularioRepository;
import com.iqtest.bfaespacial.service.VersionFormularioService;
import com.iqtest.bfaespacial.service.VersionFormularioService.ActivacionRequiereConfirmacion;
import com.iqtest.bfaespacial.model.VersionFormulario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class VersionAdminIT extends AbstractPostgresIT {

    @Autowired VersionFormularioService service;
    @Autowired VersionFormularioRepository repo;

    @Test
    void activar_desactivaLasDemasDelMismoAnio() {
        Long v1 = service.crear((short) 2026, (short) 1, true).getId();
        Long v2 = service.crear((short) 2026, (short) 2, false).getId();

        // activating v2 needs confirmation (v1 already active)
        assertThatThrownBy(() -> service.activar(v2, false))
                .isInstanceOf(ActivacionRequiereConfirmacion.class);

        service.activar(v2, true);

        assertThat(repo.findById(v1).orElseThrow().isActiva()).isFalse();
        assertThat(repo.findById(v2).orElseThrow().isActiva()).isTrue();
        assertThat(repo.findByAnio((short) 2026).stream().filter(VersionFormulario::isActiva).count()).isEqualTo(1);
    }
}

