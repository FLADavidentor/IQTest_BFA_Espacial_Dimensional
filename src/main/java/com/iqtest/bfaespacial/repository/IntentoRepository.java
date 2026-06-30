package com.iqtest.bfaespacial.repository;

import com.iqtest.bfaespacial.model.Intento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntentoRepository extends JpaRepository<Intento, Long> {

    Optional<Intento> findByCifAndPeriodoAcademico(String cif, String periodoAcademico);

    boolean existsByCifAndPeriodoAcademico(String cif, String periodoAcademico);

    java.util.List<Intento> findAllByOrderByIdDesc();
}


