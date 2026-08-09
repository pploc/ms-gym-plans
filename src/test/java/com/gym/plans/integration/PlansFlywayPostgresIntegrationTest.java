package com.gym.plans.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Empty-DB Flyway startup on real PostgreSQL.
 * Skips when Docker daemon is unavailable (local/CI without Docker).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
class PlansFlywayPostgresIntegrationTest {

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("plans_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        Assumptions.assumeTrue(dockerAvailable(), "Docker unavailable");
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("grpc.server.port", () -> "0");
        registry.add("grpc.server.tls.enabled", () -> "false");
        registry.add("grpc.server.tls.allow-plaintext", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenEmptyPostgres_whenFlywayMigrates_thenConstraintsAndIndexesExist() {
        // Given / When — Spring Boot + Flyway start empty DB before this method

        // Then
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """,
                String.class);
        assertTrue(tables.contains("gym_locations"));
        assertTrue(tables.contains("membership_plans"));
        assertTrue(tables.contains("flyway_schema_history"));

        List<String> checks = jdbcTemplate.queryForList(
                """
                SELECT conname FROM pg_constraint
                WHERE contype = 'c'
                  AND conrelid IN ('gym_locations'::regclass, 'membership_plans'::regclass)
                ORDER BY conname
                """,
                String.class);
        assertTrue(checks.contains("chk_gym_locations_status"));
        assertTrue(checks.contains("chk_membership_plans_type"));
        assertTrue(checks.contains("chk_membership_plans_price"));
        assertTrue(checks.contains("chk_membership_plans_duration"));

        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename IN ('gym_locations', 'membership_plans')
                ORDER BY indexname
                """,
                String.class);
        assertTrue(indexes.contains("idx_gym_locations_chain_city_status"));
        assertTrue(indexes.contains("idx_membership_plans_gym_type_active"));

        // DB enforces price_vnd >= 0
        jdbcTemplate.update(
                """
                INSERT INTO gym_locations (id, chain_id, name, address, city, status)
                VALUES ('g1', 'c', 'n', 'a', 'Hanoi', 'ACTIVE')
                """);
        Map<String, Object> ok = jdbcTemplate.queryForMap(
                """
                INSERT INTO membership_plans
                  (id, gym_id, name, plan_type, duration_days, price_vnd, description, active)
                VALUES ('p0', 'g1', 'Free', 'MONTHLY', 30, 0, '', true)
                RETURNING price_vnd
                """);
        assertEquals(0L, ((Number) ok.get("price_vnd")).longValue());

        boolean rejected = false;
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO membership_plans
                      (id, gym_id, name, plan_type, duration_days, price_vnd, description, active)
                    VALUES ('p-neg', 'g1', 'Bad', 'MONTHLY', 30, -1, '', true)
                    """);
        } catch (Exception ex) {
            rejected = true;
        }
        assertTrue(rejected, "negative price_vnd must fail chk_membership_plans_price");
    }
}
