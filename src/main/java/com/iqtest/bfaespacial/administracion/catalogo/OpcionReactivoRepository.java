package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.OpcionReactivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcionReactivoRepository extends JpaRepository<OpcionReactivo, Long> {

    List<OpcionReactivo> findByReactivoIdOrderByEtiqueta(Long reactivoId);

    java.util.Optional<OpcionReactivo> findByReactivoIdAndEsCorrectaTrue(Long reactivoId);
}
