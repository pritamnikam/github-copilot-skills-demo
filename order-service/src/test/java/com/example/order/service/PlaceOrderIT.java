package com.example.order.service;

import com.example.order.domain.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@Rollback
@Sql("/test-data/PlaceOrderIT.sql")
class PlaceOrderIT extends BaseIntegrationTest {

    @Autowired
    OrderRepository orderRepository;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_happyPath_persistsOrderAndReturns201() throws Exception {
        // WireMock: user exists
        userServiceMock().stubFor(
            get(urlPathMatching("/api/users/user-1"))
                .willReturn(okJson("""
                    { "id":"user-1", "email":"alice@example.com",
                      "name":"Alice Smith", "status":"ACTIVE" }
                """)));

        // WireMock: inventory reserves successfully
        inventoryServiceMock().stubFor(
            post(urlPathEqualTo("/api/inventory/reserve"))
                .willReturn(okJson("""
                    { "reservationId":"res-abc", "productId":"prod-1",
                      "quantity":2, "expiresAt":"2026-12-01T12:00:00Z" }
                """)));

        // Execute
        var result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.userId").value("user-1"))
            .andReturn();

        // Assert DB state
        String orderId = objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("id").asText();

        var saved = orderRepository.findById(orderId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.get().getQuantity()).isEqualTo(2);

        // Assert WireMock was actually called
        userServiceMock().verify(1,
            getRequestedFor(urlPathMatching("/api/users/user-1")));
        inventoryServiceMock().verify(1,
            postRequestedFor(urlPathEqualTo("/api/inventory/reserve")));
    }

    // ── User not found ────────────────────────────────────────────────────────

    @Test
    void placeOrder_userNotFound_returns404_noOrderPersisted() throws Exception {
        userServiceMock().stubFor(
            get(urlPathMatching("/api/users/unknown-user"))
                .willReturn(notFound().withBody("""
                    { "error":"User not found", "code":"USER_NOT_FOUND" }
                """)));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId":"unknown-user", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        // Nothing should be persisted
        assertThat(orderRepository.count()).isZero();

        // Inventory should never be called
        inventoryServiceMock().verify(0,
            postRequestedFor(urlPathEqualTo("/api/inventory/reserve")));
    }

    // ── Insufficient inventory ────────────────────────────────────────────────

    @Test
    void placeOrder_insufficientInventory_returns409_noOrderPersisted() throws Exception {
        userServiceMock().stubFor(
            get(urlPathMatching("/api/users/user-1"))
                .willReturn(okJson("""
                    { "id":"user-1", "email":"alice@example.com",
                      "name":"Alice Smith", "status":"ACTIVE" }
                """)));

        inventoryServiceMock().stubFor(
            post(urlPathEqualTo("/api/inventory/reserve"))
                .willReturn(aResponse()
                    .withStatus(409)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        { "error":"Insufficient stock",
                          "code":"INSUFFICIENT_STOCK", "available":1 }
                    """)));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":999, "totalPrice":9999.00 }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        assertThat(orderRepository.count()).isZero();
    }

    // ── User service unavailable ──────────────────────────────────────────────

    @Test
    void placeOrder_userServiceDown_returns503() throws Exception {
        userServiceMock().stubFor(
            get(urlPathMatching("/api/users/.*"))
                .willReturn(aResponse().withStatus(503)));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """))
            .andExpect(status().isServiceUnavailable());

        assertThat(orderRepository.count()).isZero();
    }
}