package com.example.order.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public abstract class BaseIntegrationTest {

    // ── Singleton containers (started once, shared across all IT tests) ──────

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orders")
            .withUsername("orders")
            .withPassword("orders");

    static final KafkaContainer KAFKA =
        new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    // WireMock servers — one per downstream service
    static final WireMockServer USER_SERVICE_MOCK =
        new WireMockServer(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("wiremock/user-service"));

    static final WireMockServer INVENTORY_SERVICE_MOCK =
        new WireMockServer(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("wiremock/inventory-service"));

    // Start all once
    static {
        POSTGRES.start();
        KAFKA.start();
        USER_SERVICE_MOCK.start();
        INVENTORY_SERVICE_MOCK.start();
    }

    // ── Wire Spring properties to container ports ─────────────────────────────

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",  POSTGRES::getUsername);
        registry.add("spring.datasource.password",  POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        // Point Feign clients at WireMock ports
        registry.add("clients.user-service.url",
            () -> "http://localhost:" + USER_SERVICE_MOCK.port());
        registry.add("clients.inventory-service.url",
            () -> "http://localhost:" + INVENTORY_SERVICE_MOCK.port());
    }

    // ── Injected test helpers ─────────────────────────────────────────────────

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeEach
    void resetWireMock() {
        // Reset all recorded interactions before each test
        // Stub files from classpath are still loaded — only runtime stubs clear
        USER_SERVICE_MOCK.resetRequests();
        INVENTORY_SERVICE_MOCK.resetRequests();
    }

    @AfterEach
    void verifyNoUnexpectedCalls() {
        // Optional: assert no unexpected calls were made
        // Uncomment to enforce strict stub verification:
        // USER_SERVICE_MOCK.verify(0, anyRequestedFor(anyUrl())
        //     .withoutHeader("X-Expected"));
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    protected String readStub(String relativePath) throws IOException {
        return Files.readString(
            Path.of("src/test/resources/wiremock/" + relativePath));
    }

    protected WireMockServer userServiceMock() {
        return USER_SERVICE_MOCK;
    }

    protected WireMockServer inventoryServiceMock() {
        return INVENTORY_SERVICE_MOCK;
    }
}