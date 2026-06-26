package com.iqtest.bfaespacial.domain;

import com.iqtest.bfaespacial.domain.enums.FactorEspacial;

import java.io.Serializable;
import java.util.Objects;

public class BaremoId implements Serializable {
    private FactorEspacial factor;
    private Short puntuacionDirecta;

    public BaremoId() {}

    public BaremoId(FactorEspacial factor, Short puntuacionDirecta) {
        this.factor = factor;
        this.puntuacionDirecta = puntuacionDirecta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaremoId that)) return false;
        return factor == that.factor && Objects.equals(puntuacionDirecta, that.puntuacionDirecta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(factor, puntuacionDirecta);
    }
}
