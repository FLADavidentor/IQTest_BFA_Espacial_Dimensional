package com.iqtest.bfaespacial.evaluacion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.administracion.catalogo.VersionFormularioRepository;
import com.iqtest.bfaespacial.domain.EjecucionSubtest;
import com.iqtest.bfaespacial.domain.Intento;
import com.iqtest.bfaespacial.domain.VersionFormulario;
import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import com.iqtest.bfaespacial.evaluacion.aplicacion.EjecucionSubtestRepository;
import com.iqtest.bfaespacial.evaluacion.gestion.IntentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** P2-A gate: the 1s scheduler auto-closes an expired subtest well under 2s. */
class TimerLatencyIT extends AbstractPostgresIT {

    @Autowired EjecucionSubtestRepository ejecucionRepo;
    @Autowired IntentoRepository intentoRepo;
    @Autowired VersionFormularioRepository versionRepo;

    @Test
    void schedulerCierraExpirado_enMenosDe2s() throws InterruptedException {
        // committed seed (no @Transactional) so the @Scheduled task sees it
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026); v.setNumeroVersion((short) 9); v.setActiva(false);
        v = versionRepo.save(v);

        Intento i = new Intento();
        i.setCif("CIF-LAT"); i.setPeriodoAcademico("2026-LAT"); i.setVersionFormulario(v);
        i = intentoRepo.save(i);

        EjecucionSubtest e = new EjecucionSubtest();
        e.setIntento(i); e.setTipoSubtest(TipoSubtest.S1A); e.setEstado(EstadoSubtest.EN_CURSO);
        e.setFechaInicio(OffsetDateTime.now().minusSeconds(300)); // already expired (limit 180)
        e = ejecucionRepo.save(e);
        Long eid = e.getId();

        // Leftover rows are harmless to other tests (unique cif, inactive version) — no cleanup.
        long t0 = System.currentTimeMillis();
        long elapsed = -1;
        for (int n = 0; n < 40; n++) { // up to 4s
            Thread.sleep(100);
            EstadoSubtest est = ejecucionRepo.findById(eid).orElseThrow().getEstado();
            if (est == EstadoSubtest.CERRADO_POR_TIEMPO) { elapsed = System.currentTimeMillis() - t0; break; }
        }
        assertThat(elapsed).as("scheduler close latency ms").isGreaterThan(0).isLessThan(2000);
    }
}
