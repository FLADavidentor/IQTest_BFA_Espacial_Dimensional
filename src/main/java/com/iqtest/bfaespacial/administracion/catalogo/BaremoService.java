package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.Baremo;
import com.iqtest.bfaespacial.domain.BaremoId;
import com.iqtest.bfaespacial.domain.enums.FactorEspacial;
import com.iqtest.bfaespacial.resultados.percentiles.BaremoRepository;
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
