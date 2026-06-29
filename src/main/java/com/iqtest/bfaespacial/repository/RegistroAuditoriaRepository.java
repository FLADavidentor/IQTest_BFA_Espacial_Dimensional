package com.iqtest.bfaespacial.repository;

import com.iqtest.bfaespacial.model.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    List<RegistroAuditoria> findByIntentoIdOrderByFechaHoraAscIdAsc(Long intentoId);
}


