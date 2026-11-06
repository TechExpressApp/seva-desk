package com.sevadesk;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * One real PostgreSQL (with pgvector) for all integration tests, started once per test run.
 * Flyway migrates as the owner; the app connects as the non-owner sevadesk_app role so row-level security applies.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class PostgresTestBase {

    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withInitScript("db/test-init.sql");

    static {
        DB.start();
    }

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", () -> "sevadesk_app");
        r.add("spring.datasource.password", () -> "app_test_password");
        r.add("spring.flyway.user", DB::getUsername);
        r.add("spring.flyway.password", DB::getPassword);
    }

    protected static RequestPostProcessor host(String name) {
        return req -> {
            req.setServerName(name);
            return req;
        };
    }
}
