package org.icc.pecesatierra.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
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
}
