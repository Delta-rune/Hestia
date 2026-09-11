package com.hestia.vault.controller;

import com.hestia.vault.service.HestiaKeepAliveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class KeepAliveController {

    @Autowired
    private HestiaKeepAliveService keepAliveService;

    /**
     * GET /api/keep-alive
     * Fast-path endpoint that resets Render's 15-minute inactivity timer.
     * Returns real-time anti-spin-down telemetry.
     */
    @GetMapping("/keep-alive")
    public ResponseEntity<Map<String, Object>> pingKeepAlive() {
        return ResponseEntity.ok(keepAliveService.getTelemetryStatus());
    }

    /**
     * GET /api/ping
     * Ultra-lightweight ping alias for load balancers and uptime monitors.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> quickPing() {
        return ResponseEntity.ok(Map.of("status", "PONG", "service", "hestia-vault"));
    }
}
