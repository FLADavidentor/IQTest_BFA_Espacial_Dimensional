package com.iqtest.bfaespacial.integracion;

import java.time.OffsetDateTime;

/** UC7 payload exposed to the IQTest Dashboard (§9). No item-level / es_correcta data. */
public record ResultadoIntegracionDTO(
        String cif, String periodo,
        short pdS1, short pdS2, short pdSt,
        short percS1, short percS2, short percSt,
        OffsetDateTime fechaCalculo) {
}
