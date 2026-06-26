package com.iqtest.bfaespacial.evaluacion.aplicacion;

import com.iqtest.bfaespacial.domain.EjecucionSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EjecucionSubtestRepository extends JpaRepository<EjecucionSubtest, Long> {

    Optional<EjecucionSubtest> findByIntentoIdAndTipoSubtest(Long intentoId, TipoSubtest tipoSubtest);

    Optional<EjecucionSubtest> findFirstByIntentoIdAndEstado(
            Long intentoId, com.iqtest.bfaespacial.domain.enums.EstadoSubtest estado);

    /** Server-side timer (§12): EN_CURSO executions past fecha_inicio + tiempo_limite_seg. */
    @Query(value = """
            SELECT e.id FROM ejecucion_subtest e
            JOIN configuracion_subtest c ON e.tipo_subtest = c.tipo_subtest
            WHERE e.estado = 'EN_CURSO'
              AND e.fecha_inicio + make_interval(secs => c.tiempo_limite_seg) < NOW()
            """, nativeQuery = true)
    List<Long> findIdsExpirados();
}
