package org.icc.pecesatierra.unit.ai;

import org.icc.pecesatierra.utils.sql_executor.SqlExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H-01: {@link SqlExecutor} solo delega en el {@link JdbcTemplate} que recibe
 * (el dedicado de IA) y conserva su firma publica.
 */
class SqlExecutorTest {

    @Test
    void delegatesQueryToTheInjectedJdbcTemplate_H01() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        List<Map<String, Object>> expected = List.of(Map.of("?column?", 1));
        when(jdbcTemplate.queryForList("SELECT 1")).thenReturn(expected);

        SqlExecutor sqlExecutor = new SqlExecutor(jdbcTemplate);

        assertThat(sqlExecutor.sqlExecutor("SELECT 1")).isSameAs(expected);
        verify(jdbcTemplate).queryForList("SELECT 1");
    }

    @Test
    void propagatesJdbcErrorsInsteadOfSwallowingThem_H01() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList("SELECT pg_sleep(60)"))
                .thenThrow(new QueryTimeoutException("query timeout"));

        SqlExecutor sqlExecutor = new SqlExecutor(jdbcTemplate);

        assertThatThrownBy(() -> sqlExecutor.sqlExecutor("SELECT pg_sleep(60)"))
                .isInstanceOf(QueryTimeoutException.class);
        verify(jdbcTemplate).queryForList("SELECT pg_sleep(60)");
    }

}
