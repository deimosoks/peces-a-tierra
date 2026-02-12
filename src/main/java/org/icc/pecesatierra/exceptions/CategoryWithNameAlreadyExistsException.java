package org.icc.pecesatierra.exceptions;

public class CategoryWithNameAlreadyExistsException extends RuntimeException {
    public CategoryWithNameAlreadyExistsException(String message) {
        super(message);
    }
}
