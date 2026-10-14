package com.laneoptimizer.config;

import com.laneoptimizer.optimizer.ExhaustiveStrategy;
import com.laneoptimizer.optimizer.LaneOptimizer;
import com.laneoptimizer.optimizer.LocalSearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring application configuration registering optimizer beans.
 */
@Configuration
public class AppConfig {

    @Bean
    public ExhaustiveStrategy exhaustiveStrategy() {
        return new ExhaustiveStrategy();
    }

    @Bean
    public LocalSearchStrategy localSearchStrategy() {
        return new LocalSearchStrategy();
    }

    @Bean
    public LaneOptimizer laneOptimizer(ExhaustiveStrategy exhaustiveStrategy, LocalSearchStrategy localSearchStrategy) {
        return new LaneOptimizer(exhaustiveStrategy, localSearchStrategy);
    }

    @Bean
    public org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
