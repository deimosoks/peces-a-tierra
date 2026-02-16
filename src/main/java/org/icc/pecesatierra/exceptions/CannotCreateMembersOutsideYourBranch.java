package org.icc.pecesatierra.exceptions;

public class CannotCreateMembersOutsideYourBranch extends RuntimeException {
    public CannotCreateMembersOutsideYourBranch(String message) {
        super(message);
    }
}
