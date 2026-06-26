package com.iqtest.bfaespacial.web.estudiante;

import com.iqtest.bfaespacial.domain.Intento;
import com.iqtest.bfaespacial.evaluacion.aplicacion.SubtestService;
import com.iqtest.bfaespacial.evaluacion.aplicacion.SubtestService.VistaActual;
import com.iqtest.bfaespacial.evaluacion.gestion.IntentoRepository;
import com.iqtest.bfaespacial.integracion.SesionIQTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** UC2 shell: Thymeleaf page that mounts the React SPA (§11). */
@Controller
public class SubtestController {

    private final IntentoRepository intentoRepo;
    private final SubtestService subtestService;
    private final SesionIQTestClient sesion;

    public SubtestController(IntentoRepository intentoRepo, SubtestService subtestService, SesionIQTestClient sesion) {
        this.intentoRepo = intentoRepo;
        this.subtestService = subtestService;
        this.sesion = sesion;
    }

    @GetMapping("/evaluacion/subtest")
    public String subtest(Model model) {
        Intento intento = intentoRepo.findByCifAndPeriodoAcademico(sesion.cifActual(), sesion.periodoActual())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
        VistaActual va = subtestService.vistaActual(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin subtest en curso"));

        // Data passed to React (§11) — no answer-key data.
        model.addAttribute("intentoId", intento.getId());
        model.addAttribute("cif", sesion.cifActual());
        model.addAttribute("subtestType", va.ejecucion().getTipoSubtest().name());
        model.addAttribute("tiempoRestanteSeg", va.tiempoRestanteSeg());
        return "evaluacion/subtest";
    }

    @GetMapping("/evaluacion/completado")
    public String completado() {
        return "evaluacion/completado";
    }
}
