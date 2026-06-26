package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.VersionFormulario;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VersionFormularioService {

    private final VersionFormularioRepository repo;

    public VersionFormularioService(VersionFormularioRepository repo) {
        this.repo = repo;
    }

    public List<VersionFormulario> listar() {
        return repo.findAll();
    }
}
