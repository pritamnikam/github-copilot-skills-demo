package com.example.order.events;

public record OrderCreatedEvent(
    String orderId,
    String userId,
    String productId,
    int    quantity,
    double totalPrice,
    String status,
    String createdAt
) {}