package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.VersionFormulario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionFormularioRepository extends JpaRepository<VersionFormulario, Long> {

    // §19 Q5 default: active version selected by admin flag, newest year first.
    Optional<VersionFormulario> findFirstByActivaTrueOrderByAnioDesc();

    List<VersionFormulario> findByAnio(Short anio);
}
