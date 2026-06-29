package com.iqtest.bfaespacial.evaluacion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.repository.VersionFormularioRepository;
import com.iqtest.bfaespacial.common.IntentoConflictException;
import com.iqtest.bfaespacial.model.VersionFormulario;
import com.iqtest.bfaespacial.service.IntentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class IntentoServiceIT extends AbstractPostgresIT {

    @Autowired IntentoService intentoService;
    @Autowired VersionFormularioRepository versionRepo;

    @Test
    void segundoIntentoMismoPeriodo_lanzaConflicto() {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026);
        v.setNumeroVersion((short) 1);
        v.setActiva(true);
        versionRepo.saveAndFlush(v);

        intentoService.crear("CIF-A", "2026-I");

        assertThatThrownBy(() -> intentoService.crear("CIF-A", "2026-I"))
                .isInstanceOf(IntentoConflictException.class);
    }
}

