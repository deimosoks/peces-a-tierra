package org.icc.pecesatierra.unit.ai;

import org.junit.jupiter.api.Test;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M-08: el datasource de IA debe usar siempre las credenciales dedicadas
 * ({@code AI_DB_USER}/{@code AI_DB_PASS}) y solo la URL puede caer a {@code DB_URL}.
 *
 * <p>El test lee el {@code application.properties} real, no valores inline: cambiar
 * las propiedades a {@code DB_USER}/{@code DB_PASS} o quitar el fallback de la URL
 * hace fallar la suite.</p>
 */
class AiDatasourcePropertiesTest {

    private static final String PROPERTIES_RESOURCE = "application.properties";

    private Properties applicationProperties() throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(PROPERTIES_RESOURCE)) {

            assertThat(input).as("%s debe estar en el classpath", PROPERTIES_RESOURCE).isNotNull();

            Properties properties = new Properties();
            properties.load(input);
            return properties;
        }
    }

    @Test
    void aiCredentialsAlwaysComeFromTheDedicatedAiRole_M08() throws IOException {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("ai.datasource.username")).isEqualTo("${AI_DB_USER}");
        assertThat(properties.getProperty("ai.datasource.password")).isEqualTo("${AI_DB_PASS}");
        // No debe existir un alias que reutilice las credenciales de la aplicacion.
        assertThat(properties.stringPropertyNames())
                .noneMatch(name -> name.startsWith("ai.datasource.")
                        && properties.getProperty(name).contains("${DB_USER}"));
        assertThat(properties.stringPropertyNames())
                .noneMatch(name -> name.startsWith("ai.datasource.")
                        && properties.getProperty(name).contains("${DB_PASS}"));
    }

    @Test
    void aiUrlFallsBackToDbUrlAndCanBeOverridden_M08() throws IOException {
        String template = applicationProperties().getProperty("ai.datasource.url");

        assertThat(template).isEqualTo("${AI_DB_URL:${DB_URL}}");

        // El constructor de dos argumentos no admite valores por defecto (valueSeparator
        // nulo); se usa el completo para reproducir el comportamiento de Spring.
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}", ":", false);

        assertThat(helper.replacePlaceholders(template,
                Map.of("DB_URL", "jdbc:postgresql://db.local/app")::get))
                .isEqualTo("jdbc:postgresql://db.local/app");

        assertThat(helper.replacePlaceholders(template,
                Map.of("DB_URL", "jdbc:postgresql://db.local/app",
                        "AI_DB_URL", "jdbc:postgresql://ai.local/app")::get))
                .isEqualTo("jdbc:postgresql://ai.local/app");
    }
}
