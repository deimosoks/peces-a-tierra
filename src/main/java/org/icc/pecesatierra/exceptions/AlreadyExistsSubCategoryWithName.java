package org.icc.pecesatierra.exceptions;

public class AlreadyExistsSubCategoryWithName extends RuntimeException {
    public AlreadyExistsSubCategoryWithName(String message) {
        super(message);
    }
}
