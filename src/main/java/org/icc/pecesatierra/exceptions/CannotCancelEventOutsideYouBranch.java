package org.icc.pecesatierra.exceptions;

public class CannotCancelEventOutsideYouBranch extends RuntimeException {
    public CannotCancelEventOutsideYouBranch(String message) {
        super(message);
    }
}
