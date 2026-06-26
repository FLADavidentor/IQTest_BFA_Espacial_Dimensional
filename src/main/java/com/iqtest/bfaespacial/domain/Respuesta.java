package com.iqtest.bfaespacial.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "respuesta")
@Getter @Setter @NoArgsConstructor
public class Respuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecucion_subtest_id", nullable = false)
    private EjecucionSubtest ejecucionSubtest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reactivo_id", nullable = false)
    private Reactivo reactivo;

    // NULL if unanswered
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_reactivo_id")
    private OpcionReactivo opcionReactivo;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean sincronizada = true;
}
