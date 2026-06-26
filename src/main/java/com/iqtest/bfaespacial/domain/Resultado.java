package com.iqtest.bfaespacial.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

import java.time.OffsetDateTime;

@Entity
@Table(name = "resultado")
@Getter @Setter @NoArgsConstructor
public class Resultado {

    // PK is the intento id (1:1)
    @Id
    @Column(name = "intento_id")
    private Long intentoId;

    @Column(name = "pd_s1a", nullable = false)
    private Short pdS1a;

    @Column(name = "pd_s1b", nullable = false)
    private Short pdS1b;

    // GENERATED ALWAYS AS (pd_s1a + pd_s1b) STORED — read back after insert
    @Generated
    @Column(name = "pd_s1", insertable = false, updatable = false)
    private Short pdS1;

    @Column(name = "pd_s2", nullable = false)
    private Short pdS2;

    // GENERATED ALWAYS AS (pd_s1a + pd_s1b + pd_s2) STORED
    @Generated
    @Column(name = "pd_st", insertable = false, updatable = false)
    private Short pdSt;

    @Column(name = "perc_s1", nullable = false)
    private Short percS1;

    @Column(name = "perc_s2", nullable = false)
    private Short percS2;

    @Column(name = "perc_st", nullable = false)
    private Short percSt;

    @Column(name = "fecha_calculo", nullable = false)
    private OffsetDateTime fechaCalculo = OffsetDateTime.now();
}
