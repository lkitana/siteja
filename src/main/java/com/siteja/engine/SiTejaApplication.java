package com.siteja.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Si-Teja (Sistem Tebak Tokoh) Java.
 * Run with: mvn spring-boot:run
 * Then test via Postman/browser at http://localhost:8080/api/teja/start
 */
@SpringBootApplication
public class SiTejaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiTejaApplication.class, args);
    }
}
