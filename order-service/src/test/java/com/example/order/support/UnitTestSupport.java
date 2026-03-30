package com.example.order.support;

import com.example.order.client.InventoryClient;
import com.example.order.client.UserClient;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.events.OrderCreatedEvent;

import java.time.Instant;

/**
 * Shared test data factories for unit tests.
 * Import statically: import static com.example.order.support.UnitTestSupport.*;
 *
 * No Spring context. No mocks here — just data builders.
 * Each test constructs its own mocks via @Mock + @InjectMocks.
 */
public final class UnitTestSupport {

    private UnitTestSupport() {}

    // ── User stubs ────────────────────────────────────────────────────────────

    public static UserClient.UserDTO activeUser() {
        return new UserClient.UserDTO("user-1", "alice@example.com",
                                     "Alice Smith", "ACTIVE");
    }

    public static UserClient.UserDTO inactiveUser() {
        return new UserClient.UserDTO("user-2", "bob@example.com",
                                     "Bob Jones", "INACTIVE");
    }

    public static UserClient.UserDTO userWithId(String id) {
        return new UserClient.UserDTO(id, id + "@example.com",
                                     "User " + id, "ACTIVE");
    }

    // ── Inventory stubs ───────────────────────────────────────────────────────

    public static InventoryClient.ReservationDTO successfulReservation() {
        return new InventoryClient.ReservationDTO(
            "res-abc-123", "prod-1", 2, "2026-12-01T12:00:00Z");
    }

    public static InventoryClient.ReserveRequest reserveRequest(
            String productId, int quantity, String orderId) {
        return new InventoryClient.ReserveRequest(productId, quantity, orderId);
    }

    // ── Order builders ────────────────────────────────────────────────────────

    public static Order pendingOrder() {
        return orderWithStatus(OrderStatus.PENDING);
    }

    public static Order shippedOrder() {
        return orderWithStatus(OrderStatus.SHIPPED);
    }

    public static Order cancelledOrder() {
        return orderWithStatus(OrderStatus.CANCELLED);
    }

    public static Order orderWithStatus(OrderStatus status) {
        Order order = Order.of("user-1", "prod-1", 2, 49.99);
        order.setStatus(status);
        return order;
    }

    public static Order orderWithId(String id) {
        // Use reflection to set ID for test scenarios needing a known ID
        Order order = Order.of("user-1", "prod-1", 2, 49.99);
        try {
            var field = Order.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set order ID in test", e);
        }
        return order;
    }

    // ── Event builders ────────────────────────────────────────────────────────

    public static OrderCreatedEvent orderCreatedEvent(String orderId) {
        return new OrderCreatedEvent(
            orderId, "user-1", "prod-1", 2, 49.99,
            "PENDING", Instant.now().toString());
    }

    // ── Common invalid inputs ─────────────────────────────────────────────────

    public static final String NULL_STRING    = null;
    public static final String EMPTY_STRING   = "";
    public static final String BLANK_STRING   = "   ";
    public static final int    ZERO_QUANTITY  = 0;
    public static final int    NEG_QUANTITY   = -1;
    public static final double ZERO_PRICE     = 0.0;
    public static final double NEG_PRICE      = -1.0;
}