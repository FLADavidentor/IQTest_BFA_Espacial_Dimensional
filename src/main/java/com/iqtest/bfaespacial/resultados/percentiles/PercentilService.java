package com.iqtest.bfaespacial.resultados.percentiles;

import com.iqtest.bfaespacial.domain.Baremo;
import com.iqtest.bfaespacial.domain.enums.FactorEspacial;
import org.springframework.stereotype.Service;

/**
 * Converts a direct score to a percentile via the baremo (§13).
 * §19 Q1 open question — conservative default below.
 */
@Service
public class PercentilService {

    /** GAP_STRATEGY: on a missing baremo entry, use the next lower available score's percentile. */
    public static final String GAP_STRATEGY = "NEXT_LOWER";

    public record PercentilResultado(short percentil, boolean fallback) {}

    private final BaremoRepository baremoRepo;

    public PercentilService(BaremoRepository baremoRepo) {
        this.baremoRepo = baremoRepo;
    }

    public PercentilResultado calcular(FactorEspacial factor, short puntuacionDirecta) {
        Short pd = puntuacionDirecta;

        Baremo exact = baremoRepo.findByFactorAndPuntuacionDirecta(factor, pd).orElse(null);
        if (exact != null) {
            return new PercentilResultado(exact.getPercentil(), false);
        }
        // Gap: nearest lower-or-equal score (NEXT_LOWER).
        Baremo floor = baremoRepo
                .findFirstByFactorAndPuntuacionDirectaLessThanEqualOrderByPuntuacionDirectaDesc(factor, pd)
                .orElseThrow(() -> new IllegalStateException(
                        "Sin baremo para factor %s <= %d".formatted(factor, puntuacionDirecta)));
        return new PercentilResultado(floor.getPercentil(), true);
    }
}
