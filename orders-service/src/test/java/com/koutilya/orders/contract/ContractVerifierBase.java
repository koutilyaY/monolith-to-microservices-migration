package com.koutilya.orders.contract;

import com.koutilya.orders.domain.Order;
import com.koutilya.orders.service.OrderCommandService;
import com.koutilya.orders.service.OrderQueryService;
import com.koutilya.orders.web.OrderController;
import com.koutilya.orders.web.RestExceptionHandler;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class the Spring Cloud Contract plugin extends when it generates the producer verification
 * tests from src/test/resources/contracts. It stands up the real {@link OrderController} over
 * MockMvc with deterministic mocked services, so the generated tests exercise the ACTUAL
 * controller + serialization path against the published contract.
 */
public abstract class ContractVerifierBase {

    static final String FIXED_ID = "11111111-1111-1111-1111-111111111111";

    @BeforeEach
    void setup() {
        OrderQueryService queryService = mock(OrderQueryService.class);
        OrderCommandService commandService = mock(OrderCommandService.class);

        Order fixed = new Order(FIXED_ID, 42L, new BigDecimal("199.99"), "PLACED", 1L);
        when(queryService.getOrder(FIXED_ID)).thenReturn(fixed);
        when(commandService.createOrder(eq(42L), any(BigDecimal.class))).thenReturn(fixed);

        RestAssuredMockMvc.standaloneSetup(
                new OrderController(queryService, commandService),
                new RestExceptionHandler());
    }
}
