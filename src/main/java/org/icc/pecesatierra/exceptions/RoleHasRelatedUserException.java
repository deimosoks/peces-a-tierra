package org.icc.pecesatierra.exceptions;

public class RoleHasRelatedUserException extends RuntimeException {
    public RoleHasRelatedUserException(String message) {
        super(message);
    }
}
