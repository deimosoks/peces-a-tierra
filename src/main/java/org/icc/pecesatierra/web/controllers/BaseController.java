package org.icc.pecesatierra.web.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

public abstract class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, data, HttpStatus.CREATED.name()));
    }

    protected <T> ResponseEntity<ApiResponse<T>> noContent(Runnable action) {
        action.run();
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    protected <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status) {
        return ResponseEntity.status(status).body((ApiResponse<T>) new ApiResponse<>(false, message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status, T data) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, data, message));
    }

    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;

        protected ApiResponse(final boolean success, final T data) {
            this.success = success;
            this.data = data;
            this.message = "";
        }

        @SuppressWarnings("unchecked")
        protected ApiResponse(final boolean success) {
            this.success = success;
            this.data = (T) Collections.emptyList();
            this.message = "";
        }
    }
}
