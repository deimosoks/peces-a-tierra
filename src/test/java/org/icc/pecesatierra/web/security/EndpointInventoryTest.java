package org.icc.pecesatierra.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inventario estatico de los handlers HTTP. No arranca Spring: escanea los
 * {@code @RestController} y compara cada combinacion controller+verbo+ruta con
 * {@code src/test/resources/security/endpoint-matrix.csv}. Si se agrega o
 * elimina un endpoint sin actualizar el CSV, la prueba falla.
 */
class EndpointInventoryTest {

    private static final String MATRIX = "security/endpoint-matrix.csv";
    private static final int EXPECTED_HANDLERS = 69;
    private static final int EXPECTED_PRE_AUTHORIZE = 65;

    @Test
    void everyHandlerHasARowInTheEndpointMatrix() throws Exception {
        Set<Endpoint> actual = scanHandlers();
        Set<Endpoint> documented = readMatrix();

        Set<Endpoint> missingInCsv = new LinkedHashSet<>(actual);
        missingInCsv.removeAll(documented);
        Set<Endpoint> staleRows = new LinkedHashSet<>(documented);
        staleRows.removeAll(actual);

        assertThat(actual).as("handlers detectados en los controllers").hasSize(EXPECTED_HANDLERS);
        assertThat(documented).as("filas del CSV").hasSize(EXPECTED_HANDLERS);
        assertThat(missingInCsv).as("endpoints sin fila en " + MATRIX).isEmpty();
        assertThat(staleRows).as("filas del CSV sin handler real").isEmpty();
    }

    @Test
    void matrixAlsoKeepsThePreAuthorizeInventoryStable() throws Exception {
        long annotated = controllers().stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .filter(method -> !method.isSynthetic() && !method.isBridge())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .count();

        assertThat(annotated)
                .as("@PreAuthorize inventariados; actualiza PreAuthorizeSpelTest y la matriz si cambia")
                .isEqualTo(EXPECTED_PRE_AUTHORIZE);
    }

    private static Set<Endpoint> scanHandlers() throws ClassNotFoundException {
        Set<Endpoint> endpoints = new LinkedHashSet<>();
        for (Class<?> controller : controllers()) {
            RequestMapping typeMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            String[] basePaths = typeMapping == null ? new String[]{""} : pathsOf(typeMapping);

            for (Method method : controller.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge()) {
                    continue;
                }
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) {
                    continue;
                }
                String[] methodPaths = pathsOf(methodMapping);
                for (String basePath : basePaths) {
                    for (String methodPath : methodPaths) {
                        for (String verb : verbsOf(methodMapping)) {
                            endpoints.add(new Endpoint(controller.getSimpleName(), verb, join(basePath, methodPath)));
                        }
                    }
                }
            }
        }
        return endpoints;
    }

    private static Set<Endpoint> readMatrix() throws Exception {
        InputStream input = EndpointInventoryTest.class.getClassLoader().getResourceAsStream(MATRIX);
        if (input == null) {
            throw new IllegalStateException("No se encontro el recurso de test " + MATRIX);
        }

        Set<Endpoint> rows = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalStateException("La matriz de endpoints esta vacia");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split(",", -1);
                if (columns.length < 3) {
                    throw new IllegalStateException("Fila CSV invalida: " + line);
                }
                rows.add(new Endpoint(columns[0].trim(), columns[1].trim().toUpperCase(), columns[2].trim()));
            }
        }
        return rows;
    }

    private static List<Class<?>> controllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> controllers = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("org.icc.pecesatierra.features")) {
            controllers.add(Class.forName(candidate.getBeanClassName()));
        }
        return controllers;
    }

    private static String[] pathsOf(RequestMapping mapping) {
        String[] values = mapping.path().length > 0 ? mapping.path() : mapping.value();
        return values.length == 0 ? new String[]{""} : values;
    }

    private static String[] verbsOf(RequestMapping mapping) {
        RequestMethod[] methods = mapping.method();
        if (methods.length == 0) {
            return new String[]{"ANY"};
        }
        return Arrays.stream(methods).map(Enum::name).toArray(String[]::new);
    }

    private static String join(String basePath, String methodPath) {
        String base = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        if (methodPath.isEmpty()) {
            return base.isEmpty() ? "/" : base;
        }
        String suffix = methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        String joined = base + suffix;
        return joined.replaceAll("/{2,}", "/");
    }

    private record Endpoint(String controller, String httpMethod, String path) {
    }
}
