package org.icc.pecesatierra.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de las pruebas de integracion.
 *
 * <p>Sin Docker las clases que hereden de aqui se saltan (no fallan), de modo que
 * {@code gradlew test} sigue siendo ejecutable en una maquina sin Docker. El
 * esquema se genera con Hibernate ({@code create-drop}) solo para el perfil
 * {@code test}; produccion conserva {@code validate}.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("peces_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("db/testcontainers-init.sql");

    /**
     * M-11: {@code @ServiceConnection} solo sustituye {@code spring.datasource.*}.
     * El datasource de IA se apunta tambien al contenedor para que un futuro
     * {@code SqlExecutorIT} no consulte {@code localhost}. Los suppliers son
     * perezosos: la URL se resuelve cuando se inyecta el bean.
     */
    @DynamicPropertySource
    static void aiDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("ai.datasource.username", POSTGRES::getUsername);
        registry.add("ai.datasource.password", POSTGRES::getPassword);
    }
}
