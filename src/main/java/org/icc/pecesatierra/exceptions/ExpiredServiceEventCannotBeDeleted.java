package org.icc.pecesatierra.exceptions;

public class ExpiredServiceEventCannotBeDeleted extends RuntimeException {
    public ExpiredServiceEventCannotBeDeleted(String message) {
        super(message);
    }
}
