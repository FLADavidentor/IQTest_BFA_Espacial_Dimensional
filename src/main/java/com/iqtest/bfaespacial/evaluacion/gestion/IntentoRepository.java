package com.iqtest.bfaespacial.evaluacion.gestion;

import com.iqtest.bfaespacial.domain.Intento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntentoRepository extends JpaRepository<Intento, Long> {

    Optional<Intento> findByCifAndPeriodoAcademico(String cif, String periodoAcademico);

    boolean existsByCifAndPeriodoAcademico(String cif, String periodoAcademico);
}
