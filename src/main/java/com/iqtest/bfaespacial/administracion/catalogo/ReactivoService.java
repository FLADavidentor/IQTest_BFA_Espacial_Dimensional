package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.Reactivo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReactivoService {

    private final ReactivoRepository repo;

    public ReactivoService(ReactivoRepository repo) {
        this.repo = repo;
    }

    public List<Reactivo> listar() {
        return repo.findAll();
    }
}
