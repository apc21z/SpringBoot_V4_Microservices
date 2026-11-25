package com.springboot_microservices.messaging;

import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        String eventType,
        String occurredAt,
        Long orderId,
        String customerEmail,
        List<OrderLineEvent> items) {
}
