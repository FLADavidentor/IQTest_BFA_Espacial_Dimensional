package com.iqtest.bfaespacial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BfaEspacialApplication {
    public static void main(String[] args) {
        SpringApplication.run(BfaEspacialApplication.class, args);
    }
}
