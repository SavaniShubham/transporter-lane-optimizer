package com.laneoptimizer.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes PostgreSQL connection URIs (e.g. postgres:// or postgresql://)
 * provided by cloud platforms such as Render, Railway, or Fly.io into JDBC compliant format.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equalsIgnoreCase(profile)) {
                return;
            }
        }

        String dbUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = environment.getProperty("DATABASE_URL");
        }

        if (dbUrl != null && (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://"))) {
            try {
                if (!dbUrl.startsWith("jdbc:")) {
                    URI uri = new URI(dbUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;

                    Map<String, Object> props = new HashMap<>();
                    props.put("spring.datasource.url", jdbcUrl);
                    props.put("SPRING_DATASOURCE_URL", jdbcUrl);

                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && !userInfo.isBlank()) {
                        String[] parts = userInfo.split(":", 2);
                        props.put("spring.datasource.username", parts[0]);
                        props.put("SPRING_DATASOURCE_USERNAME", parts[0]);
                        if (parts.length > 1) {
                            props.put("spring.datasource.password", parts[1]);
                            props.put("SPRING_DATASOURCE_PASSWORD", parts[1]);
                        }
                    }

                    environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlProps", props));
                }
            } catch (Exception ignored) {
                // Fall back to standard Spring Boot datasource property resolution
            }
        }
    }
}
