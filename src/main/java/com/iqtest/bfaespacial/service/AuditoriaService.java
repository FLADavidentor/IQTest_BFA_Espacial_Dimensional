package com.iqtest.bfaespacial.service;
import com.iqtest.bfaespacial.model.Intento;
import com.iqtest.bfaespacial.repository.RegistroAuditoriaRepository;

import com.iqtest.bfaespacial.model.RegistroAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** §14 audit: persists relevant intento events to registro_auditoria. */
@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository repo;

    public AuditoriaService(RegistroAuditoriaRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void registrar(Long intentoId, String cifActor, String accion, String detalle) {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setIntentoId(intentoId);
        r.setCifActor(cifActor);
        r.setAccion(accion);
        r.setDetalle(detalle);
        repo.save(r);
    }
}


