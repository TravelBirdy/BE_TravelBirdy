package com.travelbird;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class TravelBirdApplicationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("travelbird_test")
        .withUsername("travelbird")
        .withPassword("travelbird");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret");
        registry.add("app.naver.client-id", () -> "test-client-id");
        registry.add("app.naver.client-secret", () -> "test-client-secret");
        registry.add("app.ai.server-base-url", () -> "http://localhost:18080");
        registry.add("app.ai.internal-key", () -> "test-internal-ai-key");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:3000");
        registry.add("app.aws.region", () -> "ap-northeast-2");
        registry.add("app.aws.s3.bucket", () -> "travelbird-test");
    }

    @Test
    void contextLoads() {
    }
}
