package com.iqtest.bfaespacial.administracion.catalogo;

import com.iqtest.bfaespacial.domain.VersionFormulario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VersionFormularioService {

    /** Raised when activating a version whose year already has an active one, without confirmation. */
    public static class ActivacionRequiereConfirmacion extends RuntimeException {
        public ActivacionRequiereConfirmacion(Short anio) {
            super("El año " + anio + " ya tiene una versión activa. Confirme para desactivarla.");
        }
    }

    private final VersionFormularioRepository repo;

    public VersionFormularioService(VersionFormularioRepository repo) {
        this.repo = repo;
    }

    public List<VersionFormulario> listar() {
        return repo.findAll();
    }

    @Transactional
    public VersionFormulario crear(Short anio, Short numeroVersion, boolean activa) {
        VersionFormulario v = new VersionFormulario();
        v.setAnio(anio);
        v.setNumeroVersion(numeroVersion);
        v.setActiva(false);
        v = repo.save(v);
        if (activa) {
            activar(v.getId(), true);
        }
        return v;
    }

    /**
     * Activate a version. Single active per year is enforced at the service level:
     * all other versions of the same year are deactivated. If another active version
     * exists and confirmar=false, refuse (user must confirm the deactivation).
     */
    @Transactional
    public void activar(Long id, boolean confirmar) {
        VersionFormulario target = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Versión no existe: " + id));
        List<VersionFormulario> delAnio = repo.findByAnio(target.getAnio());
        boolean otraActiva = delAnio.stream().anyMatch(v -> v.isActiva() && !v.getId().equals(id));
        if (otraActiva && !confirmar) {
            throw new ActivacionRequiereConfirmacion(target.getAnio());
        }
        for (VersionFormulario v : delAnio) {
            v.setActiva(v.getId().equals(id));
        }
    }
}
