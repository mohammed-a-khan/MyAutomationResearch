package com.cstestforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for CSTestForge.
 * Initializes the Spring Boot application.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "com.cstestforge",
    "com.cstestforge.testing.service"
})
public class CSTestForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CSTestForgeApplication.class, args);
    }
} 