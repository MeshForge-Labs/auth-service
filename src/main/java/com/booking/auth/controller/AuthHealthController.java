package com.booking.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight health endpoints (separate from Actuator) for probes and local checks.
 */
@RestController
public class AuthHealthController {

    private final DataSource dataSource;

    public AuthHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        body.put("service", "auth-service");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            Map<String, Object> body = new HashMap<>();
            body.put("status", valid ? "UP" : "DOWN");
            body.put("service", "auth-service");
            body.put("timestamp", Instant.now().toString());
            body.put("database", valid ? "reachable" : "unreachable");

            return valid ? ResponseEntity.ok(body) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        } catch (Exception e) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "DOWN");
            body.put("service", "auth-service");
            body.put("timestamp", Instant.now().toString());
            body.put("database", "unreachable");
            body.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}

