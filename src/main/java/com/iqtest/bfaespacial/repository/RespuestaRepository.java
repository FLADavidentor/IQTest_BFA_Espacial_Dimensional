package com.iqtest.bfaespacial.repository;
import com.iqtest.bfaespacial.model.Intento;
import com.iqtest.bfaespacial.model.EjecucionSubtest;
import com.iqtest.bfaespacial.model.OpcionReactivo;

import com.iqtest.bfaespacial.model.Respuesta;
import com.iqtest.bfaespacial.model.TipoSubtest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {

    // Upsert key (UNIQUE(ejecucion_subtest_id, reactivo_id))
    Optional<Respuesta> findByEjecucionSubtestIdAndReactivoId(Long ejecucionSubtestId, Long reactivoId);

    // UC6 review: all answers of an intento.
    java.util.List<Respuesta> findByEjecucionSubtestIntentoId(Long intentoId);

    /** §13: correct-answer count for one subtest type within an intento. */
    @Query("""
            SELECT COUNT(r) FROM Respuesta r
            WHERE r.ejecucionSubtest.intento.id = :intentoId
              AND r.ejecucionSubtest.tipoSubtest = :tipo
              AND r.opcionReactivo.esCorrecta = true
            """)
    long countCorrectas(@Param("intentoId") Long intentoId, @Param("tipo") TipoSubtest tipo);
}


