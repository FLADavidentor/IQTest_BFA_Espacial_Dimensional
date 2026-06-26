package com.iqtest.bfaespacial.administracion.auditoria;

import com.iqtest.bfaespacial.domain.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    List<RegistroAuditoria> findByIntentoIdOrderByFechaHoraAscIdAsc(Long intentoId);
}
