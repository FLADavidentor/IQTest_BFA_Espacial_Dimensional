package com.iqtest.bfaespacial.model;

import com.iqtest.bfaespacial.model.EstadoIntento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "intento")
@Getter @Setter @NoArgsConstructor
public class Intento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String cif;

    @Column(name = "periodo_academico", nullable = false, length = 20)
    private String periodoAcademico;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "estado_intento", nullable = false)
    private EstadoIntento estado = EstadoIntento.ACTIVO;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio = OffsetDateTime.now();

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_formulario_id", nullable = false)
    private VersionFormulario versionFormulario;
}


