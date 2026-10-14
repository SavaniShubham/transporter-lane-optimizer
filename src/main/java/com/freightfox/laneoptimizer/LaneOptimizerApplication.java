package com.freightfox.laneoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot entry point for the FreightFox Lane Optimizer service.
 */
@SpringBootApplication
public class LaneOptimizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaneOptimizerApplication.class, args);
    }
}
