package org.icc.pecesatierra.unit.advice;

import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.utils.models.ApiException;
import org.icc.pecesatierra.utils.models.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato HTTP de errores (H-11). Las denegaciones por permiso son 403, la
 * entrada invalida es 400, el fallo de credenciales sigue siendo 401 y los
 * errores inesperados son 500 sin detalles internos.
 */
class GlobalHandlerExceptionTest {

    private final GlobalHandlerException handler = new GlobalHandlerException();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/prueba");

    @Test
    void authorizationDeniedIs403AndNot401_H11() {
        ResponseEntity<ErrorResponseDto> response = handler.handleAuthorizationDeniedException(
                new AuthorizationDeniedException("Access Denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getMessage())
                .isEqualTo("No tiene permisos para realizar esta acción.");
        assertThat(response.getBody().getPath()).isEqualTo("/api/prueba");
    }

    @Test
    void internalAuthenticationFailureDoesNotLeakDetails_H11() {
        ResponseEntity<ErrorResponseDto> response = handler.handleAInternalAuthenticationServiceException(
                new InternalAuthenticationServiceException("detalle interno: usuario admin no existe"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("No autorizado.");
        assertThat(response.getBody().getMessage()).doesNotContain("detalle interno");
    }

    @Test
    void invalidCredentialsRemain401() {
        ResponseEntity<ErrorResponseDto> response = handler.handleBadCredentialsException(
                new BadCredentialsException("Bad credentials"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Credenciales invalidas.");
    }

    @Test
    void unreadableBodyIs400() {
        HttpInputMessage body = new MockHttpInputMessage(new byte[0]);
        ResponseEntity<ErrorResponseDto> response = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("JSON parse error: Unexpected character", body), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getMessage())
                .isEqualTo("El cuerpo de la petición es inválido o está mal formado.");
    }

    @Test
    void beanValidationFailureIs400WithTheFieldMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "name", "El nombre es obligatorio"));
        MethodParameter parameter = new MethodParameter(dummyMethod(), 0);

        ResponseEntity<ErrorResponseDto> response = handler.handlerMethodArgumentNotValidException(
                request, new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("El nombre es obligatorio");
    }

    @Test
    void bindingFailureIs400() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "startDate", "La fecha es obligatoria"));

        ResponseEntity<ErrorResponseDto> response = handler.handleBindException(
                new BindException(bindingResult), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("La fecha es obligatoria");
    }

    @Test
    void typeMismatchOnARequestParamIs400() throws Exception {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", new MethodParameter(dummyMethod(), 0),
                new NumberFormatException("For input string: abc"));

        ResponseEntity<ErrorResponseDto> response = handler.handleMethodArgumentTypeMismatchException(
                exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("El parámetro 'page' tiene un valor inválido.");
    }

    @Test
    void invalidSortPropertyIs400() {
        PropertyReferenceException exception = new PropertyReferenceException(
                "campoInexistente", TypeInformation.of(String.class), List.of());

        ResponseEntity<ErrorResponseDto> response = handler.handlePropertyReferenceException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("El campo de ordenamiento no es válido.");
    }

    @Test
    void apiExceptionKeepsItsOwnStatus() {
        ApiException exception = new ApiException("Ya existe un registro con ese dato.", HttpStatus.CONFLICT) {
        };

        ResponseEntity<ErrorResponseDto> response = handler.handleApiException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().getMessage()).isEqualTo("Ya existe un registro con ese dato.");
    }

    @Test
    void unexpectedExceptionIs500WithoutInternalDetails() {
        ResponseEntity<ErrorResponseDto> response = handler.handleAllExceptions(
                new IllegalStateException("detalle interno que no debe salir"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().getMessage()).isEqualTo("Ha ocurrido un error inesperado");
        assertThat(response.getBody().getMessage()).doesNotContain("detalle interno");
    }

    @SuppressWarnings("unused")
    private Method dummyMethod() throws NoSuchMethodException {
        return GlobalHandlerExceptionTest.class.getDeclaredMethod("dummy", Object.class);
    }

    @SuppressWarnings("unused")
    private void dummy(Object body) {
    }
}
