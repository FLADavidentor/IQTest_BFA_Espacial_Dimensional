package com.iqtest.bfaespacial.domain;

import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "configuracion_subtest")
@Getter @Setter @NoArgsConstructor
public class ConfiguracionSubtest {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_subtest", columnDefinition = "tipo_subtest")
    private TipoSubtest tipoSubtest;

    @Column(name = "tiempo_limite_seg", nullable = false)
    private Integer tiempoLimiteSeg;

    @Column(name = "cantidad_items", nullable = false)
    private Short cantidadItems;

    @Column(name = "tipo_seleccion", nullable = false, length = 20)
    private String tipoSeleccion;
}
