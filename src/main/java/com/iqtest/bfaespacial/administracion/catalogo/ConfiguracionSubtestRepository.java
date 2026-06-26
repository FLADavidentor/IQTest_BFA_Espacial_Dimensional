package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.ConfiguracionSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionSubtestRepository extends JpaRepository<ConfiguracionSubtest, TipoSubtest> {
}
