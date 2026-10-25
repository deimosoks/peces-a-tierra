package org.icc.pecesatierra.unit.ai;

import org.icc.pecesatierra.configurations.SqlQueryValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de regresion de H-01 en su parte acotada: el validador debe rechazar
 * sentencias apiladas, verbos de escritura, catalogos del sistema, funciones
 * peligrosas, columnas sensibles y {@code UNION}, sin romper las consultas de
 * solo lectura que usa el asistente.
 */
class SqlQueryValidatorTest {

    private final SqlQueryValidator validator = new SqlQueryValidator();

    // ---------- Consultas legitimas ----------

    @Test
    void legitimateAssistantQueriesAreAccepted() {
        List<String> queries = List.of(
                "SELECT m.complete_name, b.name FROM members m "
                        + "JOIN branches b ON m.branch_id = b.id "
                        + "WHERE m.active = true ORDER BY m.complete_name LIMIT 50",
                "SELECT t.name, COUNT(*) AS total FROM members m "
                        + "JOIN member_types t ON m.type_id = t.id "
                        + "WHERE m.branch_id = 'branch-1' GROUP BY t.name ORDER BY total DESC LIMIT 10",
                "SELECT COUNT(*) FROM attendances a "
                        + "JOIN service_events e ON a.service_event_id = e.id "
                        + "WHERE e.start_date_time >= '2026-01-01' AND a.invalid = false",
                "WITH totales AS (SELECT member_id, COUNT(*) AS total FROM attendances GROUP BY member_id) "
                        + "SELECT member_id, total FROM totales ORDER BY total DESC LIMIT 20",
                "SELECT complete_name, cellphone FROM members WHERE complete_name ILIKE '%ana%' LIMIT 5;"
        );

        for (String query : queries) {
            assertThat(validator.isSafeSelect(query))
                    .as("consulta legitima rechazada: %s", query)
                    .isTrue();
        }
    }

    @Test
    void nullAndEmptyQueriesAreRejected() {
        assertThat(validator.isSafeSelect(null)).isFalse();
        assertThat(validator.isSafeSelect("")).isFalse();
        assertThat(validator.isSafeSelect("   ")).isFalse();
    }

    @Test
    void nonSelectQueriesAreRejected() {
        assertThat(validator.isSafeSelect("DELETE FROM members")).isFalse();
        assertThat(validator.isSafeSelect("DO $$BEGIN END$$")).isFalse();
        assertThat(validator.isSafeSelect("CALL mantenimiento()")).isFalse();
    }

    // ---------- H-01: sentencias multiples ----------

    @Test
    void stackedStatementsAreRejected_H01() {
        List<String> queries = List.of(
                "SELECT 1;;SET statement_timeout=0",
                "; ;DO $$BEGIN UPDATE members SET active = false; END$$",
                "SELECT 1; DROP TABLE users",
                "SELECT 1;SET statement_timeout=0",
                "SELECT 1;SELECT 2",
                "SELECT complete_name FROM members; SELECT password_hash FROM users"
        );

        for (String query : queries) {
            assertThat(validator.isSafeSelect(query))
                    .as("sentencia apilada aceptada: %s", query)
                    .isFalse();
        }
    }

    @Test
    void semicolonInsideLiteralIsRejectedConservatively_H01() {
        // Es una consulta legitima en apariencia, pero el validador no distingue
        // literales de separadores: se rechaza para no abrir el apilamiento.
        assertThat(validator.isSafeSelect("SELECT * FROM members WHERE complete_name = 'Ana;Maria'"))
                .isFalse();
    }

    // ---------- H-01: verbos y palabras clave ----------

    @Test
    void dangerousVerbsAndKeywordsAreRejected_H01() {
        List<String> keywords = List.of(
                "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT",
                "REVOKE", "EXEC", "EXECUTE", "DO", "CALL", "COPY", "SET", "COMMENT", "VACUUM",
                "ANALYZE", "REINDEX", "DISCARD", "LOCK", "PREPARE", "UNION", "INTO"
        );

        for (String keyword : keywords) {
            String query = "SELECT " + keyword + " FROM members";
            assertThat(validator.isSafeSelect(query))
                    .as("palabra clave peligrosa aceptada: %s", keyword)
                    .isFalse();
        }
    }

    @Test
    void writeQueriesBuiltAsSelectAreRejected_H01() {
        assertThat(validator.isSafeSelect("SELECT complete_name INTO copia_members FROM members")).isFalse();
        assertThat(validator.isSafeSelect("SELECT 1 FROM members UNION SELECT 1 FROM branches")).isFalse();
        assertThat(validator.isSafeSelect("SELECT 1 FROM members UNION ALL SELECT 1 FROM branches")).isFalse();
    }

    // ---------- H-01: catalogos, funciones y columnas sensibles ----------

    @Test
    void systemCatalogsAreRejected_H01() {
        assertThat(validator.isSafeSelect("SELECT table_name FROM information_schema.tables")).isFalse();
        assertThat(validator.isSafeSelect("SELECT tablename FROM pg_catalog.pg_tables")).isFalse();
    }

    @Test
    void dangerousFunctionsAreRejected_H01() {
        List<String> queries = List.of(
                "SELECT pg_sleep(60)",
                "SELECT pg_sleep(10) FROM members",
                "SELECT pg_advisory_lock(12345)",
                "SELECT set_config('statement_timeout', '0', false)",
                "SELECT current_user",
                "SELECT session_user FROM members"
        );

        for (String query : queries) {
            assertThat(validator.isSafeSelect(query))
                    .as("funcion peligrosa aceptada: %s", query)
                    .isFalse();
        }
    }

    @Test
    void sensitiveColumnsAreRejected_H01() {
        assertThat(validator.isSafeSelect("SELECT password_hash FROM users")).isFalse();
        assertThat(validator.isSafeSelect("SELECT u.password_hash, u.username FROM users u")).isFalse();
        assertThat(validator.isSafeSelect("SELECT token FROM refresh_token")).isFalse();
        assertThat(validator.isSafeSelect("SELECT rt.refresh_token FROM refresh_token rt")).isFalse();
    }
}
