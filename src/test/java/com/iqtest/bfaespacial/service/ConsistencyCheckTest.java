package com.iqtest.bfaespacial.service;

import com.iqtest.bfaespacial.model.*;
import com.iqtest.bfaespacial.repository.RespuestaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ConsistencyCheckTest {

    private RespuestaRepository respuestaRepo;
    private CalificacionService service;

    @BeforeEach
    public void setUp() {
        respuestaRepo = Mockito.mock(RespuestaRepository.class);
        service = new CalificacionService(respuestaRepo, null, null, null, null, null);
    }

    @Test
    public void testAltaRepeticionAlert() {
        Long intentoId = 1L;
        List<Respuesta> respuestas = new ArrayList<>();
        
        Intento intento = new Intento();
        intento.setId(intentoId);
        EjecucionSubtest ejecucion = new EjecucionSubtest();
        ejecucion.setIntento(intento);
        ejecucion.setTipoSubtest(TipoSubtest.S1A);
        
        OpcionReactivo opcionA = new OpcionReactivo();
        opcionA.setEtiqueta("A");
        
        for (int i = 1; i <= 10; i++) {
            Respuesta r = new Respuesta();
            r.setEjecucionSubtest(ejecucion);
            r.setOpcionReactivo(opcionA);
            respuestas.add(r);
        }
        
        when(respuestaRepo.findByEjecucionSubtestIntentoId(intentoId)).thenReturn(respuestas);
        
        String alert = service.analizarConsistencia(intentoId);
        assertThat(alert).contains("Alta repetición: S1A");
    }

    @Test
    public void testPatronConsecutivoAlert() {
        Long intentoId = 1L;
        List<Respuesta> respuestas = new ArrayList<>();
        
        Intento intento = new Intento();
        intento.setId(intentoId);
        EjecucionSubtest ejecucion = new EjecucionSubtest();
        ejecucion.setIntento(intento);
        ejecucion.setTipoSubtest(TipoSubtest.S1A);
        
        OpcionReactivo opcionA = new OpcionReactivo();
        opcionA.setEtiqueta("A");
        OpcionReactivo opcionB = new OpcionReactivo();
        opcionB.setEtiqueta("B");

        for (int i = 1; i <= 15; i++) {
            Reactivo reactivo = new Reactivo();
            reactivo.setOrden((short) i);
            
            Respuesta r = new Respuesta();
            r.setEjecucionSubtest(ejecucion);
            r.setReactivo(reactivo);
            if (i <= 12) {
                r.setOpcionReactivo(opcionA);
            } else {
                r.setOpcionReactivo(opcionB);
            }
            respuestas.add(r);
        }
        
        when(respuestaRepo.findByEjecucionSubtestIntentoId(intentoId)).thenReturn(respuestas);
        
        String alert = service.analizarConsistencia(intentoId);
        assertThat(alert).contains("Patrón consecutivo: S1A");
    }

    @Test
    public void testNormalNoAlert() {
        Long intentoId = 1L;
        List<Respuesta> respuestas = new ArrayList<>();
        
        Intento intento = new Intento();
        intento.setId(intentoId);
        EjecucionSubtest ejecucion = new EjecucionSubtest();
        ejecucion.setIntento(intento);
        ejecucion.setTipoSubtest(TipoSubtest.S1A);
        
        OpcionReactivo opcionA = new OpcionReactivo();
        opcionA.setEtiqueta("A");
        OpcionReactivo opcionB = new OpcionReactivo();
        opcionB.setEtiqueta("B");

        for (int i = 1; i <= 15; i++) {
            Reactivo reactivo = new Reactivo();
            reactivo.setOrden((short) i);
            
            Respuesta r = new Respuesta();
            r.setEjecucionSubtest(ejecucion);
            r.setReactivo(reactivo);
            r.setOpcionReactivo(i % 2 == 0 ? opcionA : opcionB);
            respuestas.add(r);
        }
        
        when(respuestaRepo.findByEjecucionSubtestIntentoId(intentoId)).thenReturn(respuestas);
        
        String alert = service.analizarConsistencia(intentoId);
        assertThat(alert).isNull();
    }
}
