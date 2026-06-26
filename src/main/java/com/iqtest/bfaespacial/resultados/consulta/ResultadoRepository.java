package com.iqtest.bfaespacial.resultados.consulta;

import com.iqtest.bfaespacial.domain.Resultado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoRepository extends JpaRepository<Resultado, Long> {
}
