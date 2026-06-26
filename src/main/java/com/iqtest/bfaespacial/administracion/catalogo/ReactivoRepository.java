package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.Reactivo;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReactivoRepository extends JpaRepository<Reactivo, Long> {

    // Subtest delivery: active items only (P0-B soft-delete).
    List<Reactivo> findByVersionFormularioIdAndTipoSubtestAndActivoTrueOrderByOrden(
            Long versionFormularioId, TipoSubtest tipoSubtest);

    List<Reactivo> findAllByOrderByIdAsc();
}
