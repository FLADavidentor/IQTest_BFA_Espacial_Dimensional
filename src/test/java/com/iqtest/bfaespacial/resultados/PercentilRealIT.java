package com.iqtest.bfaespacial.resultados;

import com.iqtest.bfaespacial.AbstractPostgresIT;
import com.iqtest.bfaespacial.domain.enums.FactorEspacial;
import com.iqtest.bfaespacial.resultados.percentiles.PercentilService;
import com.iqtest.bfaespacial.resultados.percentiles.PercentilService.PercentilResultado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Track 1: PercentilService against the REAL Normas data (V6), incl. the real ST gap at 50. */
@Transactional
class PercentilRealIT extends AbstractPostgresIT {

    @Autowired PercentilService percentilService;

    @Test
    void valoresRealesYGapReal() {
        // exact entries
        assertThat(percentilService.calcular(FactorEspacial.S1, (short) 18))
                .isEqualTo(new PercentilResultado((short) 70, false));
        assertThat(percentilService.calcular(FactorEspacial.S2, (short) 15))
                .isEqualTo(new PercentilResultado((short) 65, false));
        assertThat(percentilService.calcular(FactorEspacial.ST, (short) 33))
                .isEqualTo(new PercentilResultado((short) 65, false));

        // real gap: ST has no row for 50 (44-49 -> 90, 51-54 -> 95) => NEXT_LOWER to 49 = 90
        assertThat(percentilService.calcular(FactorEspacial.ST, (short) 50))
                .isEqualTo(new PercentilResultado((short) 90, true));
    }
}
