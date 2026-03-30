package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.client.UserClient;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.events.OrderCreatedEvent;
import com.example.order.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final UserClient userClient;
    private final InventoryClient inventoryClient;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderService(UserClient userClient,
                        InventoryClient inventoryClient,
                        OrderRepository orderRepository,
                        KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.userClient       = userClient;
        this.inventoryClient  = inventoryClient;
        this.orderRepository  = orderRepository;
        this.kafkaTemplate    = kafkaTemplate;
    }

    @Transactional
    public Order placeOrder(String userId, String productId,
                            int quantity, double totalPrice) {
        // 1. Verify user exists (throws FeignException.NotFound if 404)
        userClient.getUser(userId);

        // 2. Reserve inventory (throws FeignException.Conflict if 409)
        var reservation = inventoryClient.reserve(
            new InventoryClient.ReserveRequest(productId, quantity, null));

        // 3. Persist order
        Order order = orderRepository.save(
            Order.of(userId, productId, quantity, totalPrice));

        // 4. Publish event
        kafkaTemplate.send("orders.created",
            order.getId(),
            new OrderCreatedEvent(
                order.getId(), userId, productId,
                quantity, totalPrice,
                OrderStatus.PENDING.name(),
                order.getCreatedAt().toString()
            ));

        return order;
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new OrderAlreadyShippedException(orderId);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}