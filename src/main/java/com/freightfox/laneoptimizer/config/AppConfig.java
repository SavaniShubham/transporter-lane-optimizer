package com.freightfox.laneoptimizer.config;

import com.freightfox.laneoptimizer.optimizer.ExhaustiveStrategy;
import com.freightfox.laneoptimizer.optimizer.LaneOptimizer;
import com.freightfox.laneoptimizer.optimizer.LocalSearchStrategy;
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
}
