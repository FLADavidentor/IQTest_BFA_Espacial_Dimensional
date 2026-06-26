package com.iqtest.bfaespacial;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Singleton PG16 container shared by the whole IT suite (started once per JVM). */
@SpringBootTest
public abstract class AbstractPostgresIT {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");

    static {
        PG.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }
}
