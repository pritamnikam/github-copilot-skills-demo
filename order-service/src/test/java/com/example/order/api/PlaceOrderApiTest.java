package com.example.order.api;

import com.example.order.support.BaseApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@Transactional
@Rollback
@DisplayName("POST /api/orders")
class PlaceOrderApiTest extends BaseApiTest {

    @BeforeEach
    void stubUpstreamServices() {
        // Default happy-path stubs — individual tests override as needed
        userServiceMock().stubFor(
            get(urlPathMatching("/api/users/.*"))
                .willReturn(okJson("""
                    { "id":"user-1", "email":"alice@example.com",
                      "name":"Alice Smith", "status":"ACTIVE" }
                """)));

        inventoryServiceMock().stubFor(
            post(urlPathEqualTo("/api/inventory/reserve"))
                .willReturn(okJson("""
                    { "reservationId":"res-abc", "productId":"prod-1",
                      "quantity":2, "expiresAt":"2026-12-01T12:00:00Z" }
                """)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Happy path
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("2xx — success")
    class Success {

        @Test
        @DisplayName("valid request returns 201 with order body")
        void placeOrder_validRequest_returns201WithOrderBody() {
            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .contentType("application/json")
                .body("userId",     equalTo("user-1"))
                .body("productId",  equalTo("prod-1"))
                .body("quantity",   equalTo(2))
                .body("totalPrice", equalTo(49.99f))
                .body("status",     equalTo("PENDING"))
                .body("id",         notNullValue())
                .body("createdAt",  notNullValue());
        }

        @Test
        @DisplayName("valid request response conforms to OpenAPI schema")
        void placeOrder_validRequest_responseMatchesOpenApiSchema() {
            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath(
                    "schemas/order-response.json"));
        }

        @Test
        @DisplayName("valid request returns Location header")
        void placeOrder_validRequest_returnsLocationHeader() {
            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/orders/.*"));
        }

        @Test
        @DisplayName("minimum valid quantity=1 accepted")
        void placeOrder_minimumQuantity_returns201() {
            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":1, "totalPrice":9.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .body("quantity", equalTo(1));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 400 — Validation errors
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("400 — validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("missing userId returns 400 with field error")
        void placeOrder_missingUserId_returns400() {
            given()
                .spec(authSpec)
                .body("""
                    { "productId":"prod-1", "quantity":2,
                      "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(400)
                .body(matchesJsonSchemaInClasspath(
                    "schemas/error-response.json"))
                .body("code",           equalTo("VALIDATION_ERROR"))
                .body("fields.field",   hasItem("userId"));
        }

        @Test
        @DisplayName("quantity=0 returns 400 with field error on quantity")
        void placeOrder_quantityZero_returns400() {
            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":0, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(400)
                .body("code",         equalTo("VALIDATION_ERROR"))
                .body("fields.field", hasItem("quantity"));
        }

        @Test
        @DisplayName("quantity negative returns 400")
        void placeOrder_negativeQuantity_returns400() {
            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":-1, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(400)
                .body("fields.field", hasItem("quantity"));
        }

        @Test
        @DisplayName("price=0 returns 400")
        void placeOrder_zeroPriceValue_returns400() {
            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":0 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(400)
                .body("fields.field", hasItem("totalPrice"));
        }

        @Test
        @DisplayName("empty body returns 400")
        void placeOrder_emptyBody_returns400() {
            given()
                .spec(authSpec)
                .body("{}")
            .when()
                .post("/api/orders")
            .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("wrong Content-Type returns 415")
        void placeOrder_xmlContentType_returns415() {
            given()
                .spec(withWrongContentType())
                .body("<order/>")
            .when()
                .post("/api/orders")
            .then()
                .statusCode(415);
        }

        @Test
        @DisplayName("GET on POST-only endpoint returns 405")
        void placeOrder_wrongMethod_returns405() {
            given()
                .spec(authSpec)
            .when()
                .get("/api/orders")
            .then()
                .statusCode(405);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 401 — Auth errors
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("401 — auth errors")
    class AuthErrors {

        @Test
        @DisplayName("missing token returns 401")
        void placeOrder_noToken_returns401() {
            given()
                .spec(noAuthSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("expired token returns 401")
        void placeOrder_expiredToken_returns401() {
            given()
                .spec(withExpiredToken())
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(401)
                .body("code", equalTo("TOKEN_EXPIRED"));
        }

        @Test
        @DisplayName("malformed token returns 401")
        void placeOrder_malformedToken_returns401() {
            given()
                .spec(withMalformedToken())
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(401);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Upstream errors
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("upstream dependency errors")
    class UpstreamErrors {

        @Test
        @DisplayName("user not found returns 404")
        void placeOrder_userNotFound_returns404() {
            userServiceMock().stubFor(
                get(urlPathMatching("/api/users/.*"))
                    .willReturn(notFound().withBody("""
                        { "error":"User not found",
                          "code":"USER_NOT_FOUND" }
                    """)));

            given()
                .spec(authSpec)
                .body("""
                    { "userId":"ghost-user", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(404)
                .body("code", equalTo("USER_NOT_FOUND"));
        }

        @Test
        @DisplayName("inventory insufficient returns 409")
        void placeOrder_insufficientInventory_returns409() {
            inventoryServiceMock().stubFor(
                post(urlPathEqualTo("/api/inventory/reserve"))
                    .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            { "error":"Insufficient stock",
                              "code":"INSUFFICIENT_STOCK",
                              "available":1 }
                        """)));

            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":999, "totalPrice":9999.00 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(409)
                .body("code", equalTo("INSUFFICIENT_STOCK"));
        }

        @Test
        @DisplayName("user service down returns 503")
        void placeOrder_userServiceDown_returns503() {
            userServiceMock().stubFor(
                get(urlPathMatching("/api/users/.*"))
                    .willReturn(aResponse().withStatus(503)));

            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(503);
        }

        @Test
        @DisplayName("inventory service down returns 503")
        void placeOrder_inventoryServiceDown_returns503() {
            inventoryServiceMock().stubFor(
                post(urlPathEqualTo("/api/inventory/reserve"))
                    .willReturn(aResponse().withStatus(503)));

            given()
                .spec(authSpec)
                .body("""
                    { "userId":"user-1", "productId":"prod-1",
                      "quantity":2, "totalPrice":49.99 }
                """)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(503);
        }
    }
}