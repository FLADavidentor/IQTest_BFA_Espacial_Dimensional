package com.iqtest.bfaespacial.administracion.auditoria;

import com.iqtest.bfaespacial.domain.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
}
