package com.booking.auth.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Contributes to readiness: pod is ready only when DB is reachable.
 */
@Component
public class DatabaseReadinessIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseReadinessIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }
/**
 * Checks if the database is reachable.
 * @return Health object with the database status.
 */
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return Health.up().withDetail("database", "reachable").build();
            }
        } catch (Exception e) {
            return Health.down().withDetail("database", "unreachable").withException(e).build();
        }
        return Health.down().withDetail("database", "invalid").build();
    }
}
