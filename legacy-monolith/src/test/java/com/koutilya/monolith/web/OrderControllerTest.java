package com.koutilya.monolith.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koutilya.monolith.domain.OrderEntity;
import com.koutilya.monolith.service.OrderService;
import com.koutilya.monolith.web.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    OrderService orderService;

    @Test
    void createOrder_returns201WithLocationAndBody() throws Exception {
        OrderEntity order = new OrderEntity("11111111-1111-1111-1111-111111111111", 42L, new BigDecimal("199.99"));
        when(orderService.createOrder(eq(42L), any())).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(42L, new BigDecimal("199.99")))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/orders/11111111-1111-1111-1111-111111111111")))
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void createOrder_rejectsMissingCustomer() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalAmount\":10.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_returnsContractShape() throws Exception {
        OrderEntity order = new OrderEntity("22222222-2222-2222-2222-222222222222", 7L, new BigDecimal("10.00"));
        when(orderService.getOrder("22222222-2222-2222-2222-222222222222")).thenReturn(order);

        mockMvc.perform(get("/api/orders/22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(7))
                .andExpect(jsonPath("$.totalAmount").value(10.00));

        Mockito.verify(orderService).getOrder("22222222-2222-2222-2222-222222222222");
    }
}
