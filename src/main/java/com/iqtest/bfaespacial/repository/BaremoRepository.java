package com.iqtest.bfaespacial.repository;

import com.iqtest.bfaespacial.model.Baremo;
import com.iqtest.bfaespacial.model.BaremoId;
import com.iqtest.bfaespacial.model.FactorEspacial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BaremoRepository extends JpaRepository<Baremo, BaremoId> {

    Optional<Baremo> findByFactorAndPuntuacionDirecta(FactorEspacial factor, Short puntuacionDirecta);

    /** Gap fallback (§13, §19 Q1): nearest lower-or-equal direct score for the factor. */
    Optional<Baremo> findFirstByFactorAndPuntuacionDirectaLessThanEqualOrderByPuntuacionDirectaDesc(
            FactorEspacial factor, Short puntuacionDirecta);

    /** Fallback when score is below the minimum defined direct score. */
    Optional<Baremo> findFirstByFactorOrderByPuntuacionDirectaAsc(FactorEspacial factor);
}


