package com.iqtest.bfaespacial.common;

/** RN-BFA-05: cannot modify answers of a closed subtest. Maps to HTTP 409/423. */
public class SubtestCerradoException extends RuntimeException {
    public SubtestCerradoException(Long ejecucionId) {
        super("La ejecución de subtest %d está cerrada".formatted(ejecucionId));
    }
}
