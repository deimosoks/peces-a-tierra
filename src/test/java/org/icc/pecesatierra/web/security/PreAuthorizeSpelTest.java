package org.icc.pecesatierra.web.security;

import org.aopalliance.intercept.MethodInvocation;
import org.icc.pecesatierra.configurations.SecurityService;
import org.icc.pecesatierra.features.auth.dtos.RefreshTokenRequestDto;
import org.icc.pecesatierra.features.service.controller.ServiceController;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.controller.UserController;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Evalua (no solo parsea) las 67 expresiones {@code @PreAuthorize} sin arrancar
 * Spring Boot. Un {@link GenericApplicationContext} registra el bean
 * {@code securityService} y {@link DefaultMethodSecurityExpressionHandler}
 * resuelve los beans y las variables de metodo igual que en produccion.
 *
 * <p>Cubre H-09 (SpEL invalido en {@code DELETE /api/users/{id}}) y H-48
 * (precedencia OR/AND que permitia a un usuario con DELETE_SERVICE eludir
 * {@code isActive}).</p>
 */
class PreAuthorizeSpelTest {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final int EXPECTED_EXPRESSIONS = 67;

    private static GenericApplicationContext applicationContext;
    private static DefaultMethodSecurityExpressionHandler expressionHandler;
    private static SecurityService securityService;

    @BeforeAll
    static void setUpApplicationContext() {
        securityService = mock(SecurityService.class);
        when(securityService.isActive(any(Authentication.class))).thenReturn(true);
        when(securityService.isActive(any(RefreshTokenRequestDto.class))).thenReturn(true);

        applicationContext = new GenericApplicationContext();
        applicationContext.registerBean("securityService", SecurityService.class, () -> securityService);
        applicationContext.refresh();

        expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setApplicationContext(applicationContext);
    }

    @AfterAll
    static void closeApplicationContext() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    void allPreAuthorizeExpressionsEvaluateForAnAdministrator() {
        List<Method> annotatedMethods = annotatedControllerMethods();

        assertThat(annotatedMethods)
                .as("El inventario de @PreAuthorize cambio; actualiza tambien la matriz de endpoints")
                .hasSize(EXPECTED_EXPRESSIONS);

        for (Method method : annotatedMethods) {
            Authentication administrator = authenticationOfAllPermissions();
            Boolean authorized = null;
            try {
                authorized = evaluate(method, administrator, true);
            } catch (RuntimeException exception) {
                fail(describe(method) + " no se pudo evaluar: " + expressionOf(method), exception);
            }
            assertThat(authorized)
                    .as("%s deberia autorizar a un administrador", describe(method))
                    .isTrue();
        }
    }

    @Test
    void userDeleteRequiresDeleteUserAuthority_H09() throws Exception {
        Method delete = UserController.class.getDeclaredMethod("delete", User.class, String.class);
        String expression = expressionOf(delete);

        // La expresion original tenia hasAuthority(org...AppPermission).DELETE_USER.name(),
        // que no es SpEL valido y hacia fallar el endpoint con 500.
        assertThat(expression).contains("T(org.icc.pecesatierra.utils.enums.AppPermission).DELETE_USER");

        assertThat(evaluate(delete, authentication("DELETE_USER"), true)).isTrue();
        assertThat(evaluate(delete, authentication("ADMINISTRATOR"), true)).isTrue();
        assertThat(evaluate(delete, authentication("VIEW_USER_PANEL"), true)).isFalse();
        assertThat(evaluate(delete, authentication(), true)).isFalse();
        assertThat(evaluate(delete, authentication("DELETE_USER"), false)).isFalse();
    }

    @Test
    void serviceDeleteWithDeleteServiceButInactiveUserIsDenied_H48() throws Exception {
        Method delete = ServiceController.class.getDeclaredMethod("delete", String.class, User.class);

        // Con la expresion original (DELETE_SERVICE || ADMINISTRATOR && isActive),
        // DELETE_SERVICE=true hacia verdadera la rama OR y se saltaba isActive.
        assertThat(evaluate(delete, authentication("DELETE_SERVICE"), false)).isFalse();
        assertThat(evaluate(delete, authentication("DELETE_SERVICE"), true)).isTrue();
        assertThat(evaluate(delete, authentication("ADMINISTRATOR"), false)).isFalse();
        assertThat(evaluate(delete, authentication("ADMINISTRATOR"), true)).isTrue();
    }

    private static Boolean evaluate(Method method, Authentication authentication, boolean active) {
        when(securityService.isActive(any(Authentication.class))).thenReturn(active);

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[0]);
        when(invocation.getThis()).thenReturn(mock(method.getDeclaringClass()));

        EvaluationContext evaluationContext = expressionHandler.createEvaluationContext(authentication, invocation);
        // Parametro usado por AuthController.refresh; en produccion lo aporta el argumento del metodo.
        evaluationContext.setVariable("refreshTokenRequestDto", new RefreshTokenRequestDto("test-refresh-token"));

        Expression expression = PARSER.parseExpression(expressionOf(method));
        return expression.getValue(evaluationContext, Boolean.class);
    }

    private static Authentication authenticationOfAllPermissions() {
        String[] permissions = Arrays.stream(AppPermission.values())
                .map(Enum::name)
                .toArray(String[]::new);
        return authentication(permissions);
    }

    private static Authentication authentication(String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken("tester", "not-used", grantedAuthorities);
    }

    private static List<Method> annotatedControllerMethods() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<Method> methods = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("org.icc.pecesatierra.features")) {
            try {
                Class<?> controller = Class.forName(candidate.getBeanClassName());
                for (Method method : controller.getDeclaredMethods()) {
                    if (!method.isSynthetic() && method.isAnnotationPresent(PreAuthorize.class)) {
                        methods.add(method);
                    }
                }
            } catch (ClassNotFoundException exception) {
                fail("No se pudo cargar el controller " + candidate.getBeanClassName(), exception);
            }
        }
        return methods;
    }

    private static String expressionOf(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        if (annotation == null) {
            throw new IllegalArgumentException(describe(method) + " no tiene @PreAuthorize");
        }
        return annotation.value();
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }
}
