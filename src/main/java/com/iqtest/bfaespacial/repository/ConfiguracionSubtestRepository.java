package com.iqtest.bfaespacial.repository;

import com.iqtest.bfaespacial.model.ConfiguracionSubtest;
import com.iqtest.bfaespacial.model.TipoSubtest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionSubtestRepository extends JpaRepository<ConfiguracionSubtest, TipoSubtest> {
}


