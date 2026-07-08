package com.koutilya.orders.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koutilya.orders.events.OrderEvent;
import com.koutilya.orders.service.OrderReplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderEventConsumerTest {

    private final OrderReplicationService replication = mock(OrderReplicationService.class);
    private final OrderEventConsumer consumer = new OrderEventConsumer(new ObjectMapper(), replication);

    @Test
    void deserializesOutboxPayloadAndDelegatesToReplication() {
        when(replication.apply(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        String payload = """
                {"orderId":"o-1","customerId":42,"status":"PLACED","totalAmount":199.99,"aggregateVersion":3}
                """;

        consumer.onMessage(payload);

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(replication).apply(captor.capture());
        OrderEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo("o-1");
        assertThat(event.aggregateVersion()).isEqualTo(3);
        assertThat(event.status()).isEqualTo("PLACED");
    }

    @Test
    void poisonMessageIsDroppedNotThrown() {
        consumer.onMessage("not-json");
        verify(replication, never()).apply(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unknownExtraFieldsAreTolerated() {
        when(replication.apply(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        String payload = "{\"orderId\":\"o-2\",\"customerId\":1,\"status\":\"PAID\","
                + "\"totalAmount\":5.00,\"aggregateVersion\":9,\"newFutureField\":\"ignored\"}";

        consumer.onMessage(payload);

        verify(replication).apply(org.mockito.ArgumentMatchers.any());
    }
}
