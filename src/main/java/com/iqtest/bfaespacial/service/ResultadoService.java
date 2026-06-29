package com.iqtest.bfaespacial.service;
import com.iqtest.bfaespacial.repository.ResultadoRepository;

import com.iqtest.bfaespacial.model.Resultado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** UC5: read a computed result. Role-gated to EVALUADOR at the web layer (Phase 4). */
@Service
public class ResultadoService {

    private final ResultadoRepository resultadoRepo;

    public ResultadoService(ResultadoRepository resultadoRepo) {
        this.resultadoRepo = resultadoRepo;
    }

    @Transactional(readOnly = true)
    public Optional<Resultado> obtener(Long intentoId) {
        return resultadoRepo.findById(intentoId);
    }
}


