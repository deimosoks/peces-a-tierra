package org.icc.pecesatierra.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.error.ErrorResponseDto;
import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        return buildResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handlerMethodArgumentNotValidException(HttpServletRequest httpServletRequest,
                                                                                   MethodArgumentNotValidException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage(), httpServletRequest.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("""
                Error: {}
                StackTrace: {}
                """, ex.getMessage(), ex.getStackTrace());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado", request.getRequestURI());
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
}