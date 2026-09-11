package com.hestia.vault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Autonomous Keep-Alive & Anti-Spin-Down Pulse Generator.
 * Render free-tier instances sleep after 15 minutes of HTTP inactivity.
 * This service emits an outbound public HTTP ping every 10 minutes through Render's
 * external edge router (https://hestia-kw5k.onrender.com), refreshing Render's
 * activity lease and keeping Hestia instantly responsive 24/7.
 */
@Service
public class HestiaKeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(HestiaKeepAliveService.class);

    private final Instant startTime = Instant.now();
    private Instant lastPingTime = null;
    private int totalPingsSent = 0;
    private int successfulPings = 0;
    private String lastStatusMessage = "Initialized. Awaiting first 10-minute cycle.";

    @Value("${hestia.public.url:https://hestia-kw5k.onrender.com}")
    private String configuredPublicUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Executes every 10 minutes (600,000 ms) with a 2-minute initial warmup delay.
     */
    @Scheduled(fixedRate = 600000, initialDelay = 120000)
    public void executeKeepAliveSequence() {
        totalPingsSent++;
        String targetBaseUrl = resolveEffectivePublicUrl();
        String pingEndpoint = targetBaseUrl.replaceAll("/+$", "") + "/api/keep-alive";

        log.info("[HESTIA-HEARTBEAT] Emitting anti-spin-down ping #{}: {}", totalPingsSent, pingEndpoint);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pingEndpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Hestia-Heartbeat-Daemon/2.0 (Render Anti-SpinDown)")
                    .header("X-Keep-Alive-Pulse", "true")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            lastPingTime = Instant.now();

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                successfulPings++;
                lastStatusMessage = "Ping #" + totalPingsSent + " successful (HTTP " + response.statusCode() + ") at " + lastPingTime;
                log.info("[HESTIA-HEARTBEAT] Success! Render edge inactivity timer reset. Response: {}", response.statusCode());
            } else {
                lastStatusMessage = "Ping #" + totalPingsSent + " returned status HTTP " + response.statusCode();
                log.warn("[HESTIA-HEARTBEAT] Received non-2xx status from edge: {}", response.statusCode());
            }
        } catch (Exception ex) {
            lastStatusMessage = "Ping #" + totalPingsSent + " transient network notice: " + ex.getMessage();
            log.warn("[HESTIA-HEARTBEAT] Ping notice (Render may be provisioning or cold): {}", ex.getMessage());
        }
    }

    public String resolveEffectivePublicUrl() {
        String envRenderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (envRenderUrl != null && !envRenderUrl.isBlank()) {
            return envRenderUrl.trim();
        }
        if (configuredPublicUrl != null && !configuredPublicUrl.isBlank()) {
            return configuredPublicUrl.trim();
        }
        return "https://hestia-kw5k.onrender.com";
    }

    public Map<String, Object> getTelemetryStatus() {
        long uptimeMinutes = Duration.between(startTime, Instant.now()).toMinutes();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "Hestia Autonomous Vault");
        status.put("antiSpinDownDaemon", "ACTIVE (10-Minute Periodic Pulse)");
        status.put("uptimeMinutes", uptimeMinutes);
        status.put("totalPingsSent", totalPingsSent);
        status.put("successfulPings", successfulPings);
        status.put("lastPingTime", lastPingTime != null ? lastPingTime.toString() : "Pending");
        status.put("lastStatusMessage", lastStatusMessage);
        status.put("monitoredUrl", resolveEffectivePublicUrl());
        return status;
    }
}
