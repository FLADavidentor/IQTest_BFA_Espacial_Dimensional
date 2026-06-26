package com.iqtest.bfaespacial.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "version_formulario")
@Getter @Setter @NoArgsConstructor
public class VersionFormulario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Short anio;

    @Column(name = "numero_version", nullable = false)
    private Short numeroVersion;

    @Column(nullable = false)
    private boolean activa = false;
}
