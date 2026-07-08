package com.koutilya.gateway.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Consumer-side contract test for the gateway. It stands up a WireMock "orders-service" that
 * returns the canonical Orders contract shape (the same shape the producer's Spring Cloud Contract
 * suite verifies in the orders-service module), pins the canary weight to 100%, and asserts that:
 *   1. the gateway routes /api/orders/** to the extracted service (X-Order-Backend header), and
 *   2. the contract body the gateway receives is exactly what it expects.
 * <p>
 * This runs fully offline. In CI the WireMock stub can instead be sourced from the producer's
 * published stub jar via Spring Cloud Contract Stub Runner; the expectation is identical.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "migration.canary.initial-weight=100",
        "migration.routing.orders-service-uri=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0)
@AutoConfigureWebTestClient
class OrdersConsumerContractTest {

    private static final String ORDER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    WebTestClient webTestClient;

    @Test
    void gatewayRoutesToOrdersServiceAndReceivesContractShape() {
        stubFor(get(urlEqualTo("/api/orders/" + ORDER_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "%s",
                                  "customerId": 42,
                                  "status": "PLACED",
                                  "totalAmount": 199.99,
                                  "version": 1
                                }
                                """.formatted(ORDER_ID))));

        webTestClient.get().uri("/api/orders/{id}", ORDER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Order-Backend", "orders-service")
                .expectBody()
                .jsonPath("$.id").isEqualTo(ORDER_ID)
                .jsonPath("$.customerId").isEqualTo(42)
                .jsonPath("$.status").isEqualTo("PLACED")
                .jsonPath("$.totalAmount").isEqualTo(199.99)
                .jsonPath("$.version").isEqualTo(1);
    }
}
