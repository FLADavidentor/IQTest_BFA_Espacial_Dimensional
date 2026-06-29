package com.iqtest.bfaespacial.service;
import com.iqtest.bfaespacial.repository.OpcionReactivoRepository;
import com.iqtest.bfaespacial.repository.ReactivoRepository;
import com.iqtest.bfaespacial.model.Reactivo;

import com.iqtest.bfaespacial.model.OpcionReactivo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin management of answer options per reactivo (closes the "no option UI" gap).
 * Enforces exactly one correct option per reactivo (matches the DB partial unique index).
 */
@Service
public class OpcionReactivoService {

    private final OpcionReactivoRepository opcionRepo;
    private final ReactivoRepository reactivoRepo;

    public OpcionReactivoService(OpcionReactivoRepository opcionRepo, ReactivoRepository reactivoRepo) {
        this.opcionRepo = opcionRepo;
        this.reactivoRepo = reactivoRepo;
    }

    public List<OpcionReactivo> listar(Long reactivoId) {
        return opcionRepo.findByReactivoIdOrderByEtiqueta(reactivoId);
    }

    @Transactional
    public void agregar(Long reactivoId, String etiqueta, boolean correcta) {
        if (correcta) {
            desmarcarTodas(reactivoId);
        }
        OpcionReactivo o = new OpcionReactivo();
        o.setReactivo(reactivoRepo.getReferenceById(reactivoId));
        o.setEtiqueta(etiqueta);
        o.setEsCorrecta(correcta);
        opcionRepo.save(o);
    }

    @Transactional
    public void eliminar(Long opcionId) {
        opcionRepo.deleteById(opcionId);
    }

    /** Make one option the correct one; all siblings become incorrect (single-correct rule). */
    @Transactional
    public void marcarCorrecta(Long opcionId) {
        OpcionReactivo target = opcionRepo.findById(opcionId)
                .orElseThrow(() -> new IllegalArgumentException("Opción no existe: " + opcionId));
        desmarcarTodas(target.getReactivo().getId());
        target.setEsCorrecta(true);
        opcionRepo.save(target);
    }

    private void desmarcarTodas(Long reactivoId) {
        for (OpcionReactivo o : opcionRepo.findByReactivoIdOrderByEtiqueta(reactivoId)) {
            o.setEsCorrecta(false);
        }
        opcionRepo.flush(); // clear existing correct before setting a new one (partial unique index)
    }
}


