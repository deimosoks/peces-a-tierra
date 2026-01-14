package org.icc.pecesatierra.advice;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.icc.pecesatierra.dtos.error.ErrorResponseDto;
import org.icc.pecesatierra.exceptions.MemberNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponseDto> handlerExpiredJwtException(HttpServletRequest httpServletRequest,
                                                                       ExpiredJwtException exception){
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerExpiredJwtException(HttpServletRequest httpServletRequest,
                                                                       MemberNotFoundException exception){
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

}
