package com.iqtest.bfaespacial.evaluacion.gestion;

import com.iqtest.bfaespacial.administracion.auditoria.AuditoriaService;
import com.iqtest.bfaespacial.administracion.catalogo.VersionFormularioRepository;
import com.iqtest.bfaespacial.common.IntentoConflictException;
import com.iqtest.bfaespacial.domain.Intento;
import com.iqtest.bfaespacial.domain.VersionFormulario;
import com.iqtest.bfaespacial.domain.enums.EstadoIntento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntentoService {

    private final IntentoRepository intentoRepo;
    private final VersionFormularioRepository versionRepo;
    private final AuditoriaService auditoria;

    public IntentoService(IntentoRepository intentoRepo, VersionFormularioRepository versionRepo,
                          AuditoriaService auditoria) {
        this.intentoRepo = intentoRepo;
        this.versionRepo = versionRepo;
        this.auditoria = auditoria;
    }

    /**
     * UC1: resume an existing ACTIVO attempt, or create a new one.
     * RN-BFA-01: one attempt per cif+periodo (also enforced by the DB UNIQUE).
     */
    @Transactional
    public Intento iniciarOReanudar(String cif, String periodo) {
        return intentoRepo.findByCifAndPeriodoAcademico(cif, periodo)
                .map(existing -> {
                    if (existing.getEstado() == EstadoIntento.ACTIVO) {
                        auditoria.registrar(existing.getId(), cif, "INTENTO_REANUDADO", null);
                    }
                    return existing;
                })
                .orElseGet(() -> crear(cif, periodo));
    }

    /** Explicit create. Throws if an attempt already exists (RN-BFA-01). */
    @Transactional
    public Intento crear(String cif, String periodo) {
        if (intentoRepo.existsByCifAndPeriodoAcademico(cif, periodo)) {
            throw new IntentoConflictException(cif, periodo);
        }
        VersionFormulario version = versionRepo.findFirstByActivaTrueOrderByAnioDesc()
                .orElseThrow(() -> new IllegalStateException("No hay version_formulario activa"));

        Intento intento = new Intento();
        intento.setCif(cif);
        intento.setPeriodoAcademico(periodo);
        intento.setVersionFormulario(version);
        intento.setEstado(EstadoIntento.ACTIVO);
        intento = intentoRepo.save(intento);
        auditoria.registrar(intento.getId(), cif, "INTENTO_CREADO", "version=" + version.getId());
        return intento;
    }
}
