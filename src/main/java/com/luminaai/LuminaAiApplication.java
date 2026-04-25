package com.luminaai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LuminaAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LuminaAiApplication.class, args);
    }
}