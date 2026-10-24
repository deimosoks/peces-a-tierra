package org.icc.pecesatierra.unit.config;

import org.icc.pecesatierra.utils.RestTemplateConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H-46: el {@code RestTemplate} de Gemini debe tener timeouts finitos.
 *
 * <p>Spring Framework 6.2.15 no expone getters de timeout en
 * {@link SimpleClientHttpRequestFactory} (solo setters), asi que se leen los
 * campos privados {@code connectTimeout} y {@code readTimeout}, que estan en
 * milisegundos. Si en el futuro se anaden getters, el test puede migrar a
 * ellos sin cambiar la asercion.</p>
 */
class RestTemplateConfigTest {

    @Test
    void restTemplateHasFiniteConnectAndReadTimeouts_H46() {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate();

        assertThat(restTemplate.getRequestFactory())
                .isInstanceOf(SimpleClientHttpRequestFactory.class);

        SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();

        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout"))
                .isEqualTo(5_000);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout"))
                .isEqualTo(60_000);
    }
}
