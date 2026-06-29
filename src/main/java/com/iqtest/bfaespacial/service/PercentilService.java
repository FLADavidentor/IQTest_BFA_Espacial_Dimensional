package com.iqtest.bfaespacial.service;
import com.iqtest.bfaespacial.repository.BaremoRepository;

import com.iqtest.bfaespacial.model.Baremo;
import com.iqtest.bfaespacial.model.FactorEspacial;
import org.springframework.stereotype.Service;

/**
 * Converts a direct score to a percentile via the baremo (§13).
 * §19 Q1 RESOLVED: on a baremo gap ("-" / missing direct score), report the percentile of the
 * next lower available direct score (NEXT_LOWER). The fallback is audited as BAREMO_GAP.
 */
@Service
public class PercentilService {

    /** GAP_STRATEGY (§19 Q1): on a missing baremo entry, use the next lower available score's percentile. */
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


