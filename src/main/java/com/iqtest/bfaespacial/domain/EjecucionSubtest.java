package com.iqtest.bfaespacial.domain;

import com.iqtest.bfaespacial.domain.enums.EstadoSubtest;
import com.iqtest.bfaespacial.domain.enums.TipoSubtest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ejecucion_subtest")
@Getter @Setter @NoArgsConstructor
public class EjecucionSubtest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intento_id", nullable = false)
    private Intento intento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_subtest", columnDefinition = "tipo_subtest", nullable = false)
    private TipoSubtest tipoSubtest;

    // NULL until the student starts the subtest (P1-A); set by comenzarSubtest.
    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "estado_subtest", nullable = false)
    private EstadoSubtest estado = EstadoSubtest.PENDIENTE;

    @Column(name = "cerrada_por_tiempo", nullable = false)
    private boolean cerradaPorTiempo = false;
}
