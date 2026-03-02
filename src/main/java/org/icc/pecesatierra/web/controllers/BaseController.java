package org.icc.pecesatierra.web.controllers;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

public abstract class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, ""));
    }

    protected <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, data, "Created successfully"));
    }

    protected ResponseEntity<Void> noContent(Runnable action) {
        action.run();
        return ResponseEntity.noContent().build();
    }

    protected <T> ResponseEntity<ApiResponse<List<?>>> error(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, Collections.emptyList(), message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status, T data) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, data, message));
    }

    @Data
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
    }
}
