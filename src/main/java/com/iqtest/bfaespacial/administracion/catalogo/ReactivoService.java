package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.Reactivo;
import com.iqtest.bfaespacial.web.admin.ReactivoForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReactivoService {

    private final ReactivoRepository repo;
    private final VersionFormularioRepository versionRepo;

    public ReactivoService(ReactivoRepository repo, VersionFormularioRepository versionRepo) {
        this.repo = repo;
        this.versionRepo = versionRepo;
    }

    public List<Reactivo> listar() {
        return repo.findAllByOrderByIdAsc();
    }

    public Reactivo obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Reactivo no existe: " + id));
    }

    @Transactional
    public Reactivo guardar(ReactivoForm form) {
        Reactivo r = form.getId() == null ? new Reactivo() : obtener(form.getId());
        r.setVersionFormulario(versionRepo.getReferenceById(form.getVersionFormularioId()));
        r.setTipoSubtest(form.getTipoSubtest());
        r.setOrden(form.getOrden());
        r.setEnunciadoImagenUrl(form.getEnunciadoImagenUrl());
        r.setEnunciadoTexto(form.getEnunciadoTexto());
        return repo.save(r);
    }

    /** Soft delete (P0-B) — keeps referential integrity for existing answers. */
    @Transactional
    public void desactivar(Long id) {
        Reactivo r = obtener(id);
        r.setActivo(false);
        repo.save(r);
    }
}
