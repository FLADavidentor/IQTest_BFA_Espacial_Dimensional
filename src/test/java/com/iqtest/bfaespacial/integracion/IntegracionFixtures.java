package com.iqtest.bfaespacial.integracion;

import com.iqtest.bfaespacial.model.Intento;
import com.iqtest.bfaespacial.model.Resultado;
import com.iqtest.bfaespacial.model.VersionFormulario;
import jakarta.persistence.EntityManager;

/** Shared seed: an intento with a computed resultado (pd_s1=18, perc_st=48). */
final class IntegracionFixtures {
    private IntegracionFixtures() {}

    static void seed(EntityManager em, String cif) {
        VersionFormulario v = new VersionFormulario();
        v.setAnio((short) 2026); v.setNumeroVersion((short) 1); v.setActiva(true);
        em.persist(v);
        Intento i = new Intento();
        i.setCif(cif); i.setPeriodoAcademico("2026-I"); i.setVersionFormulario(v);
        em.persist(i);
        em.flush();

        Resultado r = new Resultado();
        r.setIntentoId(i.getId());
        r.setPdS1a((short) 10); r.setPdS1b((short) 8); r.setPdS2((short) 15);
        r.setPercS1((short) 55); r.setPercS2((short) 40); r.setPercSt((short) 48);
        em.persist(r);
        em.flush();
    }
}

