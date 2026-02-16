package org.icc.pecesatierra.exceptions;

public class CannotDeleteMemberOutSideYourBranchException extends RuntimeException {
    public CannotDeleteMemberOutSideYourBranchException(String message) {
        super(message);
    }
}
