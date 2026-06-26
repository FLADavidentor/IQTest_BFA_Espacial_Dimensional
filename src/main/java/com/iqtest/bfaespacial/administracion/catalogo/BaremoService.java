package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.Baremo;
import com.iqtest.bfaespacial.resultados.percentiles.BaremoRepository;
import org.springframework.stereotype.Service;

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
}
