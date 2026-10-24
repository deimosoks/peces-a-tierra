package org.icc.pecesatierra.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Datasource de solo lectura para las consultas generadas por el LLM (H-01).
 *
 * <p>No se declara un bean {@link javax.sql.DataSource} adicional a proposito: la
 * presencia de un segundo {@code DataSource} hace que {@code DataSourceAutoConfiguration}
 * se retire y rompe el datasource principal de JPA y {@code @ServiceConnection} de
 * Testcontainers. Aqui solo se expone el {@link JdbcTemplate} dedicado; el
 * {@link DriverManagerDataSource} es un detalle interno, sin pool, que abre una
 * conexion por consulta.</p>
 *
 * <p>El rol de base de datos {@code peces_ai_readonly} es quien garantiza que las
 * consultas no escriban; ver {@code audit/scripts/neon-ai-readonly.sql}. Los limites
 * de filas y de tiempo son defensa en profundidad contra consultas costosas
 * generadas por el modelo.</p>
 */
@Configuration
public class AiDataSourceConfig {

    /** Limite defensivo de filas que puede devolver una consulta de IA. */
    static final int AI_MAX_ROWS = 1000;

    /** Limite defensivo, en segundos, para una consulta de IA. */
    static final int AI_QUERY_TIMEOUT_SECONDS = 10;

    @Bean(name = "aiJdbcTemplate")
    public JdbcTemplate aiJdbcTemplate(
            @Value("${ai.datasource.url}") String url,
            @Value("${ai.datasource.username}") String username,
            @Value("${ai.datasource.password}") String password,
            @Value("${ai.datasource.driver-class-name}") String driverClassName) {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setMaxRows(AI_MAX_ROWS);
        jdbcTemplate.setQueryTimeout(AI_QUERY_TIMEOUT_SECONDS);
        return jdbcTemplate;
    }

}
