package org.icc.pecesatierra.exceptions;

public class CannotDeleteUserWithMemberOutsideYouBranch extends RuntimeException {
    public CannotDeleteUserWithMemberOutsideYouBranch(String message) {
        super(message);
    }
}
