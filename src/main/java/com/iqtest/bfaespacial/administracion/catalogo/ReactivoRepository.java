package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.Reactivo;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReactivoRepository extends JpaRepository<Reactivo, Long> {

    List<Reactivo> findByVersionFormularioIdAndTipoSubtestOrderByOrden(Long versionFormularioId, TipoSubtest tipoSubtest);
}
