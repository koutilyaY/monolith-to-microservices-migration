package com.koutilya.gateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Records per-backend latency and outcome for every proxied request. These are the SLO signals the
 * {@link com.koutilya.gateway.canary.SloWatcher} reads to decide whether the canary is healthy.
 * Metric: {@code gateway.backend.requests} timer, tagged {@code target} (orders-service|monolith)
 * and {@code outcome} (success|error, where error == 5xx).
 */
@Component
public class BackendMetricsFilter implements GlobalFilter, Ordered {

    public static final String METRIC = "gateway.backend.requests";

    private final MeterRegistry registry;

    public BackendMetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> record(exchange, start));
    }

    private void record(ServerWebExchange exchange, long startNanos) {
        String target = resolveTarget(exchange);
        if (target == null) {
            return; // not a proxied backend route (e.g. actuator); ignore
        }
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        boolean error = status != null && status.is5xxServerError();
        Timer.builder(METRIC)
                .tag("target", target)
                .tag("outcome", error ? "error" : "success")
                .register(registry)
                .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private String resolveTarget(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return null;
        }
        return "orders-new-service".equals(route.getId()) ? "orders-service" : "monolith";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
