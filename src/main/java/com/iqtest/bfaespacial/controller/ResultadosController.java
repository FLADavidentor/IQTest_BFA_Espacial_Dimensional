package com.iqtest.bfaespacial.controller;

import com.iqtest.bfaespacial.model.Intento;
import com.iqtest.bfaespacial.model.Resultado;
import com.iqtest.bfaespacial.model.Respuesta;
import com.iqtest.bfaespacial.repository.IntentoRepository;
import com.iqtest.bfaespacial.repository.OpcionReactivoRepository;
import com.iqtest.bfaespacial.repository.RespuestaRepository;
import com.iqtest.bfaespacial.repository.ResultadoRepository;
import com.iqtest.bfaespacial.service.ResultadoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** UC5/UC6: results + answer review + list history. ROLE_EVALUADOR (enforced by SecurityConfig). */
@Controller
public class ResultadosController {

    /** View row for answer review — correct answer is visible to the evaluator only. */
    public record RevisionItem(String imagenUrl, String tipoSubtest,
                               String respuestaEstudiante, String respuestaCorrecta, boolean correcta) {}

    private final IntentoRepository intentoRepo;
    private final ResultadoService resultadoService;
    private final RespuestaRepository respuestaRepo;
    private final OpcionReactivoRepository opcionRepo;
    private final ResultadoRepository resultadoRepo;

    public ResultadosController(IntentoRepository intentoRepo, ResultadoService resultadoService,
                                RespuestaRepository respuestaRepo, OpcionReactivoRepository opcionRepo,
                                ResultadoRepository resultadoRepo) {
        this.intentoRepo = intentoRepo;
        this.resultadoService = resultadoService;
        this.respuestaRepo = respuestaRepo;
        this.opcionRepo = opcionRepo;
        this.resultadoRepo = resultadoRepo;
    }

    @GetMapping("/resultados")
    public String listar(Model model) {
        List<Intento> intentos = intentoRepo.findAllByOrderByIdDesc();
        Map<Long, Resultado> resultadosMap = resultadoRepo.findAll().stream()
                .collect(Collectors.toMap(Resultado::getIntentoId, r -> r));
        model.addAttribute("intentos", intentos);
        model.addAttribute("resultados", resultadosMap);
        return "resultados/lista";
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

    // @Transactional: open-in-view is off, so keep the session open while the
    // view-model resolves lazy Reactivo/OpcionReactivo associations.
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
