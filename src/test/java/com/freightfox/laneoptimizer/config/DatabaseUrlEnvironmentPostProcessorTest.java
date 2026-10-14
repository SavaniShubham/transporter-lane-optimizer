package com.freightfox.laneoptimizer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DatabaseUrlEnvironmentPostProcessor Unit Tests")
class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    @DisplayName("Should convert postgres:// URL from cloud provider to standard JDBC URL and credentials")
    void shouldConvertCloudPostgresUrl() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        System.setProperty("SPRING_DATASOURCE_URL", "postgres://renderuser:secretpass@dpg-host.render.com:5432/freightfox_db");

        try {
            DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
            processor.postProcessEnvironment(environment, new SpringApplication());

            assertEquals("jdbc:postgresql://dpg-host.render.com:5432/freightfox_db", environment.getProperty("spring.datasource.url"));
            assertEquals("renderuser", environment.getProperty("spring.datasource.username"));
            assertEquals("secretpass", environment.getProperty("spring.datasource.password"));
        } finally {
            System.clearProperty("SPRING_DATASOURCE_URL");
        }
    }

    @Test
    @DisplayName("Should ignore standard jdbc:postgresql URL")
    void shouldIgnoreStandardJdbcUrl() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        System.setProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/freightfox");

        try {
            DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
            processor.postProcessEnvironment(environment, new SpringApplication());

            // Should not override with cloud properties
            assertEquals("jdbc:postgresql://localhost:5432/freightfox", environment.getProperty("SPRING_DATASOURCE_URL"));
        } finally {
            System.clearProperty("SPRING_DATASOURCE_URL");
        }
    }
}
