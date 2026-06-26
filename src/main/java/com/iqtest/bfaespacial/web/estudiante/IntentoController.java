package com.iqtest.bfaespacial.web.estudiante;

import com.iqtest.bfaespacial.domain.Intento;
import com.iqtest.bfaespacial.domain.enums.EstadoIntento;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import com.iqtest.bfaespacial.evaluacion.aplicacion.SubtestService;
import com.iqtest.bfaespacial.evaluacion.gestion.IntentoService;
import com.iqtest.bfaespacial.integracion.SesionIQTestClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** UC1: start or resume an attempt. */
@Controller
public class IntentoController {

    private final IntentoService intentoService;
    private final SubtestService subtestService;
    private final SesionIQTestClient sesion;

    public IntentoController(IntentoService intentoService, SubtestService subtestService, SesionIQTestClient sesion) {
        this.intentoService = intentoService;
        this.subtestService = subtestService;
        this.sesion = sesion;
    }

    @GetMapping("/evaluacion/inicio")
    public String inicio(Model model) {
        Intento intento = intentoService.iniciarOReanudar(sesion.cifActual(), sesion.periodoActual());
        if (intento.getEstado() == EstadoIntento.ACTIVO
                && subtestService.vistaActual(intento.getId()).isEmpty()) {
            // Prepare the first subtest as PENDIENTE; timer starts only on "Comenzar" (P1-A).
            subtestService.prepararSubtest(intento.getId(), TipoSubtest.S1A);
        }
        model.addAttribute("intento", intento);
        model.addAttribute("completado", intento.getEstado() == EstadoIntento.COMPLETADO);
        return "evaluacion/inicio";
    }
}
