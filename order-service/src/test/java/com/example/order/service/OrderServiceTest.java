package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.client.UserClient;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.events.OrderCreatedEvent;
import com.example.order.exception.InvalidOrderException;
import com.example.order.exception.OrderAlreadyShippedException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.repository.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static com.example.order.support.UnitTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @InjectMocks
    OrderService sut;

    @Mock UserClient                                  userClient;
    @Mock InventoryClient                             inventoryClient;
    @Mock OrderRepository                             orderRepository;
    @Mock KafkaTemplate<String, OrderCreatedEvent>    kafkaTemplate;

    // ══════════════════════════════════════════════════════════════════════════
    // placeOrder
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        @DisplayName("happy path — persists order and publishes event")
        void placeOrder_validInputs_persistsOrderAndPublishesEvent() {
            // Arrange
            when(userClient.getUser("user-1"))
                .thenReturn(activeUser());
            when(inventoryClient.reserve(any()))
                .thenReturn(successfulReservation());
            when(orderRepository.save(any()))
                .thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    return orderWithId("ord-new-1");
                });

            // Act
            Order result = sut.placeOrder("user-1", "prod-1", 2, 49.99);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user-1");
            assertThat(result.getProductId()).isEqualTo("prod-1");
            assertThat(result.getQuantity()).isEqualTo(2);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

            // Side effects
            verify(orderRepository).save(any(Order.class));
            verify(kafkaTemplate).send(
                eq("orders.created"), anyString(), any(OrderCreatedEvent.class));
        }

        // ── Input validation ──────────────────────────────────────────────────

        @Test
        @DisplayName("null userId — throws IllegalArgumentException")
        void placeOrder_nullUserId_throwsIllegalArgumentException() {
            // Act + Assert
            assertThatThrownBy(
                () -> sut.placeOrder(null, "prod-1", 2, 49.99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");

            verifyNoInteractions(userClient, inventoryClient,
                                 orderRepository, kafkaTemplate);
        }

        @Test
        @DisplayName("blank userId — throws IllegalArgumentException")
        void placeOrder_blankUserId_throwsIllegalArgumentException() {
            assertThatThrownBy(
                () -> sut.placeOrder(BLANK_STRING, "prod-1", 2, 49.99))
                .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(userClient);
        }

        @ParameterizedTest(name = "quantity={0}")
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("invalid quantity — throws InvalidOrderException")
        void placeOrder_invalidQuantity_throwsInvalidOrderException(int qty) {
            assertThatThrownBy(
                () -> sut.placeOrder("user-1", "prod-1", qty, 49.99))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("quantity");

            verifyNoInteractions(userClient, inventoryClient);
        }

        @ParameterizedTest(name = "price={0}")
        @ValueSource(doubles = {0.0, -1.0, -99.99})
        @DisplayName("invalid price — throws InvalidOrderException")
        void placeOrder_invalidPrice_throwsInvalidOrderException(double price) {
            assertThatThrownBy(
                () -> sut.placeOrder("user-1", "prod-1", 2, price))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("price");

            verifyNoInteractions(userClient, inventoryClient);
        }

        // ── Dependency failures ───────────────────────────────────────────────

        @Test
        @DisplayName("user not found — throws UserNotFoundException, no DB write")
        void placeOrder_userNotFound_throwsUserNotFoundException() {
            // Arrange
            when(userClient.getUser("ghost-user"))
                .thenThrow(FeignException.NotFound.class);

            // Act + Assert
            assertThatThrownBy(
                () -> sut.placeOrder("ghost-user", "prod-1", 2, 49.99))
                .isInstanceOf(UserNotFoundException.class);

            // No DB write, no event
            verifyNoInteractions(inventoryClient, orderRepository, kafkaTemplate);
        }

        @Test
        @DisplayName("inventory insufficient — throws exception, no DB write")
        void placeOrder_inventoryInsufficient_throwsInventoryUnavailableException() {
            // Arrange
            when(userClient.getUser("user-1")).thenReturn(activeUser());
            when(inventoryClient.reserve(any()))
                .thenThrow(FeignException.Conflict.class);

            // Act + Assert
            assertThatThrownBy(
                () -> sut.placeOrder("user-1", "prod-1", 999, 9999.00))
                .isInstanceOf(InventoryUnavailableException.class);

            verifyNoInteractions(orderRepository, kafkaTemplate);
        }

        @Test
        @DisplayName("repository save fails — exception propagates, event NOT sent")
        void placeOrder_repositorySaveFails_exceptionPropagates_noEventSent() {
            // Arrange
            when(userClient.getUser("user-1")).thenReturn(activeUser());
            when(inventoryClient.reserve(any())).thenReturn(successfulReservation());
            when(orderRepository.save(any()))
                .thenThrow(new RuntimeException("DB connection lost"));

            // Act + Assert
            assertThatThrownBy(
                () -> sut.placeOrder("user-1", "prod-1", 2, 49.99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection lost");

            // Critical: event must NOT be published if DB save failed
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("user service unavailable — throws ServiceUnavailableException")
        void placeOrder_userServiceDown_throwsServiceUnavailableException() {
            when(userClient.getUser(anyString()))
                .thenThrow(FeignException.ServiceUnavailable.class);

            assertThatThrownBy(
                () -> sut.placeOrder("user-1", "prod-1", 2, 49.99))
                .isInstanceOf(ServiceUnavailableException.class);
        }

        @Test
        @DisplayName("verifies kafka event contains correct payload")
        void placeOrder_happyPath_kafkaEventContainsCorrectPayload() {
            // Arrange
            when(userClient.getUser("user-1")).thenReturn(activeUser());
            when(inventoryClient.reserve(any())).thenReturn(successfulReservation());
            when(orderRepository.save(any())).thenReturn(orderWithId("ord-123"));

            // Act
            sut.placeOrder("user-1", "prod-1", 2, 49.99);

            // Assert event payload
            var eventCaptor = org.mockito.ArgumentCaptor
                .forClass(OrderCreatedEvent.class);
            verify(kafkaTemplate).send(
                eq("orders.created"), eq("ord-123"), eventCaptor.capture());

            OrderCreatedEvent event = eventCaptor.getValue();
            assertThat(event.orderId()).isEqualTo("ord-123");
            assertThat(event.userId()).isEqualTo("user-1");
            assertThat(event.productId()).isEqualTo("prod-1");
            assertThat(event.quantity()).isEqualTo(2);
            assertThat(event.status()).isEqualTo("PENDING");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // cancelOrder
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("happy path — PENDING order becomes CANCELLED")
        void cancelOrder_pendingOrder_statusSetToCancelled() {
            // Arrange
            Order existing = orderWithId("ord-1");
            existing.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById("ord-1"))
                .thenReturn(Optional.of(existing));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Order result = sut.cancelOrder("ord-1");

            // Assert
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(argThat(
                o -> o.getStatus() == OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("null orderId — throws IllegalArgumentException")
        void cancelOrder_nullOrderId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> sut.cancelOrder(null))
                .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("order not found — throws OrderNotFoundException")
        void cancelOrder_orderNotFound_throwsOrderNotFoundException() {
            when(orderRepository.findById("missing"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.cancelOrder("missing"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("missing");
        }

        @Test
        @DisplayName("shipped order — throws OrderAlreadyShippedException")
        void cancelOrder_shippedOrder_throwsOrderAlreadyShippedException() {
            when(orderRepository.findById("ord-shipped"))
                .thenReturn(Optional.of(shippedOrder()));

            assertThatThrownBy(() -> sut.cancelOrder("ord-shipped"))
                .isInstanceOf(OrderAlreadyShippedException.class);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("already cancelled order — throws InvalidOrderException")
        void cancelOrder_alreadyCancelled_throwsInvalidOrderException() {
            when(orderRepository.findById("ord-cancelled"))
                .thenReturn(Optional.of(cancelledOrder()));

            assertThatThrownBy(() -> sut.cancelOrder("ord-cancelled"))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("already cancelled");

            verify(orderRepository, never()).save(any());
        }
    }
}