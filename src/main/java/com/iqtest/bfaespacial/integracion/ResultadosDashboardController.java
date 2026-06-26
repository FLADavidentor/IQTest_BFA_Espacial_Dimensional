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

/** UC7: service-to-service results endpoint consumed by the IQTest Dashboard (§9). */
@RestController
@RequestMapping("/api/integracion")
public class ResultadosDashboardController {

    private final IntentoRepository intentoRepo;
    private final ResultadoRepository resultadoRepo;
    private final String serviceToken;

    public ResultadosDashboardController(IntentoRepository intentoRepo, ResultadoRepository resultadoRepo,
                                         @Value("${app.integracion.token}") String serviceToken) {
        this.intentoRepo = intentoRepo;
        this.resultadoRepo = resultadoRepo;
        this.serviceToken = serviceToken;
    }

    @GetMapping("/resultados/{cif}/{periodo}")
    public ResultadoIntegracionDTO resultados(@PathVariable String cif, @PathVariable String periodo,
                                              @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        // STUB — Phase 6: real Dashboard token mechanism (§19 Q4). For now a static Bearer.
        if (auth == null || !auth.equals("Bearer " + serviceToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        Intento intento = intentoRepo.findByCifAndPeriodoAcademico(cif, periodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
        Resultado r = resultadoRepo.findById(intento.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado no disponible"));

        return new ResultadoIntegracionDTO(cif, periodo,
                r.getPdS1(), r.getPdS2(), r.getPdSt(),
                r.getPercS1(), r.getPercS2(), r.getPercSt(), r.getFechaCalculo());
    }
}
