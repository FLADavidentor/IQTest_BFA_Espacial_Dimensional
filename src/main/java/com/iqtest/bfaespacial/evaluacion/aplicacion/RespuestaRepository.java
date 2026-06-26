package com.iqtest.bfaespacial.evaluacion.aplicacion;

import com.iqtest.bfaespacial.domain.Respuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {

    // Upsert key (UNIQUE(ejecucion_subtest_id, reactivo_id))
    Optional<Respuesta> findByEjecucionSubtestIdAndReactivoId(Long ejecucionSubtestId, Long reactivoId);
}
