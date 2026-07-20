package org.icc.pecesatierra.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.utils.models.ErrorResponseDto;
import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler(ApiException.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ResponseEntity<ErrorResponseDto> handleApiException(ApiException ex, HttpServletRequest request) {
        ex.printStackTrace(System.err);
        return buildResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ResponseEntity<ErrorResponseDto> handleAInternalAuthenticationServiceException(InternalAuthenticationServiceException ex, HttpServletRequest request) {
        ex.printStackTrace(System.err);
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handlerMethodArgumentNotValidException(HttpServletRequest httpServletRequest,
                                                                                   MethodArgumentNotValidException exception) {
        printStackTrace(exception);
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage(), httpServletRequest.getRequestURI());

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
        printStackTrace(ex);
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.", request.getRequestURI());
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