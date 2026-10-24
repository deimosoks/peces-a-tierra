package org.icc.pecesatierra.utils.sql_executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SqlExecutor {

    private final JdbcTemplate jdbcTemplate;

    public SqlExecutor(@Qualifier("aiJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ejecuta el SQL generado por el LLM contra la conexion de solo lectura (H-01).
     *
     * <p>Sin {@code @Transactional(readOnly = true)}: este template no participa en
     * el {@code PlatformTransactionManager} principal (su datasource es independiente)
     * y el limite real lo impone el rol {@code peces_ai_readonly}. Los topes de filas
     * y de tiempo los fija el template dedicado en {@code AiDataSourceConfig}.</p>
     */
    public List<Map<String, Object>> sqlExecutor(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

}
