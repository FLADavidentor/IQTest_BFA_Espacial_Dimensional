package com.iqtest.bfaespacial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Prints the demo credentials block once the app is ready (demo profile only). */
@Component
@Profile("demo")
public class DemoBanner {

    private static final Logger log = LoggerFactory.getLogger(DemoBanner.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("""

                ===========================================
                  BFA ESPACIAL — DEMO READY
                  URL:        http://localhost:8080
                  Admin:      admin / x
                  Evaluador:  evaluador / x
                  Estudiante: estudiante / x
                ===========================================
                """);
    }
}

