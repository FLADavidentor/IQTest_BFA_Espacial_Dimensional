package com.iqtest.bfaespacial.repository;

import com.iqtest.bfaespacial.model.Intento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IntentoRepository extends JpaRepository<Intento, Long> {

    Optional<Intento> findByCifAndPeriodoAcademico(String cif, String periodoAcademico);

    boolean existsByCifAndPeriodoAcademico(String cif, String periodoAcademico);

    /** Eager-fetch versionFormulario to avoid LazyInitializationException (open-in-view=false). */
    @Query("SELECT i FROM Intento i JOIN FETCH i.versionFormulario ORDER BY i.id DESC")
    java.util.List<Intento> findAllByOrderByIdDesc();
}



