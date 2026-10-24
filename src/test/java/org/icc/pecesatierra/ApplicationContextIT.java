package org.icc.pecesatierra;

import org.icc.pecesatierra.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Fase 0: comprueba que el contexto completo arranca con el perfil de test y
 * PostgreSQL real (Testcontainers). Se salta sola si Docker no esta disponible.
 */
class ApplicationContextIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // El fallo, si lo hay, lo produce el arranque del contexto.
    }
}
