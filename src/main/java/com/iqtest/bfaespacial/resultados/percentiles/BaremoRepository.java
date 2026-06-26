package com.iqtest.bfaespacial.resultados.percentiles;

import com.iqtest.bfaespacial.domain.Baremo;
import com.iqtest.bfaespacial.domain.BaremoId;
import com.iqtest.bfaespacial.domain.enums.FactorEspacial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BaremoRepository extends JpaRepository<Baremo, BaremoId> {

    Optional<Baremo> findByFactorAndPuntuacionDirecta(FactorEspacial factor, Short puntuacionDirecta);

    /** Gap fallback (§13, §19 Q1): nearest lower-or-equal direct score for the factor. */
    Optional<Baremo> findFirstByFactorAndPuntuacionDirectaLessThanEqualOrderByPuntuacionDirectaDesc(
            FactorEspacial factor, Short puntuacionDirecta);
}
