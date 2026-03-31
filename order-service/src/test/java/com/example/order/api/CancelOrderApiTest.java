package com.example.order.api;

import com.example.order.support.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

@Transactional
@Rollback
@Sql("/test-data/CancelOrderApiTest.sql")
@DisplayName("DELETE /api/orders/{orderId}")
class CancelOrderApiTest extends BaseApiTest {

    @Nested
    @DisplayName("2xx — success")
    class Success {

        @Test
        @DisplayName("pending order cancelled successfully returns 200")
        void cancelOrder_pendingOrder_returns200WithCancelledStatus() {
            given()
                .spec(authSpec)
            .when()
                .delete("/api/orders/ord-pending-1")
            .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body(matchesJsonSchemaInClasspath(
                    "schemas/order-response.json"));
        }
    }

    @Nested
    @DisplayName("4xx — errors")
    class Errors {

        @Test
        @DisplayName("order not found returns 404")
        void cancelOrder_orderNotFound_returns404() {
            given()
                .spec(authSpec)
            .when()
                .delete("/api/orders/ord-does-not-exist")
            .then()
                .statusCode(404)
                .body("code", equalTo("ORDER_NOT_FOUND"))
                .body(matchesJsonSchemaInClasspath(
                    "schemas/error-response.json"));
        }

        @Test
        @DisplayName("shipped order returns 409")
        void cancelOrder_shippedOrder_returns409() {
            given()
                .spec(authSpec)
            .when()
                .delete("/api/orders/ord-shipped-1")
            .then()
                .statusCode(409)
                .body("code", equalTo("ORDER_ALREADY_SHIPPED"));
        }

        @Test
        @DisplayName("no auth token returns 401")
        void cancelOrder_noToken_returns401() {
            given()
                .spec(noAuthSpec)
            .when()
                .delete("/api/orders/ord-pending-1")
            .then()
                .statusCode(401);
        }
    }
}