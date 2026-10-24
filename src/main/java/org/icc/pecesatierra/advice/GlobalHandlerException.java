package org.icc.pecesatierra.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.utils.models.ErrorResponseDto;
import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalHandlerException {

    static final String FORBIDDEN_MESSAGE = "No tiene permisos para realizar esta acción.";
    static final String UNAUTHORIZED_MESSAGE = "No autorizado.";
    static final String INVALID_BODY_MESSAGE = "El cuerpo de la petición es inválido o está mal formado.";

    @ExceptionHandler(ApiException.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ResponseEntity<ErrorResponseDto> handleApiException(ApiException ex, HttpServletRequest request) {
        ex.printStackTrace(System.err);
        return buildResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ResponseEntity<ErrorResponseDto> handleAInternalAuthenticationServiceException(InternalAuthenticationServiceException ex, HttpServletRequest request) {
        // No se devuelve ex.getMessage(): puede contener detalles internos del login publico.
        log.warn("Fallo interno autenticando al usuario: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo de peticion ilegible: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, INVALID_BODY_MESSAGE, request.getRequestURI());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDto> handleBindException(BindException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("La petición contiene datos inválidos.");
        log.warn("Peticion con datos invalidos: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Parametro con tipo invalido: {}={}", ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido.", request.getRequestURI());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponseDto> handlePropertyReferenceException(PropertyReferenceException ex, HttpServletRequest request) {
        log.warn("Campo de ordenamiento invalido: {}", ex.getPropertyName());
        return buildResponse(HttpStatus.BAD_REQUEST, "El campo de ordenamiento no es válido.", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handlerMethodArgumentNotValidException(HttpServletRequest httpServletRequest,
                                                                                   MethodArgumentNotValidException exception) {
        printStackTrace(exception);
        // Puede haber solo errores globales (constraints de clase), sin FieldError.
        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("La petición contiene datos inválidos.");
        return buildResponse(HttpStatus.BAD_REQUEST, message, httpServletRequest.getRequestURI());

    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex,
                                                                                         HttpServletRequest request) {
        log.warn("Falta el parametro requerido: {}", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getParameterName() + "' es obligatorio.", request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex,
                                                                                         HttpServletRequest request) {
        log.warn("Metodo HTTP no soportado: {} {}", ex.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "El método HTTP no está soportado para esta ruta.", request.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex,
                                                                                    HttpServletRequest request) {
        log.warn("Tipo de contenido no soportado: {}", ex.getContentType());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El tipo de contenido no está soportado.", request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                                HttpServletRequest request) {
        log.warn("Archivo demasiado grande en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido.", request.getRequestURI());
    }

    /**
     * Sin este handler, un recurso inexistente cae en el catch-all y responde 500.
     * Las rutas no-API siguen sirviendo {@code index.html} desde {@code MvcConfig};
     * solo se llega aqui cuando el resource resolver no encuentra nada.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                          HttpServletRequest request) {
        log.warn("Recurso no encontrado: {}", request.getRequestURI());
        return buildResponse(HttpStatus.NOT_FOUND,
                "El recurso solicitado no existe.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllExceptions(Exception ex, HttpServletRequest request) {
        printStackTrace(ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado", request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        printStackTrace(ex);
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.", request.getRequestURI());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthorizationDeniedException(AuthorizationDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado a {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return new ResponseEntity<>(error, status);
    }

    private void printStackTrace(Exception ex) {
        log.error("""
                Error: {}
                StackTrace:
                """, ex.getMessage());
        ex.printStackTrace(System.err);
    }

}