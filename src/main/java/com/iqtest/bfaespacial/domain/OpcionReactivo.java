package com.iqtest.bfaespacial.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "opcion_reactivo")
@Getter @Setter @NoArgsConstructor
public class OpcionReactivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reactivo_id", nullable = false)
    private Reactivo reactivo;

    @Column(nullable = false, length = 4)
    private String etiqueta;

    @Column(name = "es_correcta", nullable = false)
    private boolean esCorrecta = false;
}
