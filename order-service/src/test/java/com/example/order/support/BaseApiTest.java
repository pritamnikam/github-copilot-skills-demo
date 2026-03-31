package com.example.order.support;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Base class for all API / functional tests.
 * Extends BaseIntegrationTest — inherits all containers and WireMock.
 * Adds RestAssured setup on top.
 *
 * Usage:
 *   class PlaceOrderApiTest extends BaseApiTest { ... }
 */
public abstract class BaseApiTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    // Authenticated spec — use for all protected endpoint tests
    protected RequestSpecification authSpec;

    // Unauthenticated spec — use for 401 tests only
    protected RequestSpecification noAuthSpec;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port    = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        authSpec = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addHeader("Authorization",
                "Bearer " + JwtTestHelper.tokenForUser("user-1"))
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .build();

        noAuthSpec = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    protected RequestSpecification withToken(String userId) {
        return new RequestSpecBuilder()
            .addRequestSpecification(noAuthSpec)
            .addHeader("Authorization",
                "Bearer " + JwtTestHelper.tokenForUser(userId))
            .build();
    }

    protected RequestSpecification withExpiredToken() {
        return new RequestSpecBuilder()
            .addRequestSpecification(noAuthSpec)
            .addHeader("Authorization",
                "Bearer " + JwtTestHelper.expiredToken("user-1"))
            .build();
    }

    protected RequestSpecification withMalformedToken() {
        return new RequestSpecBuilder()
            .addRequestSpecification(noAuthSpec)
            .addHeader("Authorization", "Bearer not.a.real.token")
            .build();
    }

    protected RequestSpecification withWrongContentType() {
        return new RequestSpecBuilder()
            .addRequestSpecification(authSpec)
            .setContentType(ContentType.XML)
            .build();
    }
}