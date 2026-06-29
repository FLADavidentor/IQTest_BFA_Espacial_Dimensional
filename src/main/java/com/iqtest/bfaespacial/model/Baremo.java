package com.iqtest.bfaespacial.model;

import com.iqtest.bfaespacial.model.FactorEspacial;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "baremo")
@IdClass(BaremoId.class)
@Getter @Setter @NoArgsConstructor
public class Baremo {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "factor_espacial", nullable = false)
    private FactorEspacial factor;

    @Id
    @Column(name = "puntuacion_directa", nullable = false)
    private Short puntuacionDirecta;

    @Column(nullable = false)
    private Short percentil;

    public Baremo(FactorEspacial factor, Short puntuacionDirecta, Short percentil) {
        this.factor = factor;
        this.puntuacionDirecta = puntuacionDirecta;
        this.percentil = percentil;
    }
}


