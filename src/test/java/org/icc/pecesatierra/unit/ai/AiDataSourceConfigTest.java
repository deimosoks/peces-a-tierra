package org.icc.pecesatierra.unit.ai;

import org.icc.pecesatierra.configurations.AiDataSourceConfig;
import org.icc.pecesatierra.utils.sql_executor.SqlExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H-01: el endpoint de IA debe ejecutar SQL con un {@link JdbcTemplate} dedicado,
 * contra el rol de solo lectura, con topes de filas y de tiempo.
 *
 * <p>No se conecta a ninguna base de datos: {@link DriverManagerDataSource} abre
 * la conexion de forma perezosa, asi que basta con construir el contexto.</p>
 */
class AiDataSourceConfigTest {

    private static final String EXPECTED_URL = "jdbc:postgresql://localhost:5432/peces_test";

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withUserConfiguration(AiDataSourceConfig.class, SqlExecutor.class)
                .withPropertyValues(
                        "ai.datasource.url=" + EXPECTED_URL,
                        "ai.datasource.username=test",
                        "ai.datasource.password=test",
                        "ai.datasource.driver-class-name=org.postgresql.Driver");
    }

    @Test
    void aiJdbcTemplateHasDefensiveLimitsAndDedicatedDatasource_H01() {
        contextRunner().run(context -> {
            assertThat(context).hasSingleBean(JdbcTemplate.class);

            JdbcTemplate jdbcTemplate = context.getBean("aiJdbcTemplate", JdbcTemplate.class);

            assertThat(jdbcTemplate.getMaxRows()).isEqualTo(1000);
            assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(10);

            DataSource dataSource = jdbcTemplate.getDataSource();
            assertThat(dataSource).isInstanceOf(DriverManagerDataSource.class);

            DriverManagerDataSource driverManagerDataSource = (DriverManagerDataSource) dataSource;
            assertThat(driverManagerDataSource.getUrl()).isEqualTo(EXPECTED_URL);
            assertThat(driverManagerDataSource.getUsername()).isEqualTo("test");
        });
    }

    @Test
    void driverManagerDataSourceIsNotExposedAsBean_H01() {
        contextRunner().run(context ->
                assertThat(context).doesNotHaveBean(DataSource.class));
    }

    @Test
    void sqlExecutorIsWiredToTheAiJdbcTemplate_H01() {
        contextRunner().run(context -> {
            JdbcTemplate aiJdbcTemplate = context.getBean("aiJdbcTemplate", JdbcTemplate.class);
            SqlExecutor sqlExecutor = context.getBean(SqlExecutor.class);

            assertThat(ReflectionTestUtils.getField(sqlExecutor, "jdbcTemplate"))
                    .isSameAs(aiJdbcTemplate);
        });
    }

}
