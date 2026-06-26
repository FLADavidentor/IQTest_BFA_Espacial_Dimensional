package com.iqtest.bfaespacial.integracion;

import com.iqtest.bfaespacial.domain.Intento;
import com.iqtest.bfaespacial.domain.Resultado;
import com.iqtest.bfaespacial.evaluacion.gestion.IntentoRepository;
import com.iqtest.bfaespacial.resultados.consulta.ResultadoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/** UC7: service-to-service results endpoint consumed by the IQTest Dashboard (§9). */
@RestController
@RequestMapping("/api/integracion")
public class ResultadosDashboardController {

    private final IntentoRepository intentoRepo;
    private final ResultadoRepository resultadoRepo;
    private final String apiKey;
    private final boolean snake; // §19 Q4 — key format configurable, default snake_case

    public ResultadosDashboardController(IntentoRepository intentoRepo, ResultadoRepository resultadoRepo,
                                         @Value("${app.integracion.api-key}") String apiKey,
                                         @Value("${app.integracion.json-format:SNAKE}") String jsonFormat) {
        this.intentoRepo = intentoRepo;
        this.resultadoRepo = resultadoRepo;
        this.apiKey = apiKey;
        this.snake = !"CAMEL".equalsIgnoreCase(jsonFormat);
    }

    @GetMapping("/resultados/{cif}/{periodo}")
    public Map<String, Object> resultados(@PathVariable String cif, @PathVariable String periodo,
                                          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        // §19 Q4: API key as Authorization: Bearer <key>, validated against app.integracion.api-key.
        if (auth == null || !auth.equals("Bearer " + apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key inválida");
        }
        Intento intento = intentoRepo.findByCifAndPeriodoAcademico(cif, periodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
        Resultado r = resultadoRepo.findById(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado no disponible"));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cif", cif);
        m.put("periodo", periodo);
        m.put(snake ? "pd_s1" : "pdS1", r.getPdS1());
        m.put(snake ? "pd_s2" : "pdS2", r.getPdS2());
        m.put(snake ? "pd_st" : "pdSt", r.getPdSt());
        m.put(snake ? "perc_s1" : "percS1", r.getPercS1());
        m.put(snake ? "perc_s2" : "percS2", r.getPercS2());
        m.put(snake ? "perc_st" : "percSt", r.getPercSt());
        m.put(snake ? "fecha_calculo" : "fechaCalculo", r.getFechaCalculo());
        return m;
    }
}
