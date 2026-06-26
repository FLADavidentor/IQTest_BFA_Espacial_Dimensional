package com.iqtest.bfaespacial.web.evaluador;

import com.iqtest.bfaespacial.administracion.catalogo.OpcionReactivoRepository;
import com.iqtest.bfaespacial.domain.Intento;
import com.iqtest.bfaespacial.domain.Respuesta;
import com.iqtest.bfaespacial.evaluacion.aplicacion.RespuestaRepository;
import com.iqtest.bfaespacial.evaluacion.gestion.IntentoRepository;
import com.iqtest.bfaespacial.resultados.consulta.ResultadoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** UC5/UC6: results + answer review. ROLE_EVALUADOR (enforced by SecurityConfig). */
@Controller
public class ResultadosController {

    /** View row for answer review — correct answer is visible to the evaluator only. */
    public record RevisionItem(String imagenUrl, String tipoSubtest,
                               String respuestaEstudiante, String respuestaCorrecta, boolean correcta) {}

    private final IntentoRepository intentoRepo;
    private final ResultadoService resultadoService;
    private final RespuestaRepository respuestaRepo;
    private final OpcionReactivoRepository opcionRepo;

    public ResultadosController(IntentoRepository intentoRepo, ResultadoService resultadoService,
                                RespuestaRepository respuestaRepo, OpcionReactivoRepository opcionRepo) {
        this.intentoRepo = intentoRepo;
        this.resultadoService = resultadoService;
        this.respuestaRepo = respuestaRepo;
        this.opcionRepo = opcionRepo;
    }

    @GetMapping("/resultados/{cif}/{periodo}")
    public String ver(@PathVariable String cif, @PathVariable String periodo, Model model) {
        Intento intento = intento(cif, periodo);
        model.addAttribute("cif", cif);
        model.addAttribute("periodo", periodo);
        model.addAttribute("resultado", resultadoService.obtener(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado no disponible")));
        return "resultados/ver";
    }

    @GetMapping("/resultados/{cif}/{periodo}/respuestas")
    public String respuestas(@PathVariable String cif, @PathVariable String periodo, Model model) {
        Intento intento = intento(cif, periodo);
        List<RevisionItem> items = respuestaRepo.findByEjecucionSubtestIntentoId(intento.getId()).stream()
                .map(this::toRevision)
                .toList();
        model.addAttribute("cif", cif);
        model.addAttribute("periodo", periodo);
        model.addAttribute("items", items);
        return "resultados/respuestas";
    }

    private RevisionItem toRevision(Respuesta r) {
        String estudiante = r.getOpcionReactivo() == null ? "—" : r.getOpcionReactivo().getEtiqueta();
        String correcta = opcionRepo.findByReactivoIdAndEsCorrectaTrue(r.getReactivo().getId())
                .map(o -> o.getEtiqueta()).orElse("?");
        boolean ok = r.getOpcionReactivo() != null && r.getOpcionReactivo().isEsCorrecta();
        return new RevisionItem(r.getReactivo().getEnunciadoImagenUrl(),
                r.getEjecucionSubtest().getTipoSubtest().name(), estudiante, correcta, ok);
    }

    private Intento intento(String cif, String periodo) {
        return intentoRepo.findByCifAndPeriodoAcademico(cif, periodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
    }
}
