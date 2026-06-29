package com.iqtest.bfaespacial.service;

import com.iqtest.bfaespacial.model.Baremo;
import com.iqtest.bfaespacial.model.BaremoId;
import com.iqtest.bfaespacial.model.FactorEspacial;
import com.iqtest.bfaespacial.repository.BaremoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BaremoService {

    private final BaremoRepository repo;

    public BaremoService(BaremoRepository repo) {
        this.repo = repo;
    }

    public List<Baremo> listar() {
        return repo.findAll();
    }

    /** Edit a single percentile cell for (factor, puntuacionDirecta). */
    @Transactional
    public void actualizarPercentil(FactorEspacial factor, Short puntuacionDirecta, Short percentil) {
        Baremo b = repo.findById(new BaremoId(factor, puntuacionDirecta))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe baremo %s/%d".formatted(factor, puntuacionDirecta)));
        b.setPercentil(percentil);
        repo.save(b);
    }
}


