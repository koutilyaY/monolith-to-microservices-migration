package com.koutilya.gateway.canary;

import com.koutilya.gateway.metrics.BackendMetricsFilter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SloWatcherTest {

    private SimpleMeterRegistry registry;
    private CanaryState canary;
    private SloWatcher watcher;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        canary = new CanaryState(50); // canary is live at 50%
        // errorRateThreshold=5%, minRequests=10, latencyThreshold=500ms
        watcher = new SloWatcher(registry, canary, 0.05, 10, 500);
    }

    private void record(String outcome, int n, long millis) {
        Timer timer = Timer.builder(BackendMetricsFilter.METRIC)
                .tag("target", "orders-service")
                .tag("outcome", outcome)
                .register(registry);
        for (int i = 0; i < n; i++) {
            timer.record(millis, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    void rollsBackWhenErrorRateExceedsThreshold() {
        record("error", 20, 10);
        record("success", 5, 10);

        watcher.evaluate();

        assertThat(canary.isRolledBack()).isTrue();
        assertThat(canary.getNewServiceWeight()).isZero();
        assertThat(canary.getLastChangeReason()).contains("error rate");
    }

    @Test
    void rollsBackWhenLatencyExceedsThreshold() {
        record("success", 30, 800); // healthy status codes, but slow

        watcher.evaluate();

        assertThat(canary.isRolledBack()).isTrue();
        assertThat(canary.getLastChangeReason()).contains("latency");
    }

    @Test
    void staysGreenWhenHealthy() {
        record("success", 40, 20);
        record("error", 1, 20); // 1/41 ~= 2.4% < 5%

        watcher.evaluate();

        assertThat(canary.isRolledBack()).isFalse();
        assertThat(canary.getNewServiceWeight()).isEqualTo(50);
    }

    @Test
    void ignoresIntervalsWithTooFewRequests() {
        record("error", 5, 10); // 100% error but only 5 requests < minRequests(10)

        watcher.evaluate();

        assertThat(canary.isRolledBack()).isFalse();
    }

    @Test
    void doesNothingWhenCanaryAlreadyAtZero() {
        canary.setNewServiceWeight(0, "not yet started");
        record("error", 50, 10);

        watcher.evaluate();

        assertThat(canary.isRolledBack()).isFalse();
    }
}
