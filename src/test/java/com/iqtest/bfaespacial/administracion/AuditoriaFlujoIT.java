package com.iqtest.bfaespacial.administracion;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.repository.RegistroAuditoriaRepository;
import com.iqtest.bfaespacial.model.*;
import com.iqtest.bfaespacial.model.FactorEspacial;
import com.iqtest.bfaespacial.model.TipoSubtest;
import com.iqtest.bfaespacial.service.SubtestService;
import com.iqtest.bfaespacial.service.IntentoService;
import com.iqtest.bfaespacial.service.SincronizacionService;
import com.iqtest.bfaespacial.service.SincronizacionService.RespuestaPendiente;
import com.iqtest.bfaespacial.repository.BaremoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** P1-B gate: a full attempt records all required audit events in chronological order. */
@Transactional
class AuditoriaFlujoIT extends AbstractPostgresIT {

    @Autowired IntentoService intentoService;
    @Autowired SubtestService subtestService;
    @Autowired SincronizacionService sincronizacionService;
    @Autowired BaremoRepository baremoRepo;
    @Autowired RegistroAuditoriaRepository auditoriaRepo;
    @PersistenceContext EntityManager em;

    @Test
    void atencionCompleta_registraTodosLosEventos() {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026); v.setNumeroVersion((short) 1); v.setActiva(true);
        em.persist(v);
        em.flush();

        // S1A item with a correct option (so the synced answer counts)
        Long iidVer = v.getId();
        var intento = intentoService.crear("CIF-AUD", "2026-I");          // INTENTO_CREADO
        Long iid = intento.getId();
        intentoService.iniciarOReanudar("CIF-AUD", "2026-I");             // INTENTO_REANUDADO

        Reactivo rx = new Reactivo();
        rx.setVersionFormulario(em.getReference(VersionFormulario.class, iidVer));
        rx.setTipoSubtest(TipoSubtest.S1A); rx.setOrden((short) 1); rx.setEnunciadoImagenUrl("u");
        em.persist(rx);
        OpcionReactivo a = new OpcionReactivo();
        a.setReactivo(rx); a.setEtiqueta("A"); a.setEsCorrecta(true);
        em.persist(a);
        em.flush();

        // baremo for the resulting scores: pd_s1a=1 -> s1=1, st=1; s2=0
        baremoRepo.save(new Baremo(FactorEspacial.S1, (short) 1, (short) 50));
        baremoRepo.save(new Baremo(FactorEspacial.S2, (short) 0, (short) 10));
        baremoRepo.save(new Baremo(FactorEspacial.ST, (short) 1, (short) 45));

        subtestService.prepararSubtest(iid, TipoSubtest.S1A);
        EjecucionSubtest e1 = subtestService.comenzarSubtest(iid);        // SUBTEST_INICIADO
        sincronizacionService.sincronizar(e1.getId(),
                List.of(new RespuestaPendiente(rx.getId(), a.getId())));  // SYNC_RECIBIDA
        subtestService.cerrar(e1.getId(), false);                        // SUBTEST_CERRADO_MANUAL -> S2

        EjecucionSubtest e2 = subtestService.comenzarSubtest(iid);        // SUBTEST_INICIADO
        subtestService.cerrar(e2.getId(), true);                         // SUBTEST_CERRADO_POR_TIEMPO -> S1B

        EjecucionSubtest e3 = subtestService.comenzarSubtest(iid);        // SUBTEST_INICIADO
        subtestService.cerrar(e3.getId(), false);                        // last -> RESULTADO_CALCULADO

        List<String> acciones = auditoriaRepo.findByIntentoIdOrderByFechaHoraAscIdAsc(iid)
                .stream().map(RegistroAuditoria::getAccion).toList();

        assertThat(acciones).contains(
                "INTENTO_CREADO", "INTENTO_REANUDADO", "SUBTEST_INICIADO",
                "SUBTEST_CERRADO_MANUAL", "SUBTEST_CERRADO_POR_TIEMPO",
                "SYNC_RECIBIDA", "RESULTADO_CALCULADO");
        assertThat(acciones.get(0)).isEqualTo("INTENTO_CREADO");
        assertThat(acciones.get(acciones.size() - 1)).isEqualTo("RESULTADO_CALCULADO");
    }
}

