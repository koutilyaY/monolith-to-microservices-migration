package com.koutilya.gateway.canary;

import com.koutilya.gateway.metrics.BackendMetricsFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Automated SLO guard for the canary. On a fixed interval it reads the per-backend metrics emitted
 * by {@link BackendMetricsFilter} for orders-service, computes the error rate and mean latency over
 * the LAST interval (delta of cumulative counters, so old data does not pin the decision), and if
 * either breaches its threshold it flips all Orders traffic back to the monolith via
 * {@link CanaryState#rollback}. This is the "automated rollback on SLO breach" mechanism.
 * <p>
 * Deliberately minimal: a real deployment would source these from Prometheus with proper
 * percentiles and a burn-rate window, but the control loop is exactly this.
 */
@Component
public class SloWatcher {

    private static final Logger log = LoggerFactory.getLogger(SloWatcher.class);

    private static final String TARGET = "orders-service";

    private final MeterRegistry registry;
    private final CanaryState canaryState;
    private final double errorRateThreshold;
    private final long minRequests;
    private final double latencyThresholdMs;

    private double prevCount;
    private double prevErrors;
    private double prevTotalTimeMs;

    public SloWatcher(MeterRegistry registry,
                      CanaryState canaryState,
                      @Value("${migration.slo.error-rate-threshold:0.05}") double errorRateThreshold,
                      @Value("${migration.slo.min-requests:20}") long minRequests,
                      @Value("${migration.slo.latency-threshold-ms:500}") double latencyThresholdMs) {
        this.registry = registry;
        this.canaryState = canaryState;
        this.errorRateThreshold = errorRateThreshold;
        this.minRequests = minRequests;
        this.latencyThresholdMs = latencyThresholdMs;
    }

    @Scheduled(fixedDelayString = "${migration.slo.check-interval-ms:5000}")
    public void evaluate() {
        double count = requestCount(null);
        double errors = requestCount("error");
        double totalTimeMs = totalTimeMs();

        double dCount = count - prevCount;
        double dErrors = errors - prevErrors;
        double dTimeMs = totalTimeMs - prevTotalTimeMs;

        prevCount = count;
        prevErrors = errors;
        prevTotalTimeMs = totalTimeMs;

        // Only act when the new service is actually taking traffic and we have a meaningful sample.
        if (canaryState.getNewServiceWeight() == 0 || dCount < minRequests) {
            return;
        }

        double errorRate = dErrors / dCount;
        double meanLatencyMs = dTimeMs / dCount;

        if (errorRate > errorRateThreshold) {
            canaryState.rollback(String.format(
                    "orders-service error rate %.1f%% over last interval exceeded SLO %.1f%%",
                    errorRate * 100, errorRateThreshold * 100));
            return;
        }
        if (meanLatencyMs > latencyThresholdMs) {
            canaryState.rollback(String.format(
                    "orders-service mean latency %.0fms over last interval exceeded SLO %.0fms",
                    meanLatencyMs, latencyThresholdMs));
            return;
        }
        log.debug("SLO ok for orders-service: rate={}%, meanLatency={}ms over {} reqs",
                errorRate * 100, meanLatencyMs, (long) dCount);
    }

    private double requestCount(String outcome) {
        var search = registry.find(BackendMetricsFilter.METRIC).tag("target", TARGET);
        if (outcome != null) {
            search = search.tag("outcome", outcome);
        }
        double total = 0;
        for (Timer t : search.timers()) {
            total += t.count();
        }
        return total;
    }

    private double totalTimeMs() {
        double total = 0;
        for (Timer t : registry.find(BackendMetricsFilter.METRIC).tag("target", TARGET).timers()) {
            total += t.totalTime(TimeUnit.MILLISECONDS);
        }
        return total;
    }
}
