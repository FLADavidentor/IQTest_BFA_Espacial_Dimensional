package com.iqtest.bfaespacial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registro_auditoria")
@Getter @Setter @NoArgsConstructor
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intento_id", nullable = false)
    private Long intentoId;

    @Column(name = "cif_actor", nullable = false, length = 20)
    private String cifActor;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora = OffsetDateTime.now();

    @Column(columnDefinition = "text")
    private String detalle;
}

