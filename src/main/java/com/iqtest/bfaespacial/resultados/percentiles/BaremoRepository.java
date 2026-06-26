package com.iqtest.bfaespacial.resultados.percentiles;

import com.iqtest.bfaespacial.domain.Baremo;
import com.iqtest.bfaespacial.domain.BaremoId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaremoRepository extends JpaRepository<Baremo, BaremoId> {
}
