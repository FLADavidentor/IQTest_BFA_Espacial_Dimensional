package com.iqtest.bfaespacial;

import com.iqtest.bfaespacial.model.*;
import com.iqtest.bfaespacial.model.EstadoSubtest;
import com.iqtest.bfaespacial.model.FactorEspacial;
import com.iqtest.bfaespacial.model.TipoSubtest;
import com.iqtest.bfaespacial.repository.BaremoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DataLayerIT extends AbstractPostgresIT {

    @PersistenceContext
    EntityManager em;

    @Autowired
    BaremoRepository baremoRepo;

    @Test
    void insertChain_readsBackGeneratedColumns() {
        VersionFormulario version = new VersionFormulario();
        version.setAnio((short) 2026);
        version.setNumeroVersion((short) 1);
        version.setActiva(true);
        em.persist(version);

        Reactivo reactivo = new Reactivo();
        reactivo.setVersionFormulario(version);
        reactivo.setTipoSubtest(TipoSubtest.S1A);
        reactivo.setOrden((short) 1);
        reactivo.setEnunciadoImagenUrl("https://img/local/s1a-1.png");
        em.persist(reactivo);

        OpcionReactivo opcionA = new OpcionReactivo();
        opcionA.setReactivo(reactivo);
        opcionA.setEtiqueta("A");
        opcionA.setEsCorrecta(true);
        em.persist(opcionA);

        Intento intento = new Intento();
        intento.setCif("DEV0000001");
        intento.setPeriodoAcademico("2026-I");
        intento.setVersionFormulario(version);
        em.persist(intento);

        EjecucionSubtest ejec = new EjecucionSubtest();
        ejec.setIntento(intento);
        ejec.setTipoSubtest(TipoSubtest.S1A);
        ejec.setEstado(EstadoSubtest.EN_CURSO);
        em.persist(ejec);

        Respuesta respuesta = new Respuesta();
        respuesta.setEjecucionSubtest(ejec);
        respuesta.setReactivo(reactivo);
        respuesta.setOpcionReactivo(opcionA);
        em.persist(respuesta);
        em.flush();

        Resultado r = new Resultado();
        r.setIntentoId(intento.getId());
        r.setPdS1a((short) 10);
        r.setPdS1b((short) 5);
        r.setPdS2((short) 8);
        r.setPercS1((short) 50);
        r.setPercS2((short) 40);
        r.setPercSt((short) 45);
        em.persist(r);
        em.flush();
        em.refresh(r);

        assertThat(r.getPdS1()).isEqualTo((short) 15);
        assertThat(r.getPdSt()).isEqualTo((short) 23);

        Respuesta loaded = em.find(Respuesta.class, respuesta.getId());
        assertThat(loaded.getOpcionReactivo().isEsCorrecta()).isTrue();
        assertThat(loaded.getEjecucionSubtest().getTipoSubtest()).isEqualTo(TipoSubtest.S1A);
    }

    @Test
    void baremoLookup_knownPercentile() {
        baremoRepo.save(new Baremo(FactorEspacial.S1, (short) 30, (short) 75));
        baremoRepo.flush();
        Baremo found = baremoRepo.findById(new BaremoId(FactorEspacial.S1, (short) 30)).orElseThrow();
        assertThat(found.getPercentil()).isEqualTo((short) 75);
    }
}

